package com.jason.reader.plugin.demo

import com.jason.reader.bookapi.BookSourcePlugin
import com.jason.reader.bookapi.PluginHost
import com.jason.reader.bookapi.PluginMetadata
import com.jason.reader.bookapi.RemoteBookSource

/**
 * 插件入口：宿主通过 DexClassLoader 加载本 APK 后，
 * 按约定命名 `<packageName>.BookSourcePlugin` 反射实例化本类（必须有无参构造）。
 * 如需自定义入口类名，可在包内放 plugin.json 指定 entryClass 覆盖约定。
 *
 * 所有元数据（id/name/version/minApiVersion/author/description）只在此处 [metadata] 配置一次，
 * 宿主加载后自动回写数据库，无需在 plugin.json 或 build.gradle.kts 重复维护。
 *
 * 代码结构：一个插件可在 [createSources] 返回多个书源，
 * 网络/文件能力全部来自注入的 [PluginHost]，不要在此持有静态 OkHttpClient 或自建线程池。
 */
class BookSourcePlugin : BookSourcePlugin {

    override val metadata: PluginMetadata = PluginMetadata(
        id = "com.jason.reader.plugin.demo",
        name = "演示APK插件包",
        version = "1.0.0",
        minApiVersion = 1,
        author = "墨伴",
        description = "最小化标准 APK 插件示例，演示搜索/目录/正文全链路。",
    )

    override fun createSources(host: PluginHost): List<RemoteBookSource> =
        listOf(DemoBookSource(host))
}
