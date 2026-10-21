package com.sigpher.xpdf.main

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.App
import com.sigpher.xpdf.common.DataManager
import com.sigpher.xpdf.data.ImportPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author sigpher
 */
internal class MainPresenter(view: IMainView) : IMainPresenter(view) {

    override fun importUris(uris: List<Uri>, groupName: String) {
        if (uris.isEmpty()) {
            return
        }
        val context = App.getContext()
        (view as CoroutineScope).launch {
            view.onShowLoading()
            val count = withContext(Dispatchers.IO) {
                ImportPipeline.importUris(context, uris, groupName)
            }
            DataManager.updateAll()
            view.onUpdate()
            view.onHideLoading()
            if (count <= 0) {
                view.onShowMessage(R.string.app_import_failed)
            }
        }
    }

    override fun importTree(treeUri: Uri) {
        val context = App.getContext()
        (view as CoroutineScope).launch {
            view.onShowLoading()
            val uris = withContext(Dispatchers.IO) {
                ImportPipeline.collectPdfUris(context, treeUri)
            }
            val groupName = withContext(Dispatchers.IO) {
                DocumentFile.fromTreeUri(context, treeUri)?.name
                        ?: context.getString(R.string.app_import_folder)
            }
            val count = withContext(Dispatchers.IO) {
                ImportPipeline.importUris(context, uris, groupName)
            }
            DataManager.updateAll()
            view.onUpdate()
            view.onHideLoading()
            if (count <= 0) {
                view.onShowMessage(R.string.app_import_failed)
            }
        }
    }
}
