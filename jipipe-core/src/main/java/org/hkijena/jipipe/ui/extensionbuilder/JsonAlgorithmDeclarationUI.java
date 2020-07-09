/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.extensionbuilder;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.grouping.JsonAlgorithmDeclaration;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParametersUI;
import org.hkijena.jipipe.ui.JIPipeJsonExtensionWorkbench;
import org.hkijena.jipipe.ui.JIPipeJsonExtensionWorkbenchPanel;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI around a {@link JsonAlgorithmDeclaration}
 */
public class JsonAlgorithmDeclarationUI extends JIPipeJsonExtensionWorkbenchPanel {

    private JsonAlgorithmDeclaration declaration;

    /**
     * @param workbenchUI the workbench
     * @param declaration the algorithm declaration
     */
    public JsonAlgorithmDeclarationUI(JIPipeJsonExtensionWorkbench workbenchUI, JsonAlgorithmDeclaration declaration) {
        super(workbenchUI);
        this.declaration = declaration;

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        FormPanel parameterEditor = new FormPanel(MarkdownDocument.fromPluginResource("documentation/algorithm-extension.md"),
                FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION);
        initializeParameterEditor(parameterEditor);
        add(parameterEditor, BorderLayout.CENTER);

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

    private void initializeParameterEditor(FormPanel parameterEditor) {
        ParameterPanel declarationParameterEditor = new ParameterPanel(getWorkbench(),
                declaration,
                null,
                ParameterPanel.NO_GROUP_HEADERS);
        FormPanel.GroupHeaderPanel metadataHeader = parameterEditor.addGroupHeader("Algorithm metadata", UIUtils.getIconFromResources("info.png"));
        metadataHeader.setDescription("Please provide following metadata:");
        parameterEditor.addWideToForm(declarationParameterEditor, null);
        declarationParameterEditor.getEventBus().register(new Object() {
            @Subscribe
            public void onHoverHelp(FormPanel.HoverHelpEvent event) {
                parameterEditor.getParameterHelp().setDocument(event.getDocument());
            }
        });

        FormPanel.GroupHeaderPanel parameterHeader = parameterEditor.addGroupHeader("Exported parameters", UIUtils.getIconFromResources("parameters.png"));
        parameterHeader.setDescription("You can use the following settings to export parameters that then can be changed by users. Parameters are organized in groups " +
                "with a customizable name and description. You can either manually define groups or add all available parameters of a selected algorithm. " +
                "If you want to edit the parameter default values, you can find them in 'Edit algorithm'.");
        GraphNodeParametersUI exportedParametersEditor = new GraphNodeParametersUI(getWorkbench(), declaration.getExportedParameters());
        exportedParametersEditor.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        parameterEditor.addWideToForm(exportedParametersEditor, null);

        parameterEditor.addVerticalGlue();
    }

    private void editAlgorithm() {
        for (DocumentTabPane.DocumentTab tab : getExtensionWorkbenchUI().getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof JIPipeJsonExtensionGraphUI) {
                JIPipeJsonExtensionGraphUI ui = (JIPipeJsonExtensionGraphUI) tab.getContent();
                if (ui.getAlgorithmGraph() == declaration.getGraph()) {
                    getExtensionWorkbenchUI().getDocumentTabPane().switchToContent(ui);
                    return;
                }
            }
        }
        JIPipeJsonExtensionGraphUI ui = new JIPipeJsonExtensionGraphUI(getExtensionWorkbenchUI(), declaration.getGraph(), "");
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

    public JsonAlgorithmDeclaration getDeclaration() {
        return declaration;
    }
}
