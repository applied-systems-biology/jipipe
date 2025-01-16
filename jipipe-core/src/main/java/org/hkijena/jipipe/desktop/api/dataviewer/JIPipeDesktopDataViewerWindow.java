package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.browser.JIPipeDataBrowser;
import org.hkijena.jipipe.api.data.browser.JIPipeDataTableBrowser;
import org.hkijena.jipipe.api.data.browser.JIPipeLocalDataTableBrowser;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.window.JIPipeDesktopAlwaysOnTopToggle;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Window that displays data using {@link JIPipeDesktopDataViewer}
 */
public class JIPipeDesktopDataViewerWindow extends JFrame implements JIPipeDesktopWorkbenchAccess, Disposable, JIPipeRunnable.FinishedEventListener {

    private final JIPipeRunnableQueue downloaderQueue = new JIPipeRunnableQueue("Data download");

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeDesktopRibbon ribbon = new JIPipeDesktopRibbon();
    private final JIPipeDesktopDockPanel dockPanel = new JIPipeDesktopDockPanel();
    private JIPipeDataBrowser dataBrowser;
    private String displayName;
    private JIPipeLocalDataTableBrowser dataTableBrowser;
    private JIPipeDataTableInfo dataTableInfo;
    private List<String> dataAnnotations;
    private int currentDataRow = -1;
    private int currentDataAnnotationColumn = -1;
    private JIPipeDesktopDataViewer currentDataViewer;
    private final JPanel contentPane = new JPanel(new BorderLayout());
    private final JToolBar staticStatusBar = new JToolBar();
    private final JToolBar dynamicStatusBar = new JToolBar();
    private final JButton dataTypeInfoButton = new JButton();

    public JIPipeDesktopDataViewerWindow(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        registerHotkeys();

        downloaderQueue.getFinishedEventEmitter().subscribe(this);

        onDataChanged();
    }

    private void registerHotkeys() {
//        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
//        ActionMap actionMap = getRootPane().getActionMap();
//        setFocusTraversalKeysEnabled(false);
//
//        actionMap.put("next-row", new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                goToNextData();
//            }
//        });
//        actionMap.put("previous-row", new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                goToPreviousData();
//            }
//        });
//
//        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), "previous-row");
//        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "next-row");

        // Register a KeyEventDispatcher for global shortcuts
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                    goToNextData();
                    return true; // Consume the event
                }
                if (e.getKeyCode() == KeyEvent.VK_UP && (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                    goToPreviousData();
                    return true; // Consume the event
                }
            }
            return false; // Let the event propagate otherwise
        });
    }

    private void initialize() {
        setIconImage(UIUtils.getJIPipeIcon128());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(contentPane);
        contentPane.add(ribbon, BorderLayout.NORTH);
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
        staticStatusBar.add(new JIPipeDesktopAlwaysOnTopToggle(this));
    }

    private void onDataChanged() {
        updateWindowTitle();
        updateDataTypeInfoButton();

        // Select the viewer
        Class<? extends JIPipeDesktopDataViewer> viewerClass;
        if(dataBrowser != null) {
            viewerClass = JIPipe.getDataTypes().getDefaultDataViewer(dataBrowser.getDataClass());
        }
        else {
            viewerClass = JIPipeDesktopDefaultDataViewer.class;
        }
        if (currentDataViewer != null && currentDataViewer.getClass() != viewerClass) {
            destroyCurrentViewer();
        }

        // Create all the standard docks
        createTextAnnotationsPanel();

        // Create a new viewer if needed
        if (currentDataViewer == null) {
            currentDataViewer = (JIPipeDesktopDataViewer) ReflectionUtils.newInstance(viewerClass, this);
            dockPanel.setBackgroundComponent(currentDataViewer);
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

        currentDataViewer.postOnDataChanged();
    }

    private void updateDataTypeInfoButton() {
        if(dataBrowser != null) {
            dataTypeInfoButton.setText(dataBrowser.getDataTypeInfo().getName());
            dataTypeInfoButton.setIcon(JIPipe.getDataTypes().getIconFor(dataBrowser.getDataClass()));
        }
        else {
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
        if(dataBrowser != null) {
            JIPipeDesktopRibbon.Task exportTask = ribbon.addTask("Export");
            JIPipeDesktopRibbon.Band dataBand = exportTask.addBand("Data");
            dataBand.add(new JIPipeDesktopLargeButtonRibbonAction("As files", "Exports the data as file(s) according to a custom-set name", UIUtils.getIcon32FromResources("actions/document-export.png"), this::exportAsFilesCustom));
            dataBand.add(new JIPipeDesktopLargeButtonRibbonAction("Into directory", "Exports the data as file(s) into a directory using JIPipe's standard naming", UIUtils.getIcon32FromResources("actions/folder-new.png"), this::exportAsFilesIntoDirectory));
            if(dataTableBrowser != null) {
                JIPipeDesktopRibbon.Band tableBand = exportTask.addBand("Data table (" + StringUtils.formatPluralS(dataTableInfo.getRowCount(), "row") + ")");
                tableBand.add(new JIPipeDesktopLargeButtonRibbonAction("As *.zip", "Exports the data table into a *.zip file", UIUtils.getIcon32FromResources("actions/document-export.png"), this::exportDataTableToZip));
                tableBand.add(new JIPipeDesktopLargeButtonRibbonAction("Into directory", "Exports the data as file(s) into a directory using JIPipe's standard naming", UIUtils.getIcon32FromResources("actions/folder-new.png"), this::exportDataTableToFolder));
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

    private void goToRow(int row, int dataAnnotationColumn) {
        boolean changed = false;
        if(row != currentDataRow || dataAnnotationColumn != currentDataAnnotationColumn) {
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
        if(changed) {
            onDataChanged();
        }
    }

    private void destroyCurrentDataBrowser() {
        if(dataBrowser != null) {
            try {
                dataBrowser.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            finally {
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
            goToRow((currentDataRow + 1) % dataTableInfo.getRowCount(), currentDataAnnotationColumn);
        }
    }

    private void goToPreviousData() {
        int row = currentDataRow - 1;
        while(row < 0) {
            row += dataTableInfo.getRowCount();
        }
        goToRow(row, currentDataAnnotationColumn);
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
            goToRow(row, dataAnnotation != null ? this.dataAnnotations.indexOf(dataAnnotation) : -1);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if(event.getRun() instanceof DownloadFullDataRun) {
            DownloadFullDataRun run = (DownloadFullDataRun) event.getRun();
            if(run.getData() != null && run.getCurrentDataRow() == currentDataRow && run.getCurrentDataAnnotationColumn() == currentDataAnnotationColumn) {
                if(currentDataViewer != null) {
                    currentDataViewer.onDataDownloaded(run.getData());
                }
            }
            run.setData(null);
        }
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

    private static class ExportDataIntoDirectoryRun extends AbstractJIPipeRunnable{
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

    private static class ExportDataWithCustomNameRun extends AbstractJIPipeRunnable{
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

    private static class ExportDataTableIntoDirectoryRun extends AbstractJIPipeRunnable{
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

    private static class ExportDataTableToZipRun extends AbstractJIPipeRunnable{
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
