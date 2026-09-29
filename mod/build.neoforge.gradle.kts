plugins {
    id("net.neoforged.moddev") version "2.0.147"
}

version = "${rootProject.version}+${sc.current.version}"
base.archivesName = "SpectatorPlus-NeoForge"

// Version de Java exigée par chaque version de Minecraft
val requiredJava: Int = when {
    sc.current.parsed >= "26.1" -> 25
    sc.current.parsed >= "1.20.5" -> 21
    else -> 17
}

neoForge {
    version = sc.properties.get<String>("deps.neoforge")
}

dependencies {
    implementation(project(":core"))
    // SnakeYAML n'est pas garanti par le loader : embarqué dans le jar du mod (jar-in-jar)
    implementation("org.yaml:snakeyaml:2.4")
    jarJar("org.yaml:snakeyaml:[2.4,3.0)")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = requiredJava
        options.compilerArgs.add("-Xlint:-options")
    }

    processResources {
        val props = mapOf(
            "id" to sc.properties.get<String>("mod.id"),
            "name" to sc.properties.get<String>("mod.name"),
            "version" to project.version.toString(),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
        )
        inputs.properties(props)
        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
        exclude("fabric.mod.json", "META-INF/mods.toml")
        // Permissions et leurs valeurs par défaut : reprises du plugin.yml Bukkit
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
    }
}
