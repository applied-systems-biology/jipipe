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

package org.hkijena.jipipe.extensions.plots.ui.resultanalysis;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeAsyncResultDataPlotPreviewUI;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Renders a plot data as table cell
 */
public class PlotDataSlotPreviewUI extends JIPipeAsyncResultDataPlotPreviewUI {

    /**
     * Creates a new renderer
     *
     * @param table the parent table
     */
    public PlotDataSlotPreviewUI(JTable table) {
        super(table);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    protected JIPipeData loadData(Path storageFolder) {
        return PlotData.fromFolder(storageFolder);
    }
}
