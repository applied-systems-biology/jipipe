/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.extensionbuilder;

import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.ui.JIPipeJsonExtensionWorkbench;
import org.hkijena.jipipe.ui.JIPipeJsonExtensionWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

/**
 * UI around a {@link JsonNodeInfo}
 */
public class JsonNodeInfoUI extends JIPipeJsonExtensionWorkbenchPanel {

    private JsonNodeInfo info;

    /**
     * @param workbenchUI the workbench
     * @param info        the algorithm info
     */
    public JsonNodeInfoUI(JIPipeJsonExtensionWorkbench workbenchUI, JsonNodeInfo info) {
        super(workbenchUI);
        this.info = info;

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        FormPanel parameterEditor = new FormPanel(MarkdownDocument.fromPluginResource("documentation/algorithm-extension.md", new HashMap<>()),
                FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION);
        initializeParameterEditor(parameterEditor);
        add(parameterEditor, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JLabel nameLabel = new JLabel(info.getName(), UIUtils.getIconFromResources("actions/run-build.png"), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(info));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton editButton = new JButton("Edit algorithm", UIUtils.getIconFromResources("actions/edit.png"));
        editButton.addActionListener(e -> editAlgorithm());
        toolBar.add(editButton);

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
        deleteButton.setToolTipText("Delete algorithm");
        deleteButton.addActionListener(e -> deleteAlgorithm());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void initializeParameterEditor(FormPanel parameterEditor) {
        ParameterPanel infoParameterEditor = new ParameterPanel(getWorkbench(),
                info,
                null,
                ParameterPanel.NO_GROUP_HEADERS);
        FormPanel.GroupHeaderPanel metadataHeader = parameterEditor.addGroupHeader("Algorithm metadata", UIUtils.getIconFromResources("actions/help-info.png"));
        metadataHeader.setDescription("Please provide following metadata:");
        parameterEditor.addWideToForm(infoParameterEditor, null);
        infoParameterEditor.getHoverHelpEventEmitter().subscribeLambda((emitter, event) -> {
            parameterEditor.getParameterHelp().setDocument(event.getDocument());
        });
        parameterEditor.addVerticalGlue();
    }

    private void editAlgorithm() {
        for (DocumentTabPane.DocumentTab tab : getExtensionWorkbenchUI().getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof JIPipeJsonExtensionGraphUI) {
                JIPipeJsonExtensionGraphUI ui = (JIPipeJsonExtensionGraphUI) tab.getContent();
                if (ui.getGraph() == info.getGraph()) {
                    getExtensionWorkbenchUI().getDocumentTabPane().switchToContent(ui);
                    return;
                }
            }
        }
        JIPipeJsonExtensionGraphUI ui = new JIPipeJsonExtensionGraphUI(getExtensionWorkbenchUI(), info.getGraph(), null);
        String name = StringUtils.orElse(info.getName(), "<Unnamed algorithm>");
        getExtensionWorkbenchUI().getDocumentTabPane().addTab(name, UIUtils.getIconFromResources("actions/configure.png"),
                ui, DocumentTabPane.CloseMode.withSilentCloseButton, true);
        getExtensionWorkbenchUI().getDocumentTabPane().switchToLastTab();
    }

    private void deleteAlgorithm() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the algorithm '" + info.getName() + "'?", "Delete algorithm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            getProject().removeAlgorithm(info);
        }
    }

    public JsonNodeInfo getInfo() {
        return info;
    }
}
