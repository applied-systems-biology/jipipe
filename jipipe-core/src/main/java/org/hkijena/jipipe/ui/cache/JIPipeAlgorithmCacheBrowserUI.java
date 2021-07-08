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
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.JIPipeProjectCacheState;
import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.actions.UpdateCacheAction;
import org.hkijena.jipipe.ui.grapheditor.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.ui.quickrun.QuickRun;
import org.hkijena.jipipe.ui.quickrun.QuickRunSettings;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * UI around an {@link JIPipeRun} result
 */
public class JIPipeAlgorithmCacheBrowserUI extends JIPipeProjectWorkbenchPanel {
    private final JIPipeGraphNode graphNode;
    private final JToolBar toolBar = new JToolBar();
    private JSplitPane splitPane;
    private JIPipeAlgorithmCacheTree tree;
    private JIPipeGraphCanvasUI graphCanvasUI;

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
        showCurrentlySelectedNode();

        getProject().getCache().getEventBus().register(this);
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new JIPipeAlgorithmCacheTree(getProjectWorkbench(), graphNode);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tree,
                new JPanel());
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        add(splitPane, BorderLayout.CENTER);

        tree.getTree().addTreeSelectionListener(e -> showCurrentlySelectedNode());

        initializeToolbar();
    }

    private void showCurrentlySelectedNode() {
        Object lastPathComponent = tree.getTree().getLastSelectedPathComponent();
        if (lastPathComponent instanceof DefaultMutableTreeNode) {
            Object userObject = ((DefaultMutableTreeNode) lastPathComponent).getUserObject();
            if (userObject instanceof JIPipeDataSlot) {
                showDataSlot((JIPipeDataSlot) userObject);
            } else if (userObject instanceof JIPipeGraphNode) {
                showDataSlotsOfAlgorithm((JIPipeGraphNode) userObject);
            } else if (userObject instanceof JIPipeProjectCacheState) {
                showDataSlotsOfState((JIPipeProjectCacheState) userObject);
            } else {
                showAllDataSlots();
            }
        }
    }

    private void showDataSlotsOfState(JIPipeProjectCacheState state) {
        List<JIPipeDataSlot> result = new ArrayList<>();
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(graphNode);
        if (stateMap != null) {
            Map<String, JIPipeDataSlot> slotMap = stateMap.getOrDefault(state, null);
            if (slotMap != null) {
                result.addAll(slotMap.values());
            }
        }
        showDataSlots(result);
    }

    private void showAllDataSlots() {
        List<JIPipeDataSlot> result = new ArrayList<>();
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(graphNode);
        if (stateMap != null) {
            for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                result.addAll(stateEntry.getValue().values());
            }
        }
        showDataSlots(result);
    }

    private void showDataSlotsOfAlgorithm(JIPipeGraphNode algorithm) {
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(algorithm);
        if (stateMap != null) {
            List<JIPipeDataSlot> result = new ArrayList<>();
            for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                result.addAll(stateEntry.getValue().values());
            }
            showDataSlots(result);
        }
    }

    private void showDataSlots(List<JIPipeDataSlot> slots) {
        JIPipeCacheMultiDataSlotTableUI ui = new JIPipeCacheMultiDataSlotTableUI(getProjectWorkbench(), slots, false);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(JIPipeDataSlot dataSlot) {
        JIPipeCacheDataSlotTableUI ui = new JIPipeCacheDataSlotTableUI(getProjectWorkbench(), dataSlot);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void initializeToolbar() {
        toolBar.setFloatable(false);

        JButton updateCacheButton = new JButton("Update cache", UIUtils.getIconFromResources("actions/database.png"));
        updateCacheButton.addActionListener(e -> updateCache(false));
        toolBar.add(updateCacheButton);

        JButton cacheIntermediateResultsButton = new JButton(UIUtils.getIconFromResources("actions/cache-intermediate-results.png"));
        cacheIntermediateResultsButton.setToolTipText("Cache intermediate results");
        cacheIntermediateResultsButton.addActionListener(e -> updateCache(true));
        toolBar.add(cacheIntermediateResultsButton);

        toolBar.add(Box.createHorizontalStrut(8));

        JButton clearOutdatedButton = new JButton("Clear outdated", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearOutdatedButton.addActionListener(e -> getProject().getCache().autoClean(false, true));
        toolBar.add(clearOutdatedButton);

        JButton clearAllButton = new JButton("Clear all", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearAllButton.addActionListener(e -> getProject().getCache().clear(this.graphNode));
        toolBar.add(clearAllButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton importButton = new JButton("Import cache", UIUtils.getIconFromResources("actions/document-import.png"));
        importButton.setToolTipText("Imports cached data from a folder");
        importButton.addActionListener(e -> importCache());
        toolBar.add(importButton);

        JButton exportButton = new JButton("Export cache", UIUtils.getIconFromResources("actions/document-export.png"));
        exportButton.setToolTipText("Exports cached data to a folder");
        exportButton.addActionListener(e -> exportCache());
        toolBar.add(exportButton);

        add(toolBar, BorderLayout.NORTH);
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
        Object lastPathComponent = tree.getTree().getLastSelectedPathComponent();
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(graphNode);
        if (stateMap.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There is no cached data to export!", "Export cache", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeProjectCacheState exportedState = null;
        if (lastPathComponent instanceof DefaultMutableTreeNode) {
            Object userObject = ((DefaultMutableTreeNode) lastPathComponent).getUserObject();
            if (userObject instanceof JIPipeDataSlot) {
                for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMapEntry : stateMap.entrySet()) {
                    if (stateMapEntry.getValue().containsValue(userObject)) {
                        exportedState = stateMapEntry.getKey();
                        break;
                    }
                }
            } else if (userObject instanceof JIPipeProjectCacheState) {
                exportedState = (JIPipeProjectCacheState) userObject;
            }
        }
        if (exportedState == null) {
            // Choose the newest state
            exportedState = stateMap.keySet().stream().max(Comparator.naturalOrder()).get();
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
            JIPipeCachedSlotToOutputExporterRun run = new JIPipeCachedSlotToOutputExporterRun(getWorkbench(), outputFolder,
                    new ArrayList<>(stateMap.get(exportedState).values()), true);
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

    public JToolBar getToolBar() {
        return toolBar;
    }

    public JIPipeAlgorithmCacheTree getTree() {
        return tree;
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
            tree.refreshTree();
            showAllDataSlots();
        }
    }

    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if (!isDisplayable())
            return;
        tree.refreshTree();
        showAllDataSlots();
    }

    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        if (!isDisplayable())
            return;
        tree.refreshTree();
        showAllDataSlots();
    }
}
