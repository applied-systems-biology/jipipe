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

package org.hkijena.jipipe.extensions.tables.display;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.JIPipeProjectCacheQuery;
import org.hkijena.jipipe.api.data.JIPipeCacheSlotDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.AlwaysOnTopToggle;
import org.hkijena.jipipe.ui.tableanalyzer.JIPipeTableEditor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Map;

public class CacheAwareTableEditor extends JIPipeTableEditor {

    private final JIPipeProject project;
    private final JIPipeWorkbench workbench;
    private final JIPipeAlgorithm algorithm;
    private final String slotName;
    private JIPipeCacheSlotDataSource dataSource;
    private JLabel errorPanel;
    private JToggleButton cacheAwareToggle;
    private WeakReference<JIPipeVirtualData> lastVirtualData;

    public CacheAwareTableEditor(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource) {
        super(workbench, new ResultsTableData());
        this.project = ((JIPipeProjectWorkbench) workbench).getProject();
        this.workbench = workbench;
        this.dataSource = dataSource;
        this.algorithm = (JIPipeAlgorithm) project.getGraph().getEquivalentAlgorithm(dataSource.getSlot().getNode());
        this.slotName = dataSource.getSlot().getName();
        initialize();
        loadDataFromDataSource();

        project.getCache().getEventBus().register(this);
    }

    public static void show(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource, String displayName) {
        JFrame frame = new JFrame(displayName);
        frame.setAlwaysOnTop(GeneralUISettings.getInstance().isOpenDataWindowsAlwaysOnTop());
        CacheAwareTableEditor dataDisplay = new CacheAwareTableEditor(workbench, dataSource);
        AlwaysOnTopToggle alwaysOnTopToggle = new AlwaysOnTopToggle(frame);
        alwaysOnTopToggle.addActionListener(e -> GeneralUISettings.getInstance().setOpenDataWindowsAlwaysOnTop(alwaysOnTopToggle.isSelected()));
        dataDisplay.getToolBar().add(alwaysOnTopToggle);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        frame.setContentPane(dataDisplay);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setVisible(true);
    }

    private void initialize() {
        cacheAwareToggle.setSelected(true);
        cacheAwareToggle.addActionListener(e -> {
            if (cacheAwareToggle.isSelected()) {
                reloadFromCurrentCache();
            }
        });
        errorPanel.setText(String.format("No data available in node '%s', slot '%s', row %d", algorithm.getName(), slotName, dataSource.getRow()));
    }

    private void loadDataFromDataSource() {
        JIPipeVirtualData virtualData = dataSource.getSlot().getVirtualData(dataSource.getRow());
        if (lastVirtualData != null && virtualData == lastVirtualData.get())
            return;
        ResultsTableData data = dataSource.getSlot().getData(dataSource.getRow(), ResultsTableData.class, new JIPipeProgressInfo());
        ResultsTableData duplicate = (ResultsTableData) data.duplicate();
        setTableModel(duplicate);
        errorPanel.setVisible(false);
        lastVirtualData = new WeakReference<>(virtualData);
    }

    @Override
    protected void addLeftToolbarButtons(JToolBar toolBar) {
        super.addLeftToolbarButtons(toolBar);
        this.cacheAwareToggle = new JToggleButton("Refresh to cache", UIUtils.getIconFromResources("actions/view-refresh.png"));
        toolBar.add(cacheAwareToggle);

        errorPanel = new JLabel("", UIUtils.getIconFromResources("emblems/no-data.png"), JLabel.LEFT);
        toolBar.add(errorPanel);
    }

    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null || !window.isVisible())
            return;
        if (!isDisplayable())
            return;
        if (!cacheAwareToggle.isSelected())
            return;
        reloadFromCurrentCache();
    }

    private void reloadFromCurrentCache() {
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(project);
        Map<String, JIPipeDataSlot> currentCache = query.getCachedCache(algorithm);
        JIPipeDataSlot slot = currentCache.getOrDefault(slotName, null);
        errorPanel.setVisible(false);
        if (slot != null && slot.getRowCount() > dataSource.getRow()) {
            setTableModel(new ResultsTableData());
            dataSource = new JIPipeCacheSlotDataSource(slot, dataSource.getRow());
            loadDataFromDataSource();
        } else {
            errorPanel.setVisible(true);
            setTableModel(new ResultsTableData());
            lastVirtualData = null;
        }
    }
}
