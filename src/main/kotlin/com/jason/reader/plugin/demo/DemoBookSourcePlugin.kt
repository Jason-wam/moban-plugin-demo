package com.jason.reader.plugin.demo

import com.jason.reader.bookapi.BookSourcePlugin
import com.jason.reader.bookapi.PluginHost
import com.jason.reader.bookapi.PluginMetadata
import com.jason.reader.bookapi.RemoteBookSource

/**
 * 插件入口：宿主通过 DexClassLoader 加载本 APK 后，
 * 依据 assets/plugin.json 的 entryClass 反射实例化本类（必须有无参构造）。
 *
 * 与 sample 工程的区别仅在打包形态：标准 APK 自带 manifest 图标与 assets；
 * 代码结构完全一致——一个插件可在 [createSources] 返回多个书源，
 * 网络/文件能力全部来自注入的 [PluginHost]，不要在此持有静态 OkHttpClient 或自建线程池。
 */
class DemoBookSourcePlugin : BookSourcePlugin {

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
