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
import ij.WindowManager;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.data.JIPipeCacheSlotDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.ImageViewerPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Component;
import java.util.Map;

public class CacheAwareImagePlusDataDisplay extends ImageViewerPanel {
    private final JIPipeProject project;
    private final JIPipeWorkbench workbench;
    private JIPipeCacheSlotDataSource dataSource;
    private final JIPipeAlgorithm algorithm;
    private final String slotName;
    private Component errorPanel;
    private JToggleButton cacheAwareToggle;

    public CacheAwareImagePlusDataDisplay(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource) {
        this.project = ((JIPipeProjectWorkbench)workbench).getProject();
        this.workbench = workbench;
        this.dataSource = dataSource;
        this.algorithm = (JIPipeAlgorithm) project.getGraph().getEquivalentAlgorithm(dataSource.getSlot().getNode());
        this.slotName = dataSource.getSlot().getName();
        initialize();
        loadImageFromDataSource();

        project.getCache().getEventBus().register(this);
    }

    private void initialize() {
        cacheAwareToggle.setSelected(true);
        cacheAwareToggle.addActionListener(e -> {
            if(cacheAwareToggle.isSelected()) {
                reloadFromCurrentCache();
            }
        });
        errorPanel = new JLabel(String.format("No data available in node '%s', slot '%s', row %d", algorithm.getName(), slotName, dataSource.getRow()),
                UIUtils.getIconFromResources("emblems/no-data.png"), JLabel.LEFT);
    }

    private void loadImageFromDataSource() {
        ImagePlusData data = dataSource.getSlot().getData(dataSource.getRow(), ImagePlusData.class);
        ImagePlus image = data.getDuplicateImage();
        setImage(image);
        WindowManager.setTempCurrentImage(image);
    }

    @Override
    protected void addLeftToolbarButtons(JToolBar toolBar) {
        super.addLeftToolbarButtons(toolBar);
        this.cacheAwareToggle = new JToggleButton("Refresh to cache", UIUtils.getIconFromResources("actions/view-refresh.png"));
        toolBar.add(cacheAwareToggle);
    }

    public static void show(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource, String displayName) {
        CacheAwareImagePlusDataDisplay dataDisplay = new CacheAwareImagePlusDataDisplay(workbench, dataSource);
        JFrame frame = new JFrame(displayName);
        frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        frame.setContentPane(dataDisplay);
        frame.pack();
        frame.setSize(1024,768);
        frame.setVisible(true);
        SwingUtilities.invokeLater(dataDisplay::fitImageToScreen);
    }

    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        if(!isDisplayable())
            return;
        if(!cacheAwareToggle.isSelected())
            return;
        reloadFromCurrentCache();
    }

    private void reloadFromCurrentCache() {
        Map<String, JIPipeDataSlot> currentCache = project.getCurrentCache(algorithm);
        JIPipeDataSlot slot = currentCache.getOrDefault(slotName, null);
        if(slot != null && slot.getRowCount() > dataSource.getRow()) {
            getCanvas().setError(null);
            dataSource = new JIPipeCacheSlotDataSource(slot, dataSource.getRow());
            loadImageFromDataSource();
        }
        else {
            getCanvas().setError(errorPanel);
        }
    }
}
