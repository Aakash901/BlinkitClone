pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = uri("https://jitpack.io"))
        maven(url = "https://phonepe.mycloudrepo.io/public/repositories/phonepe-intentsdk-android")

    }
}

rootProject.name = "Blinkit"
include(":app")
