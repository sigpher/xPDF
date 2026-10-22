package com.github.barteksc.pdfviewer;

import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.legere.pdfiumandroid.PdfDocument;
import io.legere.pdfiumandroid.PdfPage;
import io.legere.pdfiumandroid.PdfTextPage;

/**
 * 文本页访问入口：负责打开/缓存 pdfium 文本页并暴露取词所需的最小能力。
 * <p>
 * 约定：
 * <ul>
 *     <li>所有方法都是 <b>同步</b> 的，pdfium 调用请放在单线程执行器中（见 TextSelectionManager）；</li>
 *     <li>内部按用户页缓存最近 {@link #MAX_CACHED_PAGES} 页的 {@link PdfPage}+{@link PdfTextPage}，
 *     淘汰时立即关闭，避免 native 句柄泄漏；</li>
 *     <li>{@link #recycle()} 由 PDFView 在 recycle 时调用，之后所有方法返回空值。</li>
 * </ul>
 * 该类为库内部使用，随 PDFView 的 pdfFile 生命周期创建与销毁。
 */
public class TextPageLoader {

    private static final String TAG = "TextPageLoader";

    /** 同时保持打开的文本页数量（选区只在当前页，3 页足够覆盖快速翻页） */
    private static final int MAX_CACHED_PAGES = 3;

    /** 长按命中字符的默认容差（page point） */
    public static final float DEFAULT_TOLERANCE = 4f;

    private final PdfFile pdfFile;

    private final LinkedHashMap<Integer, PageText> cache =
            new LinkedHashMap<Integer, PageText>(4, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, PageText> eldest) {
                    if (size() > MAX_CACHED_PAGES) {
                        eldest.getValue().close();
                        return true;
                    }
                    return false;
                }
            };

    private boolean recycled = false;

    TextPageLoader(PdfFile pdfFile) {
        this.pdfFile = pdfFile;
    }

    /** 单页文本缓存对象 */
    static class PageText {
        final PdfPage page;
        final PdfTextPage textPage;
        final String text;
        final int charCount;

        PageText(PdfPage page, PdfTextPage textPage, String text, int charCount) {
            this.page = page;
            this.textPage = textPage;
            this.text = text;
            this.charCount = charCount;
        }

        void close() {
            try {
                textPage.close();
            } catch (Exception e) {
                Log.w(TAG, "close text page failed", e);
            }
            try {
                page.close();
            } catch (Exception e) {
                Log.w(TAG, "close page failed", e);
            }
        }
    }

    public synchronized boolean isRecycled() {
        return recycled;
    }

    public synchronized void recycle() {
        recycled = true;
        for (PageText pageText : cache.values()) {
            pageText.close();
        }
        cache.clear();
    }

    /** 页面字符数；无文本层或页面不可用时返回 0 */
    public synchronized int getCharCount(int userPage) {
        PageText pageText = obtain(userPage);
        return pageText == null ? 0 : pageText.charCount;
    }

    /** 整页文本；无文本层时返回空串 */
    public synchronized String getPageText(int userPage) {
        PageText pageText = obtain(userPage);
        return pageText == null ? "" : pageText.text;
    }

    /** 页面宽度（PDF point） */
    public int getPagePointWidth(int userPage) {
        return pdfFile.getPagePointWidth(userPage);
    }

    /** 页面高度（PDF point） */
    public int getPagePointHeight(int userPage) {
        return pdfFile.getPagePointHeight(userPage);
    }

    /**
     * 命中页面坐标 (x, y) 处的字符索引；未命中返回 -1。
     *
     * @param x         页面 point 坐标
     * @param y         页面 point 坐标
     * @param tolerance 命中容差（point）
     */
    public synchronized int getCharIndexAt(int userPage, float x, float y, float tolerance) {
        PageText pageText = obtain(userPage);
        if (pageText == null || pageText.charCount <= 0) {
            return -1;
        }
        try {
            return pageText.textPage.textPageGetCharIndexAtPos(x, y, tolerance, tolerance);
        } catch (Exception e) {
            Log.w(TAG, "hit test failed, page=" + userPage, e);
            return -1;
        }
    }

    /** 取指定字符区间的文本；失败返回空串 */
    public synchronized String getText(int userPage, int start, int count) {
        PageText pageText = obtain(userPage);
        if (pageText == null || pageText.charCount <= 0) {
            return "";
        }
        int safeStart = clamp(start, 0, pageText.charCount);
        int safeCount = clamp(count, 0, pageText.charCount - safeStart);
        if (safeCount <= 0) {
            return "";
        }
        try {
            String text = pageText.textPage.textPageGetText(safeStart, safeCount);
            return text == null ? "" : text;
        } catch (Exception e) {
            Log.w(TAG, "get text failed, page=" + userPage, e);
            return "";
        }
    }

    /**
     * 取指定字符区间的行级矩形（页面 point 坐标）。
     * 使用 FPDFText_CountRects/GetRect 语义，索引相对本次区间，不依赖整页 rect 索引。
     */
    public synchronized List<RectF> getRangeRects(int userPage, int start, int count) {
        List<RectF> result = new ArrayList<>();
        PageText pageText = obtain(userPage);
        if (pageText == null || pageText.charCount <= 0) {
            return result;
        }
        int safeStart = clamp(start, 0, pageText.charCount);
        int safeCount = clamp(count, 0, pageText.charCount - safeStart);
        if (safeCount <= 0) {
            return result;
        }
        try {
            int rectCount = pageText.textPage.textPageCountRects(safeStart, safeCount);
            for (int i = 0; i < rectCount; i++) {
                RectF rect = pageText.textPage.textPageGetRect(i);
                if (rect != null && rect.width() > 0f && rect.height() > 0f) {
                    result.add(rect);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "get rects failed, page=" + userPage, e);
        }
        return result;
    }

    private PageText obtain(int userPage) {
        if (recycled) {
            return null;
        }
        PageText cached = cache.get(userPage);
        if (cached != null) {
            return cached;
        }
        PdfDocument document = pdfFile.getDocument();
        int documentPage = pdfFile.documentPage(userPage);
        if (document == null || documentPage < 0) {
            return null;
        }
        PdfPage page = null;
        PdfTextPage textPage = null;
        try {
            page = document.openPage(documentPage);
            if (page == null) {
                return null;
            }
            textPage = page.openTextPage();
            if (textPage == null) {
                page.close();
                return null;
            }
            int charCount = textPage.textPageCountChars();
            String text = "";
            if (charCount > 0) {
                String raw = textPage.textPageGetText(0, charCount);
                if (raw != null) {
                    text = raw;
                }
            }
            PageText pageText = new PageText(page, textPage, text, charCount);
            cache.put(userPage, pageText);
            return pageText;
        } catch (Exception e) {
            Log.w(TAG, "open text page failed, page=" + userPage, e);
            if (textPage != null) {
                try {
                    textPage.close();
                } catch (Exception ignored) {
                    // 忽略关闭异常
                }
            }
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                    // 忽略关闭异常
                }
            }
            return null;
        }
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
