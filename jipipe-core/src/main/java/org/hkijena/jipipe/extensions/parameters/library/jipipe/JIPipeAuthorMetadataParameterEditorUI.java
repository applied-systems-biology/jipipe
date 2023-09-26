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

package org.hkijena.jipipe.extensions.parameters.library.jipipe;

import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

public class JIPipeAuthorMetadataParameterEditorUI extends JIPipeParameterEditorUI {

    private final JXTextField firstNameEditor = new JXTextField("First name");
    private final JXTextField lastNameEditor = new JXTextField("Last name");
    private boolean isReloading = false;

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public JIPipeAuthorMetadataParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        firstNameEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!isReloading) {
                    JIPipeAuthorMetadata parameter = getParameter(JIPipeAuthorMetadata.class);
                    parameter.setFirstName(firstNameEditor.getText());
                    setParameter(parameter, false);
                }
            }
        });
        add(firstNameEditor);

        add(Box.createHorizontalStrut(8));

        lastNameEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!isReloading) {
                    JIPipeAuthorMetadata parameter = getParameter(JIPipeAuthorMetadata.class);
                    parameter.setLastName(lastNameEditor.getText());
                    setParameter(parameter, false);
                }
            }
        });
        add(lastNameEditor);

        add(Box.createHorizontalStrut(8));

        JButton editButton = new JButton("Edit", UIUtils.getIconFromResources("actions/stock_edit.png"));
        UIUtils.setStandardButtonBorder(editButton);
        editButton.setToolTipText("Shows the full editor");
        editButton.addActionListener(e -> {
            JIPipeAuthorMetadata parameter = getParameter(JIPipeAuthorMetadata.class);
            ParameterPanel.showDialog(getWorkbench(),
                    parameter,
                    new MarkdownDocument("# Edit author\n\nUse this editor to update additional author properties."),
                    "Edit author",
                    ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING);
            reload();
        });
        add(editButton);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        JIPipeAuthorMetadata parameter = getParameter(JIPipeAuthorMetadata.class);
        try {
            isReloading = true;
            firstNameEditor.setText(parameter.getFirstName());
            lastNameEditor.setText(parameter.getLastName());
        } finally {
            isReloading = false;
        }
    }
}
