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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

public class TableColumnDataViewer extends JIPipeDesktopDataViewer {
    private final JIPipeDesktopTableEditor tableEditor;

    public TableColumnDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        this.tableEditor = new JIPipeDesktopTableEditor(dataViewerWindow.getDesktopWorkbench(), new ResultsTableData());
    }

    @Override
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {
        tableEditor.setRibbon(ribbon);
    }

    @Override
    public void rebuildDock(JIPipeDesktopDockPanel dockPanel) {
        dockPanel.setBackgroundComponent(tableEditor);
    }

    @Override
    public void postOnDataChanged() {
        getDataViewerWindow().startDownloadFullData();
    }

    @Override
    public void onDataDownloaded(JIPipeData data) {
        if(data instanceof TableColumnData) {
            String label = StringUtils.orElse(((TableColumnData) data).getLabel(), "Unnamed");
            ResultsTableData resultsTableData = new ResultsTableData();
            resultsTableData.addColumn(label, (TableColumnData) data, true);
            tableEditor.setTableModel(resultsTableData);
        }
        else {
            tableEditor.setTableModel(new ResultsTableData());
        }
    }
}
