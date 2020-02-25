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
    Enhancer,
    /**
     * Algorithms that convert real-space data into binary-space data
     */
    Segmenter,
    /**
     * Algorithms that produce tabular, relational, or any quantification result data
     */
    Quantififer,
    /**
     * Algorithms that read data from filesystem
     */
    DataSource,
    /**
     * Operations on files
     */
    FileSystem,
    /**
     * Internal algorithms are only used internally within a project
     */
    Internal;

    public Color getColor(float s, float v) {
        ACAQAlgorithmCategory[] values = ACAQAlgorithmCategory.values();
        for(int i = 0; i < values.length; ++i) {
            if(values[i] == this) {
                return  Color.getHSBColor(i * 1.0f / values.length, s, v);
            }
        }
        throw new IllegalArgumentException("Unknown category");
    }
}
