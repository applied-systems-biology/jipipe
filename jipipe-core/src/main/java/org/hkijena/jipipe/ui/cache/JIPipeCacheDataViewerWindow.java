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
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.function.Function;

/**
 * Base class for a Window that displays cached data.
 */
public abstract class JIPipeCacheDataViewerWindow extends JFrame {

    private final AlwaysOnTopToggle alwaysOnTopToggle = new AlwaysOnTopToggle(this);
    private final JIPipeWorkbench workbench;
    private final JIPipeAlgorithm algorithm;
    private final JIPipeProject project;
    private final String displayName;
    private final String slotName;
    private JIPipeCacheSlotDataSource dataSource;
    private JIPipeCachedDataDisplayCacheControl cacheAwareToggle;
    private WeakReference<JIPipeVirtualData> lastVirtualData;
    private JButton previousRowButton;
    private JButton nextRowButton;
    private JLabel rowInfoLabel;
    private JPanel contentPane = new JPanel(new BorderLayout());
    private Function<JIPipeVirtualData, JIPipeVirtualData> dataConverterFunction;

    public JIPipeCacheDataViewerWindow(JIPipeWorkbench workbench, JIPipeCacheSlotDataSource dataSource, String displayName) {
        this.workbench = workbench;
        this.dataSource = dataSource;
        this.slotName = dataSource.getSlot().getName();
        this.project = ((JIPipeProjectWorkbench) workbench).getProject();
        this.displayName = displayName;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        initialize();


        if (dataSource.getSlot().getNode() != null) {
            if (dataSource.getSlot().getNode().getGraph() != null)
                this.algorithm = (JIPipeAlgorithm) project.getGraph().getEquivalentAlgorithm(dataSource.getSlot().getNode());
            else
                this.algorithm = null;
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

    private void initialize() {
        previousRowButton = new JButton(UIUtils.getIconFromResources("actions/arrow-up.png"));
        previousRowButton.setToolTipText("<html>Go to previous data row<br/>Ctrl+Up</html>");
        previousRowButton.addActionListener(e -> gotoPreviousRow());
        UIUtils.makeFlat25x25(previousRowButton);
        nextRowButton = new JButton(UIUtils.getIconFromResources("actions/arrow-down.png"));
        nextRowButton.setToolTipText("<html>Go to next data row<br/>Ctrl+Down</html>");
        nextRowButton.addActionListener(e -> gotoNextRow());
        UIUtils.makeFlat25x25(nextRowButton);
        rowInfoLabel = new JLabel("?/?");

        super.setContentPane(contentPane);
        InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = contentPane.getActionMap();

        actionMap.put("next-row", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gotoNextRow();
            }
        });
        actionMap.put("previous-row", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gotoPreviousRow();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), "previous-row");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "next-row");
    }

    @Override
    public Container getContentPane() {
        return contentPane;
    }

    @Override
    public void setContentPane(Container contentPane) {
        this.contentPane.removeAll();
        this.contentPane.add(contentPane);
        revalidate();
        repaint();
    }

    private void gotoPreviousRow() {
        int row = getDataSource().getRow() - 1;
        if (row < 0)
            row += getDataSource().getSlot().getRowCount();
        setDataSourceRow(row);
    }

    public void gotoNextRow() {
        int row = (getDataSource().getRow() + 1) % getDataSource().getSlot().getRowCount();
        setDataSourceRow(row);
    }

    public void reloadDisplayedData() {
        setDataSourceRow(getDataSource().getRow());
    }

    public void setDataSourceRow(int row) {

        rowInfoLabel.setText((row + 1) + "/" + getDataSource().getSlot().getRowCount());

        if (getAlgorithm() != null) {
            setTitle(getAlgorithm().getName() + "/" + getSlotName() + "/" + row + (dataSource.getDataAnnotation() != null ? "/$" + dataSource.getDataAnnotation() : ""));
        } else {
            setTitle(getDisplayName() + (dataSource.getDataAnnotation() != null ? "/$" + dataSource.getDataAnnotation() : ""));
        }

        removeDataControls();
        beforeSetRow();
        JIPipeDataSlot slot = dataSource.getSlot();
        dataSource = new JIPipeCacheSlotDataSource(slot, row, dataSource.getDataAnnotation());
        afterSetRow();
        addDataControls();

        revalidate();
        repaint();

        if (slot != null && slot.getRowCount() > dataSource.getRow()) {
            removeErrorUI();
            dataSource = new JIPipeCacheSlotDataSource(slot, dataSource.getRow(), dataSource.getDataAnnotation());
            loadFromDataSource();
        } else {
            lastVirtualData = null;
            addErrorUI();
        }
    }

    /**
     * Returns the toolbar that contains
     *
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
     *
     * @param virtualData  the data to be loaded
     * @param progressInfo the progress info
     */
    protected abstract void loadData(JIPipeVirtualData virtualData, JIPipeProgressInfo progressInfo);

    private void removeDataControls() {
        if (getToolBar() == null)
            return;
        getToolBar().remove(previousRowButton);
        getToolBar().remove(nextRowButton);
        getToolBar().remove(rowInfoLabel);
        getToolBar().remove(alwaysOnTopToggle);
        if (cacheAwareToggle != null)
            cacheAwareToggle.uninstall();
    }

    private void addDataControls() {
        if (getToolBar() == null)
            return;
        getToolBar().add(alwaysOnTopToggle);
        cacheAwareToggle = new JIPipeCachedDataDisplayCacheControl((JIPipeProjectWorkbench) workbench, getToolBar(), algorithm);
        cacheAwareToggle.install();
        if (algorithm != null) {
            cacheAwareToggle.installRefreshOnActivate(this::reloadFromCurrentCache);
        }
        getToolBar().add(nextRowButton, 0);
        getToolBar().add(rowInfoLabel, 0);
        getToolBar().add(previousRowButton, 0);
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
            dataSource = new JIPipeCacheSlotDataSource(slot, dataSource.getRow(), dataSource.getDataAnnotation());
            loadFromDataSource();
        } else {
            lastVirtualData = null;
            addErrorUI();
        }
    }

    private void loadFromDataSource() {
        if (dataSource.getDataAnnotation() == null) {
            JIPipeVirtualData virtualData = dataSource.getSlot().getVirtualData(dataSource.getRow());
            if (lastVirtualData != null && virtualData == lastVirtualData.get())
                return;
            if (dataConverterFunction != null)
                virtualData = dataConverterFunction.apply(virtualData);
            loadData(virtualData, new JIPipeProgressInfo());
            lastVirtualData = new WeakReference<>(virtualData);
        } else {
            JIPipeVirtualData virtualData = dataSource.getSlot().getVirtualDataAnnotation(dataSource.getRow(), dataSource.getDataAnnotation());
            if (virtualData == null) {
                addErrorUI();
                return;
            }
            if (lastVirtualData != null && virtualData == lastVirtualData.get())
                return;
            if (dataConverterFunction != null)
                virtualData = dataConverterFunction.apply(virtualData);
            loadData(virtualData, new JIPipeProgressInfo());
            lastVirtualData = new WeakReference<>(virtualData);
        }
    }

    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        if (!isVisible())
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

    public Function<JIPipeVirtualData, JIPipeVirtualData> getDataConverterFunction() {
        return dataConverterFunction;
    }

    public void setDataConverterFunction(Function<JIPipeVirtualData, JIPipeVirtualData> dataConverterFunction) {
        this.dataConverterFunction = dataConverterFunction;
    }
}
