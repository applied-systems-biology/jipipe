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

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionEvaluator;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionEvaluatorSyntaxTokenMaker;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Set;

/**
 * A parameter UI for the expression builder.
 * Allows users to easily define strings, booleans, numbers, variables, and expressions within an UI
 */
public class ExpressionBuilderParameterUI extends JPanel {
    private final JComboBox<Mode> modeJComboBox = new JComboBox<>();
    private final JIPipeExpressionEvaluatorSyntaxTokenMaker tokenMaker = new JIPipeExpressionEvaluatorSyntaxTokenMaker();
    private RSyntaxTextArea expressionEditor;

    private RSyntaxTextArea rawExpressionEditor;
    private JCheckBox booleanEditor;
    private JSpinner numberEditor;
    private JTextField variableEditor;
    private JTextField stringEditor;
    private JPanel editorPanel;

    public ExpressionBuilderParameterUI() {
        initialize();
        modeJComboBox.setSelectedItem(Mode.Expression);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // combo box
        DefaultComboBoxModel<Mode> model = new DefaultComboBoxModel<>();
        for (Mode value : Mode.values()) {
            model.addElement(value);
        }
        modeJComboBox.setModel(model);
        modeJComboBox.addActionListener(e -> switchToMode((Mode) modeJComboBox.getSelectedItem()));
        add(modeJComboBox, BorderLayout.WEST);

        // Variable editor
        variableEditor = new JTextField();
        variableEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        variableEditor.setForeground(ExpressionBuilderUI.COLOR_VARIABLE);

        // Variable editor
        stringEditor = new JTextField();
        stringEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        stringEditor.setForeground(ExpressionBuilderUI.COLOR_STRING);

        // Number editor
        numberEditor = new JSpinner(new SpinnerNumberModel(0.0, -Double.MAX_VALUE, Double.MAX_VALUE, 1));

        // Boolean editor
        booleanEditor = new JCheckBox("Return TRUE");

        // Expression editor
        TokenMakerFactory tokenMakerFactory = new TokenMakerFactory() {
            @Override
            protected TokenMaker getTokenMakerImpl(String key) {
                return tokenMaker;
            }

            @Override
            public Set<String> keySet() {
                return Collections.singleton("text/expression");
            }
        };

        expressionEditor = new RSyntaxTextArea(new RSyntaxDocument(tokenMakerFactory, "text/expression"));
        UIUtils.applyThemeToCodeEditor(expressionEditor);
        expressionEditor.setBackground(UIManager.getColor("TextArea.background"));
        expressionEditor.setLineWrap(true);
        expressionEditor.setHighlightCurrentLine(false);

        rawExpressionEditor = new RSyntaxTextArea(new RSyntaxDocument(tokenMakerFactory, "text/expression"));
        UIUtils.applyThemeToCodeEditor(rawExpressionEditor);
        rawExpressionEditor.setBackground(UIManager.getColor("TextArea.background"));
        rawExpressionEditor.setLineWrap(true);
        rawExpressionEditor.setHighlightCurrentLine(false);

        // Panel that contains the editors
        editorPanel = new JPanel(new CardLayout());
        editorPanel.add(variableEditor, Mode.Variable.name());
        editorPanel.add(numberEditor, Mode.Number.name());
        editorPanel.add(booleanEditor, Mode.Boolean.name());
        editorPanel.add(stringEditor, Mode.String.name());
        {
            JPanel expressionEditorPanel = new JPanel(new BorderLayout());
            expressionEditorPanel.setBackground(UIManager.getColor("TextArea.background"));
            expressionEditorPanel.add(expressionEditor, BorderLayout.CENTER);
            expressionEditorPanel.setBorder(BorderFactory.createCompoundBorder(UIUtils.createControlBorder(),
                    BorderFactory.createEmptyBorder(5, 4, 0, 4)));
            editorPanel.add(expressionEditorPanel, Mode.Expression.name());
        }
        {
            JPanel expressionEditorPanel = new JPanel(new BorderLayout());
            expressionEditorPanel.setBackground(UIManager.getColor("TextArea.background"));
            expressionEditorPanel.add(rawExpressionEditor, BorderLayout.CENTER);
            expressionEditorPanel.setBorder(BorderFactory.createCompoundBorder(UIUtils.createControlBorder(),
                    BorderFactory.createEmptyBorder(5, 4, 0, 4)));
            editorPanel.add(expressionEditorPanel, Mode.RawExpression.name());
        }
        add(editorPanel, BorderLayout.CENTER);
    }

    public Mode getCurrentMode() {
        return (Mode) modeJComboBox.getSelectedItem();
    }

    public String getCurrentExpressionValue() {
        switch (getCurrentMode()) {
            case Number:
                return "" + ((SpinnerNumberModel) numberEditor.getModel()).getNumber().doubleValue();
            case Boolean:
                return booleanEditor.isSelected() ? "TRUE" : "FALSE";
            case Variable:
                return JIPipeExpressionEvaluator.escapeVariable(variableEditor.getText());
            case Expression:
                return expressionEditor.getText();
            case RawExpression:
                return "${" + rawExpressionEditor.getText() + "}";
            case String:
                return "\"" + JIPipeExpressionEvaluator.escapeString(stringEditor.getText()) + "\"";
            default:
                throw new UnsupportedOperationException();
        }
    }

    public void switchToMode(Mode mode) {
        CardLayout layout = (CardLayout) editorPanel.getLayout();
        layout.show(editorPanel, mode.name());
    }

    public enum Mode {
        Expression,
        Variable,
        Boolean,
        Number,
        String,
        RawExpression;


        @Override
        public java.lang.String toString() {
            if (this == RawExpression) {
                return "Raw expression";
            }
            return super.toString();
        }
    }
}
