package com.sigpher.xpdf.common

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * @author sigpher
 */
abstract class CommonFragment : Fragment(), View.OnTouchListener, CoroutineScope {

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    /**
     * 通过 getter 取宿主 Activity，避免把 Activity 存成字段造成 View 生命周期外的强引用
     */
    @get:JvmName("hostActivity")
    protected val activity: CommonActivity
        get() = requireActivity() as CommonActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        job = Job() // View 重建后需要新的 Job，否则协程作用域已被取消
        view.setOnTouchListener(this) // 防止因多个 Fragment 叠加导致点击穿透
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job.cancel()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return true // 防止因多个 Fragment 叠加导致点击穿透
    }

    override fun startActivity(intent: Intent?) { // 如果 BaseActivity 不为空则会使用 BaseActivity 中指定的过渡动画
        activity.startActivity(intent)
    }
}
