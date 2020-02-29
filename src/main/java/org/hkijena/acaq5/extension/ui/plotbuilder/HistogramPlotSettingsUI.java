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

package org.hkijena.acaq5.extension.ui.plotbuilder;


import org.hkijena.acaq5.ui.plotbuilder.ACAQPlot;
import org.hkijena.acaq5.ui.plotbuilder.DefaultACAQPlotSettingsUI;

public class HistogramPlotSettingsUI extends DefaultACAQPlotSettingsUI {

    public HistogramPlotSettingsUI(ACAQPlot plot) {
        super(plot);
        initialize();
    }

    private void initialize() {
        addStringEditorComponent("X axis label", () -> getNativePlot().getxAxisLabel(), s -> getNativePlot().setxAxisLabel(s));
        addStringEditorComponent("Y axis label", () -> getNativePlot().getyAxisLabel(), s -> getNativePlot().setyAxisLabel(s));
    }

    private HistogramPlot getNativePlot() {
        return (HistogramPlot) getPlot();
    }
}
