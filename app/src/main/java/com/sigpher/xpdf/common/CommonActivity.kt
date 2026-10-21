package com.sigpher.xpdf.common

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.sigpher.xpdf.common.utils.NotchUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * @author sigpher
 */
abstract class CommonActivity : AppCompatActivity(), CoroutineScope {

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    protected var toolbar: Toolbar? = null

    @LayoutRes
    protected abstract fun layoutId(): Int

    protected abstract fun createToolbar(): Toolbar?

    /**
     * ViewBinding 内容视图。返回非空时优先于 [layoutId]，便于逐步迁移到 ViewBinding。
     */
    protected open fun bindContentView(): View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindingRoot = bindContentView()
        if (bindingRoot != null) setContentView(bindingRoot) else setContentView(layoutId())
        toolbar = createToolbar()
        toolbar?.run {
            setSupportActionBar(this)
            UiManager.setStatusBar(this@CommonActivity, this)
        }
        if (isAdaptNotch()) {
            NotchUtils.checkNotch(window)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    open fun isAdaptNotch(): Boolean = false

    companion object {
        init {
            // 设置启用 5.0 以下版本对矢量图形的支持
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}