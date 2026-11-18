package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
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
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.commandbar.JIPipeDesktopCommandBar;
import org.hkijena.jipipe.desktop.app.datatracer.JIPipeDesktopDataTracerUI;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.app.running.queue.JIPipeDesktopCompactRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallToggleButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.window.JIPipeDesktopAlwaysOnTopToggle;
import org.hkijena.jipipe.plugins.cef.JIPipeCefClientService;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.*;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Window that displays data using {@link JIPipeDesktopDataViewer}
 */
public class JIPipeDesktopDataViewerWindow extends JFrame implements JIPipeDesktopWorkbenchAccess, Disposable, JIPipeRunnable.FinishedEventListener, JIPipeCache.ModifiedEventListener {

    private static boolean HOTKEYS_ENABLED = false;

    /**
     * All data viewer windows that are currently open. Mirrors the
     * lifecycle of {@code JIPipeDesktopProjectWindow.OPEN_WINDOWS}: added
     * in the constructor, removed at the start of {@link #dispose()}
     */
    private static final Set<JIPipeDesktopDataViewerWindow> OPEN_WINDOWS = new HashSet<>();

    private final JIPipeQueuedRunnableExecutor downloaderQueue = new JIPipeQueuedRunnableExecutor("Data download");

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeDesktopRibbon ribbon = new JIPipeDesktopRibbon();
    private final JIPipeDesktopDockPanel dockPanel = new JIPipeDesktopDockPanel();
    private final JPanel contentPane = new JPanel(new BorderLayout());
    private final JToolBar staticStatusBar = new JToolBar();
    private final JToolBar dynamicStatusBar = new JToolBar();
    private final JButton dataTypeInfoButton = new JButton();
    private final JToggleButton toggleFocusView = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/view-fullscreen.png"));
    private final JIPipeDesktopSmallToggleButtonRibbonAction toggleAutoRefreshFromCache;
    private final StaticDebouncer refreshFromCacheDebouncer;
    private final JIPipeCefClientService cefClientService;
    /**
     * The per-window command bar (issue #1346, Task 6). Created in the
     * constructor (the shortcut dispatcher is installed globally at desktop
     * startup, so no per-window dispatcher install happens here) and
     * uninstalled in {@link #dispose()} before the content teardown.
     */
    private final JIPipeDesktopCommandBar commandBar;
    private JIPipeDataBrowser dataBrowser;
    private String displayName;
    private JIPipeLocalDataTableBrowser dataTableBrowser;
    private JIPipeDataTableInfo dataTableInfo;
    private List<String> dataAnnotations;
    private int currentDataRow = -1;
    private int currentDataAnnotationColumn = -1;
    private JIPipeDesktopDataViewer currentDataViewer;
    /**
     * Stable per-window identity used by the automation operations to target a
     * specific viewer window (the viewer_id in their parameters and results).
     */
    private final String id = UUID.randomUUID().toString();
    /**
     * Whether connected AI agents may navigate and close this window. Default off;
     * the user toggles it via {@link #toggleAgentControl} in the ribbon, and the
     * open_data_viewer automation operation turns it on for windows it opens. Plain
     * window state, never persisted.
     */
    private boolean agentControlled;
    /**
     * The ribbon toggle of {@link #agentControlled}. One instance per window that is
     * re-added to the Data band on every ribbon rebuild (same pattern as
     * {@link #toggleAutoRefreshFromCache}).
     */
    private final JIPipeDesktopSmallToggleButtonRibbonAction toggleAgentControl;

    public JIPipeDesktopDataViewerWindow(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        this.cefClientService = new JIPipeCefClientService(this);
        this.toggleAutoRefreshFromCache = new JIPipeDesktopSmallToggleButtonRibbonAction("Auto-refresh", "If enabled, automatically update the displayed data when the cache changes", JIPipe.RESOURCES.getIcon16("actions/view-refresh.png"), true, (button) -> {
            if (button.isSelected()) {
                refreshFromLocalCache();
            }
        });
        this.toggleAgentControl = new JIPipeDesktopSmallToggleButtonRibbonAction("Agent control",
                "Agent control: allow connected AI agents to navigate and close this window",
                JIPipe.RESOURCES.getIcon16("actions/ai.png"), agentControlled,
                (button) -> this.agentControlled = button.isSelected());
        this.refreshFromCacheDebouncer = new StaticDebouncer(1500, this::refreshFromLocalCache);
        OPEN_WINDOWS.add(this);
        initialize();
        registerHotkeys();
        // The per-window command bar (issue #1346, Task 6). The shortcut
        // dispatcher is installed once at desktop startup (idempotent), so
        // the bar only needs its own per-window install here, after the root
        // pane exists.
        this.commandBar = new JIPipeDesktopCommandBar();
        this.commandBar.install(this);

        downloaderQueue.getFinishedEventEmitter().subscribe(this);

        // Listen to local cache
        if (workbench.getProject() != null) {
            workbench.getProject().getCache().getModifiedEventEmitter().subscribe(this);
        }

        onDataChanged();
    }

    /**
     * @return All open data viewer windows
     */
    public static Set<JIPipeDesktopDataViewerWindow> getOpenWindows() {
        return Collections.unmodifiableSet(OPEN_WINDOWS);
    }

    private void registerHotkeys() {
        if (!HOTKEYS_ENABLED) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
                if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() instanceof JIPipeDesktopDataViewerWindow) {
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
        statusBar.setOpaque(true);
        statusBar.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        statusBar.setBorder(null);
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
        dynamicStatusBar.setOpaque(false);

        staticStatusBar.setFloatable(false);
        staticStatusBar.setOpaque(false);
        staticStatusBar.putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
        staticStatusBar.setBorder(null);
        staticStatusBar.add(Box.createHorizontalGlue(), new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));

        dataTypeInfoButton.setBorder(UIUtils.createEmptyBorder(3));
        dataTypeInfoButton.setOpaque(false);
        staticStatusBar.add(dataTypeInfoButton);
        staticStatusBar.addSeparator();

        staticStatusBar.add(new JIPipeDesktopCompactRunnableQueueButton(getDesktopWorkbench(), downloaderQueue, "actions/view-web-browser-dom-tree.png"));
        staticStatusBar.add(new JIPipeDesktopCompactRunnableQueueButton(getDesktopWorkbench(), "actions/run-build.png"));
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
            dockPanel.setMainComponent(currentDataViewer);
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
            dataTypeInfoButton.setIcon(JIPipe.RESOURCES.getIcon16("emblems/emblem-unavailable.png"));
        }
    }

    private void destroyCurrentViewer() {
        dockPanel.removeAllPanels();
        dockPanel.setMainComponent(new JPanel());
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
                        JIPipe.RESOURCES.getIcon24("actions/tag.png"),
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
                    JIPipe.RESOURCES.getIcon16("actions/caret-up.png"),
                    this::goToPreviousData));
            dataBand.add(new JIPipeDesktopSmallButtonRibbonAction(String.format("Row %d/%d", currentDataRow + 1, dataTableInfo.getRowCount()),
                    "More options",
                    JIPipe.RESOURCES.getIcon16("actions/object-rows.png")));
            dataBand.add(new JIPipeDesktopSmallButtonRibbonAction("Next",
                    "Go to the next data item",
                    JIPipe.RESOURCES.getIcon16("actions/caret-down.png"),
                    this::goToNextData));
            dataBand.add(toggleAgentControl);
        }
        if (isDisplayingLocallyCachedData()) {
            JIPipeDesktopRibbon.Band cacheBand = generalTask.addBand("Cache");
            cacheBand.addLargeMenuButton("Update cache", "Updates the cache of the node that generated the data", JIPipe.RESOURCES.getIcon32("actions/database.png"),
                    UIUtils.createMenuItem("Update cache", "Runs the pipeline up until this algorithm and caches the results. Nothing is written to disk.", JIPipe.RESOURCES.getIcon16("actions/database.png"), this::doLocalUpdateCache),
                    UIUtils.createMenuItem("Cache intermediate results", "Runs the pipeline up until this algorithm and caches the results (including intermediate results). Nothing is written to disk.", JIPipe.RESOURCES.getIcon16("actions/cache-intermediate-results.png"), this::doLocalUpdateCacheIntermediateResults));
            cacheBand.addLargeButton("Trace", "Shows all predecessors of this data if available. Requires an unbroken chain of data (i.e. use Cache intermediate results)", JIPipe.RESOURCES.getIcon32("actions/footsteps.png"), this::traceData);
            cacheBand.add(toggleAutoRefreshFromCache);
            cacheBand.addSmallButton("Refresh", "Updates the current data with the newest in cache if possible", JIPipe.RESOURCES.getIcon16("actions/view-refresh.png"), this::refreshFromLocalCache);

        }
        if (dataBrowser != null) {
            JIPipeDesktopRibbon.Task exportTask = ribbon.addTask("Export");
            JIPipeDesktopRibbon.Band dataBand = exportTask.addBand("Data");
            dataBand.add(new JIPipeDesktopLargeButtonRibbonAction("As files", "Exports the data as file(s) according to a custom-set name", JIPipe.RESOURCES.getIcon32("actions/document-export.png"), this::exportAsFilesCustom));
            dataBand.add(new JIPipeDesktopLargeButtonRibbonAction("Into directory", "Exports the data as file(s) into a directory using JIPipe's standard naming", JIPipe.RESOURCES.getIcon32("actions/folder-new.png"), this::exportAsFilesIntoDirectory));
            if (dataTableBrowser != null) {
                JIPipeDesktopRibbon.Band tableBand = exportTask.addBand("Data table (" + StringUtils.formatPluralS(dataTableInfo.getRowCount(), "row") + ")");
                tableBand.add(new JIPipeDesktopLargeButtonRibbonAction("As *.zip", "Exports the data table into a *.zip file", JIPipe.RESOURCES.getIcon32("actions/document-export.png"), this::exportDataTableToZip));
                tableBand.add(new JIPipeDesktopLargeButtonRibbonAction("Into directory", "Exports the data as file(s) into a directory using JIPipe's standard naming", JIPipe.RESOURCES.getIcon32("actions/folder-new.png"), this::exportDataTableToFolder));
            }
        }
    }

    private void traceData() {
        if (isDisplayingLocallyCachedData()) {
            JIPipeDataContext dataContext = dataTableBrowser.getLocalDataTable().getDataContext(currentDataRow);
            if (dataContext != null) {
                JIPipeDesktopDataTracerUI.openWindow((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(), dataContext.getId());
            } else {
                UIUtils.showErrorDialog(getDesktopWorkbench(), this, new NullPointerException("No data trace could be found!"));
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

                JIPipeQueuedRunnableExecutor.getInstance().enqueue(run);
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

                JIPipeQueuedRunnableExecutor.getInstance().enqueue(run);
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
        Path path = JIPipeDesktop.saveDirectory(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export data table into directory", HTMLText.EMPTY);
        ExportDataTableIntoDirectoryRun run = new ExportDataTableIntoDirectoryRun(dataTableBrowser, path);
        JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, run);
    }

    private void exportDataTableToZip() {
        Path path = JIPipeDesktop.saveFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export data table", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_ZIP);
        ExportDataTableToZipRun run = new ExportDataTableToZipRun(dataTableBrowser, path);
        JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, run);
    }

    private void exportAsFilesIntoDirectory() {
        Path path = JIPipeDesktop.saveDirectory(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export data into directory", HTMLText.EMPTY);
        ExportDataIntoDirectoryRun run = new ExportDataIntoDirectoryRun(dataBrowser, path);
        JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, run);
    }

    private void exportAsFilesCustom() {
        Path path = JIPipeDesktop.saveFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export data", HTMLText.EMPTY);
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

    /**
     * Navigates this window to a 1-based data table row, keeping the current data
     * annotation column. The agent-facing navigation entry point (used by the
     * goto_viewer_row automation operation) around the private {@link #goToRow}.
     * Returns false when the window does not display a data table or the row is out
     * of range; callers that need the distinction check {@link #getDataTableRowCount()}
     * first.
     */
    public boolean navigateToRow(int oneBasedRow) {
        if (dataTableInfo == null || dataTableInfo.getRowCount() == 0) {
            return false;
        }
        int row = oneBasedRow - 1;
        if (row < 0 || row >= dataTableInfo.getRowCount()) {
            return false;
        }
        goToRow(row, currentDataAnnotationColumn, false);
        return true;
    }

    public JIPipeQueuedRunnableExecutor getDownloaderQueue() {
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

    /**
     * @return The stable id of this window (the viewer_id the automation operations use)
     */
    public String getId() {
        return id;
    }

    /**
     * @return Whether connected AI agents may navigate and close this window
     */
    public boolean isAgentControlled() {
        return agentControlled;
    }

    /**
     * Sets whether connected AI agents may navigate and close this window. Keeps the
     * ribbon toggle in sync (the toggle is shown once the window displays a data table).
     */
    public void setAgentControlled(boolean agentControlled) {
        this.agentControlled = agentControlled;
        toggleAgentControl.getButton().setSelected(agentControlled);
    }

    /**
     * @return The 0-based index of the currently displayed data table row, or -1 when
     * the window does not display a data table
     */
    public int getCurrentDataRow() {
        return currentDataRow;
    }

    /**
     * @return The row count of the displayed data table, or 0 when the window does not
     * display a data table
     */
    public int getDataTableRowCount() {
        return dataTableInfo != null ? dataTableInfo.getRowCount() : 0;
    }

    /**
     * @return The name of the currently displayed data annotation column, or null when
     * no annotation column is selected
     */
    public String getCurrentDataAnnotationColumn() {
        if (dataTableInfo == null || currentDataAnnotationColumn < 0
                || currentDataAnnotationColumn >= dataTableInfo.getDataAnnotationColumns().size()) {
            return null;
        }
        return dataTableInfo.getDataAnnotationColumns().get(currentDataAnnotationColumn);
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
        // Mirror of JIPipeDesktopProjectWindow#dispose: the window leaves
        // the open set before its content is torn down
        OPEN_WINDOWS.remove(this);
        // Uninstall the per-window command bar (issue #1346, Task 6) before
        // the content teardown: its uninstall closes the overlay
        // defensively (a disposed bar must never leave the shortcut dispatch
        // suspended) and detaches the trigger scope from this window's root
        // pane.
        commandBar.uninstall();
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

    public JIPipeCefClientService getCefClientService() {
        return cefClientService;
    }

    public static class DownloadFullDataRun extends DefaultJIPipeRunnable {
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

    private static class ExportDataIntoDirectoryRun extends DefaultJIPipeRunnable {
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

    private static class ExportDataWithCustomNameRun extends DefaultJIPipeRunnable {
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

    private static class ExportDataTableIntoDirectoryRun extends DefaultJIPipeRunnable {
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

    private static class ExportDataTableToZipRun extends DefaultJIPipeRunnable {
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
