import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `eternalminions-java`
    `eternalminions-java-test`
    `eternalminions-repositories`

    id("de.eldoria.plugin-yml.paper") version "0.9.0"
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

dependencies {
    implementation(project(":eternalminions-api"))

    compileOnly("io.papermc.paper:paper-api:${Versions.PAPER_API}")
    testImplementation("io.papermc.paper:paper-api:${Versions.PAPER_API}")

    implementation("eu.okaeri:okaeri-configs-yaml-snakeyaml:${Versions.OKAERI_CONFIGS}")
    implementation("eu.okaeri:okaeri-configs-serdes-commons:${Versions.OKAERI_CONFIGS}")
    implementation("eu.okaeri:okaeri-configs-serdes-bukkit:${Versions.OKAERI_CONFIGS}")

    implementation("com.eternalcode:multification-paper:${Versions.MULTIFICATION}")
    implementation("com.eternalcode:multification-okaeri:${Versions.MULTIFICATION}")

    implementation("dev.rollczi:litecommands-bukkit:${Versions.LITE_COMMANDS}")
    implementation("dev.rollczi:litecommands-adventure:${Versions.LITE_COMMANDS}")

    compileOnly("com.github.retrooper:packetevents-spigot:${Versions.PACKET_EVENTS}")
    implementation("io.github.tofaa2:spigot:${Versions.ENTITY_LIB}")
    implementation("io.github.tofaa2:common:${Versions.ENTITY_LIB}")
    implementation("io.github.tofaa2:api:${Versions.ENTITY_LIB}")
    implementation("com.github.cryptomorin:XSeries:${Versions.X_SERIES}")
    implementation("com.github.stefvanschie.inventoryframework:IF:${Versions.INVENTORY_FRAMEWORK}")
    implementation("it.unimi.dsi:fastutil:${Versions.FASTUTIL}")

    implementation("com.zaxxer:HikariCP:${Versions.HIKARI_CP}")
    implementation("com.j256.ormlite:ormlite-jdbc:${Versions.ORMLITE}")
    implementation("com.h2database:h2:${Versions.H2}")
    implementation("org.mariadb.jdbc:mariadb-java-client:${Versions.MARIA_DB}")
    implementation("org.postgresql:postgresql:${Versions.POSTGRESQL}")
}

paper {
    main = "com.eternalcode.minions.EternalMinionsPlugin"
    apiVersion = "1.21"
    prefix = "EternalMinions"
    authors = listOf("EternalCodeTeam")
    name = "EternalMinions"
    description = "Efficient minion automation for Paper servers."
    website = "https://www.eternalcode.pl"
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    version = project.version.toString()
    foliaSupported = false
    serverDependencies {
        register("packetevents") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }
}

tasks.runServer {
    minecraftVersion("26.2")
}

tasks.shadowJar {
    archiveFileName.set("EternalMinions-v${project.version}.jar")

    exclude(
        "org/intellij/lang/annotations/**",
        "org/jetbrains/annotations/**",
        "META-INF/**",
    )

    val relocationPrefix = "com.eternalcode.minions.libs"
    listOf(
        "eu.okaeri",
        "org.yaml.snakeyaml",
        "com.eternalcode.multification",
        "dev.rollczi.litecommands",
        "io.github.tofaa2",
        "com.cryptomorin.xseries",
        "com.github.stefvanschie.inventoryframework",
        "it.unimi.dsi.fastutil",
        "com.zaxxer.hikari",
        "com.j256.ormlite",
        "com.h2database",
        "org.mariadb.jdbc",
        "org.postgresql",
    ).forEach { dependencyPackage ->
        relocate(dependencyPackage, "$relocationPrefix.$dependencyPackage")
    }

    manifest {
        attributes["paperweight-mappings-namespace"] = "spigot"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
