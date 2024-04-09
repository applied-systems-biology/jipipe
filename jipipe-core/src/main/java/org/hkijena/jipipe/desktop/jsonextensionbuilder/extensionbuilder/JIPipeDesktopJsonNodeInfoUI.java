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

package org.hkijena.jipipe.desktop.jsonextensionbuilder.extensionbuilder;

import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.JIPipeDesktopJsonExtensionWorkbench;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.JIPipeDesktopJsonExtensionWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

/**
 * UI around a {@link JsonNodeInfo}
 */
public class JIPipeDesktopJsonNodeInfoUI extends JIPipeDesktopJsonExtensionWorkbenchPanel {

    private JsonNodeInfo info;

    /**
     * @param workbenchUI the workbench
     * @param info        the algorithm info
     */
    public JIPipeDesktopJsonNodeInfoUI(JIPipeDesktopJsonExtensionWorkbench workbenchUI, JsonNodeInfo info) {
        super(workbenchUI);
        this.info = info;

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JIPipeDesktopFormPanel parameterEditor = new JIPipeDesktopFormPanel(MarkdownText.fromPluginResource("documentation/algorithm-extension.md", new HashMap<>()),
                JIPipeDesktopFormPanel.WITH_SCROLLING | JIPipeDesktopFormPanel.WITH_DOCUMENTATION);
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

    private void initializeParameterEditor(JIPipeDesktopFormPanel parameterEditor) {
        JIPipeDesktopParameterPanel infoParameterEditor = new JIPipeDesktopParameterPanel(getDesktopWorkbench(),
                info,
                null,
                JIPipeDesktopParameterPanel.NO_GROUP_HEADERS);
        JIPipeDesktopFormPanel.GroupHeaderPanel metadataHeader = parameterEditor.addGroupHeader("Algorithm metadata", UIUtils.getIconFromResources("actions/help-info.png"));
        metadataHeader.setDescription("Please provide following metadata:");
        parameterEditor.addWideToForm(infoParameterEditor, null);
        infoParameterEditor.getHoverHelpEventEmitter().subscribeLambda((emitter, event) -> {
            parameterEditor.getParameterHelp().setDocument(event.getDocument());
        });
        parameterEditor.addVerticalGlue();
    }

    private void editAlgorithm() {
        for (JIPipeDesktopTabPane.DocumentTab tab : getExtensionWorkbenchUI().getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof JIPipeDesktopJsonExtensionGraphUI) {
                JIPipeDesktopJsonExtensionGraphUI ui = (JIPipeDesktopJsonExtensionGraphUI) tab.getContent();
                if (ui.getGraph() == info.getGraph()) {
                    getExtensionWorkbenchUI().getDocumentTabPane().switchToContent(ui);
                    return;
                }
            }
        }
        JIPipeDesktopJsonExtensionGraphUI ui = new JIPipeDesktopJsonExtensionGraphUI(getExtensionWorkbenchUI(), info.getGraph(), null);
        String name = StringUtils.orElse(info.getName(), "<Unnamed algorithm>");
        getExtensionWorkbenchUI().getDocumentTabPane().addTab(name, UIUtils.getIconFromResources("actions/configure.png"),
                ui, JIPipeDesktopTabPane.CloseMode.withSilentCloseButton, true);
        getExtensionWorkbenchUI().getDocumentTabPane().switchToLastTab();
    }

    private void deleteAlgorithm() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the algorithm '" + info.getName() + "'?", "Delete algorithm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            getPluginProject().removeAlgorithm(info);
        }
    }

    public JsonNodeInfo getInfo() {
        return info;
    }
}
