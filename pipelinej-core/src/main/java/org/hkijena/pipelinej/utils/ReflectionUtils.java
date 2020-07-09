/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.utils;

import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.lang.reflect.MethodUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Utilities around reflection
 */
public class ReflectionUtils {

    private static final Map<Class<?>, Class<?>> primitiveWrapperMap = createPrimitveWrapperMap();

    private ReflectionUtils() {

    }

    public static Attributes getManifestAttributes() {
        try {
            String clz = ReflectionUtils.class.getSimpleName() + ".class";
            String pth = ReflectionUtils.class.getResource(clz).toString();
            String mnf = pth.substring(0, pth.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            URL url = new URL(mnf);
            Manifest manifest = new Manifest(url.openStream());
            return manifest.getMainAttributes();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<Class<?>, Class<?>> createPrimitveWrapperMap() {
        Map<Class<?>, Class<?>> result = new HashMap<>();
        result.put(boolean.class, Boolean.class);
        result.put(byte.class, Byte.class);
        result.put(char.class, Character.class);
        result.put(double.class, Double.class);
        result.put(float.class, Float.class);
        result.put(int.class, Integer.class);
        result.put(long.class, Long.class);
        result.put(short.class, Short.class);
        return result;
    }

    /**
     * Returns true if the class is a primitive wrapper
     *
     * @param targetClass target class
     * @param primitive   primitive class
     * @return if the class is a primitive wrapper
     */
    public static boolean isPrimitiveWrapperOf(Class<?> targetClass, Class<?> primitive) {
        if (!primitive.isPrimitive()) {
            throw new IllegalArgumentException("First argument has to be primitive type");
        }
        return primitiveWrapperMap.get(primitive) == targetClass;
    }

    /**
     * More powerful variant of Class.isAssignableFrom that supports primitives.
     * Because Oracle cannot be bothered to update their ancient API.
     *
     * @param from from class
     * @param to   to class
     * @return if to can be assigned from from
     */
    public static boolean isAssignableTo(Class<?> from, Class<?> to) {
        if (to.isAssignableFrom(from)) {
            return true;
        }
        if (from.isPrimitive()) {
            return isPrimitiveWrapperOf(to, from);
        }
        if (to.isPrimitive()) {
            return isPrimitiveWrapperOf(from, to);
        }
        return false;
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
    public static Object invokeMethod(Object target, String functionName, Object... args) {
        try {
            return MethodUtils.invokeMethod(target, functionName, args);
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
