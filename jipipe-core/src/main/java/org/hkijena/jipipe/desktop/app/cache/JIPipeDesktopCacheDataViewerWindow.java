/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.cache;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.utils.JIPipeWeakDataReferenceData;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFlexContentPanel;
import org.hkijena.jipipe.desktop.commons.components.window.JIPipeDesktopAlwaysOnTopToggle;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.data.WeakStore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.function.Function;

/**
 * Base class for a Window that displays cached data.
 */
public abstract class JIPipeDesktopCacheDataViewerWindow extends JFrame implements JIPipeCache.ModifiedEventListener {

    private final JIPipeDesktopAlwaysOnTopToggle alwaysOnTopToggle = new JIPipeDesktopAlwaysOnTopToggle(this);
    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeAlgorithm algorithm;
    private final JIPipeProject project;
    private final String displayName;
    private final String slotName;
    private final JPanel contentPane = new JPanel(new BorderLayout());
    private final JPopupMenu rowInfoLabelMenu = new JPopupMenu();
    private JIPipeDataTableDataSource dataSource;
    private JIPipeDesktopCachedDataDisplayCacheControl cacheAwareToggle;
    private Store<JIPipeDataItemStore> lastVirtualData;
    private JButton previousRowButton;
    private JButton nextRowButton;
    private JButton rowInfoLabel;
    private Function<JIPipeDataItemStore, JIPipeDataItemStore> dataConverterFunction;

    private JLabel standardErrorLabel;

    public JIPipeDesktopCacheDataViewerWindow(JIPipeDesktopWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        this.workbench = workbench;
        this.dataSource = dataSource;
        this.slotName = dataSource.getDataTable().getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "");
        this.project = ((JIPipeDesktopProjectWorkbench) workbench).getProject();
        this.displayName = displayName;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(UIUtils.getJIPipeIcon128());
        initialize();

        if (dataSource.getDataTable() instanceof JIPipeDataSlot) {
            JIPipeGraphNode node = ((JIPipeDataSlot) dataSource.getDataTable()).getNode();
            if (node != null && node.getParentGraph() != null) {
                this.algorithm = (JIPipeAlgorithm) project.getGraph().getEquivalentNode(node);
            } else {
                this.algorithm = null;
            }
        } else {
            this.algorithm = null;
        }

        alwaysOnTopToggle.addActionListener(e -> JIPipeGeneralUIApplicationSettings.getInstance().setOpenDataWindowsAlwaysOnTop(alwaysOnTopToggle.isSelected()));

        if (algorithm != null) {
            project.getCache().getModifiedEventEmitter().subscribeWeak(this);
        }

        pack();
        setSize(1024, 768);
        setLocationRelativeTo(workbench.getWindow());
    }

    private void initialize() {
        previousRowButton = new JButton(UIUtils.getIconFromResources("actions/caret-up.png"));
        previousRowButton.setToolTipText("<html>Go to previous data row<br/>Ctrl+Up</html>");
        previousRowButton.addActionListener(e -> gotoPreviousRow());
        UIUtils.makeFlat25x25(previousRowButton);
        nextRowButton = new JButton(UIUtils.getIconFromResources("actions/caret-down.png"));
        nextRowButton.setToolTipText("<html>Go to next data row<br/>Ctrl+Down</html>");
        nextRowButton.addActionListener(e -> gotoNextRow());
        UIUtils.makeFlat25x25(nextRowButton);
        rowInfoLabel = new JButton("?/?");
        rowInfoLabel.setBorder(null);
        UIUtils.addReloadablePopupMenuToButton(rowInfoLabel, rowInfoLabelMenu, this::reloadInfoLabelMenu);

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

        // Create a standard error label
        standardErrorLabel = new JLabel(UIUtils.getIconFromResources("emblems/no-data.png"));
    }

    private void reloadInfoLabelMenu() {
        rowInfoLabelMenu.removeAll();
        JMenuItem setRowItem = new JMenuItem("Go to row ...", UIUtils.getIconFromResources("actions/go-jump.png"));
        setRowItem.addActionListener(e -> gotoUserDefinedRow());
        rowInfoLabelMenu.add(setRowItem);

        int nRow = getDataSource().getDataTable().getRowCount();
        if (nRow > 0) {
            rowInfoLabelMenu.addSeparator();
            JComponent currentMenu = rowInfoLabelMenu;
            for (int i = 0; i < nRow; i++) {
                JMenuItem item = new JMenuItem("" + (i + 1));
                int finalI = i;
                item.addActionListener(e -> gotoRow(finalI + 1));
                currentMenu.add(item);

                if (i != nRow - 1 && currentMenu.getComponentCount() >= 10) {
                    JMenu newMenu = new JMenu("More ...");
                    currentMenu.add(newMenu);
                    currentMenu = newMenu;
                }
            }
        }
    }

    @Override
    public void dispose() {
        dataSource = null;
        if (cacheAwareToggle != null) {
            cacheAwareToggle.dispose();
        }
        super.dispose();
    }

    /**
     * Returns a standard error label that simplifies the creation of cache windows.
     * It must be included into the UI manually
     *
     * @return the standard error label
     */
    public JLabel getStandardErrorLabel() {
        return standardErrorLabel;
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

    private void gotoUserDefinedRow() {
        String input = JOptionPane.showInputDialog(this,
                "Please input the target row (1 is the first item):",
                getDataSource().getRow() + 1);
        if (!StringUtils.isNullOrEmpty(input)) {
            Integer index = NumberUtils.createInteger(input);
            if (index != null) {
                gotoRow(index);
            }
        }
    }

    private void gotoRow(int row) {
        row = Math.max(1, Math.min(getDataSource().getDataTable().getRowCount(), row));
        setDataSourceRow(row - 1);
    }


    private void gotoPreviousRow() {
        int row = getDataSource().getRow() - 1;
        if (row < 0)
            row += getDataSource().getDataTable().getRowCount();
        setDataSourceRow(row);
    }

    public void gotoNextRow() {
        int row = (getDataSource().getRow() + 1) % getDataSource().getDataTable().getRowCount();
        setDataSourceRow(row);
    }

    public void reloadDisplayedData() {
        setDataSourceRow(getDataSource().getRow());
    }

    public void setDataSourceRow(int row) {

        rowInfoLabel.setText((row + 1) + "/" + getDataSource().getDataTable().getRowCount());

        if (getAlgorithm() != null) {
            setTitle(getAlgorithm().getName() + "/" + getSlotName() + "/" + row + (dataSource.getDataAnnotation() != null ? "/$" + dataSource.getDataAnnotation() : ""));
        } else {
            setTitle(getDisplayName() + "/" + row + (dataSource.getDataAnnotation() != null ? "/$" + dataSource.getDataAnnotation() : ""));
        }

        removeDataControls();
        beforeSetRow();
        JIPipeDataTable dataTable = dataSource.getDataTable();
        dataSource = new JIPipeDataTableDataSource(dataTable, row, dataSource.getDataAnnotation());
        afterSetRow();
        addDataControls();

        revalidate();
        repaint();

        if (dataTable != null && dataTable.getRowCount() > dataSource.getRow()) {
            hideErrorUI();
            dataSource = new JIPipeDataTableDataSource(dataTable, dataSource.getRow(), dataSource.getDataAnnotation());
            loadFromDataSource();
        } else {
            lastVirtualData = null;
            showErrorUI();
        }
    }

    /**
     * The toolbar where the window pin controls are added.
     * Defaults to getToolBar(). For {@link JIPipeDesktopFlexContentPanel}, return getPinToolBar();
     *
     * @return the pin toolbar
     */
    public JToolBar getPinToolBar() {
        return getToolBar();
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
     * By default, it will hide the standard error label
     */
    protected void hideErrorUI() {
        standardErrorLabel.setVisible(false);
    }

    /**
     * Instruction to add the error UI
     * By default, it will update the standard error label and revalidate the toolbar
     */
    protected void showErrorUI() {
        if (getAlgorithm() != null) {
            standardErrorLabel.setText(String.format("No data available in node '%s', slot '%s', row %d",
                    getAlgorithm().getName(),
                    getSlotName(),
                    getDataSource().getRow()));
        } else {
            standardErrorLabel.setText("No data available");
        }
        standardErrorLabel.setVisible(true);
        getToolBar().revalidate();
        getToolBar().repaint();
    }

    /**
     * Instruction to load the data from the current data source
     *
     * @param virtualData  the data to be loaded
     * @param progressInfo the progress info
     */
    protected abstract void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo);

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
        getPinToolBar().add(alwaysOnTopToggle);
        if (algorithm != null) {
            cacheAwareToggle = new JIPipeDesktopCachedDataDisplayCacheControl((JIPipeDesktopProjectWorkbench) workbench, getToolBar(), algorithm);
            cacheAwareToggle.install();
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

    public JIPipeDataTableDataSource getDataSource() {
        return dataSource;
    }

    public JIPipeDesktopWorkbench getWorkbench() {
        return workbench;
    }

    public String getSlotName() {
        return slotName;
    }

    private void reloadFromCurrentCache() {
        if (!project.getGraph().containsNode(algorithm)) {
            lastVirtualData = null;
            showErrorUI();
            return;
        }
        Map<String, JIPipeDataTable> currentCache = project.getCache().query(algorithm, algorithm.getUUIDInParentGraph(), new JIPipeProgressInfo());
        JIPipeDataTable slot = currentCache.getOrDefault(slotName, null);
        if (slot != null && slot.getRowCount() > dataSource.getRow()) {
            hideErrorUI();
            dataSource = new JIPipeDataTableDataSource(slot, dataSource.getRow(), dataSource.getDataAnnotation());
            loadFromDataSource();
        } else {
            lastVirtualData = null;
            showErrorUI();
        }
    }

    private void loadFromDataSource() {
        if (dataSource.getDataAnnotation() == null) {
            JIPipeDataItemStore virtualData = dataSource.getDataTable().getDataItemStore(dataSource.getRow());
            if (lastVirtualData != null && virtualData == lastVirtualData.get())
                return;
            if (dataConverterFunction != null)
                virtualData = dataConverterFunction.apply(virtualData);
            loadData(virtualData, new JIPipeProgressInfo());
            lastVirtualData = new WeakStore<>(virtualData);
        } else {
            JIPipeDataItemStore virtualData = dataSource.getDataTable().getDataAnnotationItemStore(dataSource.getRow(), dataSource.getDataAnnotation());
            if (virtualData == null) {
                showErrorUI();
                return;
            }
            if (JIPipeWeakDataReferenceData.class.isAssignableFrom(virtualData.getDataClass())) {
                // Dereference weak reference data
                JIPipeWeakDataReferenceData weakDataReferenceData = virtualData.getData(JIPipeWeakDataReferenceData.class, new JIPipeProgressInfo());
                JIPipeData data = weakDataReferenceData.getDataReference().get();
                if (data == null) {
                    showErrorUI();
                    return;
                }
                virtualData = new JIPipeDataItemStore(data);
            }
            if (lastVirtualData != null && virtualData == lastVirtualData.get())
                return;
            if (dataConverterFunction != null)
                virtualData = dataConverterFunction.apply(virtualData);
            loadData(virtualData, new JIPipeProgressInfo());
            lastVirtualData = new WeakStore<>(virtualData);
        }
    }

    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
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

    public Function<JIPipeDataItemStore, JIPipeDataItemStore> getDataConverterFunction() {
        return dataConverterFunction;
    }

    public void setDataConverterFunction(Function<JIPipeDataItemStore, JIPipeDataItemStore> dataConverterFunction) {
        this.dataConverterFunction = dataConverterFunction;
    }
}
