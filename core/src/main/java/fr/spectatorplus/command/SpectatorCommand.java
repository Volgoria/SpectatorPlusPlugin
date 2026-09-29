package fr.spectatorplus.command;

import fr.spectatorplus.SpectatorCore;
import fr.spectatorplus.api.EnterReason;
import fr.spectatorplus.api.LeaveReason;
import fr.spectatorplus.api.SpectatorMode;
import fr.spectatorplus.api.event.SpectatorEventType;
import fr.spectatorplus.gui.menus.FiltersMenu;
import fr.spectatorplus.gui.menus.GameInfoMenu;
import fr.spectatorplus.gui.menus.HistoryMenu;
import fr.spectatorplus.gui.menus.MainMenu;
import fr.spectatorplus.gui.menus.SettingsMenu;
import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Sender;
import fr.spectatorplus.spectator.SpectatorSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /spectatorplus (alias /spec, /sp, /spectate), commune à toutes les plateformes :
 * chaque plateforme transmet le libellé et les arguments bruts.
 */
public final class SpectatorCommand {

    private static final List<String> SUBS = Arrays.asList("help", "join", "leave", "list", "tp", "follow", "pov",
            "next", "prev", "inspect", "inv", "ec", "menu", "filters", "events", "info", "settings", "preset",
            "freeze", "unfreeze", "eliminate", "mode", "game", "event", "resetfilters", "lang", "reload");

    private final SpectatorCore plugin;

    public SpectatorCommand(SpectatorCore plugin) {
        this.plugin = plugin;
    }

    private void msg(Sender s, String key, Object... r) {
        plugin.messages().send(s, key, r);
    }

    private boolean perm(Sender s, String perm) {
        if (s.hasPermission(perm)) return true;
        msg(s, "errors.no-permission");
        return false;
    }

    private PlatformPlayer player(Sender s) {
        if (s instanceof PlatformPlayer) return (PlatformPlayer) s;
        msg(s, "errors.player-only");
        return null;
    }

    private SpectatorSession session(Sender s) {
        PlatformPlayer p = player(s);
        if (p == null) return null;
        SpectatorSession ss = plugin.spectators().getSpectator(p);
        if (ss == null) msg(s, "errors.not-spectator");
        return ss;
    }

    private PlatformPlayer target(Sender s, String name) {
        PlatformPlayer t = plugin.platform().findPlayer(name);
        if (t == null) msg(s, "errors.player-not-found", "player", name);
        return t;
    }

    /** Exécute la commande ; {@code label} est l'alias utilisé (spec, sp...). */
    public boolean execute(Sender sender, String label, String[] args) {
        if (!perm(sender, "spectatorplus.use")) return true;
        if (args.length == 0) {
            if (sender instanceof PlatformPlayer && plugin.spectators().isSpectator((PlatformPlayer) sender)) {
                new MainMenu(plugin, (PlatformPlayer) sender).open();
            } else {
                help(sender, label);
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help":
                help(sender, label);
                return true;
            case "join":
                join(sender, args);
                return true;
            case "leave":
                leave(sender, args);
                return true;
            case "list":
                list(sender);
                return true;
            case "tp":
            case "follow":
            case "pov":
            case "inspect":
            case "inv":
            case "ec":
                targeted(sender, sub, args);
                return true;
            case "next":
            case "prev": {
                SpectatorSession s = session(sender);
                if (s != null) plugin.spectators().cycleAndFollow(s, sub.equals("next") ? 1 : -1);
                return true;
            }
            case "menu":
            case "filters":
            case "events":
            case "info":
            case "settings":
                menu(sender, sub);
                return true;
            case "preset":
                preset(sender, args);
                return true;
            case "freeze":
            case "unfreeze":
                freeze(sender, args, sub.equals("freeze"));
                return true;
            case "eliminate":
                if (!perm(sender, "spectatorplus.game")) return true;
                if (args.length < 2) {
                    usage(sender, label + " eliminate <joueur>");
                    return true;
                }
                PlatformPlayer el = target(sender, args[1]);
                if (el != null) {
                    plugin.game().eliminate(el);
                    msg(sender, "admin.eliminated", "player", el.getName());
                }
                return true;
            case "mode":
                mode(sender, args);
                return true;
            case "game":
                game(sender, label, args);
                return true;
            case "event":
                testEvent(sender, label, args);
                return true;
            case "resetfilters":
                resetFilters(sender, args);
                return true;
            case "lang":
                language(sender, args);
                return true;
            case "reload":
                if (!perm(sender, "spectatorplus.reload")) return true;
                plugin.reload();
                msg(sender, "admin.reloaded");
                return true;
            default:
                msg(sender, "errors.unknown-command", "label", label);
                return true;
        }
    }

    private void help(Sender s, String label) {
        for (String line : plugin.messages().list(s, "help", "label", label)) s.sendMessage(line);
    }

    private void usage(Sender s, String usage) {
        msg(s, "errors.usage", "usage", "/" + usage);
    }

    private void join(Sender sender, String[] args) {
        PlatformPlayer target;
        if (args.length > 1) {
            if (!perm(sender, "spectatorplus.join.others")) return;
            target = target(sender, args[1]);
        } else {
            if (!perm(sender, "spectatorplus.join")) return;
            target = player(sender);
        }
        if (target == null) return;
        if (plugin.spectators().isSpectator(target)) {
            msg(sender, "errors.already-spectator", "player", target.getName());
            return;
        }
        if (!plugin.spectators().enter(target, EnterReason.COMMAND, null, sender.hasPermission("spectatorplus.admin"))) {
            msg(sender, "errors.enter-refused", "player", target.getName());
        } else if (!target.equals(sender)) {
            msg(sender, "admin.joined", "player", target.getName());
        }
    }

    private void leave(Sender sender, String[] args) {
        PlatformPlayer target;
        if (args.length > 1) {
            if (!perm(sender, "spectatorplus.leave.others")) return;
            target = target(sender, args[1]);
        } else {
            if (!perm(sender, "spectatorplus.leave")) return;
            target = player(sender);
        }
        if (target == null) return;
        if (!plugin.spectators().isSpectator(target)) {
            msg(sender, "errors.not-spectator-other", "player", target.getName());
            return;
        }
        if (!plugin.spectators().leave(target, LeaveReason.COMMAND, sender.hasPermission("spectatorplus.admin"))) {
            msg(sender, "errors.leave-refused", "player", target.getName());
        } else if (!target.equals(sender)) {
            msg(sender, "admin.left", "player", target.getName());
        }
    }

    private void list(Sender sender) {
        if (!perm(sender, "spectatorplus.list")) return;
        List<String> names = new ArrayList<>();
        for (SpectatorSession s : plugin.spectators().getSpectators()) {
            PlatformPlayer p = s.getPlayer();
            if (p != null) names.add(p.getName() + " (" + s.getState().name().toLowerCase(Locale.ROOT) + ")");
        }
        msg(sender, "admin.list", "count", names.size(), "players", names.isEmpty() ? "-" : String.join(", ", names));
    }

    private void targeted(Sender sender, String sub, String[] args) {
        SpectatorSession s = session(sender);
        if (s == null) return;
        if (sub.equals("follow") && args.length > 1 && args[1].equalsIgnoreCase("stop")) {
            plugin.spectators().stopFollowing(s, true);
            return;
        }
        if (sub.equals("pov") && args.length > 1 && args[1].equalsIgnoreCase("stop")) {
            plugin.spectators().stopPov(s);
            return;
        }
        if (args.length < 2) {
            usage(sender, "spec " + sub + " <joueur>");
            return;
        }
        PlatformPlayer t = target(sender, args[1]);
        if (t == null) return;
        if (plugin.spectators().isSpectator(t)) {
            msg(sender, "errors.invalid-target");
            return;
        }
        switch (sub) {
            case "tp":
                plugin.spectators().teleport(s, t);
                break;
            case "follow":
                plugin.spectators().follow(s, t);
                break;
            case "pov":
                plugin.spectators().startPov(s, t);
                break;
            case "inv":
                s.openInventory(t, false);
                break;
            case "ec":
                s.openInventory(t, true);
                break;
            default:
                s.openInspection(t);
                break;
        }
    }

    private void menu(Sender sender, String sub) {
        PlatformPlayer p = player(sender);
        if (p == null) return;
        boolean spectator = plugin.spectators().isSpectator(p);
        if (!spectator && !(sub.equals("filters") || sub.equals("events")) ) {
            msg(sender, "errors.not-spectator");
            return;
        }
        if (!spectator && !p.hasPermission("spectatorplus.events.receive")) {
            msg(sender, "errors.not-spectator");
            return;
        }
        switch (sub) {
            case "filters":
                new FiltersMenu(plugin, p).open();
                break;
            case "events":
                new HistoryMenu(plugin, p).open();
                break;
            case "info":
                new GameInfoMenu(plugin, p).open();
                break;
            case "settings":
                new SettingsMenu(plugin, p).open();
                break;
            default:
                new MainMenu(plugin, p).open();
                break;
        }
    }

    private void preset(Sender sender, String[] args) {
        PlatformPlayer p = player(sender);
        if (p == null) return;
        if (args.length < 2) {
            msg(sender, "filters.presets", "presets", String.join(", ", plugin.filters().getPresets()));
            return;
        }
        if (!plugin.filters().canModify(p, "preset")) {
            msg(sender, "errors.filter-locked");
            return;
        }
        if (plugin.filters().setPreset(p, args[1])) {
            msg(sender, "filters.preset-applied", "preset", plugin.messages().presetName(plugin.messages().lang(sender), plugin.filters().preset(args[1])));
        } else {
            msg(sender, "errors.unknown-preset", "preset", args[1]);
        }
    }

    private void language(Sender sender, String[] args) {
        PlatformPlayer p = player(sender);
        if (p == null) return;
        if (args.length < 2) {
            List<String> names = new ArrayList<>();
            for (String code : plugin.messages().available()) names.add(code + " (" + plugin.messages().displayName(code) + ")");
            msg(sender, "language.current", "language", plugin.messages().displayName(plugin.messages().lang(p)),
                    "languages", String.join(", ", names));
            return;
        }
        String code = args[1].toLowerCase(Locale.ROOT);
        if (!code.equals("auto") && !plugin.messages().exists(code)) {
            msg(sender, "errors.unknown-language", "language", args[1]);
            return;
        }
        String old = plugin.filters().get(p).language;
        plugin.filters().get(p).language = code;
        plugin.filters().changed(p, "settings.language", old, code);
        plugin.spectators().refreshHotbar(p);
        msg(sender, "language.changed", "language", plugin.messages().displayName(plugin.messages().lang(p)));
    }

    private void freeze(Sender sender, String[] args, boolean freeze) {
        if (!perm(sender, "spectatorplus.freeze")) return;
        if (args.length < 2) {
            usage(sender, "spec " + (freeze ? "freeze" : "unfreeze") + " <joueur>");
            return;
        }
        PlatformPlayer t = target(sender, args[1]);
        if (t == null) return;
        SpectatorSession s = plugin.spectators().getSpectator(t);
        if (s == null) {
            msg(sender, "errors.not-spectator-other", "player", t.getName());
            return;
        }
        s.setFrozen(freeze);
        msg(sender, freeze ? "admin.frozen" : "admin.unfrozen", "player", t.getName());
    }

    private void mode(Sender sender, String[] args) {
        if (!perm(sender, "spectatorplus.admin")) return;
        if (args.length < 2) {
            msg(sender, "admin.mode", "mode", plugin.getMode().name());
            return;
        }
        try {
            SpectatorMode m = SpectatorMode.valueOf(args[1].toUpperCase(Locale.ROOT).replace('-', '_'));
            plugin.setMode(m);
            msg(sender, "admin.mode-set", "mode", m.name());
        } catch (IllegalArgumentException e) {
            usage(sender, "spec mode <MANUAL|SEMI_AUTO|AUTO>");
        }
    }

    private void game(Sender sender, String label, String[] args) {
        if (!perm(sender, "spectatorplus.game")) return;
        if (args.length < 2) {
            usage(sender, label + " game <start|end|pvp|grace|episode|endepisode|winner|teamwin>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        try {
            switch (action) {
                case "start":
                    plugin.game().startGame();
                    break;
                case "end":
                    plugin.game().endGame();
                    break;
                case "pvp":
                    plugin.game().startPvp();
                    break;
                case "grace":
                    plugin.game().endGracePeriod();
                    break;
                case "episode":
                    plugin.game().startEpisode(Integer.parseInt(args[2]));
                    break;
                case "endepisode":
                    plugin.game().endEpisode(Integer.parseInt(args[2]));
                    break;
                case "winner": {
                    PlatformPlayer t = target(sender, args[2]);
                    if (t == null) return;
                    plugin.game().declareWinner(t);
                    break;
                }
                case "teamwin":
                    plugin.game().declareWinningTeam(args[2]);
                    break;
                default:
                    usage(sender, label + " game <start|end|pvp|grace|episode|endepisode|winner|teamwin>");
                    return;
            }
            msg(sender, "admin.game-updated", "action", action);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            usage(sender, label + " game " + action + " <valeur>");
        }
    }

    private void testEvent(Sender sender, String label, String[] args) {
        if (!perm(sender, "spectatorplus.admin")) return;
        if (args.length < 2) {
            usage(sender, label + " event <id> [joueur]");
            return;
        }
        SpectatorEventType type = plugin.events().getType(args[1]);
        if (type == null) {
            msg(sender, "errors.unknown-event", "event", args[1]);
            return;
        }
        PlatformPlayer p = args.length > 2 ? target(sender, args[2]) : sender instanceof PlatformPlayer ? (PlatformPlayer) sender : null;
        plugin.events().fire(plugin.events().builder(type.getId()).player(p).data("test", "true").build());
        msg(sender, "admin.event-fired", "event", type.getId());
    }

    private void resetFilters(Sender sender, String[] args) {
        if (args.length < 2) {
            PlatformPlayer p = player(sender);
            if (p == null) return;
            plugin.filters().resetFilters(p);
            msg(sender, "filters.reset");
            return;
        }
        if (!perm(sender, "spectatorplus.admin")) return;
        if (args[1].equals("*")) {
            plugin.filters().resetAll();
            msg(sender, "admin.filters-reset-all");
            return;
        }
        PlatformPlayer t = target(sender, args[1]);
        if (t == null) return;
        plugin.filters().resetFilters(t);
        msg(sender, "admin.filters-reset", "player", t.getName());
    }

    // ------------------------------------------------------------------ complétion

    /** Suggestions pour le dernier argument. */
    public List<String> complete(Sender sender, String[] args) {
        if (args.length == 1) return filter(SUBS, args[0]);
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "preset":
                    return filter(new ArrayList<>(plugin.filters().getPresets()), args[1]);
                case "mode":
                    return filter(Arrays.asList("MANUAL", "SEMI_AUTO", "AUTO"), args[1]);
                case "lang": {
                    List<String> codes = new ArrayList<>(plugin.messages().available());
                    codes.add("auto");
                    return filter(codes, args[1]);
                }
                case "game":
                    return filter(Arrays.asList("start", "end", "pvp", "grace", "episode", "endepisode", "winner", "teamwin"), args[1]);
                case "event": {
                    List<String> ids = new ArrayList<>();
                    for (SpectatorEventType t : plugin.events().getTypes()) ids.add(t.getId());
                    return filter(ids, args[1]);
                }
                case "follow":
                case "pov": {
                    List<String> res = players();
                    res.add("stop");
                    return filter(res, args[1]);
                }
                case "join":
                case "leave":
                case "tp":
                case "inspect":
                case "inv":
                case "ec":
                case "freeze":
                case "unfreeze":
                case "eliminate":
                case "resetfilters":
                    return filter(players(), args[1]);
                default:
                    return Collections.emptyList();
            }
        }
        if (args.length == 3 && (sub.equals("event") || (sub.equals("game") && args[1].equalsIgnoreCase("winner")))) {
            return filter(players(), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> players() {
        List<String> res = new ArrayList<>();
        for (PlatformPlayer p : plugin.platform().getOnlinePlayers()) res.add(p.getName());
        return res;
    }

    private static List<String> filter(List<String> in, String prefix) {
        List<String> res = new ArrayList<>();
        String p = prefix.toLowerCase(Locale.ROOT);
        for (String s : in) if (s.toLowerCase(Locale.ROOT).startsWith(p)) res.add(s);
        return res;
    }
}
