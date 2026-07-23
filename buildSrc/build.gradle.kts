plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.gradleup.shadow:com.gradleup.shadow.gradle.plugin:9.5.1")
}

sourceSets {
    main {
        java.setSrcDirs(emptyList<String>())
        groovy.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
    test {
        java.setSrcDirs(emptyList<String>())
        kotlin.setSrcDirs(emptyList<String>())
        groovy.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}
