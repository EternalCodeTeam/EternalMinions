import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `eternalminions-java`
    `eternalminions-java-test`
    `eternalminions-repositories`

    id("de.eldoria.plugin-yml.paper") version "0.9.0"
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
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
    implementation("com.eternalcode:eternalcode-commons-bukkit:${Versions.ETERNALCODE_COMMONS}")

    compileOnly("org.projectlombok:lombok:${Versions.LOMBOK}")
    annotationProcessor("org.projectlombok:lombok:${Versions.LOMBOK}")

    implementation("dev.rollczi:litecommands-bukkit:${Versions.LITE_COMMANDS}")
    implementation("dev.rollczi:litecommands-adventure:${Versions.LITE_COMMANDS}")

    compileOnly("com.github.retrooper:packetevents-spigot:${Versions.PACKET_EVENTS}")
    compileOnly("com.github.brcdev-minecraft:shopgui-api:${Versions.SHOPGUI_API}") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    implementation("io.github.tofaa2:spigot:${Versions.ENTITY_LIB}")
    implementation("io.github.tofaa2:common:${Versions.ENTITY_LIB}")
    implementation("io.github.tofaa2:api:${Versions.ENTITY_LIB}")
    implementation("com.github.cryptomorin:XSeries:${Versions.X_SERIES}")
    implementation("com.github.stefvanschie.inventoryframework:IF:${Versions.INVENTORY_FRAMEWORK}")
    implementation("it.unimi.dsi:fastutil:${Versions.FASTUTIL}")
    implementation("com.github.ben-manes.caffeine:caffeine:${Versions.CAFFEINE}")

    implementation("com.zaxxer:HikariCP:${Versions.HIKARI_CP}")
    implementation("com.j256.ormlite:ormlite-jdbc:${Versions.ORMLITE}")

    paperLibrary("com.h2database:h2:${Versions.H2}")
    paperLibrary("org.mariadb.jdbc:mariadb-java-client:${Versions.MARIA_DB}")
    paperLibrary("org.postgresql:postgresql:${Versions.POSTGRESQL}")

    // compileOnly, never shaded/relocated: our code must see the exact same Economy class the
    // Vault plugin registers at runtime, not a shaded copy of it.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
}

paper {
    main = "com.eternalcode.minions.EternalMinionsPlugin"
    loader = "com.eternalcode.minions.database.MinionsLibraryLoader"
    generateLibrariesJson = true
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
        register("ShopGUIPlus") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
        register("Vault") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }
}

tasks.runServer {
    minecraftVersion("26.2")
    downloadPlugins {
        modrinth("FastAsyncWorldEdit", "2.15.3")
        modrinth("EternalCore", "2.0.1-SNAPSHOT+92")
        modrinth("EternalEconomy", "1.0.2-SNAPSHOT+7")
        modrinth("VaultUnlocked", "2.20.2")
    }
}
tasks.generatePaperPluginDescription {
    useDefaultCentralProxy()

    // mavenLocal() would otherwise be exported as "file:/C:/Users/<dev>/.m2/repository/", baking a
    // path from whoever built the jar into every copy we ship. It resolves to nothing anywhere
    // else, so no file-backed repository belongs in the published descriptor.
    repos.set(repos.get().filterValues { url -> !url.startsWith("file:") })
}

tasks.shadowJar {
    archiveFileName.set("EternalMinions-v${project.version}.jar")

    minimize {
        exclude(dependency("com.zaxxer:HikariCP:.*"))
        exclude(dependency("com.j256.ormlite:.*:.*"))
        exclude(dependency("com.github.ben-manes.caffeine:caffeine:.*"))
        exclude(dependency("eu.okaeri:.*:.*"))
        exclude(dependency("org.yaml:snakeyaml:.*"))
        exclude(dependency("dev.rollczi:.*:.*"))
        exclude(dependency("com.eternalcode:.*:.*"))
        exclude(dependency("io.github.tofaa2:.*:.*"))
        exclude(dependency("com.github.cryptomorin:XSeries:.*"))
        exclude(dependency("com.github.stefvanschie.inventoryframework:IF:.*"))
    }

    exclude(
        "org/intellij/lang/annotations/**",
        "org/jetbrains/annotations/**",
        "com/google/errorprone/**",
        "org/jspecify/**",
        "org/checkerframework/**",
        "org/slf4j/**",
        "META-INF/**",
    )

    val relocationPrefix = "com.eternalcode.minions.libs"
    listOf(
        "eu.okaeri",
        "org.yaml.snakeyaml",
        "com.eternalcode.multification",
        "com.eternalcode.commons",
        "dev.rollczi.litecommands",
        "me.tofaa",
        "com.cryptomorin.xseries",
        "com.github.stefvanschie.inventoryframework",
        "it.unimi.dsi.fastutil",
        "com.github.benmanes.caffeine",
        "com.zaxxer.hikari",
        "com.j256.ormlite",
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
