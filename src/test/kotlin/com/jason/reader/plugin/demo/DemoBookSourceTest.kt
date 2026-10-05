package com.jason.reader.plugin.demo

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 插件全链路 JVM 测试：搜索 → 详情 → 目录 → 正文，完全不需要宿主 App 与设备。
 * 在 IDE 中直接 Debug 运行即可在插件源码任意行打断点。
 */
class DemoBookSourceTest {

    private val source = DemoBookSourcePlugin().createSources(FakePluginHost()).first()

    @Test
    fun `搜索命中演示书籍并返回列表`() = runBlocking {
        val result = source.search(com.jason.reader.bookapi.SearchQuery(keyword = "APK"))
        assertTrue(result.isSuccess)
        val books = result.getOrThrow()
        assertEquals(1, books.size)
        assertEquals("APK插件入门", books.first().name)
        assertTrue(books.first().detailUrl.isNotBlank())
    }

    @Test
    fun `全链路 详情到目录到正文`() = runBlocking {
        val detailUrl = "https://demo.apk.local/book/2001"
        val info = source.getBookInfo(detailUrl).getOrThrow()
        assertEquals("APK插件入门", info.name)

        val chapters = source.getChapterList(detailUrl).getOrThrow()
        assertEquals(12, chapters.size)
        assertEquals(0, chapters.first().index)

        val content = source.getChapterContent(detailUrl, chapters.first()).getOrThrow()
        assertTrue(content.text.contains("第 1 章"))
        assertEquals("", content.nextUrl)
    }

    @Test
    fun `第 2 页无结果返回空列表而非失败`() = runBlocking {
        val result = source.search(com.jason.reader.bookapi.SearchQuery(keyword = "APK", page = 2))
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().size)
    }
}
