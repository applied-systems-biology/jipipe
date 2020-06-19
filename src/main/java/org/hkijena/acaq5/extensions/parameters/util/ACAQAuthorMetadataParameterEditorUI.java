package org.hkijena.acaq5.extensions.parameters.util;

import org.hkijena.acaq5.api.ACAQAuthorMetadata;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

public class ACAQAuthorMetadataParameterEditorUI extends ACAQParameterEditorUI {

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public ACAQAuthorMetadataParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
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
        ACAQAuthorMetadata parameter = getParameter(ACAQAuthorMetadata.class);

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

        JButton editAffiliationsButton = new JButton("Affiliations", UIUtils.getIconFromResources("algorithms/graduation-cap.png"));
        UIUtils.makeFlat(editAffiliationsButton);
        editAffiliationsButton.setToolTipText("<html>Edit the affiliations<br/><br/>" + parameter.getAffiliations() + "</html>");
        editAffiliationsButton.addActionListener(e -> {
            String newAffiliations = UIUtils.getMultiLineStringByDialog(this, "Edit affiliations for " + parameter.getFirstName() + " " + parameter.getLastName(),
                    "Please insert the new affiliations:",
                    parameter.getAffiliations());
            if (newAffiliations != null) {
                parameter.setAffiliations(newAffiliations);
                editAffiliationsButton.setToolTipText("<html>Edit the affiliations<br/><br/>" + parameter.getAffiliations() + "</html>");
            }
        });
        add(editAffiliationsButton);
    }
}
