package com.jason.reader.plugin.demo

import com.jason.reader.bookapi.BookChapter
import com.jason.reader.bookapi.BookInfo
import com.jason.reader.bookapi.ChapterContent
import com.jason.reader.bookapi.ExploreSlot
import com.jason.reader.bookapi.PluginHost
import com.jason.reader.bookapi.RemoteBookSource
import com.jason.reader.bookapi.SearchBook
import com.jason.reader.bookapi.SearchQuery
import com.jason.reader.bookapi.SourceException

/**
 * 演示书源（最小化）：全部使用内置演示数据，保证开箱即可在宿主中
 * 跑通「搜索 → 详情 → 目录 → 正文」。真实站点只需把各方法替换为
 * 「host.http.text 拉取 → jsoup/正则解析 → 构造 DTO」，签名与异常约定不变。
 */
class DemoBookSource(private val host: PluginHost) : RemoteBookSource {

    override val id: String = "demo.apk.source.local"
    override val name: String = "演示APK书源"
    override val group: String = "演示"

    // ---------------- 搜索 ----------------

    override suspend fun search(query: SearchQuery): Result<List<SearchBook>> =
        runCatching {
            host.log(TAG, "search: keyword=${query.keyword}, page=${query.page}")
            if (query.page > 1) return@runCatching emptyList()
            DEMO_BOOKS.filter {
                query.keyword.isBlank() ||
                    it.name.contains(query.keyword, true) ||
                    it.author.contains(query.keyword, true)
            }
        }

    // ---------------- 发现 ----------------

    override suspend fun exploreSlots(): Result<List<ExploreSlot>> = Result.success(
        listOf(ExploreSlot("热门推荐", "https://demo.apk.local/explore/hot?page={{page}}"))
    )

    override suspend fun explore(slot: ExploreSlot, page: Int): Result<List<SearchBook>> =
        runCatching { if (page > 2) emptyList() else DEMO_BOOKS }

    // ---------------- 详情 ----------------

    override suspend fun getBookInfo(detailUrl: String): Result<BookInfo> =
        runCatching {
            val demo = DEMO_BOOKS.firstOrNull { it.detailUrl == detailUrl }
                ?: throw SourceException.ParseFailed("未知书籍: $detailUrl")
            BookInfo(
                name = demo.name,
                author = demo.author,
                coverUrl = demo.coverUrl,
                intro = demo.intro,
                latestChapter = demo.latestChapter,
                detailUrl = demo.detailUrl,
            )
        }

    // ---------------- 目录 ----------------

    override suspend fun getChapterList(detailUrl: String): Result<List<BookChapter>> =
        runCatching {
            (0 until CHAPTER_COUNT).map { index ->
                BookChapter(
                    index = index,
                    title = "第 ${index + 1} 章 示例标题",
                    url = "$detailUrl/$index",
                )
            }
        }

    // ---------------- 正文 ----------------

    override suspend fun getChapterContent(
        detailUrl: String,
        chapter: BookChapter,
    ): Result<ChapterContent> = runCatching {
        val paragraphs = listOf(
            "　　这是《演示APK书源》第 ${chapter.index + 1} 章的演示正文。",
            "　　如果你能在阅读页看到这段话，说明标准 APK 插件的加载、",
            "manifest 图标提取、SPI 调用与正文装载链路已经全部打通。",
        )
        ChapterContent(text = paragraphs.joinToString("\n"))
    }

    private companion object {
        const val TAG = "DemoApkBookSource"
        const val CHAPTER_COUNT = 12

        val DEMO_BOOKS = listOf(
            SearchBook(
                name = "APK插件入门",
                author = "墨伴",
                intro = "演示标准 APK 插件形态：manifest 图标 + assets 描述符。",
                latestChapter = "第 12 章 示例标题",
                detailUrl = "https://demo.apk.local/book/2001",
            ),
            SearchBook(
                name = "免安装容器演义",
                author = "示例作者",
                intro = "演示数据之二，用于验证多本书籍与关键词过滤。",
                latestChapter = "第 12 章 示例标题",
                detailUrl = "https://demo.apk.local/book/2002",
            ),
        )
    }
}
