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

package org.hkijena.jipipe.extensions.tables.display;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeCacheSlotDataSource;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CachedTableViewerWindow extends JIPipeCacheDataViewerWindow {

    private TableEditor tableEditor;
    private JLabel errorLabel;

    public CachedTableViewerWindow(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
        initialize();
        reloadDisplayedData();
    }

    private void initialize() {
        tableEditor = new TableEditor(getWorkbench(), new ResultsTableData());
        errorLabel = new JLabel(UIUtils.getIconFromResources("emblems/no-data.png"));
        tableEditor.getToolBar().add(errorLabel, 0);
        setContentPane(tableEditor);
    }

    @Override
    public JToolBar getToolBar() {
        if(tableEditor == null)
            return null;
        return tableEditor.getToolBar();
    }

    @Override
    protected void beforeSetRow() {

    }

    @Override
    protected void afterSetRow() {

    }

    @Override
    protected void removeErrorUI() {
        errorLabel.setVisible(false);
    }

    @Override
    protected void addErrorUI() {
        if(getAlgorithm() != null) {
            errorLabel.setText(String.format("No data available in node '%s', slot '%s', row %d", getAlgorithm().getName(), getSlotName(), getDataSource().getRow()));
        }
        else {
            errorLabel.setText("No data available");
        }
        errorLabel.setVisible(true);
        getToolBar().revalidate();
        getToolBar().repaint();
    }

    @Override
    protected void loadData(JIPipeVirtualData virtualData, JIPipeProgressInfo progressInfo) {
        ResultsTableData data = (ResultsTableData) virtualData.getData(progressInfo);
        ResultsTableData duplicate = (ResultsTableData) data.duplicate();
        tableEditor.setTableModel(duplicate);
    }
}