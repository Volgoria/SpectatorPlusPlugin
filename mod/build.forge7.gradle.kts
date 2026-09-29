plugins {
    id("net.minecraftforge.gradle") version "7.0.40"
    id("net.minecraftforge.jarjar") version "0.2.3"
}

version = "${rootProject.version}+${sc.current.version}"
base.archivesName = "SpectatorPlus-Forge"

// Version de Java exigée par chaque version de Minecraft
val requiredJava: Int = if (sc.current.parsed >= "26.1") 25 else 21

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

// Jar final : le jar du mod + SnakeYAML embarqué (jar-in-jar)
jarJar.register {
    archiveClassifier = null
}

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:${sc.properties.get<String>("deps.forge")}"))
    implementation(project(":core"))
    // SnakeYAML n'est pas fourni par Forge : embarqué dans le jar du mod
    implementation("org.yaml:snakeyaml:2.4")
    "jarJar"("org.yaml:snakeyaml:2.4")
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
        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
        from(rootProject.file("bukkit/src/main/resources/plugin.yml")) {
            rename { "spectatorplus-permissions.yml" }
        }
    }

    // Le core est embarqué directement dans le jar du mod
    named<Jar>("jar") {
        archiveClassifier = "slim"
        // jar intermédiaire (sans SnakeYAML) : hors de build/libs pour ne pas être distribué par erreur
        destinationDirectory = layout.buildDirectory.dir("devlibs")
        dependsOn(":core:jar")
        from(project(":core").sourceSets.main.get().output)
        // Mixin trouve la configuration des mixins par le manifeste
        manifest.attributes(mapOf("MixinConfigs" to "spectatorplus.mixins.json"))
    }
}
