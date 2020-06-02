package org.hkijena.acaq5.ui.extensionbuilder;

import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbenchPanel;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * UI around an {@link ACAQJsonTraitDeclaration}
 */
public class ACAQJsonTraitDeclarationUI extends ACAQJsonExtensionWorkbenchPanel {

    private ACAQJsonTraitDeclaration declaration;

    /**
     * @param workbenchUI The workbench UI
     * @param declaration The declaration
     */
    public ACAQJsonTraitDeclarationUI(ACAQJsonExtensionWorkbench workbenchUI, ACAQJsonTraitDeclaration declaration) {
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
        nameLabel.setToolTipText(TooltipUtils.getTraitTooltip(declaration));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton editButton = new JButton("Edit annotation", UIUtils.getIconFromResources("edit.png"));
        editButton.addActionListener(e -> editTraitGraph());
        toolBar.add(editButton);

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete annotation");
        deleteButton.addActionListener(e -> deleteAnnotation());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void deleteAnnotation() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the annotation '" + declaration.getName() + "'?", "Delete annotation",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            getProject().removeAnnotation(declaration);
        }
    }

    private void editTraitGraph() {
        getExtensionWorkbenchUI().getDocumentTabPane().selectSingletonTab("TRAIT_GRAPH");
    }
}
