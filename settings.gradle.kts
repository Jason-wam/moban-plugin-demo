pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // 契约层 book-api 由 JitPack 提供（book-api 仓库打 tag 即发布）
        maven("https://jitpack.io")
        // 可选：本机用 gradlew publishToMavenLocal 发布过契约层时，取消下行注释可离线构建
        // mavenLocal()
    }
}

// 契约层 book-api：通过 JitPack 坐标引用（compileOnly，APK 内不含其字节码，
// 运行时由宿主 ClassLoader 提供）。无需携带宿主源码即可独立构建。
//
// 如需切回「源码复合构建」（例如在 AnyReader 宿主仓库内做联调），改回：
// includeBuild("../../modules/book-api")
// 并把 build.gradle.kts 里的 jitpack 坐标换回 com.jason.reader:book-api:0.1.0

rootProject.name = "demo-apk-plugin"
