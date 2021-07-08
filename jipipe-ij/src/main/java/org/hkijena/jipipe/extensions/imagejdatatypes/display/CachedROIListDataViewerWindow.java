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

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeCacheSlotDataSource;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.AnimationSpeedPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.CalibrationPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ImageViewerPanelPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.LUTManagerPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.PixelInfoPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ROIManagerPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer.MeasurementDrawerPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer.MeasurementPlugin;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class CachedROIListDataViewerWindow extends JIPipeCacheDataViewerWindow {

    private final JLabel errorLabel = new JLabel(UIUtils.getIconFromResources("emblems/no-data.png"));
    private ImageViewerPanel imageViewerPanel;

    public CachedROIListDataViewerWindow(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
        initialize();
        reloadDisplayedData();
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
        ROIManagerPlugin plugin = imageViewerPanel.getPlugin(ROIManagerPlugin.class);
        ROIListData data = (ROIListData) virtualData.getData(progressInfo);
        int width;
        int height;

        if (data.isEmpty()) {
            width = 128;
            height = 128;
        } else {
            Rectangle bounds = data.getBounds();
            width = bounds.x + bounds.width;
            height = bounds.y + bounds.height;
        }

        imageViewerPanel.getCanvas().setError(null);
        ImagePlus image = IJ.createImage("empty", "8-bit", width, height, 1);
        ImageProcessor processor = image.getProcessor();
        processor.setColor(0);
        processor.fill();
        boolean fitImage = imageViewerPanel.getImage() == null;
        plugin.clearROIs();
        imageViewerPanel.setImage(image);
        plugin.importROIs(data);
        if (fitImage)
            SwingUtilities.invokeLater(imageViewerPanel::fitImageToScreen);
    }
}
