pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven (url = "https://jitpack.io")
    }
}

gradle.extra.apply {
    set("androidxMediaEnableMidiModule", true)
}

// 1. Đặt tên dự án gốc trước
rootProject.name = "testProject"

// 2. Khai báo tất cả các module con sau đó
include(":app")
include(":extractor")
include("timeago-parser")
include(":nanojson")
project(":nanojson").projectDir = file("nanojson")
project(":extractor").projectDir = File(rootDir, "NewPipeExtractor/extractor")
project(":timeago-parser").projectDir = file("NewPipeExtractor/timeago-parser")