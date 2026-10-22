# xPDF

**xPDF** 是一款轻量、纯粹的 Android PDF 阅读器：不做云同步、不做在线书城，只专注于「把本地 PDF 读好」。

- 应用名：`xPDF`
- 包名（applicationId）：`com.sigpher.xpdf`
- 版本：`0.6.0`（versionCode `8`，本仓库首个公开发布版本为 `0.1.0`，版本线已重置，见「版本变更」）
- 最低支持：Android 7.0（API 24），compileSdk 37 / targetSdk 34
- 产物：**release**（约 12.0 MB，仅 arm64-v8a；其中 ML Kit 翻译引擎占约 7 MB）与 **debug** 两个变体（`app/build/outputs/apk/release/app-release.apk`）

## 功能特性

### 文档管理

- **三种视图**：最近阅读（Recent）、全部文档（All）、分组（Collection）；书封列表支持网格 / 线性两种布局，长按进入多选模式
- **导入 PDF**：通过系统文件选择器（SAF）多选导入，或选择一个目录批量导入其下全部 PDF；导入的文件会复制到应用私有目录，**无需任何存储权限**
- **分区存储合规**：不再索引外部存储，旧版本导入的失效条目会在启动时检测并引导重新导入（不删除任何记录）
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
- **长按取词**：长按正文自动取词并支持拖动手柄扩选，选区上方弹出「复制 / 高亮 / 下划线 / 笔记 / 翻译」
- **标注**：高亮与下划线保存在本地数据库，随页面缩放跟随显示，可再次长按该处编辑笔记或删除
- **翻译**：选中文本后调用 ML Kit 端侧模型翻译，支持切换目标语言、复制译文、失败重试与「用其他应用翻译」兜底

### 数据与备份

- **备份 / 恢复**：把书籍信息、分组、书签、阅读进度打包备份，随时恢复
- **本地优先**：书籍、分组、书签、进度、标注等数据仅保存在应用私有目录的 SQLite 数据库中，无统计上报；除翻译模型下载外，应用不发起任何网络请求

### 关于页

- 应用图标、名称、版本号与开源组件清单（纯展示，不含任何外部跳转）

## 环境要求

| 项目 | 版本 |
| --- | --- |
| JDK | **两套并存**：Gradle 守护进程跑 **JDK 17**，`java.toolchain` 指定 **JDK 27** 仅用于 `javac`（`io.legere:pdfiumandroid` 是 Java 21 字节码，而 Gradle 9.6 无法在 JDK 27 上运行） |
| Gradle | 9.6.0（随 wrapper 提供） |
| Android Gradle Plugin | 9.4.1 |
| Kotlin | 2.4.10（AGP 9 内置 Kotlin 支持，无需再应用 `org.jetbrains.kotlin.android`） |
| compileSdkVersion / targetSdkVersion | 37 / 34 |
| minSdkVersion | 24 |
| Android SDK | Platform 37（目录名 `android-37.0` 时需软链为 `android-37`）+ build-tools 37.0.0 |

## 构建指南

### 1. 准备 Android SDK

确保已安装 Android SDK Platform 37 与 build-tools 37.0.0，并设置环境变量 `ANDROID_SDK_ROOT`；同时准备好 JDK 17（守护进程）与 JDK 27（toolchain）。

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

工程固定产出 **arm64-v8a** 包（`ndk.abiFilters` 仅保留 `arm64-v8a`），debug 与 release 变体都可用。

```bash
# Windows
gradlew.bat :app:assembleRelease
gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleRelease
```

产物位于 `app/build/outputs/apk/release/app-release.apk`，仅包含 `lib/arm64-v8a/` 下的 so。Release 会执行混淆与资源压缩（`minifyEnabled true` + `shrinkResources true`），并把资源语言裁剪到中英文（`resourceConfigurations`）。

> 语言裁剪按 qualifier **精确匹配**：必须逐个写出带地区的限定符（`en`、`zh-rCN`、`zh-rTW`、`zh-rHK`、`zh-rMO`、`zh-rSG`），只写 `zh` 匹配不到 `values-zh-rCN` 等目录，中文会被整包裁掉、APK 里只剩 `en`。改动后可用 `aapt dump configurations app-release.apk` 确认输出里存在 `zh-rCN`。

> debug 变体 applicationId 为 `com.sigpher.xpdf.dev`，应用名 `xPDF Dev`，并接入 LeakCanary 2.14。
>
> 本机 `~/.gradle/gradle.properties` 若把 `org.gradle.java.home` 指向 JDK 27，会在解析 `app/build.gradle` 时报 `Unsupported class file major version 71`（Gradle 9.6 的 Groovy 不支持该字节码）。此时用 JDK 17 覆盖守护进程即可，`javac` 仍走 toolchain 27：
>
> ```bash
> gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot" :app:assembleRelease
> ```

### 4. 常见构建问题

- **依赖拉取失败**：`parallaxbacklayout`、`atv`、`BaseRecyclerViewAdapterHelper` 等老 AAR 只发布在已停服的 JCenter，工程通过 `https://maven.aliyun.com/repository/public` 镜像解析；若镜像不可用，可把对应 AAR 收进 `app/libs` 或本地 maven 仓库。PDF 引擎已换成 Maven Central 上的 `io.legere:pdfiumandroid:2.0.3`，不再需要 JCenter。
- **基础库已内联**：原作者的基础库 `com.aaron:base` 只发布在已停服的 JCenter 上，工程此前依赖瘦身 fork `com.github.sigpher.Base:base:1.2.0`；现在已整体移除该 AAR，把 app 实际用到的部分下沉到本仓库——`common/utils` 下的 `StatusBarUtils` / `TextWatcherImpl` / `OnClickListenerImpl` 内联自该库（[sigpher/Base](https://github.com/sigpher/Base)，Apache-2.0），`base_*` 颜色与字号资源迁入 `res/values`，封面加载改为 Glide 直接调用。原先由它传递的 `glide:4.9.0` 与 `utilcodex:1.23.7` 已在 `app/build.gradle` 显式声明，构建时不再需要 JitPack 现场编译或 `publishToMavenLocal`。
- **JDK 版本报错**：出现 `Unsupported class file major version 65` 说明 `javac` 跑在 JDK 17 上，需让 toolchain 找到 JDK 27；出现 `major version 71` 则是 Gradle 守护进程跑在了 JDK 27 上（见上一节，用 `-Dorg.gradle.java.home` 指回 JDK 17）。`~/.gradle/gradle.properties` 里的 `org.gradle.java.home` 优先级高于 `JAVA_HOME`。
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
│       │   │   ├── adapter/               # 列表基类与复用件：BaseListAdapter / CoverGridRenderer / KeywordFilter / ItemTypes
│       │   │   ├── bean/                  # GreenDAO 实体与数据类：PDF / Collection / Cover / RecentPDF / Annotation / Shortcut
│       │   │   ├── event/                 # EventBus 事件（唯一事件通道，LiveDataBus 已删除）
│       │   │   ├── greendao/              # GreenDAO 生成类（DaoMaster / DaoSession / *Dao）
│       │   │   ├── utils/                 # 工具类：PDF、图片、快捷方式、状态栏等
│       │   │   └── widgets/               # 自定义控件：BorderImageView / NewViewPager 等
│       │   ├── data/                      # SAF 导入管道（ImportPipeline：拷贝 + 封面 + 入库）
│       │   ├── main/                      # 主界面：最近 / 全部 / 分组 + 搜索
│       │   ├── preview/                   # 阅读器：翻页、缩放、书签、目录、夜间模式、长按取词、标注、翻译
│       │   └── settings/                  # 设置页
│       └── res/                           # 布局、样式、多语言（含 values-zh-rCN/rHK/rMO/rSG/rTW）
├── android-pdf-viewer/                    # PDF 渲染库（基于 barteksc/AndroidPdfViewer 3.2.0-beta.1 定制，含文本层与选区）
├── build.gradle / settings.gradle          # 根构建脚本，根工程名 xPDF
└── xPDF.plantuml                           # 组件图（可用 PlantUML 预览；包内类图已从仓库移除）
```

## 技术实现要点

- **架构**：MVP + Repository 雏形。`common/MVP.kt` 定义 `IView` / `IPresenter` 契约（Presenter 支持 `detach()`）；导入等耗时操作统一走 `data/ImportPipeline`（suspend + `Dispatchers.IO`）。
- **语言**：业务代码已全面 Kotlin 化（事件、Holder、装饰器、UiManager、DialogUtils、GroupingAdapter、Bookmark、PdfUtils、App、实体类与自定义 View 等）；仍保留 Java 的只剩 greenDAO 生成物（`DaoMaster`/`DaoSession`/`*Dao`/`MigrationHelper`/`UpdateOpenHelper`），以及 `android-pdf-viewer` 模块（与上游同构，便于合并）。
- **UI**：AppCompat + Material Components + `BaseRecyclerViewAdapterHelper`；视图绑定全部迁移到 **ViewBinding**（`kotlin-android-extensions` 已下线）；`ParallaxBackLayout` 提供侧滑返回；`RealtimeBlurView` 实现毛玻璃遮罩。
- **数据层**：GreenDAO 3.3.0（DAO 作为源码维护，不再用 gradle 插件生成），`schemaVersion 4`（新增 `ANNOTATION` 表）；升级策略是「纯增量优先」——`3 → 4` 只执行 `CREATE TABLE/INDEX IF NOT EXISTS`，只有更老的库才走 `MigrationHelper` 整表重建，且必须把所有实体 DAO 传入，否则未传入的表会被丢弃。历史数据迁移开关（`App.LEGACY_DATA_VERSION`）与 schema 版本解耦，避免每次表结构变化都重写整库记录。`DataManager` 维护书库内存缓存与「目录 → PDF」索引，启动时在 IO 线程异步加载。
- **事件通信**：EventBus 3.3.1。
- **PDF 渲染**：`android-pdf-viewer` 模块封装 PdfiumAndroidKt（`io.legere:pdfiumandroid:2.0.3`，持续维护、原生库随 NDK 构建），在原生 `PDFView` 基础上增加了滚动监听、显示监听、页切换监听、快照处理等定制能力。
- **文本层与选区**：`TextPageLoader` 负责打开 / 缓存 pdfium 文本页（LRU 3 页，淘汰即关闭），`TextSelectionManager` 负责长按命中、中英文扩词、首尾手柄拖动与选区绘制；pdfium 调用全部收敛到库内单线程执行器，选区与手柄绘制在文档坐标系内完成，`PDFView` 对外暴露 `textSelectable` / `onTextSelected` / `getPageAt` / `viewToPagePoint` / `pageRectToView` 等 API。
- **标注与翻译**：标注矩形以 0..1 页面相对坐标存库，经 `onPageOverlay` 回调按当前缩放乘算绘制（横向/纵向翻页通用）；翻译走 ML Kit `language-id` + `translate` 端侧模型，客户端懒初始化，模型按语言对下载后离线推理，失败降级为系统 `ACTION_PROCESS_TEXT`。
- **性能**：页面按需渲染 + 滚动节流、封面单次打开生成并压缩为 JPEG、书签角标位图复用、夜间模式免重载、最近阅读列表两次查询完成关联、文本页 LRU 复用、翻译客户端懒加载、LeakCanary 仅 debug 生效。

## 数据与存储

| 内容 | 位置 |
| --- | --- |
| 数据库 | 应用私有目录下 `xpdf.db`（GreenDAO，含书籍、分组、书签、封面、最近阅读、标注 `ANNOTATION`） |
| PDF 副本 | `getExternalFilesDir/pdfs`（SAF 导入时复制，文件名冲突自动加序号） |
| 封面 | 应用私有目录下的 `<书名>.jpg`（JPEG，宽度 360） |
| 备份文件 | `/storage/emulated/0/Android/data/com.sigpher.xpdf/files` |
| 设置项 | SharedPreferences，`Settings.kt` 首次读取后常驻内存（音量键翻页、点击翻页、状态栏、屏幕常亮、线性布局、快捷缩放、最近数量、翻译目标语言等） |
| 文件共享 | `FileProvider`，authority 为 `${applicationId}.fileprovider`，路径已收窄为 `external-files-path` + `cache-path` |

> 由于 applicationId 已改为 `com.sigpher.xpdf`，它被视为全新应用：无法覆盖安装旧版本，旧版本的应用数据与备份也不通用。

## 权限说明

本应用**不申请存储权限**：导入走系统文件选择器（SAF），文件复制到应用私有目录后所有读写都在私有目录内完成。

> 3.0.0 起移除了统计/崩溃上报所需的网络权限；当前版本仅为「翻译模型按需下载」声明 `INTERNET`，阅读、标注与翻译推理本身完全在本地完成。targetSdk 34 起不再声明 `READ/WRITE_EXTERNAL_STORAGE`。

## 版本变更

### 0.6.0（versionCode 8）

新增「长按取词」，并把标注与翻译接进阅读流程：

- **长按取词**：长按正文命中字词并按中文/拉丁文语义自动扩展，可拖首尾手柄调整选区；选区绘制在文档坐标系内，缩放、翻页时位置不漂移；点击空白或返回键退出
- **选区操作栏**：复制 / 高亮 / 下划线 / 笔记 / 翻译，随选区锚点浮动并做屏幕边界收敛
- **标注**：新增 greenDAO `Annotation` 表（schema 4），矩形按 0..1 页面相对坐标存储，重开文档仍在原位；再次长按已标注处可写笔记或删除；删除文档/分组时级联清理
- **翻译**：接入 ML Kit `language-id` + `translate` 端侧模型，译文卡片支持语言方向、目标语言切换（记忆）、重试、复制译文与「用其他应用翻译」兜底；仅翻译模型下载使用网络，因此重新声明 `INTERNET` 权限
- **引擎**（`android-pdf-viewer`）：新增文本层 `TextPageLoader`（LRU 缓存 3 页文本页）与 `TextSelectionManager`（pdfium 调用收敛到单线程）；`PDFView` 新增 `textSelectable` / `onTextSelected` / `onPageOverlay` 等 API，标注叠加不再受 `onDrawAll` 页面偏移影响
- **修复打开变慢**：schema 版本不再复用「历史数据迁移」开关（此前每次 schema +1 都会在主线程重写整库 PDF 记录并重置缩放）；3→4 改为增量 `CREATE TABLE IF NOT EXISTS`，不再重建全表；ML Kit 客户端改为首次翻译时才初始化
- 体积：4.69 MB → **12.03 MB**（`libtranslate_jni.so` + `liblanguage_id_l2c_jni.so` 约 7.3 MB）

### 0.5.0（versionCode 7）

PDF 引擎与构建工具链升级（含破坏性变更，升级前请阅读第一、二条）：

- **最低系统要求提升到 Android 7.0（API 24）**：新的 pdfium 绑定库声明 `minSdk 24`，Android 5.0/5.1 设备无法再安装
- **PDF 引擎替换**：`com.github.barteksc:pdfium-android:1.9.0`（2019 年构建、上游已归档）→ **`io.legere:pdfiumandroid:2.0.3`**（Maven Central，持续维护，原生库随 NDK 从源码构建）
  - 原生库从 modpdfium + modft2 + modpng + libc++_shared + jniPdfium（≈6.9 MB）缩减为 libpdfium + libpdfiumandroid（≈5.06 MB）
  - 接口迁移：`PdfiumCore(context)` → `PdfiumCore()`；`getPageSize` → `getPageWidthPoint` / `getPageHeightPoint`；`mapRectToDevice` 返回 `Rect` 并内部转 `RectF`；书签/链接/元数据类型迁到 `io.legere.pdfiumandroid.api`
  - `PdfDocument.Bookmark` 的 `pageIdx` 由 `int` 变为 `long`，`hasChildren()` 改为判断 `children` 是否为空；app 内同名 `Bookmark` 实体通过 `import ... as PdfBookmark` 区分
- **工具链**：Gradle 8.7 → **9.6.0**、AGP 8.5.2 → **9.4.1**、Kotlin 1.9.24 → **2.4.10**、compileSdk 34 → **37**
  - AGP 9 内置 Kotlin 支持，移除 `org.jetbrains.kotlin.android` 插件；`kotlinOptions` 改为 `kotlin { compilerOptions { … } }`
  - AGP 9 的优化资源裁剪要求非 final 资源 ID：开启 `android.nonFinalResIds=true`，并把 4 处 `const val R.layout.*` 改为 `val`
  - `io.legere:pdfiumandroid` 的 class 文件是 Java 21 字节码，而 Gradle 9.6 不能在 JDK 27 上运行：最终采用 **Gradle 跑在 JDK 17 + Java toolchain 27 编译**
  - `.so` 显式启用 `useLegacyPackaging`（AGP 9 默认不压缩，会让 APK 从 4.47 MB 涨到 6.96 MB）
  - 移除已废弃的 `jcenter()` 仓库与 `task clean` 写法
- 混淆规则：`-keep class com.shockwave.**` → `-keep class io.legere.**`
- 关于页「开源组件」增加 PdfiumAndroidKt
- 体积：APK 5,428,846 → **4,691,533 字节（4.47 MB）**

### 0.4.0（versionCode 6）

greenDAO 实体类 Kotlin 化，功能与界面无变化（建议覆盖安装后确认书库数据完整）：

- **Kotlin 化 3 个实体**（336 行 Java → 238 行 Kotlin）：`bean/PDF`、`bean/Collection`、`bean/RecentPDF`
- **可空性对齐存储层**：所有 `String` 属性声明为 `String?`——DAO 读取时会把 NULL 列原样回填（`entity.setCover(cursor.isNull(...) ? null : ...)`），若声明为非空，Kotlin setter 的入参校验会在读到历史脏数据时抛异常；`id` 保持 `Long?`（`insertOrReplace` 依赖 id 为 null 判断新增）
- **三套构造器齐备**：无参（DAO / Gson 使用）、`@Generated` 全参（`PDFDao.readEntity` 等直接调用）、`Parcel`
- **Parcelable 手写而非 `@Parcelize`**：读写顺序与 Java 版逐字一致，避免生成顺序变化导致升级后旧状态恢复错位；`CREATOR` 以 `@JvmField` 暴露
- **混淆规则不变**：属性名即列名 / JSON 字段名；`-keepclassmembers class com.sigpher.xpdf.common.bean.** { <fields>; }` 继续保证 Gson 备份可跨版本读取
- **仍保留 Java**：5 个 greenDAO 生成文件（`*Dao` / `DaoMaster` / `DaoSession`）、`MigrationHelper`、`UpdateOpenHelper`
- 体积：APK 5,428,748 → 5,428,846 字节

### 0.3.1（versionCode 5）

纯代码现代化（Java → Kotlin），功能与界面无变化：

- **Kotlin 化 5 个文件**（560 行 Java → 412 行 Kotlin）：
  - `common/widgets/NewViewPager`：保留 `(Context)` / `(Context, AttributeSet)` 构造器；`setScrollable(Boolean)` 仍为普通函数（`MainActivity` 以函数形式调用）
  - `common/widgets/BorderImageView`：常量与 `Paint` 初始化顺序保持；`dp2px` 返回值显式 `toFloat()`；边框绘制位置与原有实现逐像素一致
  - `common/widgets/ImageTextView`：15 组 getter/setter 收敛为属性；`setIconLeft/Right/Top/Bottom(Int)` 重载保留，调用点 `setIconTop(R.drawable.x)` 不受影响
  - `common/bean/Cover`：字段改为 `@JvmField`，保持 Java 字段访问语义
  - `common/DataManager`：改为 `object` 单例，`synchronized(this)`；对外仍提供 `getPdfList()` / `getCoverList()` 等函数形式，调用点零改动；列表 getter 返回 `MutableList` 以保持原平台类型的可变行为
- **保留 Java**：greenDAO 生成的 `*Dao` / `DaoMaster` / `DaoSession`（schema 变更会重新生成）、`MigrationHelper`（反射 + 变参泛型）、`PDF` / `Collection` / `RecentPDF` 实体（`@Entity` + Parcelable + 混淆字段名约束），以及 `android-pdf-viewer` 模块（保持与上游同构便于合并）
- 体积：APK 5,428,801 → 5,428,748 字节

### 0.3.0（versionCode 4）

修复阅读器「目录」页书签重复叠加的问题，并清理依赖与构建配置（功能集合不变）：

- **修复目录书签叠加 / 重复建树**：`ContentFragment.initContent()` 原先在书签循环体内每次都新建 `TreeNode.root()` 与 `AndroidTreeView` 并 `addView`，且从不清理容器；而 `PreviewActivity.initContentAndBookmark()` 每次重新加载文档都会调用 `update()`，导致每刷新一次就多出一整棵树、同一条书签重复堆叠。现改为渲染前 `removeAllViews()`，所有书签共用一棵树、只挂一个 `AndroidTreeView`，顶层与子节点共用 `buildNode()` 递归构建
- **修复视图未就绪丢内容**：`update()` 早于 `onViewCreated` 到达时 binding 为 null 会崩溃；现在先缓存数据，`onViewCreated` 补一次渲染，`onDestroyView` 先摘树再释放 binding
- **依赖清理**：移除 `androidx.multidex`（minSdk 21 起系统原生支持 multidex，同时删除 `MultiDex.install` 与 `multiDexEnabled`）与重复声明的 `kotlinx-coroutines-core`（由 `coroutines-android` 传递提供）
- **构建配置**：`packagingOptions` → `packaging`（AGP 8 废弃写法，`packaging` 代替）；删除模块级冗余 `repositories`；`gradle.properties` 开启 `org.gradle.parallel` / `org.gradle.caching`，并注明 Jetifier 不能关闭的原因（`parallaxbacklayout 1.1.9` 仍引用 `android.support.v4.view.ViewCompat` 与 `android.support.annotation.IntDef`）
- **混淆规则精简**：删除 WebView、okhttp/okio/squareup/findbugs 等已无对应依赖的死规则，保留 pdfium / greenDAO / Gson / EventBus / Glide / RenderScript 规则
- 体积：APK 5,435,511 → 5,428,801 字节（约 5.18 MB）

### 0.2.0（versionCode 3）

首个「可正常使用」的版本，修复了 0.1.x 在真机上的一批崩溃，并补齐中文与图标：

- **修复启动崩溃**：greenDAO 的 `DaoConfig` 用 `Class.forName(daoClass.getName() + "$Properties")` 反射取字段，R8 保留了内部类原名却重命名了 DAO 类，抛 `DaoException: Could not init DAOConfig`；用 `-keepnames` 固定 DAO 类名并保留 `createTable` / `dropTable`
- **修复空书库崩溃**：`BaseQuickAdapter.getEmptyView()` 返回内部 `FrameLayout` 容器，原代码把它当布局根 bind，抛 `ClassCastException`；改为持有自己 inflate 的 binding
- **修复导入后崩溃（且之后每次启动都崩）**：BRVAH 用反射读泛型参数实例化 `ViewHolder`，R8 合并基类后退化成通用 `BaseViewHolder`，`convert()` 的强制转换抛 `ClassCastException`；改为抽象工厂 `createHolder()` 自建
- **修复打开 PDF 崩溃**：书签 JSON 用匿名 `TypeToken<List<Bookmark>>` 解析，R8 合并匿名内部类后泛型签名丢失，抛 `Missing type parameter`；改用 `Array<Bookmark>::class.java` 解析
- **修复封面不生成**：`PdfUtils.renderPage` 用 `RGB_565` 位图调用 `PdfRenderer.Page.render`，系统抛 `Unsupported pixel format` 且异常被吞；改用 `ARGB_8888`
- **修复界面始终英文**：`resourceConfigurations` 按 qualifier 精确匹配，只写 `zh` 匹配不到 `values-zh-rCN` 等目录，中文被整包裁掉；改为逐个列出 `zh-rCN` / `zh-rTW` / `zh-rHK` / `zh-rMO` / `zh-rSG`
- **新增启动图标**：橙色 xPDF 设计，改用自适应图标（API 26+）+ 多密度 WebP，debug 变体带 Dev 角标
- **书签 / 备份 JSON 字段名保留**：Gson 按字段名读写，新增 `<fields>` 保留规则，避免升级后旧书签、旧备份读不回来

### 0.1.1（versionCode 2）

依赖瘦身与构建简化，功能与界面无变化：

- **移除 Base AAR**：不再依赖 `com.github.sigpher.Base:base:1.2.0`，把 app 实际用到的部分下沉到本仓库——`common/utils` 下的 `StatusBarUtils` / `TextWatcherImpl` / `OnClickListenerImpl` 内联自该库（Apache-2.0），`base_*` 颜色与字号迁入 `res/values`，构建不再需要 JitPack 或 `publishToMavenLocal`
- **显式声明依赖**：原本由 Base 传递的 `glide:4.9.0`（runtime）与 `utilcodex:1.23.7` 改为在 `app/build.gradle` 直接声明；封面加载由 `ImageLoader` 封装改为 Glide 直接调用，显示行为不变（无占位图、无淡入、默认磁盘缓存）
- **清理**：删除 `BookmarkFragment` 无用 import、混淆规则中 `RecentPDFEvent` 的 `-keep`（data class，无反射），补充 `-dontwarn com.bumptech.glide.**`
- **修复启动崩溃（release 混淆）**：greenDAO 的 `DaoConfig` 用 `Class.forName(daoClass.getName() + "$Properties")` 反射取字段，而 R8 只保留了 `$Properties` 内部类原名、却把 DAO 类重命名为 `a.b`，运行时拼出的 `a.b$Properties` 不存在，抛 `DaoException: Could not init DAOConfig`；现用 `-keepnames` 固定 DAO 类名，并保留 `createTable` / `dropTable`（`MigrationHelper` 反射调用）
- **修复空列表崩溃**：`BaseQuickAdapter.getEmptyView()` 返回的是内部 `FrameLayout` 容器，`BaseListAdapter` 曾把它当布局根直接 `bind`，空书库时抛 `ClassCastException: FrameLayout cannot be cast to RelativeLayout`；改为持有自己 inflate 的 binding
- **修复导入后崩溃**：BRVAH 默认用反射读取适配器泛型参数来实例化 `ViewHolder`，R8 把 `BaseListAdapter` 合并优化后反射失败、退化成通用 `BaseViewHolder`，`convert(helper: CollectionHolder, …)` 的强制转换抛 `ClassCastException`（一导入书籍就崩，且之后每次启动都崩）；改为在基类里用抽象工厂 `createHolder()` 自行创建
- **修复打开 PDF 崩溃**：书签 JSON 用匿名 `TypeToken<List<Bookmark>>` 解析，R8 合并匿名内部类后泛型签名丢失，抛 `RuntimeException: Missing type parameter.`；改用 `Array<Bookmark>::class.java` 解析
- **修复书签/备份 JSON 字段名被混淆**：Gson 按字段名读写，新增 `<fields>` 保留规则，避免升级后旧书签、旧备份读不回来
- **修复封面不生成**：`PdfUtils.renderPage` 用 `RGB_565` 位图调用 `PdfRenderer.Page.render`，系统抛 `Unsupported pixel format` 且异常被吞，封面字段存成空值、书库一直显示占位图；改用 `ARGB_8888`
- 体积：APK 5,369,260 → 5,369,442 字节（dex 类数 2566 → 2563）

### 0.1.0（versionCode 1）

本仓库（[sigpher/xPDF](https://github.com/sigpher/xPDF)）首个公开发布版本。版本线由此前的 `3.1.0`（versionCode 34）重置为 `0.1.0`（versionCode 1），相对原 `3.0.0` 一次性完成工具链、性能、体积与架构四类改造，功能集合不变（不新增业务功能）。

**构建工具链**

- Gradle 5.4.1 → 8.7、AGP 3.4.1 → 8.5.2、Kotlin 1.3.61 → 1.9.24、JDK 8 → 17、compileSdk/targetSdk 29/28 → 34
- 移除 GreenDAO / ButterKnife / Bintray 插件与残留配置，恢复 debug 变体（LeakCanary 2.14 仅 debug）
- `kotlin-android-extensions` 下线，28 个文件迁移到 ViewBinding
- 基础库切到瘦身后的 `com.github.sigpher:Base:1.2.0`（AAR 25 KB，去掉 retrofit/rxjava/autodispose/sonic/okdownload/jaeger）

**性能**

- 书库启动改为 IO 异步加载，主线程不再同步查库；最近阅读列表由 N+1 查询改为两次查询 + HashMap 关联
- 目录 → PDF 建立索引，按分组取书不再全表遍历 + 每次排序
- 阅读器：夜间模式改为色彩矩阵即时切换（不再重载文档）、书签角标位图解码一次、滚动渲染 24ms 节流、封面单次打开 + JPEG 80 + 关闭 fd
- `Settings` 常驻内存，翻页/绘制路径不再触达 SharedPreferences

**架构**

- 新增 `data/ImportPipeline`（SAF 导入管道）、`data/PreviewRepository`（进度持久化）、`common/adapter`（`BaseListAdapter`/`CoverGridRenderer`/`KeywordFilter`/`ItemTypes`）
- 删除基于反射的 `LiveDataBus`，统一走 EventBus；删除自研文件浏览（`filepicker` 包 13 个文件）与死布局/死方法
- `IPresenter` 支持 `detach()`；修复关于页开源组件清单因数组长度不一致而不显示的问题
- 业务代码 Kotlin 化：事件、Holder、装饰器、`UiManager`、`DialogUtils`、`GroupingAdapter`、`Bookmark`、`PdfUtils`、`App`、`Base64`（改用 `android.util.Base64`）

**合规与存储**

- 导入改为系统文件选择器（多选）+ 目录树批量导入，文件复制到应用私有目录，**不再申请任何存储权限**
- 旧版本录入的外部路径在启动时后台检测，失效时一次性引导重新导入（不删除记录）
- `FileProvider` 路径收窄为 `external-files-path` + `cache-path`；四大组件补齐 `exported`，`PendingIntent` 补 `FLAG_IMMUTABLE`

**体积**

- APK 5.6 MB → 5.12 MB；dex 类数 3422 → 2781

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

- **导入后需占用一份副本空间**：为满足分区存储要求，导入的 PDF 会复制到应用私有目录，删除应用即一并删除；卸载前请先备份书单。
- **仅支持本地文件**：不支持 `content://` 之外的网络资源，也不支持外部应用以非常规资源方式打开。
- **快捷方式门槛**：Android 7.1 以下不支持快捷键，Android 8.0 以下不支持创建桌面快捷方式。
- **构建依赖历史仓库**：见「常见构建问题」，部分依赖仍托管在已停服的 JCenter，需借助镜像或缓存。
- **签名策略**：release 读取 `local.properties` 中的签名配置（缺失时退回未签名构建），debug 使用默认 debug key。
- **标注仅本应用可见**：pdfium 2.0.3 未暴露 `FPDFAnnot_*`，标注存本地数据库并叠加渲染，不写入 PDF 文件，其他阅读器看不到。
- **翻译首次需联网**：ML Kit 端侧模型按语言对下载（依赖 Google Play 服务），无 GMS 设备只能走「用其他应用翻译」；模型就绪后完全离线。
- **扫描版 PDF 不支持取词**：无文本层页面长按只提示「该页无可提取文本」，未引入 OCR。
- **备份不含标注**：备份文件只序列化书籍信息（含书签、进度），高亮 / 下划线 / 笔记不参与备份与恢复。

## 第三方组件

感谢以下开源项目（应用「关于 → 开源组件」页面内也有同样清单）：

| 组件 | 作者 | 用途 |
| --- | --- | --- |
| AndroidPdfViewer / PdfiumAndroid | barteksc | PDF 渲染（视图层） |
| PdfiumAndroidKt | legere / johngray1965 | PDF 渲染（pdfium 绑定，2.0.3） |
| ML Kit Language ID | Google | 端侧语种识别（选择翻译源语言） |
| ML Kit Translation | Google | 端侧神经机器翻译（模型按语言对按需下载，离线推理） |
| AndroidTreeView | bmelnychuk | 树形目录视图 |
| RealtimeBlurView | mmin18 | 实时模糊遮罩 |
| ParallaxBackLayout | anzewei | 侧滑返回 |
| greenDAO | greenrobot | ORM |
| EventBus | greenrobot | 事件总线 |
| Glide | bumptech | 图片加载 |
| StatusBarUtil | Jaeger | 状态栏适配 |
| RxJava / RxAndroid | ReactiveX | 异步与响应式 |
| BaseRecyclerViewAdapterHelper | CymChad | RecyclerView 适配器 |
| AndroidUtilCode | Blankj | 通用工具类 |
| Base（已内联） | AaronZheng / sigpher | 状态栏工具与 TextWatcher / 点击防抖基类 |

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。`android-pdf-viewer` 模块派生自 [barteksc/AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer)（Apache-2.0），版权归原作者所有。

## 联系与反馈

- GitHub：<https://github.com/sigpher/xPDF>
- 问题反馈：<https://github.com/sigpher/xPDF/issues>
- 邮箱：<sigpher@gmail.com>
