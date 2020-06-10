package org.hkijena.acaq5.ui.extensionbuilder.traiteditor;

import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbenchPanel;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQExistingTraitNode;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQNewTraitNode;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQTraitGraph;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQTraitNode;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQSlotEditorUI;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI for a single {@link ACAQTraitNode}
 */
public class ACAQSingleTraitSelectionPanelUI extends ACAQJsonExtensionWorkbenchPanel {
    private ACAQTraitNode node;
    private ACAQTraitGraph graph;

    /**
     * @param workbenchUI the workbench
     * @param node        the node
     * @param graph       the graph
     */
    public ACAQSingleTraitSelectionPanelUI(ACAQJsonExtensionWorkbench workbenchUI, ACAQTraitNode node, ACAQTraitGraph graph) {
        super(workbenchUI);
        this.node = node;
        this.graph = graph;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane();

        ParameterPanel parametersUI = new ParameterPanel(getExtensionWorkbenchUI(),
                node,
                MarkdownDocument.fromPluginResource("documentation/trait-graph.md"),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SCROLLING);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("cog.png"),
                parametersUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI algorithmSlotEditorUI = new ACAQSlotEditorUI(node);
        tabbedPane.addTab("Inheritances", UIUtils.getIconFromResources("connect.png"),
                algorithmSlotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(node.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(node.getDeclaration())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getTraitTooltip(node.getTraitDeclaration()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete annotation");
        deleteButton.addActionListener(e -> deleteNode());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void deleteNode() {
        if (node instanceof ACAQExistingTraitNode) {

        } else if (node instanceof ACAQNewTraitNode) {
            if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the annotation type '" + node.getName() + "'?\n",
                    "Delete annotation type", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                graph.removeNode(node);
            }
        }
    }

    /**
     * @return the trait node
     */
    public ACAQTraitNode getTraitNode() {
        return node;
    }
}
