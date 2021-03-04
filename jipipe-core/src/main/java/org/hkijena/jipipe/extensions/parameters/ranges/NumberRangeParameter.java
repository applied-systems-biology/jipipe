package org.hkijena.jipipe.extensions.parameters.ranges;

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

    public Number getMaxNumber() {
        return maxNumber;
    }

    public void setMinNumber(Number minNumber) {
        this.minNumber = minNumber;
    }

    public void setMaxNumber(Number maxNumber) {
        this.maxNumber = maxNumber;
    }
}
