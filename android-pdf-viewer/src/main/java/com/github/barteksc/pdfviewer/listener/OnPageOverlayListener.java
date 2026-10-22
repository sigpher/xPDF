package com.github.barteksc.pdfviewer.listener;

import android.graphics.Canvas;

/**
 * 页面级叠加绘制回调。
 * <p>
 * 与 {@link OnDrawListener} 的区别：回调时 canvas 的原点已经平移到**该页左上角**，
 * 且无论横向还是纵向翻页模式都成立，因此调用方可以按 0..1 的页面相对坐标直接绘制。
 */
public interface OnPageOverlayListener {

    /**
     * @param canvas     已平移到页面左上角的画布（单位：当前缩放后的像素）
     * @param pageWidth  页面在当前缩放下的宽度（像素）
     * @param pageHeight 页面在当前缩放下的高度（像素）
     * @param page       从 0 开始的用户页码
     */
    void onPageOverlay(Canvas canvas, float pageWidth, float pageHeight, int page);
}
