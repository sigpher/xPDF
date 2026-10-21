package com.sigpher.xpdf.common.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import com.blankj.utilcode.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * PDF 相关工具：统一只读打开、及时关闭 fd，避免导入时重复打开文件。
 *
 * @author sigpher
 */
public final class PdfUtils {

    /**
     * 封面缩略图宽度（列表展示足够，避免整屏大图）
     */
    private static final int THUMB_WIDTH = 360;

    private static final int JPEG_QUALITY = 80;

    /**
     * 打开 PDF（调用方负责 close，PdfRenderer 关闭时会一并关闭 fd）
     */
    public static PdfRenderer openRenderer(String path) {
        if (StringUtils.isEmpty(path)) return null;
        ParcelFileDescriptor fd = null;
        try {
            fd = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(fd);
            fd = null; // 所有权移交给 PdfRenderer
            return renderer;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 渲染指定页为缩略图，背景填白（PDF 页面本身透明）
     */
    public static Bitmap renderPage(PdfRenderer renderer, int pageIndex) {
        if (renderer == null || pageIndex < 0 || pageIndex >= renderer.getPageCount()) return null;
        PdfRenderer.Page page = null;
        try {
            page = renderer.openPage(pageIndex);
            int width = Math.max(1, Math.min(THUMB_WIDTH, page.getWidth()));
            int height = Math.max(1, (int) (width * (page.getHeight() * 1.0f / page.getWidth())));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static Bitmap pdfToBitmap(String path, int curPage) {
        PdfRenderer renderer = openRenderer(path);
        try {
            return renderPage(renderer, curPage);
        } finally {
            closeQuietly(renderer);
        }
    }

    /**
     * 保存封面为 JPEG（列表只用于展示，不需要无损 PNG）
     */
    public static String saveBitmap(Bitmap bitmap, String savePath) {
        if (bitmap == null || StringUtils.isEmpty(savePath)) return null;
        File file = new File(savePath);
        if (file.exists()) {
            bitmap.recycle();
            return savePath;
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            bitmap.recycle();
        }
        return savePath;
    }

    public static int getPdfTotalPage(String path) {
        PdfRenderer renderer = openRenderer(path);
        try {
            return renderer == null ? 0 : renderer.getPageCount();
        } finally {
            closeQuietly(renderer);
        }
    }

    public static int getPdfWidth(String path, int page) {
        PdfRenderer renderer = openRenderer(path);
        PdfRenderer.Page pdfPage = null;
        try {
            if (renderer == null || page < 0 || page >= renderer.getPageCount()) return 0;
            pdfPage = renderer.openPage(page);
            return pdfPage.getWidth();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            closeQuietly(pdfPage);
            closeQuietly(renderer);
        }
    }

    public static int getPdfHeight(String path, int page) {
        PdfRenderer renderer = openRenderer(path);
        PdfRenderer.Page pdfPage = null;
        try {
            if (renderer == null || page < 0 || page >= renderer.getPageCount()) return 0;
            pdfPage = renderer.openPage(page);
            return pdfPage.getHeight();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            closeQuietly(pdfPage);
            closeQuietly(renderer);
        }
    }

    private static void closeQuietly(PdfRenderer renderer) {
        if (renderer != null) {
            try {
                renderer.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void closeQuietly(PdfRenderer.Page page) {
        if (page != null) {
            try {
                page.close();
            } catch (Exception ignored) {
            }
        }
    }

    private PdfUtils() {
    }
}
