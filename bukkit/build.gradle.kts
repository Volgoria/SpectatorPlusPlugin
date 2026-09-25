plugins {
    java
}

base {
    archivesName.set("SpectatorPlus")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Java 8 bytecode : le plugin tourne sur les serveurs 1.8 (Java 8) jusqu'aux versions 26.x (Java 25)
    options.release.set(8)
    options.compilerArgs.add("-Xlint:-options")
}

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    mavenCentral()
}

dependencies {
    // On compile contre l'API la plus ancienne supportée (1.8.8).
    // Tout ce qui est plus récent est accédé par réflexion (package fr.spectatorplus.compat).
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") {
        exclude(group = "net.md-5", module = "bungeecord-chat")
    }
    // bungeecord-chat 1.8-SNAPSHOT (dépendance de spigot-api 1.8.8) n'est plus publié en ligne :
    // copie locale dans bukkit/libs (licence BSD, compilation uniquement).
    compileOnly(files("libs/bungeecord-chat-1.8-SNAPSHOT.jar"))
    implementation(project(":core"))
}

tasks.processResources {
    val ver = project.version.toString()
    inputs.property("version", ver)
    filesMatching("plugin.yml") {
        filter { line -> line.replace("\${project.version}", ver) }
    }
}

// Le core est embarqué directement dans le jar du plugin
tasks.jar {
    dependsOn(":core:jar")
    from(project(":core").sourceSets.main.get().output)
}
