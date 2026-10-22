package com.github.barteksc.pdfviewer.text;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本选区数据。
 * <p>
 * 矩形使用 pdfium 的页面 point 坐标（原点在页面左上角），绘制时由
 * {@link com.github.barteksc.pdfviewer.PDFView#pageRectToView(int, RectF)} 或页面级叠加回调换算。
 */
public class TextSelection {

    private final int page;
    private int startIndex;
    private int endIndex;
    private String text;
    private final List<RectF> rects = new ArrayList<>();
    private final int pageWidthPoint;
    private final int pageHeightPoint;

    public TextSelection(int page, int startIndex, int endIndex, String text, List<RectF> rects,
                         int pageWidthPoint, int pageHeightPoint) {
        this.page = page;
        this.pageWidthPoint = pageWidthPoint;
        this.pageHeightPoint = pageHeightPoint;
        update(startIndex, endIndex, text, rects);
    }

    public void update(int startIndex, int endIndex, String text, List<RectF> rects) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.text = text == null ? "" : text;
        this.rects.clear();
        if (rects != null) {
            this.rects.addAll(rects);
        }
    }

    public int getPage() {
        return page;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public String getText() {
        return text;
    }

    public List<RectF> getRects() {
        return rects;
    }

    public int getPageWidthPoint() {
        return pageWidthPoint;
    }

    public int getPageHeightPoint() {
        return pageHeightPoint;
    }

    public boolean isEmpty() {
        return rects.isEmpty() || text.isEmpty();
    }

    /** 选区首行（用于手柄与操作栏定位），无矩形时返回 null */
    public RectF getFirstRect() {
        RectF first = null;
        for (RectF rect : rects) {
            if (rect == null) {
                continue;
            }
            if (first == null) {
                first = rect;
                continue;
            }
            // 取最靠上一行的第一块
            if (rect.top < first.top - 1f) {
                first = rect;
            } else if (Math.abs(rect.top - first.top) <= 1f && rect.left < first.left) {
                first = rect;
            }
        }
        return first;
    }

    /** 选区末行（用于手柄定位），无矩形时返回 null */
    public RectF getLastRect() {
        RectF last = null;
        for (RectF rect : rects) {
            if (rect == null) {
                continue;
            }
            if (last == null) {
                last = rect;
                continue;
            }
            if (rect.bottom > last.bottom + 1f) {
                last = rect;
            } else if (Math.abs(rect.bottom - last.bottom) <= 1f && rect.right > last.right) {
                last = rect;
            }
        }
        return last;
    }
}
