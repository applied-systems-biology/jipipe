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

package org.hkijena.jipipe.extensions.imagejdatatypes.display;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeCacheSlotDataSource;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.AnimationSpeedPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.AnnotationInfoPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.CalibrationPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ImageViewerPanelPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.LUTManagerPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.PixelInfoPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ROIManagerPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer.MeasurementDrawerPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer.MeasurementPlugin;
import org.hkijena.jipipe.extensions.settings.ImageViewerUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

public class CachedImagePlusDataViewerWindow extends JIPipeCacheDataViewerWindow implements WindowListener {

    private final JLabel errorLabel = new JLabel(UIUtils.getIconFromResources("emblems/no-data.png"));
    private ImageViewerPanel imageViewerPanel;

    public CachedImagePlusDataViewerWindow(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource, String displayName, boolean deferLoadingData) {
        super(workbench, dataSource, displayName);
        initialize();
        if (!deferLoadingData)
            reloadDisplayedData();
        addWindowListener(this);
    }

    private void initialize() {
        imageViewerPanel = new ImageViewerPanel();
        List<ImageViewerPanelPlugin> pluginList = new ArrayList<>();
        pluginList.add(new CalibrationPlugin(imageViewerPanel));
        pluginList.add(new PixelInfoPlugin(imageViewerPanel));
        pluginList.add(new LUTManagerPlugin(imageViewerPanel));
        pluginList.add(new ROIManagerPlugin(imageViewerPanel));
        pluginList.add(new AnimationSpeedPlugin(imageViewerPanel));
        pluginList.add(new MeasurementDrawerPlugin(imageViewerPanel));
        pluginList.add(new MeasurementPlugin(imageViewerPanel));
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
    public void setTitle(String title) {
        super.setTitle(title);
        imageViewerPanel.setName(getTitle());
    }

    @Override
    protected void beforeSetRow() {

    }

    @Override
    protected void afterSetRow() {
    }

    @Override
    protected void removeErrorUI() {
        imageViewerPanel.getCanvas().setError(null);
    }

    @Override
    protected void addErrorUI() {
        if (getAlgorithm() != null) {
            errorLabel.setText(String.format("No data available in node '%s', slot '%s', row %d", getAlgorithm().getName(), getSlotName(), getDataSource().getRow()));
        } else {
            errorLabel.setText("No data available");
        }
        imageViewerPanel.getCanvas().setError(errorLabel);
    }

    @Override
    protected void loadData(JIPipeVirtualData virtualData, JIPipeProgressInfo progressInfo) {
        ImagePlus image;
        ROIListData rois = new ROIListData();
        if (ImagePlusData.class.isAssignableFrom(virtualData.getDataClass())) {
            ImagePlusData data = (ImagePlusData) virtualData.getData(progressInfo);
            imageViewerPanel.getCanvas().setError(null);
            image = data.getViewedImage(false);
            if (data.getImage().getRoi() != null) {
                rois.add(data.getImage().getRoi());
            }
        } else if (OMEImageData.class.isAssignableFrom(virtualData.getDataClass())) {
            OMEImageData data = (OMEImageData) virtualData.getData(progressInfo);
            imageViewerPanel.getCanvas().setError(null);
            image = data.getImage();
            rois.addAll(data.getRois());
        } else {
            throw new UnsupportedOperationException();
        }
        image.setTitle(image.getTitle());
        boolean fitImage = imageViewerPanel.getImage() == null;
        imageViewerPanel.setImage(image);
        if (!rois.isEmpty() || ImageViewerUISettings.getInstance().isAlwaysClearROIs()) {
            imageViewerPanel.getPlugin(ROIManagerPlugin.class).clearROIs();
            imageViewerPanel.getPlugin(ROIManagerPlugin.class).importROIs(rois);
        }
        if (fitImage)
            SwingUtilities.invokeLater(imageViewerPanel::fitImageToScreen);
    }

    @Override
    public void windowOpened(WindowEvent e) {
        imageViewerPanel.setName(getTitle());
        imageViewerPanel.addToOpenPanels();
        imageViewerPanel.setAsActiveViewerPanel();
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
        imageViewerPanel.setAsActiveViewerPanel();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
