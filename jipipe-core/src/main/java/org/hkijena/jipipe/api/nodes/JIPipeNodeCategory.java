/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.nodes;

import java.awt.*;

/**
 * Available algorithm categories
 */
public enum JIPipeNodeCategory {
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
        JIPipeNodeCategory[] values = JIPipeNodeCategory.values();
        for (int i = 0; i < values.length; ++i) {
            if (values[i] == this) {
                return Color.getHSBColor(i * 1.0f / values.length, s, v);
            }
        }
        throw new IllegalArgumentException("Unknown category");
    }
}
