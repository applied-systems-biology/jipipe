package org.hkijena.acaq5.api.parameters;

import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A custom parameter access
 */
public class ACAQManualParameterAccess implements ACAQParameterAccess {

    private String key;
    private String name;
    private String description;
    private ACAQParameterVisibility visibility = ACAQParameterVisibility.TransitiveVisible;
    private Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();
    private Class<?> fieldClass;
    private Supplier<Object> getter;
    private Function<Object, Boolean> setter;
    private ACAQParameterCollection source;
    private double priority;
    private String shortKey;
    private int uiOrder;

    private ACAQManualParameterAccess() {

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
    public ACAQParameterVisibility getVisibility() {
        return visibility;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return (T) annotations.getOrDefault(klass, null);
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
    public ACAQParameterCollection getSource() {
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

    /**
     * Creates a new builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link ACAQManualParameterAccess}
     */
    public static class Builder {
        private final ACAQManualParameterAccess access = new ACAQManualParameterAccess();

        /**
         * Sets the parameter source
         *
         * @param source the source
         * @return this
         */
        public Builder setSource(ACAQParameterCollection source) {
            access.source = source;
            return this;
        }

        /**
         * Sets the priority
         *
         * @param priority
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
         * @param visibility the visibility
         * @return this
         */
        public Builder setVisibility(ACAQParameterVisibility visibility) {
            access.visibility = visibility;
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
         * Sets up access via reflection
         * The field class is extracted from the getter return type
         *
         * @param source the source object
         * @param getter the getter method
         * @param setter the setter method
         * @return this
         */
        public Builder reflectionAccess(ACAQParameterCollection source, Method getter, Method setter) {
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
            return setFieldClass(getter.getReturnType()).setGetter(getterSupplier).setSetter(setterConsumer);
        }

        /**
         * Sets up a dummy access.
         * The source is set to an {@link ACAQDummyParameterCollection} instance.
         *
         * @param fieldClass the field class to store
         * @return this
         */
        public Builder dummyAccess(Class<?> fieldClass) {
            try {
                ACAQDummyParameterCollection collection = new ACAQDummyParameterCollection();
                Method getter = ACAQDummyParameterCollection.class.getDeclaredMethod("get");
                Method setter = ACAQDummyParameterCollection.class.getDeclaredMethod("accept", Object.class);
                return setFieldClass(fieldClass).reflectionAccess(collection, getter, setter);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Returns the finished object
         *
         * @return the finished object
         */
        public ACAQManualParameterAccess build() {
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
