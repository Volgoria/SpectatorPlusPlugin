package fr.spectatorplus.config;

import fr.spectatorplus.core.CoreContext;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Sender;
import fr.spectatorplus.util.Text;

import java.util.List;

/**
 * Langues avec résolution par destinataire.
 * <p>
 * Langue d'un joueur : choix personnel (paramètres) → langue du client Minecraft (si activé)
 * → langue par défaut du serveur. La console utilise la langue par défaut.
 */
public final class Messages extends Lang {

    private final CoreContext core;

    public Messages(CoreContext core) {
        super(core.host(), core.files());
        this.core = core;
    }

    /** Langue utilisée pour un destinataire. */
    public String lang(Sender sender) {
        if (!(sender instanceof PlatformPlayer)) return defaultLang();
        PlatformPlayer p = (PlatformPlayer) sender;
        String chosen = core.filters() == null ? null : core.filters().get(p.getUniqueId()).language;
        return resolve(chosen, perPlayer() ? p.getClientLocale() : null);
    }

    public String raw(Sender sender, String key) {
        return raw(lang(sender), key);
    }

    public String get(Sender sender, String key, Object... replacements) {
        return Text.color(replace(raw(lang(sender), key), replacements));
    }

    public List<String> list(Sender sender, String key, Object... replacements) {
        return listIn(lang(sender), key, replacements);
    }

    public String prefix(Sender sender) {
        return Text.color(raw(lang(sender), "prefix"));
    }

    public void send(Sender to, String key, Object... replacements) {
        String msg = raw(lang(to), key);
        if (msg.isEmpty()) return;
        to.sendMessage(prefix(to) + Text.color(replace(msg, replacements)));
    }
}
