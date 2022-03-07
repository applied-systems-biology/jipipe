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

package org.hkijena.jipipe.ui.grapheditor.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatchAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.batchassistant.DataBatchAssistantUI;
import org.hkijena.jipipe.ui.cache.JIPipeAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.documentation.JIPipeAlgorithmCompendiumUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.quickrun.QuickRunSettings;
import org.hkijena.jipipe.ui.running.JIPipeRunQueuePanelUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * UI for a single {@link JIPipeGraphNode}
 */
public class JIPipeSingleAlgorithmSelectionPanelUI extends JIPipeProjectWorkbenchPanel {
    private static String SAVED_TAB = null;
    private final JIPipeGraphEditorUI graphEditorUI;
    private final JIPipeGraphCanvasUI canvas;
    private final JIPipeGraphNode algorithm;
    private JPanel testBenchTabContent;
    private JPanel cacheBrowserTabContent;
    private JPanel batchAssistantTabContent;
    private JPanel currentRunTabContent;
    private DocumentTabPane tabbedPane;

    /**
     * @param graphEditorUI the graph editor
     * @param algorithm     the algorithm
     */
    public JIPipeSingleAlgorithmSelectionPanelUI(JIPipeGraphEditorUI graphEditorUI, JIPipeGraphNode algorithm) {
        super((JIPipeProjectWorkbench) graphEditorUI.getWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.canvas = graphEditorUI.getCanvasUI();
        this.algorithm = algorithm;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tabbedPane = new DocumentTabPane();

        ParameterPanel parametersUI = new ParameterPanel(getProjectWorkbench(),
                algorithm,
                TooltipUtils.getAlgorithmDocumentation(algorithm),
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SEARCH_BAR);
        tabbedPane.registerSingletonTab("PARAMETERS", "Parameters", UIUtils.getIconFromResources("actions/configure.png"),
                () -> parametersUI, DocumentTabPane.CloseMode.withoutCloseButton, false);

        JIPipeSlotEditorUI slotEditorUI = new JIPipeSlotEditorUI(graphEditorUI, algorithm);
        tabbedPane.registerSingletonTab("SLOTS", "Slots", UIUtils.getIconFromResources("actions/plug.png"),
                () -> slotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton, false);

        if (algorithm.getGraph().getAttachment(JIPipeGraphType.class) == JIPipeGraphType.Project) {
            cacheBrowserTabContent = new JPanel(new BorderLayout());
            if (algorithm instanceof JIPipeAlgorithm) {
                tabbedPane.registerSingletonTab("CACHE_BROWSER", "Cache browser", UIUtils.getIconFromResources("actions/database.png"),
                        () -> cacheBrowserTabContent,
                        DocumentTabPane.CloseMode.withoutCloseButton, false);
            }
            if (algorithm instanceof JIPipeDataBatchAlgorithm) {
                batchAssistantTabContent = new JPanel(new BorderLayout());
                tabbedPane.registerSingletonTab("DATA_BATCHES", "Data batches", UIUtils.getIconFromResources("actions/package.png"),
                        () -> batchAssistantTabContent,
                        DocumentTabPane.CloseMode.withoutCloseButton, false);
            }

            testBenchTabContent = new JPanel(new BorderLayout());
            if (algorithm.getInfo().isRunnable()) {
                tabbedPane.registerSingletonTab("QUICK_RUN", "Quick run", UIUtils.getIconFromResources("actions/media-play.png"),
                        () -> testBenchTabContent,
                        DocumentTabPane.CloseMode.withoutCloseButton, false);
            }

            if (JIPipeRunnerQueue.getInstance().getCurrentRun() != null) {
                currentRunTabContent = new JPanel(new BorderLayout());
                tabbedPane.registerSingletonTab("CURRENT_RUN", "Current process", UIUtils.getIconFromResources("actions/show_log.png"),
                        () -> currentRunTabContent, DocumentTabPane.CloseMode.withoutCloseButton, false);
            }
        }

        add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.getTabbedPane().addChangeListener(e -> activateLazyContent(tabbedPane));
        restoreTabState();
        tabbedPane.getTabbedPane().addChangeListener(e -> saveTabState(tabbedPane));

        initializeToolbar();
    }

    private void restoreTabState() {
        if (SAVED_TAB == null)
            return;
        tabbedPane.selectSingletonTab(SAVED_TAB);
        activateLazyContent(tabbedPane);
    }

    private void saveTabState(DocumentTabPane tabbedPane) {
        SAVED_TAB = tabbedPane.getCurrentlySelectedSingletonTabId();
    }

    private void activateLazyContent(DocumentTabPane tabbedPane) {
        if (testBenchTabContent != null && tabbedPane.getCurrentContent() == testBenchTabContent) {
            if (testBenchTabContent.getComponentCount() == 0) {
                QuickRunSetupUI testBenchSetupUI = new QuickRunSetupUI(getProjectWorkbench(), algorithm);
                testBenchTabContent.add(testBenchSetupUI, BorderLayout.CENTER);
            }
        }
        if (cacheBrowserTabContent != null && tabbedPane.getCurrentContent() == cacheBrowserTabContent) {
            if (cacheBrowserTabContent.getComponentCount() == 0) {
                JIPipeAlgorithmCacheBrowserUI browserUI = new JIPipeAlgorithmCacheBrowserUI(getProjectWorkbench(),
                        algorithm,
                        graphEditorUI.getCanvasUI());
                cacheBrowserTabContent.add(browserUI, BorderLayout.CENTER);
            }
        }
        if (algorithm instanceof JIPipeDataBatchAlgorithm) {
            if (batchAssistantTabContent != null && tabbedPane.getCurrentContent() == batchAssistantTabContent) {
                if (batchAssistantTabContent.getComponentCount() == 0) {
                    DataBatchAssistantUI browserUI = new DataBatchAssistantUI(getProjectWorkbench(), algorithm,
                            () -> executeQuickRun(false,
                                    false,
                                    true,
                                    false,
                                    false,
                                    true));
                    batchAssistantTabContent.add(browserUI, BorderLayout.CENTER);
                }
            }
        }
        if (currentRunTabContent != null && tabbedPane.getCurrentContent() == currentRunTabContent) {
            currentRunTabContent.add(new JIPipeRunQueuePanelUI(), BorderLayout.CENTER);
        }
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(algorithm.getName(), new SolidColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getInfo())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getInfo()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(Collections.singleton(algorithm)),
                canvas.getContextActions(),
                canvas);

        if (JIPipe.getNodes().getRegisteredNodeInfos().containsValue(algorithm.getInfo())) {
            JButton openCompendiumButton = new JButton(UIUtils.getIconFromResources("actions/help.png"));
            UIUtils.makeFlat25x25(openCompendiumButton);
            openCompendiumButton.setToolTipText("Open in algorithm compendium");
            openCompendiumButton.addActionListener(e -> {
                JIPipeAlgorithmCompendiumUI compendiumUI = new JIPipeAlgorithmCompendiumUI();
                compendiumUI.selectItem(algorithm.getInfo());
                getWorkbench().getDocumentTabPane().addTab("Algorithm compendium",
                        UIUtils.getIconFromResources("actions/help.png"),
                        compendiumUI,
                        DocumentTabPane.CloseMode.withSilentCloseButton,
                        true);
                getWorkbench().getDocumentTabPane().switchToLastTab();
            });
            toolBar.add(openCompendiumButton);
        }

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * @return the algorithm
     */
    public JIPipeGraphNode getAlgorithm() {
        return algorithm;
    }

    /**
     * Activates and runs the quick run as automatically as possible.
     *
     * @param showResults              show results after a successful run
     * @param showCache                show slot cache after a successful run
     * @param showBatchAssistant       show batch assistant after a run
     * @param saveToDisk               if the run should save outputs
     * @param storeIntermediateOutputs if the run should store intermediate outputs
     * @param excludeSelected          if the current algorithm should be excluded
     */
    public void executeQuickRun(boolean showResults, boolean showCache, boolean showBatchAssistant, boolean saveToDisk, boolean storeIntermediateOutputs, boolean excludeSelected) {
        // Activate the quick run
        tabbedPane.switchToContent(testBenchTabContent);
        QuickRunSetupUI testBenchSetupUI = (QuickRunSetupUI) testBenchTabContent.getComponent(0);
        QuickRunSettings settings = new QuickRunSettings();
        settings.setSaveToDisk(saveToDisk);
        settings.setExcludeSelected(excludeSelected);
        settings.setStoreIntermediateResults(storeIntermediateOutputs);
        boolean success = testBenchSetupUI.tryAutoRun(showResults, settings, testBench -> {
            if (showCache) {
                SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(cacheBrowserTabContent));
            } else if (showBatchAssistant) {
                if (algorithm instanceof JIPipeDataBatchAlgorithm) {
                    SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(batchAssistantTabContent));
                }
            }
        });
        if (!success) {
            SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(testBenchTabContent));
        }
    }
}
