package org.hkijena.jipipe.utils;

public class AnyClassFilter implements ClassFilter {
    @Override
    public boolean test(Class<?> aClass) {
        return true;
    }
}
