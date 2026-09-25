plugins {
    `java-library`
}

// Le core doit rester en bytecode Java 8 : il est embarqué dans le jar Bukkit 1.8.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(8)
    options.compilerArgs.add("-Xlint:-options")
}

repositories {
    mavenCentral()
}

dependencies {
    // SnakeYAML est fourni par Bukkit, et embarqué/fourni côté loaders
    compileOnly("org.yaml:snakeyaml:1.33")
}
