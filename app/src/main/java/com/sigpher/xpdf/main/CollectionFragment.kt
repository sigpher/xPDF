package com.sigpher.xpdf.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.*
import com.sigpher.xpdf.common.bean.PDF
import com.sigpher.xpdf.common.bean.Shortcut
import com.sigpher.xpdf.common.event.AllEvent
import com.sigpher.xpdf.common.event.UpdateAllEvent
import com.sigpher.xpdf.common.utils.DialogUtils
import com.sigpher.xpdf.common.utils.ShortcutUtils
import com.sigpher.xpdf.common.utils.TextWatcherImpl
import com.sigpher.xpdf.preview.PreviewActivity
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*
import com.sigpher.xpdf.databinding.AppFragmentCollectionBinding

/**
 * @author sigpher
 */
class CollectionFragment : DialogFragment(), IOperation, GroupingAdapter.Callback {

    private val deleteDialog: BottomSheetDialog by lazy(LazyThreadSafetyMode.NONE) {
        val view = LayoutInflater.from(activity!!).inflate(R.layout.app_bottomdialog_delete, null)
        tvDeleteDescription = view.findViewById(R.id.app_tv_description)
        deleteLocal = view.findViewById(R.id.app_delete_local)
        val btnCancel = view.findViewById<Button>(R.id.app_btn_cancel)
        val btnDelete = view.findViewById<Button>(R.id.app_btn_delete)
        val cb = deleteLocal?.findViewById<CheckBox>(R.id.app_cb)
        deleteLocal?.setOnClickListener {
            it.isSelected = !it.isSelected
            cb?.isChecked = it.isSelected
        }
        btnCancel.setOnClickListener { deleteDialog.dismiss() }
        btnDelete.setOnClickListener {
            deleteDialog.dismiss()
            delete(deleteLocal?.isSelected ?: false)
            deleteLocal?.isSelected = false
            cb?.isChecked = false
        }
        DialogUtils.createBottomSheetDialog(requireActivity(), view)
    }
    private val regroupingDialog: BottomSheetDialog by lazy(LazyThreadSafetyMode.NONE) {
        DialogManager.createGroupingDialog(activity!!, true, this)
    }
    private val addNewGroupDialog: Dialog by lazy(LazyThreadSafetyMode.NONE) {
        DialogManager.createInputDialog(activity!!) { tvTitle, etInput, btnLeft, btnRight ->
            tvTitle.setText(R.string.app_add_new_group)
            etInput.inputType = InputType.TYPE_CLASS_TEXT
            etInput.setHint(R.string.app_type_new_group_name)
            this.etInput = etInput
            btnLeft.setText(R.string.app_cancel)
            btnLeft.setOnClickListener { addNewGroupDialog.dismiss() }
            btnRight.setText(R.string.app_confirm)
            btnRight.setOnClickListener {
                createNewGroup(this.etInput?.text.toString())
            }
        }.apply { setOnDismissListener { etInput?.setText("") } }
    }

    private lateinit var adapter: CollectionAdapter
    private var tvDeleteDescription: TextView? = null
    private var deleteLocal: View? = null
    private var etInput: EditText? = null
    private var name: String? = null
    private var newDirName: String? = null
    private val selectPDFList: MutableList<PDF> = ArrayList()
    private val pdfList: MutableList<PDF> = ArrayList()
    private val savedCollections = DataManager.getCollectionList()
    private var isNeedUpdateDb = false
    private var isHorizontalLayout = false
    private var xItemDecoration: RecyclerView.ItemDecoration? = null
    private var yItemDecoration: RecyclerView.ItemDecoration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AppCollectionFragment)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            val lp = this.attributes
            lp.gravity = Gravity.CENTER
            lp.dimAmount = 0.0f
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            this.attributes = lp
            this.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            this.setWindowAnimations(R.style.AppPwCollection)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
        if (isHorizontalLayout != Settings.linearLayout) {
            isHorizontalLayout = Settings.linearLayout
            initView()
        }
    }

    private var _binding: AppFragmentCollectionBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AppFragmentCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun localDeleteVisibility(): Int = View.VISIBLE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments
        if (args != null) {
            name = args.getString(BUNDLE_NAME)
            binding.appEtName.setText(name)
        }
        initView()
    }

    override fun onResume() {
        super.onResume()
        isNeedUpdateDb = false
        pdfList.apply {
            clear()
            addAll(DataManager.getPdfList(name))
            adapter.setNewData(pdfList)
        }
        // 监听返回键
        val view = view
        if (view != null) {
            view.isFocusableInTouchMode = true
            view.requestFocus()
            view.setOnKeyListener(View.OnKeyListener { _, i, keyEvent ->
                if (keyEvent.action == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_BACK) {
                    if (binding.appIncludeOperationBar.appVgOperation.visibility == View.VISIBLE) {
                        cancelSelect()
                        return@OnKeyListener true
                    }
                }
                false
            })
        }
    }

    override fun onStop() {
        super.onStop()
        cancelSelect()
        if (isNeedUpdateDb) {
            DBHelper.updatePDFs(pdfList)
            DataManager.updatePDFs()
            EventBus.getDefault().post(UpdateAllEvent())
        }
    }

    private fun startPicking() {
        selectPDFList.clear()
        binding.appEtName.isEnabled = false
        cancelRename()
        binding.appIncludeOperationBar.appIbtnSelectAll.isSelected = false
        OperationBarHelper.show(binding.appIncludeOperationBar.appVgOperation)
        binding.appTvRegrouping.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun select(selectAll: Boolean) {
        binding.appIncludeOperationBar.appIbtnCreateShortcut.isEnabled = selectPDFList.isNotEmpty()
        binding.appIncludeOperationBar.appIbtnDelete.isEnabled = selectPDFList.isNotEmpty()
        binding.appIncludeOperationBar.appIbtnSelectAll.isSelected = selectAll
        binding.appIncludeOperationBar.appTvOperationbarTitle.text = getString(R.string.app_selected_count, selectPDFList.size)
    }

    override fun createShortcut() {
    }

    override fun delete(deleteLocal: Boolean) {
        ThreadUtils.executeByIo<List<String>>(object : ThreadUtils.SimpleTask<List<String>>() {
            override fun doInBackground(): List<String>? {
                if (deleteLocal) {
                    selectPDFList.forEach { File(it.path).delete() }
                }
                selectPDFList.run { pdfList.removeAll(this) }
                if (pdfList.isEmpty()) DBHelper.deleteCollection(name)
                return DBHelper.deletePDF(selectPDFList)
            }

            override fun onSuccess(nameList: List<String>?) {
                DataManager.updateAll()
                UiManager.showShort(R.string.app_delete_completed)
                cancelSelect()
                adapter.notifyDataSetChanged()
                nameList?.run { EventBus.getDefault().post(PdfDeleteEvent(this, name!!, pdfList.isEmpty())) }
                if (pdfList.isEmpty()) dismiss()
            }
        })
    }

    override fun selectAll(selectAll: Boolean) {
        binding.appIncludeOperationBar.appIbtnSelectAll.isSelected = selectAll
        adapter.selectAll(selectAll)
        selectPDFList.clear()
        if (selectAll) {
            selectPDFList.addAll(pdfList)
        }
        select(selectAll)
    }

    override fun cancelSelect() {
        binding.appEtName.isEnabled = true
        OperationBarHelper.hide(binding.appIncludeOperationBar.appVgOperation)
        adapter.exitSelectMode()
        binding.appTvRegrouping.visibility = View.GONE
        if (regroupingDialog.isShowing) {
            regroupingDialog.dismiss()
        }
    }

    /**
     * 分组方法，属于 GroupingAdapter
     */
    override fun onAddNewGroup() {
        addNewGroupDialog.show()
    }

    private fun createNewGroup(name: String) {
        if (StringUtils.isEmpty(name)) {
            UiManager.showShort(R.string.app_type_new_group_name)
            return
        }
        for (c in savedCollections) {
            if (c.name == name) {
                UiManager.showShort(R.string.app_group_name_existed)
                return
            }
        }
        pdfList.removeAll(selectPDFList)
        var max = selectPDFList.size - 1
        for (pdf in selectPDFList) {
            pdf.position = max--
        }
        DBHelper.insertNewCollection(name, selectPDFList)
        DataManager.updatePDFs()
        pdfList.removeAll(selectPDFList)

        cancelSelect()
        addNewGroupDialog.dismiss()
        notifyGroupUpdate()
    }

    /**
     * 分组方法，属于 GroupingAdapter
     */
    override fun onAddToGroup(dir: String) {
        if (name == dir) {
            cancelSelect()
            return
        }
        pdfList.removeAll(selectPDFList)
        val targetList = DataManager.getPdfList(dir)
        targetList.addAll(selectPDFList)
        var max = targetList.size - 1
        for (pdf in targetList) {
            pdf.position = max--
        }
        DBHelper.insertPDFsToCollection(dir, targetList)
        DataManager.updatePDFs()
        cancelSelect()
        notifyGroupUpdate()
    }

    private fun notifyGroupUpdate() {
//        if (pdfList.isEmpty()) {
//        } else {
//        }
        dismiss()
        adapter.notifyDataSetChanged()
        EventBus.getDefault().post(AllEvent(pdfList.isEmpty(), name))
    }

    override fun deleteDescription(): String? {
        return getString(R.string.app_whether_delete_all, selectPDFList.size)
    }

    private fun initView() {
        setListener()
        if (xItemDecoration != null) {
            binding.appRvCollection.removeItemDecoration(xItemDecoration!!)
        }
        if (yItemDecoration != null) {
            binding.appRvCollection.removeItemDecoration(yItemDecoration!!)
        }
        xItemDecoration = XGridDecoration()
        yItemDecoration = YGridDecoration()
        binding.appRvCollection.addItemDecoration(xItemDecoration!!)
        binding.appRvCollection.addItemDecoration(yItemDecoration!!)
        val lm = if (isHorizontalLayout) {
            LinearLayoutManager(activity)
        } else {
            GridLayoutManager(activity, 3).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (pdfList.isEmpty()) 3 else 1
                    }
                }
            }
        }
        binding.appRvCollection.layoutManager = lm
        adapter = CollectionAdapter(pdfList)
        adapter.setOnItemClickListener { _, view, position ->
            if (adapter.isSelectMode) {
                val pdf = pdfList[position]
                val checkBox: CheckBox? = view.findViewById(adapter.checkBoxId)
                if (checkBox?.isChecked == true) {
                    selectPDFList.remove(pdf)
                } else {
                    selectPDFList.add(pdf)
                }
                select(selectPDFList.size == pdfList.size)
            } else {
                val pdf = pdfList[position]
                PreviewActivity.start(context!!, pdf)
//                dismissAllowingStateLoss()
            }
        }
        adapter.setOnItemDragListener(object : OnItemDragListener {
            private lateinit var checkBox: CheckBox
            private lateinit var pdf: PDF

            override fun onItemDragMoving(
                    viewHolder: RecyclerView.ViewHolder,
                    fromPos: Int,
                    target: RecyclerView.ViewHolder,
                    toPos: Int
            ) {
            }

            override fun onItemDragStart(viewHolder: RecyclerView.ViewHolder, position: Int) {
                pdf = pdfList[position]
                checkBox = viewHolder.itemView.findViewById(R.id.app_cb)
                checkBox.visibility = View.GONE
            }

            override fun onItemDragEnd(viewHolder: RecyclerView.ViewHolder, position: Int) {
                if (!adapter.isSelectMode) {
                    startPicking()
                    selectPDFList.add(pdfList[position])
                    select(selectPDFList.size == pdfList.size)
                    adapter.enterSelectMode(viewHolder.itemView, position)
                }
                checkBox.visibility = View.VISIBLE

                var max = pdfList.size - 1
                for (pdf in pdfList) {
                    pdf.position = max--
                }
                isNeedUpdateDb = true
            }
        })
        val dragHelper = ItemTouchHelper(ItemDragAndSwipeCallback(adapter))
        dragHelper.attachToRecyclerView(binding.appRvCollection)
        adapter.enableDragItem(dragHelper)
        binding.appRvCollection.adapter = adapter
    }

    override fun showExport(): Boolean = true

    @SuppressLint("SetTextI18n")
    private fun setListener() {
        binding.appBlurView.setOnClickListener {
            when {
                binding.appEtName.hasFocus() -> {
                    cancelRename()
                }
                binding.appIncludeOperationBar.appVgOperation.visibility == View.VISIBLE -> {
                    cancelSelect()
                }
                else -> {
                    dismissAllowingStateLoss()
                }
            }
        }
        binding.appTvRegrouping.setOnClickListener {
            regroupingDialog.show()
        }
        binding.appIncludeOperationBar.appIbtnCancel.setOnClickListener { cancelSelect() }
        binding.appIncludeOperationBar.appIbtnCreateShortcut.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                visibility = View.VISIBLE
            }
            setOnClickListener {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    ToastUtils.showShort(R.string.app_not_support_launcher_shortcut)
                    return@setOnClickListener
                }
                val selecteds = selectPDFList
                if (selecteds.size > 1) {
                    ToastUtils.showShort(App.getContext().resources.getString(R.string.app_shortcut_max), 1)
                    return@setOnClickListener
                }
                selecteds[0].let {
                    ShortcutUtils.createPinnedShortcut(App.getContext(), Shortcut(
                            PreviewActivity::class.java,
                            it.id.toString(),
                            it.name,
                            it.name,
                            R.drawable.app_ic_shortcut_book,
                            PreviewActivity.EXTRA_PATH,
                            it.path
                    ))
                }
                if (Settings.firstCreateShortcut) {
                    ToastUtils.showLong(R.string.app_first_create_shortcut_tips)
                    Settings.firstCreateShortcut = false
                }
                cancelSelect()
            }
        }
        binding.appIncludeOperationBar.appIbtnDelete.setOnClickListener {
            deleteDialog.show()
            tvDeleteDescription?.text = getString(R.string.app_whether_delete_all, selectPDFList.size)
            deleteLocal?.visibility = localDeleteVisibility()
        }
        binding.appIncludeOperationBar.appIbtnSelectAll.setOnClickListener { selectAll(!it.isSelected) }
        binding.appEtName.onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            binding.appIbtnClear.visibility = if (hasFocus) View.VISIBLE else View.GONE
        }
        binding.appEtName.addTextChangedListener(object : TextWatcherImpl() {
            override fun onTextChanged(c: CharSequence, i: Int, i1: Int, i2: Int) {
                newDirName = c.toString().trim { it <= ' ' }
            }
        })
        binding.appEtName.setOnKeyListener { _, _, _ ->
            cancelRename()
            true
        }
        binding.appIbtnClear.setOnClickListener {
            binding.appEtName.setText(" ")
            binding.appEtName.setSelection(0)
        }
    }

    private fun cancelRename() {
        KeyboardUtils.hideSoftInput(binding.appEtName)
        binding.appEtName.clearFocus()
        if (newDirName != null && StringUtils.isEmpty(newDirName)) {
            binding.appEtName.setText(name)
            UiManager.showShort(R.string.app_not_support_empty_string)
        } else {
            val success = DBHelper.updateDirName(name ?: "", newDirName ?: "")
            if (success) EventBus.getDefault().post(AllEvent())
        }
    }

    companion object {
        private const val BUNDLE_NAME = "BUNDLE_NAME"

        fun newInstance(name: String?): CollectionFragment {
            val fragment = CollectionFragment()
            val args = Bundle()
            args.putString(BUNDLE_NAME, name)
            fragment.arguments = args
            return fragment
        }
    }
}