package com.sigpher.xpdf.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.sigpher.xpdf.common.BaseHolder
import com.sigpher.xpdf.databinding.AppRecyclerItemEmptyviewBinding

/**
 * 书库列表统一基类：多选状态、payload 局部刷新、空视图与拖拽排序都在这里收口。
 *
 * 多选状态以「数据对象」为 key（而不是 position），这样拖拽排序、局部刷新后选中态不会错位。
 *
 * @author sigpher
 */
abstract class BaseListAdapter<T, K : BaseViewHolder>(
        @LayoutRes private val itemLayoutId: Int,
        data: MutableList<T>
) : BaseItemDraggableAdapter<T, K>(itemLayoutId, data) {

    /**
     * 自己创建 ViewHolder，而不走 BRVAH 的默认实现。
     *
     * BRVAH 默认通过反射读取子类的泛型参数 K 来实例化 ViewHolder，R8 把本基类合并优化掉之后
     * 反射失败，会退化成通用的 BaseViewHolder，`convert(helper: XxxHolder, ...)` 里的强制转换
     * 随即抛 ClassCastException（导入书籍后一进列表就崩）。
     */
    protected abstract fun createHolder(itemView: View): K

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): K =
            createHolder(LayoutInflater.from(parent.context).inflate(itemLayoutId, parent, false))

    /** 多选框资源 id，子类布局不同需各自提供 */
    abstract val checkBoxId: Int

    /** 空视图文案 */
    protected abstract val emptyText: CharSequence

    /** 空视图图标 */
    protected abstract val emptyIcon: Int

    private val checkArray: MutableMap<T, Boolean> = HashMap()

    /** 空视图的 binding：BaseQuickAdapter.getEmptyView() 返回的是内部 FrameLayout 容器，不能直接 bind */
    private var emptyBinding: AppRecyclerItemEmptyviewBinding? = null

    var isSelectMode = false
        protected set

    override fun bindToRecyclerView(recyclerView: RecyclerView?) {
        super.bindToRecyclerView(recyclerView)
        recyclerView ?: return
        val binding = AppRecyclerItemEmptyviewBinding.inflate(
                LayoutInflater.from(recyclerView.context), recyclerView, false)
        emptyBinding = binding
        setEmptyView(binding.root)
    }

    override fun onBindViewHolder(holder: K, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder is BaseHolder) {
            handleCheckBox(holder, holder.checkBox, position)
        } else {
            emptyBinding?.let {
                it.appItvPlaceholder.text = emptyText
                it.appItvPlaceholder.setIconTop(emptyIcon)
            }
        }
    }

    override fun onBindViewHolder(holder: K, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // payload 刷新只更新选中态，避免整项重绘导致封面闪烁
            (holder as? BaseHolder)?.run {
                handleCheckBox(holder, checkBox, position)
            }
        }
    }

    override fun setOnItemClick(v: View, position: Int) {
        super.setOnItemClick(v, position)
        if (isContentType(position)) {
            val cb = v.findViewById<CheckBox>(checkBoxId)
            val isChecked = !cb.isChecked
            cb.isChecked = isChecked
            checkArray[data[position]] = isChecked
        }
    }

    override fun setOnItemLongClick(v: View, position: Int): Boolean {
        val consume = super.setOnItemLongClick(v, position)
        if (isContentType(position)) {
            enterSelectMode(v, position)
            return consume
        }
        return false
    }

    fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            for (index in 0 until itemCount) {
                checkArray[data[index]] = true
            }
        } else {
            checkArray.clear()
        }
        notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECT)
    }

    fun enterSelectMode(v: View, position: Int) {
        if (isContentType(position)) {
            isSelectMode = true
            checkArray[data[position]] = true
            v.findViewById<CheckBox>(checkBoxId).isChecked = true
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECT)
        }
    }

    fun exitSelectMode() {
        isSelectMode = false
        checkArray.clear()
        notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECT)
    }

    fun isChecked(position: Int): Boolean = checkArray[data[position]] ?: false

    protected open fun handleCheckBox(holder: K, cb: CheckBox, position: Int) {
        cb.visibility = if (isSelectMode) View.VISIBLE else View.GONE
        if (isSelectMode) {
            cb.alpha = 1.0f
            cb.scaleX = 0.8f
            cb.scaleY = 0.8f
            cb.isChecked = checkArray[data[position]] ?: false
        }
    }

    private fun isContentType(position: Int): Boolean {
        return position in data.indices && getItemViewType(position) == ItemTypes.TYPE_NORMAL
    }

    companion object {
        /** 仅刷新选中态的 payload，避免整项重建 */
        const val PAYLOAD_SELECT = 1001
    }
}
