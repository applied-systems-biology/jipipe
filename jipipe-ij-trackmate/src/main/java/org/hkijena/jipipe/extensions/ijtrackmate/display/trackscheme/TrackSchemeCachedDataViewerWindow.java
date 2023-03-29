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
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.ui.cache.JIPipeCachedDataViewerAnnotationInfoPanel;
import org.hkijena.jipipe.ui.components.FlexContentPanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class TrackSchemeCachedDataViewerWindow extends JIPipeCacheDataViewerWindow {
    private final JIPipeCachedDataViewerAnnotationInfoPanel annotationInfoPanel;
    private final DisplaySettings displaySettings = new DisplaySettings();

    private final FlexContentPanel flexContentPanel = new FlexContentPanel();

    public TrackSchemeCachedDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
        this.annotationInfoPanel = new JIPipeCachedDataViewerAnnotationInfoPanel(workbench);
        initialize();
        reloadDisplayedData();
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(flexContentPanel, BorderLayout.CENTER);

        flexContentPanel.getToolBar().add(getStandardErrorLabel());
        flexContentPanel.getSideBar().addTab("Annotations",
                UIUtils.getIconFromResources("data-types/annotation.png"),
                annotationInfoPanel,
                DocumentTabPane.CloseMode.withoutCloseButton);
    }

    @Override
    public JToolBar getToolBar() {
        return flexContentPanel.getToolBar();
    }

    @Override
    protected void beforeSetRow() {

    }

    @Override
    protected void afterSetRow() {

    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        annotationInfoPanel.displayAnnotations(getDataSource());
        TrackCollectionData trackCollectionData = virtualData.getData(TrackCollectionData.class, progressInfo);
        TrackScheme trackScheme = new TrackScheme(trackCollectionData.getModel(), new SelectionModel(trackCollectionData.getModel()), displaySettings);
        trackScheme.render();
        trackScheme.getGUI().setVisible(false);
        flexContentPanel.getContentPanel().removeAll();
        flexContentPanel.getContentPanel().add(trackScheme.getGUI().getContentPane());
        flexContentPanel.getContentPanel().revalidate();
        flexContentPanel.getContentPanel().repaint();
    }
}
