@echo off
cd /d f:\Projects\xPDF
del build_check.bat build_check.log deps.bat deps.log metrics.ps1 pkgs.ps1 >nul 2>&1
git add -A
git commit -q -m "build(deps): 切换到瘦身后的 sigpher/Base 1.2.0" -m "- 依赖坐标改为 com.github.sigpher.Base:base:1.2.0（base/image/impl/util）" -m "- 基础库不再传递 retrofit/rxjava/autodispose/sonic/okdownload/jaeger/constraintlayout" -m "- android-pdf-viewer 去掉对 Base 的依赖，显式补齐 utilcodex 与 lifecycle-livedata-core" -m "- 根 build.gradle 增加 mavenLocal()，本地产物可优先命中" -m "- APK 5.18MB -> 5.12MB，dex 类数 2816 -> 2777" -m "- README 同步基础库坐标与体积"
git log --oneline -1
