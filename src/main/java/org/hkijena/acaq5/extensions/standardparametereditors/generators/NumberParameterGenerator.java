package org.hkijena.acaq5.extensions.standardparametereditors.generators;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.ui.parameters.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.parameters.ACAQParameterGeneratorUI;
import org.scijava.Context;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generator that creates instances of {@link Number}.
 * This cannot be used directly in {@link org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry}, as the constructor does
 * not match. You have to inherit from this type and define the number type.
 */
public class NumberParameterGenerator<T extends Number & Comparable<T>> extends ACAQParameterGeneratorUI implements ACAQCustomParameterHolder {

    private EventBus eventBus = new EventBus();
    private Class<? extends Number> numberClass;
    private ACAQDynamicParameterHolder parameters = new ACAQDynamicParameterHolder();

    /**
     * Creates a new instance
     *
     * @param context     the SciJava context
     * @param numberClass the Number class that is created
     */
    public NumberParameterGenerator(Context context, Class<? extends Number> numberClass) {
        super(context);
        this.numberClass = numberClass;
        initializeParameters();
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(new ACAQParameterAccessUI(getContext(),
                this,
                null,
                false,
                false,
                true));
    }

    private void initializeParameters() {
        ACAQMutableParameterAccess minParameter = parameters.addParameter("min", numberClass);
        minParameter.setName("Minimum value");
        ACAQMutableParameterAccess maxParameter = parameters.addParameter("max", numberClass);
        maxParameter.setName("Maximum value");
        ACAQMutableParameterAccess stepSizeParameter = parameters.addParameter("step-size", numberClass);
        stepSizeParameter.setName("Step size");
        parameters.setAllowUserModification(false);
    }

    private T getCurrentMin() {
        Object result = parameters.get("min").get();
        if (result == null) {
            result = getZero();
        }
        return (T) result;
    }

    private T getCurrentMax() {
        Object result = parameters.get("max").get();
        if (result == null) {
            result = getZero();
        }
        return (T) result;
    }

    private T getCurrentStepSize() {
        Object result = parameters.get("step-size").get();
        if (result == null) {
            result = getZero();
        }
        return (T) result;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (getCurrentMin().compareTo(getCurrentMax()) > 0) {
            report.reportIsInvalid("Invalid minimum and maximum values! Please ensure that the minimum value is less or equal to the maximum value.");
        }
        if (isZero(getCurrentStepSize()) || isNegative(getCurrentStepSize())) {
            report.reportIsInvalid("Invalid step size! Please ensure that the step size is greater than zero.");
        }
    }

    private boolean isNegative(Number number) {
        if (number.getClass() == Byte.class) {
            return number.byteValue() < 0;
        } else if (number.getClass() == Short.class) {
            return number.shortValue() < 0;
        } else if (number.getClass() == Integer.class) {
            return number.intValue() < 0;
        } else if (number.getClass() == Long.class) {
            return number.longValue() < 0;
        } else if (number.getClass() == Float.class) {
            return number.floatValue() < 0;
        } else if (number.getClass() == Double.class) {
            return number.doubleValue() < 0;
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + number.getClass());
        }
    }

    private boolean isZero(Number number) {
        if (number.getClass() == Byte.class) {
            return number.byteValue() == 0;
        } else if (number.getClass() == Short.class) {
            return number.shortValue() == 0;
        } else if (number.getClass() == Integer.class) {
            return number.intValue() == 0;
        } else if (number.getClass() == Long.class) {
            return number.longValue() == 0;
        } else if (number.getClass() == Float.class) {
            return number.floatValue() == 0;
        } else if (number.getClass() == Double.class) {
            return number.doubleValue() == 0;
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + number.getClass());
        }
    }

    private Number getZero() {
        if (numberClass == byte.class || numberClass == Byte.class) {
            return (byte) 0;
        } else if (numberClass == short.class || numberClass == Short.class) {
            return (short) 0;
        } else if (numberClass == int.class || numberClass == Integer.class) {
            return 0;
        } else if (numberClass == long.class || numberClass == Long.class) {
            return 0L;
        } else if (numberClass == float.class || numberClass == Float.class) {
            return 0f;
        } else if (numberClass == double.class || numberClass == Double.class) {
            return 0d;
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + numberClass);
        }
    }

    private Number getIncremented(Number current) {
        if (numberClass == byte.class || numberClass == Byte.class) {
            return current.byteValue() + getCurrentStepSize().byteValue();
        } else if (numberClass == short.class || numberClass == Short.class) {
            return current.shortValue() + getCurrentStepSize().shortValue();
        } else if (numberClass == int.class || numberClass == Integer.class) {
            return current.intValue() + getCurrentStepSize().intValue();
        } else if (numberClass == long.class || numberClass == Long.class) {
            return current.longValue() + getCurrentStepSize().longValue();
        } else if (numberClass == float.class || numberClass == Float.class) {
            return current.floatValue() + getCurrentStepSize().floatValue();
        } else if (numberClass == double.class || numberClass == Double.class) {
            return current.doubleValue() + getCurrentStepSize().doubleValue();
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + numberClass);
        }
    }

    @Override
    public List<Object> get() {
        List<Object> result = new ArrayList<>();
        Number current = getCurrentMin();
        while (((T) current).compareTo(getCurrentMax()) <= 0) {
            result.add(current);
            current = getIncremented(current);
        }
        return result;
    }

    @Override
    public Map<String, ACAQParameterAccess> getCustomParameters() {
        return ACAQParameterHolder.getParameters(parameters);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
