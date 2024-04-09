/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.parameters.library.ranges;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class IntNumberRangeParameter extends NumberRangeParameter {

    public IntNumberRangeParameter() {
        super(0.0f, 1.0f);
    }

    public IntNumberRangeParameter(IntNumberRangeParameter other) {
        super(other);
    }

    public IntNumberRangeParameter(float min, float max) {
        super(min, max);
    }

    @Override
    public Number getMinNumber() {
        return super.getMinNumber().intValue();
    }

    @Override
    public void setMinNumber(Number minNumber) {
        super.setMinNumber(minNumber.intValue());
    }

    @Override
    public Number getMaxNumber() {
        return super.getMaxNumber().intValue();
    }

    @Override
    public void setMaxNumber(Number maxNumber) {
        super.setMaxNumber(maxNumber.intValue());
    }

    @JsonGetter("min")
    public int getMin() {
        return getMinNumber().intValue();
    }

    @JsonSetter("min")
    public void setMin(int min) {
        setMinNumber(min);
    }

    @JsonGetter("max")
    public int getMax() {
        return getMaxNumber().intValue();
    }

    @JsonSetter("max")
    public void setMax(int max) {
        setMaxNumber(max);
    }
}
