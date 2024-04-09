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

package org.hkijena.jipipe.desktop.app.batchassistant;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.utils.JIPipeWeakDataReferenceData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.plugins.batchassistant.DataBatchStatusData;
import org.hkijena.jipipe.plugins.strings.StringData;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.data.WeakStore;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A tool that assists the user in configuring batch generation for
 * {@link JIPipeIteratingAlgorithm} and
 * {@link JIPipeMergingAlgorithm}
 */
public class JIPipeDesktopDataBatchAssistantUI extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeCache.ModifiedEventListener, JIPipeGraphNode.NodeSlotsChangedEventListener, JIPipeGraph.NodeDisconnectedEventListener, JIPipeParameterCollection.ParameterChangedEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {

    private static boolean SHOW_ADVANCED_SETTINGS = false;

    private final JIPipeAlgorithm algorithm;
    private final Runnable runUpdatePredecessorCache;
    private final JIPipeParameterCollection batchSettings;
    private final Multimap<String, Store<JIPipeDataTable>> currentCache = HashMultimap.create();
    private final JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();
    private final JIPipeDesktopDataBatchAssistantBatchPanel batchPanel;
    private final JIPipeDesktopDataBatchAssistantInputPreviewPanel inputPreviewPanel;
    private final JIPipeRunnableQueue calculatePreviewQueue = new JIPipeRunnableQueue("Data batch preview calculation");
    AutoResizeSplitPane splitPane1 = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, 0.5);
    AutoResizeSplitPane splitPane2 = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, 0.4);
    private JIPipeGraphNode batchesNodeCopy;
    private boolean autoRefresh = true;


    /**
     * @param workbenchUI               The workbench UI
     * @param algorithm                 the target algorithm
     * @param runUpdatePredecessorCache function that updates the cache
     */
    public JIPipeDesktopDataBatchAssistantUI(JIPipeDesktopProjectWorkbench workbenchUI, JIPipeGraphNode algorithm, Runnable runUpdatePredecessorCache) {
        super(workbenchUI);
        this.algorithm = (JIPipeAlgorithm) algorithm;
        this.batchSettings = ((JIPipeIterationStepAlgorithm) algorithm).getGenerationSettingsInterface();
        this.runUpdatePredecessorCache = runUpdatePredecessorCache;
        this.batchPanel = new JIPipeDesktopDataBatchAssistantBatchPanel(workbenchUI, this);
        this.inputPreviewPanel = new JIPipeDesktopDataBatchAssistantInputPreviewPanel(workbenchUI, this);
        initialize();
        updateStatus();
        getProject().getCache().getModifiedEventEmitter().subscribeWeak(this);
        algorithm.getNodeSlotsChangedEventEmitter().subscribeWeak(this);
        algorithm.getParentGraph().getNodeDisconnectedEventEmitter().subscribeWeak(this);
        batchSettings.getParameterChangedEventEmitter().subscribeWeak(this);
        calculatePreviewQueue.getFinishedEventEmitter().subscribe(this);
        calculatePreviewQueue.getInterruptedEventEmitter().subscribe(this);
    }

    private void updateCurrentCache() {
        messagePanel.clear();
        currentCache.clear();
        if (algorithm.getInputSlots().isEmpty()) {
            return;
        }
        for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
            Set<JIPipeDataSlot> sourceSlots = algorithm.getParentGraph().getInputIncomingSourceSlots(inputSlot);
            if (!sourceSlots.isEmpty()) {
                for (JIPipeDataSlot sourceSlot : sourceSlots) {
                    Map<String, JIPipeDataTable> sourceCache = getProject().getCache().query(sourceSlot.getNode(), sourceSlot.getNode().getUUIDInParentGraph(), new JIPipeProgressInfo());
                    if (sourceCache == null || sourceCache.isEmpty()) {
                        messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Error, "No cached data available! Click 'Update predecessor cache' to generate the data.", true, true);
                        currentCache.clear();
                        return;
                    }
                    JIPipeDataTable cache = sourceCache.getOrDefault(sourceSlot.getName(), null);
                    if (cache != null) {
                        currentCache.put(inputSlot.getName(), new WeakStore<>(cache));
                    } else {
                        currentCache.clear();
                        messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Error, "Cached data is outdated! Click 'Update predecessor cache' to re-generate the data.", true, true);
                        return;
                    }
                }
            } else if (!inputSlot.getInfo().isOptional()) {
                currentCache.clear();
                messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Error, "Not all inputs are connected to an output!", true, true);
                return;
            }
        }
    }

    private void updateStatus() {
        updateCurrentCache();
        refreshBatchPreview();
    }

    public JIPipeAlgorithm getAlgorithm() {
        return algorithm;
    }

    public Multimap<String, Store<JIPipeDataTable>> getCurrentCache() {
        return currentCache;
    }

    private void refreshBatchPreview() {

        // Update the input panel
        inputPreviewPanel.updateStatus();

        messagePanel.clear();
        messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Info, "Calculating preview ...", false, false);

        // Stop worker
        calculatePreviewQueue.cancelAll();

        batchesNodeCopy = algorithm.getInfo().duplicate(algorithm);
        // Pass cache as input slots
        for (JIPipeDataSlot inputSlot : batchesNodeCopy.getDataInputSlots()) {
            for (Store<JIPipeDataTable> cacheSlotReference : currentCache.get(inputSlot.getName())) {
                JIPipeDataTable cacheSlot = cacheSlotReference.get();
                if (cacheSlot == null) {
                    messagePanel.clear();
                    messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Warning, "Cache was cleared while previewing iteration steps. The process was cancelled.", true, true);
                    return;
                }
                inputSlot.addDataFromTable(cacheSlot, new JIPipeProgressInfo());
            }
        }
        // Generate dry-run
        batchPanel.setDataTable(new JIPipeDataTable(JIPipeData.class));

        DataBatchGeneratorRun run = new DataBatchGeneratorRun(batchesNodeCopy);
        calculatePreviewQueue.enqueue(run);
    }

    private void displayBatches(JIPipeDataBatchGenerationResult iterationStepGenerationResult, JIPipeGraphNode algorithm) {
        messagePanel.clear();
        batchPanel.setDataTable(new JIPipeDataTable(DataBatchStatusData.class));
        inputPreviewPanel.highlightResults(iterationStepGenerationResult);

//        batchPreviewNumberLabel.setText(batches.size() + " batches");
//        batchPreviewMissingLabel.setVisible(false);
//        batchPreviewDuplicateLabel.setVisible(false);

        JIPipeDataTable dataTable = new JIPipeDataTable(JIPipeData.class);
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

        for (JIPipeMultiIterationStep batch : iterationStepGenerationResult.getDataBatches()) {
            List<JIPipeTextAnnotation> textAnnotations = new ArrayList<>(batch.getMergedTextAnnotations().values());
            List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();

            int numIncompleteRequired = 0;
            int numIncompleteOptional = 0;
            int numMerging = 0;
            DataBatchStatusData statusData = new DataBatchStatusData();

            for (JIPipeDataSlot inputSlot : algorithm.getDataInputSlots()) {
                List<JIPipeData> dataList = batch.getInputData(inputSlot, JIPipeData.class, progressInfo);
                Map<String, Object> status = new HashMap<>();
                JIPipeData singletonData;
                if (dataList.isEmpty()) {
                    singletonData = new StringData("Empty");
                    if (inputSlot.getInfo().isOptional()) {
                        ++numIncompleteOptional;
                    } else {
                        ++numIncompleteRequired;
                    }

                    status.put("Slot", inputSlot.getName());
                    status.put("Status", "Slot has no data!");
                } else if (dataList.size() == 1) {
                    singletonData = new JIPipeWeakDataReferenceData(dataList.get(0));

                    status.put("Slot", inputSlot.getName());
                    status.put("Status", "Contains 1 item.");
                } else {
                    JIPipeDataTable list = new JIPipeDataTable(JIPipeData.class);
                    for (JIPipeData datum : dataList) {
                        list.addData(new JIPipeWeakDataReferenceData(datum), progressInfo);
                    }
                    singletonData = list;
                    ++numMerging;

                    status.put("Slot", inputSlot.getName());
                    status.put("Status", "Slot contains " + dataList.size() + " items");
                }

                statusData.getPerSlotStatus().addRow(status);
                dataAnnotations.add(new JIPipeDataAnnotation(inputSlot.getName(), singletonData));
            }

//            if (hasEmpty)
//                batchPreviewMissingLabel.setVisible(true);
//            if (hasMultiple)
//                batchPreviewDuplicateLabel.setVisible(true);

            statusData.setStatusValid(numIncompleteRequired <= 0);
            if (numIncompleteRequired > 0) {
                statusData.setStatusMessage("Missing data!");
            } else if (numIncompleteOptional > 0) {
                statusData.setStatusMessage("Missing optional data");
            } else if (numMerging > 0) {
                statusData.setStatusMessage("Multiple data per slot");
            } else {
                statusData.setStatusMessage("One data per slot");
            }
            statusData.setNumIncompleteRequired(numIncompleteRequired);
            statusData.setNumIncompleteOptional(numIncompleteOptional);
            statusData.setNumMerging(numMerging);

            dataTable.addData(statusData,
                    textAnnotations,
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    dataAnnotations,
                    JIPipeDataAnnotationMergeMode.OverwriteExisting, progressInfo);
        }

        batchPanel.setDataTable(dataTable);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Setup split panes
        add(splitPane1, BorderLayout.CENTER);
        splitPane1.setTopComponent(splitPane2);

        initializeTopPanel();
        initializeParameterPanel();

        splitPane1.setBottomComponent(batchPanel);
        splitPane2.setTopComponent(inputPreviewPanel);

    }


    private void initializeParameterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JIPipeDesktopParameterPanel parameterPanel = new JIPipeDesktopParameterPanel(getDesktopWorkbench(),
                ((JIPipeIterationStepAlgorithm) algorithm).getGenerationSettingsInterface(),
                null,
                JIPipeDesktopParameterPanel.WITH_SCROLLING | JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.DOCUMENTATION_NO_UI | JIPipeDesktopParameterPanel.NO_EMPTY_GROUP_HEADERS);
        toggleParameterPanelAdvancedMode(parameterPanel, SHOW_ADVANCED_SETTINGS);

        JIPipeDesktopFormPanel.GroupHeaderPanel groupHeaderPanel = new JIPipeDesktopFormPanel.GroupHeaderPanel("Settings", UIUtils.getIconFromResources("actions/configure.png"), 4);

        JCheckBox advancedSettingsCheck = new JCheckBox("Advanced settings");
        advancedSettingsCheck.setOpaque(false);
        advancedSettingsCheck.setSelected(SHOW_ADVANCED_SETTINGS);
        advancedSettingsCheck.addActionListener(e -> {
            SHOW_ADVANCED_SETTINGS = advancedSettingsCheck.isSelected();
            toggleParameterPanelAdvancedMode(parameterPanel, advancedSettingsCheck.isSelected());
        });
        groupHeaderPanel.addColumn(advancedSettingsCheck);

        groupHeaderPanel.addColumn(UIUtils.createBalloonHelpButton("The settings change the behavior of how iteration steps are generated. " +
                "You can also access more advanced settings by activating them."));
        panel.add(groupHeaderPanel, BorderLayout.NORTH);


        panel.add(parameterPanel, BorderLayout.CENTER);

        splitPane2.setBottomComponent(panel);
    }

    private void toggleParameterPanelAdvancedMode(JIPipeDesktopParameterPanel parameterPanel, boolean advancedMode) {
        boolean containsPinned = parameterPanel.getParameterTree().getParameters().values().stream().anyMatch(JIPipeParameterAccess::isPinned);
        if (containsPinned) {
            parameterPanel.setOnlyPinned(!advancedMode);
        } else {
            parameterPanel.setOnlyPinned(false);
        }
    }

    private void initializeTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        add(topPanel, BorderLayout.NORTH);

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench(), calculatePreviewQueue).makeFlat());

        toolBar.add(Box.createHorizontalGlue());

        JButton updateCacheButton = new JButton("Update predecessor cache", UIUtils.getIconFromResources("actions/cache-predecessors.png"));
        updateCacheButton.setToolTipText("Runs the pipeline up until the predecessors of the selected node. Nothing is written to disk.");
        updateCacheButton.addActionListener(e -> updatePredecessorCache());
        toolBar.add(updateCacheButton);
        topPanel.add(toolBar);

        JButton refreshButton = new JButton("Refresh", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.setToolTipText("Refreshes the preview of generated batches");
        refreshButton.addActionListener(e -> refreshBatchPreview());
        toolBar.add(refreshButton);

        JToggleButton autoRefreshButton = new JToggleButton(UIUtils.getIconFromResources("actions/quickopen-function.png"));
        autoRefreshButton.setToolTipText("Auto refresh");
        autoRefreshButton.setSelected(autoRefresh);
        autoRefreshButton.addActionListener(e -> {
            autoRefresh = autoRefreshButton.isSelected();
            if (autoRefresh)
                refreshBatchPreview();
        });
        toolBar.add(autoRefreshButton);

        // Message panel
        topPanel.add(messagePanel);
    }

    public void updatePredecessorCache() {
        runUpdatePredecessorCache.run();
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
        if (!isDisplayable()) {
            clearCaches();
            return;
        }
        updateStatus();
    }

    private void clearCaches() {
        currentCache.clear();
        if (batchesNodeCopy != null) {
            batchesNodeCopy.clearSlotData();
        }
        batchPanel.setDataTable(new JIPipeDataTable(JIPipeData.class));
    }

    /**
     * Triggered when the slots were changed
     *
     * @param event generated event
     */
    @Override
    public void onNodeSlotsChanged(JIPipeGraphNode.NodeSlotsChangedEvent event) {
        if (!isDisplayable()) {
            clearCaches();
            return;
        }
        updateStatus();
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (event.getSource() == batchSettings && autoRefresh) {
            refreshBatchPreview();
        }
    }

    @Override
    public void onNodeDisconnected(JIPipeGraph.NodeDisconnectedEvent event) {
        if (event.getTarget().getNode() == algorithm) {
            updateStatus();
        }
    }

    public JIPipeRunnableQueue getCalculatePreviewQueue() {
        return calculatePreviewQueue;
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof DataBatchGeneratorRun) {
            displayBatches(((DataBatchGeneratorRun) event.getRun()).getResult(), ((DataBatchGeneratorRun) event.getRun()).algorithm);
        }
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() instanceof DataBatchGeneratorRun) {
            displayBatches(new JIPipeDataBatchGenerationResult(), ((DataBatchGeneratorRun) event.getRun()).algorithm);
        }
    }

    private static class DataBatchGeneratorRun extends AbstractJIPipeRunnable {

        private final JIPipeGraphNode algorithm;
        private JIPipeDataBatchGenerationResult result;

        private DataBatchGeneratorRun(JIPipeGraphNode algorithm) {
            this.algorithm = algorithm;
        }

        @Override
        public String getTaskLabel() {
            return "Calculate preview";
        }

        @Override
        public void run() {
            result = ((JIPipeIterationStepAlgorithm) algorithm).generateDataBatchesGenerationResult(algorithm.getDataInputSlots(), getProgressInfo());
            if (result == null) {
                result = new JIPipeDataBatchGenerationResult();
            }
        }

        public JIPipeDataBatchGenerationResult getResult() {
            return result;
        }
    }
}
