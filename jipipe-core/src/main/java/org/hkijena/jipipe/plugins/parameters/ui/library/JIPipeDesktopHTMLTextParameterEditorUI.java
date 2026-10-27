/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.parameters.ui.library;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopHTMLEditor;
import org.hkijena.jipipe.desktop.commons.components.textfield.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Objects;

/**
 * Parameter editor for {@link HTMLText}
 */
public class JIPipeDesktopHTMLTextParameterEditorUI extends JIPipeDesktopParameterEditorUI<HTMLText> {

    private final JIPipeDesktopHTMLEditor editor;
    private boolean isReloading = false;


    public JIPipeDesktopHTMLTextParameterEditorUI(InitializationParameters parameters) {
        super(HTMLText.class, parameters);
        // The inline widget keeps the editable body but drops the formatting
        // toolbar (NO_TOOLBAR, #1339) — the full editing controls live in the
        // dialog opened from the name-row "Edit" button (see createHeaderControls).
        editor = new JIPipeDesktopHTMLEditor(parameters.getWorkbench(), JIPipeDesktopHTMLEditor.Mode.Compact,
                JIPipeDesktopHTMLEditor.WITH_DIALOG_EDITOR_BUTTON | JIPipeDesktopHTMLEditor.NO_TOOLBAR);
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
        // The editor carries its own control border (spec §8.2); no redundant
        // outer border here.
        editor.setText(stringValue);
        add(editor, BorderLayout.CENTER);

        editor.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
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

    /**
     * Stacked — the HTML editor is a compound control (toolbar + body) that
     * needs the full row width (spec §8.1). The name sits above the widget.
     */
    @Override
    public boolean isUIInline() {
        return false;
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

    /**
     * The name-row chrome (#1339): an **Edit** button (icon
     * {@code actions/edit.png}, the modern compound-editor pattern) that opens
     * the full HTML editor dialog with the formatting toolbar. The inline
     * widget itself is an editable body only.
     */
    @Override
    public JComponent createHeaderControls() {
        JButton editButton = UIUtils.createParameterButton("Edit", JIPipe.RESOURCES.getIcon16("actions/edit.png"),
                e -> {
                    HTMLText result = UIUtils.getHTMLByDialog(getDesktopWorkbench(), this, "Edit", null,
                            new HTMLText(editor.getHTML()));
                    if (result != null) {
                        setTextFromDialog(result.getHtml());
                    }
                });
        editButton.setToolTipText("Edit this HTML text in a dedicated window");
        // Right-align on the name row (the header cell stretches to full width,
        // so a plain panel would center the button) — consistent with the list
        // editor's header (#1339).
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        controls.setOpaque(false);
        controls.add(editButton);
        return controls;
    }

    private void setTextFromDialog(String html) {
        isReloading = true;
        editor.setText(html);
        try {
            Object value = getParameterAccess().get(Object.class);
            String stringValue = "";
            if (value != null) {
                stringValue = "" + value;
            }
            if (!Objects.equals(stringValue, html)) {
                setParameter(new HTMLText(html), true);
            }
        } finally {
            isReloading = false;
        }
    }
}
