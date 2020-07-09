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

package org.hkijena.pipelinej.extensions.plots.ui.resultanalysis;

import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.api.data.ACAQExportedDataTable;
import org.hkijena.pipelinej.extensions.plots.datatypes.PlotData;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.components.DocumentTabPane;
import org.hkijena.pipelinej.ui.plotbuilder.ACAQPlotBuilderUI;
import org.hkijena.pipelinej.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.pipelinej.utils.PathUtils;
import org.hkijena.pipelinej.utils.UIUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Result UI for {@link org.hkijena.pipelinej.extensions.plots.datatypes.PlotData}
 */
public class PlotDataSlotRowUI extends ACAQDefaultResultDataSlotRowUI {
    /**
     * Creates a new UI
     *
     * @param workbenchUI The workbench UI
     * @param slot        The data slot
     * @param row         The data slow row
     */
    public PlotDataSlotRowUI(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
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
                    UIUtils.getIconFromResources("filetype-image.png"),
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
                    UIUtils.getIconFromResources("filetype-image.png"),
                    slot -> {
                        try {
                            Desktop.getDesktop().open(pngFile.toFile());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
        registerAction("Open in ACAQ5",
                "Opens the plot in ACAQ5",
                UIUtils.getIconFromResources("acaq5.png"),
                slot -> openPlot());
    }

    private void openPlot() {
        PlotData plotData = PlotData.fromFolder(getRowStorageFolder());
        ACAQPlotBuilderUI plotBuilderUI = new ACAQPlotBuilderUI(getWorkbench());
        plotBuilderUI.importExistingPlot(plotData);
        getWorkbench().getDocumentTabPane().addTab(getDisplayName(), UIUtils.getIconFromResources("data-types/data-type-plot.png"),
                plotBuilderUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }
}
