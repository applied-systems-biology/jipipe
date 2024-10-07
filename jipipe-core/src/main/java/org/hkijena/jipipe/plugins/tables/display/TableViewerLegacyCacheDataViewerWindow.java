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

package org.hkijena.jipipe.plugins.tables.display;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopLegacyCacheDataViewerWindow;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCachedDataViewerAnnotationInfoPanel;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class TableViewerLegacyCacheDataViewerWindow extends JIPipeDesktopLegacyCacheDataViewerWindow {

    private JIPipeDesktopTableEditor tableEditor;
    private JLabel errorLabel;
    private JIPipeDesktopCachedDataViewerAnnotationInfoPanel annotationInfoPanel;

    public TableViewerLegacyCacheDataViewerWindow(JIPipeDesktopWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName, boolean deferLoading) {
        super(workbench, dataSource, displayName);
        initialize();
        if (!deferLoading)
            reloadDisplayedData();
    }

    private void initialize() {
        tableEditor = new JIPipeDesktopTableEditor(getWorkbench(), new ResultsTableData());
        errorLabel = new JLabel(UIUtils.getIconFromResources("emblems/no-data.png"));
//        tableEditor.getToolBar().add(errorLabel, 0);
//
//        annotationInfoPanel = new JIPipeDesktopCachedDataViewerAnnotationInfoPanel(getWorkbench());
//        tableEditor.getSideBar().addTab("Annotations",
//                UIUtils.getIconFromResources("data-types/annotation.png"),
//                annotationInfoPanel,
//                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        setContentPane(tableEditor);
    }

    @Override
    public JToolBar getToolBar() {
//        if (tableEditor == null)
//            return null;
//        return tableEditor.getToolBar();
        return null;
    }

    @Override
    public JToolBar getPinToolBar() {
//        if (tableEditor == null)
//            return null;
//        return tableEditor.getPinToolBar();
        return null;
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
            errorLabel.setText(String.format("No data available in node '%s', slot '%s', row %d",
                    getAlgorithm().getName(),
                    getSlotName(),
                    getDataSource().getRow()));
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
        ResultsTableData data = (ResultsTableData) virtualData.getData(progressInfo);
        ResultsTableData duplicate = (ResultsTableData) data.duplicate(progressInfo);
        tableEditor.setTableModel(duplicate);
    }

    @Override
    public void dispose() {
        super.dispose();
        tableEditor.setTableModel(new ResultsTableData());
    }
}
