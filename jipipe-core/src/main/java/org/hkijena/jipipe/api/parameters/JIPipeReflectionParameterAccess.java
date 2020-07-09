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

package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * {@link JIPipeParameterAccess} generated from reflection
 */
public class JIPipeReflectionParameterAccess implements JIPipeParameterAccess {

    private String key;
    private Method getter;
    private Method setter;
    private double priority;
    private JIPipeDocumentation documentation;
    private JIPipeParameterVisibility visibility = JIPipeParameterVisibility.TransitiveVisible;
    private JIPipeParameterCollection source;
    private String shortKey;
    private int uiOrder;
    private JIPipeParameterPersistence persistence;

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
    public JIPipeDocumentation getDocumentation() {
        return documentation;
    }

    public void setDocumentation(JIPipeDocumentation documentation) {
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
    public <T> T get(Class<T> klass) {
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
            Object existing = get(Object.class);
            if (existing != value && Objects.equals(existing, value)) {
                return true;
            }
            Object result = setter.invoke(source, value);

            // Trigger change in parent parameter holder
            if (source != null)
                source.getEventBus().post(new ParameterChangedEvent(source, key));

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
    public JIPipeParameterCollection getSource() {
        return source;
    }

    public void setSource(JIPipeParameterCollection source) {
        this.source = source;
    }

    @Override
    public JIPipeParameterVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(JIPipeParameterVisibility visibility) {
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

    @Override
    public JIPipeParameterPersistence getPersistence() {
        return persistence;
    }

    public void setPersistence(JIPipeParameterPersistence persistence) {
        this.persistence = persistence;
    }
}
