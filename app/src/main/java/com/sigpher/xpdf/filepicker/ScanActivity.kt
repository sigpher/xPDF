package com.sigpher.xpdf.filepicker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.sigpher.base.impl.OnClickListenerImpl
import com.sigpher.base.impl.TextWatcherImpl
import com.sigpher.base.util.StatusBarUtils
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.CommonActivity
import com.sigpher.xpdf.common.DialogManager
import com.sigpher.xpdf.common.GroupingAdapter
import com.sigpher.xpdf.common.UiManager
import com.sigpher.xpdf.filepicker.ScanActivity
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SDCardUtils
import com.blankj.utilcode.util.StringUtils
import com.github.anzewei.parallaxbacklayout.ParallaxBack
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.sqrt
import com.sigpher.xpdf.databinding.AppActivityScanBinding

@ParallaxBack
class ScanActivity : CommonActivity() {

    // 揭露动画参数
    private val duration = 250
    private var centerX = 0
    private var centerY = 0
    private var radius = 0f
    private val fileList: MutableList<File> = ArrayList()
    private val selectList: MutableList<String> = ArrayList()
    private var tvScanCount: TextView? = null
    private var tvPdfCount: TextView? = null
    private var btnStopScan: Button? = null
    private var scanCount = 0
    private var pdfCount = 0
    private var stopScan = false
    private var newGroupName: String? = null
    private var threadPool: ExecutorService? = null
    private val listable: IListable = ByNameListable()

    private var adapter: ViewAllAdapter? = null

    private val scanDialog: BottomSheetDialog by lazy(LazyThreadSafetyMode.NONE) {
        DialogManager.createScanDialog(this) { tvTitle, tvContent, btn ->
            tvScanCount = tvTitle
            tvScanCount?.text = getString(R.string.app_already_scan, 0)
            tvPdfCount = tvContent
            tvPdfCount?.text = getString(R.string.app_find, 0)
            btnStopScan = btn
            btnStopScan?.setOnClickListener(object : OnClickListenerImpl() {
                override fun onViewClick(v: View, interval: Long) {
                    btnStopScan?.isSelected = true
                    stopScan = true
                    scanDialog.dismiss()
                    threadPool?.shutdownNow()
                    updateUI()
                }
            })
        }
    }
    private val importDialog: BottomSheetDialog by lazy(LazyThreadSafetyMode.NONE) {
        DialogManager.createImportDialog(this) { etInput, btnLeft, btnCenter, btnRight ->
            etInput.addTextChangedListener(object : TextWatcherImpl() {
                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    newGroupName = charSequence.toString()
                }
            })
            btnLeft.setOnClickListener(object : OnClickListenerImpl() {
                override fun onViewClick(v: View, interval: Long) {
                    groupingDialog.show()
                }
            })
            btnCenter.setOnClickListener(object : OnClickListenerImpl() {
                override fun onViewClick(v: View, interval: Long) {
                    setResultBack(SelectActivity.TYPE_BASE_FOLDER, null)
                }
            })
            btnRight.setOnClickListener(object : OnClickListenerImpl() {
                override fun onViewClick(v: View, interval: Long) {
                    if (StringUtils.isEmpty(newGroupName)) {
                        UiManager.showShort(R.string.app_type_new_group_name)
                    } else {
                        setResultBack(SelectActivity.TYPE_CUSTOM, newGroupName)
                    }
                }
            })
        }
    }
    private val groupingDialog: BottomSheetDialog by lazy(LazyThreadSafetyMode.NONE) {
        DialogManager.createGroupingDialog(this@ScanActivity, false, object : GroupingAdapter.Callback {
            override fun onAddNewGroup() { // empty
            }

            override fun onAddToGroup(dir: String) {
                setResultBack(SelectActivity.TYPE_TO_EXIST, dir)
            }
        })
    }
    private val dataObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            if (!isFinishing) {
                selectList.clear()
                binding.appIbtnSelectAll.isSelected = false
                binding.appBtnImportCount.setText(getString(R.string.app_import_count, 0))
                if (stopScan) {
                    val enableSelectAll = adapter!!.reset()
                    binding.appIbtnSelectAll.isEnabled = enableSelectAll
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initToolbar()
        toolbar?.setPadding(0, 0, 0, 0)
        StatusBarUtils.setStatusBarLight(this, true)
        initView()
    }

    private val binding by lazy { AppActivityScanBinding.inflate(layoutInflater) }

    override fun bindContentView() = binding.root

    override fun layoutId(): Int {
        return R.layout.app_activity_scan
    }

    override fun createToolbar(): Toolbar? {
        return findViewById(R.id.app_toolbar)
    }

    override fun onStop() {
        super.onStop()
        if (KeyboardUtils.isSoftInputVisible(this)) {
            KeyboardUtils.hideSoftInput(this)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onBackPressed() {
        if (binding.appSearchView.root.visibility == View.VISIBLE) {
            closeSearchView()
        } else {
            super.onBackPressed()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        val imported: List<String>? = intent.getStringArrayListExtra(EXTRA_IMPORTED)
        threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        binding.appSearchView.root.post {
            centerX = binding.appIbtnSearch.left + binding.appIbtnSearch.measuredWidth / 2
            centerY = binding.appIbtnSearch.top + binding.appIbtnSearch.measuredHeight / 2
            val width = centerX * 2
            val height = binding.appSearchView.root.measuredHeight
            radius = (sqrt(width * width + height * height.toDouble()) / 2).toFloat()
        }
        binding.appSearchView.appIbtnBack.setOnClickListener { closeSearchView() }
        binding.appIbtnSearch.setOnClickListener { openSearchView() }
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
        binding.appIbtnSelectAll.setOnClickListener {
            it.isSelected = !it.isSelected
            adapter?.selectAll(it.isSelected)
        }
        binding.appBtnImportCount.setOnClickListener(object : OnClickListenerImpl() {
            override fun onViewClick(v: View, interval: Long) {
                if (KeyboardUtils.isSoftInputVisible(this@ScanActivity)) {
                    KeyboardUtils.hideSoftInput(this@ScanActivity)
                }
                if (selectList.isEmpty()) {
                    UiManager.showShort(R.string.app_have_not_select)
                } else {
                    importDialog.show()
                }
            }
        })
        binding.appIbtnSelectAll.isEnabled = false // XML 设置无效，只能这里初始化
        val lm: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.appRvSelect.layoutManager = lm
        adapter = ViewAllAdapter(fileList, imported, object : ViewAllAdapter.Callback {
            override fun onDirTap(dirPath: String) { // empty impl
            }

            @SuppressLint("SetTextI18n")
            override fun onSelectResult(pathList: List<String>, total: Int) {
                LogUtils.e(pathList.size)
                if (total != 0) {
                    binding.appIbtnSelectAll.isSelected = pathList.size == total
                }
                binding.appBtnImportCount.text = getString(R.string.app_import_count, pathList.size)
                selectList.clear()
                selectList.addAll(pathList)
            }
        })
        adapter?.registerAdapterDataObserver(dataObserver)
        binding.appRvSelect.adapter = adapter
        scanDialog.show()
        Observable.create(ObservableOnSubscribe { emitter: ObservableEmitter<Double?> -> emitter.onNext(traverseFile()) })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (btnStopScan?.isSelected == false) {
                        scanDialog.dismiss()
                        updateUI()
                    }
                }) { throwable: Throwable -> LogUtils.e(throwable.message) }
    }

    private fun traverseFile(): Double {
        val start = System.currentTimeMillis()
        val result = SDCardUtils.getSDCardInfo()
        for (info in result) {
            if (!stopScan && "mounted" == info.state) {
                traverse(File(info.path))
            }
        }
        while (true) {
            val temp = scanCount
            SystemClock.sleep(1000)
            if (temp == scanCount) {
                stopScan = true
                val end = System.currentTimeMillis()
                val cost = (end - start - 1000).toDouble() / 1000
                LogUtils.e("总共耗时：$cost 秒")
                return cost
            }
        }
    }

    //    private int refreshLayoutFlag = 2000;
    @SuppressLint("SetTextI18n")
    @Synchronized
    private fun traverse(file: File?) {
        if (stopScan) {
            return
        }
        threadPool?.execute {
            val fileList = listable.listFile(file?.absolutePath)
            for (f in fileList) {
                if (stopScan) {
                    return@execute
                }
                scanCount++
                runOnUiThread { tvScanCount?.text = getString(R.string.app_already_scan, scanCount) }
                if (f.isFile) {
                    this.fileList.add(f)
                    pdfCount++
                    runOnUiThread {
                        toolbar?.title = getString(R.string.app_scan_result, pdfCount)
                        tvPdfCount?.text = getString(R.string.app_find, pdfCount)
                    }
                } else {
                    traverse(f)
                }
            }
        }
    }

    private fun initToolbar() {
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.app_ic_action_back_black)
        }
        toolbar?.title = getString(R.string.app_scan_result, 0)
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
                KeyboardUtils.showSoftInput(this@ScanActivity)
            }
        })
        animator.start()
    }

    private fun closeSearchView() {
        val animator = ViewAnimationUtils.createCircularReveal(binding.appSearchView.root, centerX, centerY, radius, 0f)
        animator.duration = duration.toLong()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (KeyboardUtils.isSoftInputVisible(this@ScanActivity)) {
                    KeyboardUtils.hideSoftInput(this@ScanActivity)
                }
                binding.appSearchView.root.visibility = View.GONE
            }
        })
        animator.start()
    }

    private fun setResultBack(type: Int, groupName: String?) {
        val data = Intent()
        data.putStringArrayListExtra(SelectActivity.EXTRA_SELECTED, selectList as ArrayList<String>)
        data.putExtra(SelectActivity.EXTRA_TYPE, type)
        data.putExtra(SelectActivity.EXTRA_GROUP_NAME, groupName)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun updateUI() {
        val lac = AnimationUtils.loadLayoutAnimation(this, R.anim.app_layout_fall_down)
        binding.appRvSelect.layoutAnimation = lac
        binding.appRvSelect.adapter!!.notifyDataSetChanged()
        binding.appRvSelect.scheduleLayoutAnimation()
    }

    companion object {
        const val EXTRA_IMPORTED = "EXTRA_IMPORTED"
        fun start(activity: Activity?, imported: ArrayList<String?>?, requestCode: Int) {
            val starter = Intent(activity, ScanActivity::class.java)
            starter.putStringArrayListExtra(EXTRA_IMPORTED, imported)
            activity!!.startActivityForResult(starter, requestCode)
        }
    }
}