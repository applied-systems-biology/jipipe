package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.scijava.Context;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Parameter editor for {@link String}
 */
public class StringParameterEditorUI extends ACAQParameterEditorUI {

    private JTextComponent textComponent;
    private boolean skipNextReload = false;
    private boolean isReloading = false;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public StringParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        StringParameterSettings settings = getParameterAccess().getAnnotationOfType(StringParameterSettings.class);
        Object value = getParameterAccess().get();
        String stringValue = "";
        if (value != null) {
            stringValue = "" + value;
        }
        boolean monospaced = settings != null && settings.monospace();
        if (settings != null && settings.multiline()) {
            JTextArea textArea = new JTextArea(stringValue);
            textArea.setBorder(BorderFactory.createEtchedBorder());
            textComponent = textArea;
            add(textArea, BorderLayout.CENTER);
        } else {
            JTextField textField = new JTextField(stringValue);
            textComponent = textField;
            add(textField, BorderLayout.CENTER);
        }
        if (monospaced)
            textComponent.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        textComponent.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!isReloading) {
                    skipNextReload = true;
                    getParameterAccess().set(textComponent.getText());
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
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        isReloading = true;
        Object value = getParameterAccess().get();
        String stringValue = "";
        if (value != null) {
            stringValue = "" + value;
        }
        textComponent.setText(stringValue);
        isReloading = false;
    }
}
