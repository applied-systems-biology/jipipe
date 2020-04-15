package org.hkijena.acaq5.utils;

/**
 * Utilities around reflection
 */
public class ReflectionUtils {
    private ReflectionUtils() {

    }

    /**
     * Creates a new class instance.
     * Can handle primitives
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
        } else {
            try {
                return klass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
