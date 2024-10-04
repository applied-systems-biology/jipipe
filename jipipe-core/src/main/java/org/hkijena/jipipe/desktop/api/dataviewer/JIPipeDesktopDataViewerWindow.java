package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.browser.JIPipeDataBrowser;
import org.hkijena.jipipe.api.data.browser.JIPipeLocalDataTableBrowser;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Window that displays data using {@link JIPipeDesktopDataViewer}
 */
public class JIPipeDesktopDataViewerWindow extends JFrame implements JIPipeDesktopWorkbenchAccess, Disposable {
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

    public JIPipeDesktopDataViewerWindow(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        onDataChanged();
    }

    private void initialize() {
        setIconImage(UIUtils.getJIPipeIcon128());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(ribbon, BorderLayout.NORTH);
        getContentPane().add(dockPanel, BorderLayout.CENTER);
        pack();
        setSize(1280, 800);
    }

    private void onDataChanged() {
        updateWindowTitle();

        // Select the viewer
        Class<? extends JIPipeDesktopDataViewer> viewerClass = JIPipeDesktopDefaultDataViewer.class;
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
            if(dataBrowser != null) {
                try {
                    dataBrowser.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

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
}
