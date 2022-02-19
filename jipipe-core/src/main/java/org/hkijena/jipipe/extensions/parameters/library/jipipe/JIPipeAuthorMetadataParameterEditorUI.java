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

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public JIPipeAuthorMetadataParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        JIPipeAuthorMetadata parameter = getParameter(JIPipeAuthorMetadata.class);

        JXTextField firstNameEditor = new JXTextField("First name");
        firstNameEditor.setText(parameter.getFirstName());
        firstNameEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                parameter.setFirstName(firstNameEditor.getText());
            }
        });
        add(firstNameEditor);

        add(Box.createHorizontalStrut(8));

        JXTextField lastNameEditor = new JXTextField("Last name");
        lastNameEditor.setText(parameter.getLastName());
        lastNameEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                parameter.setLastName(lastNameEditor.getText());
            }
        });
        add(lastNameEditor);

        add(Box.createHorizontalStrut(8));

        JButton editButton = new JButton("Edit", UIUtils.getIconFromResources("actions/stock_edit.png"));
        UIUtils.makeFlat(editButton);
        editButton.setToolTipText("<html>Edit the affiliations<br/><br/>" + parameter.getAffiliations() + "</html>");
        editButton.addActionListener(e -> {
            ParameterPanel.showDialog(getWorkbench(),
                    parameter,
                    new MarkdownDocument("# Edit author\n\nUse this editor to update additional author properties."),
                    "Edit author",
                    ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING);
            reload();
        });
        add(editButton);
    }
}
