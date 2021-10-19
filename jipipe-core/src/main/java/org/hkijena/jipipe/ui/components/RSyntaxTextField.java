package org.hkijena.jipipe.ui.components;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import java.awt.event.KeyEvent;

public class RSyntaxTextField extends RSyntaxTextArea {
    public RSyntaxTextField() {
    }

    public RSyntaxTextField(RSyntaxDocument doc) {
        super(doc);
    }

    public RSyntaxTextField(String text) {
        super(text);
    }

    public RSyntaxTextField(int rows, int cols) {
        super(rows, cols);
    }

    public RSyntaxTextField(String text, int rows, int cols) {
        super(text, rows, cols);
    }

    public RSyntaxTextField(RSyntaxDocument doc, String text, int rows, int cols) {
        super(doc, text, rows, cols);
    }

    public RSyntaxTextField(int textMode) {
        super(textMode);
    }

    @Override
    protected void processComponentKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            transferFocus();
            e.consume();
        } else {
            super.processComponentKeyEvent(e);
        }
    }
}
