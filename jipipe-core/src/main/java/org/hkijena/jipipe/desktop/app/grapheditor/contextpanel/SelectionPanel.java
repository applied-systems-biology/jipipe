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

package org.hkijena.jipipe.desktop.app.grapheditor.contextpanel;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.GraphInteractiveObjectUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeControlPointUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;

public class SelectionPanel extends JIPipeDesktopGraphEditorContextPanelIsland {

    private final JIPipeDesktopFormPanel formPanel;

    public SelectionPanel(JIPipeDesktopGraphEditorUI graphEditorUI) {
        super(graphEditorUI);
        this.formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);
    }

    @Override
    public void initializeContent() {
        super.initializeContent();
        getContentPanel().add(formPanel, BorderLayout.CENTER);

        List<JIPipeDesktopGraphInteractiveObjectUI> nodeUIs = getGraphEditorUI().getSelectionManager().getSelection().stream().filter(sel -> sel instanceof JIPipeDesktopGraphNodeUI).toList();
        List<JIPipeDesktopGraphInteractiveObjectUI> edgeUIs = getGraphEditorUI().getSelectionManager().getSelection().stream().filter(sel -> sel instanceof JIPipeDesktopGraphEdgeUI).toList();
        List<JIPipeDesktopGraphInteractiveObjectUI> controlPointUIs = getGraphEditorUI().getSelectionManager().getSelection().stream().filter(sel -> sel instanceof JIPipeDesktopGraphEdgeControlPointUI).toList();

        createSelectionSpecifier(nodeUIs, "Nodes");
        createSelectionSpecifier(edgeUIs, "Edges");
        createSelectionSpecifier(controlPointUIs, "Edge control points");

        formPanel.addWideToForm(UIUtils.createHorizontalFillingSeparator());

        // Common actions
        for (GraphInteractiveObjectUIContextAction contextAction : getGraphEditorUI().getCanvasUI().getContextActions()) {
            if (contextAction != null && contextAction.isDisplayedInToolbar() && contextAction.matches(getGraphEditorUI().getSelectionManager().getSelection())) {
                JButton button = UIUtils.createLeftAlignedButton(contextAction.getName(), contextAction.getIcon(), () -> {
                    contextAction.run(getGraphEditorUI().getCanvasUI(), getGraphEditorUI().getSelectionManager().getSelection());
                    getGraphEditorUI().rebuildContextPanel();
                });
                button.setToolTipText(contextAction.getDescription());
                formPanel.addWideToForm(button);
            }
        }

        // More menu
        JButton moreButton = new JButton("More ...");
        moreButton.setHorizontalAlignment(SwingConstants.LEFT);
        UIUtils.addPopupMenuToButton(moreButton, getGraphEditorUI().getCanvasUI().createContextMenu());
        formPanel.addWideToForm(moreButton);

        formPanel.addWideToForm(Box.createVerticalStrut(8));
        formPanel.addWideToForm(UIUtils.createLeftAlignedButton("Clear selection", JIPipe.RESOURCES.getIcon16("actions/message-close.png"), () -> {
            getGraphEditorUI().getSelectionManager().clearSelection();
        }));
    }

    private void createSelectionSpecifier(List<JIPipeDesktopGraphInteractiveObjectUI> uiList, String label) {
        if (uiList.isEmpty()) {
            return;
        }

        JButton typeButton = new JButton(label + " (" + uiList.size() + ")");
        typeButton.setHorizontalAlignment(SwingConstants.LEFT);
        typeButton.setIcon(JIPipe.RESOURCES.getIcon16("actions/caret-down.png"));
        typeButton.setBorder(null);
        JPopupMenu typeMenu = UIUtils.addPopupMenuToButton(typeButton);
        for (JIPipeDesktopGraphInteractiveObjectUI ui : uiList) {
            typeMenu.add(UIUtils.createMenuItem(ui.getDisplayName(), ui.getDescription(), ui.getIcon(), () -> {
                getGraphEditorUI().getSelectionManager().selectOnly(ui);
            }));
        }
        formPanel.addToForm(UIUtils.boxHorizontal(
                UIUtils.createButton("Only", JIPipe.RESOURCES.getIcon16("actions/eye-dropper.png"), () -> {
                    getGraphEditorUI().getSelectionManager().setSelection(new HashSet<>(uiList));
                }),
                UIUtils.createButton("Deselect", JIPipe.RESOURCES.getIcon16("actions/edit-select-none.png"), () -> {
                    getGraphEditorUI().getSelectionManager().removeFromSelection(uiList);
                })
        ), typeButton);

    }

    @Override
    protected Icon getTitleIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/gtk-select-all.png");
    }

    @Override
    protected String getTitle() {
        return "Selection (" + getGraphEditorUI().getSelectionManager().getSelection().size() + ")";
    }
}
