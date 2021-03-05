package org.hkijena.jipipe.extensions.parameters.ranges;

public enum NumberRangeInvertedMode {
    /**
     * Switch min and max, so they are in the correct order
     */
    SwitchMinMax,
    /**
     * Exclude everything inside min/max
     */
    OutsideMinMax
}
