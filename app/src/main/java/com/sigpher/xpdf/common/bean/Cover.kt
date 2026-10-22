package com.sigpher.xpdf.common.bean

/**
 * 分组封面的聚合结果（纯内存对象，不落库）。
 *
 * 字段保持 `@JvmField`：Java 侧（含 DataManager 与适配器）历来以字段形式访问 `name` / `coverList` / `count`。
 *
 * @author sigpher
 */
class Cover(
    @JvmField var name: String,
    @JvmField var coverList: List<String>,
    @JvmField var count: Int
) {

    override fun toString(): String {
        return "Cover{name='$name', coverList=$coverList, count=$count}"
    }
}
