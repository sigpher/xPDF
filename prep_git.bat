@echo off
cd /d f:\Projects\xPDF
del build_release.bat build_release.log >nul 2>&1
git add -A
git commit -q -m "release: 版本线重置为 0.1.0（versionCode 1）" -m "- versionName 3.1.0 -> 0.1.0，versionCode 34 -> 1，作为本仓库首个公开发布版本" -m "- README 顶部版本行与「版本变更」首段补版本线重置说明，并链接到 sigpher/xPDF" -m "- .gitignore 补充 *.keystore 与 keystore.properties" -m "- 移除临时脚本 commit.bat"
git tag -d v3.1.0
git tag -a v0.1.0 -m "xPDF 0.1.0"
echo ---TAGS---
git tag -l "v0.*"
echo ---LOG---
git log --oneline -2
echo ---STATUS---
git status --short
