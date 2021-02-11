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

package org.hkijena.jipipe.extensions.parameters.roi;

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

    public IntModificationParameter(int exactValue, double factor, boolean useExactValue) {
        this.exactValue = exactValue;
        this.factor = factor;
        this.useExactValue = useExactValue;
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

    public static IntModificationParameter exact(int value) {
        return new IntModificationParameter(value, 1, true);
    }

    public static IntModificationParameter relative(int value) {
        return new IntModificationParameter(0, value, false);
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
