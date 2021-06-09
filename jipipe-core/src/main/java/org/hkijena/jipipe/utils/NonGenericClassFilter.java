package org.hkijena.jipipe.utils;

public class NonGenericClassFilter implements ClassFilter {
    @Override
    public boolean test(Class<?> aClass) {
        return !ReflectionUtils.isAbstractOrInterface(aClass);
    }
}
