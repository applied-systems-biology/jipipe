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
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.utils.DocumentationUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * {@link JIPipeParameterAccess} generated from reflection
 */
public class JIPipeReflectionParameterAccess implements JIPipeParameterAccess {

    private String key;
    private Method getter;
    private Method setter;
    private double priority;
    private JIPipeDocumentation documentation;
    private boolean hidden;
    private JIPipeParameterCollection source;
    private String shortKey;
    private int uiOrder;
    private boolean important;
    private JIPipeParameterPersistence persistence;

    private boolean pinned;

    @Override
    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

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
            return DocumentationUtils.getDocumentationDescription(getDocumentation(), getFieldClass());
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
        T annotation = getter.getAnnotation(klass);
        if (annotation != null)
            return annotation;
        annotation = setter.getAnnotation(klass);
        if (annotation != null)
            return annotation;
        return getFieldClass().getAnnotation(klass);
    }

    @Override
    public <T extends Annotation> List<T> getAnnotationsOfType(Class<T> klass) {
        List<T> result = new ArrayList<>();
        for (T t : getter.getAnnotationsByType(klass)) {
            result.add(t);
        }
        for (T t : setter.getAnnotationsByType(klass)) {
            result.add(t);
        }
        for (T t : getFieldClass().getAnnotationsByType(klass)) {
            result.add(t);
        }
        return result;
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        List<Annotation> annotationList = new ArrayList<>();
        if (getter != null) {
            annotationList.addAll(Arrays.asList(getter.getAnnotations()));
        }
        if (setter != null) {
            annotationList.addAll(Arrays.asList(setter.getAnnotations()));
        }
        return annotationList;
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
            throw new JIPipeValidationRuntimeException(e, "Unable to get parameter data!",
                    "Affected parameter " + getName() + "." + " There is an error in the code.",
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
                source.getParameterChangedEventEmitter().emit(new JIPipeParameterCollection.ParameterChangedEvent(source, key));

            if (result instanceof Boolean) {
                return (boolean) result;
            } else {
                return true;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new JIPipeValidationRuntimeException(e, "Unable to set parameter data!",
                    "Affected parameter " + getName() + "." + " There is an error in the code.",
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
    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
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

    @Override
    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }
}
