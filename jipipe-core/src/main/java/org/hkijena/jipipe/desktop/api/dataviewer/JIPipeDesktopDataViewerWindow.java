package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.browser.JIPipeDataBrowser;
import org.hkijena.jipipe.api.data.browser.JIPipeDataTableBrowser;
import org.hkijena.jipipe.api.data.browser.JIPipeLocalDataTableBrowser;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallToggleButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.window.JIPipeDesktopAlwaysOnTopToggle;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.debounce.DynamicDebouncer;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Window that displays data using {@link JIPipeDesktopDataViewer}
 */
public class JIPipeDesktopDataViewerWindow extends JFrame implements JIPipeDesktopWorkbenchAccess, Disposable, JIPipeRunnable.FinishedEventListener, JIPipeCache.ModifiedEventListener {

    private static boolean HOTKEYS_ENABLED = false;

    private final JIPipeRunnableQueue downloaderQueue = new JIPipeRunnableQueue("Data download");

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeDesktopRibbon ribbon = new JIPipeDesktopRibbon();
    private final JIPipeDesktopDockPanel dockPanel = new JIPipeDesktopDockPanel();
    private final JPanel contentPane = new JPanel(new BorderLayout());
    private final JToolBar staticStatusBar = new JToolBar();
    private final JToolBar dynamicStatusBar = new JToolBar();
    private final JButton dataTypeInfoButton = new JButton();
    private final JToggleButton toggleFocusView = new JToggleButton(UIUtils.getIconFromResources("actions/view-fullscreen.png"));
    private final JIPipeDesktopSmallToggleButtonRibbonAction toggleAutoRefreshFromCache;
    private JIPipeDataBrowser dataBrowser;
    private String displayName;
    private JIPipeLocalDataTableBrowser dataTableBrowser;
    private JIPipeDataTableInfo dataTableInfo;
    private List<String> dataAnnotations;
    private int currentDataRow = -1;
    private int currentDataAnnotationColumn = -1;
    private JIPipeDesktopDataViewer currentDataViewer;
    private final StaticDebouncer refreshFromCacheDebouncer;

    public JIPipeDesktopDataViewerWindow(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        this.toggleAutoRefreshFromCache = new JIPipeDesktopSmallToggleButtonRibbonAction("Auto-refresh", "If enabled, automatically update the displayed data when the cache changes", UIUtils.getIconFromResources("actions/view-refresh.png"), true, (button) -> {
            if (button.isSelected()) {
                refreshFromLocalCache();
            }
        });
        this.refreshFromCacheDebouncer = new StaticDebouncer(1500, this::refreshFromLocalCache);
        initialize();
        registerHotkeys();

        downloaderQueue.getFinishedEventEmitter().subscribe(this);

        // Listen to local cache
        if (workbench.getProject() != null) {
            workbench.getProject().getCache().getModifiedEventEmitter().subscribe(this);
        }

        onDataChanged();
    }

    private void registerHotkeys() {
        if(!HOTKEYS_ENABLED) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
                if(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() instanceof JIPipeDesktopDataViewerWindow) {
                    JIPipeDesktopDataViewerWindow dataViewerWindow = (JIPipeDesktopDataViewerWindow) KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        if (e.getKeyCode() == KeyEvent.VK_DOWN && (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                            dataViewerWindow.goToNextData();
                            return true;
                        }
                        if (e.getKeyCode() == KeyEvent.VK_UP && (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                            dataViewerWindow.goToPreviousData();
                            return true;
                        }
                    }
                }
                return false;
            });
            HOTKEYS_ENABLED = true;
        }
    }

    private void initialize() {
        setIconImage(UIUtils.getJIPipeIcon128());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(contentPane);
        contentPane.add(ribbon, BorderLayout.NORTH);
        dockPanel.setRightPanelWidth(350);
        contentPane.add(dockPanel, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("MenuBar.borderColor")));
        statusBar.add(dynamicStatusBar, BorderLayout.WEST);
        statusBar.add(staticStatusBar, BorderLayout.CENTER);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        initializeStatusBar();

        pack();
        setSize(1280, 800);
    }

    private void initializeStatusBar() {

        dynamicStatusBar.setFloatable(false);
        dynamicStatusBar.setBorder(null);

        staticStatusBar.setFloatable(false);
        staticStatusBar.putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
        staticStatusBar.setBorder(null);
        staticStatusBar.add(Box.createHorizontalGlue(), new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));

        dataTypeInfoButton.setBorder(UIUtils.createEmptyBorder(3));
        staticStatusBar.add(dataTypeInfoButton);
        staticStatusBar.addSeparator();

        JIPipeDesktopRunnableQueueButton downloadQueueButton = new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench(), downloaderQueue);
        downloadQueueButton.makeFlat();
        downloadQueueButton.setReadyLabel("Data");
        downloadQueueButton.setTasksFinishedLabel("Data");
        staticStatusBar.add(downloadQueueButton);
        staticStatusBar.addSeparator();
        JIPipeDesktopRunnableQueueButton globalQueueButton = new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench());
        globalQueueButton.makeFlat();
        staticStatusBar.add(globalQueueButton);
        staticStatusBar.addSeparator();
        staticStatusBar.add(toggleFocusView);
        staticStatusBar.add(new JIPipeDesktopAlwaysOnTopToggle(this));

        toggleFocusView.setToolTipText("Hide dock panels and ribbon");
        toggleFocusView.addActionListener(e -> updateFocusView());
    }

    private void updateFocusView() {
        if (toggleFocusView.isSelected()) {
            dockPanel.setHideToolbars(true);
            ribbon.setVisible(false);
        } else {
            dockPanel.setHideToolbars(false);
            ribbon.setVisible(true);
        }
        revalidate();
        repaint(50);
    }

    private void onDataChanged() {
        updateWindowTitle();
        updateDataTypeInfoButton();

        // Select the viewer
        Class<? extends JIPipeDesktopDataViewer> viewerClass;
        if (dataBrowser != null) {
            viewerClass = JIPipe.getDataTypes().getDefaultDataViewer(dataBrowser.getDataClass());
        } else {
            viewerClass = JIPipeDesktopDefaultDataViewer.class;
        }
        if (currentDataViewer != null && currentDataViewer.getClass() != viewerClass) {
            destroyCurrentViewer();
        }

        // Create all the standard docks
        createTextAnnotationsPanel();

        // Create a new viewer if needed
        boolean hasNewViewer = false;
        if (currentDataViewer == null) {
            currentDataViewer = (JIPipeDesktopDataViewer) ReflectionUtils.newInstance(viewerClass, this);
            dockPanel.setBackgroundComponent(currentDataViewer);
            hasNewViewer = true;
        }

        rebuildUI();

        currentDataViewer.postOnDataChanged();

        // Force a full revalidate
        if (hasNewViewer) {
            revalidateDockLater();
        }
    }

    /**
     * Rebuilds the dock, ribbon, and status bar of the current data viewer
     */
    public void rebuildUI() {

        if (currentDataViewer == null) {
            return;
        }

        // Update the viewer
        currentDataViewer.preOnDataChanged();
        currentDataViewer.rebuildDock(dockPanel);

        // Update the ribbon
        rebuildRibbon();
        currentDataViewer.rebuildRibbon(ribbon);
        ribbon.rebuildRibbon();

        // Update the status bar
        dynamicStatusBar.removeAll();
        currentDataViewer.rebuildStatusBar(dynamicStatusBar);
    }

    private void updateDataTypeInfoButton() {
        if (dataBrowser != null) {
            dataTypeInfoButton.setText(dataBrowser.getDataTypeInfo().getName());
            dataTypeInfoButton.setIcon(JIPipe.getDataTypes().getIconFor(dataBrowser.getDataClass()));
        } else {
            dataTypeInfoButton.setText("No data");
            dataTypeInfoButton.setIcon(UIUtils.getIconFromResources("emblems/emblem-unavailable.png"));
        }
    }

    private void destroyCurrentViewer() {
        dockPanel.removeAllPanels();
        dockPanel.setBackgroundComponent(new JPanel());
        currentDataViewer.dispose();
        currentDataViewer = null;
    }

    private void updateWindowTitle() {
        // Update the title
        if (dataBrowser != null) {
            if (dataTableBrowser != null) {
                if (currentDataAnnotationColumn >= 0) {
                    setTitle(String.format("%d/%d %s/$%s - JIPipe",
                            currentDataRow + 1,
                            dataTableInfo.getRowCount(),
                            displayName,
                            dataTableInfo.getDataAnnotationColumns().get(currentDataAnnotationColumn)));
                } else {
                    setTitle(String.format("%d/%d %s - JIPipe",
                            currentDataRow + 1,
                            dataTableInfo.getRowCount(),
                            displayName));
                }
            } else {
                setTitle(String.format("%s - JIPipe", displayName));
            }
        } else {
            setTitle("No data - JIPipe");
        }
    }

    private void createTextAnnotationsPanel() {
        if (dataTableInfo != null) {
            if (!dockPanel.containsPanel("TEXT_ANNOTATIONS")) {
                JIPipeDesktopDataViewerTextAnnotationsPanel panel = new JIPipeDesktopDataViewerTextAnnotationsPanel(getDesktopWorkbench());
                dockPanel.addDockPanel("TEXT_ANNOTATIONS", "Annotations",
                        UIUtils.getIcon32FromResources("actions/tag.png"),
                        JIPipeDesktopDockPanel.PanelLocation.TopLeft,
                        false,
                        0,
                        panel);
            }
            JIPipeDesktopDataViewerTextAnnotationsPanel textAnnotationsPanel = dockPanel.getPanelComponent("TEXT_ANNOTATIONS",
                    JIPipeDesktopDataViewerTextAnnotationsPanel.class);
            JIPipeDataTableRowInfo row = dataTableInfo.getRow(currentDataRow);
            ResultsTableData tableData = new ResultsTableData();
            for (JIPipeTextAnnotation textAnnotation : row.getTextAnnotations()) {
                tableData.addAndModifyRow()
                        .set("Name", textAnnotation.getName())
                        .set("Value", textAnnotation.getValue())
                        .build();
            }
            textAnnotationsPanel.setCurrentData(tableData);
        }
    }

    private void rebuildRibbon() {
        ribbon.clear();
        JIPipeDesktopRibbon.Task generalTask = ribbon.addTask("General");
        if (dataTableBrowser != null) {
            JIPipeDesktopRibbon.Band dataBand = generalTask.addBand("Data");

            // Browsing through rows
            dataBand.add(new JIPipeDesktopSmallButtonRibbonAction("Previous",
                    "Go to the previous data item",
                    UIUtils.getIconFromResources("actions/caret-up.png"),
                    this::goToPreviousData));
            dataBand.add(new JIPipeDesktopSmallButtonRibbonAction(String.format("Row %d/%d", currentDataRow + 1, dataTableInfo.getRowCount()),
                    "More options",
                    UIUtils.getIconFromResources("actions/object-rows.png")));
            dataBand.add(new JIPipeDesktopSmallButtonRibbonAction("Next",
                    "Go to the next data item",
                    UIUtils.getIconFromResources("actions/caret-down.png"),
                    this::goToNextData));
        }
        if (isDisplayingLocallyCachedData()) {
            JIPipeDesktopRibbon.Band cacheBand = generalTask.addBand("Cache");
            cacheBand.addLargeMenuButton("Update cache", "Updates the cache of the node that generated the data", UIUtils.getIcon32FromResources("actions/database.png"),
                    UIUtils.createMenuItem("Update cache", "Runs the pipeline up until this algorithm and caches the results. Nothing is written to disk.", UIUtils.getIconFromResources("actions/database.png"), this::doLocalUpdateCache),
                    UIUtils.createMenuItem("Cache intermediate results", "Runs the pipeline up until this algorithm and caches the results (including intermediate results). Nothing is written to disk.", UIUtils.getIconFromResources("actions/cache-intermediate-results.png"), this::doLocalUpdateCacheIntermediateResults));
            cacheBand.add(toggleAutoRefreshFromCache);
            cacheBand.addSmallButton("Refresh", "Updates the current data with the newest in cache if possible", UIUtils.getIconFromResources("actions/view-refresh.png"), this::refreshFromLocalCache);
        }
        if (dataBrowser != null) {
            JIPipeDesktopRibbon.Task exportTask = ribbon.addTask("Export");
            JIPipeDesktopRibbon.Band dataBand = exportTask.addBand("Data");
            dataBand.add(new JIPipeDesktopLargeButtonRibbonAction("As files", "Exports the data as file(s) according to a custom-set name", UIUtils.getIcon32FromResources("actions/document-export.png"), this::exportAsFilesCustom));
            dataBand.add(new JIPipeDesktopLargeButtonRibbonAction("Into directory", "Exports the data as file(s) into a directory using JIPipe's standard naming", UIUtils.getIcon32FromResources("actions/folder-new.png"), this::exportAsFilesIntoDirectory));
            if (dataTableBrowser != null) {
                JIPipeDesktopRibbon.Band tableBand = exportTask.addBand("Data table (" + StringUtils.formatPluralS(dataTableInfo.getRowCount(), "row") + ")");
                tableBand.add(new JIPipeDesktopLargeButtonRibbonAction("As *.zip", "Exports the data table into a *.zip file", UIUtils.getIcon32FromResources("actions/document-export.png"), this::exportDataTableToZip));
                tableBand.add(new JIPipeDesktopLargeButtonRibbonAction("Into directory", "Exports the data as file(s) into a directory using JIPipe's standard naming", UIUtils.getIcon32FromResources("actions/folder-new.png"), this::exportDataTableToFolder));
            }
        }
    }

    private void doLocalUpdateCacheIntermediateResults() {
        if (isDisplayingLocallyCachedData()) {
            JIPipeGraphNode node = ((JIPipeDataSlot) dataTableBrowser.getLocalDataTable()).getNode();
            if (node != null) {
                // Generate settings
                JIPipeDesktopQuickRunSettings settings = new JIPipeDesktopQuickRunSettings(getWorkbench().getProject());
                settings.setSaveToDisk(false);
                settings.setExcludeSelected(false);
                settings.setLoadFromCache(true);
                settings.setStoreToCache(true);
                settings.setStoreIntermediateResults(true);

                // Run
                JIPipeDesktopQuickRun run = new JIPipeDesktopQuickRun(getWorkbench().getProject(), node, settings);
                JIPipeRuntimeApplicationSettings.getInstance().setDefaultQuickRunThreads(settings.getNumThreads());

                JIPipeRunnableQueue.getInstance().enqueue(run);
            }
        }
    }

    private void doLocalUpdateCache() {
        if (isDisplayingLocallyCachedData()) {
            JIPipeGraphNode node = ((JIPipeDataSlot) dataTableBrowser.getLocalDataTable()).getNode();
            if (node != null) {
                // Generate settings
                JIPipeDesktopQuickRunSettings settings = new JIPipeDesktopQuickRunSettings(getWorkbench().getProject());
                settings.setSaveToDisk(false);
                settings.setExcludeSelected(false);
                settings.setLoadFromCache(true);
                settings.setStoreToCache(true);
                settings.setStoreIntermediateResults(false);

                // Run
                JIPipeDesktopQuickRun run = new JIPipeDesktopQuickRun(getWorkbench().getProject(), node, settings);
                JIPipeRuntimeApplicationSettings.getInstance().setDefaultQuickRunThreads(settings.getNumThreads());

                JIPipeRunnableQueue.getInstance().enqueue(run);
            }
        }
    }

    private void refreshFromLocalCache() {
        if (isDisplayingLocallyCachedData()) {
            String slotName = ((JIPipeDataSlot) dataTableBrowser.getLocalDataTable()).getName();
            JIPipeGraphNode node = ((JIPipeDataSlot) dataTableBrowser.getLocalDataTable()).getNode();
            if (slotName != null && node != null) {
                Map<String, JIPipeDataTable> cacheResult = getWorkbench().getProject().getCache().query(node, node.getUUIDInParentGraph(), JIPipeProgressInfo.SILENT);
                if (cacheResult != null) {
                    JIPipeDataTable newTable = cacheResult.getOrDefault(slotName, null);
                    if (newTable != null) {
                        dataTableBrowser = new JIPipeLocalDataTableBrowser(newTable);
                        goToRow(currentDataRow, currentDataAnnotationColumn, true);
                    }
                }
            }
        }
    }

    private void exportDataTableToFolder() {
        Path path = JIPipeFileChooserApplicationSettings.saveDirectory(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export data table into directory");
        ExportDataTableIntoDirectoryRun run = new ExportDataTableIntoDirectoryRun(dataTableBrowser, path);
        JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, run);
    }

    private void exportDataTableToZip() {
        Path path = JIPipeFileChooserApplicationSettings.saveFile(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export data table", UIUtils.EXTENSION_FILTER_ZIP);
        ExportDataTableToZipRun run = new ExportDataTableToZipRun(dataTableBrowser, path);
        JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, run);
    }

    private void exportAsFilesIntoDirectory() {
        Path path = JIPipeFileChooserApplicationSettings.saveDirectory(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export data into directory");
        ExportDataIntoDirectoryRun run = new ExportDataIntoDirectoryRun(dataBrowser, path);
        JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, run);
    }

    private void exportAsFilesCustom() {
        Path path = JIPipeFileChooserApplicationSettings.saveFile(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export data");
        ExportDataWithCustomNameRun run = new ExportDataWithCustomNameRun(dataBrowser, path);
        JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, run);
    }

    private void goToRow(int row, int dataAnnotationColumn, boolean force) {
        boolean changed = false;
        if (force || row != currentDataRow || dataAnnotationColumn != currentDataAnnotationColumn) {
            this.currentDataRow = row;
            this.currentDataAnnotationColumn = dataAnnotationColumn;

            // Destroy old browser
            destroyCurrentDataBrowser();

            changed = true;
        }
        if (dataBrowser == null && dataTableBrowser != null) {
            if (currentDataAnnotationColumn >= 0) {
                dataBrowser = dataTableBrowser.browse(currentDataRow, dataTableInfo.getDataAnnotationColumns().get(currentDataAnnotationColumn));
            } else {
                dataBrowser = dataTableBrowser.browse(currentDataRow);
            }
        }
        if (changed) {
            onDataChanged();
        }
    }

    private void destroyCurrentDataBrowser() {
        if (dataBrowser != null) {
            try {
                dataBrowser.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                dataBrowser = null;
            }
        }
    }

    public JIPipeRunnableQueue getDownloaderQueue() {
        return downloaderQueue;
    }

    public JIPipeDataBrowser getDataBrowser() {
        return dataBrowser;
    }

    public JIPipeLocalDataTableBrowser getDataTableBrowser() {
        return dataTableBrowser;
    }

    public JIPipeDesktopRibbon getRibbon() {
        return ribbon;
    }

    public JIPipeDesktopDockPanel getDockPanel() {
        return dockPanel;
    }

    private void goToNextData() {
        if (dataTableInfo != null) {
            goToRow((currentDataRow + 1) % dataTableInfo.getRowCount(), currentDataAnnotationColumn, false);
        }
    }

    private void goToPreviousData() {
        int row = currentDataRow - 1;
        while (row < 0) {
            row += dataTableInfo.getRowCount();
        }
        goToRow(row, currentDataAnnotationColumn, false);
    }

    /**
     * Starts the download of the full data for the current data browser.
     * All enqueued downloads will be cancelled.
     * When the download is finished, the current data viewer is informed about this.
     */
    public void startDownloadFullData() {
        downloaderQueue.cancelAll();
        downloaderQueue.enqueue(new DownloadFullDataRun(dataBrowser, currentDataRow, currentDataAnnotationColumn));
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void browseData(JIPipeDataBrowser dataBrowser, String displayName) {
        this.dataBrowser = dataBrowser;
        this.displayName = displayName;
        this.dataTableBrowser = null;
        this.dataTableInfo = null;
        this.dataAnnotations = new ArrayList<>();
        this.currentDataRow = 0;
        this.currentDataAnnotationColumn = -1;
        onDataChanged();
    }

    public void browseDataTable(JIPipeLocalDataTableBrowser dataTableBrowser, int row, String dataAnnotation, String displayName) {
        try {
            JIPipeDataTableInfo info = dataTableBrowser.getDataTableInfo().get();
            this.dataBrowser = null;
            this.displayName = displayName;
            this.dataTableBrowser = dataTableBrowser;
            this.dataTableInfo = info;
            this.dataAnnotations = info.getDataAnnotationColumns();
            goToRow(row, dataAnnotation != null ? this.dataAnnotations.indexOf(dataAnnotation) : -1, false);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof DownloadFullDataRun) {
            DownloadFullDataRun run = (DownloadFullDataRun) event.getRun();
            if (run.getData() != null && run.getCurrentDataRow() == currentDataRow && run.getCurrentDataAnnotationColumn() == currentDataAnnotationColumn) {
                if (currentDataViewer != null) {
                    currentDataViewer.onDataDownloaded(run.getData());
                }
            }
            run.setData(null);
        }
    }

    public void revalidateDockLater() {
        Timer timer = new Timer(250, e -> {
            SwingUtilities.invokeLater(() -> {
                dockPanel.updateSizes();
            });
        });
        timer.setRepeats(false);
        timer.start();
    }

    @Override
    public void dispose() {
        super.dispose();
        destroyCurrentViewer();
        destroyCurrentDataBrowser();
        workbench.getProject().getCache().getModifiedEventEmitter().unsubscribe(this);
    }

    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
        if (isDisplayingLocallyCachedData() && toggleAutoRefreshFromCache.isSelected()) {
            refreshFromCacheDebouncer.debounce();
        }
    }

    private boolean isDisplayingLocallyCachedData() {
        return getWorkbench().getProject() != null && dataTableBrowser != null && dataTableBrowser.getLocalDataTable() instanceof JIPipeDataSlot && ((JIPipeDataSlot) dataTableBrowser.getLocalDataTable()).getNode() != null;
    }

    public static class DownloadFullDataRun extends AbstractJIPipeRunnable {
        private final JIPipeDataBrowser dataBrowser;
        private final int currentDataRow;
        private final int currentDataAnnotationColumn;
        private JIPipeData data;

        public DownloadFullDataRun(JIPipeDataBrowser dataBrowser, int currentDataRow, int currentDataAnnotationColumn) {
            this.dataBrowser = dataBrowser;
            this.currentDataRow = currentDataRow;
            this.currentDataAnnotationColumn = currentDataAnnotationColumn;
        }

        @Override
        public String getTaskLabel() {
            return "Download full data";
        }

        @Override
        public void run() {
            try {
                data = dataBrowser.getData(JIPipeData.class, getProgressInfo()).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public JIPipeData getData() {
            return data;
        }

        public void setData(JIPipeData data) {
            this.data = data;
        }

        public int getCurrentDataRow() {
            return currentDataRow;
        }

        public int getCurrentDataAnnotationColumn() {
            return currentDataAnnotationColumn;
        }
    }

    private static class ExportDataIntoDirectoryRun extends AbstractJIPipeRunnable {
        private final JIPipeDataBrowser dataBrowser;
        private final Path path;

        public ExportDataIntoDirectoryRun(JIPipeDataBrowser dataBrowser, Path path) {
            this.dataBrowser = dataBrowser;
            this.path = path;
        }

        @Override
        public String getTaskLabel() {
            return "Export data into directory";
        }

        @Override
        public void run() {
            PathUtils.createDirectories(path);
            try {
                JIPipeData data = dataBrowser.getData(JIPipeData.class, getProgressInfo().resolve("Download data")).get();
                data.exportData(new JIPipeFileSystemWriteDataStorage(getProgressInfo(), path), "data", false, getProgressInfo().resolve("Export"));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class ExportDataWithCustomNameRun extends AbstractJIPipeRunnable {
        private final JIPipeDataBrowser dataBrowser;
        private final Path path;

        public ExportDataWithCustomNameRun(JIPipeDataBrowser dataBrowser, Path path) {
            this.dataBrowser = dataBrowser;
            this.path = path;
        }

        @Override
        public String getTaskLabel() {
            return "Export data into directory";
        }

        @Override
        public void run() {
            PathUtils.createDirectories(path.getParent());
            try {
                JIPipeData data = dataBrowser.getData(JIPipeData.class, getProgressInfo().resolve("Download data")).get();
                data.exportData(new JIPipeFileSystemWriteDataStorage(getProgressInfo(), path.getParent()), path.getFileName().toString(), false, getProgressInfo().resolve("Export"));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class ExportDataTableIntoDirectoryRun extends AbstractJIPipeRunnable {
        private final JIPipeDataTableBrowser dataTableBrowser;
        private final Path path;

        public ExportDataTableIntoDirectoryRun(JIPipeDataTableBrowser dataTableBrowser, Path path) {
            this.dataTableBrowser = dataTableBrowser;
            this.path = path;
        }

        @Override
        public String getTaskLabel() {
            return "Export data table into directory";
        }

        @Override
        public void run() {
            PathUtils.createDirectories(path);
            try {
                JIPipeDataTable dataTable = dataTableBrowser.getDataTable(getProgressInfo().resolve("Download data table")).get();
                dataTable.exportData(new JIPipeFileSystemWriteDataStorage(getProgressInfo(), path), "data", false, getProgressInfo().resolve("Export"));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class ExportDataTableToZipRun extends AbstractJIPipeRunnable {
        private final JIPipeDataTableBrowser dataTableBrowser;
        private final Path path;

        public ExportDataTableToZipRun(JIPipeDataTableBrowser dataTableBrowser, Path path) {
            this.dataTableBrowser = dataTableBrowser;
            this.path = path;
        }

        @Override
        public String getTaskLabel() {
            return "Export data table to ZIP";
        }

        @Override
        public void run() {
            PathUtils.createDirectories(path.getParent());
            try (JIPipeZIPWriteDataStorage storage = new JIPipeZIPWriteDataStorage(getProgressInfo().resolve("Storage"), path)) {
                JIPipeDataTable dataTable = dataTableBrowser.getDataTable(getProgressInfo().resolve("Download data table")).get();
                dataTable.exportData(storage, "data", false, getProgressInfo().resolve("Export"));
            } catch (IOException | InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
