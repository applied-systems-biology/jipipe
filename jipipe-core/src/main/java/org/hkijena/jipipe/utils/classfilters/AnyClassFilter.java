package org.hkijena.jipipe.utils.classfilters;

public class AnyClassFilter implements ClassFilter {
    @Override
    public boolean test(Class<?> aClass) {
        return true;
    }
}
