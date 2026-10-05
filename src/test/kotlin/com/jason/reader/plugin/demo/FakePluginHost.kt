package com.jason.reader.plugin.demo

import com.jason.reader.bookapi.API_VERSION
import com.jason.reader.bookapi.HttpFacade
import com.jason.reader.bookapi.PluginHost
import com.jason.reader.bookapi.RequestOptions
import java.io.File

/**
 * JVM 测试/调试用假宿主：实现契约接口，让插件逻辑完全脱离 Android 宿主运行。
 *
 * - 单元测试：`gradlew test`；
 * - 断点调试：在 IDE 里直接 Run/Debug 该测试类，插件源码全程可打断点；
 * - 真实站点开发时把 [FakeHttp] 替换为读取本地 HTML 样本的实现，解析逻辑离线可调。
 */
class FakePluginHost(
    private val baseDir: File = File("build/fake-host").apply { mkdirs() },
) : PluginHost {

    override val apiVersion: Int = API_VERSION

    override val http: HttpFacade = FakeHttp

    override fun cacheDir(name: String): File =
        File(baseDir, name).apply { mkdirs() }

    override fun log(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] $message${throwable?.let { ": ${it.message}" } ?: ""}")
    }
}

/** 假 HTTP：演示源不联网；真实插件可改为读本目录下的 HTML/JSON 样本文件 */
object FakeHttp : HttpFacade {
    override suspend fun text(url: String, options: RequestOptions): Result<String> =
        Result.failure(UnsupportedOperationException("FakeHttp 不发真实请求：$url"))

    override suspend fun download(
        url: String,
        destPath: String,
        options: RequestOptions,
        onProgress: ((bytes: Long) -> Unit)?,
    ): Result<String> =
        Result.failure(UnsupportedOperationException("FakeHttp 不支持下载：$url"))
}
