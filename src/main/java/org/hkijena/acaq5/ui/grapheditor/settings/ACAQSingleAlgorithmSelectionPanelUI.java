package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.ACAQProjectUIPanel;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.settings.ACAQGraphWrapperAlgorithmExporter;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI for a single {@link ACAQAlgorithm}
 */
public class ACAQSingleAlgorithmSelectionPanelUI extends ACAQProjectUIPanel {
    private ACAQAlgorithmGraph graph;
    private ACAQAlgorithm algorithm;

    /**
     * @param workbenchUI the workbench UI
     * @param graph       the graph
     * @param algorithm   the algorithm
     */
    public ACAQSingleAlgorithmSelectionPanelUI(ACAQProjectUI workbenchUI, ACAQAlgorithmGraph graph, ACAQAlgorithm algorithm) {
        super(workbenchUI);
        this.graph = graph;
        this.algorithm = algorithm;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane();

        ACAQAlgorithmParametersUI parametersUI = new ACAQAlgorithmParametersUI(getWorkbenchUI(),
                algorithm,
                MarkdownDocument.fromPluginResource("documentation/algorithm-graph.md"),
                true, true);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("cog.png"),
                parametersUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI slotEditorUI = new ACAQSlotEditorUI(algorithm);
        tabbedPane.addTab("Slots", UIUtils.getIconFromResources("database.png"),
                slotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQTraitEditorUI traitEditorUI = new ACAQTraitEditorUI(algorithm);
        tabbedPane.addTab("Annotations", UIUtils.getIconFromResources("label.png"),
                traitEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        if (algorithm.getCategory() != ACAQAlgorithmCategory.Internal) {
            ACAQTestBenchSetupUI testBenchSetupUI = new ACAQTestBenchSetupUI(getWorkbenchUI(), algorithm, graph);
            tabbedPane.addTab("Testbench", UIUtils.getIconFromResources("testbench.png"),
                    testBenchSetupUI,
                    DocumentTabPane.CloseMode.withoutCloseButton,
                    false);
        }

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        JLabel nameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getDeclaration())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getDeclaration()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

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

        add(toolBar, BorderLayout.NORTH);
    }

    private void exportAlgorithm() {
        ACAQValidityReport report = new ACAQValidityReport();
        algorithm.reportValidity(report);
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report);
            return;
        }

        ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph();
        graph.insertNode(algorithm.getDeclaration().clone(algorithm), ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
        ACAQGraphWrapperAlgorithmExporter exporter = new ACAQGraphWrapperAlgorithmExporter(getWorkbenchUI(), graph);
        exporter.getAlgorithmDeclaration().getMetadata().setName(algorithm.getName());
        exporter.getAlgorithmDeclaration().getMetadata().setDescription(algorithm.getCustomDescription());
        getWorkbenchUI().getDocumentTabPane().addTab("Export algorithm '" + algorithm.getName() + "'",
                UIUtils.getIconFromResources("export.png"),
                exporter,
                DocumentTabPane.CloseMode.withAskOnCloseButton);
        getWorkbenchUI().getDocumentTabPane().switchToLastTab();
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
    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
