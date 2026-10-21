package com.sigpher.xpdf.filepicker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.sigpher.base.impl.TextWatcherImpl
import com.sigpher.base.util.StatusBarUtils
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.CommonActivity
import com.blankj.utilcode.util.KeyboardUtils
import com.github.anzewei.parallaxbacklayout.ParallaxBack
import com.google.android.material.tabs.TabLayout
import java.util.*
import kotlin.math.sqrt
import com.sigpher.xpdf.databinding.AppActivitySelectBinding

/**
 * @author sigpher
 */
@ParallaxBack
class SelectActivity : CommonActivity() {

    val ibtnSelectAll: ImageButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<ImageButton>(R.id.app_ibtn_check)
    }
    val ibtnSearch: ImageButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<ImageButton>(R.id.app_ibtn_search)
    }
    val ibtnInverse: ImageButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<ImageButton>(R.id.app_ibtn_inverse)
    }
    val etSearch: EditText by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<EditText>(R.id.app_et_search)
    }
    var importeds: List<String>? = null
    // 揭露动画参数
    private val duration = 250
    private var centerX = 0
    private var centerY = 0
    private var radius = 0f
    private var viewAllFragment: ViewAllFragment? = null
    private var fragmentPagerAdapter: FragmentPagerAdapter? = null

    fun setRevealParam() {
        binding.appSearchView.root.post {
            centerX = binding.appIbtnSearch.left + binding.appIbtnSearch.measuredWidth / 2
            centerY = binding.appIbtnSearch.top + binding.appIbtnSearch.measuredHeight / 2
            val width = centerX * 2
            val height = binding.appSearchView.root.measuredHeight
            radius = (sqrt(width * width + height * height.toDouble()) / 2).toFloat()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initToolbar()
        toolbar?.setPadding(0, 0, 0, 0)
        StatusBarUtils.setStatusBarLight(this, true)
        initView()
    }

    override fun onStop() {
        super.onStop()
        if (KeyboardUtils.isSoftInputVisible(this)) {
            KeyboardUtils.hideSoftInput(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val list = supportFragmentManager.fragments
        for (f in list) {
            if (f is ViewAllFragment) viewAllFragment = f
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onBackPressed() {
        if (binding.appSearchView.root.visibility == View.VISIBLE) {
            closeSearchView()
            viewAllFragment?.setFocus()
        } else {
            super.onBackPressed()
        }
    }

    private val binding by lazy { AppActivitySelectBinding.inflate(layoutInflater) }

    override fun bindContentView() = binding.root

    override fun layoutId(): Int {
        return R.layout.app_activity_select
    }

    override fun createToolbar(): Toolbar? {
        return findViewById(R.id.app_toolbar)
    }

    private fun initView() {
        val data = intent
        importeds = data.getStringArrayListExtra(EXTRA_IMPORTED)
        binding.appSearchView.appIbtnBack.setOnClickListener { closeSearchView() }
        binding.appIbtnSearch.setOnClickListener { openSearchView() }
        binding.appSearchView.appIbtnInverse.setOnClickListener {
            if (KeyboardUtils.isSoftInputVisible(this)) {
                KeyboardUtils.hideSoftInput(this)
            }
            binding.appSearchView.appIbtnInverse.isSelected = !binding.appSearchView.appIbtnInverse.isSelected
            viewAllFragment?.adapter?.setInverse(binding.appSearchView.appIbtnInverse.isSelected)
            viewAllFragment?.adapter?.filter?.filter(binding.appSearchView.appEtSearch.text)
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
                viewAllFragment?.adapter?.filter?.filter(c)
            }
        })
        binding.appTabLayout.setupWithViewPager(binding.appVp)
        fragmentPagerAdapter = SelectFragmentAdapter(supportFragmentManager)
        binding.appVp.adapter = fragmentPagerAdapter
    }

    private fun initToolbar() {
        supportActionBar?.run {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.app_ic_action_back_black)
        }
        toolbar?.setTitle(R.string.app_import_file)
    }

    private fun openSearchView() {
        val animator = ViewAnimationUtils.createCircularReveal(binding.appSearchView.root, centerX, centerY, 0f, radius)
        animator.duration = duration.toLong()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                binding.appSearchView.root.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator) {
                binding.appSearchView.appEtSearch.requestFocus()
                KeyboardUtils.showSoftInput(this@SelectActivity)
            }
        })
        animator.start()
    }

    fun closeSearchView() {
        val animator = ViewAnimationUtils.createCircularReveal(binding.appSearchView.root, centerX, centerY, radius, 0f)
        animator.duration = duration.toLong()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (KeyboardUtils.isSoftInputVisible(this@SelectActivity)) {
                    KeyboardUtils.hideSoftInput(this@SelectActivity)
                }
                binding.appSearchView.root.visibility = View.GONE
            }
        })
        animator.start()
    }

    companion object {
        const val EXTRA_SELECTED = "EXTRA_SELECTED"
        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val EXTRA_GROUP_NAME = "EXTRA_GROUP_NAME"
        const val TYPE_TO_EXIST = 1
        const val TYPE_BASE_FOLDER = 2
        const val TYPE_CUSTOM = 3
        const val REQUEST_CODE = 111
        const val EXTRA_IMPORTED = "EXTRA_IMPORTED"

        fun start(activity: Activity, requestCode: Int, imported: ArrayList<String?>?) {
            val starter = Intent(activity, SelectActivity::class.java)
            starter.putStringArrayListExtra(EXTRA_IMPORTED, imported)
            activity.startActivityForResult(starter, requestCode)
        }
    }
}