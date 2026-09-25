package fr.spectatorplus.compat;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/**
 * Isolé dans sa propre classe : si l'API BungeeCord Chat n'est pas présente,
 * seul le chargement de cette classe échoue et {@link Compat#clickable} se replie sur du texte simple.
 */
@SuppressWarnings("deprecation")
final class ChatComponents {

    private ChatComponents() {
    }

    static void send(Player p, String text, String hover, String command) {
        BaseComponent[] parts = TextComponent.fromLegacyText(text);
        TextComponent root = new TextComponent("");
        for (BaseComponent c : parts) root.addExtra(c);
        if (hover != null && !hover.isEmpty()) {
            root.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover)));
        }
        if (command != null && !command.isEmpty()) {
            root.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        }
        p.spigot().sendMessage(root);
    }
}
