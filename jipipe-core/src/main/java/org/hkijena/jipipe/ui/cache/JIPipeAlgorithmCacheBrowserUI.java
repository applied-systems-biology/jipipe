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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.cache.JIPipeCacheClearOutdatedRun;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.exporters.JIPipeDataTableToOutputExporterRun;
import org.hkijena.jipipe.ui.cache.importers.JIPipeImportCachedSlotOutputRun;
import org.hkijena.jipipe.ui.cache.renderers.CachedOutputDataSlotListCellRenderer;
import org.hkijena.jipipe.ui.components.ribbon.LargeButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.Ribbon;
import org.hkijena.jipipe.ui.components.ribbon.SmallButtonAction;
import org.hkijena.jipipe.ui.datatable.JIPipeExtendedDataTableUI;
import org.hkijena.jipipe.ui.datatable.JIPipeExtendedMultiDataTableUI;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.actions.UpdateCacheAction;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.ui.quickrun.QuickRun;
import org.hkijena.jipipe.ui.quickrun.QuickRunSettings;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.WeakStore;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UI around an {@link JIPipeGraphRun} result
 */
public class JIPipeAlgorithmCacheBrowserUI extends JIPipeProjectWorkbenchPanel implements JIPipeCache.ModifiedEventListener, JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.FinishedEventListener {
    private final JIPipeGraphNode graphNode;
    private final JIPipeGraphCanvasUI graphCanvasUI;
    private JIPipeDataSlot selectedSlot;
    private Component currentContent;

    /**
     * @param workbenchUI   the workbench
     * @param graphNode     the node
     * @param graphCanvasUI can be null
     */
    public JIPipeAlgorithmCacheBrowserUI(JIPipeProjectWorkbench workbenchUI, JIPipeGraphNode graphNode, JIPipeGraphCanvasUI graphCanvasUI) {
        super(workbenchUI);
        this.graphNode = graphNode;
        this.graphCanvasUI = graphCanvasUI;
        initialize();

        getProject().getCache().getModifiedEventEmitter().subscribeWeak(this);
        JIPipeRunnerQueue.getInstance().getInterruptedEventEmitter().subscribeWeak(this);
        JIPipeRunnerQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);

        // Show all data slots
        refreshTable();
    }

    private void initialize() {
        setLayout(new BorderLayout());
    }

    public void refreshTable() {
        List<JIPipeDataTable> slotsToDisplay = new ArrayList<>();
        Map<String, JIPipeDataTable> slotMap = getProject().getCache().query(graphNode, graphNode.getUUIDInParentGraph(), new JIPipeProgressInfo());
        if (slotMap != null) {
            if (selectedSlot == null) {
                slotsToDisplay.addAll(getSortedDataTables(slotMap));
            } else {
                JIPipeDataTable cachedSlot = slotMap.getOrDefault(selectedSlot.getName(), null);
                if (cachedSlot != null) {
                    slotsToDisplay.add(cachedSlot);
                }
            }
        }
        if (slotsToDisplay.size() == 1) {
            JIPipeDataTable first = slotsToDisplay.iterator().next();
            showDataSlot(first);
        } else {
            showDataSlots(slotsToDisplay);
        }
    }

    private List<JIPipeDataTable> getSortedDataTables(Map<String, JIPipeDataTable> slotMap) {
        List<JIPipeDataTable> result = new ArrayList<>();
        for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
            JIPipeDataTable dataTable = slotMap.getOrDefault(outputSlot.getName(), null);
            if (dataTable != null) {
                result.add(dataTable);
            }
        }
        return result;
    }

    private void showDataSlots(List<JIPipeDataTable> dataTables) {
        if (currentContent != null) {
            remove(currentContent);
        }
        JIPipeExtendedMultiDataTableUI ui = new JIPipeExtendedMultiDataTableUI(getProjectWorkbench(), dataTables.stream().map(WeakStore::new).collect(Collectors.toList()), false);
        initializeDataTableAdditionalRibbon(ui.getRibbon());
        add(ui, BorderLayout.CENTER);
        currentContent = ui;
        revalidate();
    }

    private void showDataSlot(JIPipeDataTable dataTable) {
        if (currentContent != null) {
            remove(currentContent);
        }
        JIPipeExtendedDataTableUI ui = new JIPipeExtendedDataTableUI(getProjectWorkbench(), new WeakStore<>(dataTable), true);
        initializeDataTableAdditionalRibbon(ui.getRibbon());
//        initializeTableMenu(ui.getMenuManager());
        add(ui, BorderLayout.CENTER);
        currentContent = ui;
        revalidate();
    }

    private void initializeDataTableAdditionalRibbon(Ribbon ribbon) {
        Ribbon.Task cacheTask = ribbon.getOrCreateTask("Cache");
        Ribbon.Task exportTask = ribbon.getOrCreateTask("Export");
        exportTask.setLabel("Import/Export");

        // Cache task
        Ribbon.Band resultsBand = cacheTask.addBand("Displayed results");
        JComboBox<JIPipeDataSlot> slotSelection = new JComboBox<>();

        initializeSlotSelectionComboBox(slotSelection);

        resultsBand.add(new Ribbon.Action(Arrays.asList(new JLabel("Data slot"), Box.createHorizontalStrut(8), slotSelection), 1, new Insets(2, 2, 2, 2)));

        Ribbon.Band cacheUpdateBand = cacheTask.addBand("Update");
        LargeButtonAction updateCacheAction = new LargeButtonAction("Update cache", "Updates the cache. Intermediate results are not stored", UIUtils.getIcon32FromResources("actions/update-cache.png"), () -> {
        });
        JPopupMenu updateCacheMenu = UIUtils.addPopupMenuToButton(updateCacheAction.getButton());
        JMenuItem updateCacheMenuItem = new JMenuItem("Update cache", UIUtils.getIcon16FromResources("actions/update-cache.png"));
        updateCacheMenuItem.addActionListener(e -> updateCache(false));
        updateCacheMenu.add(updateCacheMenuItem);
        JMenuItem cacheIntermediateResultsItem = new JMenuItem("Cache intermediate results", UIUtils.getIcon16FromResources("actions/cache-intermediate-results.png"));
        cacheIntermediateResultsItem.addActionListener(e -> updateCache(true));
        updateCacheMenu.add(cacheIntermediateResultsItem);
        cacheUpdateBand.add(updateCacheAction);

        Ribbon.Band cacheManageBand = cacheTask.addBand("Data");
        cacheManageBand.add(new SmallButtonAction("Clear all", "Clears all cached data of this node", UIUtils.getIconFromResources("actions/clear-brush.png"), () -> getProject().getCache().clearAll(this.graphNode.getUUIDInParentGraph(), false, new JIPipeProgressInfo())));
        cacheManageBand.add(new SmallButtonAction("Clear outdated", "Clears all cached data of this node that was not generated with the current parameters", UIUtils.getIconFromResources("actions/document-open-recent.png"), () -> JIPipeRunnerQueue.getInstance().enqueue(new JIPipeCacheClearOutdatedRun(getProject().getCache()))));

        // Export task
        Ribbon.Band exportCacheBand = exportTask.addBand("Cache");
        exportCacheBand.add(new LargeButtonAction("Import", "Imports the whole cache from a directory", UIUtils.getIcon32FromResources("actions/document-import.png"), this::importCache));
        exportCacheBand.add(new LargeButtonAction("Export", "Exports the whole cache into a directory", UIUtils.getIcon32FromResources("actions/document-export.png"), this::exportCache));

        ribbon.reorderTasks(Collections.singletonList("Cache")); // Will move the cache up
        ribbon.rebuildRibbon();
    }

    private void initializeSlotSelectionComboBox(JComboBox<JIPipeDataSlot> slotSelection) {
        slotSelection.setRenderer(new CachedOutputDataSlotListCellRenderer());
        DefaultComboBoxModel<JIPipeDataSlot> model = new DefaultComboBoxModel<>();
        model.addElement(null);
        for (JIPipeOutputDataSlot slot : graphNode.getOutputSlots()) {
            model.addElement(slot);
        }
        slotSelection.setModel(model);
        slotSelection.setSelectedItem(selectedSlot);
        slotSelection.addActionListener(e -> {
            selectedSlot = (JIPipeDataSlot) slotSelection.getSelectedItem();
            refreshTable();
        });
    }

    private void updateCache(boolean storeIntermediateResults) {
        if (graphCanvasUI != null) {
            JIPipeGraphNodeUI ui = graphCanvasUI.getNodeUIs().getOrDefault(graphNode, null);
            if (ui != null) {
                // Same event as triggered by any other canvas tool
                ui.getNodeUIActionRequestedEventEmitter().emit(new JIPipeGraphNodeUI.NodeUIActionRequestedEvent(ui, new UpdateCacheAction(storeIntermediateResults, false)));
                return;
            }
        }

        // The backup case
        QuickRunSettings settings = new QuickRunSettings();
        settings.setLoadFromCache(true);
        settings.setStoreIntermediateResults(storeIntermediateResults);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        QuickRun testBench = new QuickRun(getProject(), graphNode, settings);
        JIPipeRunnerQueue.getInstance().enqueue(testBench);
    }

    private void exportCache() {
        Map<String, JIPipeDataTable> slotMap = getProject().getCache().query(graphNode, graphNode.getUUIDInParentGraph(), new JIPipeProgressInfo());
        if (slotMap == null || slotMap.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There is no cached data to export!", "Export cache", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Path outputFolder = FileChooserSettings.saveDirectory(this, FileChooserSettings.LastDirectoryKey.Data, "Export cache");
        if (outputFolder != null) {
            // Save the node's state to a file
            Path nodeStateFile = outputFolder.resolve("node.json");
            try {
                Files.createDirectories(outputFolder);
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(nodeStateFile.toFile(), graphNode);
            } catch (IOException e) {
                e.printStackTrace();
            }
            JIPipeDataTableToOutputExporterRun run = new JIPipeDataTableToOutputExporterRun(getWorkbench(), outputFolder,
                    new ArrayList<>(slotMap.values()), true, false);
            JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), run);
        }
    }

    private void importCache() {
        Path inputFolder = FileChooserSettings.openDirectory(this, FileChooserSettings.LastDirectoryKey.Data, "Import cache");
        // Temporarily removed
//        Path nodeStateFile = inputFolder.resolve("node.json");
//        if (Files.exists(nodeStateFile)) {
//            try {
//                JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(nodeStateFile.toFile());
//                JIPipeIssueReport report = new JIPipeIssueReport();
//                JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
//                JIPipeGraphNode stateNode = JIPipeGraphNode.fromJsonNode(node, report, notifications);
//                if (!report.isValid()) {
//                    UIUtils.openValidityReportDialog(this, report, "Error while loading node", new HTMLText("Information about the cached node could not be loaded.<br/>" +
//                            "Although the data will be loaded, this could indicate a problem with the cached data.").getHtml(), true);
//                }
//                if (stateNode.getInfo() != graphNode.getInfo()) {
//                    if (JOptionPane.showConfirmDialog(this,
//                            "It looks like that this folder was created for a different node type.\n" +
//                                    "The node you have selected has the type ID '" + graphNode.getInfo().getId() + "',\n" +
//                                    "while the cache folder was created for type ID '" + stateNode.getInfo().getId() + "'.\n\nContinue anyway?",
//                            "Import cache",
//                            JOptionPane.YES_NO_OPTION,
//                            JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
//                        return;
//                    }
//                } else if ((graphNode instanceof JIPipeAlgorithm) && (stateNode instanceof JIPipeAlgorithm)) {
//                    String stateId = ((JIPipeAlgorithm) stateNode).getStateId();
//                    String currentStateId = ((JIPipeAlgorithm) graphNode).getStateId();
//                    ObjectNode stateIdJson = JsonUtils.getObjectMapper().readerFor(ObjectNode.class).readValue(stateId);
//                    ObjectNode currentStateIdJson = JsonUtils.getObjectMapper().readerFor(ObjectNode.class).readValue(currentStateId);
//                    stateIdJson.remove("jipipe:node-alias-id");
//                    stateIdJson.remove("jipipe:node-uuid");
//                    currentStateIdJson.remove("jipipe:node-alias-id");
//                    currentStateIdJson.remove("jipipe:node-uuid");
//                    if (!Objects.equals(stateIdJson, currentStateIdJson)) {
//                        if (JOptionPane.showConfirmDialog(this,
//                                "The cache folder was created for a different parameter set.\n\nContinue anyway?",
//                                "Import cache",
//                                JOptionPane.YES_NO_OPTION,
//                                JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
//                            return;
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                UIUtils.openErrorDialog(this, e);
//                if (JOptionPane.showConfirmDialog(this,
//                        "There was an error while checking for data compatibility.\n\nContinue anyway?",
//                        "Import cache",
//                        JOptionPane.YES_NO_OPTION,
//                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
//                    return;
//                }
//            }
//        } else {
//            if (JOptionPane.showConfirmDialog(this,
//                    "The folder does not contain 'node.json', which is there to check if " +
//                            "you have chosen the correct node.\n\nContinue anyway?",
//                    "Import cache",
//                    JOptionPane.YES_NO_OPTION,
//                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
//                return;
//            }
//        }
        List<String> missingSlots = new ArrayList<>();
        for (JIPipeDataSlot outputSlot : graphNode.getOutputSlots()) {
            if (!Files.isDirectory(inputFolder.resolve(outputSlot.getName()))) {
                missingSlots.add(outputSlot.getName());
            }
        }
        if (!missingSlots.isEmpty()) {
            if (JOptionPane.showConfirmDialog(this,
                    "Could not find cached folders for following outputs:\n\n" + String.join("\n", missingSlots) + "\n\nContinue anyway?",
                    "Import cache",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        JIPipeImportCachedSlotOutputRun run = new JIPipeImportCachedSlotOutputRun(getProjectWorkbench().getProject(), graphNode, inputFolder);
        JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), run);
    }

    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
        if (!isDisplayable())
            return;
        if (JIPipeRunnerQueue.getInstance().getCurrentRun() == null) {
            refreshTable();
        }
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (!isDisplayable())
            return;
        refreshTable();
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (!isDisplayable())
            return;
        refreshTable();
    }
}
