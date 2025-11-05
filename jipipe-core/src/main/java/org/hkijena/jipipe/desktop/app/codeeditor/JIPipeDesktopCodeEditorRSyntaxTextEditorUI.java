package org.hkijena.jipipe.desktop.app.codeeditor;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.textfield.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.plugins.parameters.api.scripts.JIPipeScriptParameter;
import org.hkijena.jipipe.utils.CustomEditorPane;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

public class JIPipeDesktopCodeEditorRSyntaxTextEditorUI extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeDesktopCodeEditorUI editorUI;
    private final StaticDebouncer pushDebouncer;
    private CustomEditorPane textArea;
    private boolean isReloading;

    public JIPipeDesktopCodeEditorRSyntaxTextEditorUI(JIPipeDesktopCodeEditorUI editorUI) {
        super(editorUI.getDesktopWorkbench());
        this.editorUI = editorUI;
        this.pushDebouncer = new StaticDebouncer(125, this::pushDocument);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Initialize text area
        textArea = new CustomEditorPane();
        ThemeUtils.applyThemeToCodeEditor(textArea);
        textArea.setBackground(UIManager.getColor("TextArea.background"));
        textArea.setHighlightCurrentLine(false);

        textArea.setTabSize(4);
        editorUI.getWorkbench().getContext().inject(textArea);
        textArea.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                pushDebouncer.debounce();
            }
        });

        add(textArea, BorderLayout.CENTER);

        // Initialize toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(UIUtils.makeButtonFlat25x25(UIUtils.createIconOnlyButton("Cut", JIPipe.RESOURCES.getIcon16("actions/edit-cut.png"), textArea::cut)));
        toolBar.add(UIUtils.makeButtonFlat25x25(UIUtils.createIconOnlyButton("Copy", JIPipe.RESOURCES.getIcon16("actions/edit-copy.png"), textArea::copy)));
        toolBar.add(UIUtils.makeButtonFlat25x25(UIUtils.createIconOnlyButton("Paste", JIPipe.RESOURCES.getIcon16("actions/edit-paste.png"), textArea::paste)));
        toolBar.addSeparator();
        toolBar.add(UIUtils.makeButtonFlat25x25(UIUtils.createIconOnlyButton("Undo", JIPipe.RESOURCES.getIcon16("actions/edit-undo.png"), textArea::undoLastAction)));
        toolBar.add(UIUtils.makeButtonFlat25x25(UIUtils.createIconOnlyButton("Redo", JIPipe.RESOURCES.getIcon16("actions/edit-redo.png"), textArea::redoLastAction)));

        add(toolBar, BorderLayout.NORTH);
    }

    private void pushDocument() {
        if (!isReloading) {
            JIPipeDesktopCodeEditorDocument document = editorUI.getDocument();
            if (document != null) {
                JIPipeScriptParameter copy = JIPipe.duplicateParameter(document.pull());
                copy.setCode(textArea.getText());
                document.push(copy);
            }
        }
    }

    public void onDocumentUpdated() {
        JIPipeDesktopCodeEditorDocument document = editorUI.getDocument();
        try {
            isReloading = true;

            if (document != null) {
                JIPipeScriptParameter script = document.pull();
                if (script.getLanguage() != null) {
                    textArea.setLanguage(script.getLanguage());
                }
                textArea.setText(script.getCode());
                textArea.setSyntaxEditingStyle(script.getMimeType());
            }
        } finally {
            isReloading = false;
        }

    }
}
