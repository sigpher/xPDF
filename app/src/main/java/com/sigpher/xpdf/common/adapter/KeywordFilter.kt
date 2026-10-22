package com.sigpher.xpdf.common.adapter

import android.widget.Filter
import com.blankj.utilcode.util.StringUtils

/**
 * 关键字过滤器：支持「包含」与「反向包含」两种模式，结果通过 [onFilter] 回调给 Adapter。
 *
 * 关键字为空时返回空列表（与搜索页原有语义一致：未输入关键字不展示任何结果）。
 *
 * @author sigpher
 */
class KeywordFilter<T>(
        private val source: List<T>,
        private val keywordOf: (T) -> String
) : Filter() {

    /** 反向过滤：true 时只保留「不包含」关键字的项 */
    var inverse: Boolean = false

    /** 过滤完成后回调，参数为新结果列表 */
    var onFilter: ((List<T>) -> Unit)? = null

    /** 同步过滤一次，供需要立即拿到结果的场景复用（不切线程） */
    fun filterNow(keyword: CharSequence?): List<T> {
        val text = keyword?.toString().orEmpty()
        if (StringUtils.isEmpty(text)) {
            return emptyList()
        }
        return source.filter { keywordOf(it).contains(text) != inverse }
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        return FilterResults().apply { values = filterNow(constraint) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
        onFilter?.invoke(results?.values as? List<T> ?: emptyList())
    }
}
