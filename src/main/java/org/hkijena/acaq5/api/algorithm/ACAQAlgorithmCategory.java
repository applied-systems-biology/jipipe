package org.hkijena.acaq5.api.algorithm;

import java.awt.*;

/**
 * Available algorithm categories
 */
public enum ACAQAlgorithmCategory {
    /**
     * Algorithms that convert one data type to another
     */
    Converter,
    /**
     * Algorithms that process one data type and produce the same
     */
    Processor,
    /**
     * Algorithms that produce tabular, relational, or any quantification result data
     */
    Analysis,
    /**
     * Algorithms that read data from filesystem
     */
    DataSource,
    /**
     * Operations on files
     */
    FileSystem,
    /**
     * Algorithms associated to annotating data
     */
    Annotation,
    /**
     * Any other algorithm
     */
    Miscellaneous,
    /**
     * Internal algorithms are only used internally within a project
     */
    Internal;

    /**
     * Converts the category into a color
     *
     * @param s HSV Saturation
     * @param v HSV Color value
     * @return Color instance
     */
    public Color getColor(float s, float v) {
        ACAQAlgorithmCategory[] values = ACAQAlgorithmCategory.values();
        for (int i = 0; i < values.length; ++i) {
            if (values[i] == this) {
                return Color.getHSBColor(i * 1.0f / values.length, s, v);
            }
        }
        throw new IllegalArgumentException("Unknown category");
    }
}
