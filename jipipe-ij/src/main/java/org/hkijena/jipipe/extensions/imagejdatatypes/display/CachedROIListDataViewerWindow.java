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
import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imageviewer.plugins.*;
import org.hkijena.jipipe.extensions.imageviewer.plugins.maskdrawer.MeasurementDrawerPlugin;
import org.hkijena.jipipe.extensions.imageviewer.plugins.maskdrawer.MeasurementPlugin;
import org.hkijena.jipipe.extensions.imageviewer.plugins.roimanager.ROIManagerPlugin;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

public class CachedROIListDataViewerWindow extends JIPipeCacheDataViewerWindow implements WindowListener {

    private final JLabel errorLabel = new JLabel(UIUtils.getIconFromResources("emblems/no-data.png"));
    private ImageViewerPanel imageViewerPanel;

    public CachedROIListDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName, boolean deferLoading) {
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
        ROIManagerPlugin plugin = imageViewerPanel.getPlugin(ROIManagerPlugin.class);
        ROIListData data = JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), ROIListData.class);
        int width;
        int height;
        int numZ = 1;
        int numC = 1;
        int numT = 1;

        if (data.isEmpty()) {
            width = 128;
            height = 128;
        } else {
            Rectangle bounds = data.getBounds();
            width = bounds.x + bounds.width;
            height = bounds.y + bounds.height;
            for (Roi roi : data) {
                numZ = Math.max(roi.getZPosition(), numZ);
                numC = Math.max(roi.getCPosition(), numC);
                numT = Math.max(roi.getTPosition(), numT);
            }
        }

        imageViewerPanel.getCanvas().setError(null);
        ImagePlus image = IJ.createImage("empty", "8-bit", width, height, numC, numZ, numT);
        ImageJUtils.forEachSlice(image, ip -> {
            ip.setColor(0);
            ip.fill();
        }, new JIPipeProgressInfo());
        boolean fitImage = imageViewerPanel.getImage() == null;
        plugin.clearROIs(true);
        plugin.importROIs(data, true);
        imageViewerPanel.setImage(image);
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
