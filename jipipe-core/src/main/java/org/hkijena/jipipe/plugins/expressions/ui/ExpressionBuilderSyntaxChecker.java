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

package org.hkijena.jipipe.plugins.expressions.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

public class ExpressionBuilderSyntaxChecker extends JPanel {
    private final RSyntaxTextArea expressionEditor;
    private final JLabel statusLabel = new JLabel();

    public ExpressionBuilderSyntaxChecker(RSyntaxTextArea expressionEditor) {
        this.expressionEditor = expressionEditor;
        initialize();
        updateStatus();
    }

    private void updateStatus() {
        Exception exception = JIPipeExpressionParameter.getEvaluatorInstance().checkSyntax(expressionEditor.getText());
        if (exception == null) {
            statusLabel.setIcon(UIUtils.getIconFromResources("emblems/vcs-normal.png"));
            if (expressionEditor.getText().trim().length() == 0)
                statusLabel.setText("Evaluates to TRUE");
            else
                statusLabel.setText("");
        } else {
            statusLabel.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
            statusLabel.setText(exception.getMessage());
        }
    }

    private void initialize() {
        expressionEditor.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                updateStatus();
            }
        });
        setLayout(new BorderLayout());
        add(statusLabel, BorderLayout.CENTER);
    }
}
