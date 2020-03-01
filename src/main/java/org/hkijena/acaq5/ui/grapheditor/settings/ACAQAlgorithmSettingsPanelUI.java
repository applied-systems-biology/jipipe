package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ACAQAlgorithmSettingsPanelUI extends ACAQUIPanel {
    private ACAQAlgorithmGraph graph;
    private ACAQAlgorithm algorithm;

    public ACAQAlgorithmSettingsPanelUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithmGraph graph, ACAQAlgorithm algorithm) {
        super(workbenchUI);
        this.graph = graph;
        this.algorithm = algorithm;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane();

        ACAQAlgorithmParametersUI parametersUI = new ACAQAlgorithmParametersUI(algorithm,
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

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete algorithm");
        deleteButton.setEnabled(graph.canUserDelete(algorithm));
        deleteButton.addActionListener(e -> deleteAlgorithm());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void deleteAlgorithm() {
        graph.removeNode(algorithm);
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
