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

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.properties;

import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.nodes.JIPipeAdaptiveParametersAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.api.nodes.JIPipeDesktopNodeQuickAction;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.batchassistant.JIPipeDesktopDataBatchAssistantUI;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphNodeSlotEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel.JIPipeDesktopLegacyAddNodePanel;
import org.hkijena.jipipe.desktop.app.history.JIPipeDesktopHistoryJournalUI;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueuePanelUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplateBox;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * UI for a single {@link JIPipeGraphNode}
 */
public class JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI extends JIPipeDesktopProjectWorkbenchPanel implements Disposable {
    private static String SAVED_TAB = null;
    private final AbstractJIPipeDesktopGraphEditorUI graphEditorUI;
    private final JIPipeDesktopGraphCanvasUI canvas;
    private final JIPipeGraphNode node;
    private JPanel testBenchTabContent;
    private JPanel cacheBrowserTabContent;
    private JPanel batchAssistantTabContent;
    private JPanel currentRunTabContent;
    private JIPipeDesktopTabPane tabbedPane;
    private boolean disposed;

    /**
     * @param graphEditorUI the graph editor
     * @param node          the algorithm
     */
    public JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI(AbstractJIPipeDesktopGraphEditorUI graphEditorUI, JIPipeGraphNode node) {
        super((JIPipeDesktopProjectWorkbench) graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.canvas = graphEditorUI.getCanvasUI();
        this.node = node;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tabbedPane = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Right);

        tabbedPane.registerSingletonTab("OVERVIEW", "Overview", UIUtils.getIcon32FromResources("actions/view-list-details.png"),
                () -> new JIPipeDesktopPipelineSingleAlgorithmSelectionOverviewPanelUI(this), JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);

        tabbedPane.registerSingletonTab("PARAMETERS", "Parameters", UIUtils.getIcon32FromResources("actions/configure.png"),
                this::createParametersPanel, JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);

        if (node.getParentGraph().getAttachment(JIPipeGraphType.class) == JIPipeGraphType.Project) {

            JIPipeDesktopGraphNodeSlotEditorUI slotEditorUI = new JIPipeDesktopGraphNodeSlotEditorUI(graphEditorUI, node);
            tabbedPane.registerSingletonTab("SLOTS", "Slots", UIUtils.getIcon32FromResources("actions/plug.png"),
                    () -> slotEditorUI,
                    JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);
            if (node instanceof JIPipeIterationStepAlgorithm) {
                batchAssistantTabContent = new JPanel(new BorderLayout());
                tabbedPane.registerSingletonTab("DATA_BATCHES", "Input management", UIUtils.getIcon32FromResources("actions/extract-archive.png"),
                        () -> batchAssistantTabContent,
                        JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);
            }
//            if (node instanceof JIPipeAlgorithm && !getDesktopProjectWorkbench().getProject().getNodeExamples(node.getInfo().getId()).isEmpty()) {
//                tabbedPane.registerSingletonTab("EXAMPLES",
//                        "Examples",
//                        UIUtils.getIcon32FromResources("actions/graduation-cap.png"),
//                        () -> new JIPipeDesktopNodeExamplesUI(getDesktopProjectWorkbench(), (JIPipeAlgorithm) node, tabbedPane),
//                        JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
//                        JIPipeDesktopTabPane.SingletonTabMode.Present);
//            }

            testBenchTabContent = new JPanel(new BorderLayout());
            if (node.getInfo().isRunnable()) {
                tabbedPane.registerSingletonTab("QUICK_RUN", "Run", UIUtils.getIcon32FromResources("actions/media-play.png"),
                        () -> testBenchTabContent,
                        JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);
            }

            cacheBrowserTabContent = new JPanel(new BorderLayout());
            if (node instanceof JIPipeAlgorithm) {
                tabbedPane.registerSingletonTab("CACHE_BROWSER", "Results", UIUtils.getIcon32FromResources("actions/network-server-database.png"),
                        () -> cacheBrowserTabContent,
                        JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);
            }

            if (JIPipeRunnableQueue.getInstance().getCurrentRun() != null) {
                currentRunTabContent = new JPanel(new BorderLayout());
                tabbedPane.registerSingletonTab("CURRENT_RUN", "Progress", UIUtils.getIcon32FromResources("actions/new-command-alarm.png"),
                        () -> currentRunTabContent, JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);
            }
        } else {
            JIPipeDesktopGraphNodeSlotEditorUI slotEditorUI = new JIPipeDesktopGraphNodeSlotEditorUI(graphEditorUI, node);
            tabbedPane.registerSingletonTab("SLOTS", "Slots", UIUtils.getIcon32FromResources("actions/plug.png"),
                    () -> slotEditorUI,
                    JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);
//            if (node instanceof JIPipeAlgorithm && !getDesktopProjectWorkbench().getProject().getNodeExamples(node.getInfo().getId()).isEmpty()) {
//                tabbedPane.registerSingletonTab("EXAMPLES",
//                        "Examples",
//                        UIUtils.getIcon32FromResources("actions/graduation-cap.png"),
//                        () -> new JIPipeDesktopNodeExamplesUI(getDesktopProjectWorkbench(), (JIPipeAlgorithm) node, tabbedPane),
//                        JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
//                        JIPipeDesktopTabPane.SingletonTabMode.Present);
//            }
            if (node instanceof JIPipeIterationStepAlgorithm) {
                tabbedPane.addTab("Inputs",
                        UIUtils.getIcon32FromResources("actions/package.png"),
                        new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), ((JIPipeIterationStepAlgorithm) node).getGenerationSettingsInterface(), null, JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_SCROLLING),
                        JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
            }
        }

        // Additional tabs for the help panel
        tabbedPane.addTab("Add nodes", UIUtils.getIcon32FromResources("actions/node-add.png"),
                new JIPipeDesktopLegacyAddNodePanel(getDesktopWorkbench(), true), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        tabbedPane.addTab("Templates", UIUtils.getIcon32FromResources("actions/star.png"),
                new NodeTemplateBox(getDesktopWorkbench(), true, canvas, Collections.singleton(node)), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        tabbedPane.addTab("Bookmarks", UIUtils.getIcon32FromResources("actions/bookmarks.png"),
                new JIPipeDesktopBookmarkListPanel(getDesktopWorkbench(), graphEditorUI.getGraph(), graphEditorUI, Collections.singleton(node)), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        tabbedPane.addTab("History",
                UIUtils.getIcon32FromResources("actions/edit-undo-history.png"),
                new JIPipeDesktopHistoryJournalUI(graphEditorUI.getHistoryJournal()),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        add(tabbedPane, BorderLayout.CENTER);


        // Lazy content
        tabbedPane.getTabbedPane().addChangeListener(e -> activateLazyContent(tabbedPane));
        restoreTabState();
        tabbedPane.getTabbedPane().addChangeListener(e -> saveTabState(tabbedPane));

//        initializeToolbar();
    }

    public AbstractJIPipeDesktopGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    public JIPipeDesktopGraphCanvasUI getCanvas() {
        return canvas;
    }

    public JIPipeDesktopTabPane getTabbedPane() {
        return tabbedPane;
    }

    private JPanel createParametersPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Parameters
        JIPipeDesktopParameterFormPanel parametersUI = new JIPipeDesktopParameterFormPanel(getDesktopProjectWorkbench(),
                node,
                TooltipUtils.getAlgorithmDocumentation(node),
                JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.DOCUMENTATION_BELOW | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR);
        panel.add(parametersUI, BorderLayout.CENTER);

//        JToolBar toolBar = new JToolBar();
//        toolBar.setFloatable(false);
//        panel.add(toolBar, BorderLayout.NORTH);

        if (node instanceof JIPipeParameterSlotAlgorithm) {

            JButton menuButton = new JButton("External", ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().isHasParameterSlot() ?
                    UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                    UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(menuButton);

            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Enable external parameters");
            toggle.setToolTipText("If enabled, the node will include an additional input 'Parameters' that receives parameter sets from an external source. " +
                    "If the parameter data contains multiple items, the node's workload will be repeated for each parameter set.");
            toggle.setSelected(((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().isHasParameterSlot());
            toggle.addActionListener(e -> {
                ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().setParameter("has-parameter-slot", toggle.isSelected());
                menuButton.setIcon(toggle.isSelected() ?
                        UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                        UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            });
            popupMenu.add(toggle);
            popupMenu.addSeparator();

            popupMenu.add(UIUtils.createMenuItem("Configure", "Configure external parameters", UIUtils.getIconFromResources("actions/configure.png"), () -> {
                JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(),
                        ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings(),
                        MarkdownText.fromPluginResource("documentation/multi-parameters.md", Collections.emptyMap()),
                        "Configure external parameters",
                        JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS);
            }));

            popupMenu.add(UIUtils.createMenuItem("What is this?", "Shows a help window", UIUtils.getIconFromResources("actions/help.png"), () -> {
                JIPipeDesktopMarkdownReader.showDialog(MarkdownText.fromPluginResource("documentation/multi-parameters.md", Collections.emptyMap()),
                        true,
                        "About external parameters",
                        this,
                        false);
            }));

//            toolBar.add(menuButton);
            parametersUI.getToolBar().add(menuButton);
        }

        if (node instanceof JIPipeAdaptiveParametersAlgorithm) {

            JButton menuButton = new JButton("Adaptive", ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings().isEnabled() ?
                    UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                    UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(menuButton);

            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Enable adaptive parameters");
            toggle.setToolTipText("If enabled, the node will support parameters that are calculated by expressions.");
            toggle.setSelected(((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings().isEnabled());
            toggle.addActionListener(e -> {
                ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings().setParameter("enabled", toggle.isSelected());
                menuButton.setIcon(toggle.isSelected() ?
                        UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                        UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            });
            popupMenu.add(toggle);
            popupMenu.addSeparator();

            popupMenu.add(UIUtils.createMenuItem("Configure", "Configure external parameters", UIUtils.getIconFromResources("actions/configure.png"), () -> {
                JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(),
                        ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings(),
                        MarkdownText.fromPluginResource("documentation/adaptive-parameters.md", Collections.emptyMap()),
                        "Configure external parameters",
                        JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS);
                node.getParameterUIChangedEventEmitter().emit(new JIPipeParameterCollection.ParameterUIChangedEvent(node));
            }));

            popupMenu.add(UIUtils.createMenuItem("What is this?", "Shows a help window", UIUtils.getIconFromResources("actions/help.png"), () -> {
                JIPipeDesktopMarkdownReader.showDialog(MarkdownText.fromPluginResource("documentation/adaptive-parameters.md", Collections.emptyMap()),
                        true,
                        "About external parameters",
                        this,
                        false);
            }));

//            toolBar.add(menuButton);
            parametersUI.getToolBar().add(menuButton);
        }

        // Create quick action menu
        List<JIPipeDesktopNodeQuickAction> quickActions = JIPipeDesktopNodeQuickAction.getQuickActions(node);
        if (!quickActions.isEmpty()) {
            JPopupMenu quickActionsMenu = new JPopupMenu();
            for (JIPipeDesktopNodeQuickAction quickAction : quickActions) {
                quickActionsMenu.add(UIUtils.createMenuItem(quickAction.getName(),
                        quickAction.getDescription(),
                        UIUtils.getIconFromResources(quickAction.getIcon()),
                        () -> quickAction.run(node, canvas)));
            }
            JButton button = UIUtils.createButton("Tools", UIUtils.getIconFromResources("actions/quickopen.png"), null);
            UIUtils.makeButtonHighlightedSuccess(button);
            UIUtils.addPopupMenuToButton(button, quickActionsMenu);

            parametersUI.getToolBar().add(button);
        }

        return panel;
    }

    @Override
    public void dispose() {
        disposed = true;
        UIUtils.removeAllWithDispose(this);
    }

    private void restoreTabState() {
        if (SAVED_TAB == null)
            return;
        tabbedPane.selectSingletonTab(SAVED_TAB);
        activateLazyContent(tabbedPane);
    }

    private void saveTabState(JIPipeDesktopTabPane tabbedPane) {
        if (!disposed) {
            String id = tabbedPane.getCurrentlySelectedSingletonTabId();
            if (id != null) {
                SAVED_TAB = id;
            }
        }
    }

    private void activateLazyContent(JIPipeDesktopTabPane tabbedPane) {
        if (disposed)
            return;
        if (testBenchTabContent != null && tabbedPane.getCurrentContent() == testBenchTabContent) {
            if (testBenchTabContent.getComponentCount() == 0) {
                JIPipeDesktopQuickRunSetupUI testBenchSetupUI = new JIPipeDesktopQuickRunSetupUI(getDesktopProjectWorkbench(), node);
                testBenchTabContent.add(testBenchSetupUI, BorderLayout.CENTER);
            }
        }
        if (cacheBrowserTabContent != null && tabbedPane.getCurrentContent() == cacheBrowserTabContent) {
            if (cacheBrowserTabContent.getComponentCount() == 0) {
                JIPipeDesktopAlgorithmCacheBrowserUI browserUI = new JIPipeDesktopAlgorithmCacheBrowserUI(getDesktopProjectWorkbench(),
                        node,
                        graphEditorUI.getCanvasUI());
                cacheBrowserTabContent.add(browserUI, BorderLayout.CENTER);
            }
        }
        if (node instanceof JIPipeIterationStepAlgorithm) {
            if (batchAssistantTabContent != null && tabbedPane.getCurrentContent() == batchAssistantTabContent) {
                if (batchAssistantTabContent.getComponentCount() == 0) {
                    JIPipeDesktopDataBatchAssistantUI browserUI = new JIPipeDesktopDataBatchAssistantUI(getDesktopProjectWorkbench(), node,
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
            currentRunTabContent.add(new JIPipeDesktopRunnableQueuePanelUI(), BorderLayout.CENTER);
        }
    }


    /**
     * @return the algorithm
     */
    public JIPipeGraphNode getNode() {
        return node;
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
        activateLazyContent(tabbedPane);
        JIPipeDesktopQuickRunSetupUI testBenchSetupUI = (JIPipeDesktopQuickRunSetupUI) testBenchTabContent.getComponent(0);
        JIPipeDesktopQuickRunSettings settings = new JIPipeDesktopQuickRunSettings(getProject());
        settings.setSaveToDisk(saveToDisk);
        settings.setExcludeSelected(excludeSelected);
        settings.setStoreIntermediateResults(storeIntermediateOutputs);
        boolean success = testBenchSetupUI.tryAutoRun(showResults, settings, testBench -> {
            if (showCache) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        tabbedPane.switchToContent(cacheBrowserTabContent);
                    } catch (IllegalArgumentException ignored) {
                    }
                });

            } else if (showBatchAssistant) {
                if (node instanceof JIPipeIterationStepAlgorithm) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            tabbedPane.switchToContent(batchAssistantTabContent);
                        } catch (IllegalArgumentException ignored) {
                        }
                    });
                }
            }
        });
        if (!success) {
            SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(testBenchTabContent));
        }
    }
}
