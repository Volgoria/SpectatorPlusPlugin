package fr.spectatorplus.compat;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Petits utilitaires de réflexion avec cache.
 * Toutes les méthodes sont "silencieuses" : elles renvoient null en cas d'échec.
 */
public final class Reflect {

    private static final Object MISSING = new Object();
    private static final Map<String, Object> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> CLASS_CACHE = new ConcurrentHashMap<>();

    private Reflect() {
    }

    public static boolean classExists(String name) {
        Boolean cached = CLASS_CACHE.get(name);
        if (cached != null) return cached;
        boolean exists;
        try {
            Class.forName(name);
            exists = true;
        } catch (Throwable t) {
            exists = false;
        }
        CLASS_CACHE.put(name, exists);
        return exists;
    }

    public static Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean hasMethod(Object target, String name, int argCount) {
        return target != null && find(target.getClass(), name, argCount, null) != null;
    }

    /** Appelle une méthode publique par son nom ; renvoie null si elle n'existe pas ou échoue. */
    public static Object invoke(Object target, String name, Object... args) {
        if (target == null) return null;
        Method m = find(target.getClass(), name, args.length, args);
        if (m == null) return null;
        try {
            return m.invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Comme invoke mais indique si l'appel a réellement eu lieu. */
    public static boolean tryInvoke(Object target, String name, Object... args) {
        if (target == null) return false;
        Method m = find(target.getClass(), name, args.length, args);
        if (m == null) return false;
        try {
            m.invoke(target, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Object invokeStatic(Class<?> clazz, String name, Object... args) {
        if (clazz == null) return null;
        Method m = find(clazz, name, args.length, args);
        if (m == null) return null;
        try {
            return m.invoke(null, args);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object getStaticField(Class<?> clazz, String name) {
        if (clazz == null) return null;
        try {
            return clazz.getField(name).get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object getField(Object target, String name) {
        if (target == null) return null;
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static Method find(Class<?> clazz, String name, int argCount, Object[] args) {
        StringBuilder key = new StringBuilder(clazz.getName()).append('#').append(name).append('/').append(argCount);
        if (args != null) {
            for (Object a : args) key.append(',').append(a == null ? "null" : a.getClass().getName());
        }
        String k = key.toString();
        Object cached = METHOD_CACHE.get(k);
        if (cached == MISSING) return null;
        if (cached != null) return (Method) cached;

        Method found = null;
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterTypes().length != argCount) continue;
            if (args != null && !compatible(m.getParameterTypes(), args)) continue;
            found = m;
            break;
        }
        if (found != null) {
            try {
                found.setAccessible(true);
            } catch (Throwable ignored) {
            }
            METHOD_CACHE.put(k, found);
        } else {
            METHOD_CACHE.put(k, MISSING);
        }
        return found;
    }

    private static boolean compatible(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            Object a = args[i];
            Class<?> t = wrap(types[i]);
            if (a == null) {
                if (types[i].isPrimitive()) return false;
                continue;
            }
            if (!t.isInstance(a)) return false;
        }
        return true;
    }

    private static Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == boolean.class) return Boolean.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        if (c == long.class) return Long.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        if (c == char.class) return Character.class;
        return c;
    }
}
