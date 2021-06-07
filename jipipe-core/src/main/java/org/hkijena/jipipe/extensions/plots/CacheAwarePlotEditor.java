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

package org.hkijena.jipipe.extensions.plots;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.JIPipeProjectCacheQuery;
import org.hkijena.jipipe.api.data.JIPipeCacheSlotDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCachedDataDisplayCacheControl;
import org.hkijena.jipipe.ui.components.AlwaysOnTopToggle;
import org.hkijena.jipipe.ui.plotbuilder.JIPipePlotBuilderUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Map;

public class CacheAwarePlotEditor extends JIPipePlotBuilderUI {

    private final JIPipeProject project;
    private final JIPipeWorkbench workbench;
    private final JIPipeAlgorithm algorithm;
    private final String slotName;
    private JIPipeCacheSlotDataSource dataSource;
    private JLabel errorPanel;
    private JIPipeCachedDataDisplayCacheControl cacheAwareToggle;
    private WeakReference<JIPipeVirtualData> lastVirtualData;

    public CacheAwarePlotEditor(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource) {
        super(workbench);
        this.project = ((JIPipeProjectWorkbench) workbench).getProject();
        this.workbench = workbench;
        this.dataSource = dataSource;
        this.slotName = dataSource.getSlot().getName();
        if (dataSource.getSlot().getNode() != null) {
            this.algorithm = (JIPipeAlgorithm) project.getGraph().getEquivalentAlgorithm(dataSource.getSlot().getNode());
            this.cacheAwareToggle = new JIPipeCachedDataDisplayCacheControl((JIPipeProjectWorkbench) workbench, getToolBar(), algorithm);
        } else {
            this.algorithm = null;
        }
        initialize();
        loadDataFromDataSource();

        if (algorithm != null)
            project.getCache().getEventBus().register(this);
    }

    public static void show(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource, String displayName) {
        JFrame frame = new JFrame(displayName);
        frame.setAlwaysOnTop(GeneralUISettings.getInstance().isOpenDataWindowsAlwaysOnTop());
        CacheAwarePlotEditor dataDisplay = new CacheAwarePlotEditor(workbench, dataSource);
        AlwaysOnTopToggle alwaysOnTopToggle = new AlwaysOnTopToggle(frame);
        alwaysOnTopToggle.addActionListener(e -> GeneralUISettings.getInstance().setOpenDataWindowsAlwaysOnTop(alwaysOnTopToggle.isSelected()));
        dataDisplay.getToolBar().add(alwaysOnTopToggle);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        frame.setContentPane(dataDisplay);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(workbench.getWindow());
        frame.setVisible(true);
    }

    private void initialize() {
        if (cacheAwareToggle != null) {
            errorPanel = new JLabel("", UIUtils.getIconFromResources("emblems/no-data.png"), JLabel.LEFT);
            errorPanel.setText(String.format("No data available in node '%s', slot '%s', row %d", algorithm.getName(), slotName, dataSource.getRow()));

            cacheAwareToggle.installRefreshOnActivate(this::reloadFromCurrentCache);

            getToolBar().add(Box.createHorizontalStrut(8), 0);
            getToolBar().add(errorPanel, 0);
            cacheAwareToggle.install();
        } else {
            errorPanel = new JLabel("No data available", UIUtils.getIconFromResources("emblems/no-data.png"), JLabel.LEFT);
        }
    }

    private void loadDataFromDataSource() {
        JIPipeVirtualData virtualData = dataSource.getSlot().getVirtualData(dataSource.getRow());
        if (lastVirtualData != null && virtualData == lastVirtualData.get())
            return;
        PlotData data = dataSource.getSlot().getData(dataSource.getRow(), PlotData.class, new JIPipeProgressInfo());
        PlotData duplicate = (PlotData) data.duplicate();
        importExistingPlot(duplicate);
        errorPanel.setVisible(false);
        lastVirtualData = new WeakReference<>(virtualData);
    }

    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null || !window.isVisible())
            return;
        if (!isDisplayable())
            return;
        if (!cacheAwareToggle.shouldRefreshToCache())
            return;
        reloadFromCurrentCache();
    }

    private void reloadFromCurrentCache() {
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(project);
        Map<String, JIPipeDataSlot> currentCache = query.getCachedCache(algorithm);
        JIPipeDataSlot slot = currentCache.getOrDefault(slotName, null);
        errorPanel.setVisible(false);
        if (slot != null && slot.getRowCount() > dataSource.getRow()) {
            dataSource = new JIPipeCacheSlotDataSource(slot, dataSource.getRow());
            loadDataFromDataSource();
        } else {
            errorPanel.setVisible(true);
            lastVirtualData = null;
        }
    }
}
