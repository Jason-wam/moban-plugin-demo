package com.jason.reader.plugin.demo

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/** [JvmHttp] 真实联网冒烟测试：验证 JVM 单测可直接请求站点（无 CI 时执行） */
class JvmHttpTest {

    @Test
    fun `直连真实站点拉取文本`() = runBlocking {
        if (System.getenv("CI") == "true") return@runBlocking
        val html = JvmHttp.text("https://example.com").getOrThrow()
        assertTrue(html.contains("Example Domain"))
    }
}
