package org.hkijena.acaq5.extensions.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.function.Function;

/**
 * A parameter that is intended to set or scale an integer
 */
public class IntModificationParameter implements Function<Integer, Integer> {

    private int exactValue = 0;
    private double factor = 1;
    private boolean useExactValue = false;

    /**
     * Creates a new instance with default values
     */
    public IntModificationParameter() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntModificationParameter(IntModificationParameter other) {
        this.exactValue = other.exactValue;
        this.factor = other.factor;
        this.useExactValue = other.useExactValue;
    }

    @Override
    public Integer apply(Integer integer) {
        if (useExactValue)
            return exactValue;
        else
            return (int) (integer * factor);
    }

    @JsonGetter("exact-value")
    public int getExactValue() {
        return exactValue;
    }

    @JsonSetter("exact-value")
    public void setExactValue(int exactValue) {
        this.exactValue = exactValue;
    }

    @JsonGetter("factor")
    public double getFactor() {
        return factor;
    }

    @JsonSetter("factor")
    public void setFactor(double factor) {
        this.factor = factor;
    }

    @JsonGetter("use-exact-value")
    public boolean isUseExactValue() {
        return useExactValue;
    }

    @JsonSetter("use-exact-value")
    public void setUseExactValue(boolean useExactValue) {
        this.useExactValue = useExactValue;
    }
}
