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

package org.hkijena.jipipe.extensions.ijtrackmate.display.spots;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imageviewer.plugins.*;
import org.hkijena.jipipe.extensions.imageviewer.plugins.maskdrawer.MeasurementDrawerPlugin;
import org.hkijena.jipipe.extensions.imageviewer.plugins.roimanager.ROIManagerPlugin;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

public class CachedSpotCollectionDataViewerWindow extends JIPipeCacheDataViewerWindow implements WindowListener {

    private final JLabel errorLabel = new JLabel(UIUtils.getIconFromResources("emblems/no-data.png"));
    private ImageViewerPanel imageViewerPanel;

    public CachedSpotCollectionDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName, boolean deferLoading) {
        super(workbench, dataSource, displayName);
        initialize();
        if (!deferLoading)
            reloadDisplayedData();
        addWindowListener(this);
    }

    private void initialize() {
        imageViewerPanel = new ImageViewerPanel(getWorkbench());
        List<ImageViewerPanelPlugin> pluginList = new ArrayList<>();
        pluginList.add(new CalibrationPlugin(imageViewerPanel));
        pluginList.add(new PixelInfoPlugin(imageViewerPanel));
        pluginList.add(new LUTManagerPlugin(imageViewerPanel));
        pluginList.add(new SpotsManagerPlugin(imageViewerPanel));
        pluginList.add(new ROIManagerPlugin(imageViewerPanel));
        pluginList.add(new AnimationSpeedPlugin(imageViewerPanel));
        pluginList.add(new MeasurementDrawerPlugin(imageViewerPanel));
        pluginList.add(new AnnotationInfoPlugin(imageViewerPanel, this));
        imageViewerPanel.setPlugins(pluginList);
        setContentPane(imageViewerPanel);
        revalidate();
        repaint();
    }

    @Override
    public JToolBar getToolBar() {
        if (imageViewerPanel == null)
            return null;
        else
            return imageViewerPanel.getToolBar();
    }

    @Override
    protected void beforeSetRow() {

    }

    @Override
    protected void afterSetRow() {
    }

    @Override
    protected void hideErrorUI() {
        imageViewerPanel.getCanvas().setError(null);
    }

    @Override
    protected void showErrorUI() {
        if (getAlgorithm() != null) {
            errorLabel.setText(String.format("No data available in node '%s', slot '%s', row %d", getAlgorithm().getName(), getSlotName(), getDataSource().getRow()));
        } else {
            errorLabel.setText("No data available");
        }
        imageViewerPanel.getCanvas().setError(errorLabel);
    }

    @Override
    protected void loadData(JIPipeVirtualData virtualData, JIPipeProgressInfo progressInfo) {
//        ROIManagerPlugin plugin = imageViewerPanel.getPlugin(ROIManagerPlugin.class);
//        SpotsCollectionData data = JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), SpotsCollectionData.class);
//        imageViewerPanel.getCanvas().setError(null);
//        boolean fitImage = imageViewerPanel.getImage() == null;
//        plugin.clearROIs(true);
//        plugin.importROIs(data.spotsToROIList(), true);
        SpotsManagerPlugin plugin = imageViewerPanel.getPlugin(SpotsManagerPlugin.class);
        SpotsCollectionData data = JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), SpotsCollectionData.class);
        boolean fitImage = imageViewerPanel.getImage() == null;
        plugin.setSpotCollection(data, true);
        imageViewerPanel.getCanvas().setError(null);
        imageViewerPanel.setImage(data.getImage());
        if (fitImage)
            SwingUtilities.invokeLater(imageViewerPanel::fitImageToScreen);
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        imageViewerPanel.dispose();
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
