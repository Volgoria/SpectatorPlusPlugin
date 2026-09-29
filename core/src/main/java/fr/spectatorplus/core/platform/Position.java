package fr.spectatorplus.core.platform;

/**
 * Position immuable indépendante de la plateforme : nom du monde, coordonnées et orientation.
 */
public final class Position {

    private final String world;
    private final double x, y, z;
    private final float yaw, pitch;

    public Position(String world, double x, double y, double z) {
        this(world, x, y, z, 0f, 0f);
    }

    public Position(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static Position of(String world, Vector3 v, float yaw, float pitch) {
        return new Position(world, v.getX(), v.getY(), v.getZ(), yaw, pitch);
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public int getBlockX() {
        return (int) Math.floor(x);
    }

    public int getBlockY() {
        return (int) Math.floor(y);
    }

    public int getBlockZ() {
        return (int) Math.floor(z);
    }

    public boolean sameWorld(Position o) {
        return o != null && world != null && world.equals(o.world);
    }

    public double distanceSquared(Position o) {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distance(Position o) {
        return Math.sqrt(distanceSquared(o));
    }

    public Vector3 toVector() {
        return new Vector3(x, y, z);
    }

    public Position add(double dx, double dy, double dz) {
        return new Position(world, x + dx, y + dy, z + dz, yaw, pitch);
    }

    public Position withY(double newY) {
        return new Position(world, x, newY, z, yaw, pitch);
    }

    public Position withRotation(float newYaw, float newPitch) {
        return new Position(world, x, y, z, newYaw, newPitch);
    }

    /** Direction du regard (même calcul que Location#getDirection de Bukkit). */
    public Vector3 getDirection() {
        double rotX = Math.toRadians(yaw), rotY = Math.toRadians(pitch);
        double xz = Math.cos(rotY);
        return new Vector3(-xz * Math.sin(rotX), -Math.sin(rotY), xz * Math.cos(rotX));
    }

    @Override
    public String toString() {
        return world + "(" + x + "," + y + "," + z + ")";
    }
}
