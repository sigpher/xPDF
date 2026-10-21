# xPDF

**xPDF** 是一款轻量、纯粹的 Android PDF 阅读器：不做云同步、不做在线书城，只专注于「把本地 PDF 读好」。

- 应用名：`xPDF`
- 包名（applicationId）：`com.sigpher.xpdf`
- 版本：`3.0.0`（versionCode `33`）
- 最低支持：Android 5.0（API 21），targetSdk 28
- 产物：仅 **release** 变体、仅 **arm64-v8a**（`app/build/outputs/apk/release/app-release.apk`）

## 功能特性

### 文档管理

- **三种视图**：最近阅读（Recent）、全部文档（All）、分组（Collection）；书封列表支持网格 / 线性两种布局，长按进入多选模式
- **导入 PDF**：内置文件选择器支持多选导入，可指定导入到已有分组或新建分组
- **全盘扫描**：自动扫描存储卡中的 PDF 文件，扫描过程可随时停止并显示实时进度
- **搜索**：按文件名快速检索已导入文档
- **分组管理**：支持「按文件夹名分组」和「自定义命名」两种方式，可对已有分组重新分组
- **桌面快捷方式**：为指定 PDF 在桌面创建直达图标（单次可选数量有上限，Android 8.0 以下不支持创建）
- **阅读进度**：自动记录每本书的阅读进度与最近阅读时间，最近列表展示数量可配置（3/6/9/12/15/不限）

### 阅读

- **横向 / 纵向翻页**，支持点击屏幕翻页、音量键翻页
- **缩放**：25% / 50% / 75% / 100% / 200% / 300% 快捷缩放，也支持手势缩放与滚动
- **目录与书签**：解析 PDF 大纲目录，支持添加 / 删除书签并快速跳转
- **加密文档**：支持打开需要密码的 PDF，密码错误、解析失败均有明确提示
- **夜间模式**：颜色反转的深色阅读
- **阅读辅助**：屏幕常亮、显示状态栏、锁定横屏

### 数据与备份

- **备份 / 恢复**：把书籍信息、分组、书签、阅读进度打包备份，随时恢复
- **本地优先**：书籍、分组、书签、进度等数据仅保存在应用私有目录的 SQLite 数据库中，应用**不申请网络权限**，无统计上报

### 关于页

- 应用图标、名称、版本号与开源组件清单（纯展示，不含任何外部跳转）

## 环境要求

| 项目 | 版本 |
| --- | --- |
| JDK | **8**（Gradle 5.4.1 兼容 JDK 8~12，推荐 8） |
| Gradle | 5.4.1（随 wrapper 提供） |
| Android Gradle Plugin | 3.4.1 |
| Kotlin | 1.3.61 |
| compileSdkVersion | 29 |
| minSdkVersion | 21 |
| Android SDK | Platform 29 + 对应 build-tools（AGP 3.4.1 默认 28.0.3） |

## 构建指南

### 1. 准备 Android SDK

确保已安装 Android SDK Platform 29 与对应 build-tools，并设置环境变量 `ANDROID_SDK_ROOT`。

### 2. 配置 `local.properties`

工程根目录下的 `local.properties` **必须存在**，否则 Gradle 在配置阶段就会失败——`app/build.gradle` 的 `signingConfigs` 会直接读取它，release 包使用其中配置的签名。

```properties
# Android SDK 路径
sdk.dir=D\:\\android_sdk

# release 签名配置（文件名、密码、别名）
STORE_FILE_NAME=your-keystore.jks
KEYSTORE_PASSWORD=your_store_password
STORE_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

> 本地只想验证编译时，可用调试 keystore 填入上述四项；请勿把真实签名信息提交到仓库。

### 3. 编译

工程已固定为「**只构建 arm64-v8a 的 release 包**」：`defaultConfig` 的 `ndk.abiFilters` 仅保留 `arm64-v8a`，并通过 `variantFilter` 忽略了 debug 变体，因此 `assembleDebug` 会直接报 *Task 'assembleDebug' not found*。

```bash
# Windows
gradlew.bat :app:assembleRelease

# macOS / Linux
./gradlew :app:assembleRelease
```

产物位于 `app/build/outputs/apk/release/app-release.apk`，仅包含 `lib/arm64-v8a/` 下的 so。Release 会执行混淆与资源压缩（`minifyEnabled true` + `shrinkResources true`），配置见 `app/proguard-rules.pro`。

> 如需临时在设备上调试，可注释 `app/build.gradle` 中的 `variantFilter` 代码块，重新启用 debug 变体（applicationId 为 `com.sigpher.xpdf.dev`，应用名 `xPDF Dev`）。

建议使用 Android Studio 打开工程，并将 Gradle JDK 指定为 JDK 8。

### 4. 常见构建问题

- **依赖拉取失败**：工程仍在使用 `jcenter()` 与 bintray 仓库（已停止服务），若无法解析依赖，可将其替换为 `mavenCentral()`、`google()` 或 `https://maven.aliyun.com/repository/public` 等镜像。
- **基础库依赖**：原作者的基础库 `com.aaron:base` 发布在已停服的 JCenter 上，工程已改用 fork 版本 `com.github.sigpher:Base:1.1.5-beta9`（源码见 [sigpher/Base](https://github.com/sigpher/Base)，包名 `com.sigpher.base`，由 JitPack 按 tag 构建发布）。首次构建会由 JitPack 现场编译，耗时略长；若 JitPack 不可用，可克隆该仓库后执行 `./gradlew :base:install` 安装到本地 `~/.m2`，并在根 `build.gradle` 的 `allprojects.repositories` 中加入 `mavenLocal()`。
- **JDK 版本报错**：出现 `Unsupported class file major version` 等错误时，确认 Gradle JDK 为 8。
- **签名文件不存在**：检查 `local.properties` 中 `STORE_FILE_NAME` 指向的文件是否在 `app/` 目录下（`file(...)` 以模块目录为基准解析）。

## 工程结构

```
xPDF/
├── app/                                  # 主应用模块（Java + Kotlin 混合）
│   ├── build.gradle                       # applicationId、签名、GreenDAO、混淆配置
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/sigpher/xpdf/
│       │   ├── about/                     # 关于页：版本信息、开源组件清单
│       │   ├── common/                    # 基础设施
│       │   │   ├── bean/                  # GreenDAO 实体与数据类：PDF / Collection / Cover / RecentPDF / Shortcut
│       │   │   ├── event/                 # EventBus 事件
│       │   │   ├── greendao/              # GreenDAO 生成类（DaoMaster / DaoSession / *Dao）
│       │   │   ├── utils/                 # 工具类：PDF、图片、快捷方式、状态栏等
│       │   │   └── widgets/               # 自定义控件：BorderImageView / NewViewPager 等
│       │   ├── filepicker/                # 文件选择、全盘扫描、导入
│       │   ├── main/                      # 主界面：最近 / 全部 / 分组 + 搜索
│       │   ├── preview/                   # 阅读器：翻页、缩放、书签、目录、夜间模式
│       │   └── settings/                  # 设置页
│       └── res/                           # 布局、样式、多语言（含 values-zh-rCN/rHK/rMO/rSG/rTW）
├── android-pdf-viewer/                    # PDF 渲染库（基于 barteksc/AndroidPdfViewer 3.2.0-beta.1 定制）
├── build.gradle / settings.gradle          # 根构建脚本，根工程名 xPDF
└── xPDF.plantuml                           # 组件图（可用 PlantUML 预览；包内类图已从仓库移除）
```

## 技术实现要点

- **架构**：MVP。`common/MVP.kt` 定义 `IView` / `IPresenter` 契约，各业务包内自带 `I*Contract` 接口与 `*Presenter` 实现。
- **UI**：AppCompat + Material Components + `BaseRecyclerViewAdapterHelper` 实现多布局列表；ButterKnife 负责视图绑定；`ParallaxBackLayout` 提供侧滑返回；`RealtimeBlurView` 实现毛玻璃遮罩。
- **数据层**：GreenDAO 3.2.2，`schemaVersion 3`，DAO 包为 `com.sigpher.xpdf.common.greendao`；`DBHelper` + `UpdateOpenHelper` + `MigrationHelper` 负责建库与升级迁移。
- **事件通信**：EventBus 3.1.1 + 自实现 `LiveDataBus`。
- **PDF 渲染**：`android-pdf-viewer` 模块封装 PdfiumAndroid（`com.github.barteksc:pdfium-android:1.9.0`），在原生 `PDFView` 基础上增加了滚动监听、显示监听、页切换监听、快照处理等定制能力。
- **性能**：页面按需渲染、封面异步加载与内存缓存、LeakCanary 检测内存泄漏。

## 数据与存储

| 内容 | 位置 |
| --- | --- |
| 数据库 | 应用私有目录下 `xpdf.db`（GreenDAO，含书籍、分组、书签、封面、最近阅读） |
| 备份文件 | `/storage/emulated/0/Android/data/com.sigpher.xpdf/files` |
| 设置项 | SharedPreferences（`Settings.kt`：音量键翻页、点击翻页、状态栏、屏幕常亮、线性布局、快捷缩放、最近数量等） |
| 文件共享 | `FileProvider`，authority 为 `${applicationId}.fileprovider`，路径配置见 `res/xml/app_file_paths.xml` |

> 由于 applicationId 已改为 `com.sigpher.xpdf`，它被视为全新应用：无法覆盖安装旧版本，旧版本的应用数据与备份也不通用。

## 权限说明

| 权限 | 用途 |
| --- | --- |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | 读取本地 PDF、导入文件与备份 |
| `com.android.launcher.permission.INSTALL_SHORTCUT` 等 | 创建 / 移除 / 查询桌面快捷方式 |

> 3.0.0 起不再申请 `INTERNET` 等网络权限，应用完全离线运行。

## 版本变更

### 3.0.0（versionCode 33）

定位收敛为「只做本地 PDF 阅读」，本版本为删除型更新：

- **移除联网能力**：删除友盟统计与崩溃上报 SDK（7 个 AAR）及初始化逻辑，移除 `INTERNET`、`ACCESS_NETWORK_STATE`、`ACCESS_WIFI_STATE` 权限，应用完全离线
- **移除自动滚屏**：阅读器底部工具栏不再有「自动滚动」按钮，相关速度滑杆、引导弹窗、播放动画与设置项一并删除
- **移除全局灰度**：删除实验性的全局灰度渲染开关
- **移除导出/裁剪图片**：「更多」面板中的「导出图片」「裁剪」入口及其缩放滑杆删除
- **关于页精简**：仅保留版本信息与开源组件清单，去掉介绍、反馈、源码、GitHub、评分、捐赠入口
- **工程清理**：删除 Bugly/Tinker 死配置（`config.gradle`、注释代码与混淆规则）、清理无引用字符串资源、移除阅读器中对 RxJava/AutoDispose 的依赖
- **设置页**：由 10 项精简为 7 项（音量键翻页、点击翻页、显示状态栏、屏幕常亮、线性布局、快捷缩放、最近数量）

> 由于移除了功能与权限，本版本与 2.x 不兼容，升级后无需迁移任何数据。

## 已知限制

- **未适配分区存储**：targetSdk 28，在 Android 11+ 上以兼容模式访问外部存储。
- **仅支持本地文件**：不支持 `content://` 之外的网络资源，也不支持外部应用以非常规资源方式打开。
- **快捷方式门槛**：Android 7.1 以下不支持快捷键，Android 8.0 以下不支持创建桌面快捷方式。
- **构建依赖历史仓库**：见「常见构建问题」，部分依赖仍托管在已停服的 JCenter，需借助镜像或缓存。
- **签名策略**：Debug 与 Release 共用 release 签名配置，属当前工程的临时约定，正式发布前请拆分。

## 第三方组件

感谢以下开源项目（应用「关于 → 开源组件」页面内也有同样清单）：

| 组件 | 作者 | 用途 |
| --- | --- | --- |
| AndroidPdfViewer / PdfiumAndroid | barteksc | PDF 渲染 |
| AndroidTreeView | bmelnychuk | 树形目录视图 |
| RealtimeBlurView | mmin18 | 实时模糊遮罩 |
| ParallaxBackLayout | anzewei | 侧滑返回 |
| greenDAO | greenrobot | ORM |
| EventBus | greenrobot | 事件总线 |
| ButterKnife | JakeWharton | 视图绑定 |
| Glide | bumptech | 图片加载 |
| StatusBarUtil | Jaeger | 状态栏适配 |
| RxJava / RxAndroid | ReactiveX | 异步与响应式 |
| BaseRecyclerViewAdapterHelper | CymChad | RecyclerView 适配器 |
| AndroidUtilCode | Blankj | 通用工具类 |
| Base（sigpher fork） | AaronZheng / sigpher | Activity 基类、图片加载、状态栏工具 |

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。`android-pdf-viewer` 模块派生自 [barteksc/AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer)（Apache-2.0），版权归原作者所有。

## 联系与反馈

- GitHub：<https://github.com/sigpher/xPDF>
- 问题反馈：<https://github.com/sigpher/xPDF/issues>
- 邮箱：<sigpher@gmail.com>
