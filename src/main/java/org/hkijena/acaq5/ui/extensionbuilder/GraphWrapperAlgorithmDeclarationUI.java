package org.hkijena.acaq5.ui.extensionbuilder;

import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUI;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUIPanel;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.extensionbuilder.grapheditor.ACAQJsonExtensionAlgorithmGraphUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI around a {@link GraphWrapperAlgorithmDeclaration}
 */
public class GraphWrapperAlgorithmDeclarationUI extends ACAQJsonExtensionUIPanel {

    private GraphWrapperAlgorithmDeclaration declaration;

    /**
     * @param workbenchUI the workbench
     * @param declaration the algorithm declaration
     */
    public GraphWrapperAlgorithmDeclarationUI(ACAQJsonExtensionUI workbenchUI, GraphWrapperAlgorithmDeclaration declaration) {
        super(workbenchUI);
        this.declaration = declaration;

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        ACAQParameterAccessUI parameterAccessUI = new ACAQParameterAccessUI(getWorkbenchUI(), declaration,
                null, false, false);
        add(parameterAccessUI, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JLabel nameLabel = new JLabel(declaration.getName(), UIUtils.getIconFromResources("run.png"), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton editButton = new JButton("Edit algorithm", UIUtils.getIconFromResources("edit.png"));
        editButton.addActionListener(e -> editAlgorithm());
        toolBar.add(editButton);

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete algorithm");
        deleteButton.addActionListener(e -> deleteAlgorithm());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void editAlgorithm() {
        for (DocumentTabPane.DocumentTab tab : getWorkbenchUI().getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof ACAQJsonExtensionAlgorithmGraphUI) {
                ACAQJsonExtensionAlgorithmGraphUI ui = (ACAQJsonExtensionAlgorithmGraphUI) tab.getContent();
                if (ui.getAlgorithmGraph() == declaration.getGraph()) {
                    getWorkbenchUI().getDocumentTabPane().switchToContent(ui);
                    return;
                }
            }
        }
        ACAQJsonExtensionAlgorithmGraphUI ui = new ACAQJsonExtensionAlgorithmGraphUI(getWorkbenchUI(), declaration.getGraph(), "");
        getWorkbenchUI().getDocumentTabPane().addTab(declaration.getName(), UIUtils.getIconFromResources("cog.png"),
                ui, DocumentTabPane.CloseMode.withSilentCloseButton, true);
        getWorkbenchUI().getDocumentTabPane().switchToLastTab();
    }

    private void deleteAlgorithm() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the algorithm '" + declaration.getName() + "'?", "Delete algorithm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            getProject().removeAlgorithm(declaration);
        }
    }
}
