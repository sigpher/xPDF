package com.sigpher.xpdf.common.event;

/**
 * 旧版本导入的书架条目在新系统上已无法直接读取（分区存储），需要引导用户重新导入。
 *
 * @author sigpher
 */
public class LegacyMissingEvent {

    public final int count;

    public LegacyMissingEvent(int count) {
        this.count = count;
    }
}
