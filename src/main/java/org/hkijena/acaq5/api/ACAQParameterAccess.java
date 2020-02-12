package org.hkijena.acaq5.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ACAQParameterAccess {

    private String key;
    private Method getter;
    private Method setter;
    private ACAQDocumentation documentation;

    private ACAQParameterAccess() {

    }

    public static Map<String, ACAQParameterAccess> getParameters(Class<?> klass) {
        Map<String, ACAQParameterAccess> result = new HashMap<>();
        for(Method method : klass.getMethods()) {
            ACAQParameter[] parameterAnnotations = method.getAnnotationsByType(ACAQParameter.class);
            if(parameterAnnotations.length > 0) {
                ACAQParameter parameterAnnotation = parameterAnnotations[0];
                ACAQParameterAccess access = result.putIfAbsent(parameterAnnotation.value(), new ACAQParameterAccess());
                if(access == null)
                    access = result.get(parameterAnnotation.value());
                if(method.getParameters().length == 1) {
                    // Is a setter
                    access.setter = method;
                    access.key = parameterAnnotation.value();

                    ACAQDocumentation[] documentations = method.getAnnotationsByType(ACAQDocumentation.class);
                    if(documentations.length > 0)
                        access.documentation = documentations[0];
                }
                else {
                    // Is a getter
                    access.getter = method;
                    access.key = parameterAnnotation.value();

                    ACAQDocumentation[] documentations = method.getAnnotationsByType(ACAQDocumentation.class);
                    if(documentations.length > 0)
                        access.documentation = documentations[0];
                }
            }

        }
        return result;
    }

    public Method getGetter() {
        return getter;
    }

    public Method getSetter() {
        return setter;
    }

    public Class<?> getFieldClass() {
        return getGetter().getReturnType();
    }

    public ACAQDocumentation getDocumentation() {
        return documentation;
    }

    public String getName() {
        if(getDocumentation() != null)
            return getDocumentation().name();
        return key;
    }

    public String getDescription() {
        if(getDocumentation() != null)
            return getDocumentation().description();
        return null;
    }

    public <T> Instance<T> instantiate(ACAQAlgorithm algorithm) {
        return new Instance<>(algorithm, this);
    }

    public static class Instance<T> {
        private ACAQAlgorithm algorithm;
        private ACAQParameterAccess access;

        public Instance(ACAQAlgorithm algorithm, ACAQParameterAccess access) {
            this.algorithm = algorithm;
            this.access = access;
        }

        public T get() {
            try {
                return (T)access.getGetter().invoke(algorithm);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public void set(T value) {
            try {
                access.getSetter().invoke(algorithm, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
