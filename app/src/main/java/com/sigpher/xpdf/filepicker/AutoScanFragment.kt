package com.sigpher.xpdf.filepicker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sigpher.base.impl.OnClickListenerImpl
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.CommonFragment
import java.util.*
import com.sigpher.xpdf.databinding.AppFragmentAutoScanBinding

class AutoScanFragment : CommonFragment() {

    private var _binding: AppFragmentAutoScanBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View { // Inflate the layout for this fragment
        _binding = AppFragmentAutoScanBinding.inflate(inflater, container, false)
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

    override fun onResume() {
        super.onResume()
        (activity as SelectActivity).ibtnSelectAll.visibility = View.GONE
        (activity as SelectActivity).ibtnSearch.visibility = View.GONE
        view?.post { (activity as SelectActivity).closeSearchView() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            activity.finish()
        }
    }

    private fun initView() {
        binding.appBtnScan.setOnClickListener(object : OnClickListenerImpl() {
            override fun onViewClick(v: View, interval: Long) {
                ScanActivity.start(activity, (activity as SelectActivity).importeds as ArrayList<String?>, SelectActivity.REQUEST_CODE)
            }
        })
    }

    companion object {
        private const val REQUEST_CODE = 1
        fun newInstance(): Fragment {
            return AutoScanFragment()
        }
    }
}