plugins {
    `eternalminions-java`
    `eternalminions-java-test`
    `eternalminions-repositories`
}

dependencies {
    compileOnlyApi("io.papermc.paper:paper-api:${Versions.PAPER_API}")
    api("org.jetbrains:annotations:${Versions.JETBRAINS_ANNOTATIONS}")
}
