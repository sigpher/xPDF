package com.sigpher.xpdf.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.blankj.utilcode.util.LogUtils
import com.sigpher.xpdf.common.DBHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * SAF 导入管道：系统选择器 -> 私有目录副本 -> 生成封面/页数 -> 入库。
 * <p>
 * 应用不再申请存储权限，也不直接索引外部目录：所有 PDF 都先复制到
 * `getExternalFilesDir/pdfs`，数据库里保存的是这份私有副本的路径，
 * 因此阅读、备份、快捷方式等既有逻辑无需区分来源。
 *
 * @author sigpher
 */
object ImportPipeline {

    private const val PDF_DIR = "pdfs"
    private const val MIME_PDF = "application/pdf"

    /** 目录树导入的最大递归深度，避免超大目录把导入拖成"卡死" */
    private const val MAX_DEPTH = 8

    /** 单次导入的文件数量上限，防止误选整张 SD 卡 */
    private const val MAX_FILES = 800

    /**
     * 导入系统选择器返回的 URI。
     *
     * @return 成功拷贝并入库的文件数
     */
    suspend fun importUris(context: Context, uris: List<Uri>, groupName: String): Int {
        if (uris.isEmpty()) {
            return 0
        }
        return withContext(Dispatchers.IO) {
            val targetDir = ensurePdfDir(context)
            val paths = ArrayList<String>(uris.size)
            for (uri in uris) {
                val copied = copyToPrivate(context, uri, targetDir) ?: continue
                // 已经入库的私有副本不再重复插入，避免书架上出现重复条目
                if (DBHelper.queryPDFByPath(copied.absolutePath) != null) {
                    continue
                }
                paths.add(copied.absolutePath)
            }
            if (paths.isEmpty()) {
                return@withContext 0
            }
            DBHelper.insert(paths, groupName)
            paths.size
        }
    }

    /**
     * 收集目录树下的全部 PDF（后台遍历，不触碰主线程）。
     */
    suspend fun collectPdfUris(context: Context, treeUri: Uri): List<Uri> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
        if (root == null || !root.isDirectory) {
            return@withContext emptyList()
        }
        val result = ArrayList<Uri>()
        collect(context, root, result, 0)
        result
    }

    private fun collect(context: Context, dir: DocumentFile, out: MutableList<Uri>, depth: Int) {
        if (depth > MAX_DEPTH || out.size >= MAX_FILES) {
            return
        }
        val children = dir.listFiles()
        for (child in children) {
            if (out.size >= MAX_FILES) {
                return
            }
            if (child.isDirectory) {
                collect(context, child, out, depth + 1)
                continue
            }
            if (isPdf(context, child)) {
                out.add(child.uri)
            }
        }
    }

    private fun isPdf(context: Context, file: DocumentFile): Boolean {
        if (file.type == MIME_PDF) {
            return true
        }
        val name = file.name ?: return false
        return name.endsWith(".pdf", ignoreCase = true)
    }

    /**
     * 把 URI 流式拷贝到应用私有目录；同名同大小的文件视为已导入，直接跳过。
     */
    private fun copyToPrivate(context: Context, uri: Uri, targetDir: File): File? {
        val displayName = queryDisplayName(context, uri) ?: return null
        if (!displayName.endsWith(".pdf", ignoreCase = true)) {
            return null
        }
        val safeName = displayName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val existing = File(targetDir, safeName)
        if (existing.exists() && existing.length() > 0) {
            LogUtils.d("skip duplicated pdf: $safeName")
            return existing
        }
        val target = uniqueFile(targetDir, safeName)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE * 8)
                }
            } ?: return null
            if (target.length() == 0L) {
                target.delete()
                return null
            }
            target
        } catch (e: Exception) {
            LogUtils.e(e)
            target.delete()
            null
        }
    }

    private fun ensurePdfDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, PDF_DIR).apply { if (!exists()) mkdirs() }
    }

    private fun uniqueFile(dir: File, name: String): File {
        val file = File(dir, name)
        if (!file.exists()) {
            return file
        }
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var index = 1
        while (true) {
            val candidate = File(dir, "$base($index)$ext")
            if (!candidate.exists()) {
                return candidate
            }
            index++
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (index >= 0) {
                                return cursor.getString(index)
                            }
                        }
                    }
        } catch (e: Exception) {
            LogUtils.e(e)
        }
        val fallback = uri.lastPathSegment ?: return null
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))
        return if (ext != null && !fallback.endsWith(".$ext", ignoreCase = true)) "$fallback.$ext" else fallback
    }
}
