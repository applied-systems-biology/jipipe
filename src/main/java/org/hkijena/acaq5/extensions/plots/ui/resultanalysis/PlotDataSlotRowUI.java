package org.hkijena.acaq5.extensions.plots.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotData;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.plotbuilder.ACAQPlotBuilderUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.UIUtils;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Result UI for {@link org.hkijena.acaq5.extensions.plots.datatypes.PlotData}
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
