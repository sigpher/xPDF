package com.sigpher.xpdf.preview

import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.text.TextSelection
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.DBHelper
import com.sigpher.xpdf.common.bean.Annotation
import com.sigpher.xpdf.common.utils.ClipboardHelper
import com.sigpher.xpdf.common.utils.DialogUtils
import com.sigpher.xpdf.databinding.AppViewSelectionMenuBinding

/**
 * 长按选区的操作栏与标注流程：复制 / 高亮 / 笔记 / 翻译。
 *
 * 与库的分工：
 * - 选区（命中、扩词、手柄拖动、选区绘制）由 `android-pdf-viewer` 的 TextSelectionManager 负责；
 * - 这里只负责"选好之后做什么"：弹出操作栏、落库标注、唤起翻译卡片。
 */
class TextSelectionController(
    private val activity: PreviewActivity,
    private val pdfView: PDFView,
    private val menuBinding: AppViewSelectionMenuBinding,
    private val translateController: TranslateController,
    private val annotationsOfPage: (Int) -> List<Annotation>,
    private val onAnnotationsChanged: () -> Unit
) {

    private var pdfId: Long = 0

    private var selection: TextSelection? = null
    private var anchorInView: RectF? = null

    init {
        menuBinding.appTvSelectionCopy.setOnClickListener { copySelection() }
        menuBinding.appTvSelectionHighlight.setOnClickListener { annotateSelection(Annotation.TYPE_HIGHLIGHT) }
        menuBinding.appTvSelectionUnderline.setOnClickListener { annotateSelection(Annotation.TYPE_UNDERLINE) }
        menuBinding.appTvSelectionNote.setOnClickListener { noteSelection() }
        menuBinding.appTvSelectionTranslate.setOnClickListener { translateSelection() }
    }

    fun setPdfId(id: Long) {
        pdfId = id
    }

    fun isMenuVisible(): Boolean = menuBinding.root.visibility == View.VISIBLE

    //region 选区回调

    fun onTextSelected(selection: TextSelection, anchorInView: RectF) {
        this.selection = selection
        this.anchorInView = anchorInView
        showMenu(anchorInView)
    }

    fun onTextSelectionChanged(selection: TextSelection, anchorInView: RectF) {
        this.selection = selection
        this.anchorInView = anchorInView
        if (isMenuVisible()) {
            moveMenu(anchorInView)
        }
    }

    fun onSelectionCleared() {
        selection = null
        anchorInView = null
        hideMenu()
    }

    fun hideMenu() {
        if (isMenuVisible()) {
            menuBinding.root.animate().cancel()
            menuBinding.root.visibility = View.GONE
        }
    }

    //endregion

    //region 操作

    private fun copySelection() {
        val text = selection?.text.orEmpty()
        if (text.isEmpty()) {
            ToastUtils.showShort(R.string.app_copy_empty)
            return
        }
        ClipboardHelper.copy(activity, text)
        ToastUtils.showShort(R.string.app_copy_success)
        hideMenu()
        pdfView.clearTextSelection()
    }

    /**
     * 高亮 / 下划线：命中已有标注时进入编辑（可写笔记或删除），否则直接落库。
     */
    private fun annotateSelection(type: Int) {
        val current = selection ?: return
        val existing = findAnnotation(current)
        if (existing != null) {
            hideMenu()
            // 先结束选区，避免弹窗关闭后选区仍占着手势
            pdfView.clearTextSelection()
            openNoteDialog(existing, isNew = false)
            return
        }
        val annotation = buildAnnotation(current, type)
        DBHelper.saveAnnotation(annotation)
        ToastUtils.showShort(if (type == Annotation.TYPE_UNDERLINE) {
            R.string.app_underline_added
        } else {
            R.string.app_highlight_added
        })
        onAnnotationsChanged()
        hideMenu()
        pdfView.clearTextSelection()
    }

    private fun noteSelection() {
        val current = selection ?: return
        hideMenu()
        val existing = findAnnotation(current)
        if (existing != null) {
            openNoteDialog(existing, isNew = false)
            pdfView.clearTextSelection()
            return
        }
        val annotation = buildAnnotation(current, Annotation.TYPE_HIGHLIGHT)
        DBHelper.saveAnnotation(annotation)
        onAnnotationsChanged()
        pdfView.clearTextSelection()
        openNoteDialog(annotation, isNew = true)
    }

    private fun translateSelection() {
        val text = selection?.text.orEmpty()
        if (text.isEmpty()) {
            ToastUtils.showShort(R.string.app_copy_empty)
            return
        }
        hideMenu()
        translateController.show(text)
        // 译文卡片需要能自由滚动页面，这里结束选区，只保留卡片
        pdfView.clearTextSelection()
    }

    //endregion

    //region 标注

    private fun buildAnnotation(selection: TextSelection, type: Int): Annotation {
        val relativeRects = AnnotationOverlayRenderer.toRelativeRects(selection)
        return Annotation().apply {
            this.pdfId = this@TextSelectionController.pdfId
            page = selection.page
            startIndex = selection.startIndex
            endIndex = selection.endIndex
            text = selection.text
            this.type = type
            color = if (type == Annotation.TYPE_UNDERLINE) {
                Annotation.DEFAULT_UNDERLINE_COLOR
            } else {
                Annotation.DEFAULT_HIGHLIGHT_COLOR
            }
            rects = AnnotationOverlayRenderer.toJson(relativeRects)
            createdAt = System.currentTimeMillis()
        }
    }

    /**
     * 判断选区是否落在已有标注上：任一选区矩形中心点落在标注矩形内即视为命中。
     */
    private fun findAnnotation(selection: TextSelection): Annotation? {
        val candidates = annotationsOfPage(selection.page)
        if (candidates.isEmpty()) {
            return null
        }
        val targets = AnnotationOverlayRenderer.toRelativeRects(selection)
        if (targets.isEmpty()) {
            return null
        }
        for (annotation in candidates) {
            val rects = AnnotationOverlayRenderer.parseRects(annotation.rects)
            for (rect in rects) {
                for (target in targets) {
                    if (rect.contains(target.centerX(), target.centerY())) {
                        return annotation
                    }
                }
            }
        }
        return null
    }

    private fun openNoteDialog(annotation: Annotation, isNew: Boolean) {
        val view = LayoutInflater.from(activity).inflate(R.layout.app_dialog_note, null)
        val tvTitle = view.findViewById<TextView>(R.id.app_tv_title)
        val tvQuote = view.findViewById<TextView>(R.id.app_tv_quote)
        val etNote = view.findViewById<EditText>(R.id.app_et_note)
        val btnDelete = view.findViewById<View>(R.id.app_btn_delete)
        val btnCancel = view.findViewById<View>(R.id.app_btn_cancel)
        val btnConfirm = view.findViewById<View>(R.id.app_btn_confirm)

        val dialog = DialogUtils.createDialog(activity, view)
        dialog.setCanceledOnTouchOutside(true)

        tvTitle.setText(if (isNew) R.string.app_note_dialog_title else R.string.app_note_dialog_title_existing)
        tvQuote.text = annotation.text.orEmpty()
        tvQuote.visibility = if (annotation.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        etNote.setText(annotation.note.orEmpty())
        etNote.setSelection(etNote.text?.length ?: 0)
        btnDelete.visibility = if (isNew) View.GONE else View.VISIBLE

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnDelete.setOnClickListener {
            DBHelper.deleteAnnotation(annotation)
            dialog.dismiss()
            ToastUtils.showShort(R.string.app_annotation_deleted)
            onAnnotationsChanged()
        }
        btnConfirm.setOnClickListener {
            val note = etNote.text?.toString()?.trim().orEmpty()
            annotation.note = note.ifEmpty { null }
            DBHelper.saveAnnotation(annotation)
            dialog.dismiss()
            onAnnotationsChanged()
        }
        dialog.show()
    }

    //endregion

    //region 操作栏定位

    private fun showMenu(anchor: RectF) {
        val menu = menuBinding.root
        menu.animate().cancel()
        menu.visibility = View.VISIBLE
        menu.alpha = 0f
        menu.post {
            moveMenu(anchor)
            menu.animate().alpha(1f).setDuration(120).start()
        }
    }

    /**
     * 按选区锚点定位操作栏。
     * 注意：PDFView 可能被 scaleFactor 整体缩放（scaleX/scaleY），锚点是 View 局部坐标，
     * 需要先按缩放换算，再转成相对父容器的坐标。
     */
    private fun moveMenu(anchor: RectF) {
        val menu = menuBinding.root
        if (menu.visibility != View.VISIBLE) {
            return
        }
        val width = menu.width
        val height = menu.height
        if (width <= 0 || height <= 0) {
            menu.post { moveMenu(anchor) }
            return
        }
        val parent = menu.parent as? View ?: return
        val parentLocation = IntArray(2)
        parent.getLocationOnScreen(parentLocation)
        val viewLocation = IntArray(2)
        pdfView.getLocationOnScreen(viewLocation)

        val scaleX = pdfView.scaleX
        val scaleY = pdfView.scaleY
        val anchorX = viewLocation[0] - parentLocation[0] + anchor.centerX() * scaleX
        val anchorTop = viewLocation[1] - parentLocation[1] + anchor.top * scaleY
        val anchorBottom = viewLocation[1] - parentLocation[1] + anchor.bottom * scaleY

        val margin = dp(8f)
        var x = anchorX - width / 2f
        x = x.coerceIn(margin, (parent.width - width - margin).coerceAtLeast(margin))
        var y = anchorTop - height - margin
        if (y < margin + dp(24f)) {
            // 上方空间不足，改到选区下方
            y = anchorBottom + margin
        }
        y = y.coerceIn(margin, (parent.height - height - margin).coerceAtLeast(margin))

        menu.x = x
        menu.y = y
    }

    private fun dp(value: Float): Float = ConvertUtils.dp2px(value).toFloat()

    //endregion
}
