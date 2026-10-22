package com.sigpher.xpdf.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.App
import com.sigpher.xpdf.common.CommonFragment
import com.sigpher.xpdf.preview.ContentHolder.IconTreeItem
import io.legere.pdfiumandroid.api.Bookmark as PdfBookmark
import com.unnamed.b.atv.model.TreeNode
import com.unnamed.b.atv.view.AndroidTreeView
import java.util.*
import com.sigpher.xpdf.databinding.AppFragmentContentBinding

/**
 * @author sigpher
 */
class ContentFragment : CommonFragment(), IContentFragInterface {

    private val contentList: MutableList<PdfBookmark> = ArrayList()

    private var _binding: AppFragmentContentBinding? = null

    private val binding get() = _binding!!

    override fun update(collection: MutableCollection<PdfBookmark>) {
        contentList.clear()
        contentList.addAll(collection)
        // 视图尚未就绪时只缓存数据，等 onViewCreated 后再渲染
        initContent()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AppFragmentContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appRecyclerItemEmptyview.appItvPlaceholder.apply {
            setText(R.string.app_have_no_content)
            setTextColor(App.getContext().resources.getColor(R.color.app_content_bookmark_primary))
            setIconTop(R.drawable.app_ic_action_content_white)
            alpha = 0.6f
        }
        // 数据可能早于视图到达，这里补一次渲染
        initContent()
    }

    override fun onDestroyView() {
        // 先摘掉树，避免视图销毁后仍被 AndroidTreeView 持有
        _binding?.appLl?.removeAllViews()
        _binding = null
        super.onDestroyView()
    }

    private fun initContent() {
        val b = _binding ?: return
        // 每次刷新先清空容器，否则重复调用 update() 会把整棵树叠加在一起
        b.appLl.removeAllViews()
        if (contentList.isEmpty()) {
            b.appRecyclerItemEmptyview.appItvPlaceholder.visibility = View.VISIBLE
            return
        }
        b.appRecyclerItemEmptyview.appItvPlaceholder.visibility = View.GONE

        // 所有书签共用一棵树、一个 AndroidTreeView
        val root = TreeNode.root()
        for (bk in contentList) {
            root.addChild(buildNode(bk))
        }
        val treeView = AndroidTreeView(activity, root).apply {
            // 关闭默认动画
            setDefaultAnimation(false)
            // 默认样式与 ViewHolder
            setDefaultContainerStyle(R.style.AppTreeNode)
            setDefaultViewHolder(ContentHolder::class.java)
        }
        b.appLl.addView(treeView.view)
    }

    /**
     * 递归构建单个书签节点，顶层与子节点共用同一套 ViewHolder 绑定逻辑
     */
    private fun buildNode(bk: PdfBookmark): TreeNode {
        val hasChildren = bk.children.isNotEmpty()
        val icon = if (hasChildren) R.drawable.app_ic_can_down_white else R.drawable.app_ic_cannot_down_white
        val holder = ContentHolder(activity)
        holder.setOnIconTapListener(object : ContentHolder.OnIconTapListener {
            override fun onIconTap(node: TreeNode) {
                holder.treeView.toggleNode(node)
            }
        })
        holder.setOnNodeTapListener(object : ContentHolder.OnNodeTapListener {
            override fun onNodeTap(pageId: Int) {
                (activity as IActivityInterface).onJumpTo(pageId)
            }
        })
        val pageId = (bk.pageIdx + 1).toString()
        val node = TreeNode(IconTreeItem(icon, bk.title.orEmpty(), pageId)).setViewHolder(holder)
        if (hasChildren) {
            for (child in bk.children) {
                node.addChild(buildNode(child))
            }
        }
        return node
    }

    companion object {
        @JvmStatic
        fun newInstance(): Fragment {
            return ContentFragment()
        }
    }
}
