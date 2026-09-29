package fr.spectatorplus.bukkit;

import fr.spectatorplus.compat.Reflect;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Points de vie sous le pseudo des joueurs, visibles uniquement par les spectateurs.
 * <p>
 * L'affichage « sous le pseudo » d'un scoreboard est vu par tous ceux qui partagent ce scoreboard :
 * <ul>
 *     <li>spectateur sur le scoreboard principal : il reçoit une copie de ce scoreboard (équipes, sidebar,
 *     scores synchronisés chaque seconde) qui contient en plus l'objectif de vie ;</li>
 *     <li>spectateur qui a déjà un scoreboard personnel (donné par un autre plugin) : l'objectif y est ajouté.</li>
 * </ul>
 * L'objectif utilise le critère « health », mis à jour automatiquement par le serveur.
 */
final class BukkitHealthDisplay {

    private static final String OBJECTIVE = "sp_health";

    private final Plugin plugin;
    /** Copies du scoreboard principal données aux spectateurs. */
    private final Map<UUID, Scoreboard> mirrors = new HashMap<>();
    /** Spectateurs dont le scoreboard personnel a reçu l'objectif. */
    private final Set<UUID> personal = new HashSet<>();
    private boolean started;

    BukkitHealthDisplay(Plugin plugin) {
        this.plugin = plugin;
    }

    void set(Player p, boolean enabled, String title) {
        if (Bukkit.getScoreboardManager() == null) return;
        if (!enabled) {
            disable(p);
            return;
        }
        start();
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Scoreboard board = mirrors.get(p.getUniqueId());
        if (board == null) {
            if (p.getScoreboard() == main) {
                board = Bukkit.getScoreboardManager().getNewScoreboard();
                sync(main, board);
                mirrors.put(p.getUniqueId(), board);
                p.setScoreboard(board);
            } else {
                board = p.getScoreboard();
                personal.add(p.getUniqueId());
            }
        }
        Objective o = board.getObjective(OBJECTIVE);
        if (o == null) o = register(board, title);
        if (o == null) return;
        o.setDisplayName(title);
        o.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    private void disable(Player p) {
        Scoreboard mirror = mirrors.remove(p.getUniqueId());
        if (mirror != null) {
            if (p.getScoreboard() == mirror) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            return;
        }
        if (personal.remove(p.getUniqueId())) {
            Objective o = p.getScoreboard().getObjective(OBJECTIVE);
            if (o != null) o.unregister();
        }
    }

    void forget(UUID id) {
        mirrors.remove(id);
        personal.remove(id);
    }

    void shutdown() {
        for (UUID id : new HashSet<>(mirrors.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) disable(p);
        }
        for (UUID id : new HashSet<>(personal)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) disable(p);
        }
        mirrors.clear();
        personal.clear();
    }

    /** Critère « health » : API 1.8 (nom du critère), puis Criteria.HEALTH quand l'ancienne méthode disparaît. */
    @SuppressWarnings("deprecation")
    private static Objective register(Scoreboard board, String title) {
        try {
            return board.registerNewObjective(OBJECTIVE, "health");
        } catch (Throwable ignored) {
            // API récente uniquement
        }
        Class<?> criteria = Reflect.getClass("org.bukkit.scoreboard.Criteria");
        Object health = criteria == null ? null : Reflect.getStaticField(criteria, "HEALTH");
        Object o = Reflect.invoke(board, "registerNewObjective", OBJECTIVE, health, title);
        return o instanceof Objective ? (Objective) o : null;
    }

    // ------------------------------------------------------------------ synchronisation des copies

    private void start() {
        if (started) return;
        started = true;
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (mirrors.isEmpty()) return;
                Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
                for (Map.Entry<UUID, Scoreboard> e : new HashMap<>(mirrors).entrySet()) {
                    Player p = Bukkit.getPlayer(e.getKey());
                    // un autre plugin a remplacé le scoreboard du spectateur : on n'y touche plus
                    if (p == null || p.getScoreboard() != e.getValue()) {
                        mirrors.remove(e.getKey());
                        continue;
                    }
                    try {
                        sync(main, e.getValue());
                    } catch (Throwable ignored) {
                        // scoreboard modifié pendant la copie : réessayé à la prochaine seconde
                    }
                }
            }
        }, 20L, 20L);
    }

    /** Recopie objectifs, scores et équipes du scoreboard principal (sauf l'objectif de vie). */
    @SuppressWarnings("deprecation")
    private static void sync(Scoreboard main, Scoreboard mirror) {
        // objectifs
        Set<String> names = new HashSet<>();
        for (Objective om : main.getObjectives()) {
            if (om.getName().equals(OBJECTIVE)) continue;
            names.add(om.getName());
            Objective o = mirror.getObjective(om.getName());
            if (o != null && !o.getCriteria().equals(om.getCriteria())) {
                o.unregister();
                o = null;
            }
            if (o == null) o = mirror.registerNewObjective(om.getName(), om.getCriteria());
            if (!o.getDisplayName().equals(om.getDisplayName())) o.setDisplayName(om.getDisplayName());
            DisplaySlot slot = om.getDisplaySlot();
            // l'emplacement « sous le pseudo » est réservé à la vie
            if (slot == DisplaySlot.BELOW_NAME) slot = null;
            if (o.getDisplaySlot() != slot) {
                if (slot == null) o.setDisplaySlot(null);
                else o.setDisplaySlot(slot);
            }
            Object render = Reflect.invoke(om, "getRenderType");
            if (render != null) Reflect.tryInvoke(o, "setRenderType", render);
        }
        for (Objective o : new HashSet<>(mirror.getObjectives())) {
            if (!o.getName().equals(OBJECTIVE) && !names.contains(o.getName())) o.unregister();
        }
        // scores
        Set<String> entries = main.getEntries();
        for (String entry : entries) {
            for (Objective om : main.getObjectives()) {
                if (om.getName().equals(OBJECTIVE)) continue;
                Score s = om.getScore(entry);
                Objective o = mirror.getObjective(om.getName());
                if (o == null) continue;
                Score target = o.getScore(entry);
                if (isSet(s)) {
                    if (!isSet(target) || target.getScore() != s.getScore()) target.setScore(s.getScore());
                }
            }
        }
        for (String entry : new HashSet<>(mirror.getEntries())) {
            if (!entries.contains(entry) && Bukkit.getPlayerExact(entry) == null) mirror.resetScores(entry);
        }
        // équipes
        Set<String> teams = new HashSet<>();
        for (Team tm : main.getTeams()) {
            teams.add(tm.getName());
            Team t = mirror.getTeam(tm.getName());
            if (t == null) t = mirror.registerNewTeam(tm.getName());
            copyTeam(tm, t);
        }
        for (Team t : new HashSet<>(mirror.getTeams())) {
            if (!teams.contains(t.getName())) t.unregister();
        }
    }

    private static boolean isSet(Score s) {
        Object set = Reflect.invoke(s, "isScoreSet");
        return set instanceof Boolean ? (Boolean) set : s.getScore() != 0;
    }

    @SuppressWarnings("deprecation")
    private static void copyTeam(Team from, Team to) {
        if (!to.getDisplayName().equals(from.getDisplayName())) to.setDisplayName(from.getDisplayName());
        if (!to.getPrefix().equals(from.getPrefix())) to.setPrefix(from.getPrefix());
        if (!to.getSuffix().equals(from.getSuffix())) to.setSuffix(from.getSuffix());
        if (to.allowFriendlyFire() != from.allowFriendlyFire()) to.setAllowFriendlyFire(from.allowFriendlyFire());
        if (to.canSeeFriendlyInvisibles() != from.canSeeFriendlyInvisibles()) to.setCanSeeFriendlyInvisibles(from.canSeeFriendlyInvisibles());
        // couleur (1.12+) et options (1.9+) : par réflexion
        Object color = Reflect.invoke(from, "getColor");
        if (color != null) Reflect.tryInvoke(to, "setColor", color);
        Class<?> option = Reflect.getClass("org.bukkit.scoreboard.Team$Option");
        if (option != null && option.isEnum()) {
            for (Object opt : option.getEnumConstants()) {
                Object value = Reflect.invoke(from, "getOption", opt);
                if (value != null) Reflect.tryInvoke(to, "setOption", opt, value);
            }
        } else {
            Object visibility = Reflect.invoke(from, "getNameTagVisibility");
            if (visibility != null) Reflect.tryInvoke(to, "setNameTagVisibility", visibility);
        }
        Set<String> entries = from.getEntries();
        for (String e : entries) if (!to.hasEntry(e)) to.addEntry(e);
        for (String e : new HashSet<>(to.getEntries())) if (!entries.contains(e)) to.removeEntry(e);
    }
}
