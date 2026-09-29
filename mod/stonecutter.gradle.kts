plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // propriétés communes, puis par version, puis par loader et version (stonecutter.properties.toml)
    properties {
        tags(version, loader)
    }

    // constantes utilisables dans les commentaires : //? if fabric { ... //?}
    constants {
        match(loader, "fabric", "neoforge", "forge")
    }

    // renommages de classes entre versions, appliqués au texte du code
    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
        string(current.parsed >= "26.1") {
            replace("ClickType", "ContainerInput")
        }
    }
}
