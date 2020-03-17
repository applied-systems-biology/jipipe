package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.settings.ACAQGraphWrapperAlgorithmExporter;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.stream.Collectors;

public class ACAQMultiAlgorithmSelectionPanelUI extends ACAQUIPanel {
    private ACAQAlgorithmGraph graph;
    private Set<ACAQAlgorithm> algorithms;

    public ACAQMultiAlgorithmSelectionPanelUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithmGraph graph, Set<ACAQAlgorithm> algorithms) {
        super(workbenchUI);
        this.graph = graph;
        this.algorithms = algorithms;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        JLabel nameLabel = new JLabel(algorithms.size() + " algorithms", UIUtils.getIconFromResources("select.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton exportButton = new JButton(UIUtils.getIconFromResources("export.png"));
        exportButton.setToolTipText("Export custom algorithm");
        exportButton.setEnabled(algorithms.stream().allMatch(algorithm -> algorithm.getCategory() != ACAQAlgorithmCategory.Internal));
        exportButton.addActionListener(e -> exportAlgorithms());
        toolBar.add(exportButton);

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete algorithms");
        deleteButton.setEnabled(algorithms.stream().allMatch(algorithm -> graph.canUserDelete(algorithm)));
        deleteButton.addActionListener(e -> deleteAlgorithms());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void exportAlgorithms() {
        ACAQValidityReport report = new ACAQValidityReport();
        for (ACAQAlgorithm algorithm : algorithms) {
            algorithm.reportValidity(report.forCategory(algorithm.getName()));
        }
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report);
            return;
        }

        ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph();
        for (ACAQAlgorithm algorithm : algorithms) {
            if (algorithm.getCategory() == ACAQAlgorithmCategory.Internal)
                continue;
            graph.insertNode(algorithm.getIdInGraph(), algorithm.getDeclaration().clone(algorithm), ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
        }
        ACAQGraphWrapperAlgorithmExporter exporter = new ACAQGraphWrapperAlgorithmExporter(getWorkbenchUI(), graph);
        getWorkbenchUI().getDocumentTabPane().addTab("Export custom algorithm",
                UIUtils.getIconFromResources("export.png"),
                exporter,
                DocumentTabPane.CloseMode.withAskOnCloseButton);
        getWorkbenchUI().getDocumentTabPane().switchToLastTab();
    }

    private void deleteAlgorithms() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the following algorithms: " +
                        algorithms.stream().map(a -> "'" + a.getName() + "'").collect(Collectors.joining(", "))
                        + "?", "Delete algorithm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            for (ACAQAlgorithm algorithm : algorithms) {
                graph.removeNode(algorithm);
            }
        }
    }
}
