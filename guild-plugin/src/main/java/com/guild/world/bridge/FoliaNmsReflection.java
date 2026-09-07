package com.guild.world.bridge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * NMS 反射工具（依赖 {@link FoliaPlatformProbe} 解析的 ClassLoader）。
 */
final class FoliaNmsReflection {

    private FoliaNmsReflection() {
    }

    static Class<?> clazz(String name) {
        try {
            return Class.forName(name, true, FoliaPlatformProbe.nmsClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("NMS class not found: " + name, e);
        }
    }

    static Class<?> tryClazz(String name) {
        try {
            return Class.forName(name, true, FoliaPlatformProbe.nmsClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    static Object invoke(Object target, String name, Object... args) throws Exception {
        return invokeMethod(target.getClass(), target, name, args);
    }

    static Object invokeStatic(Class<?> cls, String name, Object... args) throws Exception {
        return invokeMethod(cls, null, name, args);
    }

    static Object tryInvoke(Object target, String name, Object... args) throws Exception {
        Method m = findMethod(target.getClass(), name, argClasses(args));
        if (m == null) {
            return null;
        }
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    static Object tryInvokeStatic(Class<?> cls, String name, Object... args) throws Exception {
        if (cls == null) {
            return null;
        }
        Method m = findMethod(cls, name, argClasses(args));
        if (m == null) {
            return null;
        }
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    static boolean tryInvokeVoid(Object target, String name, Object... args) throws Exception {
        Method m = findMethod(target.getClass(), name, argClasses(args));
        if (m == null) {
            return false;
        }
        m.setAccessible(true);
        m.invoke(target, args);
        return true;
    }

    static Object construct(Class<?> cls, Object... args) throws Exception {
        Object o = tryConstruct(cls, args);
        if (o == null) {
            throw new NoSuchMethodException(cls.getName() + Arrays.toString(argClasses(args)));
        }
        return o;
    }

    static Object tryConstruct(Class<?> cls, Object... args) throws Exception {
        if (cls == null) {
            return null;
        }
        Class<?>[] types = argClasses(args);
        Constructor<?> exact = null;
        Constructor<?> assignable = null;
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            Class<?>[] pts = c.getParameterTypes();
            if (pts.length != types.length) {
                continue;
            }
            boolean isExact = true;
            boolean isAssign = true;
            for (int i = 0; i < pts.length; i++) {
                if (types[i] == null) {
                    continue;
                }
                if (pts[i] != types[i]) {
                    isExact = false;
                }
                if (!matches(pts[i], types[i])) {
                    isAssign = false;
                    break;
                }
            }
            if (isExact) {
                exact = c;
                break;
            }
            if (isAssign && assignable == null) {
                assignable = c;
            }
        }
        Constructor<?> c = exact != null ? exact : assignable;
        if (c == null) {
            return null;
        }
        c.setAccessible(true);
        return c.newInstance(args);
    }

    static Object getField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        if (f == null) {
            throw new NoSuchFieldException(target.getClass().getName() + "#" + name);
        }
        return f.get(target);
    }

    static Object requireField(Object target, String name) throws Exception {
        return getField(target, name);
    }

    static Object tryGetField(Object target, String name) {
        try {
            Field f = findField(target.getClass(), name);
            return f == null ? null : f.get(target);
        } catch (Exception e) {
            return null;
        }
    }

    static void setField(Object target, String name, Object value) throws Exception {
        Field f = findField(target.getClass(), name);
        if (f == null) {
            throw new NoSuchFieldException(target.getClass().getName() + "#" + name);
        }
        f.set(target, value);
    }

    static void trySetField(Object target, String name, Object value) {
        if (value == null) {
            return;
        }
        try {
            setField(target, name, value);
        } catch (Exception ignored) {
        }
    }

    static Object getStatic(Class<?> cls, String field) throws Exception {
        Field f = findField(cls, field);
        if (f == null) {
            throw new NoSuchFieldException(cls.getName() + "#" + field);
        }
        return f.get(null);
    }

    static Object unwrapOptional(Object value) {
        if (value instanceof Optional<?> opt) {
            return opt.orElse(null);
        }
        return value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object enumConstant(String className, String name) {
        return Enum.valueOf((Class<? extends Enum>) clazz(className), name);
    }

    private static Object invokeMethod(Class<?> cls, Object target, String name, Object[] args) throws Exception {
        Method m = findMethod(cls, name, argClasses(args));
        if (m == null) {
            throw new NoSuchMethodException(cls.getName() + "#" + name + Arrays.toString(argClasses(args)));
        }
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static Field findField(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
            try {
                Field f = c.getField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>[] params) {
        Method fallback = null;
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name)) {
                    continue;
                }
                Class<?>[] pts = m.getParameterTypes();
                if (pts.length != params.length) {
                    continue;
                }
                boolean exact = true;
                boolean assign = true;
                for (int i = 0; i < pts.length; i++) {
                    if (params[i] == null) {
                        continue;
                    }
                    if (pts[i] != params[i]) {
                        exact = false;
                    }
                    if (!matches(pts[i], params[i])) {
                        assign = false;
                        break;
                    }
                }
                if (exact) {
                    return m;
                }
                if (assign && fallback == null) {
                    fallback = m;
                }
            }
        }
        for (Method m : cls.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != params.length) {
                continue;
            }
            Class<?>[] pts = m.getParameterTypes();
            boolean assign = true;
            for (int i = 0; i < pts.length; i++) {
                if (params[i] != null && !matches(pts[i], params[i])) {
                    assign = false;
                    break;
                }
            }
            if (assign) {
                return m;
            }
        }
        return fallback;
    }

    private static Class<?>[] argClasses(Object[] args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] == null ? null : args[i].getClass();
        }
        return types;
    }

    private static boolean matches(Class<?> declared, Class<?> actual) {
        if (actual == null) {
            return true;
        }
        if (declared.isAssignableFrom(actual)) {
            return true;
        }
        if (declared == boolean.class && actual == Boolean.class) return true;
        if (declared == int.class && actual == Integer.class) return true;
        if (declared == long.class && actual == Long.class) return true;
        if (declared == float.class && actual == Float.class) return true;
        if (declared == double.class && actual == Double.class) return true;
        if (declared == byte.class && actual == Byte.class) return true;
        if (declared == short.class && actual == Short.class) return true;
        if (declared == char.class && actual == Character.class) return true;
        return false;
    }
}
