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

import com.google.common.eventbus.Subscribe;
import gnu.trove.set.TIntSet;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.events.NodeDisconnectedEvent;
import org.hkijena.jipipe.api.events.NodeSlotsChangedEvent;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tool that assists the user in configuring batch generation for
 * {@link org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm} and
 * {@link org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm}
 */
public class DataBatchAssistantUI extends JIPipeProjectWorkbenchPanel {
    private final JIPipeAlgorithm algorithm;
    private final Runnable runTestBench;
    private JPanel errorUI;
    private Map<String, JIPipeDataSlot> currentCache = new HashMap<>();
    private JLabel errorLabel;
    private FormPanel batchPreviewPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

    /**
     * @param workbenchUI  The workbench UI
     * @param algorithm    the target algorithm
     * @param runTestBench function that updates the cache
     */
    public DataBatchAssistantUI(JIPipeProjectWorkbench workbenchUI, JIPipeGraphNode algorithm, Runnable runTestBench) {
        super(workbenchUI);
        this.algorithm = (JIPipeAlgorithm) algorithm;
        this.runTestBench = runTestBench;
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
        List<JIPipeGraphNode> traversedAlgorithms = algorithm.getGraph().traverseAlgorithms();
        for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
            JIPipeDataSlot sourceSlot = algorithm.getGraph().getSourceSlot(inputSlot);
            if (sourceSlot != null) {
                Map<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> sourceCaches = getProject().getCache().extract((JIPipeAlgorithm) sourceSlot.getNode());
                if (sourceCaches == null || sourceCaches.isEmpty()) {
                    errorLabel.setText("No cached data available");
                    currentCache.clear();
                    return;
                }
                Map<String, JIPipeDataSlot> sourceCache = sourceCaches.getOrDefault(getProject().getStateIdOf((JIPipeAlgorithm) sourceSlot.getNode(),
                        traversedAlgorithms), null);
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
        splitPane.setBottomComponent(batchPreviewPanel);

        refreshBatchPreview();
    }

    private void refreshBatchPreview() {
        batchPreviewPanel.clear();
        JIPipeGraphNode copy = algorithm.getInfo().clone(algorithm);
        if (copy instanceof JIPipeParameterSlotAlgorithm)
            ((JIPipeParameterSlotAlgorithm) copy).getParameterSlotAlgorithmSettings().setHasParameterSlot(false);
        // Pass cache as input slots
        for (JIPipeDataSlot inputSlot : copy.getInputSlots()) {
            JIPipeDataSlot cache = currentCache.getOrDefault(inputSlot.getName(), null);
            inputSlot.copyFrom(cache);
        }
        // Generate dry-run
        JIPipeDataBatchAlgorithm batchAlgorithm = (JIPipeDataBatchAlgorithm) copy;
        Map<JIPipeDataBatchKey, Map<String, TIntSet>> groups = batchAlgorithm.groupDataByMetadata(copy.getInputSlotMap());
        List<JIPipeMergingDataBatch> batches = batchAlgorithm.generateDataBatchesDryRun(groups);

        for (JIPipeMergingDataBatch batch : batches) {
            DataBatchUI ui = new DataBatchUI(getProjectWorkbench(), copy, batch);
            batchPreviewPanel.addWideToForm(ui, null);
        }
        batchPreviewPanel.addVerticalGlue();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeErrorUI();
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

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when the slots were changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onSlotsChanged(NodeSlotsChangedEvent event) {
        updateStatus();
    }

    /**
     * Triggered when an algorithm input is disconnected
     *
     * @param event generated event
     */
    @Subscribe
    public void onDisconnected(NodeDisconnectedEvent event) {
        if (event.getTarget().getNode() == algorithm) {
            updateStatus();
        }
    }
}
