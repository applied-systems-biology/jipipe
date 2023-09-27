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

package org.hkijena.jipipe.ui.batchassistant;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeWeakDataReferenceData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.batchassistant.DataBatchStatusData;
import org.hkijena.jipipe.extensions.strings.StringData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.data.WeakStore;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * A tool that assists the user in configuring batch generation for
 * {@link org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm} and
 * {@link org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm}
 */
public class DataBatchAssistantUI extends JIPipeProjectWorkbenchPanel implements JIPipeCache.ModifiedEventListener, JIPipeGraphNode.NodeSlotsChangedEventListener, JIPipeGraph.NodeDisconnectedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {
    private final JIPipeAlgorithm algorithm;
    private final Runnable runTestBench;
    private final JIPipeParameterCollection batchSettings;
    private final Multimap<String, Store<JIPipeDataTable>> currentCache = HashMultimap.create();
    private final MessagePanel messagePanel = new MessagePanel();
    AutoResizeSplitPane splitPane1 = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, AutoResizeSplitPane.RATIO_1_TO_3);
    AutoResizeSplitPane splitPane2 = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, AutoResizeSplitPane.RATIO_1_TO_1);
    private DataBatchAssistantBatchPanel batchPanel;
    private JIPipeGraphNode batchesNodeCopy;
    private boolean autoRefresh = true;
    private DataBatchGeneratorWorker lastWorker = null;



    /**
     * @param workbenchUI  The workbench UI
     * @param algorithm    the target algorithm
     * @param runTestBench function that updates the cache
     */
    public DataBatchAssistantUI(JIPipeProjectWorkbench workbenchUI, JIPipeGraphNode algorithm, Runnable runTestBench) {
        super(workbenchUI);
        this.algorithm = (JIPipeAlgorithm) algorithm;
        this.batchSettings = ((JIPipeDataBatchAlgorithm) algorithm).getGenerationSettingsInterface();
        this.runTestBench = runTestBench;
        initialize();
        updateStatus();
        getProject().getCache().getModifiedEventEmitter().subscribeWeak(this);
        algorithm.getNodeSlotsChangedEventEmitter().subscribeWeak(this);
        algorithm.getParentGraph().getNodeDisconnectedEventEmitter().subscribeWeak(this);
        batchSettings.getParameterChangedEventEmitter().subscribeWeak(this);
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
                        messagePanel.addMessage(MessagePanel.MessageType.Error, "No cached data available! Click 'Update predecessor cache' to generate the data.", true, true);
                        currentCache.clear();
                        return;
                    }
                    JIPipeDataTable cache = sourceCache.getOrDefault(sourceSlot.getName(), null);
                    if (cache != null) {
                        currentCache.put(inputSlot.getName(), new WeakStore<>(cache));
                    } else {
                        currentCache.clear();
                        messagePanel.addMessage(MessagePanel.MessageType.Error, "Cached data is outdated! Click 'Update predecessor cache' to re-generate the data.", true, true);
                        return;
                    }
                }
            } else if (!inputSlot.getInfo().isOptional()) {
                currentCache.clear();
                messagePanel.addMessage(MessagePanel.MessageType.Error, "Not all inputs are connected to an output!", true, true);
                return;
            }
        }
    }

    private void updateStatus() {
        updateCurrentCache();
        if (currentCache.isEmpty()) {
//            switchToError();
        } else {
            refreshBatchPreview();
        }
    }

    private void refreshBatchPreview() {
//        batchPreviewNumberLabel.setText("Please wait ...");

        // Stop worker
        if (lastWorker != null) {
            lastWorker.cancel(true);
            lastWorker = null;
        }

        batchesNodeCopy = algorithm.getInfo().duplicate(algorithm);
        // Pass cache as input slots
        for (JIPipeDataSlot inputSlot : batchesNodeCopy.getDataInputSlots()) {
            for (Store<JIPipeDataTable> cacheSlotReference : currentCache.get(inputSlot.getName())) {
                JIPipeDataTable cacheSlot = cacheSlotReference.get();
                if (cacheSlot == null) {
//                    batchPreviewNumberLabel.setText("Cache was cleared! Aborted.");
                    return;
                }
                inputSlot.addDataFromTable(cacheSlot, new JIPipeProgressInfo());
            }
        }
        // Generate dry-run
        batchPanel.setDataTable(new JIPipeDataTable(JIPipeData.class));

        lastWorker = new DataBatchGeneratorWorker(this, batchesNodeCopy);
        lastWorker.execute();
    }

    private void displayBatches(List<JIPipeMergingDataBatch> batches, JIPipeGraphNode algorithm) {
        batchPanel.setDataTable(new JIPipeDataTable(DataBatchStatusData.class));

//        batchPreviewNumberLabel.setText(batches.size() + " batches");
//        batchPreviewMissingLabel.setVisible(false);
//        batchPreviewDuplicateLabel.setVisible(false);

        JIPipeDataTable dataTable = new JIPipeDataTable(JIPipeData.class);
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

        for (JIPipeMergingDataBatch batch : batches) {
            List<JIPipeTextAnnotation> textAnnotations = new ArrayList<>(batch.getMergedTextAnnotations().values());
            List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();

            boolean hasEmpty = false;
            boolean hasMultiple = false;
            DataBatchStatusData statusData = new DataBatchStatusData();

            for (JIPipeDataSlot inputSlot : algorithm.getDataInputSlots()) {
                List<JIPipeData> dataList = batch.getInputData(inputSlot, JIPipeData.class, progressInfo);
                Map<String, Object> status = new HashMap<>();
                JIPipeData singletonData;
                if (dataList.isEmpty()) {
                    singletonData = new StringData("Empty");
                    hasEmpty = true;

                    status.put("Slot", inputSlot.getName());
                    status.put("Status", "Slot contains no data!");
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
                    hasMultiple = true;

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

            statusData.setStatusValid(!hasEmpty);
            statusData.setStatusMessage(hasEmpty ? "Missing data!" : (hasMultiple ? "Multiple data per slot" : "One data per slot"));

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
    }


    private void initializeParameterPanel() {
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(),
                ((JIPipeDataBatchAlgorithm) algorithm).getGenerationSettingsInterface(),
                null,
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_NO_UI | ParameterPanel.NO_EMPTY_GROUP_HEADERS);
        splitPane1.setTopComponent(parameterPanel);
    }

    private void initializeTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        add(topPanel, BorderLayout.NORTH);

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton updateCacheButton = new JButton("Update predecessor cache", UIUtils.getIconFromResources("actions/cache-predecessors.png"));
        updateCacheButton.setToolTipText("Runs the pipeline up until the predecessors of the selected node. Nothing is written to disk.");
        updateCacheButton.addActionListener(e -> runTestBench.run());
        toolBar.add(updateCacheButton);
        topPanel.add(toolBar);

        toolBar.add(Box.createHorizontalGlue());

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

    private static class DataBatchGeneratorWorker extends SwingWorker<List<JIPipeMergingDataBatch>, Object> implements JIPipeProgressInfo.StatusUpdatedEventListener {

        private final DataBatchAssistantUI assistantUI;
        private final JIPipeGraphNode algorithm;
        private final JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

        private DataBatchGeneratorWorker(DataBatchAssistantUI assistantUI, JIPipeGraphNode algorithm) {
            this.assistantUI = assistantUI;
            this.algorithm = algorithm;
            progressInfo.getStatusUpdatedEventEmitter().subscribeWeak(this);
        }

        @Override
        protected List<JIPipeMergingDataBatch> doInBackground() throws Exception {
            List<JIPipeMergingDataBatch> batches = ((JIPipeDataBatchAlgorithm) algorithm).generateDataBatchesDryRun(algorithm.getDataInputSlots(), progressInfo);
            if (batches == null)
                batches = Collections.emptyList();
            return batches;
        }

        @Override
        protected void done() {
            super.done();
            if (!isCancelled()) {
                try {
                    assistantUI.displayBatches(get(), algorithm);
                } catch (InterruptedException | ExecutionException e) {
                    assistantUI.displayBatches(Collections.emptyList(), algorithm);
                }
            }
        }

        @Override
        public void onProgressStatusUpdated(JIPipeProgressInfo.StatusUpdatedEvent event) {
            SwingUtilities.invokeLater(() -> {
                System.out.println("TODO: Update status");
//                assistantUI.batchPreviewNumberLabel.setText("Please wait ... " + event.getMessage());
            });
        }
    }
}
