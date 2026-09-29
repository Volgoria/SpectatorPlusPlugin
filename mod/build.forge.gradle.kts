plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.147"
}

version = "${rootProject.version}+${sc.current.version}"
base.archivesName = "SpectatorPlus-Forge"

legacyForge {
    version = sc.properties.get<String>("deps.forge")
}

// Forge 1.20.1 et avant utilise les noms SRG à l'exécution : la refmap convertit les cibles des mixins
mixin {
    add(sourceSets.main.get(), "spectatorplus.refmap.json")
    config("spectatorplus.mixins.json")
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    implementation(project(":core"))
    // SnakeYAML n'est pas fourni par Forge : embarqué dans le jar du mod (jar-in-jar)
    implementation("org.yaml:snakeyaml:2.4")
    jarJar("org.yaml:snakeyaml:[2.4,3.0)")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 17
        options.compilerArgs.add("-Xlint:-options")
    }

    processResources {
        val props = mapOf(
            "id" to sc.properties.get<String>("mod.id"),
            "name" to sc.properties.get<String>("mod.name"),
            "version" to project.version.toString(),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "loader" to sc.properties.get<String>("deps.forge_loader"),
        )
        inputs.properties(props)
        filesMatching("META-INF/mods.toml") { expand(props) }
        filesMatching("spectatorplus.mixins.json") {
            filter { line -> line.replace("\"package\":", "\"refmap\": \"spectatorplus.refmap.json\",\n  \"package\":") }
        }
        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
        from(rootProject.file("bukkit/src/main/resources/plugin.yml")) {
            rename { "spectatorplus-permissions.yml" }
        }
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    // Le core est embarqué directement dans le jar du mod
    jar {
        dependsOn(":core:jar")
        from(project(":core").sourceSets.main.get().output)
        // Forge 1.20.1 et avant trouve la configuration des mixins par le manifeste
        manifest.attributes(mapOf("MixinConfigs" to "spectatorplus.mixins.json"))
    }
}
