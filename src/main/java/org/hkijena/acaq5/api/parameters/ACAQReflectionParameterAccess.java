package org.hkijena.acaq5.api.parameters;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.scijava.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * {@link ACAQParameterAccess} generated from reflection
 */
public class ACAQReflectionParameterAccess implements ACAQParameterAccess {

    private String key;
    private Method getter;
    private Method setter;
    private double priority;
    private ACAQDocumentation documentation;
    private ACAQParameterVisibility visibility = ACAQParameterVisibility.TransitiveVisible;
    private ACAQParameterCollection source;
    private String shortKey;
    private int uiOrder;

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    /**
     * @return Documentation of this parameter
     */
    public ACAQDocumentation getDocumentation() {
        return documentation;
    }

    public void setDocumentation(ACAQDocumentation documentation) {
        this.documentation = documentation;
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
            return (T) getter.invoke(source);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UserFriendlyRuntimeException(e, "Unable to get parameter data!",
                    "Parameter " + getName(), "There is an error in the algorithm's code.",
                    "Please contact the authors of the algorithm.");
        }
    }

    @Override
    public <T> boolean set(T value) {
        try {
            Object result = setter.invoke(source, value);
            if (result instanceof Boolean) {
                return (boolean) result;
            } else {
                return true;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UserFriendlyRuntimeException(e, "Unable to set parameter data!",
                    "Parameter " + getName(), "There is an error in the algorithm's code.",
                    "Please contact the authors of the algorithm.");
        }
    }

    @Override
    public ACAQParameterCollection getSource() {
        return source;
    }

    public void setSource(ACAQParameterCollection source) {
        this.source = source;
    }

    @Override
    public ACAQParameterVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ACAQParameterVisibility visibility) {
        this.visibility = visibility;
    }

    @Override
    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    public void setSetter(Method setter) {
        this.setter = setter;
    }

    public void setGetter(Method getter) {
        this.getter = getter;
    }

    @Override
    public String getShortKey() {
        return !StringUtils.isNullOrEmpty(shortKey) ? shortKey : getKey();
    }

    public void setShortKey(String shortKey) {
        this.shortKey = shortKey;
    }

    @Override
    public int getUIOrder() {
        return uiOrder;
    }

    public void setUIOrder(int uiOrder) {
        this.uiOrder = uiOrder;
    }
}
