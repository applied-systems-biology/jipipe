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

package org.hkijena.jipipe.desktop.app.codeeditor;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Cental panel for code editing
 */
public class JIPipeDesktopCodeEditorUI extends JIPipeDesktopWorkbenchPanel {
    private JIPipeDesktopCodeEditorDocument document;
    private JIPipeDesktopCodeEditorRSyntaxTextEditorUI rSyntaxTextEditorUI;
    private JLabel noDocumentMessage;
    private JButton currentDocumentInfoButton;
    private final JToolBar toolBar = new JToolBar();
    private final JPopupMenu currentDocumentInfoPopupMenu = new JPopupMenu();

    public JIPipeDesktopCodeEditorUI(JIPipeDesktopWorkbench desktopWorkbench, JIPipeDesktopCodeEditorDocument document) {
        super(desktopWorkbench);
        initialize();
        setDocument(document);
    }

    public JIPipeDesktopCodeEditorUI(JIPipeDesktopWorkbench desktopWorkbench) {
        this(desktopWorkbench, null);
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    private void initialize() {
        setLayout(new BorderLayout(8,8));
        rSyntaxTextEditorUI = new JIPipeDesktopCodeEditorRSyntaxTextEditorUI(this);
        noDocumentMessage = UIUtils.createInfoLabel("No document to edit",
                "The code editor currently has no script to edit",
                JIPipe.RESOURCES.getIcon32("mimetypes/empty.png"));

        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0,0,0, ThemeUtils.getCurrentStyle().getBorderColor()),
                BorderFactory.createEmptyBorder(3,0,3,0)));
        currentDocumentInfoButton = new JButton();
        currentDocumentInfoButton.setBorder(null);
        UIUtils.addReloadablePopupMenuToButton(currentDocumentInfoButton, currentDocumentInfoPopupMenu, this::reloadPopupMenu);
        toolBar.add(currentDocumentInfoButton);
    }

    private void reloadPopupMenu() {
        if(document != null) {
            currentDocumentInfoPopupMenu.removeAll();
            document.createActionsMenu(this, currentDocumentInfoPopupMenu);
        }
    }

    private void onDocumentUpdated() {
        removeAll();
        if(document != null) {
            // Update the button
            currentDocumentInfoButton.setText(document.getTitle());
            currentDocumentInfoButton.setIcon(document.getIcon());
            reloadPopupMenu();

            // Add the editor
            add(rSyntaxTextEditorUI, BorderLayout.CENTER);
            add(toolBar, BorderLayout.SOUTH);
            rSyntaxTextEditorUI.onDocumentUpdated();
        }
        else {
            add(noDocumentMessage, BorderLayout.CENTER);
        }

        revalidate();
        repaint(50);
    }

    public void setDocument(JIPipeDesktopCodeEditorDocument document) {
        this.document = document;
        onDocumentUpdated();
    }

    public JIPipeDesktopCodeEditorDocument getDocument() {
        return document;
    }
}
