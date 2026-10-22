package com.sigpher.xpdf.common.event

/**
 * 旧版本导入的书架条目在新系统上已无法直接读取（分区存储），需要引导用户重新导入。
 *
 * @param count 失效条目数量
 * @author sigpher
 */
class LegacyMissingEvent(val count: Int)
