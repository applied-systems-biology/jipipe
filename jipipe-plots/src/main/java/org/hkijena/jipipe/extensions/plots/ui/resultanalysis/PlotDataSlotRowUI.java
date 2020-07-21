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

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.plotbuilder.JIPipePlotBuilderUI;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeDefaultResultDataSlotRowUI;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Result UI for {@link org.hkijena.jipipe.extensions.plots.datatypes.PlotData}
 */
public class PlotDataSlotRowUI extends JIPipeDefaultResultDataSlotRowUI {
    /**
     * Creates a new UI
     *
     * @param workbenchUI The workbench UI
     * @param slot        The data slot
     * @param row         The data slow row
     */
    public PlotDataSlotRowUI(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
        super(workbenchUI, slot, row);
    }

    private Path findFile(String extension) {
        if (getRowStorageFolder() != null && Files.isDirectory(getRowStorageFolder())) {
            return PathUtils.findFileByExtensionIn(getRowStorageFolder(), extension);
        }
        return null;
    }

    @Override
    protected void registerActions() {
        super.registerActions();

        Path svgFile = findFile(".svg");
        Path pngFile = findFile(".png");
        if (svgFile != null) {
            registerAction("Open *.svg",
                    "Opens the SVG-rendered plot",
                    UIUtils.getIconFromResources("actions/viewimage.png"),
                    slot -> {
                        try {
                            Desktop.getDesktop().open(svgFile.toFile());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
        if (pngFile != null) {
            registerAction("Open *.png",
                    "Opens the PNG-rendered plot",
                    UIUtils.getIconFromResources("actions/viewimage.png"),
                    slot -> {
                        try {
                            Desktop.getDesktop().open(pngFile.toFile());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
        registerAction("Open in JIPipe",
                "Opens the plot in JIPipe",
                UIUtils.getIconFromResources("apps/jipipe.png"),
                slot -> openPlot());
    }

    private void openPlot() {
        PlotData plotData = PlotData.fromFolder(getRowStorageFolder());
        JIPipePlotBuilderUI plotBuilderUI = new JIPipePlotBuilderUI(getWorkbench());
        plotBuilderUI.importExistingPlot(plotData);
        getWorkbench().getDocumentTabPane().addTab(getDisplayName(), UIUtils.getIconFromResources("data-types/data-type-plot.png"),
                plotBuilderUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }
}
