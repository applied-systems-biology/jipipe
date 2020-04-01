/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.extensions.standardplots.ui.plots;


import org.hkijena.acaq5.ui.plotbuilder.ACAQPlot;
import org.hkijena.acaq5.ui.plotbuilder.DefaultACAQPlotSettingsUI;

/**
 * UI for {@link XYPlot}
 */
public class XYPlotSettingsUI extends DefaultACAQPlotSettingsUI {

    /**
     * @param plot the plot
     */
    public XYPlotSettingsUI(ACAQPlot plot) {
        super(plot);
        initialize();
    }

    private void initialize() {
        addStringEditorComponent("X axis label", () -> getNativePlot().getxAxisLabel(), s -> getNativePlot().setxAxisLabel(s));
        addStringEditorComponent("Y axis label", () -> getNativePlot().getyAxisLabel(), s -> getNativePlot().setyAxisLabel(s));
    }

    private XYPlot getNativePlot() {
        return (XYPlot) getPlot();
    }
}
