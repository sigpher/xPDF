package com.sigpher.xpdf.preview

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sigpher.base.base.BaseFragment
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.CommonFragment
import com.blankj.utilcode.util.LogUtils
import java.util.*
import com.sigpher.xpdf.databinding.AppFragmentBookmarkBinding

/**
 * @author sigpher
 */
class BookmarkFragment : CommonFragment(), IBkFragInterface {

    private var mAdapter: RecyclerView.Adapter<*>? = null
    private val mBookmarks: MutableList<Bookmark> = ArrayList()

    override fun update(collection: MutableCollection<Bookmark>) {
        mBookmarks.clear()
        mBookmarks.addAll(collection)
        mBookmarks.sortWith(Comparator { bk1: Bookmark, bk2: Bookmark -> (bk2.time - bk1.time).toInt() })
        mAdapter?.notifyDataSetChanged()
    }

    private var _binding: AppFragmentBookmarkBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AppFragmentBookmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        val lm = LinearLayoutManager(activity)
        binding.appRvBookmark.layoutManager = lm
        val decoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(resources.getColor(R.color.base_black_divider)))
        binding.appRvBookmark.addItemDecoration(decoration)
        mAdapter = BookmarkAdapter(mBookmarks)
        binding.appRvBookmark.adapter = mAdapter
    }

    companion object {
        @JvmStatic
        fun newInstance(): Fragment {
            return BookmarkFragment()
        }
    }
}