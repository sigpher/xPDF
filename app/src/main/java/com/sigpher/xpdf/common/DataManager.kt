package com.sigpher.xpdf.common

import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.ThreadUtils.SimpleTask
import com.sigpher.xpdf.common.bean.Collection
import com.sigpher.xpdf.common.bean.Cover
import com.sigpher.xpdf.common.bean.PDF
import com.sigpher.xpdf.common.event.AllEvent
import com.sigpher.xpdf.common.event.LegacyMissingEvent
import com.sigpher.xpdf.common.event.RecentPDFEvent
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 * 书库内存缓存。
 *
 * 所有列表都以“就地更新”的方式维护（clear + addAll），页面持有的列表引用因此始终有效；
 * 数据库读取统一在 IO 线程完成，主线程只读缓存。
 *
 * 对外保留 `DataManager.getXxx()` 函数形式（而非属性），以免改动既有调用点。
 *
 * @author sigpher
 */
object DataManager {

    private val pathList: MutableList<String> = ArrayList()
    private val pdfList: MutableList<PDF> = ArrayList()
    private val recentPdfList: MutableList<PDF> = ArrayList()
    private val collectionList: MutableList<Collection> = ArrayList()
    private val coverList: MutableList<Cover> = ArrayList()

    /**
     * 目录 -> 该目录下的 PDF（按 position 倒序），避免每次按目录筛选都全表遍历
     */
    private val dirIndex: MutableMap<String, MutableList<PDF>> = HashMap()

    /**
     * 异步加载书库数据：Application 启动时调用，避免冷启动在主线程同步读库。
     * 加载完成后通过 EventBus 通知书库刷新，页面此时尚未创建也不会丢数据。
     */
    fun initAsync() {
        ThreadUtils.executeByIo<Any>(object : SimpleTask<Any?>() {
            override fun doInBackground(): Any? {
                reloadAllFromDb()
                return countUnreadablePdf()
            }

            override fun onSuccess(result: Any?) {
                EventBus.getDefault().post(AllEvent())
                EventBus.getDefault().post(RecentPDFEvent())
                val missing = (result as? Int) ?: 0
                if (missing > 0) {
                    EventBus.getDefault().post(LegacyMissingEvent(missing))
                }
            }
        })
    }

    private fun reloadAllFromDb() {
        val pdfs = DBHelper.queryAllPDF()
        val recents = DBHelper.queryRecentPDF()
        val collections = DBHelper.queryAllCollection()
        synchronized(this) {
            pdfList.clear()
            pdfList.addAll(pdfs)
            recentPdfList.clear()
            recentPdfList.addAll(recents)
            collectionList.clear()
            collectionList.addAll(collections)
            updatePathList()
            rebuildDirIndex()
            updateCoverList()
        }
    }

    /**
     * 统计旧版本遗留、在当前系统上已无法读取的条目数（分区存储上线后外部路径不再可访问）。
     * 只在 IO 线程调用。
     */
    private fun countUnreadablePdf(): Int {
        var count = 0
        for (pdf in pdfList) {
            val path = pdf.path
            if (path == null || !File(path).canRead()) {
                count++
            }
        }
        return count
    }

    fun updateAll() {
        synchronized(this) {
            collectionList.clear()
            collectionList.addAll(DBHelper.queryAllCollection())
            recentPdfList.clear()
            recentPdfList.addAll(DBHelper.queryRecentPDF())
            pdfList.clear()
            pdfList.addAll(DBHelper.queryAllPDF())
            updatePathList()
            rebuildDirIndex()
            updateCoverList()
        }
    }

    fun updatePDFs() {
        synchronized(this) {
            pdfList.clear()
            pdfList.addAll(DBHelper.queryAllPDF())
            updatePathList()
            rebuildDirIndex()
            updateCoverList()
        }
    }

    /** 重建“目录 -> PDF”索引；pdfList 变化后必须调用，否则封面与分组列表会读到旧数据 */
    private fun rebuildDirIndex() {
        dirIndex.clear()
        for (pdf in pdfList) {
            val dir = pdf.dir ?: continue
            dirIndex.getOrPut(dir) { ArrayList() }.add(pdf)
        }
        val desc = Comparator<PDF> { o1, o2 -> o2.position - o1.position }
        for (group in dirIndex.values) {
            group.sortWith(desc)
        }
    }

    fun updateRecentPDFs() {
        val recents = DBHelper.queryRecentPDF()
        synchronized(this) {
            recentPdfList.clear()
            recentPdfList.addAll(recents)
        }
    }

    fun updateCollection() {
        synchronized(this) {
            collectionList.clear()
            collectionList.addAll(DBHelper.queryAllCollection())
            updateCoverList()
        }
    }

    private fun updateCoverList() {
        coverList.clear()
        for (c in collectionList) {
            val temp = dirIndex[c.name]
            if (temp == null || temp.isEmpty()) {
                continue
            }
            // 每个分组最多取前 4 个 PDF 封面，索引内已按 position 倒序
            val list = temp.subList(0, minOf(temp.size, 4))
            val coverPaths = ArrayList<String>(list.size)
            for (pdf in list) {
                coverPaths.add(pdf.cover)
            }
            coverList.add(Cover(c.name, coverPaths, temp.size))
        }
    }

    private fun updatePathList() {
        pathList.clear()
        for (pdf in pdfList) {
            pathList.add(pdf.path)
        }
    }

    // 返回类型保持 MutableList：与改造前 Java 返回 List（平台类型）时调用方可直接增删的行为一致
    fun getRecentPdfList(): MutableList<PDF> = recentPdfList

    fun getPathList(): MutableList<String> = pathList

    fun getPdfList(): MutableList<PDF> = pdfList

    /**
     * 取某目录下的 PDF（已按 position 倒序）。
     * 返回副本，调用方可自由修改，不会破坏内部索引。
     */
    fun getPdfList(name: String?): MutableList<PDF> {
        synchronized(this) {
            val group = if (name == null) null else dirIndex[name]
            if (group == null || group.isEmpty()) {
                return ArrayList()
            }
            return ArrayList(group)
        }
    }

    fun getCollectionList(): MutableList<Collection> = collectionList

    fun getCoverList(): MutableList<Cover> = coverList
}
