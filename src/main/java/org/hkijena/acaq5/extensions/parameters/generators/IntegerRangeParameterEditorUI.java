package org.hkijena.acaq5.extensions.parameters.generators;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.scijava.Context;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * Editor for {@link IntegerRange}
 */
public class IntegerRangeParameterEditorUI extends ACAQParameterEditorUI {

    private JTextField textField;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public IntegerRangeParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
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
