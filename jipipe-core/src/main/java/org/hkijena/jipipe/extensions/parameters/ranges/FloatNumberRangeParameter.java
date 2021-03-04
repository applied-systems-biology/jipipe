package org.hkijena.jipipe.extensions.parameters.ranges;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class FloatNumberRangeParameter extends NumberRangeParameter {

    public FloatNumberRangeParameter() {
        super(0.0f, 1.0f);
    }

    public FloatNumberRangeParameter(FloatNumberRangeParameter other) {
        super(other);
    }

    public FloatNumberRangeParameter(float min, float max) {
        super(min, max);
    }

    @Override
    public Number getMinNumber() {
        return super.getMinNumber().floatValue();
    }

    @Override
    public void setMinNumber(Number minNumber) {
        super.setMinNumber(minNumber.floatValue());
    }

    @Override
    public Number getMaxNumber() {
        return super.getMaxNumber().floatValue();
    }

    @Override
    public void setMaxNumber(Number maxNumber) {
        super.setMaxNumber(maxNumber.floatValue());
    }

    @JsonGetter("min")
    public float getMin() {
        return getMinNumber().floatValue();
    }

    @JsonSetter("min")
    public void setMin(float min) {
        setMinNumber(min);
    }

    @JsonGetter("max")
    public float getMax() {
        return getMaxNumber().floatValue();
    }

    @JsonSetter("max")
    public void setMax(float max) {
        setMaxNumber(max);
    }
}
