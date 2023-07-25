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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.nodeexamples.JIPipeNodeExamplePickerDialog;
import org.hkijena.jipipe.extensions.nodetemplate.AddTemplateContextMenuAction;
import org.hkijena.jipipe.extensions.nodetemplate.NodeTemplateBox;
import org.hkijena.jipipe.extensions.nodetoolboxtool.NodeToolBox;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.batchassistant.DataBatchAssistantUI;
import org.hkijena.jipipe.ui.bookmarks.BookmarkListPanel;
import org.hkijena.jipipe.ui.cache.JIPipeAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.documentation.JIPipeAlgorithmCompendiumUI;
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
    private final JButton loadExampleButton = new JButton("Load example", UIUtils.getIconFromResources("actions/graduation-cap.png"));
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
        updateExampleButton();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tabbedPane = new DocumentTabPane(false);
        tabbedPane.registerSingletonTab("PARAMETERS", "Parameters", UIUtils.getIconFromResources("actions/configure.png"),
                this::createParametersPanel, DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);

        if (node.getParentGraph().getAttachment(JIPipeGraphType.class) == JIPipeGraphType.Project) {

            JIPipeSlotEditorUI slotEditorUI = new JIPipeSlotEditorUI(graphEditorUI, node);
            tabbedPane.registerSingletonTab("SLOTS", "Slots", UIUtils.getIconFromResources("actions/plug.png"),
                    () -> slotEditorUI,
                    DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            if (node instanceof JIPipeDataBatchAlgorithm) {
                batchAssistantTabContent = new JPanel(new BorderLayout());
                tabbedPane.registerSingletonTab("DATA_BATCHES", "Data batches", UIUtils.getIconFromResources("actions/package.png"),
                        () -> batchAssistantTabContent,
                        DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            }
            if (node instanceof JIPipeAlgorithm && !getProjectWorkbench().getProject().getNodeExamples(node.getInfo().getId()).isEmpty()) {
                tabbedPane.registerSingletonTab("EXAMPLES",
                        "Examples",
                        UIUtils.getIconFromResources("actions/graduation-cap.png"),
                        () -> new NodeExamplesUI(getProjectWorkbench(), (JIPipeAlgorithm) node),
                        DocumentTabPane.CloseMode.withoutCloseButton,
                        DocumentTabPane.SingletonTabMode.Present);
            }
            cacheBrowserTabContent = new JPanel(new BorderLayout());
            if (node instanceof JIPipeAlgorithm) {
                tabbedPane.registerSingletonTab("CACHE_BROWSER", "Cache browser", UIUtils.getIconFromResources("actions/database.png"),
                        () -> cacheBrowserTabContent,
                        DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            }

            testBenchTabContent = new JPanel(new BorderLayout());
            if (node.getInfo().isRunnable()) {
                tabbedPane.registerSingletonTab("QUICK_RUN", "Quick run", UIUtils.getIconFromResources("actions/media-play.png"),
                        () -> testBenchTabContent,
                        DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            }

            if (JIPipeRunnerQueue.getInstance().getCurrentRun() != null) {
                currentRunTabContent = new JPanel(new BorderLayout());
                tabbedPane.registerSingletonTab("CURRENT_RUN", "Current process", UIUtils.getIconFromResources("actions/show_log.png"),
                        () -> currentRunTabContent, DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            }
        } else {
            JIPipeSlotEditorUI slotEditorUI = new JIPipeSlotEditorUI(graphEditorUI, node);
            tabbedPane.registerSingletonTab("SLOTS", "Slots", UIUtils.getIconFromResources("actions/plug.png"),
                    () -> slotEditorUI,
                    DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
            if (node instanceof JIPipeAlgorithm && !getProjectWorkbench().getProject().getNodeExamples(node.getInfo().getId()).isEmpty()) {
                tabbedPane.registerSingletonTab("EXAMPLES",
                        "Examples",
                        UIUtils.getIconFromResources("actions/graduation-cap.png"),
                        () -> new NodeExamplesUI(getProjectWorkbench(), (JIPipeAlgorithm) node),
                        DocumentTabPane.CloseMode.withoutCloseButton,
                        DocumentTabPane.SingletonTabMode.Present);
            }
            if (node instanceof JIPipeDataBatchAlgorithm) {
                tabbedPane.addTab("Data batches",
                        UIUtils.getIconFromResources("actions/package.png"),
                        new ParameterPanel(getWorkbench(), ((JIPipeDataBatchAlgorithm) node).getGenerationSettingsInterface(), null, ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING),
                        DocumentTabPane.CloseMode.withoutCloseButton);
            }
        }

        add(tabbedPane, BorderLayout.CENTER);


        // Lazy content
        tabbedPane.getTabbedPane().addChangeListener(e -> activateLazyContent(tabbedPane));
        restoreTabState();
        tabbedPane.getTabbedPane().addChangeListener(e -> saveTabState(tabbedPane));

        initializeToolbar();
    }

    private JPanel createParametersPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Parameters
        ParameterPanel parametersUI = new ParameterPanel(getProjectWorkbench(),
                node,
                TooltipUtils.getAlgorithmDocumentation(node),
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.TABBED_DOCUMENTATION);
        panel.add(parametersUI, BorderLayout.CENTER);

        // Advanced parameters
        if ((node instanceof JIPipeParameterSlotAlgorithm) || (node instanceof JIPipeAdaptiveParametersAlgorithm)) {
            MessagePanel messagePanel = new MessagePanel();
            panel.add(messagePanel, BorderLayout.NORTH);
            if (node instanceof JIPipeParameterSlotAlgorithm) {
                JButton configureButton = new JButton("Configure", UIUtils.getIconFromResources("actions/configure.png"));
                configureButton.addActionListener(e -> {
                    ParameterPanel.showDialog(getWorkbench(),
                            ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings(),
                            MarkdownDocument.fromPluginResource("documentation/multi-parameters.md", Collections.emptyMap()),
                            "Configure external parameters",
                            ParameterPanel.DEFAULT_DIALOG_FLAGS);
                });

                JToggleButton toggleButton = new JToggleButton("Enable", UIUtils.getIconFromResources("data-types/parameters.png"));
                toggleButton.setToolTipText("If enabled, the node will include an additional input 'Parameters' that receives parameter sets from an external source. " +
                        "If the parameter data contains multiple items, the node's workload will be repeated for each parameter set.");
                toggleButton.setSelected(((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().isHasParameterSlot());
                toggleButton.addActionListener(e -> ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().setParameter("has-parameter-slot", toggleButton.isSelected()));

                JButton helpButton = new JButton(UIUtils.getIconFromResources("actions/help.png"));
                helpButton.addActionListener(e -> MarkdownReader.showDialog(MarkdownDocument.fromPluginResource("documentation/multi-parameters.md", Collections.emptyMap()), true, "About external parameters", this, false));
                UIUtils.makeFlat(helpButton);

                messagePanel.addMessage(MessagePanel.MessageType.Gray, "External parameters are supported", false, false, configureButton, toggleButton, helpButton);
            }
            if (node instanceof JIPipeAdaptiveParametersAlgorithm) {
                JButton configureButton = new JButton("Configure", UIUtils.getIconFromResources("actions/configure.png"));
                JButton helpButton = new JButton(UIUtils.getIconFromResources("actions/help.png"));
                helpButton.addActionListener(e -> MarkdownReader.showDialog(MarkdownDocument.fromPluginResource("documentation/adaptive-parameters.md", Collections.emptyMap()), true, "About external parameters", this, false));
                UIUtils.makeFlat(helpButton);

                configureButton.addActionListener(e -> {
                    ParameterPanel.showDialog(getWorkbench(),
                            ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings(),
                            MarkdownDocument.fromPluginResource("documentation/adaptive-parameters.md", Collections.emptyMap()),
                            "Configure external parameters",
                            ParameterPanel.DEFAULT_DIALOG_FLAGS);
                    node.getParameterUIChangedEventEmitter().emit(new JIPipeParameterCollection.ParameterUIChangedEvent(node));
                });

                messagePanel.addMessage(MessagePanel.MessageType.Gray, "Adaptive parameters are supported", false, false, configureButton, helpButton);
            }

        }

        // Additional tabs for the help panel
        parametersUI.getDocumentationTabPane().addTab("Available nodes", UIUtils.getIconFromResources("actions/graph-node-add.png"),
                new NodeToolBox(getWorkbench(), true), DocumentTabPane.CloseMode.withoutCloseButton);

        parametersUI.getDocumentationTabPane().addTab("Node templates", UIUtils.getIconFromResources("actions/favorite.png"),
                new NodeTemplateBox(getWorkbench(), true), DocumentTabPane.CloseMode.withoutCloseButton);

        parametersUI.getDocumentationTabPane().addTab("Bookmarks", UIUtils.getIconFromResources("actions/bookmarks.png"),
                new BookmarkListPanel(getWorkbench(), graphEditorUI.getGraph(), graphEditorUI), DocumentTabPane.CloseMode.withoutCloseButton);

        parametersUI.getDocumentationTabPane().addTab("Journal",
                UIUtils.getIconFromResources("actions/edit-undo-history.png"),
                new HistoryJournalUI(graphEditorUI.getHistoryJournal()),
                DocumentTabPane.CloseMode.withoutCloseButton);

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
        if (node instanceof JIPipeDataBatchAlgorithm) {
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

    private void updateExampleButton() {
        if (node instanceof JIPipeAlgorithm) {
            List<JIPipeNodeExample> nodeExamples = getProject().getNodeExamples(node.getInfo().getId());
            loadExampleButton.setVisible(!nodeExamples.isEmpty());
        } else {
            loadExampleButton.setVisible(false);
        }
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(node.getName(), new SolidColorIcon(16, 16, UIUtils.getFillColorFor(node.getInfo())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(node.getInfo()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(Collections.singleton(node)),
                canvas.getContextActions(),
                canvas);

        JButton createTemplateButton = new JButton("Create node template", UIUtils.getIconFromResources("actions/starred.png"));
        createTemplateButton.addActionListener(e -> createNodeTemplate());
        toolBar.add(createTemplateButton);

        loadExampleButton.addActionListener(e -> loadExample());
        toolBar.add(loadExampleButton);

        if (JIPipe.getNodes().getRegisteredNodeInfos().containsValue(node.getInfo())) {
            JButton openCompendiumButton = new JButton(UIUtils.getIconFromResources("actions/help.png"));
            UIUtils.makeFlat25x25(openCompendiumButton);
            openCompendiumButton.setToolTipText("Open in algorithm compendium");
            openCompendiumButton.addActionListener(e -> {
                JIPipeAlgorithmCompendiumUI compendiumUI = new JIPipeAlgorithmCompendiumUI();
                compendiumUI.selectItem(node.getInfo());
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

    private void loadExample() {
        JIPipeNodeExamplePickerDialog pickerDialog = new JIPipeNodeExamplePickerDialog(getWorkbench().getWindow());
        pickerDialog.setTitle("Load example");
        List<JIPipeNodeExample> nodeExamples = getProject().getNodeExamples(node.getInfo().getId());
        pickerDialog.setAvailableItems(nodeExamples);
        JIPipeNodeExample selection = pickerDialog.showDialog();
        if (selection != null) {
            ((JIPipeAlgorithm) node).loadExample(selection);
        }
    }

    private void createNodeTemplate() {
        AddTemplateContextMenuAction action = new AddTemplateContextMenuAction();
        action.run(canvas, canvas.getNodeUIsFor(Collections.singleton(node)));
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
                try {
                    SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(cacheBrowserTabContent));
                }
                catch (IllegalArgumentException ignored) {
                }
            } else if (showBatchAssistant) {
                if (node instanceof JIPipeDataBatchAlgorithm) {
                    SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(batchAssistantTabContent));
                }
            }
        });
        if (!success) {
            SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(testBenchTabContent));
        }
    }
}
