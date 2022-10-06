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
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.JIPipeProjectCacheQuery;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.window.AlwaysOnTopToggle;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
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
    private final JPanel contentPane = new JPanel(new BorderLayout());
    private JIPipeDataTableDataSource dataSource;
    private JIPipeCachedDataDisplayCacheControl cacheAwareToggle;
    private WeakReference<JIPipeVirtualData> lastVirtualData;
    private JButton previousRowButton;
    private JButton nextRowButton;
    private JButton rowInfoLabel;

    private JPopupMenu rowInfoLabelMenu = new JPopupMenu();
    private Function<JIPipeVirtualData, JIPipeVirtualData> dataConverterFunction;

    private JLabel standardErrorLabel;

    public JIPipeCacheDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        this.workbench = workbench;
        this.dataSource = dataSource;
        this.slotName = dataSource.getDataTable().getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "");
        this.project = ((JIPipeProjectWorkbench) workbench).getProject();
        this.displayName = displayName;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        initialize();

        if (dataSource.getDataTable() instanceof JIPipeDataSlot) {
            JIPipeGraphNode node = ((JIPipeDataSlot) dataSource.getDataTable()).getNode();
            if (node != null && node.getParentGraph() != null) {
                this.algorithm = (JIPipeAlgorithm) project.getGraph().getEquivalentAlgorithm(node);
            } else {
                this.algorithm = null;
            }
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
        rowInfoLabel = new JButton("?/?");
        rowInfoLabel.setBorder(null);
        UIUtils.addReloadablePopupMenuToComponent(rowInfoLabel, rowInfoLabelMenu, this::reloadInfoLabelMenu);

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
     * Defaults to getToolBar(). For {@link org.hkijena.jipipe.ui.components.FlexContentPanel}, return getPinToolBar();
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
        getPinToolBar().add(alwaysOnTopToggle);
        if (algorithm != null) {
            cacheAwareToggle = new JIPipeCachedDataDisplayCacheControl((JIPipeProjectWorkbench) workbench, getToolBar(), algorithm);
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

    public JIPipeWorkbench getWorkbench() {
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
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(project);
        Map<String, JIPipeDataSlot> currentCache = query.getCachedData(algorithm);
        JIPipeDataSlot slot = currentCache.getOrDefault(slotName, null);
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
            JIPipeVirtualData virtualData = dataSource.getDataTable().getVirtualData(dataSource.getRow());
            if (lastVirtualData != null && virtualData == lastVirtualData.get())
                return;
            if (dataConverterFunction != null)
                virtualData = dataConverterFunction.apply(virtualData);
            loadData(virtualData, new JIPipeProgressInfo());
            lastVirtualData = new WeakReference<>(virtualData);
        } else {
            JIPipeVirtualData virtualData = dataSource.getDataTable().getVirtualDataAnnotation(dataSource.getRow(), dataSource.getDataAnnotation());
            if (virtualData == null) {
                showErrorUI();
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
