@echo off
cd /d f:\Projects\xPDF
del build_check.bat build_check.log metrics.ps1 fix_names.ps1 rename.bat >nul 2>&1
git add -A
git commit -q -m "refactor(adapter): 统一列表基类、书封渲染与关键字过滤，去掉 *2 命名" -m "- 新增 common/adapter：BaseListAdapter（多选 + payload 局部刷新）、CoverGridRenderer（1~4 封面拼图共用）、KeywordFilter、ItemTypes" -m "- AllAdapter2 -> AllAdapter、CollectionAdapter2 -> CollectionAdapter，均改用 BaseListAdapter 与 CoverGridRenderer" -m "- GroupingAdapter 复用 CoverGridRenderer，删除重复的 setVisibility/setCover" -m "- SearchAdapter 改用 KeywordFilter，SearchAdapter/AbstractAdapter 的 viewType 常量统一到 ItemTypes" -m "- AllFragment2 -> AllFragment、CollectionFragment2 -> CollectionFragment" -m "- 删除 LiveDataBus（反射改 LiveData 私有字段），刷新全部改为 EventBus 的 UpdateAllEvent" -m "- 删除已随自研文件选择器废弃的 import/scan 对话框方法与三个布局" -m "- APK 5.119MB，dex 类数 2775"
git log --oneline -1
