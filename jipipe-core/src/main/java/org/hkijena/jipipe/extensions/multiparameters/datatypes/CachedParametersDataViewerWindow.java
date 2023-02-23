package org.hkijena.jipipe.extensions.multiparameters.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class CachedParametersDataViewerWindow extends JIPipeCacheDataViewerWindow {

    private final JToolBar toolBar = new JToolBar();
    private final ParametersDataViewer viewer;
    private JLabel errorLabel;

    public CachedParametersDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
        this.viewer = new ParametersDataViewer(workbench);
        initialize();
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout());

        toolBar.setFloatable(false);
        getContentPane().add(toolBar, BorderLayout.NORTH);

        // Error label
        errorLabel = new JLabel(UIUtils.getIconFromResources("emblems/no-data.png"));
        toolBar.add(errorLabel);
        toolBar.add(Box.createHorizontalGlue());

        // Show
        getContentPane().add(viewer, BorderLayout.CENTER);
    }

    @Override
    public JToolBar getToolBar() {
        return toolBar;
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
        ParametersData data = virtualData.getData(ParametersData.class, progressInfo);
        viewer.setParametersData(data);
    }
}
