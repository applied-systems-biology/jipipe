package org.hkijena.jipipe.extensions.parameters.library.ranges;

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
