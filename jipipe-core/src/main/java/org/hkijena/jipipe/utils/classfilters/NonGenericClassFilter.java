package org.hkijena.jipipe.utils.classfilters;

import org.hkijena.jipipe.utils.ReflectionUtils;

public class NonGenericClassFilter implements ClassFilter {
    @Override
    public boolean test(Class<?> aClass) {
        return !ReflectionUtils.isAbstractOrInterface(aClass);
    }
}
