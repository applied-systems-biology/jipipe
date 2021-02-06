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
import org.hkijena.jipipe.ui.components.FancyTextField;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Objects;

/**
 * Parameter editor for {@link FormattedTextParameter}
 */
public class FormattedTextParameterEditorUI extends JIPipeParameterEditorUI {

    private JTextPane textComponent;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public FormattedTextParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
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

        JTextPane textArea = new JTextPane();
        textArea.setContentType("text/richtext");
        RTFEditorKit editorKit = new RTFEditorKit();
        textArea.setEditorKit(editorKit);
        textArea.setText(stringValue);
        setBorder(BorderFactory.createEtchedBorder());
        textComponent = textArea;
        add(textArea, BorderLayout.CENTER);

        textComponent.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                try {
                    Document document = textArea.getDocument();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream ();
                    editorKit.write(byteArrayOutputStream, document, 0, document.getLength());
                    setParameter(new FormattedTextParameter(document.getText(0, document.getLength()),
                            byteArrayOutputStream.toString()), false);
                    System.out.println(document.getText(0, document.getLength()));
                    System.out.println(byteArrayOutputStream.toString());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton formatTextBold = new JButton(UIUtils.getIconFromResources("actions/format-text-bold.png"));
        formatTextBold.setToolTipText("Format bold");
        formatTextBold.addActionListener(e -> formatTextBold());
        toolBar.add(formatTextBold);

        add(toolBar, BorderLayout.NORTH);
    }

    private void formatTextBold() {
        StyleContext context = new StyleContext();
        Style style = context.addStyle("custom", null);
        style.addAttribute(StyleConstants.Bold, true);
        textComponent.setCharacterAttributes(style , true);
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
        if (!Objects.equals(stringValue, textComponent.getText()))
            textComponent.setText(stringValue);
    }
}
