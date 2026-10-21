package com.sigpher.xpdf.common;

import com.sigpher.xpdf.common.bean.Collection;
import com.sigpher.xpdf.common.bean.Cover;
import com.sigpher.xpdf.common.bean.PDF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sigpher
 */
public final class DataManager {

    private static List<String> pathList;
    private static List<PDF> pdfList;
    private static List<PDF> recentPdfList;
    private static List<Collection> collectionList;

    private static List<Cover> coverList = new ArrayList<>();
    /** 目录 -> 该目录下的 PDF（按 position 倒序），避免每次按目录筛选都全表遍历 */
    private static final Map<String, List<PDF>> dirIndex = new HashMap<>();

    static void init() {
        pdfList = DBHelper.queryAllPDF();
        recentPdfList = DBHelper.queryRecentPDF();
        collectionList = DBHelper.queryAllCollection();
        updatePathList();
        rebuildDirIndex();
        updateCoverList();
    }

    public static void updateAll() {
        DataManager.collectionList.clear();
        DataManager.collectionList.addAll(DBHelper.queryAllCollection());
        DataManager.recentPdfList.clear();
        DataManager.recentPdfList.addAll(DBHelper.queryRecentPDF());
        DataManager.pdfList.clear();
        DataManager.pdfList.addAll(DBHelper.queryAllPDF());
        updatePathList();
        rebuildDirIndex();
        updateCoverList();
    }

    public static void updatePDFs() {
        DataManager.pdfList.clear();
        DataManager.pdfList.addAll(DBHelper.queryAllPDF());
        updatePathList();
        rebuildDirIndex();
        updateCoverList();
    }

    /** 重建"目录 -> PDF"索引；pdfList 变化后必须调用，否则封面与分组列表会读到旧数据 */
    private static void rebuildDirIndex() {
        dirIndex.clear();
        if (pdfList == null) {
            return;
        }
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
        DataManager.recentPdfList.clear();
        DataManager.recentPdfList.addAll(DBHelper.queryRecentPDF());
    }

    public static void updateCollection() {
        DataManager.collectionList.clear();
        DataManager.collectionList.addAll(DBHelper.queryAllCollection());
        updateCoverList();
    }

    private static void updateCoverList() {
        DataManager.coverList.clear();
        for (Collection c : DataManager.collectionList) {
            List<PDF> temp = dirIndex.get(c.getName());
            if (temp == null || temp.isEmpty()) {
                continue;
            }
            // 每个分组最多取前 4 个 PDF 封面，索引内已按 position 倒序
            List<PDF> list = temp.subList(0, Math.min(temp.size(), 4));
            String name = c.getName();
            List<String> coverList = new ArrayList<>(list.size());
            for (PDF pdf : list) {
                coverList.add(pdf.getCover());
            }
            int count = temp.size();
            Cover cover = new Cover(name, coverList, count);
            DataManager.coverList.add(cover);
        }
    }

    private static void updatePathList() {
        if (pathList == null) {
            pathList = new ArrayList<>();
        }
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
        List<PDF> group = name == null ? null : dirIndex.get(name);
        if (group == null || group.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(group);
    }

    public static List<Collection> getCollectionList() {
        return collectionList;
    }

    public static List<Cover> getCoverList() {
        return coverList;
    }

    private DataManager() {}
}
