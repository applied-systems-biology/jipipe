package org.hkijena.acaq5.ui.extensionbuilder.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbenchPanel;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQSlotEditorUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQTraitEditorUI;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.Collections;

/**
 * Shown when one algorithm is selected
 */
public class ACAQJsonExtensionSingleAlgorithmSelectionPanelUI extends ACAQJsonExtensionWorkbenchPanel {
    private ACAQAlgorithmGraph graph;
    private ACAQAlgorithmGraphCanvasUI canvas;
    private ACAQAlgorithm algorithm;

    /**
     * @param workbenchUI The workbench UI
     * @param canvas      The algorithm graph
     * @param algorithm   The algorithm
     */
    public ACAQJsonExtensionSingleAlgorithmSelectionPanelUI(ACAQJsonExtensionWorkbench workbenchUI, ACAQAlgorithmGraphCanvasUI canvas, ACAQAlgorithm algorithm) {
        super(workbenchUI);
        this.graph = canvas.getAlgorithmGraph();
        this.canvas = canvas;
        this.algorithm = algorithm;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane();

        ParameterPanel parametersUI = new ParameterPanel(getExtensionWorkbenchUI(),
                algorithm,
                TooltipUtils.getAlgorithmDocumentation(algorithm.getDeclaration()),
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW);
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

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
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

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete algorithm");
        deleteButton.setEnabled(graph.canUserDelete(algorithm));
        deleteButton.addActionListener(e -> deleteAlgorithm());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void deleteAlgorithm() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the algorithm '" + algorithm.getName() + "'?", "Delete algorithm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            graph.removeNode(algorithm);
        }
    }

    /**
     * @return The algorithm
     */
    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
