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

package org.hkijena.jipipe.extensions.multiparameters.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCacheDataViewerWindow;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class CachedParametersDataViewerWindow extends JIPipeDesktopCacheDataViewerWindow {

    private final JToolBar toolBar = new JToolBar();
    private final ParametersDataViewer viewer;
    private JLabel errorLabel;

    public CachedParametersDataViewerWindow(JIPipeDesktopWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
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
