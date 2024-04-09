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

package org.hkijena.jipipe.plugins.plots;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.plots.datatypes.PlotData;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCacheDataViewerWindow;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCachedDataViewerAnnotationInfoPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.app.ploteditor.JIPipeDesktopPlotEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CachedPlotViewerWindow extends JIPipeDesktopCacheDataViewerWindow {

    private JIPipeDesktopPlotEditorUI plotEditor;
    private JLabel errorLabel;
    private JIPipeDesktopCachedDataViewerAnnotationInfoPanel annotationInfoPanel;

    public CachedPlotViewerWindow(JIPipeDesktopWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName, boolean deferLoading) {
        super(workbench, dataSource, displayName);
        initialize();
        if (!deferLoading)
            reloadDisplayedData();
    }

    private void initialize() {
        plotEditor = new JIPipeDesktopPlotEditorUI(getWorkbench());
        errorLabel = new JLabel(UIUtils.getIconFromResources("emblems/no-data.png"));
        plotEditor.getToolBar().add(errorLabel, 0);

        annotationInfoPanel = new JIPipeDesktopCachedDataViewerAnnotationInfoPanel(getWorkbench());
        plotEditor.getSideBar().addTab("Annotations",
                UIUtils.getIconFromResources("data-types/annotation.png"),
                annotationInfoPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        setContentPane(plotEditor);
    }

    @Override
    public JToolBar getToolBar() {
        if (plotEditor == null)
            return null;
        else
            return plotEditor.getToolBar();
    }

    @Override
    protected void beforeSetRow() {

    }

    @Override
    protected void afterSetRow() {

    }

    @Override
    protected void hideErrorUI() {
        errorLabel.setVisible(false);
    }

    @Override
    protected void showErrorUI() {
        if (getAlgorithm() != null) {
            errorLabel.setText(String.format("No data available in node '%s', slot '%s', row %d", getAlgorithm().getName(), getSlotName(), getDataSource().getRow()));
        } else {
            errorLabel.setText("No data available");
        }
        errorLabel.setVisible(true);
        getToolBar().revalidate();
        getToolBar().repaint();
    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        annotationInfoPanel.displayAnnotations(getDataSource());
        PlotData data = (PlotData) virtualData.getData(progressInfo);
        PlotData duplicate = (PlotData) data.duplicate(progressInfo);
        plotEditor.importExistingPlot(duplicate);
    }
}
