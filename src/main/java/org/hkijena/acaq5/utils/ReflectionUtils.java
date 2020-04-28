package org.hkijena.acaq5.utils;

import java.lang.reflect.Field;

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
     * @return instance
     */
    public static Object newInstance(Class<?> klass) {
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
                return klass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
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
}
