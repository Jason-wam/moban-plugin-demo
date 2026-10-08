// AGP 9 默认内置 Kotlin（2.2.x，读不了宿主 Kotlin 2.4 的元数据）；把与宿主一致的
// KGP 放到 classpath 上，AGP 会自动改用外部 Kotlin 编译器（不要直接 apply
// org.jetbrains.kotlin.android —— AGP 9 已禁止该插件）。
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    id("com.android.application") version "9.1.1"
}

android {
    namespace = "com.jason.reader.plugin.demo"
    compileSdk = 36

    defaultConfig {
        // 本 APK 仅作为「免安装插件容器」被宿主加载，不会真正安装到系统；
        // applicationId 仅为满足打包要求。
        applicationId = "com.jason.reader.plugin.demo"
        minSdk = 24
        // 插件版本：versionName/versionCode 会被自动写入 plugin.json，
        // 与宿主插件管理页展示的版本一致。
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // 契约层只编译期可见；严禁改为 implementation，否则宿主/插件各加载一份
    // 同名接口，类型转换失败（ClassCastException / 单例分裂）。
    // 契约层由 JitPack 提供（Jason-wam/book-api 仓库，tag 决定版本）。
    compileOnly("com.github.Jason-wam:book-api:v0.1.1")
    // 本地联调替代方案（已实测可编译）：book-api 执行 publishToMavenLocal，
    // settings 里启用 mavenLocal() 并换回 com.jason.reader:book-api:0.1.0
    // 需要直接用宿主网络封装（OkHttpManager 等）时启用（同样走 jitpack/私有源）：
    // compileOnly("com.github.Jason-wam:network:v0.1.0")

    // ── 仅测试期使用：JVM 单元测试让你不装宿主、不连设备即可调试插件逻辑 ──
    // 注：compileOnly 不进测试类路径，测试源集需单独声明（不会被打进 APK）
    testImplementation("com.github.Jason-wam:book-api:v0.1.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.20")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.4.20")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

// KGP 会自动把 kotlin-stdlib 加为 implementation 并打进 APK；宿主进程里 stdlib
// 永远由宿主 ClassLoader 提供（parent-first），内嵌副本只会白白增大插件体积。
// 只从 APK 打包用的变体运行时类路径排除（编译与单元测试不受影响），保持插件最小化。
configurations.matching { it.name.matches(Regex("(debug|release)RuntimeClasspath")) }.configureEach {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk11")
}

/**
 * packagePlugin：把 release APK 复制为稳定的插件文件名，推入宿主
 * filesDir/book-plugins/ 即可。
 *
 * 本任务产出**标准 APK**：带 AndroidManifest.xml 与资源表，宿主导入时
 * 图标优先取 manifest 的 android:icon；plugin.json 按约定放在 assets/ 下。
 * 注意：宿主按「免安装容器」加载，不校验签名，故 release 未签名 APK 可直接使用；
 * 若后续需要签名信任链，自行配置 signingConfigs 即可。
 */
tasks.register<Copy>("packagePlugin") {
    group = "reader-plugin"
    description = "导出可导入宿主的标准 APK 插件"
    dependsOn("assembleRelease")
    from(layout.buildDirectory.file("outputs/apk/release/moban-plugin-demo-release-unsigned.apk"))
    into(layout.buildDirectory.dir("dist"))
    rename { "moban-plugin-demo.apk" }
}

// ─────────────────────────────────────────────────────────────────────────────
// 自动生成 assets/plugin.json：作者无需手动维护 json，只需配置下方 pluginMeta。
// versionName / versionCode 从 android.defaultConfig 自动同步。
// entryClass 约定为「<namespace>.BookSourcePlugin」；若你的入口类不叫这个名字，
// 在 pluginMeta 里显式指定 entryClass 即可。
// ─────────────────────────────────────────────────────────────────────────────

val pluginMeta = mapOf(
    "id" to "com.jason.reader.plugin.demo",
    "name" to "演示APK插件包",
    // 不填则自动推断为 "${android.namespace}.BookSourcePlugin"
    "entryClass" to "com.jason.reader.plugin.demo.DemoBookSourcePlugin",
    "minApiVersion" to 1,
    "author" to "墨伴",
    "description" to "最小化标准 APK 插件示例：manifest 图标 + 自动生成 plugin.json，搜索/目录/正文全链路演示数据。",
)

val generatePluginJson by tasks.registering {
    group = "reader-plugin"
    description = "自动生成 assets/plugin.json（从 build.gradle.kts 配置同步）"
    val outputFile = layout.projectDirectory.file("src/main/assets/plugin.json")
    outputs.file(outputFile)
    doLast {
        outputFile.asFile.parentFile?.mkdirs()
        val ns = android.namespace
        val entry = pluginMeta["entryClass"]?.toString()
            ?: "$ns.BookSourcePlugin"
        val versionName = android.defaultConfig.versionName ?: "0.0.0"
        val versionCode = android.defaultConfig.versionCode ?: 0
        val minApi = pluginMeta["minApiVersion"]?.toString()?.toIntOrNull() ?: 1
        val json = """
            {
              "id": "${pluginMeta["id"]}",
              "name": "${pluginMeta["name"]}",
              "version": "$versionName",
              "versionCode": $versionCode,
              "entryClass": "$entry",
              "minApiVersion": $minApi,
              "author": "${pluginMeta["author"] ?: ""}",
              "description": "${pluginMeta["description"] ?: ""}"
            }
        """.trimIndent()
        outputFile.asFile.writeText(json)
    }
}

// preBuild 依赖生成任务，确保每次构建前 plugin.json 都是最新的
tasks.named("preBuild") {
    dependsOn(generatePluginJson)
}
