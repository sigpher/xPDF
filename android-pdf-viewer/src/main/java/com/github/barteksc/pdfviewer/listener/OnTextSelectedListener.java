package com.github.barteksc.pdfviewer.listener;

import android.graphics.RectF;

import com.github.barteksc.pdfviewer.text.TextSelection;

/**
 * 文本选区回调。
 * <p>
 * 所有回调都在主线程触发，调用方可以直接更新 UI。
 */
public interface OnTextSelectedListener {

    /**
     * 长按完成取词，选区已建立。
     *
     * @param selection    选区数据（页面 point 坐标）
     * @param anchorInView 选区首行在 View 坐标系中的锚点，用于定位操作栏
     */
    void onTextSelected(TextSelection selection, RectF anchorInView);

    /**
     * 拖动选区手柄后选区发生变化。
     */
    void onTextSelectionChanged(TextSelection selection, RectF anchorInView);

    /**
     * 选区被清除（点击空白、翻页或主动清除）。
     */
    void onSelectionCleared();

    /**
     * 长按的页面没有文本层（扫描件、纯图片页）。
     */
    void onPageWithoutText(int page);
}
