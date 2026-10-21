package com.sigpher.xpdf.filepicker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.sigpher.base.impl.OnClickListenerImpl
import com.sigpher.base.impl.TextWatcherImpl
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.CommonFragment
import com.sigpher.xpdf.common.DialogManager
import com.sigpher.xpdf.common.GroupingAdapter
import com.sigpher.xpdf.common.UiManager
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.StringUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.util.*
import com.sigpher.xpdf.databinding.AppFragmentViewAllBinding

/**
 * A simple [Fragment] subclass.
 */
class ViewAllFragment : CommonFragment(), IViewAllView, ViewAllAdapter.Callback {

    var adapter: ViewAllAdapter? = null
    var selectList: MutableList<String> = ArrayList()

    private lateinit var presenter: IViewAllPresenter
    private val importDialog: BottomSheetDialog by lazy(LazyThreadSafetyMode.NONE) {
        DialogManager.createImportDialog(activity) { etInput, btnLeft, btnCenter, btnRight ->
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
        DialogManager.createGroupingDialog(activity, false, object : GroupingAdapter.Callback {
            override fun onAddNewGroup() { // empty
            }

            override fun onAddToGroup(dir: String) {
                setResultBack(SelectActivity.TYPE_TO_EXIST, dir)
            }
        })
    }
    private var newGroupName: String? = null
    private val fileList: MutableList<File> = ArrayList()
    private val onClickListener = View.OnClickListener {
        val path = it.tag as String
        val index = binding.appLl.indexOfChild(it)
        val count = binding.appLl.childCount
        binding.appLl.removeViews(index + 1, count - index - 1)
        presenter.listFile(path)
    }
    private val dataObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            selectList.clear()
            (activity as SelectActivity).ibtnSelectAll.isSelected = false
            binding.appBtnImportCount.text = getString(R.string.app_import_count, 0)
            val enableSelectAll = adapter?.reset() ?: false
            (activity as SelectActivity).ibtnSelectAll.isEnabled = enableSelectAll
        }
    }

    private var _binding: AppFragmentViewAllBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View { // Inflate the layout for this fragment
        _binding = AppFragmentViewAllBinding.inflate(inflater, container, false)
        presenter = ViewAllPresenter(this)
        presenter.listStorage()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    fun setFocus() {
        binding.appHorizontalSv.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        (activity as SelectActivity).ibtnSelectAll.visibility = View.VISIBLE
        (activity as SelectActivity).ibtnSearch.visibility = View.VISIBLE
        (activity as SelectActivity).setRevealParam()
        binding.appHorizontalSv.isFocusableInTouchMode = true
        binding.appHorizontalSv.requestFocus()
        binding.appHorizontalSv.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                if (!presenter.canFinish()) {
                    (activity as SelectActivity).ibtnInverse.isSelected = false
                    (activity as SelectActivity).etSearch.setText("")
                    presenter.goBack()
                    return@OnKeyListener true
                }
            }
            false
        })
    }

    override fun onPause() {
        super.onPause()
        binding.appHorizontalSv.setOnKeyListener(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        adapter?.unregisterAdapterDataObserver(dataObserver)
        presenter.detach()
    }

    override fun onShowMessage(stringId: Int) {
        UiManager.showShort(stringId)
    }

    override fun onShowFileList(fileList: List<File>?) {
        this.fileList.clear()
        this.fileList.addAll(fileList!!)
        adapter?.notifyDataSetChanged()
    }

    override fun onShowPath(pathList: List<String>) {
        binding.appLl.removeViews(1, binding.appLl.childCount - 1)
        val inflater = LayoutInflater.from(context)
        val parent = StringBuilder(IViewAllPresenter.ROOT_PATH)
        for (dirName in pathList) {
            val tvPath = inflater.inflate(R.layout.app_include_tv_path, null) as TextView
            tvPath.setOnClickListener(onClickListener)
            tvPath.text = dirName
            parent.append("/").append(dirName) // 当前节点生成后即变成下一节点的父节点
            tvPath.tag = parent.toString()
            binding.appLl.addView(tvPath)
        }
    }

    override fun onDirTap(dirPath: String) {
        presenter.listFile(dirPath)
    }

    @SuppressLint("SetTextI18n")
    override fun onSelectResult(pathList: List<String>, total: Int) {
        if (total != 0) {
            (activity as SelectActivity).ibtnSelectAll.isSelected = pathList.size == total
        }
        binding.appBtnImportCount.text = getString(R.string.app_import_count, pathList.size)
        selectList.clear()
        selectList.addAll(pathList)
    }

    private fun initView() {
        binding.appIncludeTvPath.appTvPath.setOnClickListener(onClickListener)
        binding.appIncludeTvPath.appTvPath.tag = IViewAllPresenter.ROOT_PATH // 原始路径
        binding.appBtnImportCount.setOnClickListener(object : OnClickListenerImpl() {
            @SuppressLint("SetTextI18n")
            override fun onViewClick(v: View, interval: Long) {
                if (KeyboardUtils.isSoftInputVisible(activity)) {
                    KeyboardUtils.hideSoftInput(activity)
                }
                if (selectList.isEmpty()) {
                    UiManager.showShort(R.string.app_have_not_select)
                } else {
                    importDialog.show()
                }
            }
        })
        (activity as SelectActivity).ibtnSelectAll.setOnClickListener {
            it.isSelected = !it.isSelected
            adapter!!.selectAll(it.isSelected)
        }
        (activity as SelectActivity).ibtnSelectAll.isEnabled = false // XML 设置无效，只能这里初始化
        val lm: RecyclerView.LayoutManager = LinearLayoutManager(activity)
        binding.appRvSelect.layoutManager = lm
        adapter = ViewAllAdapter(fileList, (activity as SelectActivity).importeds, this)
        adapter?.registerAdapterDataObserver(dataObserver)
        binding.appRvSelect.adapter = adapter
    }

    private fun setResultBack(type: Int, groupName: String?) {
        val data = Intent()
        data.putStringArrayListExtra(SelectActivity.EXTRA_SELECTED, selectList as ArrayList<String>)
        data.putExtra(SelectActivity.EXTRA_TYPE, type)
        data.putExtra(SelectActivity.EXTRA_GROUP_NAME, groupName)
        activity.setResult(Activity.RESULT_OK, data)
        activity.finish()
    }

    companion object {
        fun newInstance(): Fragment {
            return ViewAllFragment()
        }
    }
}