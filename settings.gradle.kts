pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy{
        eachPlugin {
            if (requested.id.namespace == "org.jetbrains.kotlin") {
                useVersion("1.9.0")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 使用阿里云的 Maven 镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        // 默认的 Google 和 Maven Central 仓库
        google()
        mavenCentral()
    }
}

rootProject.name = "SpeakerIdentification"
include(":app")
