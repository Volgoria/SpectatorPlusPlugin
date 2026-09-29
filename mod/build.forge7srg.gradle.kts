plugins {
    id("net.minecraftforge.gradle") version "7.0.40"
    id("net.minecraftforge.renamer") version "1.1.7"
    id("com.gradleup.shadow") version "9.6.1"
}

version = "${rootProject.version}+${sc.current.version}"
base.archivesName = "SpectatorPlus-Forge"

// Version de Java exigée par chaque version de Minecraft
val requiredJava = 17

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

minecraft {
    mappings("official", sc.current.version)
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
}

// Code embarqué dans le jar : le core et SnakeYAML (relocalisé pour éviter tout conflit avec un autre mod)
val shade: Configuration by configurations.creating

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:${sc.properties.get<String>("deps.forge")}"))
    implementation(project(":core"))
    implementation("org.yaml:snakeyaml:2.4")
    shade(project(":core")) { isTransitive = false }
    shade("org.yaml:snakeyaml:2.4")
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
}

// Noms Mojang → SRG, y compris les cibles des mixins (refmap générée par le processeur d'annotations)
renamer.mappings(minecraft.dependency.toSrg)
renamer.enableMixinRefmaps {
    config("spectatorplus.mixins.json")
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

    named<Jar>("jar") {
        archiveClassifier = "slim"
        destinationDirectory = layout.buildDirectory.dir("devlibs")
    }

    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveClassifier = "mojmap"
        // jar intermédiaire (noms Mojang) : seul le jar renommé en SRG est à distribuer
        destinationDirectory = layout.buildDirectory.dir("devlibs")
        configurations = listOf(shade)
        relocate("org.yaml.snakeyaml", "fr.spectatorplus.lib.snakeyaml")
        // refmap des mixins produite par le processeur d'annotations à la compilation
        from(layout.buildDirectory.file("tmp/compileJava/compileJava-refmap.json")) {
            rename { "spectatorplus.refmap.json" }
        }
        // Mixin trouve la configuration des mixins par le manifeste
        manifest.attributes(mapOf("MixinConfigs" to "spectatorplus.mixins.json"))
    }
}

// Jar final : le jar complet renommé en SRG
renamer.classes(tasks.named<Jar>("shadowJar")) {
    archiveClassifier = ""
    mappings(renamer.mixin.generatedMappings)
}

tasks.named("build") {
    dependsOn("renameShadowJar")
}
