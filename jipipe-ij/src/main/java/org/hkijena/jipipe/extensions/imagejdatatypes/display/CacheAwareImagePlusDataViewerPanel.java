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

import com.google.common.eventbus.Subscribe;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.JIPipeProjectCacheQuery;
import org.hkijena.jipipe.api.data.JIPipeCacheSlotDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerWindow;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer.MeasurementDrawerPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer.MeasurementPlugin;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCachedDataDisplayCacheControl;
import org.hkijena.jipipe.ui.components.AlwaysOnTopToggle;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.List;

public class CacheAwareImagePlusDataViewerPanel extends ImageViewerPanel {
    private final JIPipeProject project;
    private final JIPipeWorkbench workbench;
    private final JIPipeAlgorithm algorithm;
    private final String slotName;
    private JIPipeCacheSlotDataSource dataSource;
    private Component errorPanel;
    private JIPipeCachedDataDisplayCacheControl cacheAwareToggle;
    private WeakReference<JIPipeVirtualData> lastVirtualData;

    public CacheAwareImagePlusDataViewerPanel(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource) {
        List<ImageViewerPanelPlugin> pluginList = new ArrayList<>();
        pluginList.add(new CalibrationPlugin(this));
        pluginList.add(new PixelInfoPlugin(this));
        pluginList.add(new LUTManagerPlugin(this));
        pluginList.add(new ROIManagerPlugin(this));
        pluginList.add(new AnimationSpeedPlugin(this));
        pluginList.add(new MeasurementDrawerPlugin(this));
        pluginList.add(new MeasurementPlugin(this));
        setPlugins(pluginList);
        this.project = ((JIPipeProjectWorkbench) workbench).getProject();
        this.workbench = workbench;
        this.dataSource = dataSource;
        this.slotName = dataSource.getSlot().getName();
        if(dataSource.getSlot().getNode() != null) {
            this.algorithm = (JIPipeAlgorithm) project.getGraph().getEquivalentAlgorithm(dataSource.getSlot().getNode());
            this.cacheAwareToggle = new JIPipeCachedDataDisplayCacheControl((JIPipeProjectWorkbench) workbench, getToolBar(), algorithm);
        }
        else {
            this.algorithm = null;
        }
        initialize();
        loadImageFromDataSource();

        if(algorithm != null)
            project.getCache().getEventBus().register(this);
    }

    private void initialize() {
        if(algorithm != null) {
            cacheAwareToggle.installRefreshOnActivate(this::reloadFromCurrentCache);
            errorPanel = new JLabel(String.format("No data available in node '%s', slot '%s', row %d", algorithm.getName(), slotName, dataSource.getRow()),
                    UIUtils.getIconFromResources("emblems/no-data.png"), JLabel.LEFT);
            getToolBar().add(Box.createHorizontalStrut(8), 0);
            cacheAwareToggle.install();
        }
        else {
            errorPanel = new JLabel("No data available",
                    UIUtils.getIconFromResources("emblems/no-data.png"), JLabel.LEFT);
        }
//        getToolBar().add(cacheAwareToggle, 0);
    }

    private void loadImageFromDataSource() {
        JIPipeVirtualData virtualData = dataSource.getSlot().getVirtualData(dataSource.getRow());
        if (lastVirtualData != null && virtualData == lastVirtualData.get())
            return;
        ImagePlusData data = dataSource.getSlot().getData(dataSource.getRow(), ImagePlusData.class, new JIPipeProgressInfo());
        ImagePlus image = data.getViewedImage(true);
        image.setTitle(data.getImage().getTitle());
        setImage(image);
        lastVirtualData = new WeakReference<>(virtualData);
    }

    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null || !window.isVisible())
            return;
        if (!isDisplayable())
            return;
        if(cacheAwareToggle == null)
            return;
        reloadFromCurrentCache();
    }

    private void reloadFromCurrentCache() {
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(project);
        Map<String, JIPipeDataSlot> currentCache = query.getCachedCache(algorithm);
        JIPipeDataSlot slot = currentCache.getOrDefault(slotName, null);
        if (slot != null && slot.getRowCount() > dataSource.getRow()) {
            getCanvas().setError(null);
            dataSource = new JIPipeCacheSlotDataSource(slot, dataSource.getRow());
            loadImageFromDataSource();
        } else {
            lastVirtualData = null;
            getCanvas().setError(errorPanel);
        }
    }

    public static void show(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource, String displayName) {
        CacheAwareImagePlusDataViewerPanel dataDisplay = new CacheAwareImagePlusDataViewerPanel(workbench, dataSource);
        ImageViewerWindow window = new ImageViewerWindow(dataDisplay);
        window.setAlwaysOnTop(GeneralUISettings.getInstance().isOpenDataWindowsAlwaysOnTop());
        AlwaysOnTopToggle alwaysOnTopToggle = new AlwaysOnTopToggle(window);
        alwaysOnTopToggle.addActionListener(e -> GeneralUISettings.getInstance().setOpenDataWindowsAlwaysOnTop(alwaysOnTopToggle.isSelected()));
        dataDisplay.getToolBar().add(alwaysOnTopToggle);
        window.setTitle(displayName);
        window.setLocationRelativeTo(workbench.getWindow());
        window.setVisible(true);
    }
}
