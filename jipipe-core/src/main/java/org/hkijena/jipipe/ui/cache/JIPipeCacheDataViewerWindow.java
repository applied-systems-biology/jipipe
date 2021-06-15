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

package org.hkijena.jipipe.ui.cache;

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
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.AlwaysOnTopToggle;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Window;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Base class for a Window that displays cached data.
 */
public abstract class JIPipeCacheDataViewerWindow extends JFrame {

    private final  AlwaysOnTopToggle alwaysOnTopToggle = new AlwaysOnTopToggle(this);
    private final JIPipeWorkbench workbench;
    private JIPipeCacheSlotDataSource dataSource;
    private final JIPipeAlgorithm algorithm;
    private final JIPipeProject project;
    private final String displayName;
    private final String slotName;
    private JIPipeCachedDataDisplayCacheControl cacheAwareToggle;
    private WeakReference<JIPipeVirtualData> lastVirtualData;

    public JIPipeCacheDataViewerWindow(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource, String displayName) {
        this.workbench = workbench;
        this.dataSource = dataSource;
        this.slotName = dataSource.getSlot().getName();
        this.project = ((JIPipeProjectWorkbench) workbench).getProject();
        this.displayName = displayName;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());


        if (dataSource.getSlot().getNode() != null) {
            this.algorithm = (JIPipeAlgorithm) project.getGraph().getEquivalentAlgorithm(dataSource.getSlot().getNode());
        } else {
            this.algorithm = null;
        }

        alwaysOnTopToggle.addActionListener(e -> GeneralUISettings.getInstance().setOpenDataWindowsAlwaysOnTop(alwaysOnTopToggle.isSelected()));

        if (algorithm != null)
            project.getCache().getEventBus().register(this);

        pack();
        setSize(1024, 768);
        setLocationRelativeTo(workbench.getWindow());
    }

    public void reloadDisplayedData() {
        setDataSourceRow(getDataSource().getRow());
    }

    public void setDataSourceRow(int row) {

        if(getAlgorithm() != null) {
            setTitle(getAlgorithm().getName() + "/" + getSlotName() + "/" + row);
        }
        else {
            setTitle(getDisplayName());
        }

        removeDataControls();
        beforeSetRow();
        JIPipeDataSlot slot = dataSource.getSlot();
        dataSource = new JIPipeCacheSlotDataSource(slot, row);
        afterSetRow();
        addDataControls();

        revalidate();
        repaint();

        if (slot != null && slot.getRowCount() > dataSource.getRow()) {
            removeErrorUI();
            dataSource = new JIPipeCacheSlotDataSource(slot, dataSource.getRow());
            loadFromDataSource();
        } else {
            lastVirtualData = null;
            addErrorUI();
        }
    }

    /**
     * Returns the toolbar that contains
     * @return the toolbar
     */
    public abstract JToolBar getToolBar();

    /**
     * Called before the data row is changed
     */
    protected abstract void beforeSetRow();

    /**
     * Called after the data row was changed
     */
    protected abstract void afterSetRow();

    /**
     * Instruction to remove the error UI
     */
    protected abstract void removeErrorUI();

    /**
     * Instruction to add the error UI
     */
    protected abstract void addErrorUI();

    /**
     * Instruction to load the data from the current data source
     * @param virtualData the data to be loaded
     * @param progressInfo the progress info
     */
    protected abstract void loadData(JIPipeVirtualData virtualData, JIPipeProgressInfo progressInfo);

    private void removeDataControls() {
        if(getToolBar() == null)
            return;
        getToolBar().remove(alwaysOnTopToggle);
        if(cacheAwareToggle != null)
            cacheAwareToggle.uninstall();
    }

    private void addDataControls() {
        if(getToolBar() == null)
            return;
        getToolBar().add(alwaysOnTopToggle);
        cacheAwareToggle = new JIPipeCachedDataDisplayCacheControl((JIPipeProjectWorkbench) workbench, getToolBar(), algorithm);
        cacheAwareToggle.install();
        if(algorithm != null) {
            cacheAwareToggle.installRefreshOnActivate(this::reloadFromCurrentCache);
        }
    }

    public JIPipeAlgorithm getAlgorithm() {
        return algorithm;
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeCacheSlotDataSource getDataSource() {
        return dataSource;
    }

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public String getSlotName() {
        return slotName;
    }

    private void reloadFromCurrentCache() {
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(project);
        Map<String, JIPipeDataSlot> currentCache = query.getCachedCache(algorithm);
        JIPipeDataSlot slot = currentCache.getOrDefault(slotName, null);
        if (slot != null && slot.getRowCount() > dataSource.getRow()) {
            removeErrorUI();
            dataSource = new JIPipeCacheSlotDataSource(slot, dataSource.getRow());
            loadFromDataSource();
        } else {
            lastVirtualData = null;
            addErrorUI();
        }
    }

    private void loadFromDataSource() {
        JIPipeVirtualData virtualData = dataSource.getSlot().getVirtualData(dataSource.getRow());
        if (lastVirtualData != null && virtualData == lastVirtualData.get())
            return;
        loadData(virtualData, new JIPipeProgressInfo());
        lastVirtualData = new WeakReference<>(virtualData);
    }

    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null || !window.isVisible())
            return;
        if (!isDisplayable())
            return;
        if (cacheAwareToggle == null)
            return;
        reloadFromCurrentCache();
    }

    public String getDisplayName() {
        return displayName;
    }
}
