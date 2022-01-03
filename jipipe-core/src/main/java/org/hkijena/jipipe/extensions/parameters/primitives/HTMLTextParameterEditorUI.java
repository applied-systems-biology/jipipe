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

package org.hkijena.jipipe.extensions.parameters.primitives;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.html.HTMLEditor;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.util.Objects;

/**
 * Parameter editor for {@link HTMLText}
 */
public class HTMLTextParameterEditorUI extends JIPipeParameterEditorUI {

    private final HTMLEditor editor;
    private boolean isReloading = false;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public HTMLTextParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        editor = new HTMLEditor(workbench, HTMLEditor.Mode.Compact, HTMLEditor.WITH_DIALOG_EDITOR_BUTTON);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        Object value = getParameterAccess().get(Object.class);
        String stringValue = "";
        if (value != null) {
            stringValue = "" + value;
        }
        editor.setBorder(BorderFactory.createEtchedBorder());
        editor.setText(stringValue);
        add(editor, BorderLayout.CENTER);

        editor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!isReloading) {
                    Object value = getParameterAccess().get(Object.class);
                    String stringValue = "";
                    if (value != null) {
                        stringValue = "" + value;
                    }
                    if (!Objects.equals(stringValue, editor.getHTML())) {
                        setParameter(new HTMLText(editor.getHTML()), false);
                    }
                }
            }
        });
    }


    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        Object value = getParameterAccess().get(Object.class);
        String stringValue = "";
        if (value != null) {
            stringValue = "" + value;
        }
        if (!isReloading && !Objects.equals(stringValue, editor.getHTML())) {
            isReloading = true;
            editor.setText(stringValue);
            SwingUtilities.invokeLater(this::revalidate);
            isReloading = false;
        }
    }
}
