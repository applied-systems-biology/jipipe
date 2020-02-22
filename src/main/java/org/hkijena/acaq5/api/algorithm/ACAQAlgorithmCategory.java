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
     * Internal algorithms are only used internally within a project
     */
    Internal;

    public Color getColor(float s, float v) {
        float n = ACAQAlgorithmCategory.values().length;
        switch(this) {
            case Converter:
                return Color.getHSBColor(0, s, v);
            case Enhancer:
                return Color.getHSBColor(1 / n, s, v);
            case Segmenter:
                return Color.getHSBColor(2 / n, s, v);
            case Quantififer:
                return Color.getHSBColor(3 / n, s, v);
            case DataSource:
                return Color.getHSBColor(4 / n, s, v);
            case Internal:
                return Color.getHSBColor(5 / n, s, v);
            default:
                throw new RuntimeException("Unknown category " + this);
        }
    }
}
