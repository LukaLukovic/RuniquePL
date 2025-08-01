pluginManagement {
    includeBuild("build-logic")
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
    }
}

rootProject.name = "RuniquePL"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")
include(":auth:data")
include(":auth:presentation")
include(":auth:domain")
include(":core:presentation:designsystem")
include(":core:presentation:ui")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":run:data")
include(":run:domain")
include(":run:presentation")
include(":run:location")
include(":run:network")
include(":analytics:data")
include(":analytics:domain")
include(":analytics:presentation")
include(":analytics:analytics_feature")
include(":wear:app")
include(":wear:run:data")
include(":wear:run:domain")
include(":wear:run:presentation")
include(":core:presentation:designsystem_wear")
include(":core:connectivity:domain")
include(":core:connectivity:data")
include(":core:notification")
