package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.browser.JIPipeDataBrowser;
import org.hkijena.jipipe.api.data.browser.JIPipeLocalDataTableBrowser;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.window.JIPipeDesktopAlwaysOnTopToggle;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ReflectionUtils;
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
    private final JToolBar statusBar = new JToolBar();

    public JIPipeDesktopDataViewerWindow(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        registerHotkeys();

        downloaderQueue.getFinishedEventEmitter().subscribe(this);

        onDataChanged();
    }

    private void registerHotkeys() {
        InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = contentPane.getActionMap();

        actionMap.put("next-row", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToNextData();
            }
        });
        actionMap.put("previous-row", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToPreviousData();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), "previous-row");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "next-row");
    }

    private void initialize() {
        setIconImage(UIUtils.getJIPipeIcon128());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(contentPane);
        contentPane.add(ribbon, BorderLayout.NORTH);
        contentPane.add(dockPanel, BorderLayout.CENTER);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        initializeStatusBar();

        pack();
        setSize(1280, 800);
    }

    private void initializeStatusBar() {
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("MenuBar.borderColor")));
        statusBar.setFloatable(false);
        statusBar.putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
        statusBar.add(Box.createHorizontalGlue(), new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));

        JIPipeDesktopRunnableQueueButton downloadQueueButton = new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench(), downloaderQueue);
        downloadQueueButton.makeFlat();
        downloadQueueButton.setReadyLabel("Data");
        downloadQueueButton.setTasksFinishedLabel("Data");
        statusBar.add(downloadQueueButton);
        statusBar.addSeparator();
        JIPipeDesktopRunnableQueueButton globalQueueButton = new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench());
        globalQueueButton.makeFlat();
        statusBar.add(globalQueueButton);
        statusBar.addSeparator();
        statusBar.add(new JIPipeDesktopAlwaysOnTopToggle(this));
    }

    private void onDataChanged() {
        updateWindowTitle();

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
        currentDataViewer.postOnDataChanged();
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
            JIPipeDesktopDataViewerTextAnnotationsPanel textAnnotationsPanel = dockPanel.getPanel("TEXT_ANNOTATIONS",
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

        ribbon.rebuildRibbon();

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
                data = dataBrowser.getData(getProgressInfo()).get();
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
}
