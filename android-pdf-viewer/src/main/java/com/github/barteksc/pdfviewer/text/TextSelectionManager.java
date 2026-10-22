package com.github.barteksc.pdfviewer.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.TextPageLoader;
import com.github.barteksc.pdfviewer.listener.OnTextSelectedListener;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文本选区状态机：
 * <ol>
 *     <li>长按 → 后台线程打开文本页、命中字符、按词/句边界扩展；</li>
 *     <li>主线程建立选区并回调；</li>
 *     <li>拖动首尾手柄调整选区，点击空白或 {@link #clearSelection()} 退出。</li>
 * </ol>
 * 绘制在 {@link PDFView#onDraw(Canvas)} 的文档坐标系内完成（canvas 已平移 currentOffset）。
 */
public class TextSelectionManager {

    private static final String TAG = "TextSelectionMgr";

    private static final int HANDLE_NONE = 0;
    private static final int HANDLE_START = 1;
    private static final int HANDLE_END = 2;

    /** 手柄绘制半径（dp） */
    private static final float HANDLE_RADIUS_DP = 7f;
    /** 手柄触摸热区半径（dp），比绘制半径大，便于手指拖动 */
    private static final float HANDLE_TOUCH_DP = 24f;

    /** 扩词时 CJK 连续字符的最大选区长度，避免整段被选中 */
    private static final int CJK_MAX_LENGTH = 30;
    /** 扩词时拉丁文单词的最大选区长度，防止无空白文本被整页选中 */
    private static final int WORD_MAX_LENGTH = 60;

    private final PDFView pdfView;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** pdfium 调用串行化：库内其它 native 调用同样是"调用方保证线程"，这里统一收敛到单线程 */
    private final ThreadPoolExecutor executor;

    private final Paint selectionPaint = new Paint();
    private final Paint handlePaint = new Paint();
    private final Paint handleBorderPaint = new Paint();

    private TextPageLoader loader;
    private OnTextSelectedListener listener;

    private TextSelection selection;
    private int dragHandle = HANDLE_NONE;
    private int lastHitIndex = -1;
    private int requestToken = 0;
    private boolean enabled = false;
    private boolean recycled = false;

    private final float handleRadiusPx;
    private final float handleTouchPx;

    public TextSelectionManager(PDFView pdfView) {
        this.pdfView = pdfView;
        float density = pdfView.getResources().getDisplayMetrics().density;
        handleRadiusPx = HANDLE_RADIUS_DP * density;
        handleTouchPx = HANDLE_TOUCH_DP * density;

        selectionPaint.setAntiAlias(true);
        selectionPaint.setColor(0x553B82F6);
        selectionPaint.setStyle(Paint.Style.FILL);

        handlePaint.setAntiAlias(true);
        handlePaint.setColor(0xFF3B82F6);
        handlePaint.setStyle(Paint.Style.FILL);

        handleBorderPaint.setAntiAlias(true);
        handleBorderPaint.setColor(0xFFFFFFFF);
        handleBorderPaint.setStyle(Paint.Style.STROKE);
        handleBorderPaint.setStrokeWidth(Math.max(1f, 1.5f * density));

        final AtomicInteger threadIndex = new AtomicInteger();
        executor = new ThreadPoolExecutor(1, 1, 30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), r -> {
                    Thread thread = new Thread(r, "PDF text-" + threadIndex.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        executor.allowCoreThreadTimeOut(true);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clearSelection();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setListener(OnTextSelectedListener listener) {
        this.listener = listener;
    }

    /** 由 PDFView 在文档加载完成时注入 */
    public void setLoader(TextPageLoader loader) {
        this.loader = loader;
    }

    public TextSelection getSelection() {
        return selection;
    }

    public boolean isActive() {
        return selection != null;
    }

    /**
     * 长按入口。
     *
     * @return true 表示已消费该长按（即使页面无文本也返回 true，避免再触发旧的 longPress 回调）
     */
    public boolean onLongPress(final float viewX, final float viewY) {
        if (!enabled || recycled) {
            return false;
        }
        final TextPageLoader pageLoader = loader;
        if (pageLoader == null) {
            return false;
        }
        final int page = pdfView.getPageAt(viewX, viewY);
        if (page < 0) {
            return false;
        }
        // 页面外的长按（页间距、页面留白）不参与取词
        RectF bounds = pdfView.getPageBounds(page);
        if (bounds == null) {
            return false;
        }
        float docX = viewX + pdfView.getCurrentXOffset();
        float docY = viewY + pdfView.getCurrentYOffset();
        float slack = handleTouchPx;
        if (docX < bounds.left - slack || docX > bounds.right + slack
                || docY < bounds.top - slack || docY > bounds.bottom + slack) {
            return false;
        }
        final PointF pagePoint = pdfView.viewToPagePoint(page, viewX, viewY);
        if (pagePoint == null) {
            return false;
        }
        // 长按前先清理旧选区，避免状态叠加
        clearSelection();

        final int token = ++requestToken;
        executor.execute(() -> {
            if (pageLoader.isRecycled()) {
                return;
            }
            int charCount = pageLoader.getCharCount(page);
            if (charCount <= 0) {
                mainHandler.post(() -> {
                    if (token != requestToken || recycled) {
                        return;
                    }
                    if (listener != null) {
                        listener.onPageWithoutText(page);
                    }
                });
                return;
            }
            int index = pageLoader.getCharIndexAt(page, pagePoint.x, pagePoint.y,
                    TextPageLoader.DEFAULT_TOLERANCE);
            if (index < 0) {
                // 手指落在行间空隙时放宽容差重试一次
                index = pageLoader.getCharIndexAt(page, pagePoint.x, pagePoint.y,
                        TextPageLoader.DEFAULT_TOLERANCE * 4f);
            }
            if (index < 0 || index >= charCount) {
                return;
            }
            final int hitIndex = index;
            String pageText = pageLoader.getPageText(page);
            int[] range = expandWord(pageText, hitIndex, charCount);
            final TextSelection newSelection = buildSelection(pageLoader, page, range[0], range[1]);
            if (newSelection == null || newSelection.isEmpty()) {
                return;
            }
            mainHandler.post(() -> {
                if (token != requestToken || recycled) {
                    return;
                }
                selection = newSelection;
                lastHitIndex = hitIndex;
                pdfView.invalidate();
                if (listener != null) {
                    listener.onTextSelected(selection, anchorOf(selection));
                }
            });
        });
        return true;
    }

    /** 由 DragPinchManager 在分发手势前调用；返回 true 表示事件已被选区消费 */
    public boolean onTouchEvent(MotionEvent event) {
        if (selection == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                RectF startHandle = startHandleRect();
                RectF endHandle = endHandleRect();
                if (startHandle != null
                        && distance(startHandle.left, startHandle.bottom, event.getX(), event.getY()) <= handleTouchPx) {
                    dragHandle = HANDLE_START;
                    lastHitIndex = selection.getStartIndex();
                    return true;
                }
                if (endHandle != null
                        && distance(endHandle.right, endHandle.bottom, event.getX(), event.getY()) <= handleTouchPx) {
                    dragHandle = HANDLE_END;
                    lastHitIndex = selection.getEndIndex();
                    return true;
                }
                if (containsSelection(event.getX(), event.getY())) {
                    // 点在选择区域内：保持选区，交给上层重新弹出操作栏
                    if (listener != null) {
                        listener.onTextSelectionChanged(selection, anchorOf(selection));
                    }
                    return true;
                }
                clearSelection();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (dragHandle == HANDLE_NONE) {
                    return true;
                }
                updateSelectionByTouch(event.getX(), event.getY(), dragHandle);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                boolean wasDragging = dragHandle != HANDLE_NONE;
                dragHandle = HANDLE_NONE;
                if (wasDragging && selection != null && listener != null) {
                    listener.onTextSelectionChanged(selection, anchorOf(selection));
                }
                return true;
            }
            default:
                return true;
        }
    }

    /** 在 PDFView.onDraw 的文档坐标系中绘制选区与手柄 */
    public void draw(Canvas canvas) {
        TextSelection current = selection;
        if (current == null || current.isEmpty()) {
            return;
        }
        RectF bounds = pdfView.getPageBounds(current.getPage());
        if (bounds == null || bounds.width() <= 0f || bounds.height() <= 0f) {
            return;
        }
        int pointWidth = current.getPageWidthPoint();
        int pointHeight = current.getPageHeightPoint();
        if (pointWidth <= 0 || pointHeight <= 0) {
            return;
        }
        float scaleX = bounds.width() / pointWidth;
        float scaleY = bounds.height() / pointHeight;

        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(scaleX, scaleY);
        for (RectF rect : current.getRects()) {
            if (rect != null) {
                canvas.drawRect(rect, selectionPaint);
            }
        }
        canvas.restore();

        RectF first = current.getFirstRect();
        RectF last = current.getLastRect();
        if (first != null) {
            float cx = bounds.left + first.left * scaleX;
            float cy = bounds.top + first.bottom * scaleY;
            canvas.drawCircle(cx, cy, handleRadiusPx, handlePaint);
            canvas.drawCircle(cx, cy, handleRadiusPx, handleBorderPaint);
        }
        if (last != null) {
            float cx = bounds.left + last.right * scaleX;
            float cy = bounds.top + last.bottom * scaleY;
            canvas.drawCircle(cx, cy, handleRadiusPx, handlePaint);
            canvas.drawCircle(cx, cy, handleRadiusPx, handleBorderPaint);
        }
    }

    public void clearSelection() {
        boolean hadSelection = selection != null;
        selection = null;
        dragHandle = HANDLE_NONE;
        lastHitIndex = -1;
        requestToken++;
        if (hadSelection) {
            pdfView.invalidate();
            if (listener != null) {
                listener.onSelectionCleared();
            }
        }
    }

    /** 文档被回收（PDFView.recycle）时调用：清理选区并关闭文本页句柄，之后可再次加载文档 */
    public void onDocumentClosed() {
        selection = null;
        dragHandle = HANDLE_NONE;
        lastHitIndex = -1;
        requestToken++;
        TextPageLoader current = loader;
        loader = null;
        if (current != null) {
            current.recycle();
        }
    }

    /** 视图销毁时调用 */
    public void recycle() {
        recycled = true;
        onDocumentClosed();
        listener = null;
    }

    //region 内部实现

    private TextSelection buildSelection(TextPageLoader pageLoader, int page, int start, int end) {
        if (end <= start) {
            return null;
        }
        List<RectF> rects = pageLoader.getRangeRects(page, start, end - start);
        if (rects.isEmpty()) {
            return null;
        }
        String text = pageLoader.getText(page, start, end - start);
        int pointWidth = pageLoader.getPagePointWidth(page);
        int pointHeight = pageLoader.getPagePointHeight(page);
        return new TextSelection(page, start, end, text, rects, pointWidth, pointHeight);
    }

    private void updateSelectionByTouch(float viewX, float viewY, int handle) {
        TextSelection current = selection;
        TextPageLoader pageLoader = loader;
        if (current == null || pageLoader == null) {
            return;
        }
        int page = current.getPage();
        PointF pagePoint = pdfView.viewToPagePoint(page, viewX, viewY);
        if (pagePoint == null) {
            return;
        }
        int charCount = pageLoader.getCharCount(page);
        if (charCount <= 0) {
            return;
        }
        int index = pageLoader.getCharIndexAt(page, pagePoint.x, pagePoint.y,
                TextPageLoader.DEFAULT_TOLERANCE * 2f);
        if (index < 0 || index == lastHitIndex) {
            return;
        }
        lastHitIndex = index;
        int start;
        int end;
        if (handle == HANDLE_START) {
            start = Math.min(index, current.getEndIndex() - 1);
            end = current.getEndIndex();
        } else {
            start = current.getStartIndex();
            end = Math.max(index + 1, start + 1);
        }
        start = clamp(start, 0, charCount - 1);
        end = clamp(end, start + 1, charCount);
        List<RectF> rects = pageLoader.getRangeRects(page, start, end - start);
        if (rects.isEmpty()) {
            return;
        }
        String text = pageLoader.getText(page, start, end - start);
        current.update(start, end, text, rects);
        pdfView.invalidate();
        if (listener != null) {
            listener.onTextSelectionChanged(current, anchorOf(current));
        }
    }

    /** 选区首行顶部中点（View 坐标），供操作栏定位 */
    private RectF anchorOf(TextSelection current) {
        RectF first = current.getFirstRect();
        if (first == null) {
            return new RectF();
        }
        RectF viewRect = pdfView.pageRectToView(current.getPage(), first);
        if (viewRect == null) {
            return new RectF();
        }
        float x = (viewRect.left + viewRect.right) / 2f;
        return new RectF(x, viewRect.top, x, viewRect.bottom);
    }

    private RectF startHandleRect() {
        TextSelection current = selection;
        if (current == null) {
            return null;
        }
        RectF first = current.getFirstRect();
        return first == null ? null : pdfView.pageRectToView(current.getPage(), first);
    }

    private RectF endHandleRect() {
        TextSelection current = selection;
        if (current == null) {
            return null;
        }
        RectF last = current.getLastRect();
        return last == null ? null : pdfView.pageRectToView(current.getPage(), last);
    }

    private boolean containsSelection(float x, float y) {
        TextSelection current = selection;
        if (current == null) {
            return false;
        }
        for (RectF rect : current.getRects()) {
            if (rect == null) {
                continue;
            }
            RectF viewRect = pdfView.pageRectToView(current.getPage(), rect);
            if (viewRect != null && viewRect.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private static float distance(float hx, float hy, float x, float y) {
        return (float) Math.hypot(x - hx, y - hy);
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

    /**
     * 按语义边界扩展选区：
     * <ul>
     *     <li>CJK：向两侧扩展到非 CJK 字符为止，最长 {@link #CJK_MAX_LENGTH} 字；</li>
     *     <li>拉丁文：扩展到空白或标点边界，最长 {@link #WORD_MAX_LENGTH} 字符。</li>
     * </ul>
     */
    static int[] expandWord(String pageText, int index, int charCount) {
        int length = pageText == null ? 0 : pageText.length();
        int safeIndex = clamp(index, 0, Math.max(0, charCount - 1));
        if (length <= 0 || safeIndex >= length) {
            return new int[]{safeIndex, Math.min(safeIndex + 1, Math.max(charCount, safeIndex + 1))};
        }
        char target = pageText.charAt(safeIndex);
        if (isCjk(target)) {
            int start = safeIndex;
            while (start > 0 && isCjk(pageText.charAt(start - 1))) {
                start--;
            }
            int end = safeIndex + 1;
            while (end < length && isCjk(pageText.charAt(end))) {
                end++;
            }
            if (end - start > CJK_MAX_LENGTH) {
                int half = CJK_MAX_LENGTH / 2;
                start = Math.max(0, safeIndex - half);
                end = Math.min(length, start + CJK_MAX_LENGTH);
            }
            return new int[]{start, end};
        }
        if (Character.isWhitespace(target) || isBoundary(target)) {
            return new int[]{safeIndex, Math.min(safeIndex + 1, length)};
        }
        int start = safeIndex;
        while (start > 0 && !isSeparator(pageText.charAt(start - 1))) {
            start--;
        }
        int end = safeIndex + 1;
        while (end < length && !isSeparator(pageText.charAt(end))) {
            end++;
        }
        if (end - start > WORD_MAX_LENGTH) {
            end = Math.min(start + WORD_MAX_LENGTH, length);
        }
        return new int[]{start, end};
    }

    private static boolean isSeparator(char c) {
        return Character.isWhitespace(c) || isBoundary(c);
    }

    private static boolean isBoundary(char c) {
        switch (c) {
            case ',':
            case '.':
            case ';':
            case ':':
            case '!':
            case '?':
            case '"':
            case '\'':
            case '(':
            case ')':
            case '[':
            case ']':
            case '{':
            case '}':
            case '<':
            case '>':
            case '、':
            case '。':
            case '，':
            case '；':
            case '：':
            case '！':
            case '？':
            case '“':
            case '”':
            case '‘':
            case '’':
            case '（':
            case '）':
            case '《':
            case '》':
            case '【':
            case '】':
            case '·':
            case '—':
            case '…':
                return true;
            default:
                return false;
        }
    }

    private static boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)
                || (c >= 0x3400 && c <= 0x4DBF)
                || (c >= 0x3040 && c <= 0x30FF)
                || (c >= 0xAC00 && c <= 0xD7AF)
                || (c >= 0xF900 && c <= 0xFAFF);
    }

    //endregion
}
