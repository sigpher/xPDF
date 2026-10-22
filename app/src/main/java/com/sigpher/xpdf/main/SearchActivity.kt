package com.sigpher.xpdf.main

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.CommonActivity
import com.sigpher.xpdf.common.DataManager
import com.sigpher.xpdf.common.Settings
import com.sigpher.xpdf.common.XGridDecoration
import com.sigpher.xpdf.common.event.RecentPDFEvent
import com.sigpher.xpdf.common.utils.TextWatcherImpl
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.github.anzewei.parallaxbacklayout.ParallaxBack
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.sigpher.xpdf.databinding.AppActivitySearchBinding

@ParallaxBack
class SearchActivity : CommonActivity() {

    private var adapter: SearchAdapter? = null

    private val binding by lazy { AppActivitySearchBinding.inflate(layoutInflater) }

    override fun bindContentView() = binding.root

    override fun layoutId(): Int {
        return R.layout.app_activity_search
    }

    override fun createToolbar(): Toolbar? {
        return findViewById(R.id.app_toolbar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        initToolbar()
        initView()
    }

    override fun onStop() {
        super.onStop()
        if (KeyboardUtils.isSoftInputVisible(this)) {
            KeyboardUtils.hideSoftInput(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRecentPDFEvent(event: RecentPDFEvent) {
        adapter?.update()
        // 实时更新最新阅读列表
        if (event.isFromPreviewActivity) { // 由 PreviewActivity 发出而接收
            adapter?.filter?.filter(binding.appSearchView.appEtSearch.text)
        }
    }

    private fun initView() {
        val pdfList = DataManager.getPdfList()
        binding.appSearchView.appIbtnBack.setOnClickListener { finish() }
        binding.appSearchView.appIbtnInverse.setOnClickListener {
            if (KeyboardUtils.isSoftInputVisible(this)) {
                KeyboardUtils.hideSoftInput(this)
            }
            binding.appSearchView.appIbtnInverse.isSelected = !binding.appSearchView.appIbtnInverse.isSelected
            adapter?.setInverse(binding.appSearchView.appIbtnInverse.isSelected)
            adapter?.filter?.filter(binding.appSearchView.appEtSearch.text)
        }
        binding.appSearchView.appIbtnClear.setOnClickListener {
            binding.appSearchView.appEtSearch.setText("")
            if (!KeyboardUtils.isSoftInputVisible(this)) {
                KeyboardUtils.showSoftInput(this)
            }
        }
        binding.appSearchView.appEtSearch.addTextChangedListener(object : TextWatcherImpl() {
            override fun onTextChanged(c: CharSequence, i: Int, i1: Int, i2: Int) {
                binding.appSearchView.appIbtnInverse.visibility = if (c.isEmpty()) View.GONE else View.VISIBLE
                binding.appSearchView.appIbtnClear.visibility = if (c.isEmpty()) View.GONE else View.VISIBLE
                adapter?.filter?.filter(c)
            }
        })
        binding.appRv.addItemDecoration(XGridDecoration())
        binding.appRv.addItemDecoration(YItemDecoration())
        val lm = GridLayoutManager(this, 3)
        lm.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter?.isEmpty == true
                        || position == 0
                        || Settings.linearLayout) {
                    3
                } else 1
            }
        }
        binding.appRv.layoutManager = lm
        adapter = SearchAdapter(pdfList, false)
        binding.appRv.adapter = adapter
    }

    private fun initToolbar() {
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private class YItemDecoration : ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val pos = parent.getChildAdapterPosition(view)
            if (pos > 3) {
                outRect.top = ConvertUtils.dp2px(24f)
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val starter = Intent(context, SearchActivity::class.java)
            context.startActivity(starter)
        }
    }
}