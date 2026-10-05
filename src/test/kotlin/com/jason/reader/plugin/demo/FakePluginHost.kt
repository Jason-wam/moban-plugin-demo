package com.jason.reader.plugin.demo

import com.jason.reader.bookapi.API_VERSION
import com.jason.reader.bookapi.HttpFacade
import com.jason.reader.bookapi.PluginHost
import com.jason.reader.bookapi.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset
import java.time.Duration

/**
 * JVM 测试/调试用假宿主：实现契约接口，让插件逻辑完全脱离 Android 宿主运行。
 *
 * - `FakePluginHost()` 默认走 [JvmHttp]：**JDK HttpClient 直连真实站点**，
 *   断点调试真实站点的解析逻辑无需先离线保存网页源码；
 * - 站点反爬强（Cookie 登录态/指纹/证书）导致 JVM 直连失败时，改传 `FakePluginHost(http = FakeHttp)`
 *   并自备 HTML/JSON 样本做离线调试（见 [FakeHttp]）；
 * - 需要完整宿主链路（持久化 Cookie、DNS、SSL 降级）时在真机上调试（README 调试章节 ②③）。
 */
class FakePluginHost(
    private val baseDir: File = File("build/fake-host").apply { mkdirs() },
    override val http: HttpFacade = JvmHttp,
) : PluginHost {

    override val apiVersion: Int = API_VERSION

    override fun cacheDir(name: String): File =
        File(baseDir, name).apply { mkdirs() }

    override fun log(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] $message${throwable?.let { ": ${it.message}" } ?: ""}")
    }
}

/**
 * 测试期真实 HTTP：用 JDK 自带 HttpClient 实现 [HttpFacade]，插件单测可直接请求真实站点。
 * 与宿主 PluginHttpFacade 的差异（测试够用即可）：无 Cookie 持久化、无 HappyDNS、
 * 无 SSL 降级重试；charset 探测仅按声明解析，不做字节级检测。
 */
object JvmHttp : HttpFacade {

    private const val DEFAULT_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"

    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private fun buildRequest(
        url: String,
        options: RequestOptions,
        bodyPublisher: HttpRequest.BodyPublisher,
    ): HttpRequest = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofMillis(options.timeoutMs ?: 20_000))
        .header("User-Agent", DEFAULT_UA)
        .apply {
            options.headers.forEach { (k, v) -> if (!k.equals("User-Agent", true)) header(k, v) }
            if (options.contentType != null) header("Content-Type", options.contentType!!)
        }
        .method(options.method.uppercase(), bodyPublisher)
        .build()

    override suspend fun text(url: String, options: RequestOptions): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val publisher = if (options.body != null) {
                    HttpRequest.BodyPublishers.ofString(options.body!!)
                } else HttpRequest.BodyPublishers.noBody()
                val resp = client.send(
                    buildRequest(url, options, publisher),
                    HttpResponse.BodyHandlers.ofByteArray()
                )
                check(resp.statusCode() in 200..299) { "HTTP ${resp.statusCode()}: $url" }
                // 编码：显式声明 → 响应头 charset → UTF-8（不做字节级探测）
                val charsetName = options.charset
                    ?: resp.headers().firstValue("content-type").orNull()
                        ?.let { Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
                    ?: "UTF-8"
                String(resp.body(), Charset.forName(charsetName))
            }
        }

    override suspend fun download(
        url: String,
        destPath: String,
        options: RequestOptions,
        onProgress: ((bytes: Long) -> Unit)?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dest = File(destPath).apply { parentFile?.mkdirs() }
            val publisher = if (options.body != null) {
                HttpRequest.BodyPublishers.ofString(options.body!!)
            } else HttpRequest.BodyPublishers.noBody()
            val resp = client.send(
                buildRequest(url, options, publisher),
                HttpResponse.BodyHandlers.ofFile(dest.toPath())
            )
            check(resp.statusCode() in 200..299) { "HTTP ${resp.statusCode()}: $url" }
            dest.absolutePath
        }
    }
}

/** 离线样本模式：不发真实请求；真实插件可改为读本目录下的 HTML/JSON 样本文件 */
object FakeHttp : HttpFacade {
    override suspend fun text(url: String, options: RequestOptions): Result<String> =
        Result.failure(UnsupportedOperationException("FakeHttp 不发真实请求（离线模式）：$url"))

    override suspend fun download(
        url: String,
        destPath: String,
        options: RequestOptions,
        onProgress: ((bytes: Long) -> Unit)?,
    ): Result<String> =
        Result.failure(UnsupportedOperationException("FakeHttp 不支持下载（离线模式）：$url"))
}

private fun java.util.Optional<String>.orNull(): String? = orElse(null)
