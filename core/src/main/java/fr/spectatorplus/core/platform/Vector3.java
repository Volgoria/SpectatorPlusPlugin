package fr.spectatorplus.core.platform;

/**
 * Vecteur 3D mutable (mêmes opérations que {@code org.bukkit.util.Vector}, pour porter le code tel quel).
 */
public final class Vector3 implements Cloneable {

    private double x, y, z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
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

    public int getBlockX() {
        return (int) Math.floor(x);
    }

    public int getBlockY() {
        return (int) Math.floor(y);
    }

    public int getBlockZ() {
        return (int) Math.floor(z);
    }

    public Vector3 setX(double x) {
        this.x = x;
        return this;
    }

    public Vector3 setY(double y) {
        this.y = y;
        return this;
    }

    public Vector3 setZ(double z) {
        this.z = z;
        return this;
    }

    public Vector3 add(Vector3 o) {
        x += o.x;
        y += o.y;
        z += o.z;
        return this;
    }

    public Vector3 subtract(Vector3 o) {
        x -= o.x;
        y -= o.y;
        z -= o.z;
        return this;
    }

    public Vector3 multiply(double m) {
        x *= m;
        y *= m;
        z *= m;
        return this;
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public Vector3 normalize() {
        double l = length();
        if (l > 0) {
            x /= l;
            y /= l;
            z /= l;
        }
        return this;
    }

    public double distanceSquared(Vector3 o) {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public Vector3 clone() {
        return new Vector3(x, y, z);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}
