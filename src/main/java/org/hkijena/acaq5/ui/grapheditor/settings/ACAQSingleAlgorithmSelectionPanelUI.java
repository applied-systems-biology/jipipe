package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.grouping.JsonAlgorithm;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.testbench.ACAQTestBenchSettings;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.cache.ACAQAlgorithmCacheBrowserUI;
import org.hkijena.acaq5.ui.compendium.ACAQAlgorithmCompendiumUI;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.ui.extensionbuilder.ACAQGraphWrapperAlgorithmExporter;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

/**
 * UI for a single {@link ACAQGraphNode}
 */
public class ACAQSingleAlgorithmSelectionPanelUI extends ACAQProjectWorkbenchPanel {
    private ACAQAlgorithmGraph graph;
    private ACAQAlgorithmGraphCanvasUI canvas;
    private ACAQGraphNode algorithm;
    private JPanel testBenchTabContent;
    private JPanel cacheBrowserTabContent;
    private DocumentTabPane tabbedPane;

    /**
     * @param workbenchUI the workbench UI
     * @param canvas      the graph
     * @param algorithm   the algorithm
     */
    public ACAQSingleAlgorithmSelectionPanelUI(ACAQProjectWorkbench workbenchUI, ACAQAlgorithmGraphCanvasUI canvas, ACAQGraphNode algorithm) {
        super(workbenchUI);
        this.graph = canvas.getAlgorithmGraph();
        this.canvas = canvas;
        this.algorithm = algorithm;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tabbedPane = new DocumentTabPane();

        ParameterPanel parametersUI = new ParameterPanel(getProjectWorkbench(),
                algorithm,
                TooltipUtils.getAlgorithmDocumentation(algorithm.getDeclaration()),
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SEARCH_BAR);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("cog.png"),
                parametersUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI slotEditorUI = new ACAQSlotEditorUI(algorithm);
        tabbedPane.addTab("Slots", UIUtils.getIconFromResources("plug.png"),
                slotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        {
            cacheBrowserTabContent = new JPanel(new BorderLayout());
            tabbedPane.addTab("Cache browser", UIUtils.getIconFromResources("database.png"),
                    cacheBrowserTabContent,
                    DocumentTabPane.CloseMode.withoutCloseButton,
                    false);
        }

        if (algorithm.getCategory() != ACAQAlgorithmCategory.Internal) {
            testBenchTabContent = new JPanel(new BorderLayout());
            tabbedPane.addTab("Quick run", UIUtils.getIconFromResources("play.png"),
                    testBenchTabContent,
                    DocumentTabPane.CloseMode.withoutCloseButton,
                    false);
        }


        add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.getTabbedPane().addChangeListener(e -> activateLazyContent(tabbedPane));

        initializeToolbar();
    }

    private void activateLazyContent(DocumentTabPane tabbedPane) {
        if (testBenchTabContent != null && tabbedPane.getCurrentContent() == testBenchTabContent) {
            if (testBenchTabContent.getComponentCount() == 0) {
                ACAQTestBenchSetupUI testBenchSetupUI = new ACAQTestBenchSetupUI(getProjectWorkbench(), algorithm);
                testBenchTabContent.add(testBenchSetupUI, BorderLayout.CENTER);
            }
        }
        if (cacheBrowserTabContent != null && tabbedPane.getCurrentContent() == cacheBrowserTabContent) {
            if (cacheBrowserTabContent.getComponentCount() == 0) {
                ACAQAlgorithmCacheBrowserUI browserUI = new ACAQAlgorithmCacheBrowserUI(getProjectWorkbench(), algorithm);
                cacheBrowserTabContent.add(browserUI, BorderLayout.CENTER);
            }
        }
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getDeclaration())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getDeclaration()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        if (canvas.getCopyPasteBehavior() != null) {
            JButton cutButton = new JButton(UIUtils.getIconFromResources("cut.png"));
            cutButton.setToolTipText("Cut");
            cutButton.setEnabled(algorithm.getCategory() != ACAQAlgorithmCategory.Internal);
            cutButton.addActionListener(e -> canvas.getCopyPasteBehavior().cut(Collections.singleton(algorithm)));
            toolBar.add(cutButton);

            JButton copyButton = new JButton(UIUtils.getIconFromResources("copy.png"));
            copyButton.setToolTipText("Copy");
            copyButton.setEnabled(algorithm.getCategory() != ACAQAlgorithmCategory.Internal);
            copyButton.addActionListener(e -> canvas.getCopyPasteBehavior().copy(Collections.singleton(algorithm)));
            toolBar.add(copyButton);
        }

        if(algorithm instanceof JsonAlgorithm) {
            JButton toGroupButton = new JButton(UIUtils.getIconFromResources("archive-extract.png"));
            toGroupButton.setToolTipText("Convert into group");
            toGroupButton.addActionListener(e -> unpackAlgorithm());
            toolBar.add(toGroupButton);
        }

        JButton exportButton = new JButton(UIUtils.getIconFromResources("export.png"));
        exportButton.setToolTipText("Export algorithm");
        exportButton.setEnabled(algorithm.getCategory() != ACAQAlgorithmCategory.Internal);
        exportButton.addActionListener(e -> exportAlgorithm());
        toolBar.add(exportButton);

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete algorithm");
        deleteButton.setEnabled(graph.canUserDelete(algorithm));
        deleteButton.addActionListener(e -> deleteAlgorithm());
        toolBar.add(deleteButton);

        if (ACAQAlgorithmRegistry.getInstance().getRegisteredAlgorithms().containsValue(algorithm.getDeclaration())) {
            JButton openCompendiumButton = new JButton(UIUtils.getIconFromResources("help.png"));
            openCompendiumButton.setToolTipText("Open in algorithm compendium");
            openCompendiumButton.addActionListener(e -> {
                ACAQAlgorithmCompendiumUI compendiumUI = new ACAQAlgorithmCompendiumUI();
                compendiumUI.selectItem(algorithm.getDeclaration());
                getWorkbench().getDocumentTabPane().addTab("Algorithm compendium",
                        UIUtils.getIconFromResources("help.png"),
                        compendiumUI,
                        DocumentTabPane.CloseMode.withSilentCloseButton,
                        true);
                getWorkbench().getDocumentTabPane().switchToLastTab();
            });
            toolBar.add(openCompendiumButton);
        }

        add(toolBar, BorderLayout.NORTH);
    }

    private void unpackAlgorithm() {
        if(algorithm instanceof JsonAlgorithm) {
            JsonAlgorithm.unpackToNodeGroup((JsonAlgorithm) algorithm);
        }
    }

    private void exportAlgorithm() {
        ACAQValidityReport report = new ACAQValidityReport();
        algorithm.reportValidity(report);
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report, false);
            return;
        }

        NodeGroup group;
        if(algorithm instanceof NodeGroup) {
            group = (NodeGroup) algorithm.duplicate();
        }
        else {
            ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph();
            graph.insertNode(algorithm.getDeclaration().clone(algorithm), ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
            group = new NodeGroup(graph, true);
        }

        ACAQGraphWrapperAlgorithmExporter.createExporter(getProjectWorkbench(), group, algorithm.getName(), algorithm.getCustomDescription());
    }

    private void deleteAlgorithm() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the algorithm '" + algorithm.getName() + "'?", "Delete algorithm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            graph.removeNode(algorithm);
        }
    }

    /**
     * @return the algorithm
     */
    public ACAQGraphNode getAlgorithm() {
        return algorithm;
    }

    /**
     * Activates and runs the quick run as automatically as possible.
     *
     * @param showResults show results after a successful run
     * @param showCache   show slot cache after a successful run
     */
    public void runTestBench(boolean showResults, boolean showCache) {
        // Activate the quick run
        tabbedPane.switchToContent(testBenchTabContent);
        ACAQTestBenchSetupUI testBenchSetupUI = (ACAQTestBenchSetupUI) testBenchTabContent.getComponent(0);
        boolean success = testBenchSetupUI.tryAutoRun(showResults, new ACAQTestBenchSettings(), testBench -> {
            if (showCache) {
                SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(cacheBrowserTabContent));
            }
        });
        if (!success) {
            SwingUtilities.invokeLater(() -> tabbedPane.switchToContent(testBenchTabContent));
        }
    }
}
