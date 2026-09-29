pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Stonecutter : un seul code source par loader, compilé pour chaque version de Minecraft
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Applique la bonne variante de Loom (remap pour ≤ 1.21.11, sans remap pour 26.x)
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    // Téléchargement automatique des JDK nécessaires (8, 17, 21, 25)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "SpectatorPlus"

// core : logique commune, sans aucune dépendance à Bukkit ni à un loader
include("core")
// bukkit : le plugin Spigot/Paper/hybrides (un seul jar 1.8 → 26.x)
include("bukkit")

stonecutter {
    // mod : un seul code source pour Fabric / NeoForge / Forge, compilé pour chaque version clé.
    // Chaque nœud s'appelle « <minecraft>-<loader> » et utilise build.<loader>.gradle.kts.
    create(":mod") {
        fun match(version: String, vararg loaders: String) {
            for (loader in loaders) version("$version-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        // Fabric n'existe qu'à partir de 1.14 : première version clé 1.16.5
        match("1.16.5", "fabric")
        // Forge 1.17 → 1.20.1 : noms SRG à l'exécution (ModDevGradle legacyforge, refmap des mixins)
        match("1.18.2", "fabric", "forge")
        match("1.19.2", "fabric", "forge")
        match("1.20.1", "fabric", "forge")
        match("1.20.4", "fabric")
        // NeoForge : versions clés à partir de 1.21.1 (NeoForge 1.20.1 est couvert par le jar Forge 1.20.1)
        match("1.21.1", "fabric", "neoforge")
        match("1.21.11", "fabric", "neoforge")
        match("26.3", "fabric", "neoforge")
        // Forge 1.20.6+ : noms Mojang à l'exécution (ForgeGradle 7)
        for (v in listOf("1.21.1", "1.21.11", "26.3")) version("$v-forge", v).buildscript("build.forge7.gradle.kts")
        // Forge 1.20.4 : noms SRG à l'exécution (ForgeGradle 7 + renommage, SnakeYAML relocalisé).
        // Forge 1.16.5 n'est pas possible : ses mappings « officiels » gardent les classes MCP, incompatibles avec le code commun.
        version("1.20.4-forge", "1.20.4").buildscript("build.forge7srg.gradle.kts")
        vcsVersion = "26.3-fabric"
    }
}
