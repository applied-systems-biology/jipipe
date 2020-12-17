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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatchAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A tool that assists the user in configuring batch generation for
 * {@link org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm} and
 * {@link org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm}
 */
public class DataBatchAssistantUI extends JIPipeProjectWorkbenchPanel {
    private final JIPipeAlgorithm algorithm;
    private final Runnable runTestBench;
    private final ArrayDeque<JIPipeMergingDataBatch> infiniteScrollingQueue = new ArrayDeque<>();
    private JPanel errorUI;
    private Multimap<String, JIPipeDataSlot> currentCache = HashMultimap.create();
    private JLabel errorLabel;
    private FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private final Timer scrollToBeginTimer = new Timer(200, e -> scrollToBeginning());
    private JLabel batchPreviewNumberLabel;
    private JLabel batchPreviewMissingLabel;
    private JLabel batchPreviewDuplicateLabel;
    private JIPipeGraphNode batchesNodeCopy;
    private List<JIPipeMergingDataBatch> batches = new ArrayList<>();


    /**
     * @param workbenchUI  The workbench UI
     * @param algorithm    the target algorithm
     * @param runTestBench function that updates the cache
     */
    public DataBatchAssistantUI(JIPipeProjectWorkbench workbenchUI, JIPipeGraphNode algorithm, Runnable runTestBench) {
        super(workbenchUI);
        this.algorithm = (JIPipeAlgorithm) algorithm;
        this.runTestBench = runTestBench;
        this.scrollToBeginTimer.setRepeats(false);
        initialize();
        updateStatus();
        getProject().getCache().getEventBus().register(this);
        algorithm.getEventBus().register(this);
        algorithm.getGraph().getEventBus().register(this);
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
                    Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> sourceCaches = getProject().getCache().extract(sourceSlot.getNode());
                    if (sourceCaches == null || sourceCaches.isEmpty()) {
                        errorLabel.setText("No cached data available");
                        currentCache.clear();
                        return;
                    }
                    Map<String, JIPipeDataSlot> sourceCache = sourceCaches.getOrDefault(query.getCachedId(sourceSlot.getNode()), null);
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
            } else {
                currentCache.clear();
                errorLabel.setText("Unconnected input slots");
                return;
            }
        }
    }

    private void updateStatus() {
        updateCurrentCache();
        if (currentCache.isEmpty()) {
            removeAll();
            add(errorUI, BorderLayout.CENTER);
            revalidate();
            repaint();
        } else {
            removeAll();
            switchToEditor();
            revalidate();
            repaint();
        }
    }

    private void switchToEditor() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton refreshButton = new JButton("Refresh batches", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.setToolTipText("Refreshes the preview of generated batches");
        refreshButton.addActionListener(e -> refreshBatchPreview());
        toolBar.add(refreshButton);

        add(toolBar, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
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

        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(),
                ((JIPipeDataBatchAlgorithm) algorithm).getGenerationSettingsInterface(),
                null,
                ParameterPanel.WITH_SCROLLING);
        splitPane.setTopComponent(parameterPanel);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(formPanel, BorderLayout.CENTER);

        JToolBar batchPreviewOverview = new JToolBar();

        batchPreviewNumberLabel = new JLabel();
        batchPreviewOverview.add(batchPreviewNumberLabel);
        batchPreviewOverview.add(Box.createHorizontalGlue());

        batchPreviewMissingLabel = new JLabel("Missing items found!", UIUtils.getIconFromResources("emblems/warning.png"), JLabel.LEFT);
        batchPreviewOverview.add(batchPreviewMissingLabel);

        batchPreviewDuplicateLabel = new JLabel("Multiple items per group", UIUtils.getIconFromResources("emblems/emblem-information.png"), JLabel.LEFT);
        batchPreviewOverview.add(batchPreviewDuplicateLabel);

        batchPreviewOverview.setFloatable(false);
        bottomPanel.add(batchPreviewOverview, BorderLayout.NORTH);

        splitPane.setBottomComponent(bottomPanel);

        refreshBatchPreview();
    }

    private void refreshBatchPreview() {
        infiniteScrollingQueue.clear();
        formPanel.clear();
        batchesNodeCopy = algorithm.getInfo().duplicate(algorithm);
        // Pass cache as input slots
        for (JIPipeDataSlot inputSlot : batchesNodeCopy.getEffectiveInputSlots()) {
            for (JIPipeDataSlot cacheSlot : currentCache.get(inputSlot.getName())) {
                inputSlot.addData(cacheSlot, new JIPipeProgressInfo());
            }
        }
        // Generate dry-run
        JIPipeDataBatchAlgorithm batchAlgorithm = (JIPipeDataBatchAlgorithm) batchesNodeCopy;
        batches = batchAlgorithm.generateDataBatchesDryRun(batchesNodeCopy.getEffectiveInputSlots());

        batchPreviewNumberLabel.setText(batches.size() + " batches");
        batchPreviewMissingLabel.setVisible(false);
        batchPreviewDuplicateLabel.setVisible(false);
        for (JIPipeMergingDataBatch batch : batches) {
            for (JIPipeDataSlot inputSlot : batchesNodeCopy.getInputSlots()) {
                List<JIPipeData> data = batch.getInputData(inputSlot, JIPipeData.class, new JIPipeProgressInfo());
                if (data.isEmpty())
                    batchPreviewMissingLabel.setVisible(true);
                if (data.size() > 1)
                    batchPreviewDuplicateLabel.setVisible(true);
            }
            infiniteScrollingQueue.addLast(batch);
        }
//        for (JIPipeMergingDataBatch batch : batches) {
//            DataBatchUI ui = new DataBatchUI(getProjectWorkbench(), copy, batch);
//            batchPreviewPanel.addWideToForm(ui, null);
//        }
        formPanel.addVerticalGlue();
        SwingUtilities.invokeLater(this::updateInfiniteScroll);
        scrollToBeginTimer.restart();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeErrorUI();
        formPanel.getScrollPane().getVerticalScrollBar().addAdjustmentListener(e -> {
            updateInfiniteScroll();
        });
    }

    private void initializeErrorUI() {
        errorUI = new JPanel(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());
        JButton updateCacheButton = new JButton("Update cache", UIUtils.getIconFromResources("actions/database.png"));
        updateCacheButton.setToolTipText("Updates the data cache, so this tool can be used");
        updateCacheButton.addActionListener(e -> runTestBench.run());
        toolBar.add(updateCacheButton);
        errorUI.add(toolBar, BorderLayout.NORTH);

        JPanel errorContent = new JPanel(new BorderLayout());
        errorLabel = new JLabel("No cached data available", UIUtils.getIcon64FromResources("no-data.png"), JLabel.LEFT);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setFont(errorLabel.getFont().deriveFont(20.0f));
        errorContent.add(errorLabel, BorderLayout.NORTH);

        JTextArea explanation = UIUtils.makeReadonlyBorderlessTextArea("This tool can only work if it knows which metadata columns are available. " +
                "Such data is stored in the project-wide cache. You might need to generate or update the cache by clicking the 'Update cache' button at the top-right corner.");
        explanation.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        errorContent.add(explanation, BorderLayout.CENTER);
        errorUI.add(errorContent, BorderLayout.CENTER);
    }

    private void scrollToBeginning() {
        formPanel.getScrollPane().getVerticalScrollBar().setValue(0);
    }

    private void updateInfiniteScroll() {
        JScrollBar scrollBar = formPanel.getScrollPane().getVerticalScrollBar();
        if ((!scrollBar.isVisible() || (scrollBar.getValue() + scrollBar.getVisibleAmount()) > (scrollBar.getMaximum() - 32)) && !infiniteScrollingQueue.isEmpty()) {
            formPanel.removeLastRow();
            JIPipeMergingDataBatch dataBatch = infiniteScrollingQueue.removeFirst();
            DataBatchUI ui = new DataBatchUI(getProjectWorkbench(), batchesNodeCopy, dataBatch);
            formPanel.addWideToForm(ui, null);
            formPanel.addVerticalGlue();
            formPanel.revalidate();
            formPanel.repaint();
            SwingUtilities.invokeLater(this::updateInfiniteScroll);
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
}
