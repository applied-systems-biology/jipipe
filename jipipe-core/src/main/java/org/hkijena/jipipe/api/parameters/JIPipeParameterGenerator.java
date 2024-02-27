/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.awt.*;
import java.util.List;

/**
 * A class that generates parameters
 */
public interface JIPipeParameterGenerator {
    /**
     * @return name displayed in the menu
     */
    String getName();

    /**
     * @return description displayed in the menu
     */
    String getDescription();

    /**
     * Generates the values
     *
     * @param <T>       the generated field type
     * @param workbench the workbench
     * @param parent    the parent component for any dialogs
     * @param klass     the generated field class
     * @return the generated values
     */
    <T> List<T> generate(JIPipeWorkbench workbench, Component parent, Class<T> klass);
}
