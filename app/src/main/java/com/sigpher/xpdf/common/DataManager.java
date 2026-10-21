package com.sigpher.xpdf.common;

import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ThreadUtils.SimpleTask;
import com.sigpher.xpdf.common.bean.Collection;
import com.sigpher.xpdf.common.bean.Cover;
import com.sigpher.xpdf.common.bean.PDF;
import com.sigpher.xpdf.common.event.AllEvent;
import com.sigpher.xpdf.common.event.LegacyMissingEvent;
import com.sigpher.xpdf.common.event.RecentPDFEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 书库内存缓存。
 * <p>
 * 所有列表都以"就地更新"的方式维护（clear + addAll），页面持有的列表引用因此始终有效；
 * 数据库读取统一在 IO 线程完成，主线程只读缓存。
 *
 * @author sigpher
 */
public final class DataManager {

    private static final List<String> pathList = new ArrayList<>();
    private static final List<PDF> pdfList = new ArrayList<>();
    private static final List<PDF> recentPdfList = new ArrayList<>();
    private static final List<Collection> collectionList = new ArrayList<>();
    private static final List<Cover> coverList = new ArrayList<>();

    /**
     * 目录 -> 该目录下的 PDF（按 position 倒序），避免每次按目录筛选都全表遍历
     */
    private static final Map<String, List<PDF>> dirIndex = new HashMap<>();

    /**
     * 异步加载书库数据：Application 启动时调用，避免冷启动在主线程同步读库。
     * 加载完成后通过 EventBus 通知书库刷新，页面此时尚未创建也不会丢数据。
     */
    public static void initAsync() {
        ThreadUtils.executeByIo(new SimpleTask<Integer>() {
            @Override
            public Integer doInBackground() {
                reloadAllFromDb();
                return countUnreadablePdf();
            }

            @Override
            public void onSuccess(Integer missing) {
                EventBus.getDefault().post(new AllEvent());
                EventBus.getDefault().post(new RecentPDFEvent());
                if (missing > 0) {
                    EventBus.getDefault().post(new LegacyMissingEvent(missing));
                }
            }
        });
    }

    private static void reloadAllFromDb() {
        List<PDF> pdfs = DBHelper.queryAllPDF();
        List<PDF> recents = DBHelper.queryRecentPDF();
        List<Collection> collections = DBHelper.queryAllCollection();
        synchronized (DataManager.class) {
            pdfList.clear();
            pdfList.addAll(pdfs);
            recentPdfList.clear();
            recentPdfList.addAll(recents);
            collectionList.clear();
            collectionList.addAll(collections);
            updatePathList();
            rebuildDirIndex();
            updateCoverList();
        }
    }

    /**
     * 统计旧版本遗留、在当前系统上已无法读取的条目数（分区存储上线后外部路径不再可访问）。
     * 只在 IO 线程调用。
     */
    private static int countUnreadablePdf() {
        int count = 0;
        for (PDF pdf : pdfList) {
            String path = pdf.getPath();
            if (path == null || !new java.io.File(path).canRead()) {
                count++;
            }
        }
        return count;
    }

    public static void updateAll() {
        synchronized (DataManager.class) {
            collectionList.clear();
            collectionList.addAll(DBHelper.queryAllCollection());
            recentPdfList.clear();
            recentPdfList.addAll(DBHelper.queryRecentPDF());
            pdfList.clear();
            pdfList.addAll(DBHelper.queryAllPDF());
            updatePathList();
            rebuildDirIndex();
            updateCoverList();
        }
    }

    public static void updatePDFs() {
        synchronized (DataManager.class) {
            pdfList.clear();
            pdfList.addAll(DBHelper.queryAllPDF());
            updatePathList();
            rebuildDirIndex();
            updateCoverList();
        }
    }

    /** 重建"目录 -> PDF"索引；pdfList 变化后必须调用，否则封面与分组列表会读到旧数据 */
    private static void rebuildDirIndex() {
        dirIndex.clear();
        for (PDF pdf : pdfList) {
            String dir = pdf.getDir();
            if (dir == null) {
                continue;
            }
            List<PDF> group = dirIndex.get(dir);
            if (group == null) {
                group = new ArrayList<>();
                dirIndex.put(dir, group);
            }
            group.add(pdf);
        }
        Comparator<PDF> desc = (o1, o2) -> o2.getPosition() - o1.getPosition();
        for (List<PDF> group : dirIndex.values()) {
            Collections.sort(group, desc);
        }
    }

    public static void updateRecentPDFs() {
        List<PDF> recents = DBHelper.queryRecentPDF();
        synchronized (DataManager.class) {
            recentPdfList.clear();
            recentPdfList.addAll(recents);
        }
    }

    public static void updateCollection() {
        synchronized (DataManager.class) {
            collectionList.clear();
            collectionList.addAll(DBHelper.queryAllCollection());
            updateCoverList();
        }
    }

    private static void updateCoverList() {
        coverList.clear();
        for (Collection c : collectionList) {
            List<PDF> temp = dirIndex.get(c.getName());
            if (temp == null || temp.isEmpty()) {
                continue;
            }
            // 每个分组最多取前 4 个 PDF 封面，索引内已按 position 倒序
            List<PDF> list = temp.subList(0, Math.min(temp.size(), 4));
            List<String> coverPaths = new ArrayList<>(list.size());
            for (PDF pdf : list) {
                coverPaths.add(pdf.getCover());
            }
            coverList.add(new Cover(c.getName(), coverPaths, temp.size()));
        }
    }

    private static void updatePathList() {
        pathList.clear();
        for (PDF pdf : pdfList) {
            pathList.add(pdf.getPath());
        }
    }

    public static List<PDF> getRecentPdfList() {
        return recentPdfList;
    }

    public static List<String> getPathList() {
        return pathList;
    }

    public static List<PDF> getPdfList() {
        return pdfList;
    }

    /**
     * 取某目录下的 PDF（已按 position 倒序）。
     * 返回副本，调用方可自由修改，不会破坏内部索引。
     */
    public static List<PDF> getPdfList(String name) {
        synchronized (DataManager.class) {
            List<PDF> group = name == null ? null : dirIndex.get(name);
            if (group == null || group.isEmpty()) {
                return new ArrayList<>();
            }
            return new ArrayList<>(group);
        }
    }

    public static List<Collection> getCollectionList() {
        return collectionList;
    }

    public static List<Cover> getCoverList() {
        return coverList;
    }

    private DataManager() {
    }
}
