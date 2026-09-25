pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie" }
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
    // fabric : un mod server-side par version clé de Minecraft
    create(":fabric") {
        versions("1.16.5", "1.18.2", "1.19.2", "1.20.1", "1.20.4", "1.21.1", "1.21.11", "26.3")
        vcsVersion = "26.3"
    }
}
