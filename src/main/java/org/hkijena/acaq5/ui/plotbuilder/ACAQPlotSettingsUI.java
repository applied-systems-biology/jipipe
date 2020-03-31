/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.plotbuilder;

import javax.swing.*;

/**
 * UI for a plot
 */
public class ACAQPlotSettingsUI extends JPanel {
    private ACAQPlot plot;

    /**
     * @param plot The plot
     */
    public ACAQPlotSettingsUI(ACAQPlot plot) {
        this.plot = plot;
    }

    /**
     * @return The plot
     */
    public ACAQPlot getPlot() {
        return plot;
    }
}
