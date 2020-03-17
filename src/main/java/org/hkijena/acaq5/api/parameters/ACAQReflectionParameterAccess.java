package org.hkijena.acaq5.api.parameters;

import org.hkijena.acaq5.api.ACAQDocumentation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ACAQReflectionParameterAccess implements ACAQParameterAccess {

    private String key;
    private Method getter;
    private Method setter;
    private ACAQDocumentation documentation;
    private Object parameterHolder;
    private String holderName;
    private String holderDescription;
    private ACAQParameterVisibility visibility = ACAQParameterVisibility.TransitiveVisible;

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getName() {
        if (getDocumentation() != null)
            return getDocumentation().name();
        return key;
    }

    @Override
    public String getDescription() {
        if (getDocumentation() != null)
            return getDocumentation().description();
        return null;
    }

    public ACAQDocumentation getDocumentation() {
        return documentation;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        T getterAnnotation = getter.getAnnotation(klass);
        if (getterAnnotation != null)
            return getterAnnotation;
        return setter.getAnnotation(klass);
    }

    @Override
    public Class<?> getFieldClass() {
        return getter.getReturnType();
    }

    @Override
    public <T> T get() {
        try {
            return (T) getter.invoke(parameterHolder);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> boolean set(T value) {
        try {
            Object result = setter.invoke(parameterHolder, value);
            if (result instanceof Boolean) {
                return (boolean) result;
            } else {
                return true;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getParameterHolder() {
        return parameterHolder;
    }

    @Override
    public ACAQParameterVisibility getVisibility() {
        return visibility;
    }

    @Override
    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    @Override
    public String getHolderDescription() {
        return holderDescription;
    }

    public void setHolderDescription(String holderDescription) {
        this.holderDescription = holderDescription;
    }

    /**
     * Extracts parameters from an object
     *
     * @param parameterHolder
     * @return
     */
    public static Map<String, ACAQParameterAccess> getReflectionParameters(Object parameterHolder) {
        Map<String, ACAQParameterAccess> result = new HashMap<>();
        for (Method method : parameterHolder.getClass().getMethods()) {
            ACAQParameter[] parameterAnnotations = method.getAnnotationsByType(ACAQParameter.class);
            if (parameterAnnotations.length > 0) {
                ACAQParameter parameterAnnotation = parameterAnnotations[0];
                ACAQReflectionParameterAccess access = (ACAQReflectionParameterAccess) result.putIfAbsent(parameterAnnotation.value(), new ACAQReflectionParameterAccess());
                if (access == null)
                    access = (ACAQReflectionParameterAccess) result.get(parameterAnnotation.value());
                access.parameterHolder = parameterHolder;
                access.visibility = access.visibility.intersectWith(parameterAnnotation.visibility());
                if (method.getParameters().length == 1) {
                    // Is a setter
                    access.setter = method;
                    access.key = parameterAnnotation.value();

                    ACAQDocumentation[] documentations = method.getAnnotationsByType(ACAQDocumentation.class);
                    if (documentations.length > 0)
                        access.documentation = documentations[0];
                } else {
                    // Is a getter
                    access.getter = method;
                    access.key = parameterAnnotation.value();

                    ACAQDocumentation[] documentations = method.getAnnotationsByType(ACAQDocumentation.class);
                    if (documentations.length > 0)
                        access.documentation = documentations[0];
                }
            }

            ACAQSubParameters[] subAlgorithms = method.getAnnotationsByType(ACAQSubParameters.class);
            if (subAlgorithms.length > 0) {
                try {
                    ACAQSubParameters subAlgorithmAnnotation = subAlgorithms[0];
                    Object subAlgorithm = method.invoke(parameterHolder);
                    String subAlgorithmName = null;
                    String subAlgorithmDescription = null;

                    ACAQDocumentation[] documentations = method.getAnnotationsByType(ACAQDocumentation.class);
                    if (documentations.length > 0) {
                        subAlgorithmName = documentations[0].name();
                        subAlgorithmDescription = documentations[0].description();
                    }

                    for (Map.Entry<String, ACAQParameterAccess> kv : ACAQParameterAccess.getParameters(subAlgorithm).entrySet()) {
                        ACAQParameterAccess subParameter = kv.getValue();
                        if (subParameter.getParameterHolder() == subAlgorithm) {
                            if (subParameter instanceof ACAQMutableParameterAccess) {
                                ((ACAQMutableParameterAccess) subParameter).setHolderName(subAlgorithmName);
                                ((ACAQMutableParameterAccess) subParameter).setHolderDescription(subAlgorithmDescription);
                            } else if (subParameter instanceof ACAQReflectionParameterAccess) {
                                ((ACAQReflectionParameterAccess) subParameter).setHolderName(subAlgorithmName);
                                ((ACAQReflectionParameterAccess) subParameter).setHolderDescription(subAlgorithmDescription);
                            }
                        }
                        result.put(subAlgorithmAnnotation.value() + "/" + kv.getKey(), subParameter);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }
}
