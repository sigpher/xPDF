package com.sigpher.xpdf.common.event

/**
 * 分组内书籍顺序或内容发生变化后，通知「全部文档」列表刷新。
 *
 * 原先通过 LiveDataBus（反射改写 LiveData 私有字段）传递，现已统一到 EventBus 单通道。
 *
 * @author sigpher
 */
class UpdateAllEvent
