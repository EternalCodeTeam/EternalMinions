plugins {
    `eternalminions-java`
    `eternalminions-repositories`

    id("xyz.jpenilla.run-paper")
}

dependencies {
    compileOnly(project(":eternalminions-api"))
    compileOnly("io.papermc.paper:paper-api:${Versions.PAPER_API}")
}

val prepareApiExamplesRun = tasks.register<Copy>("prepareApiExamplesRun") {
    from(layout.projectDirectory.file("src/runServer/eula.txt"))
    into(layout.projectDirectory.dir("run"))
}

tasks.runServer {
    minecraftVersion("26.2")
    dependsOn(prepareApiExamplesRun)
    dependsOn(":eternalminions-plugin:shadowJar")
    pluginJars.from(
        layout.projectDirectory.file(
            "../eternalminions-plugin/build/libs/EternalMinions-v${project.version}.jar"
        )
    )

    downloadPlugins {
        modrinth("packetevents", "2.13.0+spigot")
    }

}
