package org.hkijena.acaq5.api.parameters;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQDocumentation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ACAQParameterAccess {

    private String key;
    private Method getter;
    private Method setter;
    private ACAQDocumentation documentation;
    private ACAQAlgorithm algorithm;

    private ACAQParameterAccess() {

    }

    /**
     * Extracts parameters from an algorithm instance
     * @param algorithm
     * @return
     */
    public static Map<String, ACAQParameterAccess> getParameters(ACAQAlgorithm algorithm) {
        Map<String, ACAQParameterAccess> result = new HashMap<>();
        for(Method method : algorithm.getClass().getMethods()) {
            ACAQParameter[] parameterAnnotations = method.getAnnotationsByType(ACAQParameter.class);
            if(parameterAnnotations.length > 0) {
                ACAQParameter parameterAnnotation = parameterAnnotations[0];
                ACAQParameterAccess access = result.putIfAbsent(parameterAnnotation.value(), new ACAQParameterAccess());
                if(access == null)
                    access = result.get(parameterAnnotation.value());
                access.algorithm = algorithm;
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

            ACAQSubAlgorithm[] subAlgorithms = method.getAnnotationsByType(ACAQSubAlgorithm.class);
            if(subAlgorithms.length > 0) {
                try {
                    ACAQSubAlgorithm subAlgorithmAnnotation = subAlgorithms[0];
                    ACAQAlgorithm subAlgorithm = (ACAQAlgorithm) method.invoke(algorithm);
                    for(Map.Entry<String, ACAQParameterAccess> kv : getParameters(subAlgorithm).entrySet()) {

                        // Do not allow name parameter
                        if(kv.getKey().equals("name"))
                            continue;

                        result.put(subAlgorithmAnnotation.value() + "/" + kv.getKey(), kv.getValue());
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
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

    public <T> T get() {
        try {
            return (T)getGetter().invoke(algorithm);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> boolean set(T value) {
        try {
            Object result = getSetter().invoke(algorithm, value);
            if(result instanceof Boolean) {
                return (boolean)result;
            }
            else {
                return true;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
