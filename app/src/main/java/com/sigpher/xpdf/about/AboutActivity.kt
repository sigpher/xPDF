package com.sigpher.xpdf.about

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sigpher.xpdf.BuildConfig
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.CommonActivity
import com.github.anzewei.parallaxbacklayout.ParallaxBack
import com.sigpher.xpdf.databinding.AppActivityAboutBinding

@ParallaxBack
class AboutActivity : CommonActivity(), IAboutView {

    private lateinit var mPresenter: IAboutPresenter

    private val binding by lazy { AppActivityAboutBinding.inflate(layoutInflater) }

    override fun bindContentView() = binding.root

    override fun layoutId(): Int {
        return R.layout.app_activity_about
    }

    override fun createToolbar(): Toolbar? {
        return findViewById(R.id.app_toolbar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPresenter = AboutPresenter(this)
        initView()
        mPresenter.requestLibrary(
                AboutPresenter.Element.LIBRARY_NAME,
                AboutPresenter.Element.LIBRARY_AUTHOR,
                AboutPresenter.Element.LIBRARY_INTRODUCE
        )
    }

    /**
     * 标题栏返回键销毁活动
     */
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onShowLibrary(list: List<Library>) {
        initLibrary(list)
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        initToolbar()
        initVersionStatus()
    }

    private fun initToolbar() {
        supportActionBar?.run {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.app_ic_action_back_black)
        }
        toolbar?.setTitle(R.string.app_about)
    }

    @SuppressLint("SetTextI18n")
    private fun initVersionStatus() {
        val version = findViewById<TextView>(R.id.app_tv_version)
        val versionName = BuildConfig.VERSION_NAME
        version.text = "Version $versionName"
    }

    private fun <T> initLibrary(libraryList: List<T>) {
        val libraryManager = LinearLayoutManager(this)
        binding.appRvLibrary.layoutManager = libraryManager
        val libraryAdapter: LibraryAdapter<*> = LibraryAdapter(this, libraryList)
        binding.appRvLibrary.adapter = libraryAdapter
    }

    companion object {
        fun start(context: Context) {
            val starter = Intent(context, AboutActivity::class.java)
            context.startActivity(starter)
        }
    }
}