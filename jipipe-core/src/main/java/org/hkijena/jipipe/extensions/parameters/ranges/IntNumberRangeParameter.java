package org.hkijena.jipipe.extensions.parameters.ranges;

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
    public void setMinNumber(Number minNumber) {
        super.setMinNumber(minNumber.intValue());
    }

    @Override
    public void setMaxNumber(Number maxNumber) {
        super.setMaxNumber(maxNumber.intValue());
    }

    @Override
    public Number getMinNumber() {
        return super.getMinNumber().intValue();
    }

    @Override
    public Number getMaxNumber() {
        return super.getMaxNumber().intValue();
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
