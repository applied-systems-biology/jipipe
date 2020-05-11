package org.hkijena.acaq5.utils;

import org.apache.commons.lang.reflect.ConstructorUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utilities around reflection
 */
public class ReflectionUtils {
    private ReflectionUtils() {

    }

    /**
     * Creates a new class instance.
     * Can handle primitives
     * Can handle enums
     *
     * @param klass instantiated class
     * @param args  for primitives, ignored. For classes passed to the constructor
     * @return instance
     */
    public static Object newInstance(Class<?> klass, Object... args) {
        if (klass == int.class) {
            return 0;
        } else if (klass == long.class) {
            return 0L;
        } else if (klass == byte.class) {
            return (byte) 0;
        } else if (klass == float.class) {
            return 0f;
        } else if (klass == double.class) {
            return 0d;
        } else if (klass == boolean.class) {
            return false;
        } else if (klass == char.class) {
            return '\0';
        } else if (klass == short.class) {
            return (short) 0;
        } else if (Enum.class.isAssignableFrom(klass)) {
            try {
                return getEnumValues(klass)[0];
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                return ConstructorUtils.invokeConstructor(klass, args);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Gets all enum values via reflection
     *
     * @param <E>       the enum class
     * @param enumClass the enum class
     * @return all enum values
     * @throws NoSuchFieldException   triggered by reflection
     * @throws IllegalAccessException triggered by reflection
     */
    public static <E extends Enum> E[] getEnumValues(Class<?> enumClass)
            throws NoSuchFieldException, IllegalAccessException {
        Field f = enumClass.getDeclaredField("$VALUES");
        f.setAccessible(true);
        Object o = f.get(null);
        return (E[]) o;
    }

    /**
     * Calls a function and returns its output
     *
     * @param target       The object
     * @param functionName the function
     * @return the output
     */
    public static Object invokeMethod(Object target, String functionName) {
        try {
            Method declaredMethod = target.getClass().getDeclaredMethod(functionName);
            declaredMethod.setAccessible(true);
            return declaredMethod.invoke(target);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if the class has a default constructor
     *
     * @param klass the class
     * @return if the class has a default constructor
     */
    public static boolean hasDefaultConstructor(Class<?> klass) {
        for (Constructor<?> constructor : klass.getConstructors()) {
            // In Java 7-, use getParameterTypes and check the length of the array returned
            if (constructor.getParameterTypes().length == 0) {
                return true;
            }
        }
        return false;
    }
}
