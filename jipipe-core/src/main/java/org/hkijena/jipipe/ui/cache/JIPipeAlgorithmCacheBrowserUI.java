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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.datatable.JIPipeExtendedDataTableUI;
import org.hkijena.jipipe.ui.datatable.JIPipeExtendedMultiDataTableUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.actions.UpdateCacheAction;
import org.hkijena.jipipe.ui.grapheditor.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.ui.quickrun.QuickRun;
import org.hkijena.jipipe.ui.quickrun.QuickRunSettings;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunWorkerInterruptedEvent;
import org.hkijena.jipipe.utils.MenuManager;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

/**
 * UI around an {@link JIPipeProjectRun} result
 */
public class JIPipeAlgorithmCacheBrowserUI extends JIPipeProjectWorkbenchPanel {
    private final JIPipeGraphNode graphNode;
    private final JIPipeGraphCanvasUI graphCanvasUI;
    private final JIPipeAlgorithmCacheBrowserOutputSelectorUI selectorUI;
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
        this.selectorUI = new JIPipeAlgorithmCacheBrowserOutputSelectorUI(graphNode);
        initialize();

        getProject().getCache().getEventBus().register(this);
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
        selectorUI.getEventBus().register(this);

        // Selects all outputs
        selectorUI.selectOutput(JIPipeAlgorithmCacheBrowserOutputSelectorUI.SELECT_ALL_OUTPUTS);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        selectorUI.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("TabbedPane.darkShadow")),
                BorderFactory.createEmptyBorder(8, 2, 8, 2)));
        add(selectorUI, BorderLayout.NORTH);
    }

    private void showAllDataSlots() {
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(getProject());
        Map<String, JIPipeDataSlot> cachedData = query.getCachedData(graphNode);
        if (cachedData.size() == 1) {
            JIPipeDataSlot first = cachedData.values().iterator().next();
            showDataSlot(first);
        } else {
            showDataSlots(new ArrayList<>(cachedData.values()));
        }
    }

    private void showDataSlot(String name) {
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(getProject());
        Map<String, JIPipeDataSlot> cachedData = query.getCachedData(graphNode);
        if (cachedData.containsKey(name)) {
            showDataSlot(cachedData.get(name));
        } else {
            showDataSlots(Collections.emptyList());
        }
    }

    private void showDataSlots(List<JIPipeDataSlot> slots) {
        if (currentContent != null) {
            remove(currentContent);
        }
        JIPipeExtendedMultiDataTableUI ui = new JIPipeExtendedMultiDataTableUI(getProjectWorkbench(), slots, false);
        initializeTableMenu(ui.getMenuManager());
        add(ui, BorderLayout.CENTER);
        currentContent = ui;
        revalidate();
    }

    private void initializeTableMenu(MenuManager menuManager) {

        {
            JButton cacheIntermediateResultsButton = new JButton(UIUtils.getIconFromResources("actions/cache-intermediate-results.png"));
            cacheIntermediateResultsButton.setToolTipText("Cache intermediate results");
            cacheIntermediateResultsButton.addActionListener(e -> updateCache(true));
            menuManager.addFirst(cacheIntermediateResultsButton);

            JButton updateCacheButton = new JButton("Update cache", UIUtils.getIconFromResources("actions/database.png"));
            updateCacheButton.addActionListener(e -> updateCache(false));
            menuManager.addFirst(updateCacheButton);
        }
        {
            JMenu cacheMenu = menuManager.getOrCreateMenu("Cache");

            JMenuItem updateCacheItem = new JMenuItem("Update cache", UIUtils.getIconFromResources("actions/database.png"));
            updateCacheItem.addActionListener(e -> updateCache(false));
            cacheMenu.add(updateCacheItem);

            JMenuItem cacheIntermediateResultsItem = new JMenuItem("Cache intermediate results", UIUtils.getIconFromResources("actions/cache-intermediate-results.png"));
            cacheIntermediateResultsItem.addActionListener(e -> updateCache(true));
            cacheMenu.add(cacheIntermediateResultsItem);

            cacheMenu.addSeparator();

            JMenuItem clearAllButton = new JMenuItem("Clear all (this node)", UIUtils.getIconFromResources("actions/clear-brush.png"));
            clearAllButton.addActionListener(e -> getProject().getCache().clear(this.graphNode.getUUIDInParentGraph()));
            cacheMenu.add(clearAllButton);

            JMenuItem clearOutdatedButton = new JMenuItem("Clear outdated (this node)", UIUtils.getIconFromResources("actions/clear-brush.png"));
            clearOutdatedButton.addActionListener(e -> getProject().getCache().autoClean(false, true, new JIPipeProgressInfo()));
            cacheMenu.add(clearOutdatedButton);
        }
        {
            JMenu exportMenu = menuManager.getOrCreateMenu("Export");
            exportMenu.setText("Import/Export");

            exportMenu.addSeparator();

            JMenuItem importButton = new JMenuItem("Import cache", UIUtils.getIconFromResources("actions/document-import.png"));
            importButton.setToolTipText("Imports cached data from a folder");
            importButton.addActionListener(e -> importCache());
            exportMenu.add(importButton);

            JMenuItem exportButton = new JMenuItem("Export cache", UIUtils.getIconFromResources("actions/document-export.png"));
            exportButton.setToolTipText("Exports cached data to a folder");
            exportButton.addActionListener(e -> exportCache());
            exportMenu.add(exportButton);
        }
    }

    private void showDataSlot(JIPipeDataSlot dataSlot) {
        if (currentContent != null) {
            remove(currentContent);
        }
        JIPipeExtendedDataTableUI ui = new JIPipeExtendedDataTableUI(getProjectWorkbench(), dataSlot, true);
        initializeTableMenu(ui.getMenuManager());
        add(ui, BorderLayout.CENTER);
        currentContent = ui;
        revalidate();
    }

    private void updateCache(boolean storeIntermediateResults) {
        if (graphCanvasUI != null) {
            JIPipeNodeUI ui = graphCanvasUI.getNodeUIs().getOrDefault(graphNode, null);
            if (ui != null) {
                ui.getEventBus().post(new JIPipeGraphCanvasUI.NodeUIActionRequestedEvent(ui, new UpdateCacheAction(false)));
                return;
            }
        }

        QuickRunSettings settings = new QuickRunSettings();
        settings.setLoadFromCache(true);
        settings.setStoreIntermediateResults(storeIntermediateResults);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        QuickRun testBench = new QuickRun(getProject(), graphNode, settings);
        JIPipeRunnerQueue.getInstance().enqueue(testBench);
    }

    private void exportCache() {
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(graphNode.getUUIDInParentGraph());
        if (stateMap.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There is no cached data to export!", "Export cache", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeProjectCacheState exportedState = stateMap.keySet().stream().max(Comparator.naturalOrder()).get();
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
                    new ArrayList<>(stateMap.get(exportedState).values()), true, false);
            JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), run);
        }
    }

    private void importCache() {
        Path inputFolder = FileChooserSettings.openDirectory(this, FileChooserSettings.LastDirectoryKey.Data, "Import cache");
        Path nodeStateFile = inputFolder.resolve("node.json");
        if (Files.exists(nodeStateFile)) {
            try {
                JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(nodeStateFile.toFile());
                JIPipeIssueReport report = new JIPipeIssueReport();
                JIPipeGraphNode stateNode = JIPipeGraphNode.fromJsonNode(node, report);
                if (!report.isValid()) {
                    UIUtils.openValidityReportDialog(this, report, true);
                }
                if (stateNode.getInfo() != graphNode.getInfo()) {
                    if (JOptionPane.showConfirmDialog(this,
                            "It looks like that this folder was created for a different node type.\n" +
                                    "The node you have selected has the type ID '" + graphNode.getInfo().getId() + "',\n" +
                                    "while the cache folder was created for type ID '" + stateNode.getInfo().getId() + "'.\n\nContinue anyways?",
                            "Import cache",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                        return;
                    }
                } else if ((graphNode instanceof JIPipeAlgorithm) && (stateNode instanceof JIPipeAlgorithm)) {
                    String stateId = ((JIPipeAlgorithm) stateNode).getStateId();
                    String currentStateId = ((JIPipeAlgorithm) graphNode).getStateId();
                    ObjectNode stateIdJson = JsonUtils.getObjectMapper().readerFor(ObjectNode.class).readValue(stateId);
                    ObjectNode currentStateIdJson = JsonUtils.getObjectMapper().readerFor(ObjectNode.class).readValue(currentStateId);
                    stateIdJson.remove("jipipe:node-alias-id");
                    stateIdJson.remove("jipipe:node-uuid");
                    currentStateIdJson.remove("jipipe:node-alias-id");
                    currentStateIdJson.remove("jipipe:node-uuid");
                    if (!Objects.equals(stateIdJson, currentStateIdJson)) {
                        if (JOptionPane.showConfirmDialog(this,
                                "The cache folder was created for a different parameter set.\n\nContinue anyways?",
                                "Import cache",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                UIUtils.openErrorDialog(this, e);
                if (JOptionPane.showConfirmDialog(this,
                        "There was an error while checking for data compatibility.\n\nContinue anyways?",
                        "Import cache",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                    return;
                }
            }
        } else {
            if (JOptionPane.showConfirmDialog(this,
                    "The folder does not contain 'node.json', which is there to check if " +
                            "you have chosen the correct node.\n\nContinue anyways?",
                    "Import cache",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        List<String> missingSlots = new ArrayList<>();
        for (JIPipeDataSlot outputSlot : graphNode.getOutputSlots()) {
            if (!Files.isDirectory(inputFolder.resolve(outputSlot.getName()))) {
                missingSlots.add(outputSlot.getName());
            }
        }
        if (!missingSlots.isEmpty()) {
            if (JOptionPane.showConfirmDialog(this,
                    "Could not find cached folders for following outputs:\n\n" + String.join("\n", missingSlots) + "\n\nContinue anyways?",
                    "Import cache",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        JIPipeImportCachedSlotOutputRun run = new JIPipeImportCachedSlotOutputRun(getProjectWorkbench().getProject(), graphNode, inputFolder);
        JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), run);
    }

    @Subscribe
    public void onOutputSelected(JIPipeAlgorithmCacheBrowserOutputSelectorUI.OutputSelectedEvent event) {
        if (JIPipeAlgorithmCacheBrowserOutputSelectorUI.SELECT_ALL_OUTPUTS.equals(event.getName())) {
            showAllDataSlots();
        } else {
            showDataSlot(event.getName());
        }
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        if (!isDisplayable())
            return;
        if (JIPipeRunnerQueue.getInstance().getCurrentRun() == null) {
            selectorUI.selectOutput(JIPipeAlgorithmCacheBrowserOutputSelectorUI.SELECT_ALL_OUTPUTS);
        }
    }

    @Subscribe
    public void onWorkerFinished(RunWorkerFinishedEvent event) {
        if (!isDisplayable())
            return;
        selectorUI.selectOutput(JIPipeAlgorithmCacheBrowserOutputSelectorUI.SELECT_ALL_OUTPUTS);
    }

    @Subscribe
    public void onWorkerInterrupted(RunWorkerInterruptedEvent event) {
        if (!isDisplayable())
            return;
        selectorUI.selectOutput(JIPipeAlgorithmCacheBrowserOutputSelectorUI.SELECT_ALL_OUTPUTS);
    }
}
