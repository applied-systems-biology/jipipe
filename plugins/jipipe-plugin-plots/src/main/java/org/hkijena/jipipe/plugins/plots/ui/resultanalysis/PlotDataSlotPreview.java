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

package org.hkijena.jipipe.plugins.plots.ui.resultanalysis;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataAnnotationInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopAsyncResultDataSlotPreview;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotData;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Renders a plot data as table cell
 */
public class PlotDataSlotPreview extends JIPipeDesktopAsyncResultDataSlotPreview {

    /**
     * Creates a new renderer
     *
     * @param workbench      the workbench
     * @param table          the table where the data is rendered in
     * @param slot           the data slot
     * @param row            the row
     * @param dataAnnotation optional data annotation
     */
    public PlotDataSlotPreview(JIPipeDesktopProjectWorkbench workbench, JTable table, JIPipeDataSlot slot, JIPipeDataTableRowInfo row, JIPipeDataAnnotationInfo dataAnnotation) {
        super(workbench, table, slot, row, dataAnnotation);
        initialize();
    }

    private void initialize() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    protected JIPipeData loadData(Path storageFolder) {
        return JFreeChartPlotData.importData(new JIPipeFileSystemReadDataStorage(new JIPipeProgressInfo(), storageFolder), new JIPipeProgressInfo());
    }
}
