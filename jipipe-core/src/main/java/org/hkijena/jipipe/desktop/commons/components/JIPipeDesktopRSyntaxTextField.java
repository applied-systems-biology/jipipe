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

package org.hkijena.jipipe.desktop.commons.components;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import java.awt.event.KeyEvent;

public class JIPipeDesktopRSyntaxTextField extends RSyntaxTextArea {
    public JIPipeDesktopRSyntaxTextField() {
    }

    public JIPipeDesktopRSyntaxTextField(RSyntaxDocument doc) {
        super(doc);
    }

    public JIPipeDesktopRSyntaxTextField(String text) {
        super(text);
    }

    public JIPipeDesktopRSyntaxTextField(int rows, int cols) {
        super(rows, cols);
    }

    public JIPipeDesktopRSyntaxTextField(String text, int rows, int cols) {
        super(text, rows, cols);
    }

    public JIPipeDesktopRSyntaxTextField(RSyntaxDocument doc, String text, int rows, int cols) {
        super(doc, text, rows, cols);
    }

    public JIPipeDesktopRSyntaxTextField(int textMode) {
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
