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
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.JIPipeProjectCacheQuery;
import org.hkijena.jipipe.api.JIPipeProjectCacheState;
import org.hkijena.jipipe.api.annotation.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.batchassistant.DataBatchStatusData;
import org.hkijena.jipipe.extensions.strings.StringData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchAccess;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

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
public class DataBatchAssistantUI extends JIPipeProjectWorkbenchPanel {
    private final JIPipeAlgorithm algorithm;
    private final Runnable runTestBench;
    private final JIPipeParameterCollection batchSettings;
    private final Multimap<String, JIPipeDataSlot> currentCache = HashMultimap.create();
    AutoResizeSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, AutoResizeSplitPane.RATIO_1_TO_3);
    private JPanel batchPanel;
    private JPanel errorPanel;
    private JLabel errorLabel;
    private JLabel batchPreviewNumberLabel;
    private JLabel batchPreviewMissingLabel;
    private JLabel batchPreviewDuplicateLabel;
    private JIPipeGraphNode batchesNodeCopy;
    private boolean autoRefresh = true;
    private DataBatchGeneratorWorker lastWorker = null;
    private DataBatchTableUI2 batchTable;


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
        getProject().getCache().getEventBus().register(this);
        algorithm.getEventBus().register(this);
        algorithm.getGraph().getEventBus().register(this);
        batchSettings.getEventBus().register(this);
    }

    private void updateCurrentCache() {
        currentCache.clear();
        if (algorithm.getInputSlots().isEmpty()) {
            errorLabel.setText("No input slots");
            return;
        }
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(getProject());
        for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
            Set<JIPipeDataSlot> sourceSlots = algorithm.getGraph().getSourceSlots(inputSlot);
            if (!sourceSlots.isEmpty()) {
                for (JIPipeDataSlot sourceSlot : sourceSlots) {
                    Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> sourceCaches = getProject().getCache().extract(sourceSlot.getNode().getUUIDInGraph());
                    if (sourceCaches == null || sourceCaches.isEmpty()) {
                        errorLabel.setText("No cached data available");
                        currentCache.clear();
                        return;
                    }
                    Map<String, JIPipeDataSlot> sourceCache = sourceCaches.getOrDefault(query.getCachedId(sourceSlot.getNode().getUUIDInGraph()), null);
                    if (sourceCache != null) {
                        JIPipeDataSlot cache = sourceCache.getOrDefault(sourceSlot.getName(), null);
                        if (cache != null) {
                            currentCache.put(inputSlot.getName(), cache);
                        } else {
                            currentCache.clear();
                            errorLabel.setText("No up-to-date cached data available");
                            return;
                        }
                    } else {
                        currentCache.clear();
                        errorLabel.setText("No up-to-date cached data available");
                        return;
                    }
                }
            } else if (!inputSlot.getInfo().isOptional()) {
                currentCache.clear();
                errorLabel.setText("Unconnected input slots");
                return;
            }
        }
    }

    private void updateStatus() {
        updateCurrentCache();
        if (currentCache.isEmpty()) {
            switchToError();
        } else {
            refreshBatchPreview();
        }
    }

    private void switchToError() {
        splitPane.setBottomComponent(errorPanel);
        revalidate();
        repaint();
    }

    private void switchToBatches() {
        splitPane.setBottomComponent(batchPanel);
        revalidate();
        repaint();
    }

    private void refreshBatchPreview() {
        batchPreviewNumberLabel.setText("Please wait ...");

        // Stop worker
        if (lastWorker != null) {
            lastWorker.cancel(true);
            lastWorker = null;
        }

        batchesNodeCopy = algorithm.getInfo().duplicate(algorithm);
        // Pass cache as input slots
        for (JIPipeDataSlot inputSlot : batchesNodeCopy.getEffectiveInputSlots()) {
            for (JIPipeDataSlot cacheSlot : currentCache.get(inputSlot.getName())) {
                inputSlot.addData(cacheSlot, new JIPipeProgressInfo());
            }
        }
        // Generate dry-run
        batchTable.setDataTable(new JIPipeDataTable(JIPipeData.class));

        lastWorker = new DataBatchGeneratorWorker(this, batchesNodeCopy);
        lastWorker.execute();
    }

    private void displayBatches(List<JIPipeMergingDataBatch> batches, JIPipeGraphNode algorithm) {
        batchTable.setDataTable(new JIPipeDataTable(DataBatchStatusData.class));

        batchPreviewNumberLabel.setText(batches.size() + " batches");
        batchPreviewMissingLabel.setVisible(false);
        batchPreviewDuplicateLabel.setVisible(false);

        JIPipeDataTable dataTable = new JIPipeDataTable(JIPipeData.class);
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

        for (JIPipeMergingDataBatch batch : batches) {
            List<JIPipeTextAnnotation> textAnnotations = new ArrayList<>(batch.getMergedTextAnnotations().values());
            List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();

            boolean hasEmpty = false;
            boolean hasMultiple = false;
            DataBatchStatusData statusData = new DataBatchStatusData();

            for (JIPipeDataSlot inputSlot : algorithm.getEffectiveInputSlots()) {
                List<JIPipeData> dataList = batch.getInputData(inputSlot, JIPipeData.class, progressInfo);
                Map<String, Object> status = new HashMap<>();
                JIPipeData singletonData;
                if (dataList.isEmpty()) {
                    singletonData = new StringData("Empty");
                    hasEmpty = true;

                    status.put("Slot", inputSlot.getName());
                    status.put("Status", "Slot contains no data!");
                }
                else if(dataList.size() == 1) {
                    singletonData = dataList.get(0);

                    status.put("Slot", inputSlot.getName());
                    status.put("Status", "Contains 1 item.");
                }
                else {
                    JIPipeDataTable list = new JIPipeDataTable(JIPipeData.class);
                    for (JIPipeData datum : dataList) {
                        list.addData(datum, progressInfo);
                    }
                    singletonData = list;
                    hasMultiple = true;

                    status.put("Slot", inputSlot.getName());
                    status.put("Status", "Slot contains " + dataList.size() + " items");
                }

                statusData.getPerSlotStatus().addRow(status);
                dataAnnotations.add(new JIPipeDataAnnotation(inputSlot.getName(), singletonData));
            }

            if(hasEmpty)
                batchPreviewMissingLabel.setVisible(true);
            if(hasMultiple)
                batchPreviewDuplicateLabel.setVisible(true);
            statusData.setStatusValid(!hasEmpty);
            statusData.setStatusMessage(hasEmpty ? "Missing data!" : (hasMultiple ? "Multiple data per slot" : "One data per slot"));

            dataTable.addData(statusData,
                    textAnnotations,
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    dataAnnotations,
                    JIPipeDataAnnotationMergeMode.OverwriteExisting);
        }

        batchTable.setDataTable(dataTable);
        switchToBatches();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
        initializeToolBar();
        initializeParameterPanel();
        initializeBatchPanel();
        initializeErrorPanel();
    }

    private void initializeBatchPanel() {
        batchPanel = new JPanel(new BorderLayout());

        JToolBar batchPreviewOverview = new JToolBar();

        batchPreviewNumberLabel = new JLabel();
        batchPreviewOverview.add(batchPreviewNumberLabel);
        batchPreviewOverview.add(Box.createHorizontalGlue());

        batchPreviewMissingLabel = new JLabel("Missing items found!", UIUtils.getIconFromResources("emblems/warning.png"), JLabel.LEFT);
        batchPreviewOverview.add(batchPreviewMissingLabel);

        batchPreviewDuplicateLabel = new JLabel("Multiple items per batch", UIUtils.getIconFromResources("emblems/emblem-information.png"), JLabel.LEFT);
        batchPreviewOverview.add(batchPreviewDuplicateLabel);

        batchPreviewOverview.setFloatable(false);
        batchPanel.add(batchPreviewOverview, BorderLayout.NORTH);

        this.batchTable = new DataBatchTableUI2(getWorkbench(), new JIPipeDataTable(JIPipeData.class));
        batchPanel.add(batchTable, BorderLayout.CENTER);
    }

    private void initializeParameterPanel() {
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(),
                ((JIPipeDataBatchAlgorithm) algorithm).getGenerationSettingsInterface(),
                null,
                ParameterPanel.WITH_SCROLLING);
        splitPane.setTopComponent(parameterPanel);
    }

    private void initializeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton updateCacheButton = new JButton("Update cache", UIUtils.getIconFromResources("actions/database.png"));
        updateCacheButton.setToolTipText("Updates the data cache");
        updateCacheButton.addActionListener(e -> runTestBench.run());
        toolBar.add(updateCacheButton);
        add(toolBar, BorderLayout.NORTH);

        toolBar.add(Box.createHorizontalGlue());

        JButton refreshButton = new JButton("Refresh batches", UIUtils.getIconFromResources("actions/view-refresh.png"));
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
    }

    private void initializeErrorPanel() {
        errorPanel = new JPanel(new BorderLayout());
        errorLabel = new JLabel("No cached data available", UIUtils.getIcon64FromResources("no-data.png"), JLabel.LEFT);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setFont(errorLabel.getFont().deriveFont(20.0f));
        errorPanel.add(errorLabel, BorderLayout.NORTH);

        JTextArea explanation = UIUtils.makeReadonlyBorderlessTextArea("This tool can only work if it knows which metadata columns are available. " +
                "Such data is stored in the project-wide cache. You might need to generate or update the cache by clicking the 'Update cache' button at the top-right corner.");
        explanation.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        errorPanel.add(explanation, BorderLayout.CENTER);
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
        updateStatus();
    }

    /**
     * Triggered when the slots were changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onSlotsChanged(JIPipeGraph.NodeSlotsChangedEvent event) {
        if (!isDisplayable())
            return;
        updateStatus();
    }

    /**
     * Triggered when an algorithm input is disconnected
     *
     * @param event generated event
     */
    @Subscribe
    public void onDisconnected(JIPipeGraph.NodeDisconnectedEvent event) {
        if (event.getTarget().getNode() == algorithm) {
            updateStatus();
        }
    }

    @Subscribe
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (event.getSource() == batchSettings && autoRefresh) {
            refreshBatchPreview();
        }
    }

    private static class DataBatchGeneratorWorker extends SwingWorker<List<JIPipeMergingDataBatch>, Object> {

        private final DataBatchAssistantUI assistantUI;
        private final JIPipeGraphNode algorithm;
        private final JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

        private DataBatchGeneratorWorker(DataBatchAssistantUI assistantUI, JIPipeGraphNode algorithm) {
            this.assistantUI = assistantUI;
            this.algorithm = algorithm;
            progressInfo.getEventBus().register(this);
        }

        @Subscribe
        public void onProgressInfoStatusUpdate(JIPipeProgressInfo.StatusUpdatedEvent event) {
            SwingUtilities.invokeLater(() -> {
                assistantUI.batchPreviewNumberLabel.setText("Please wait ... " + event.getMessage());
            });
        }

        @Override
        protected List<JIPipeMergingDataBatch> doInBackground() throws Exception {
            List<JIPipeMergingDataBatch> batches = ((JIPipeDataBatchAlgorithm) algorithm).generateDataBatchesDryRun(algorithm.getEffectiveInputSlots(), progressInfo);
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
    }
}
