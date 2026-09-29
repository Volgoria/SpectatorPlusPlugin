package fr.spectatorplus.spectator;

import fr.spectatorplus.core.platform.PlatformPlayer;
import fr.spectatorplus.core.platform.Position;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/** Joueurs de test : seuls l'identifiant, le nom et la position répondent. */
final class FakePlayers {

    private FakePlayers() {
    }

    static PlatformPlayer at(final String name, final String world, final double x, final double z) {
        final UUID id = UUID.nameUUIDFromBytes(name.getBytes());
        return (PlatformPlayer) Proxy.newProxyInstance(PlatformPlayer.class.getClassLoader(),
                new Class<?>[]{PlatformPlayer.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        switch (method.getName()) {
                            case "getUniqueId":
                                return id;
                            case "getName":
                            case "toString":
                                return name;
                            case "getLocation":
                                return new Position(world, x, 64, z);
                            case "isOnline":
                                return true;
                            case "equals":
                                return proxy == args[0];
                            case "hashCode":
                                return id.hashCode();
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    }
                });
    }
}
