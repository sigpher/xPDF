@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot
cd /d f:\Projects\xPDF
call .\gradlew.bat -Dorg.gradle.java.home="C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot" :android-pdf-viewer:compileReleaseJavaWithJavac :app:compileReleaseJavaWithJavac > build_check.log 2>&1
echo EXIT=%ERRORLEVEL% >> build_check.log
