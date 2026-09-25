plugins {
    id("dev.kikugie.loom-back-compat")
}

version = "${rootProject.version}+${sc.current.version}"
base.archivesName = "SpectatorPlus-Fabric"

// Version de Java exigée par chaque version de Minecraft
val requiredJava: Int = when {
    sc.current.parsed >= "26.1" -> 25
    sc.current.parsed >= "1.20.5" -> 21
    sc.current.parsed >= "1.18" -> 17
    sc.current.parsed >= "1.17" -> 16
    else -> 8
}

dependencies {
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties.get<String>("deps.fabric_api")))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Mappings Mojang sur les versions obfusquées : mêmes noms que les versions 26.x
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${sc.properties.get<String>("deps.fabric_loader")}")
    fapi("fabric-api-base", "fabric-lifecycle-events-v1", sc.properties.get<String>("deps.command_api"), "fabric-networking-api-v1")

    implementation(project(":core"))
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
            "loader" to sc.properties.get<String>("deps.fabric_loader"),
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
    }

    // Le core est embarqué directement dans le jar du mod
    jar {
        dependsOn(":core:jar")
        from(project(":core").sourceSets.main.get().output)
    }
}
