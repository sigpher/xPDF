@echo off
cd /d f:\Projects\xPDF
del build_check.bat build_check.log clean_build.bat metrics.ps1 >nul 2>&1
git add -A
git commit -q -m "refactor(common): 用 android.util.Base64 替换自带的 Base64 实现" -m "- 备份/恢复的编解码改用系统 API，删除 39KB 的 common/utils/Base64.java" -m "- APK 5.118MB -> 5.117MB，dex 类数 2775 -> 2772"
git log --oneline -1
