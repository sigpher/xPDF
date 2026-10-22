package com.sigpher.xpdf.about

internal class AboutPresenter(view: IAboutView) : IAboutPresenter(view) {

    private val libraryList: MutableList<Library> = ArrayList()

    override fun requestLibrary(name: Array<String>, author: Array<String>, introduce: Array<String>) {
        if (name.isEmpty() || author.size != name.size || introduce.size != name.size) {
            return
        }
        libraryList.clear()
        for (i in name.indices) {
            libraryList.add(Library(name[i], author[i], introduce[i]))
        }
        view.onShowLibrary(libraryList)
    }

    /**
     * 开源组件清单，三个数组必须等长，顺序一一对应。
     */
    internal object Element {
        val LIBRARY_NAME = arrayOf(
                "AndroidTreeView",
                "RealtimeBlurView",
                "ParallaxBackLayout",
                "greenDAO",
                "EventBus",
                "Glide",
                "AndroidUtilCode",
                "Material Components",
                "AndroidPdfViewer / PdfiumAndroid",
                "Base（sigpher fork）")
        val LIBRARY_AUTHOR = arrayOf(
                "bmelnychuk",
                "mmin18",
                "anzewei",
                "greenrobot",
                "greenrobot",
                "bumptech",
                "blankj",
                "Google",
                "barteksc",
                "sigpher")
        val LIBRARY_INTRODUCE = arrayOf(
                "AndroidTreeView. TreeView implementation for android",
                "A realtime blurring overlay for Android (like iOS UIVisualEffectView)",
                "无需改动原有 activity，只需一个 annotation 即可实现任意方向的滑动返回",
                "greenDAO is a light & fast ORM solution for Android that maps objects to SQLite databases.",
                "Event bus for Android and Java that simplifies communication between Activities, Fragments, Threads, Services, etc.",
                "An image loading and caching library for Android focused on smooth scrolling",
                "Android developers' tool library, providing common utility classes",
                "Modular and customizable Material Design UI components for Android",
                "Android view for displaying PDFs rendered with PdfiumAndroid",
                "Activity/Fragment 基类、图片加载与状态栏工具（上游 AaronZheng9603/Base 的瘦身 fork）")
    }
}
