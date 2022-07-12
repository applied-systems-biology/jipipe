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
 *
 */

package org.hkijena.jipipe.ui.extensions;

import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.extensions.expressions.FunctionSelectorList;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExtensionInfoPanel extends JPanel {
    private final JIPipeExtension extension;

    public ExtensionInfoPanel(JIPipeExtension extension) {
        this.extension = extension;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabPane = new DocumentTabPane();
        add(tabPane, BorderLayout.CENTER);
        initializeOverview(tabPane);
        initializeButtonPanel();
    }

    private void initializeOverview(DocumentTabPane tabPane) {
        FormPanel panel = new FormPanel(null, FormPanel.WITH_SCROLLING);
        FormPanel.GroupHeaderPanel headerPanel = panel.addGroupHeader(extension.getMetadata().getName(), UIUtils.getIconFromResources("actions/help-info.png"));
        headerPanel.setDescription(extension.getMetadata().getSummary().getHtml());
        if(!Objects.equals(extension.getMetadata().getSummary().getHtml(), extension.getMetadata().getDescription().getHtml())) {
            panel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane(extension.getMetadata().getDescription().getHtml()), null);
        }
        panel.addVerticalGlue();
        tabPane.addTab("Overview", UIUtils.getIconFromResources("actions/plugins.png"), panel, DocumentTabPane.CloseMode.withoutCloseButton);
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(new ExtensionItemActionButton(extension));
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public static void showDialog(Component parent, JIPipeExtension extension) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        dialog.setContentPane(new ExtensionInfoPanel(extension));
        dialog.setModal(true);
        dialog.setTitle(extension.getMetadata().getName());
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
    }
}
