package com.sigpher.xpdf.common

import android.content.Context
import com.sigpher.xpdf.common.bean.Annotation
import com.sigpher.xpdf.common.bean.Collection
import com.sigpher.xpdf.common.bean.Cover
import com.sigpher.xpdf.common.bean.PDF
import com.sigpher.xpdf.common.bean.RecentPDF
import com.sigpher.xpdf.common.event.ImportEvent
import com.sigpher.xpdf.common.greendao.*
import com.sigpher.xpdf.common.utils.PdfUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.StringUtils
import org.greenrobot.eventbus.EventBus
import java.util.*


/**
 * @author sigpher
 */
object DBHelper {

    private lateinit var sDaoSession: DaoSession

    @JvmStatic
    fun init(context: Context, dbName: String) {
//        val helper = DevOpenHelper(context, dbName)
//        val db = helper.writableDatabase
//        val daoMaster = DaoMaster(db)
//        sDaoSession = daoMaster.newSession()

        val helper = UpdateOpenHelper(context, dbName, null)
        val db = helper.writableDatabase
        // 注意：该数据库连接属于 DaoMaster，所以多个 Session 指的是相同的数据库连接。
        val daoMaster = DaoMaster(db)
        sDaoSession = daoMaster.newSession()
    }

    /**
     * 历史数据修正：补齐 position 并重置 scaleFactor。
     *
     * 只在 [App] 检测到历史数据版本落后时执行一次，因此必须尽量快：
     * 整体放在一个事务里，并按分组批量 updateInTx，避免逐条写入（几千本书会耗时数秒）。
     */
    fun migrate() {
        val database = sDaoSession.database
        val start = System.currentTimeMillis()
        database.beginTransaction()
        try {
            val collections = queryAllCollection()
            collections.forEachIndexed { index, item -> item.position = index }
            if (collections.isNotEmpty()) {
                sDaoSession.collectionDao.updateInTx(collections)
            }
            for (item in collections) {
                val pdfs = queryPDF(item.name)
                pdfs.forEachIndexed { index, pdf ->
                    pdf.position = index
                    pdf.scaleFactor = 1.0f
                }
                if (pdfs.isNotEmpty()) {
                    sDaoSession.pdfDao.updateInTx(pdfs)
                }
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
        LogUtils.i("legacy data migrate cost ${System.currentTimeMillis() - start}ms")
    }

    fun updateDirName(oldDirName: String, newDirName: String): Boolean {
        if (StringUtils.isEmpty(newDirName)
                || oldDirName == newDirName) {
            return false
        }
        val pdfDao = sDaoSession.pdfDao
        val recentPDFDao = sDaoSession.recentPDFDao
        val collectionDao = sDaoSession.collectionDao
        val pdfList = pdfDao.queryBuilder()
                .where(PDFDao.Properties.Dir.eq(oldDirName))
                .list()
        val recentList = recentPDFDao.queryBuilder()
                .where(RecentPDFDao.Properties.Dir.eq(oldDirName))
                .list()
        val collectionList = collectionDao.queryBuilder()
                .where(CollectionDao.Properties.Name.eq(oldDirName))
                .list()
        for (pdf in pdfList) {
            pdf.dir = newDirName
        }
        for (recent in recentList) {
            recent.dir = newDirName
        }
        for (c in collectionList) {
            c.name = newDirName
        }
        pdfDao.updateInTx(pdfList)
        recentPDFDao.updateInTx(recentList)
        collectionDao.updateInTx(collectionList)
        return true
    }

    fun updatePDF(pdf: PDF?) {
        pdf?.run {
            sDaoSession.pdfDao.insertOrReplace(pdf)
        }
    }

    private fun updateCollection(c: Collection) {
        val collectionDao = sDaoSession.collectionDao
        collectionDao?.insertOrReplace(c)
    }

    fun updateCollection() {
        sDaoSession.collectionDao.updateInTx(DataManager.getCollectionList())
    }

    fun updatePDFs(pdfs: List<PDF>) {
        sDaoSession.pdfDao.updateInTx(pdfs)
    }

    @JvmStatic
    fun queryRecentPDF(): List<PDF> {
        val list = sDaoSession.loadAll<RecentPDF, Any>(RecentPDF::class.java)
        if (list.isEmpty()) return ArrayList()
        // 两次查询即可完成关联，避免逐条查询造成的 N+1
        val pdfMap = HashMap<String, PDF>(list.size * 2)
        for (pdf in sDaoSession.loadAll<PDF, Any>(PDF::class.java)) {
            pdfMap["${pdf.dir}\u0000${pdf.name}"] = pdf
        }
        val recentPDFList: MutableList<PDF> = ArrayList(list.size)
        for (recent in list) {
            pdfMap["${recent.dir}\u0000${recent.name}"]?.let { recentPDFList.add(it) }
        }
        // 按最新阅读时间排序
        recentPDFList.sortWith(Comparator { p1: PDF, p2: PDF -> (p2.latestRead - p1.latestRead).toInt() })
        return recentPDFList
    }

    @JvmStatic
    fun queryAllCollection(): List<Collection> {
        val list = sDaoSession.loadAll<Collection, Any>(Collection::class.java)
        list.sortWith(Comparator { c1, c2 ->
            c2.position - c1.position
        })
        return list
    }

    @JvmStatic
    fun queryAllPDF(): List<PDF> {
        val list = sDaoSession.loadAll<PDF, Any>(PDF::class.java)
        list.sortWith(kotlin.Comparator { o1, o2 ->
            o2.position - o1.position
        })
        return list
    }

    @JvmStatic
    fun queryPDFByPath(path: String): PDF? {
        return sDaoSession.pdfDao.queryRaw("where PATH = ?", path).getOrNull(0)
    }

    private fun queryPDF(dir: String?): List<PDF> {
        return if (StringUtils.isEmpty(dir)) {
            ArrayList()
        } else sDaoSession.queryRaw<PDF, Any>(PDF::class.java, "where DIR = ?", dir)
    }

    fun insertRecent(pdf: PDF) {
        val dir = pdf.dir
        val name = pdf.name
        val recent = RecentPDF(dir, name)
        sDaoSession.insertOrReplace(recent)
    }

    fun deleteRecent(list: List<PDF>): List<String> {
        val dirList: MutableList<String> = ArrayList()
        for (pdf in list) {
            // 历史数据里 name 可能为空，用空串兜底以免调用方收到 null
            val name = pdf.name.orEmpty()
            dirList.add(name)
            sDaoSession.database.execSQL("delete from RECENT_PDF where name = ?", arrayOf(name))
        }
        return dirList
    }

    fun deleteCollection(name: String?): String? {
        sDaoSession.database.execSQL("delete from COLLECTION where name = ?", arrayOf(name))
        return name
    }

    fun deleteCollection(list: List<Cover>): List<String> {
        val dirList: MutableList<String> = ArrayList()
        for (c in list) {
            dirList.add(c.name)
            sDaoSession.database.execSQL("delete from Collection where name = ?", arrayOf(c.name))
            // 先级联删除该分组下文档的标注，再删除文档本身
            sDaoSession.database.execSQL(
                    "delete from ANNOTATION where PDF_ID in (select _id from PDF where dir = ?)",
                    arrayOf(c.name))
            sDaoSession.database.execSQL("delete from PDF where dir = ?", arrayOf(c.name))
            sDaoSession.database.execSQL("delete from RECENT_PDF where dir = ?", arrayOf(c.name))
        }
        return dirList
    }

    fun deletePDF(list: List<PDF>?): List<String> {
        val nameList: MutableList<String> = ArrayList()
        if (list != null) {
            for (pdf in list) {
                nameList.add(pdf.name.orEmpty())
                sDaoSession.pdfDao.delete(pdf)
                // 级联清理该文档的标注
                pdf.id?.let { deleteAnnotationsByPdf(it) }
            }
        }
        return nameList
    }

    //region 标注

    /** 查询某个文档的全部标注（按页、起始索引排序） */
    @JvmStatic
    fun queryAnnotations(pdfId: Long): List<Annotation> {
        return sDaoSession.annotationDao.queryBuilder()
                .where(AnnotationDao.Properties.PdfId.eq(pdfId))
                .orderAsc(AnnotationDao.Properties.Page, AnnotationDao.Properties.StartIndex)
                .list()
    }

    /** 插入或更新标注，返回行 ID */
    @JvmStatic
    fun saveAnnotation(annotation: Annotation): Long {
        return sDaoSession.annotationDao.insertOrReplace(annotation)
    }

    @JvmStatic
    fun deleteAnnotation(annotation: Annotation) {
        sDaoSession.annotationDao.delete(annotation)
    }

    @JvmStatic
    fun deleteAnnotationsByPdf(pdfId: Long) {
        sDaoSession.database.execSQL("delete from ANNOTATION where PDF_ID = ?", arrayOf<Any>(pdfId))
    }

    //endregion

    fun insert(pathList: List<String>?): Boolean {
        if (pathList == null || pathList.isEmpty()) {
            return false
        }
        val event = ImportEvent()
        event.curProgress = 0
        event.totalProgress = pathList.size
        val tempPath = pathList[0]
        val parentPath = tempPath.substring(0, tempPath.lastIndexOf("/"))
        val dir = tempPath.substring(0, tempPath.lastIndexOf("/")).substring(parentPath.lastIndexOf("/") + 1)
        var max = if (dir.isNotEmpty()) {
            queryPDF(dir).size
        } else 0
        for (path in pathList) {
            if (event.stop) return false
            event.name = getName(path)
            event.curProgress++
            EventBus.getDefault().post(event)
//            // 去除了文件名称的父路径
//            val parentPath = path.substring(0, path.lastIndexOf("/"))
//            // 所属文件夹
//            dir = parentPath.substring(parentPath.lastIndexOf("/") + 1)
            // 插入 Collection 对象
            val collectionList = DataManager.getCollectionList()
            val c = Collection(dir, collectionList.size)
            sDaoSession.insertOrReplace(c)
            insertPDFs(dir, path, max++)
        }
        return true
    }

    fun insertBackup(pdfs: List<PDF>, groupName: String) {
        if (pdfs.isEmpty()) {
            return
        }
        val c = Collection(groupName, DataManager.getCollectionList().size)
        sDaoSession.insertOrReplace(c)
        for (pdf in pdfs) {
            insertPDFs(groupName, "", 0, pdf)
        }
    }

    fun insert(pathList: List<String>?, groupName: String): Boolean {
        if (pathList == null || pathList.isEmpty()) return false
        // 插入 Collection 对象
        val c = Collection(groupName, DataManager.getCollectionList().size)
        sDaoSession.insertOrReplace(c)
        val event = ImportEvent()
        event.curProgress = 0
        event.totalProgress = pathList.size
        var max = queryPDF(groupName).size
        for (path in pathList) {
            if (event.stop) return false
            event.name = getName(path)
            event.curProgress++
            EventBus.getDefault().post(event)
            insertPDFs(groupName, path, max++)
        }
        return true
    }

    fun insertNewCollection(newDirName: String, pdfList: List<PDF>) {
        val c = Collection(newDirName, DataManager.getCollectionList().size)
        sDaoSession.insertOrReplace(c)
        transferPDFs(newDirName, pdfList)
    }

    fun insertPDFsToCollection(dirName: String, pdfList: List<PDF>) {
        transferPDFs(dirName, pdfList)
    }

    private fun insertPDFs(groupName: String, filePath: String, position: Int, pdf: PDF? = null): Boolean {
        val dir = pdf?.dir ?: groupName
        val name = pdf?.name ?: getName(filePath)
        val path = pdf?.path ?: filePath
        // 一次打开完成"取页数 + 渲染封面"，避免同一文件被打开两次
        val renderer = PdfUtils.openRenderer(path)
        var pageCount = 0
        val cover = try {
            pageCount = renderer?.pageCount ?: 0
            PdfUtils.saveBitmap(PdfUtils.renderPage(renderer, 0),
                    "${PathUtils.getInternalAppDataPath()}/$name.jpg")
        } finally {
            try {
                renderer?.close()
            } catch (ignored: Exception) {
            }
        }
        val progress = pdf?.progress ?: "0.0%"
        val curPage = pdf?.curPage ?: 0
        val bookmarkPage = pdf?.bookmark ?: ""
        val totalPage = pdf?.totalPage ?: pageCount
        val latestRead = pdf?.latestRead ?: 0
        val pos = pdf?.position ?: position
        val scaleFactor = pdf?.scaleFactor ?: 1.0f
        PDF().apply {
            this.dir = dir
            this.name = name
            this.path = path
            this.cover = cover
            this.progress = progress
            this.curPage = curPage
            this.bookmark = bookmarkPage
            this.totalPage = totalPage
            this.latestRead = latestRead
            this.position = pos
            this.scaleFactor = scaleFactor
            sDaoSession.insertOrReplace(this)
        }
        return true
    }

    private fun transferPDFs(dirName: String, pdfList: List<PDF>) {
        for (pdf in pdfList) {
            pdf.dir = dirName
        }
        sDaoSession.pdfDao.updateInTx(pdfList)
    }

    private fun getName(path: String): String {
        return path.substring(path.lastIndexOf("/") + 1, path.length - 4)
    }
}