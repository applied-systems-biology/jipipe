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

package org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.properties;

import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.nodetemplate.NodeTemplateBox;
import org.hkijena.jipipe.extensions.nodetoolboxtool.NodeToolBox;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.batchassistant.DataBatchAssistantUI;
import org.hkijena.jipipe.ui.bookmarks.BookmarkListPanel;
import org.hkijena.jipipe.ui.cache.JIPipeAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.general.properties.JIPipeSlotEditorUI;
import org.hkijena.jipipe.ui.history.HistoryJournalUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.quickrun.QuickRunSettings;
import org.hkijena.jipipe.ui.running.JIPipeRunQueuePanelUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
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
public class JIPipePipelineSingleAlgorithmSelectionPanelUI extends JIPipeProjectWorkbenchPanel implements Disposable {
    private static String SAVED_TAB = null;
    private final JIPipeGraphEditorUI graphEditorUI;
    private final JIPipeGraphCanvasUI canvas;
    private final JIPipeGraphNode node;
    private JPanel testBenchTabContent;
    private JPanel cacheBrowserTabContent;
    private JPanel batchAssistantTabContent;
    private JPanel currentRunTabContent;
    private DocumentTabPane tabbedPane;
    private boolean disposed;

    /**
     * @param graphEditorUI the graph editor
     * @param node          the algorithm
     */
    public JIPipePipelineSingleAlgorithmSelectionPanelUI(JIPipeGraphEditorUI graphEditorUI, JIPipeGraphNode node) {
        super((JIPipeProjectWorkbench) graphEditorUI.getWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.canvas = graphEditorUI.getCanvasUI();
        this.node = node;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tabbedPane = new DocumentTabPane(false, DocumentTabPane.TabPlacement.Right);

        tabbedPane.registerSingletonTab("OVERVIEW", "Overview", UIUtils.getIcon32FromResources("actions/view-list-details.png"),
                () -> new JIPipePipelineSingleAlgorithmSelectionOverviewPanelUI(this), DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);

        tabbedPane.registerSingletonTab("PARAMETERS", "Parameters", UIUtils.getIcon32FromResources("actions/configure.png"),
                this::createParametersPanel, DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);

        if (node.getParentGraph().getAttachment(JIPipeGraphType.class) == JIPipeGraphType.Project) {

            JIPipeSlotEditorUI slotEditorUI = new JIPipeSlotEditorUI(graphEditorUI, node);
            tabbedPane.registerSingletonTab("SLOTS", "Slots", UIUtils.getIcon32FromResources("actions/plug.png"),
                    () -> slotEditorUI,
                    DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            if (node instanceof JIPipeIterationStepAlgorithm) {
                batchAssistantTabContent = new JPanel(new BorderLayout());
                tabbedPane.registerSingletonTab("DATA_BATCHES", "Input management", UIUtils.getIcon32FromResources("actions/extract-archive.png"),
                        () -> batchAssistantTabContent,
                        DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            }
            if (node instanceof JIPipeAlgorithm && !getProjectWorkbench().getProject().getNodeExamples(node.getInfo().getId()).isEmpty()) {
                tabbedPane.registerSingletonTab("EXAMPLES",
                        "Examples",
                        UIUtils.getIcon32FromResources("actions/graduation-cap.png"),
                        () -> new NodeExamplesUI(getProjectWorkbench(), (JIPipeAlgorithm) node, tabbedPane),
                        DocumentTabPane.CloseMode.withoutCloseButton,
                        DocumentTabPane.SingletonTabMode.Present);
            }

            testBenchTabContent = new JPanel(new BorderLayout());
            if (node.getInfo().isRunnable()) {
                tabbedPane.registerSingletonTab("QUICK_RUN", "Run", UIUtils.getIcon32FromResources("actions/media-play.png"),
                        () -> testBenchTabContent,
                        DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            }

            cacheBrowserTabContent = new JPanel(new BorderLayout());
            if (node instanceof JIPipeAlgorithm) {
                tabbedPane.registerSingletonTab("CACHE_BROWSER", "Results", UIUtils.getIcon32FromResources("actions/network-server-database.png"),
                        () -> cacheBrowserTabContent,
                        DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            }

            if (JIPipeRunnerQueue.getInstance().getCurrentRun() != null) {
                currentRunTabContent = new JPanel(new BorderLayout());
                tabbedPane.registerSingletonTab("CURRENT_RUN", "Progress", UIUtils.getIcon32FromResources("actions/rabbitvcs-show_log.png"),
                        () -> currentRunTabContent, DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            }
        } else {
            JIPipeSlotEditorUI slotEditorUI = new JIPipeSlotEditorUI(graphEditorUI, node);
            tabbedPane.registerSingletonTab("SLOTS", "Slots", UIUtils.getIcon32FromResources("actions/plug.png"),
                    () -> slotEditorUI,
                    DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            if (node instanceof JIPipeAlgorithm && !getProjectWorkbench().getProject().getNodeExamples(node.getInfo().getId()).isEmpty()) {
                tabbedPane.registerSingletonTab("EXAMPLES",
                        "Examples",
                        UIUtils.getIcon32FromResources("actions/graduation-cap.png"),
                        () -> new NodeExamplesUI(getProjectWorkbench(), (JIPipeAlgorithm) node, tabbedPane),
                        DocumentTabPane.CloseMode.withoutCloseButton,
                        DocumentTabPane.SingletonTabMode.Present);
            }
            if (node instanceof JIPipeIterationStepAlgorithm) {
                tabbedPane.addTab("Inputs",
                        UIUtils.getIcon32FromResources("actions/package.png"),
                        new ParameterPanel(getWorkbench(), ((JIPipeIterationStepAlgorithm) node).getGenerationSettingsInterface(), null, ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING),
                        DocumentTabPane.CloseMode.withoutCloseButton);
            }
        }

        // Additional tabs for the help panel
        tabbedPane.addTab("Add nodes", UIUtils.getIcon32FromResources("actions/node-add.png"),
                new NodeToolBox(getWorkbench(), true), DocumentTabPane.CloseMode.withoutCloseButton);

        tabbedPane.addTab("Templates", UIUtils.getIcon32FromResources("actions/star.png"),
                new NodeTemplateBox(getWorkbench(), true, canvas, Collections.singleton(node)), DocumentTabPane.CloseMode.withoutCloseButton);

        tabbedPane.addTab("Bookmarks", UIUtils.getIcon32FromResources("actions/bookmarks.png"),
                new BookmarkListPanel(getWorkbench(), graphEditorUI.getGraph(), graphEditorUI, Collections.singleton(node)), DocumentTabPane.CloseMode.withoutCloseButton);

        tabbedPane.addTab("History",
                UIUtils.getIcon32FromResources("actions/edit-undo-history.png"),
                new HistoryJournalUI(graphEditorUI.getHistoryJournal()),
                DocumentTabPane.CloseMode.withoutCloseButton);

        add(tabbedPane, BorderLayout.CENTER);


        // Lazy content
        tabbedPane.getTabbedPane().addChangeListener(e -> activateLazyContent(tabbedPane));
        restoreTabState();
        tabbedPane.getTabbedPane().addChangeListener(e -> saveTabState(tabbedPane));

//        initializeToolbar();
    }

    public JIPipeGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    public JIPipeGraphCanvasUI getCanvas() {
        return canvas;
    }

    public DocumentTabPane getTabbedPane() {
        return tabbedPane;
    }

    private JPanel createParametersPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Parameters
        ParameterPanel parametersUI = new ParameterPanel(getProjectWorkbench(),
                node,
                TooltipUtils.getAlgorithmDocumentation(node),
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SEARCH_BAR);
        panel.add(parametersUI, BorderLayout.CENTER);

//        JToolBar toolBar = new JToolBar();
//        toolBar.setFloatable(false);
//        panel.add(toolBar, BorderLayout.NORTH);

        if (node instanceof JIPipeParameterSlotAlgorithm) {

            JButton menuButton = new JButton("External", ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().isHasParameterSlot() ?
                    UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                    UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(menuButton);

            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Enable");
            toggle.setToolTipText("If enabled, the node will include an additional input 'Parameters' that receives parameter sets from an external source. " +
                    "If the parameter data contains multiple items, the node's workload will be repeated for each parameter set.");
            toggle.setSelected(((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().isHasParameterSlot());
            toggle.addActionListener(e -> {
                ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().setParameter("has-parameter-slot", toggle.isSelected());
                menuButton.setIcon( toggle.isSelected() ?
                        UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                        UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            });
            popupMenu.add(toggle);
            popupMenu.addSeparator();

            popupMenu.add(UIUtils.createMenuItem("Configure", "Configure external parameters", UIUtils.getIconFromResources("actions/configure.png"), () -> {
                ParameterPanel.showDialog(getWorkbench(),
                        ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings(),
                        MarkdownDocument.fromPluginResource("documentation/multi-parameters.md", Collections.emptyMap()),
                        "Configure external parameters",
                        ParameterPanel.DEFAULT_DIALOG_FLAGS);
            }));

            popupMenu.add(UIUtils.createMenuItem("What is this?", "Shows a help window", UIUtils.getIconFromResources("actions/help.png"), () -> {
                MarkdownReader.showDialog(MarkdownDocument.fromPluginResource("documentation/multi-parameters.md", Collections.emptyMap()),
                        true,
                        "About external parameters",
                        this,
                        false);
            }));

//            toolBar.add(menuButton);
            parametersUI.getToolBar().add(menuButton);
        }

        if (node instanceof JIPipeAdaptiveParametersAlgorithm) {

            JButton menuButton = new JButton("Adaptive",  ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings().isEnabled() ?
                    UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                    UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(menuButton);

            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Enable");
            toggle.setToolTipText("If enabled, the node will support parameters that are calculated by expressions.");
            toggle.setSelected(((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings().isEnabled());
            toggle.addActionListener(e -> {
                ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings().setParameter("enabled", toggle.isSelected());
                menuButton.setIcon( toggle.isSelected() ?
                        UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                        UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
            });
            popupMenu.add(toggle);
            popupMenu.addSeparator();

            popupMenu.add(UIUtils.createMenuItem("Configure", "Configure external parameters", UIUtils.getIconFromResources("actions/configure.png"), () -> {
                ParameterPanel.showDialog(getWorkbench(),
                        ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings(),
                        MarkdownDocument.fromPluginResource("documentation/adaptive-parameters.md", Collections.emptyMap()),
                        "Configure external parameters",
                        ParameterPanel.DEFAULT_DIALOG_FLAGS);
                node.getParameterUIChangedEventEmitter().emit(new JIPipeParameterCollection.ParameterUIChangedEvent(node));
            }));

            popupMenu.add(UIUtils.createMenuItem("What is this?", "Shows a help window", UIUtils.getIconFromResources("actions/help.png"), () -> {
                MarkdownReader.showDialog(MarkdownDocument.fromPluginResource("documentation/adaptive-parameters.md", Collections.emptyMap()),
                        true,
                        "About external parameters",
                        this,
                        false);
            }));

//            toolBar.add(menuButton);
            parametersUI.getToolBar().add(menuButton);
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

    private void saveTabState(DocumentTabPane tabbedPane) {
        if (!disposed) {
            String id = tabbedPane.getCurrentlySelectedSingletonTabId();
            if (id != null) {
                SAVED_TAB = id;
            }
        }
    }

    private void activateLazyContent(DocumentTabPane tabbedPane) {
        if (disposed)
            return;
        if (testBenchTabContent != null && tabbedPane.getCurrentContent() == testBenchTabContent) {
            if (testBenchTabContent.getComponentCount() == 0) {
                QuickRunSetupUI testBenchSetupUI = new QuickRunSetupUI(getProjectWorkbench(), node);
                testBenchTabContent.add(testBenchSetupUI, BorderLayout.CENTER);
            }
        }
        if (cacheBrowserTabContent != null && tabbedPane.getCurrentContent() == cacheBrowserTabContent) {
            if (cacheBrowserTabContent.getComponentCount() == 0) {
                JIPipeAlgorithmCacheBrowserUI browserUI = new JIPipeAlgorithmCacheBrowserUI(getProjectWorkbench(),
                        node,
                        graphEditorUI.getCanvasUI());
                cacheBrowserTabContent.add(browserUI, BorderLayout.CENTER);
            }
        }
        if (node instanceof JIPipeIterationStepAlgorithm) {
            if (batchAssistantTabContent != null && tabbedPane.getCurrentContent() == batchAssistantTabContent) {
                if (batchAssistantTabContent.getComponentCount() == 0) {
                    DataBatchAssistantUI browserUI = new DataBatchAssistantUI(getProjectWorkbench(), node,
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
        QuickRunSetupUI testBenchSetupUI = (QuickRunSetupUI) testBenchTabContent.getComponent(0);
        QuickRunSettings settings = new QuickRunSettings();
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
