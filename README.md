# moban-plugin-demo · 最小化标准 APK 书源插件

[墨伴](https://github.com/Jason-wam) 的**标准 APK 形态**书源插件示例：带 AndroidManifest
与资源表（插件图标来自 manifest 的 `android:icon`），插件描述符放在 `assets/plugin.json`，
class 与 dex 打包为普通 APK——但**不安装到系统**，由宿主以「免安装容器」方式加载。

与之相对的「容器式 APK」（自合成 zip，无 manifest）见墨伴主仓库
`plugins/sample-book-source` 的 `buildPluginApk` 任务，两者宿主均支持。

## 构建

```bash
gradlew packagePlugin
# 产物：build/dist/demo-apk-plugin.apk（release 未签名 APK，宿主加载不校验签名）
```

## 导入宿主

1. 打开墨伴 → 书源管理页 → **插件包** → 「导入插件（jar/apk）」选择本 APK；
2. 或放进 `filesDir/book-plugins/`（`adb push` 后启动时自动扫描入库）；
3. 导入后插件图标显示为橙色书本，书源「演示APK书源」出现在发现页，可搜索/看正文。

## plugin.json 字段（assets/plugin.json）

| 字段 | 说明 |
|---|---|
| `id` | 插件唯一 id（包名风格，与 `PluginMetadata.id` 一致） |
| `name` / `version` / `author` / `description` | 展示信息 |
| `entryClass` | 插件入口类 FQN，必须有无参构造 |
| `minApiVersion` | 兼容的最低宿主契约版本（book-api `API_VERSION`） |

## 开发自己的插件

1. 参考本仓库：`settings.gradle.kts` 引入契约层（JitPack 坐标），`compileOnly` 依赖；
2. 实现 `BookSourcePlugin`（入口）+ `RemoteBookSource`（书源，方法全 suspend，
   失败返回 `Result.failure(SourceException.Xxx)`）；能力一律走 `host.http` / `host.cacheDir`；
3. `assets/plugin.json` 写好元数据，`AndroidManifest.xml` 配 `android:icon`；
4. AGP 9 注意：内置 Kotlin 读不了宿主 Kotlin 2.4 元数据，本仓库用 buildscript classpath
   注入 KGP 2.4.20（见 `build.gradle.kts`）；stdlib 已从 APK 运行时排除保持体积最小。

契约层 API 详见 [book-api 仓库](https://github.com/Jason-wam/book-api)。

## 调试：不装宿主也能调

**核心答案：逻辑调试完全不需要宿主 App。** 契约层是纯 JVM 接口，插件代码是纯 Kotlin，
本仓库提供了 `FakePluginHost`，三个层次由轻到重：

### ① JVM 单元测试（首选，秒级循环）

`src/test/kotlin/.../DemoBookSourceTest.kt` 演示了搜索 → 详情 → 目录 → 正文全链路测试：

```bash
gradlew test          # 命令行运行
```

在 Android Studio / IntelliJ 里直接 **Debug 运行测试类**，可在插件源码任意行打断点、
看变量、单步执行。真实站点开发时，把 `FakeHttp` 改成读取本地保存的 HTML/JSON 样本，
解析逻辑即可离线调试，不必反复请求站点。

### ② 设备快速循环：adb push + logcat

只把插件文件拷进宿主目录即可，**不是安装**，无需任何签名/安装流程：

```bash
gradlew packagePlugin
adb push build/dist/demo-apk-plugin.apk /data/local/tmp/demo.apk
adb shell run-as com.jason.any.reader cp /data/local/tmp/demo.apk files/book-plugins/
adb logcat | grep -i "DemoApkBookSource\|PluginManager"
```

重启 App（或重新导入）触发 `scanDirectory + loadAll`；插件里用 `host.log(tag, msg)`
打的日志会进 logcat，是运行期的"println 调试"通道。

### ③ 宿主进程内断点调试（进阶，可选）

让宿主跑 debug 包 → Android Studio **Attach Debugger to Android Process** 附加宿主进程。
由于插件类由 `DexClassLoader` 动态加载，IDE 在类加载前无法解析断点，需要：
断点打在触发插件调用的宿主代码上（如 `PluginManager`/`RemoteSources`），命中后用
Evaluate/Watch 检查插件对象；或在插件源码上先打断点（IDE 提示 "no executable code"
属正常），待类加载后重新触发调用即可解析。此法调试**跨 ClassLoader 交互**（类型转换、
单例共享）最直观，日常逻辑用 ① 就够了。

### 排错速查

| 现象 | 原因 |
|---|---|
| 导入后书源不出现 | `plugin.json` 路径/`entryClass` 拼写错误；入口类缺无参构造 |
| `ClassCastException` / 单例分裂 | 契约层被打进了插件（必须 `compileOnly`） |
| 加载即崩、AbstractMethodError | 插件 `minApiVersion` 高于宿主，或宿主与插件 book-api 版本不匹配 |
| 网络行为与预期不符 | 插件自建了网络栈；统一改走 `host.http` 共享 Cookie/缓存 |
