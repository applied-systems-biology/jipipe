package org.hkijena.acaq5.ui.extensionbuilder;

import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbenchPanel;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.extensionbuilder.grapheditor.ACAQJsonExtensionAlgorithmGraphUI;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * UI around a {@link GraphWrapperAlgorithmDeclaration}
 */
public class GraphWrapperAlgorithmDeclarationUI extends ACAQJsonExtensionWorkbenchPanel {

    private GraphWrapperAlgorithmDeclaration declaration;

    /**
     * @param workbenchUI the workbench
     * @param declaration the algorithm declaration
     */
    public GraphWrapperAlgorithmDeclarationUI(ACAQJsonExtensionWorkbench workbenchUI, GraphWrapperAlgorithmDeclaration declaration) {
        super(workbenchUI);
        this.declaration = declaration;

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        ParameterPanel parameterAccessUI = new ParameterPanel(getExtensionWorkbenchUI(), declaration,
                null, ParameterPanel.WITH_SCROLLING);
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
        for (DocumentTabPane.DocumentTab tab : getExtensionWorkbenchUI().getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof ACAQJsonExtensionAlgorithmGraphUI) {
                ACAQJsonExtensionAlgorithmGraphUI ui = (ACAQJsonExtensionAlgorithmGraphUI) tab.getContent();
                if (ui.getAlgorithmGraph() == declaration.getGraph()) {
                    getExtensionWorkbenchUI().getDocumentTabPane().switchToContent(ui);
                    return;
                }
            }
        }
        ACAQJsonExtensionAlgorithmGraphUI ui = new ACAQJsonExtensionAlgorithmGraphUI(getExtensionWorkbenchUI(), declaration.getGraph(), "");
        String name = StringUtils.orElse(declaration.getName(), "<Unnamed algorithm>");
        getExtensionWorkbenchUI().getDocumentTabPane().addTab(name, UIUtils.getIconFromResources("cog.png"),
                ui, DocumentTabPane.CloseMode.withSilentCloseButton, true);
        getExtensionWorkbenchUI().getDocumentTabPane().switchToLastTab();
    }

    private void deleteAlgorithm() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the algorithm '" + declaration.getName() + "'?", "Delete algorithm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            getProject().removeAlgorithm(declaration);
        }
    }

    public GraphWrapperAlgorithmDeclaration getDeclaration() {
        return declaration;
    }
}
