package fr.spectatorplus.game;

import fr.spectatorplus.core.platform.Platform;

/**
 * TPS du serveur : valeur fournie par la plateforme (Paper...) si disponible, sinon mesure maison.
 */
public final class TpsMonitor implements Runnable {

    private long last = System.nanoTime();
    private double tps = 20.0;
    private Platform platform;

    public void start(Platform platform) {
        this.platform = platform;
        platform.runTimer(this, 100L, 100L);
    }

    @Override
    public void run() {
        long now = System.nanoTime();
        double seconds = (now - last) / 1_000_000_000.0;
        last = now;
        if (seconds > 0) tps = Math.min(20.0, 100.0 / seconds);
    }

    public double current() {
        double[] paper = platform == null ? null : platform.serverTps();
        if (paper != null && paper.length > 0) return Math.min(20.0, paper[0]);
        return tps;
    }
}
