package com.sigpher.xpdf.common.greendao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.greenrobot.greendao.database.Database;

/**
 * 数据库升级入口。
 * <p>
 * 约束：
 * <ul>
 *     <li>纯新增表/列的升级必须走增量 SQL（CREATE TABLE IF NOT EXISTS），不要重建表；</li>
 *     <li>只有结构发生破坏性变化时才使用 {@link MigrationHelper}，并务必把所有实体 DAO 传入，
 *     否则未传入的表会被丢弃（历史数据丢失）。</li>
 * </ul>
 * 注意：不要调用 super.onUpgrade，父类实现为 no-op，真正工作由这里完成。
 *
 * @author sigpher
 */
public class UpdateOpenHelper extends DaoMaster.OpenHelper {

    private static final String TAG = "DBUpgrade";

    /** 引入 ANNOTATION 表的 schema 版本 */
    private static final int SCHEMA_VERSION_ANNOTATION = 4;

    /** 引入 position / scaleFactor 的 schema 版本（之前的版本需要整表重建迁移） */
    private static final int SCHEMA_VERSION_LEGACY = 3;

    public UpdateOpenHelper(Context context, String name) {
        super(context, name);
    }

    public UpdateOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
        super(context, name, factory);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        Log.i(TAG, "upgrade from " + oldVersion + " to " + newVersion);
        long start = System.currentTimeMillis();

        if (oldVersion < SCHEMA_VERSION_LEGACY) {
            // 历史结构变更：整表重建并迁移数据，这里把全部实体 DAO 都带上，避免丢数据
            MigrationHelper.migrate(db, new MigrationHelper.ReCreateAllTableListener() {
                @Override
                public void onCreateAllTables(Database db, boolean ifNotExists) {
                    DaoMaster.createAllTables(db, ifNotExists);
                }

                @Override
                public void onDropAllTables(Database db, boolean ifExists) {
                    DaoMaster.dropAllTables(db, ifExists);
                }
            }, AnnotationDao.class, PDFDao.class, CollectionDao.class, RecentPDFDao.class);
        } else if (oldVersion < SCHEMA_VERSION_ANNOTATION) {
            // 3 -> 4：只新增标注表及其索引，纯增量，不重建表（重建会带来大量 IO 并清空 RECENT_PDF）
            AnnotationDao.createTable(db, true);
        }

        Log.i(TAG, "upgrade finished in " + (System.currentTimeMillis() - start) + "ms");
    }
}
