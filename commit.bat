@echo off
cd /d f:\Projects\xPDF
del build_check.bat build_check.log >nul 2>&1
git add -A
git commit -q -m "release: 3.1.0（versionCode 34）" -m "- app/build.gradle 升版本号" -m "- README 新增 3.1.0 版本变更（工具链/性能/架构/合规/体积五部分）" -m "- APK 5.12MB，dex 类数 2781"
git tag -a v3.1.0 -m "xPDF 3.1.0" 2>nul || echo tag already exists
copy /y app\build\outputs\apk\release\app-release.apk f:\xPDF-3.1.0.apk >nul
git log --oneline -3
