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
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Color;

public class ExpressionParameterEditorUI extends JIPipeParameterEditorUI {

    private final RSyntaxTextArea expressionEditor = new RSyntaxTextArea();
    private final JPanel expressionEditorPanel = new JPanel(new BorderLayout());
    private final JPanel editorPanel = new JPanel(new BorderLayout());
    private final JComboBox<Object> availableModes = new JComboBox<>();

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public ExpressionParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        setToAdvancedEditor();
    }

    private void setToAdvancedEditor() {
        editorPanel.removeAll();
        editorPanel.add(expressionEditorPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(availableModes, BorderLayout.WEST);
        add(editorPanel, BorderLayout.CENTER);

        expressionEditorPanel.setBorder(BorderFactory.createEtchedBorder());
        expressionEditorPanel.setOpaque(true);
        expressionEditorPanel.setBackground(Color.WHITE);
        expressionEditorPanel.add(expressionEditor, BorderLayout.CENTER);

        JButton expressionEditorOptions = new JButton(UIUtils.getIconFromResources("actions/settings.png"));
        UIUtils.makeFlat25x25(expressionEditorOptions);
        expressionEditorPanel.add(expressionEditorOptions, BorderLayout.EAST);

        expressionEditor.setHighlightCurrentLine(false);
        expressionEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                ExpressionParameter parameter = new ExpressionParameter();
                parameter.setExpression(expressionEditor.getText());
                setParameter(parameter, false);
            }
        });
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        ExpressionParameter parameter = getParameter(ExpressionParameter.class);
        expressionEditor.setText(parameter.getExpression());
    }
}
