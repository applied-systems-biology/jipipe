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
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.display.trackscheme;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.ui.cache.JIPipeCachedDataViewerAnnotationInfoPanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class TrackSchemeCachedDataViewerWindow extends JIPipeCacheDataViewerWindow {

    private JToolBar toolBar;
    private AutoResizeSplitPane splitPane;
    private JIPipeCachedDataViewerAnnotationInfoPanel annotationInfoPanel;

    private DisplaySettings displaySettings = new DisplaySettings();

    public TrackSchemeCachedDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
        initialize();
        reloadDisplayedData();
    }

    private void initialize() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(getStandardErrorLabel());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(toolBar, BorderLayout.NORTH);

        DocumentTabPane tabPane = new DocumentTabPane();

        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, AutoResizeSplitPane.RATIO_3_TO_1);

        // Add annotation info
        annotationInfoPanel = new JIPipeCachedDataViewerAnnotationInfoPanel(getWorkbench());
        tabPane.addTab("Annotations",
                UIUtils.getIconFromResources("data-types/annotation.png"),
                annotationInfoPanel,
                DocumentTabPane.CloseMode.withoutCloseButton);

        splitPane.setRightComponent(tabPane);
        getContentPane().add(splitPane, BorderLayout.CENTER);
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
    protected void loadData(JIPipeVirtualData virtualData, JIPipeProgressInfo progressInfo) {
        annotationInfoPanel.displayAnnotations(getDataSource());
        TrackCollectionData trackCollectionData = virtualData.getData(TrackCollectionData.class, progressInfo);
        TrackScheme trackScheme = new TrackScheme(trackCollectionData.getModel(), new SelectionModel(trackCollectionData.getModel()), displaySettings);
        trackScheme.render();
        trackScheme.getGUI().setVisible(false);
        splitPane.setLeftComponent(trackScheme.getGUI().getContentPane());
    }
}
