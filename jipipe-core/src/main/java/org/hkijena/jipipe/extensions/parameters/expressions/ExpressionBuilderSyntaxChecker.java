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

package org.hkijena.jipipe.extensions.parameters.expressions;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;

public class ExpressionBuilderSyntaxChecker extends JPanel {
    private final RSyntaxTextArea expressionEditor;
    private final JLabel statusLabel = new JLabel();

    public ExpressionBuilderSyntaxChecker(RSyntaxTextArea expressionEditor) {
        this.expressionEditor = expressionEditor;
        initialize();
        updateStatus();
    }

    private void updateStatus() {
        Exception exception = DefaultExpressionParameter.EVALUATOR.checkSyntax(expressionEditor.getText());
        if(exception == null) {
            statusLabel.setIcon(UIUtils.getIconFromResources("emblems/vcs-normal.png"));
            if(expressionEditor.getText().trim().length() == 0)
                statusLabel.setText("Evaluates to TRUE");
            else
                statusLabel.setText("");
        }
        else {
            statusLabel.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
            statusLabel.setText(exception.getMessage());
        }
    }

    private void initialize() {
        expressionEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                updateStatus();
            }
        });
        setLayout(new BorderLayout());
        add(statusLabel, BorderLayout.CENTER);
    }
}
