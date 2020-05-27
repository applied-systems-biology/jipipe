package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;
import org.hkijena.acaq5.extensions.parameters.generators.IntRangeStringParameter;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;

/**
 * Editor for {@link org.hkijena.acaq5.extensions.parameters.generators.IntRangeStringParameter}
 */
public class IntRangeStringParameterEditorUI extends ACAQParameterEditorUI {

    private JTextField textField;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public IntRangeStringParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        IntRangeStringParameter rangeString = getParameterAccess().get(IntRangeStringParameter.class);
        if (rangeString == null) {
            getParameterAccess().set(new IntRangeStringParameter());
            return;
        }
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
        IntRangeStringParameter rangeString = getParameterAccess().get(IntRangeStringParameter.class);
        if (rangeString == null) {
            getParameterAccess().set(new IntRangeStringParameter());
            return;
        }
        try {
            List<Integer> integers = rangeString.getIntegers();
            textField.setBorder(BorderFactory.createEtchedBorder());
            textField.setToolTipText("Valid!");
        }
        catch (Exception e) {
            textField.setBorder(BorderFactory.createLineBorder(Color.RED));
            textField.setToolTipText("Invalid: " + e.getMessage());
        }
    }
}
