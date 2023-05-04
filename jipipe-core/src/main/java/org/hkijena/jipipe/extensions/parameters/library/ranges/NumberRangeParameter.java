package org.hkijena.jipipe.extensions.parameters.library.ranges;

import java.util.Objects;

public class NumberRangeParameter {
    private Number minNumber;
    private Number maxNumber;

    public NumberRangeParameter(Number minNumber, Number maxNumber) {
        this.minNumber = minNumber;
        this.maxNumber = maxNumber;
    }

    public NumberRangeParameter(NumberRangeParameter other) {
        this.minNumber = other.minNumber;
        this.maxNumber = other.maxNumber;
    }

    public Number getMinNumber() {
        return minNumber;
    }

    public void setMinNumber(Number minNumber) {
        this.minNumber = minNumber;
    }

    public Number getMaxNumber() {
        return maxNumber;
    }

    public void setMaxNumber(Number maxNumber) {
        this.maxNumber = maxNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NumberRangeParameter that = (NumberRangeParameter) o;
        return Objects.equals(minNumber, that.minNumber) && Objects.equals(maxNumber, that.maxNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minNumber, maxNumber);
    }
}
