package com.sigpher.xpdf.main

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.CheckBox
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sigpher.base.impl.OnClickListenerImpl
import com.sigpher.xpdf.R
import com.sigpher.xpdf.about.AboutActivity
import com.sigpher.xpdf.common.*
import com.sigpher.xpdf.common.bean.Backup
import com.sigpher.xpdf.common.bean.Cover
import com.sigpher.xpdf.common.event.ImportEvent
import com.sigpher.xpdf.common.event.LegacyMissingEvent
import android.util.Base64
import com.sigpher.xpdf.common.widgets.ImageTextView
import com.sigpher.xpdf.settings.SettingsActivity
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.*
import com.sigpher.xpdf.databinding.AppActivityMainBinding

/**
 * @author sigpher
 */
class MainActivity : CommonActivity(), IMainView {

    private val presenter: IMainPresenter by lazy(LazyThreadSafetyMode.NONE) {
        MainPresenter(this)
    }

    /** 系统文件选择器：一次可选多个 PDF，无需任何存储权限 */
    private val importDocuments = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNullOrEmpty()) {
            return@registerForActivityResult
        }
        presenter.importUris(uris, getString(R.string.app_import))
    }

    /** 目录树导入：选中一个目录后批量导入其下全部 PDF */
    private val importTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            return@registerForActivityResult
        }
        presenter.importTree(uri)
    }

    private val pwMenu: PopupWindow by lazy(LazyThreadSafetyMode.NONE) {
        @SuppressLint("InflateParams")
        val pwView = LayoutInflater.from(this).inflate(R.layout.app_pw_main, null)
        val tvImport = pwView.findViewById<TextView>(R.id.app_tv_import)
        val tvBackup = pwView.findViewById<TextView>(R.id.app_tv_backup)
        val tvSettings = pwView.findViewById<TextView>(R.id.app_tv_settings)
        val tvAbout = pwView.findViewById<TextView>(R.id.app_tv_about)
        PopupWindow(pwView).apply {
            val tvImportFolder = pwView.findViewById<TextView>(R.id.app_tv_import_folder)
            tvImport.setOnClickListener {
                dismiss()
                importDocuments.launch(arrayOf(MIME_PDF))
            }
            tvImportFolder.setOnClickListener {
                dismiss()
                importTree.launch(null)
            }
            tvBackup.setOnClickListener {
                // 备份写在应用外部私有目录，无需存储权限
                createBackupDialog().show()
                dismiss()
            }
            tvSettings.setOnClickListener {
                SettingsActivity.start(this@MainActivity)
                dismiss()
            }
            tvAbout.setOnClickListener {
                AboutActivity.start(this@MainActivity)
                dismiss()
            }
            animationStyle = R.style.AppPwMenu
            isFocusable = true
            isOutsideTouchable = true
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            elevation = ConvertUtils.dp2px(4f).toFloat()
        }
    }
    private val deleteDialog: BottomSheetDialog by lazy(LazyThreadSafetyMode.NONE) {
        DialogManager.createDeleteDialog(this) { tv, deleteLocal, btnLeft, btnRight ->
            tvDeleteDescription = tv
            this.deleteLocal = deleteLocal
            val cb = deleteLocal.findViewById<CheckBox>(R.id.app_cb)
            deleteLocal.setOnClickListener {
                it.isSelected = !it.isSelected
                cb.isChecked = it.isSelected
            }
            btnLeft.setOnClickListener { deleteDialog.dismiss() }
            btnRight.setOnClickListener {
                deleteDialog.dismiss()
                operation?.delete(deleteLocal.isSelected)
                deleteLocal.isSelected = false
                cb.isChecked = false
            }
        }
    }
    private val importInfoDialog: BottomSheetDialog by lazy(LazyThreadSafetyMode.NONE) {
        DialogManager.createImportInfoDialog(this) { tvTitle, progressBar, tvProgressInfo, btn ->
            tvDialogTitle = tvTitle
            pbDialogProgress = progressBar
            tvDialogProgressInfo = tvProgressInfo
            btn.setOnClickListener(object : OnClickListenerImpl() {
                override fun onViewClick(v: View, interval: Long) {
                    importInfoDialog.dismiss()
                    pbDialogProgress?.max = 0
                }
            })
        }
    }

    private var operation: IOperation? = null
    private var tvDeleteDescription: TextView? = null
    private var deleteLocal: View? = null
    private var tvDialogTitle: TextView? = null
    private var pbDialogProgress: ProgressBar? = null
    private var tvDialogProgressInfo: TextView? = null

    /**
     * 用于显示备份恢复的结果展示
     */
    private var result: TextView? = null
    private var ok: View? = null
    private var rotationAnim: Animator? = null
    private var colorAnim: Animator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        initView()
        shortcut(intent ?: return)
    }

    override fun onStop() {
        super.onStop()
        finishOperation()
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        shortcut(intent ?: return)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.app_search) {
            SearchActivity.start(this)
        } else if (itemId == R.id.app_more) {
            val x = ConvertUtils.dp2px(6f)
            val y = ConvertUtils.dp2px(80f)
            pwMenu.showAtLocation(window.decorView, Gravity.TOP or Gravity.END, x, y)
        }
        return true
    }

    override fun onBackPressed() {
        if (binding.appIncludeOperationBar.appVgOperation.visibility == View.VISIBLE) {
            finishOperation()
        } else {
            super.onBackPressed()
        }
    }

    private val binding by lazy { AppActivityMainBinding.inflate(layoutInflater) }

    override fun bindContentView() = binding.root

    override fun layoutId(): Int = R.layout.app_activity_main

    override fun createToolbar(): Toolbar? = findViewById(R.id.app_toolbar)

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onImportEvent(event: ImportEvent) {
        if (!importInfoDialog.isShowing) {
            event.stop = true
            return
        }
        tvDialogTitle?.text = getString(R.string.app_importing, event.name)
        if (pbDialogProgress?.max == 0) {
            pbDialogProgress?.max = event.totalProgress
        }
        pbDialogProgress?.progress = event.curProgress
        tvDialogProgressInfo?.text = getString(R.string.app_import_progress, event.curProgress, event.totalProgress)
    }

    /**
     * 旧版本录入的外部路径在新系统上已不可读，引导用户用"导入文件夹"重新导入一次。
     * 只提示一次，且不删除任何书架记录。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLegacyMissingEvent(event: LegacyMissingEvent) {
        if (event.count <= 0 || SPStaticUtils.getBoolean(SP_LEGACY_PROMPTED, false)) {
            return
        }
        SPStaticUtils.put(SP_LEGACY_PROMPTED, true)
        AlertDialog.Builder(this)
                .setMessage(getString(R.string.app_legacy_missing_tips, event.count))
                .setPositiveButton(R.string.app_import_folder) { _: DialogInterface, _: Int -> importTree.launch(null) }
                .setNegativeButton(R.string.app_cancel, null)
                .show()
    }

    fun injectOperation(fragment: IOperation) {
        operation = fragment
    }

    fun startOperation() {
        binding.appVp.setScrollable(false)
        binding.appIncludeOperationBar.appIbtnCreateShortcut.visibility =
                if (operation?.showExport() == true)
                    View.VISIBLE
                else
                    View.GONE
        binding.appIncludeOperationBar.appIbtnSelectAll.isSelected = false
        OperationBarHelper.show(binding.appIncludeOperationBar.appVgOperation)
    }

    fun finishOperation() {
        binding.appVp.setScrollable(true)
        OperationBarHelper.hide(binding.appIncludeOperationBar.appVgOperation)
        operation?.cancelSelect()
    }

    fun selectResult(count: Int, selectAll: Boolean) {
        binding.appIncludeOperationBar.appIbtnCreateShortcut.isEnabled = count > 0
        binding.appIncludeOperationBar.appIbtnDelete.isEnabled = count > 0
        binding.appIncludeOperationBar.appIbtnSelectAll.isSelected = selectAll
        binding.appIncludeOperationBar.appTvOperationbarTitle.text = getString(R.string.app_selected_count, count)
    }

    override fun onShowMessage(stringId: Int): Unit = UiManager.showShort(stringId)

    override fun onShowLoading(): Unit = importInfoDialog.show()

    override fun onHideLoading() {
        importInfoDialog.dismiss()
        pbDialogProgress?.max = 0
    }

    override fun onUpdate() {
        for (fragment in supportFragmentManager.fragments) {
            (fragment as? AllFragment)?.run {
                update()
                return
            }
        }
    }

    private fun initView() {
        binding.appIncludeOperationBar.appIbtnCancel.setOnClickListener { finishOperation() }
        binding.appIncludeOperationBar.appIbtnCreateShortcut.setOnClickListener {
            operation?.createShortcut()
        }
        binding.appIncludeOperationBar.appIbtnDelete.setOnClickListener {
            deleteDialog.show()
            tvDeleteDescription?.text = operation?.deleteDescription()
            deleteLocal?.visibility = operation?.localDeleteVisibility() ?: View.GONE
        }
        binding.appIncludeOperationBar.appIbtnSelectAll.setOnClickListener { operation?.selectAll(!it.isSelected) }

        binding.appTabLayout.setupWithViewPager(binding.appVp)
        binding.appVp.adapter = MainFragmentAdapter(supportFragmentManager)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun createBackupUpdateDialog(@StringRes titleId: Int): BottomSheetDialog {
        return DialogManager.createBackupUpdateDialog(this) { title, progress, result, ok, dialog ->
            this.result = result
            this.ok = ok
            title.text = resources.getString(titleId)
            val duration = 1200L
            rotationAnim = ValueAnimator.ofFloat(0f, 360f).apply {
                this.duration = duration
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    progress.rotation = it.animatedValue as Float
                }
                start()
            }
            val c1 = resources.getColor(R.color.app_backup_update_progress_1)
            val c2 = resources.getColor(R.color.app_backup_update_progress_2)
            val c3 = resources.getColor(R.color.app_backup_update_progress_3)
            val c4 = resources.getColor(R.color.app_backup_update_progress_4)
            val c5 = resources.getColor(R.color.app_backup_update_progress_5)
            colorAnim = ValueAnimator.ofArgb(c1, c2, c3, c4, c5).apply {
                this.duration = duration
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener {
                    val d = progress.drawable
                    d.setTint(it.animatedValue as Int)
                    progress.setImageDrawable(d)
                }
                start()
            }
            ok.setOnClickListener {
                dialog.dismiss()
            }
        }
    }

    private fun createBackupDialog(): BottomSheetDialog {
        return DialogManager.createBackupDialog(this) { dialog, selectAll, cb, rv, backup, recovery ->
            val datas = DataManager.getCoverList()
            rv.apply {
                if (datas.size > 9) {
                    layoutParams = layoutParams.apply { height = ConvertUtils.dp2px(270f) }
                    requestLayout()
                }
                addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                        super.getItemOffsets(outRect, view, parent, state)
                        val pos = parent.getChildAdapterPosition(view)
                        if (pos % 3 == 0) {
                            outRect.left = ConvertUtils.dp2px(16f)
                            outRect.right = ConvertUtils.dp2px(8f)
                        } else if (pos % 3 == 1) {
                            outRect.left = ConvertUtils.dp2px(8f)
                            outRect.right = ConvertUtils.dp2px(8f)
                        } else {
                            outRect.left = ConvertUtils.dp2px(8f)
                            outRect.right = ConvertUtils.dp2px(16f)
                        }
                    }
                })
                addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                        super.getItemOffsets(outRect, view, parent, state)
                        val pos = parent.getChildAdapterPosition(view)
                        if (pos < 3) {
                            outRect.top = ConvertUtils.dp2px(16f)
                        }
                        outRect.bottom = ConvertUtils.dp2px(16f)
                    }
                })
                layoutManager = GridLayoutManager(this@MainActivity, 3).apply {
                    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return if (datas.isEmpty()) 3 else 1
                        }
                    }
                }
                adapter = BackupAdapter(datas) {
                    var isSelectAll = true
                    for (index in 0 until rv.childCount) {
                        if (!rv.getChildAt(index).findViewById<View>(R.id.app_bg).isSelected) {
                            isSelectAll = false
                            break
                        }
                    }
                    cb.isChecked = isSelectAll
                }
            }
            selectAll.setOnClickListener {
                if (datas.isEmpty()) {
                    return@setOnClickListener
                }
                cb.isChecked = !cb.isChecked
                for (index in 0 until rv.childCount) {
                    rv.getChildAt(index).findViewById<View>(R.id.app_bg).isSelected = cb.isChecked
                }
            }
            backup.setOnClickListener {
                if (datas.isEmpty()) {
                    return@setOnClickListener
                }
                var none = true
                for (index in 0 until rv.childCount) {
                    if (rv.getChildAt(index).findViewById<View>(R.id.app_bg).isSelected) {
                        none = false
                        break
                    }
                }
                if (none) {
                    ToastUtils.showShort(R.string.app_backup_select_must_not_be_empty)
                    return@setOnClickListener
                }
                dialog.dismiss()
                createBackupUpdateDialog(R.string.app_backup).show()
                launch {
                    var count = 0
                    var failed = 0
                    withContext(Dispatchers.IO) {
                        SystemClock.sleep(SLEEP_TIME)
                        for (index in 0 until rv.childCount) {
                            if (!rv.getChildAt(index).findViewById<View>(R.id.app_bg).isSelected) {
                                continue
                            }
                            count++
                            val dirName = rv.getChildAt(index).findViewById<TextView>(R.id.app_title).text.toString()
                            val pdfList = DataManager.getPdfList(dirName)
                            val bean = Backup(dirName, pdfList)
                            val json = GsonUtils.toJson(bean)
                            val state = FileIOUtils.writeFileFromBytesByChannel(File(BACKUP_PATH, "$dirName.txt"),
                                    Base64.encode(json.toByteArray(), Base64.NO_WRAP), true)
                            if (!state) failed++
                        }
                    }
                    rotationAnim?.cancel()
                    colorAnim?.cancel()
                    result?.apply {
                        visibility = View.VISIBLE
                        text = resources.getString(R.string.app_backup_completed, count, failed)
                    }
                    ok?.isEnabled = true
                }
            }
            recovery.setOnClickListener {
                dialog.dismiss()
                createBackupUpdateDialog(R.string.app_recovery).show()
                launch {
                    var count = 0
                    var failed = 0
                    withContext(Dispatchers.IO) {
                        SystemClock.sleep(SLEEP_TIME)
                        val fileList = FileUtils.listFilesInDir(BACKUP_PATH)
                        for (file in fileList) {
                            file ?: continue
                            if (!file.isFile || !file.name.endsWith(".txt", true)) {
                                continue
                            }
                            count++
                            val fileName = file.name
                            val base64 = FileIOUtils.readFile2BytesByChannel(File(BACKUP_PATH, fileName))
                            if (base64 == null) {
                                failed++
                                continue
                            }
                            val bytes = Base64.decode(base64, Base64.NO_WRAP)
                            val json = String(bytes)
                            val bean = GsonUtils.fromJson<Backup>(json, Backup::class.java)
                            if (bean == null) {
                                failed++
                                continue
                            }
                            val dirName = bean.dirName
                            val pdfs = bean.datas
                            DBHelper.insertBackup(pdfs, dirName)
                            DataManager.updateAll()
                        }
                    }
                    onUpdate()
                    rotationAnim?.cancel()
                    colorAnim?.cancel()
                    result?.apply {
                        visibility = View.VISIBLE
                        text = resources.getString(R.string.app_recovery_completed, count, failed)
                    }
                    ok?.isEnabled = true
                }
            }
        }
    }

    private fun shortcut(intent: Intent) {
        intent.getStringExtra(EXTRA_DIR_NAME)?.let {
            intent.extras?.remove(EXTRA_DIR_NAME)
            binding.appVp.apply {
                currentItem = 1
                post { (operation as? AllFragment)?.openCollection(it) }
            }
        }
    }

    companion object {
        const val EXTRA_DIR_NAME = "EXTRA_DIR_NAME"
        private const val MIME_PDF = "application/pdf"
        private const val SP_LEGACY_PROMPTED = "SP_LEGACY_PROMPTED"
        private val BACKUP_PATH = "${PathUtils.getExternalAppDataPath()}/files"
        private const val SLEEP_TIME = 1000L
    }
}

private class BackupAdapter(val datas: List<Cover>, val onClick: () -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        if (viewType == TYPE_EMPTY) {
            val view = LayoutInflater.from(context)
                    .inflate(R.layout.app_recycler_item_emptyview, parent, false)
            return EmptyHolder(view).apply {
                imageTv.setText(R.string.app_have_no_all)
                imageTv.setIconTop(R.drawable.app_img_all)
            }
        }
        val view = LayoutInflater.from(context)
                .inflate(R.layout.app_recycler_item_backup, parent, false)
        return ViewHolder(view).apply {
            itemView.setOnClickListener {
                if (viewType == TYPE_EMPTY) {
                    return@setOnClickListener
                }
                bg.isSelected = !bg.isSelected
                onClick()
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? ViewHolder)?.apply {
            val cover = datas[position]
            title.text = cover.name
            count.text = App.getContext().resources.getString(R.string.app_total, cover.count)
        }
    }

    override fun getItemCount(): Int {
        return if (datas.isEmpty()) 1 else datas.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (datas.isEmpty()) TYPE_EMPTY else TYPE_CONTENT
    }

    companion object {
        private const val TYPE_EMPTY = 1
        private const val TYPE_CONTENT = 2
    }
}

private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.app_title)
    val count: TextView = itemView.findViewById(R.id.app_count)
    val bg: ViewGroup = itemView.findViewById(R.id.app_bg)
}

private class EmptyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageTv: ImageTextView = itemView.findViewById(R.id.app_itv_placeholder)
}