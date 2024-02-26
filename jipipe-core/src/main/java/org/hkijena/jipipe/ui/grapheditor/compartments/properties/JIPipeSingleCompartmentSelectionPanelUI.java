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

package org.hkijena.jipipe.ui.grapheditor.compartments.properties;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.bookmarks.BookmarkListPanel;
import org.hkijena.jipipe.ui.cache.JIPipeAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.properties.QuickRunSetupUI;
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
import java.util.HashMap;

/**
 * UI for a single {@link JIPipeProjectCompartment}
 */
public class JIPipeSingleCompartmentSelectionPanelUI extends JIPipeProjectWorkbenchPanel implements Disposable {

    private static String SAVED_TAB = null;
    private final JIPipeProjectCompartment compartment;
    private final JIPipeGraphCanvasUI canvas;
    private final JIPipeGraphEditorUI graphEditorUI;

    private final DocumentTabPane tabbedPane = new DocumentTabPane(false, DocumentTabPane.TabPlacement.Right);
    private boolean disposed;

    /**
     * @param graphEditorUI the graph editor
     * @param compartment   the compartment
     */
    public JIPipeSingleCompartmentSelectionPanelUI(JIPipeGraphEditorUI graphEditorUI, JIPipeProjectCompartment compartment) {
        super((JIPipeProjectWorkbench) graphEditorUI.getWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.compartment = compartment;
        this.canvas = graphEditorUI.getCanvasUI();
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        ParameterPanel parametersUI = new ParameterPanel(getProjectWorkbench(),
                compartment,
                MarkdownDocument.fromPluginResource("documentation/compartment-graph.md", new HashMap<>()),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SCROLLING);

        tabbedPane.registerSingletonTab("OVERVIEW", "Overview", UIUtils.getIcon32FromResources("actions/view-list-details.png"),
                () -> new JIPipeSingleCompartmentSelectionOverviewPanelUI(this), DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);

        tabbedPane.registerSingletonTab("PARAMETERS","Parameters", UIUtils.getIcon32FromResources("actions/configure2.png"),
                () -> parametersUI, DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);

        JIPipeSlotEditorUI compartmentSlotEditorUI = new JIPipeSlotEditorUI(graphEditorUI, compartment);
        tabbedPane.addTab("Connections", UIUtils.getIcon32FromResources("actions/plug2.png"),
                compartmentSlotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        tabbedPane.registerSingletonTab("QUICK_RUN", "Run", UIUtils.getIcon32FromResources("actions/media-play.png"),
                this::createQuickRunPanel,
                DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);

        tabbedPane.registerSingletonTab("CACHE_BROWSER", "Results", UIUtils.getIcon32FromResources("actions/network-server-database.png"),
                this::createCacheBrowser,
                DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);

        if (JIPipeRunnerQueue.getInstance().getCurrentRun() != null) {
            tabbedPane.registerSingletonTab("CURRENT_RUN", "Progress", UIUtils.getIcon32FromResources("actions/rabbitvcs-show_log.png"),
                    this::createCurrentRunInfo, DocumentTabPane.CloseMode.withoutCloseButton, DocumentTabPane.SingletonTabMode.Present);
        }

        // Bottom tabs
        tabbedPane.addTab("Bookmarks", UIUtils.getIcon32FromResources("actions/bookmarks.png"),
                new BookmarkListPanel(getWorkbench(), getProject().getGraph(), graphEditorUI, Collections.singleton(compartment)), DocumentTabPane.CloseMode.withoutCloseButton);

        tabbedPane.addTab("History",
                UIUtils.getIcon32FromResources("actions/edit-undo-history.png"),
                new HistoryJournalUI(graphEditorUI.getHistoryJournal()),
                DocumentTabPane.CloseMode.withoutCloseButton);

        add(tabbedPane, BorderLayout.CENTER);

        restoreTabState();
        tabbedPane.getTabbedPane().addChangeListener(e -> saveTabState(tabbedPane));

//        initializeToolbar();
    }

    private void restoreTabState() {
        if (SAVED_TAB == null)
            return;
        tabbedPane.selectSingletonTab(SAVED_TAB);
    }

    private void saveTabState(DocumentTabPane tabbedPane) {
        if (!disposed) {
            String id = tabbedPane.getCurrentlySelectedSingletonTabId();
            if (id != null) {
                SAVED_TAB = id;
            }
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        UIUtils.removeAllWithDispose(this);
    }

    public JIPipeGraphCanvasUI getCanvas() {
        return canvas;
    }

    public JIPipeGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    public DocumentTabPane getTabbedPane() {
        return tabbedPane;
    }

    private Component createQuickRunPanel() {
        JIPipeCompartmentOutput node = compartment.getOutputNode();
        return new QuickRunSetupUI(getProjectWorkbench(), node);
    }

    private Component createCurrentRunInfo() {
        return new JIPipeRunQueuePanelUI();
    }

    private Component createCacheBrowser() {
        JIPipeCompartmentOutput node = compartment.getOutputNode();
        return new JIPipeAlgorithmCacheBrowserUI(getProjectWorkbench(),
                node,
                graphEditorUI.getCanvasUI());
    }

//    private void initializeToolbar() {
//        JToolBar toolBar = new JToolBar();
//        toolBar.setFloatable(false);
//        JLabel nameLabel = new JLabel(compartment.getName(), new SolidColorIcon(16, 16, UIUtils.getFillColorFor(compartment.getInfo())), JLabel.LEFT);
//        nameLabel.setToolTipText(TooltipUtils.getProjectCompartmentTooltip(compartment, getProject().getGraph()));
//        toolBar.add(nameLabel);
//
//        toolBar.add(Box.createHorizontalGlue());
//
//        JIPipeGraphEditorUI.installContextActionsInto(toolBar,
//                canvas.getNodeUIsFor(Collections.singleton(compartment)),
//                canvas.getContextActions(),
//                canvas);
//
//        JButton openButton = new JButton("Open in editor", UIUtils.getIconFromResources("actions/edit.png"));
//        openButton.addActionListener(e -> openInEditor());
//        toolBar.add(openButton);
//
//        add(toolBar, BorderLayout.NORTH);
//    }

    private void openInEditor() {
        getProjectWorkbench().getOrOpenPipelineEditorTab(compartment, true);
    }

    public JIPipeProjectCompartment getCompartment() {
        return compartment;
    }

    /**
     * Activates and runs the quick run as automatically as possible.
     *
     * @param showResults              show results after a successful run
     * @param showCache                show slot cache after a successful run
     * @param saveToDisk               if the run should save outputs
     * @param storeIntermediateOutputs if the run should store intermediate outputs
     * @param excludeSelected          if the current algorithm should be excluded
     */
    public void executeQuickRun(boolean showResults, boolean showCache, boolean saveToDisk, boolean storeIntermediateOutputs, boolean excludeSelected) {
        if (compartment.getOutputNode().getOutputSlots().isEmpty()) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), "<html>The output node of the compartment '" + compartment.getName() + "' transfers no data.<br/>" +
                    "This means that the there will be no outputs generated for the compartment.<br/><br/>" +
                    "Please edit the compartment via a double-click and add inputs into the '" + compartment.getOutputNode().getName() + "' node.</html>", "No outputs to generate", JOptionPane.WARNING_MESSAGE);
        }
        // Activate the quick run
        tabbedPane.selectSingletonTab("QUICK_RUN");
        QuickRunSetupUI testBenchSetupUI = (QuickRunSetupUI) tabbedPane.getCurrentContent();
        QuickRunSettings settings = new QuickRunSettings();
        settings.setSaveToDisk(saveToDisk);
        settings.setExcludeSelected(excludeSelected);
        settings.setStoreIntermediateResults(storeIntermediateOutputs);
        boolean success = testBenchSetupUI.tryAutoRun(showResults, settings, testBench -> {
            if (showCache) {
                SwingUtilities.invokeLater(() -> tabbedPane.selectSingletonTab("CACHE_BROWSER"));
            }
        });
        if (!success) {
            SwingUtilities.invokeLater(() -> tabbedPane.selectSingletonTab("QUICK_RUN"));
        }
    }
}
