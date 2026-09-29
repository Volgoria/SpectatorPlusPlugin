package fr.spectatorplus.mod;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Points de vie sous le pseudo des joueurs, visibles uniquement par les spectateurs.
 * <p>
 * L'objectif n'existe que chez les spectateurs : il leur est envoyé par paquets, sans toucher au
 * scoreboard du serveur (équipes, sidebar et objectifs des autres mods restent intacts).
 */
public final class ModHealthDisplay {

    private static final String OBJECTIVE = "sp_health";

    private final ModPlatform platform;
    /** Spectateur → dernier score envoyé pour chaque joueur. */
    private final Map<UUID, Map<String, Integer>> viewers = new ConcurrentHashMap<>();
    private final Scoreboard scoreboard = new Scoreboard();

    ModHealthDisplay(ModPlatform platform) {
        this.platform = platform;
    }

    void set(ServerPlayer viewer, boolean enabled, String title) {
        boolean active = viewers.containsKey(viewer.getUUID());
        if (!enabled) {
            if (active) {
                viewers.remove(viewer.getUUID());
                viewer.connection.send(new ClientboundSetObjectivePacket(objective(""), 1));
            }
            return;
        }
        Objective o = objective(title);
        if (active) {
            // déjà affiché : seul le titre est mis à jour (recréer l'objectif ferait planter le client)
            viewer.connection.send(new ClientboundSetObjectivePacket(o, 2));
            return;
        }
        viewer.connection.send(new ClientboundSetObjectivePacket(o, 0));
        viewer.connection.send(Mc.displayBelowName(o));
        viewers.put(viewer.getUUID(), new HashMap<String, Integer>());
    }

    private Objective objective(String title) {
        Component name = Texts.of(title);
        //? if >=1.20.3 {
        return new Objective(scoreboard, OBJECTIVE, ObjectiveCriteria.DUMMY, name, ObjectiveCriteria.RenderType.INTEGER, false, null);
        //?} else
        /*return new Objective(scoreboard, OBJECTIVE, ObjectiveCriteria.DUMMY, name, ObjectiveCriteria.RenderType.INTEGER);*/
    }

    /** Envoie les points de vie qui ont changé (appelé toutes les demi-secondes). */
    void tick() {
        if (viewers.isEmpty()) return;
        for (Map.Entry<UUID, Map<String, Integer>> e : viewers.entrySet()) {
            ServerPlayer viewer = platform.server().getPlayerList().getPlayer(e.getKey());
            if (viewer == null) continue;
            Map<String, Integer> sent = e.getValue();
            for (ServerPlayer p : platform.server().getPlayerList().getPlayers()) {
                int health = (int) Math.ceil(p.getHealth());
                String name = p.getScoreboardName();
                Integer last = sent.get(name);
                if (last != null && last == health) continue;
                sent.put(name, health);
                viewer.connection.send(Mc.score(name, OBJECTIVE, health));
            }
        }
    }

    void forget(UUID id) {
        viewers.remove(id);
    }
}
