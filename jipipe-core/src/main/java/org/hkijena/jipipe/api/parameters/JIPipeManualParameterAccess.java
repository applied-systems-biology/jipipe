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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A custom parameter access
 */
public class JIPipeManualParameterAccess implements JIPipeParameterAccess {

    private final Multimap<Class<? extends Annotation>, Annotation> annotations = HashMultimap.create();
    private String key;
    private String name;
    private String description;
    private boolean hidden;
    private Function<Class<? extends Annotation>, Annotation> annotationSupplier;
    private Class<?> fieldClass;
    private Supplier<Object> getter;
    private Function<Object, Boolean> setter;
    private JIPipeParameterCollection source;
    private double priority;
    private String shortKey;
    private int uiOrder;
    private boolean important;

    private JIPipeManualParameterAccess() {

    }

    /**
     * Creates a new builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        Collection<Annotation> available = annotations.get(klass);
        Annotation result = available.isEmpty() ? null : (Annotation) available.iterator().next();
        if (result == null && annotationSupplier != null)
            result = annotationSupplier.apply(klass);
        if (result != null)
            return (T) result;
        else
            return null;
    }

    @Override
    public <T extends Annotation> List<T> getAnnotationsOfType(Class<T> klass) {
        return annotations.get(klass).stream().map(ann -> (T)ann).collect(Collectors.toList());
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        return annotations.values();
    }

    public Function<Class<? extends Annotation>, Annotation> getAnnotationSupplier() {
        return annotationSupplier;
    }

    public void setAnnotationSupplier(Function<Class<? extends Annotation>, Annotation> annotationSupplier) {
        this.annotationSupplier = annotationSupplier;
    }

    @Override
    public Class<?> getFieldClass() {
        return fieldClass;
    }

    @Override
    public <T> T get(Class<T> klass) {
        return (T) getter.get();
    }

    @Override
    public <T> boolean set(T value) {
        return setter.apply(value);
    }

    @Override
    public JIPipeParameterCollection getSource() {
        return source;
    }

    @Override
    public double getPriority() {
        return priority;
    }

    @Override
    public String getShortKey() {
        return !StringUtils.isNullOrEmpty(shortKey) ? shortKey : getKey();
    }

    @Override
    public int getUIOrder() {
        return uiOrder;
    }

    @Override
    public boolean isImportant() {
        return important;
    }

    /**
     * A builder for {@link JIPipeManualParameterAccess}
     */
    public static class Builder {
        private final JIPipeManualParameterAccess access = new JIPipeManualParameterAccess();

        /**
         * Sets the parameter source
         *
         * @param source the source
         * @return this
         */
        public Builder setSource(JIPipeParameterCollection source) {
            access.source = source;
            return this;
        }

        /**
         * Sets the priority
         *
         * @param priority the priority
         * @return this
         */
        public Builder setPriority(double priority) {
            access.priority = priority;
            return this;
        }

        /**
         * Sets the UI order
         *
         * @param uiOrder the ui order
         * @return this
         */
        public Builder setUIOrder(int uiOrder) {
            access.uiOrder = uiOrder;
            return this;
        }

        /**
         * Sets the field class
         *
         * @param fieldClass the field class
         * @return this
         */
        public Builder setFieldClass(Class<?> fieldClass) {
            access.fieldClass = fieldClass;
            return this;
        }

        /**
         * Sets the name
         *
         * @param name the name
         * @return this
         */
        public Builder setName(String name) {
            access.name = name;
            return this;
        }

        /**
         * Sets the description
         *
         * @param description the description
         * @return this
         */
        public Builder setDescription(String description) {
            access.description = description;
            return this;
        }

        /**
         * Sets the short key
         *
         * @param shortKey the sort key
         * @return this
         */
        public Builder setShortKey(String shortKey) {
            access.shortKey = shortKey;
            return this;
        }

        /**
         * Sets the visibility
         *
         * @param hidden the visibility
         * @return this
         */
        public Builder setHidden(boolean hidden) {
            access.hidden = hidden;
            return this;
        }

        /**
         * Allows to make the parameter important (only for UI)
         *
         * @param important if the parameter should be highlighted in the UI
         * @return this
         */
        public Builder setImportant(boolean important) {
            access.important = important;
            return this;
        }

        /**
         * Sets the getter
         *
         * @param getter the getter
         * @return this
         */
        public Builder setGetter(Supplier<Object> getter) {
            access.getter = getter;
            return this;
        }

        /**
         * Sets the setter
         *
         * @param setter the setter
         * @return this
         */
        public Builder setSetter(Function<Object, Boolean> setter) {
            access.setter = setter;
            return this;
        }

        /**
         * Sets the unique key
         *
         * @param key the key
         * @return this
         */
        public Builder setKey(String key) {
            access.key = key;
            return this;
        }

        /**
         * Sets the setter
         *
         * @param setter the setter
         * @return this
         */
        public <T> Builder setSetter(Consumer<T> setter) {
            access.setter = value -> {
                setter.accept((T) value);
                return true;
            };
            return this;
        }

        public Builder addAnnotation(Annotation annotation) {
            access.annotations.put(annotation.annotationType(), annotation);
            return this;
        }

        public Builder setAnnotationSupplier(Function<Class<? extends Annotation>, Annotation> supplier) {
            access.setAnnotationSupplier(supplier);
            return this;
        }

        /**
         * Sets up access via reflection
         * The field class is extracted from the getter return type
         *
         * @param source the source object
         * @param getter the getter method
         * @param setter the setter method
         * @return this
         */
        public Builder reflectionAccess(JIPipeParameterCollection source, Method getter, Method setter) {
            Supplier<Object> getterSupplier = () -> {
                try {
                    return getter.invoke(source);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new UserFriendlyRuntimeException(e, "Unable to get parameter data!",
                            "Parameter " + access.getName(), "There is an error in the algorithm's code.",
                            "Please contact the authors of the algorithm.");
                }
            };
            Function<Object, Boolean> setterConsumer = (value) -> {
                try {
                    Object result = setter.invoke(source, value);
                    if (result instanceof Boolean) {
                        return (boolean) result;
                    } else {
                        return true;
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new UserFriendlyRuntimeException(e, "Unable to set parameter data!",
                            "Parameter " + access.getName(), "There is an error in the algorithm's code.",
                            "Please contact the authors of the algorithm.");
                }
            };
            return setSource(source).setFieldClass(getter.getReturnType()).setGetter(getterSupplier).setSetter(setterConsumer);
        }

        /**
         * Sets up a dummy access.
         * The source is set to an {@link JIPipeDummyParameterCollection} instance.
         *
         * @param fieldClass the field class to store
         * @return this
         */
        public Builder dummyAccess(Class<?> fieldClass) {
            try {
                JIPipeDummyParameterCollection collection = new JIPipeDummyParameterCollection();
                Method getter = JIPipeDummyParameterCollection.class.getDeclaredMethod("get");
                Method setter = JIPipeDummyParameterCollection.class.getDeclaredMethod("accept", Object.class);
                return reflectionAccess(collection, getter, setter).setFieldClass(fieldClass).setKey("value");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Returns the finished object
         * Requires following properties to be set:
         * source, field class, getter, setter
         *
         * @return the finished object
         */
        public JIPipeManualParameterAccess build() {
            if (access.source == null)
                throw new IllegalArgumentException("The source is null!");
            if (access.fieldClass == null)
                throw new IllegalArgumentException("The field class is null!");
            if (access.getter == null)
                throw new IllegalArgumentException("The getter is null!");
            if (access.setter == null)
                throw new IllegalArgumentException("The setter is null!");
            return access;
        }
    }
}
