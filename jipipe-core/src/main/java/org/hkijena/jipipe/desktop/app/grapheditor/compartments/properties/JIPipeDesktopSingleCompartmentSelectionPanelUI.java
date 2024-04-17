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

package org.hkijena.jipipe.desktop.app.grapheditor.compartments.properties;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphNodeSlotEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.properties.JIPipeDesktopQuickRunSetupUI;
import org.hkijena.jipipe.desktop.app.history.JIPipeDesktopHistoryJournalUI;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueuePanelUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;

/**
 * UI for a single {@link JIPipeProjectCompartment}
 */
public class JIPipeDesktopSingleCompartmentSelectionPanelUI extends JIPipeDesktopProjectWorkbenchPanel implements Disposable {

    private static String SAVED_TAB = null;
    private final JIPipeProjectCompartment compartment;
    private final JIPipeDesktopGraphCanvasUI canvas;
    private final JIPipeDesktopGraphEditorUI graphEditorUI;

    private final JIPipeDesktopTabPane tabbedPane = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Right);
    private boolean disposed;

    /**
     * @param graphEditorUI the graph editor
     * @param compartment   the compartment
     */
    public JIPipeDesktopSingleCompartmentSelectionPanelUI(JIPipeDesktopGraphEditorUI graphEditorUI, JIPipeProjectCompartment compartment) {
        super((JIPipeDesktopProjectWorkbench) graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.compartment = compartment;
        this.canvas = graphEditorUI.getCanvasUI();
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JIPipeDesktopParameterPanel parametersUI = new JIPipeDesktopParameterPanel(getDesktopProjectWorkbench(),
                compartment,
                MarkdownText.fromPluginResource("documentation/compartment-graph.md", new HashMap<>()),
                JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.DOCUMENTATION_BELOW | JIPipeDesktopParameterPanel.WITH_SCROLLING);

        tabbedPane.registerSingletonTab("OVERVIEW", "Overview", UIUtils.getIcon32FromResources("actions/view-list-details.png"),
                () -> new JIPipeDesktopSingleCompartmentSelectionOverviewPanelUI(this), JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);

        tabbedPane.registerSingletonTab("PARAMETERS", "Parameters", UIUtils.getIcon32FromResources("actions/configure.png"),
                () -> parametersUI, JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);

        JIPipeDesktopGraphNodeSlotEditorUI compartmentSlotEditorUI = new JIPipeDesktopGraphNodeSlotEditorUI(graphEditorUI, compartment);
        tabbedPane.addTab("Connections", UIUtils.getIcon32FromResources("actions/plug.png"),
                compartmentSlotEditorUI,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                false);

        tabbedPane.registerSingletonTab("QUICK_RUN", "Run", UIUtils.getIcon32FromResources("actions/media-play.png"),
                this::createQuickRunPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);

        tabbedPane.registerSingletonTab("CACHE_BROWSER", "Results", UIUtils.getIcon32FromResources("actions/network-server-database.png"),
                this::createCacheBrowser,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);

        if (JIPipeRunnableQueue.getInstance().getCurrentRun() != null) {
            tabbedPane.registerSingletonTab("CURRENT_RUN", "Progress", UIUtils.getIcon32FromResources("actions/rabbitvcs-show_log.png"),
                    this::createCurrentRunInfo, JIPipeDesktopTabPane.CloseMode.withoutCloseButton, JIPipeDesktopTabPane.SingletonTabMode.Present);
        }

        // Bottom tabs
        tabbedPane.addTab("Bookmarks", UIUtils.getIcon32FromResources("actions/bookmarks.png"),
                new JIPipeDesktopBookmarkListPanel(getDesktopWorkbench(), getProject().getGraph(), graphEditorUI, Collections.singleton(compartment)), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        tabbedPane.addTab("History",
                UIUtils.getIcon32FromResources("actions/edit-undo-history.png"),
                new JIPipeDesktopHistoryJournalUI(graphEditorUI.getHistoryJournal()),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

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

    private void saveTabState(JIPipeDesktopTabPane tabbedPane) {
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

    public JIPipeDesktopGraphCanvasUI getCanvas() {
        return canvas;
    }

    public JIPipeDesktopGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    public JIPipeDesktopTabPane getTabbedPane() {
        return tabbedPane;
    }

    private Component createQuickRunPanel() {
        JIPipeCompartmentOutput node = compartment.getOutputNode();
        return new JIPipeDesktopQuickRunSetupUI(getDesktopProjectWorkbench(), node);
    }

    private Component createCurrentRunInfo() {
        return new JIPipeDesktopRunnableQueuePanelUI();
    }

    private Component createCacheBrowser() {
        JIPipeCompartmentOutput node = compartment.getOutputNode();
        return new JIPipeDesktopAlgorithmCacheBrowserUI(getDesktopProjectWorkbench(),
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
        getDesktopProjectWorkbench().getOrOpenPipelineEditorTab(compartment, true);
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
        JIPipeDesktopQuickRunSetupUI testBenchSetupUI = (JIPipeDesktopQuickRunSetupUI) tabbedPane.getCurrentContent();
        JIPipeDesktopQuickRunSettings settings = new JIPipeDesktopQuickRunSettings();
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