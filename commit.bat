@echo off
cd /d f:\Projects\xPDF
del build_check.bat build_check.log metrics.ps1 >nul 2>&1
git add -A
git commit -q -m "refactor(mvp): Presenter 增加 detach 解绑，修复关于页开源组件清单不显示" -m "- IPresenter 改为可解绑：detach() 后访问 view 会立即失败，MainActivity/AboutActivity 在 onDestroy 调用" -m "- AboutPresenter 三个组件数组长度原本不一致（10/11/11），导致列表永远不显示；顺带移除已删除的 RxJava/RxAndroid/StatusBarUtil 条目" -m "- 关于页清单改为 10 项并对齐作者"
git log --oneline -1
