package org.hkijena.jipipe.extensions.plots.ui.resultanalysis;

import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.plotbuilder.JIPipePlotBuilderUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

public class OpenPlotInJIPipeDataOperation implements JIPipeDataDisplayOperation, JIPipeDataImportOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        data.display(displayName, workbench, source);
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        PlotData plotData = PlotData.fromFolder(rowStorageFolder);
        JIPipePlotBuilderUI plotBuilderUI = new JIPipePlotBuilderUI(workbench);
        plotBuilderUI.importExistingPlot(plotData);
        workbench.getDocumentTabPane().addTab(displayName, UIUtils.getIconFromResources("data-types/data-type-plot.png"),
                plotBuilderUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        workbench.getDocumentTabPane().switchToLastTab();
        return plotData;
    }

    @Override
    public String getName() {
        return "Open in JIPipe";
    }

    @Override
    public String getDescription() {
        return "Opens the plot in JIPipe";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/jipipe.png");
    }
}
