// Projet multi-loader Spectator Plus.
// Chaque module définit son propre build ; ici uniquement les réglages communs.
allprojects {
    group = property("maven_group") as String
    version = property("mod_version") as String
}
