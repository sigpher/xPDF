package com.sigpher.xpdf.preview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.MutableLiveData
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.*
import com.sigpher.xpdf.common.bean.PDF
import com.sigpher.xpdf.common.event.RecentPDFEvent
import com.sigpher.xpdf.common.utils.NotchUtils
import com.sigpher.xpdf.common.utils.OnClickListenerImpl
import com.sigpher.xpdf.common.utils.TextWatcherImpl
import com.sigpher.xpdf.data.PreviewRepository
import com.sigpher.xpdf.settings.SettingsActivity.Companion.start
import com.blankj.utilcode.util.*
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.PDFView.Configurator
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfPasswordException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt
import com.sigpher.xpdf.databinding.AppActivityPreviewBinding

/**
 * 注意点：显示到界面上的页数需要加 1 ，因为 PDFView 获取到的页数是从 0 计数的。
 */
class PreviewActivity : CommonActivity(), IActivityInterface, View.OnClickListener {

    private lateinit var scales: List<TextView>
    private val scaleLevel: ViewGroup by lazy { findViewById<ViewGroup>(R.id.app_scale_level) }
    private val scale025: TextView by lazy { findViewById<TextView>(R.id.app_scale_0_25) }
    private val scale050: TextView by lazy { findViewById<TextView>(R.id.app_scale_0_50) }
    private val scale075: TextView by lazy { findViewById<TextView>(R.id.app_scale_0_75) }
    private val scale100: TextView by lazy { findViewById<TextView>(R.id.app_scale_1_00) }
    private val scale200: TextView by lazy { findViewById<TextView>(R.id.app_scale_2_00) }
    private val scale300: TextView by lazy { findViewById<TextView>(R.id.app_scale_3_00) }
    private val nightModeBtn: View by lazy { findViewById<View>(R.id.app_night_btn) }
    private val isNightMode: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply {
        observe(this@PreviewActivity) {
            Settings.nightMode = it

            if (it) {
                binding.appPdfviewBg.setImageResource(R.drawable.app_ic_placeholder_white)
                binding.appPdfviewBg.setBackgroundColor(Color.BLACK)
            } else {
                binding.appPdfviewBg.setImageResource(R.drawable.app_ic_placeholder_black)
                binding.appPdfviewBg.setBackgroundColor(Color.WHITE)
            }
            scaleViewParentAnim(0.0f, 0L, object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    scaleLevel.visibility = View.GONE
                    scales.forEach { scaleView -> scaleView.translationX = 0f }
                }
            })

            // 文档已加载时只切换色彩矩阵，保留已渲染的页面缓存；未加载时由 initPdf 里的 nightMode() 生效
            if (docLoaded) {
                binding.appPdfview.setNightMode(it == true)
                binding.appPdfview.invalidate()
            }

            val textColor = if (it) R.color.base_black else R.color.base_white
            val bg = if (it) R.drawable.app_shape_bg_scale_view_white else R.drawable.app_shape_bg_scale_view_black
            scales.forEach { tv ->
                tv.setTextColor(resources.getColor(textColor))
                tv.background = resources.getDrawable(bg)
            }
        }
    }
    private val barShowState: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply {
        observe(this@PreviewActivity) {
            if (!Settings.scrollShortCut) {
                return@observe
            }

            val pair: Pair<Float, Animator.AnimatorListener?> = if (it) {
                Pair(0.0f, null)
            } else {
                Pair(1.0f, if (binding.appPdfview.zoom != 1.0f) {
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            scaleLevel.visibility = View.VISIBLE
                        }
                    }
                } else null)
            }
            scaleViewParentAnim(pair.first, 0L, pair.second)
        }
    }
    private val updateDB: MutableLiveData<Int> = MutableLiveData<Int>().apply {
        observe(this@PreviewActivity) {
            PreviewRepository.saveProgress(
                    pdf = pdf,
                    curPage = curPage,
                    totalPage = pageCount,
                    bookmarkJson = GsonUtils.toJson(bookmarkMap.values),
                    scaleFactor = scaleFactor
            )
        }
    }
    private val alertDialog: Dialog by lazy(LazyThreadSafetyMode.NONE) {
        DialogManager.createAlertDialog(this) { tvTitle, tvContent, btn ->
            tvTitle.setText(R.string.app_oop_error)
            tvContent.setText(R.string.app_doc_parse_error)
            btn.setText(R.string.app_exit_cur_content)
            btn.setOnClickListener { finish() }
        }
    }
    private val inputDialog: Dialog by lazy(LazyThreadSafetyMode.NONE) {
        DialogManager.createInputDialog(this) { tvTitle, etInput, btnLeft, btnRight ->
            tvTitle.setText(R.string.app_need_verify_password)
            etInput.addTextChangedListener(object : TextWatcherImpl() {
                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    password = charSequence.toString()
                }
            })
            btnLeft.setText(R.string.app_cancel)
            btnLeft.setOnClickListener { finish() }
            btnRight.setText(R.string.app_confirm)
            btnRight.setOnClickListener {
                initPdf(uri, pdf)
                inputDialog.dismiss()
            }
        }
    }
    private var init = true
    private var pdf: PDF? = null // 本应用打开
    private var uri: Uri? = null // 一般是外部应用打开
    private var curPage = 0
    private var pageCount = 0
    private var password: String? = null
    private var isVolumeControl = Settings.volumeControl
    private var contentFragInterface: IContentFragInterface? = null
    private var bkFragInterface: IBkFragInterface? = null
    private var hideBar = false
    private val contentMap: MutableMap<Long, PdfDocument.Bookmark> = HashMap()
    private val bookmarkMap: MutableMap<Long, Bookmark> = HashMap()
    private val pageList: MutableList<Long> = ArrayList()

    private var scaleFactor = 1.0f
    private var previousPage = 0 // 记录 redo/undo的页码
    private var nextPage = 0
    private var canvas: Canvas? = null // AndroidPDFView 的画布
    private var pageWidth = 0F
    private var docLoaded = false // 文档是否已加载完成，用于区分"首次加载"和"运行中切换"
    private lateinit var bookmarkBadge: BookmarkBadgeRenderer // 书签角标，位图解码一次复用
    //endregion

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarkBadge = BookmarkBadgeRenderer(resources)
        initView(savedInstanceState)
        EventBus.getDefault().register(this)

        scales = listOf(scale025, scale050, scale075, scale100, scale200, scale300)
        binding.appPdfview.curZoom.observe(this) { scale ->
            if (!Settings.scrollShortCut) {
                return@observe
            }

            fun scaleViewAnim(target: View, value: Float) {
                target.animate().setDuration(SCALE_VIEW_ITEM_ANIM_DURATION).translationX(value).start()
            }

            if (barShowState.value == true) {
                return@observe
            }
            if (Settings.scrollShortCut && scaleLevel.visibility != View.VISIBLE) {
                scaleViewParentAnim(1.0f, 0L, object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        scaleLevel.visibility = View.VISIBLE
                    }
                })
                return@observe
            }
            scales.forEach { scaleViewAnim(it, 0f) }
            if (scale == -1f) return@observe
            when (scale) {
                0.25f -> scale025
                0.50f -> scale050
                0.75f -> scale075
                2.00f -> scale200
                3.00f -> scale300
                else -> null
            }?.apply { scaleViewAnim(this, SCALE_VIEW_ITEM_TRANS_VALUE) }
        }
    }

    override fun onRestart() {
        super.onRestart()
        fixBackToForegroundClick()
        checkScrollShortcut()
    }

    override fun onStart() {
        super.onStart()
        enterFullScreen()
    }

    override fun onResume() {
        super.onResume()
        if (!Settings.scrollShortCut) {
            binding.appPdfview.apply {
                minZoom = PDFView.DEFAULT_SCALE
                if (binding.appPdfview.zoom != PDFView.DEFAULT_SCALE) {
                    zoomWithAnimation(minZoom)
                }
            }
        } else {
            binding.appPdfview.minZoom = PDFView.DEFAULT_MIN_SCALE
        }
    }

    override fun onPause() {
        super.onPause()

        updateDB.value = 0
        // 这里发出事件主要是更新界面阅读进度
        EventBus.getDefault().post(RecentPDFEvent(true))
    }

    override fun onStop() {
        super.onStop()
        hideBar()
        resetUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        binding.appPdfview.recycle()
        bookmarkBadge.recycle()
        canvas = null
    }
    //endregion

    //region Override from system activity
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (Settings.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_CUR_PAGE, curPage)
        outState.putString(BUNDLE_PASSWORD, password)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onBackPressed() {
        when {
            binding.appLlContent.translationX == 0f -> {
                // 等于 0 表示正处于打开状态，需要隐藏
                hideContent(null)
            }
            binding.appLlReadMethod.translationY != ScreenUtils.getScreenHeight().toFloat() -> {
                // 不等于屏幕高度表示正处于显示状态，需要隐藏
                hideReadMethod()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                && ScreenUtils.isPortrait() && isVolumeControl && toolbar?.alpha == 0.0f) {
            // 如果非全屏状态是无法使用音量键翻页的
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    var currentPage1 = binding.appPdfview.currentPage
                    binding.appPdfview.jumpTo(--currentPage1, true)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    var currentPage2 = binding.appPdfview.currentPage
                    binding.appPdfview.jumpTo(++currentPage2, true)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SETTINGS) {
            isVolumeControl = Settings.volumeControl
        }
    }
    //endregion

    //region Implement from CommonActivity
    private val binding by lazy { AppActivityPreviewBinding.inflate(layoutInflater) }

    override fun bindContentView() = binding.root

    override fun layoutId(): Int {
        return R.layout.app_activity_preview
    }

    override fun createToolbar(): Toolbar? {
        return findViewById(R.id.app_toolbar)
    }

    override fun isAdaptNotch(): Boolean = true
    //endregion

    //region Implement from interfaces
    override fun onJumpTo(page: Int) {
        binding.appPdfviewBg.visibility = View.VISIBLE
        hideContent(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                binding.appPdfview.jumpTo(page)
            }
        })
    }

    /**
     * 处理缩放等级和 View 的动画
     */
    override fun onClick(v: View?) {
        v ?: return
        if (!Settings.scrollShortCut) {
            return
        }

        val scale = when (v.id) {
            R.id.app_scale_0_25 -> 0.25f
            R.id.app_scale_0_50 -> 0.50f
            R.id.app_scale_0_75 -> 0.75f
            R.id.app_scale_1_00 -> 1.00f
            R.id.app_scale_2_00 -> 2.00f
            R.id.app_scale_3_00 -> 3.00f
            else -> 1.00f
        }
        binding.appPdfview.zoomWithAnimation(scale)
        binding.appPdfview.curZoom.value = scale
        if (scale == 1.00f) {
            scaleViewParentAnim(0.0f, SCALE_VIEW_ITEM_ANIM_DURATION, object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    scaleLevel.visibility = View.GONE
                    scale100.translationX = 0f
                }
            })
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotchEvent(safeInset: NotchUtils.SafeInset) {
        val (left, _, top, _) = safeInset
        toolbar?.apply { setPadding(paddingLeft + left, paddingTop, paddingRight, paddingBottom) }
        binding.appTabLayout?.apply { setPadding(paddingLeft, paddingTop + top, paddingRight, paddingBottom) }
        binding.appVp?.apply { setPadding(paddingLeft + left, paddingTop, paddingRight, paddingBottom) }
        binding.appTvPageinfo?.apply {
            layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply { leftMargin += left }
            requestLayout()
        }
        scaleLevel.apply { setPadding(left, paddingTop, paddingRight, paddingBottom) }
        binding.appLlBottombar.apply { setPadding(left, paddingTop, paddingRight, paddingBottom) }
        binding.appLlUndoredobar.apply {
            layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply { leftMargin += left }
            requestLayout()
        }
    }
    //endregion

    //region Private function
    @SuppressLint("SwitchIntDef")
    private fun initView(savedInstanceState: Bundle?) {
        binding.appLlUndoredobar.apply {
            layoutParams = layoutParams.apply {
                width = if (ScreenUtils.isLandscape()) {
                    ConvertUtils.dp2px(300f)
                } else (ScreenUtils.getScreenWidth() * 0.7f).toInt()
            }
            requestLayout()
        }
        binding.appLlContent.post {
            binding.appLlContent.layoutParams.apply {
                val screenWidth = min(ScreenUtils.getScreenHeight(), ScreenUtils.getScreenWidth())
                width = if (Settings.lockLandscape) screenWidth else (screenWidth * 0.85f).toInt()
                binding.appLlContent.requestLayout()
            }
        }
        supportActionBar?.run {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.app_ic_action_back_white)
        }
        if (!Settings.swipeHorizontal) {
            val lp = binding.appPdfviewBg.layoutParams
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            binding.appPdfviewBg.layoutParams = lp
        }
        isNightMode.value = Settings.nightMode
        nightModeBtn.isSelected = isNightMode.value == true
        nightModeBtn.background = if (nightModeBtn.isSelected) {
            resources.getDrawable(R.drawable.app_shape_circle_white_alpha)
        } else resources.getDrawable(R.drawable.app_shape_circle_black_alpha)
        // 移动到屏幕下方
        binding.appLlReadMethod.translationY = ScreenUtils.getScreenHeight().toFloat()
        binding.appLlMore.translationY = ScreenUtils.getScreenHeight().toFloat()
        // 移动到屏幕左边
        binding.appLlContent.post {
            if (binding.appLlContent != null) {
                binding.appLlContent.translationX = -binding.appLlContent.measuredWidth.toFloat()
            }
        }
        if (Settings.swipeHorizontal) {
            binding.appTvHorizontal.setTextColor(resources.getColor(R.color.app_color_accent))
        } else {
            binding.appTvVertical.setTextColor(resources.getColor(R.color.app_color_accent))
        }
        if (Settings.lockLandscape) {
            binding.appTvLockLandscape.setTextColor(resources.getColor(R.color.app_color_accent))
            if (!ScreenUtils.isLandscape()) ScreenUtils.setLandscape(this)
        } else {
            binding.appTvLockLandscape.setTextColor(Color.WHITE)
        }
        // 目录书签侧滑页初始化
        val fm = supportFragmentManager
        val adapter: FragmentPagerAdapter = PagerAdapter(fm)
        binding.appVp.adapter = adapter
        binding.appTabLayout.setupWithViewPager(binding.appVp)
        val tab1 = binding.appTabLayout.getTabAt(0)
        val tab2 = binding.appTabLayout.getTabAt(1)
        tab1?.setCustomView(R.layout.app_tab_content)
        tab2?.setCustomView(R.layout.app_tab_bookmark)
        getData(savedInstanceState)
        initScaleFactor()
        initListener()
        initPdf(uri, pdf)
    }

    /**
     * 解决锁定横屏时从后台回到前台时头两次点击无效
     */
    private fun fixBackToForegroundClick() {
        val screenHeight = ScreenUtils.getScreenHeight()
        binding.appLlReadMethod.translationY = screenHeight.toFloat()
        binding.appLlMore.translationY = screenHeight.toFloat()
    }

    private fun checkScrollShortcut() {
        if (Settings.scrollShortCut && binding.appPdfview.curZoom.value != 1.0f) {
            scaleLevel.visibility = View.VISIBLE
        } else {
            scaleLevel.visibility = View.GONE
        }
    }

    private fun scaleViewParentAnim(alpha: Float, startDelayed: Long = 0L, listener: Animator.AnimatorListener? = null) {
        if (!Settings.scrollShortCut) {
            return
        }
        scaleLevel.animate()
                .setStartDelay(startDelayed)
                .setDuration(ANIM_DURATION)
                .alpha(alpha)
                .setListener(listener)
                .start()
    }

    private fun resetUI() {
        // 书签页回原位
        binding.appLlContent.translationX = -binding.appLlContent.measuredWidth.toFloat()
        binding.appScreenCover.alpha = 0f // 隐藏界面遮罩
        // 阅读方式回原位
        binding.appLlReadMethod.translationY = ScreenUtils.getScreenHeight().toFloat()
        binding.appLlMore.translationY = ScreenUtils.getScreenHeight().toFloat()
    }

    private fun initScaleFactor() {
        scaleFactor = pdf?.scaleFactor ?: 1.0f
        binding.appPdfview.apply {
            scaleX = scaleFactor
            scaleY = scaleFactor
        }
    }

    private fun getData(savedInstanceState: Bundle?) {
        intent.apply {
            uri = data
            val uri = this@PreviewActivity.uri
            pdf = getParcelableExtra(EXTRA_PDF)
            // 如果都为空，那一般来自快捷方式，直接通过路径查找
            if (uri == null && pdf == null) {
                val path = getStringExtra(EXTRA_PATH)
                path?.let { pdf = DBHelper.queryPDFByPath(it) }
            } else if (uri != null) {
                val file = UriUtils.uri2File(uri)
                val path = if (file != null) UriUtils.uri2File(uri).absolutePath else null
                path?.also {
                    pdf = DBHelper.queryPDFByPath(it)
                    if (pdf == null) {
                        DBHelper.insert(listOf(it))
                        pdf = DBHelper.queryPDFByPath(it)
                    }
                }
            }
            pdf?.let {
                curPage = savedInstanceState?.getInt(BUNDLE_CUR_PAGE) ?: it.curPage
                password = savedInstanceState?.getString(BUNDLE_PASSWORD)
                pageCount = it.totalPage

                val cur = System.currentTimeMillis()
                @SuppressLint("SimpleDateFormat")
                val df: DateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                it.latestRead = TimeUtils.millis2String(cur, df).toLong()
                DBHelper.updatePDF(it)
                DBHelper.insertRecent(it)
                DataManager.updatePDFs()
                EventBus.getDefault().post(RecentPDFEvent())
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        binding.appLlBottombar.setOnClickListener { }
        binding.appIbtnQuickbarAction.setOnClickListener {
            // 当前页就是操作后的上一页或者下一页
            if (it.isSelected) {
                previousPage = binding.appPdfview.currentPage
                binding.appPdfview.jumpTo(nextPage) // Redo
            } else {
                nextPage = binding.appPdfview.currentPage
                binding.appPdfview.jumpTo(previousPage) // Undo
            }
            it.isSelected = !it.isSelected
        }
        binding.appTvPreviousChapter.setOnClickListener {
            // 减 1 是为了防止当前页面有标题的情况下无法跳转，因为是按标题来跳转
            if (hideBar) {
                return@setOnClickListener   // 防误触
            }
            var targetPage = binding.appPdfview.currentPage - 1
            if (pageList.isNotEmpty() && targetPage >= pageList[0]) {
                if (binding.appLlUndoredobar.visibility != View.VISIBLE) {
                    showQuickbar()
                }
                binding.appIbtnQuickbarAction.isSelected = false // 将状态调为 Undo
                while (!pageList.contains(targetPage.toLong())) {
                    if (targetPage < pageList[0]) {
                        return@setOnClickListener   // 如果实在匹配不到就跳出方法，不执行跳转
                    }
                    targetPage-- // 如果匹配不到会一直减 1 搜索
                }
                previousPage = binding.appPdfview.currentPage
                binding.appPdfview.jumpTo(targetPage)
            }
        }
        binding.appTvNextChapter.setOnClickListener {
            if (hideBar) {
                return@setOnClickListener   // 防误触
            }
            // 这里的原理和上面跳转上一章节一样
            var targetPage = binding.appPdfview.currentPage + 1
            if (pageList.isNotEmpty() && targetPage <= pageList[pageList.size - 1]) {
                binding.appPdfviewBg.visibility = View.VISIBLE
                if (binding.appLlUndoredobar.visibility != View.VISIBLE) {
                    showQuickbar()
                }
                binding.appIbtnQuickbarAction.isSelected = false
                while (!pageList.contains(targetPage.toLong())) {
                    if (targetPage > pageList[pageList.size - 1]) {
                        return@setOnClickListener
                    }
                    targetPage++
                }
                previousPage = binding.appPdfview.currentPage
                binding.appPdfview.jumpTo(targetPage)
            }
        }
        binding.appTvContent.setOnClickListener(object : OnClickListenerImpl() {
            override fun onViewClick(v: View, interval: Long) {
                val tab = binding.appTabLayout.getTabAt(0)
                tab?.select()
                hideBar()
                enterFullScreen()
                showContent()
            }
        })
        binding.appTvReadMethod.setOnClickListener(object : OnClickListenerImpl() {
            override fun onViewClick(v: View, interval: Long) {
                hideBar()
                enterFullScreen()
                showReadMethod()
            }
        })
        binding.appTvBookmark.setOnClickListener {
            it.isSelected = !it.isSelected
            val curPage = binding.appPdfview.currentPage
            if (it.isSelected) {
                val title = getTitle(curPage)
                val time = System.currentTimeMillis()
                val bk = Bookmark(curPage, title, time)
                bookmarkMap[curPage.toLong()] = bk
            } else {
                bookmarkMap.remove(curPage.toLong())
            }
            bkFragInterface?.update(bookmarkMap.values)
            binding.appPdfview.invalidate()
            updateDB.value = 0
        }
        binding.appTvBookmark.setOnLongClickListener {
            val tab = binding.appTabLayout.getTabAt(1)
            tab?.select()
            hideBar()
            enterFullScreen()
            showContent()
            true
        }
        binding.appTvMore.setOnClickListener(object : OnClickListenerImpl() {
            override fun onViewClick(v: View, interval: Long) {
                hideBar()
                enterFullScreen()
                showMore()
            }
        })
        binding.appTvLockLandscape.setOnClickListener {
            if (ScreenUtils.isPortrait()) {
                Settings.lockLandscape = true
                ScreenUtils.setLandscape(this)
            } else {
                Settings.lockLandscape = false
                ScreenUtils.setPortrait(this)
            }
        }
        binding.appTvSettings.setOnClickListener(object : OnClickListenerImpl() {
            override fun onViewClick(v: View, interval: Long) {
                hideBar()
                start(this@PreviewActivity, REQUEST_CODE_SETTINGS)
            }
        })
        binding.appScreenCover.setOnTouchListener { _: View?, _: MotionEvent? ->
            if (binding.appLlContent.translationX == 0f) {
                hideContent(null)
                return@setOnTouchListener true
            }
            false
        }
        binding.appTvHorizontal.setOnClickListener {
            hideReadMethod()
            if (!Settings.swipeHorizontal) {
                binding.appTvHorizontal.setTextColor(resources.getColor(R.color.app_color_accent))
                binding.appTvVertical.setTextColor(resources.getColor(R.color.base_white))
                Settings.swipeHorizontal = true
                initPdf(uri, pdf)
            }
        }
        binding.appTvVertical.setOnClickListener {
            hideReadMethod()
            if (Settings.swipeHorizontal) {
                binding.appTvVertical.setTextColor(resources.getColor(R.color.app_color_accent))
                binding.appTvHorizontal.setTextColor(resources.getColor(R.color.base_white))
                Settings.swipeHorizontal = false
                initPdf(uri, pdf)
            }
        }
        nightModeBtn.setOnClickListener {
            it ?: return@setOnClickListener
            it.isSelected = !it.isSelected
            nightModeBtn.background = if (it.isSelected) {
                resources.getDrawable(R.drawable.app_shape_circle_white_alpha)
            } else resources.getDrawable(R.drawable.app_shape_circle_black_alpha)
            isNightMode.value = it.isSelected
        }
        scale025.setOnClickListener(this)
        scale050.setOnClickListener(this)
        scale075.setOnClickListener(this)
        scale100.setOnClickListener(this)
        scale200.setOnClickListener(this)
        scale300.setOnClickListener(this)
    }

    private fun showReadMethod() {
        barShowState.value = true
        val screenHeight = ScreenUtils.getScreenHeight()
        var viewHeight = binding.appLlReadMethod.measuredHeight
        if (binding.appLlReadMethod.translationY < viewHeight) {
            viewHeight += ConvertUtils.dp2px(24f)
        }
        binding.appLlReadMethod.animate()
                .setDuration(ANIM_DURATION)
                .setStartDelay(100)
                .translationY(screenHeight - viewHeight.toFloat())
                .start()
    }

    private fun hideReadMethod() {
        barShowState.value = false
        val screenHeight = ScreenUtils.getScreenHeight()
        binding.appLlReadMethod.animate()
                .setDuration(ANIM_DURATION)
                .translationY(screenHeight.toFloat())
                .start()
    }

    private fun showMore() {
        barShowState.value = true
        val screenHeight = ScreenUtils.getScreenHeight()
        var viewHeight = binding.appLlMore.measuredHeight
        if (binding.appLlMore.translationY < viewHeight) {
            viewHeight += ConvertUtils.dp2px(24f)
        }
        binding.appLlMore.animate()
                .setDuration(ANIM_DURATION)
                .setStartDelay(100)
                .translationY(screenHeight - viewHeight.toFloat())
                .start()
    }

    private fun hideMore() {
        barShowState.value = false
        val screenHeight = ScreenUtils.getScreenHeight()
        binding.appLlMore.animate()
                .setDuration(ANIM_DURATION)
                .translationY(screenHeight.toFloat())
                .start()
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun initPdf(uri: Uri?, pdf: PDF?) {
        docLoaded = false
        binding.appSbProgress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                binding.appTvPageinfo.text = (i + 1).toString() + " / " + pageCount
                // Quickbar
                binding.appQuickbarTitle.text = getTitle(i)
                binding.appTvPageinfo2.text = (i + 1).toString() + " / " + pageCount
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                binding.appIbtnQuickbarAction.isSelected = false
                previousPage = seekBar.progress
                if (!hideBar) {
                    showQuickbar()
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                nextPage = seekBar.progress
                binding.appPdfview.jumpTo(seekBar.progress)
            }
        })
        var configurator: Configurator
        when {
//            uri != null -> {
//                val file = UriUtils.uri2File(uri)
//                val path = if (file != null) UriUtils.uri2File(uri).absolutePath else null
//                binding.appTvPageinfo.text = "1 / $pageCount"
//                val bookName = path?.substring(path.lastIndexOf("/") + 1, path.length - 4) ?: ""
//                toolbar?.post { toolbar?.title = bookName }
//                configurator = binding.appPdfview.fromUri(uri).defaultPage(curPage)
//            }
            pdf != null -> {
                val bkJson = pdf.bookmark // 获取书签 json
                // 用 Class 解析而非匿名 TypeToken：R8 会合并匿名内部类并丢弃泛型签名，
                // 运行时 TypeToken 会抛 RuntimeException: Missing type parameter.
                val bkList = GsonUtils.fromJson(bkJson, Array<Bookmark>::class.java)
                bkList?.forEach { bk ->
                    LogUtils.e(bk)
                    val pageId = bk.pageId
                    bookmarkMap[pageId.toLong()] = bk
                }
                binding.appSbProgress.progress = curPage
                binding.appTvPageinfo.text = (curPage + 1).toString() + " / " + pageCount
                toolbar?.post { toolbar?.title = pdf.name }
                configurator = binding.appPdfview.fromFile(File(pdf.path)).defaultPage(curPage)
            }
            else -> {
                UiManager.showCenterShort(R.string.app_file_is_empty)
                finish()
                return
            }
        }
        if (password != null) {
            configurator = configurator.password(password)
        }
        configurator.disableLongpress()
                .swipeHorizontal(Settings.swipeHorizontal)
                .nightMode(isNightMode.value == true)
                .pageFling(Settings.swipeHorizontal)
                .pageSnap(Settings.swipeHorizontal)
                .enableDoubletap(false)
                .fitEachPage(true) // .spacing(ConvertUtils.dp2px(4))
                .onError { throwable: Throwable ->
                    LogUtils.e(throwable.message)
                    showError(throwable)
                }
                .onPageError { page: Int, throwable: Throwable ->
                    LogUtils.e(throwable.message)
                    UiManager.showShort(getString(R.string.app_cur_page_parse_error, page))
                }
                .onDrawAll { canvas: Canvas?, pageWidth: Float, _: Float, displayedPage: Int ->
                    this.canvas = canvas
                    this.pageWidth = pageWidth
                    if (bookmarkMap.containsKey(displayedPage.toLong())) {
                        drawBookmark(canvas, pageWidth)
                    }
                }
                .onPageChange { page: Int, _: Int ->
                    curPage = page
                    val pageInfo = "${page + 1} / ${this.pageCount}"
                    binding.appTvBookmark.isSelected = bookmarkMap.containsKey(page.toLong()) // 如果是书签则标红
                    binding.appQuickbarTitle.text = getTitle(page)
                    binding.appTvPageinfo2.text = pageInfo
                    binding.appTvPageinfo.text = pageInfo
                    binding.appSbProgress.progress = page
                }
                .onLoad {
                    docLoaded = true
                    initContentAndBookmark()
                    initAboutPage()
                    initBgSize()
                    init = false
                }
                .onTap { event: MotionEvent ->
                    if (judgeCloseMore()) return@onTap true
                    if (judgeCloseReadMethod()) return@onTap true
                    judgeFlipPage(event)
                }
                .load()
    }

    private fun initBgSize() {
        if (isNightMode.value != true) {
            if (Settings.swipeHorizontal) {
                val page = (binding.appPdfview.pageCount / 2.toFloat()).roundToInt()
                val sizeF = binding.appPdfview.getPageSize(page)
                val lp = binding.appPdfviewBg.layoutParams
                lp.width = sizeF.width.toInt()
                lp.height = sizeF.height.toInt()
                binding.appPdfviewBg.layoutParams = lp
            } else {
                val lp = binding.appPdfviewBg.layoutParams
                lp.width = ScreenUtils.getScreenWidth()
                lp.height = ScreenUtils.getScreenHeight()
                binding.appPdfviewBg.layoutParams = lp
            }
        }
    }

    private fun initAboutPage() {
        pageCount = binding.appPdfview.pageCount
        binding.appSbProgress.max = pageCount - 1
        val keySet: Set<Long> = contentMap.keys
        pageList.addAll(keySet)
        pageList.sort()
    }

    private fun initContentAndBookmark() {
        val list = binding.appPdfview.tableOfContents
        if (init) {
            findContent(list)
        }
        val fragmentList = supportFragmentManager.fragments
        for (f in fragmentList) {
            if (f is IContentFragInterface) {
                contentFragInterface = f
            } else if (f is IBkFragInterface) {
                bkFragInterface = f
            }
        }
        if (init) {
            contentFragInterface?.update(list)
        }
        bkFragInterface?.update(bookmarkMap.values)
    }

    private fun showError(throwable: Throwable) {
        if (throwable is PdfPasswordException) {
            if (!StringUtils.isEmpty(password)) {
                UiManager.showCenterShort(R.string.app_password_error)
            }
            showInputDialog()
        } else {
            showAlertDialog()
        }
    }

    private fun judgeFlipPage(event: MotionEvent): Boolean {
        val x = event.rawX
        val previous: Float
        val next: Float
        if (Settings.clickFlipPage) {
            previous = ScreenUtils.getScreenWidth() * 0.3f
            next = ScreenUtils.getScreenWidth() * 0.7f
        } else {
            previous = 0f
            next = ScreenUtils.getScreenWidth().toFloat()
        }
        if (ScreenUtils.isPortrait() && x <= previous) {
            if (toolbar?.alpha == 1.0f) {
                hideBar()
                enterFullScreen()
            } else {
                var currentPage = binding.appPdfview.currentPage
                binding.appPdfview.jumpTo(--currentPage, true)
            }
        } else if (ScreenUtils.isPortrait() && x >= next) {
            if (toolbar?.alpha == 1.0f) {
                hideBar()
                enterFullScreen()
            } else {
                var currentPage = binding.appPdfview.currentPage
                binding.appPdfview.jumpTo(++currentPage, true)
            }
        } else {
            val visible = (toolbar?.alpha == 1.0f
                    && binding.appLlBottombar.alpha == 1.0f)
            if (visible) {
                hideBar()
                enterFullScreen()
            } else {
                exitFullScreen()
                showBar()
            }
        }
        return true
    }

    private fun judgeCloseMore(): Boolean {
        if (binding.appLlMore.translationY != ScreenUtils.getScreenHeight().toFloat()) {
            hideMore()
            return true
        }
        return false
    }

    private fun judgeCloseReadMethod(): Boolean {
        if (binding.appLlReadMethod.translationY != ScreenUtils.getScreenHeight().toFloat()) {
            hideReadMethod()
            return true
        }
        return false
    }

    private fun showInputDialog() {
        inputDialog.show()
    }

    private fun showAlertDialog() {
        alertDialog.show()
    }

    private fun drawBookmark(canvas: Canvas?, pageWidth: Float) {
        bookmarkBadge.draw(canvas, pageWidth, scaleFactor)
    }

    private fun showQuickbar() {
        binding.appLlUndoredobar.animate()
                .setDuration(50)
                .alpha(1f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        binding.appLlUndoredobar.visibility = View.VISIBLE
                    }
                })
                .start()
    }

    private fun showContent() {
        barShowState.value = true
        binding.appLlContent.animate()
                .setDuration(ANIM_DURATION)
                .setStartDelay(100)
                .translationX(0f)
                .setUpdateListener { valueAnimator: ValueAnimator -> binding.appScreenCover.alpha = valueAnimator.animatedFraction }
                .start()
    }

    private fun hideContent(listener: Animator.AnimatorListener?) {
        barShowState.value = false
        binding.appLlContent.animate()
                .setDuration(ANIM_DURATION)
                .translationX(-binding.appLlContent.measuredWidth.toFloat())
                .setUpdateListener { valueAnimator: ValueAnimator -> binding.appScreenCover.alpha = 1 - valueAnimator.animatedFraction }
                .setListener(listener)
                .start()
    }

    private fun findContent(list: List<PdfDocument.Bookmark>) {
        for (bk in list) {
            contentMap[bk.pageIdx] = bk
            if (bk.hasChildren()) {
                findContent(bk.children)
            }
        }
    }

    private fun getTitle(page: Int): String? {
        if (pageList.isEmpty()) {
            return null
        }
        var tempPage = page
        if (contentMap.isEmpty()) return getString(R.string.app_have_no_content)
        var title: String?
        val bk = contentMap[tempPage.toLong()]
        title = bk?.title
        if (StringUtils.isEmpty(title)) {
            if (tempPage < pageList[0]) {
                title = contentMap[pageList[0]]?.title
            } else {
                var index = pageList.indexOf(tempPage.toLong())
                while (index == -1) {
                    index = pageList.indexOf(tempPage--.toLong())
                }
                title = contentMap[pageList[index]]?.title
            }
        }
        return title
    }

    private fun showBar() {
        hideBar = false
        barShowState.value = true
        toolbar?.animate()?.setDuration(ANIM_DURATION)?.alpha(1f)
                ?.setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        toolbar?.visibility = View.VISIBLE
                    }
                })?.start()
        binding.appLlBottombar.animate().setDuration(ANIM_DURATION).alpha(1f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        binding.appLlBottombar.visibility = View.VISIBLE
                    }
                }).start()
        binding.appTvPageinfo.animate().setDuration(ANIM_DURATION).alpha(1f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        binding.appTvPageinfo.visibility = View.VISIBLE
                    }
                }).start()
        nightModeBtn.animate().setDuration(ANIM_DURATION).alpha(1f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        nightModeBtn.visibility = View.VISIBLE
                    }
                }).start()
    }

    private fun hideBar() {
        hideBar = true
        barShowState.value = false
        toolbar?.animate()?.setDuration(ANIM_DURATION)?.alpha(0f)
                ?.setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        toolbar?.visibility = View.GONE
                    }
                })?.start()
        binding.appLlBottombar.animate().setDuration(ANIM_DURATION).alpha(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (binding.appLlBottombar != null) binding.appLlBottombar.visibility = View.GONE
                    }
                }).start()
        binding.appTvPageinfo.animate().setDuration(ANIM_DURATION).alpha(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (binding.appTvPageinfo != null) binding.appTvPageinfo.visibility = View.GONE
                    }
                }).start()
        binding.appLlUndoredobar.animate().setDuration(ANIM_DURATION).alpha(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (binding.appLlUndoredobar != null) binding.appLlUndoredobar.visibility = View.GONE
                    }
                }).start()
        nightModeBtn.animate().setDuration(ANIM_DURATION).alpha(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        nightModeBtn.visibility = View.GONE
                    }
                }).start()
        if (binding.appIbtnQuickbarAction != null) binding.appIbtnQuickbarAction.isSelected = false // 初始化为 Undo 状态
    }

    private fun enterFullScreen() {
        UiManager.setTransparentNavigationBar(window)
        if (Settings.showStatusBar) {
            UiManager.setTranslucentStatusBar(this)
            binding.appLlContent.setPadding(0, ConvertUtils.dp2px(25f), 0, 0)
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        } else {
            UiManager.setTransparentStatusBar(this)
            binding.appLlContent.setPadding(0, 0, 0, 0)
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun exitFullScreen() {
        if (Settings.showStatusBar) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
//        toolbar?.post { UiManager.setNavigationBarColor(this, resources.getColor(R.color.base_black)) }
    }
    //endregion

    companion object {
        const val EXTRA_PATH = "EXTRA_ID"
        private const val EXTRA_PDF = "EXTRA_PDF"
        private const val BUNDLE_CUR_PAGE = "BUNDLE_CUR_PAGE"
        private const val BUNDLE_PASSWORD = "BUNDLE_PASSWORD"
        private const val REQUEST_CODE_SETTINGS = 101
        private const val ANIM_DURATION = 250L
        private const val SCALE_VIEW_ITEM_ANIM_DURATION = 150L
        private val SCALE_VIEW_ITEM_TRANS_VALUE = ConvertUtils.dp2px(12f).toFloat()
        /**
         * 非外部文件打开
         */
        fun start(context: Context, pdf: PDF?) {
            val starter = Intent(context, PreviewActivity::class.java)
            starter.putExtra(EXTRA_PDF, pdf)
            context.startActivity(starter)
        }
    }
}