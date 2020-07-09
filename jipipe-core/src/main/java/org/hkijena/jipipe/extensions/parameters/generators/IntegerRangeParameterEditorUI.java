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

package org.hkijena.jipipe.extensions.parameters.generators;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * Editor for {@link IntegerRange}
 */
public class IntegerRangeParameterEditorUI extends JIPipeParameterEditorUI {

    private JTextField textField;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public IntegerRangeParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        removeAll();
        IntegerRange rangeString = getParameter(IntegerRange.class);
        textField = new JTextField(rangeString.getValue());
        textField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                rangeString.setValue(textField.getText());
                checkParameter();
            }
        });
        add(textField);
        revalidate();
        repaint();
        checkParameter();
    }

    private void checkParameter() {
        IntegerRange rangeString = getParameter(IntegerRange.class);
        try {
            rangeString.getIntegers();
            textField.setBorder(BorderFactory.createEtchedBorder());
            textField.setToolTipText("Valid!");
        } catch (Exception e) {
            textField.setBorder(BorderFactory.createLineBorder(Color.RED));
            textField.setToolTipText("Invalid: " + e.getMessage());
        }
    }
}
