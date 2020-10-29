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

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultExpressionParameterEditorUI extends JIPipeParameterEditorUI {

    private RSyntaxTextArea expressionEditor;
    private final JPanel expressionEditorPanel = new JPanel(new BorderLayout());
    private final JPanel editorPanel = new JPanel(new BorderLayout());
    private JButton variableOptions;
    private DefaultExpressionEvaluatorSyntaxTokenMaker tokenMaker = new DefaultExpressionEvaluatorSyntaxTokenMaker();
    private Set<ExpressionParameterVariable> variables = new HashSet<>();

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public DefaultExpressionParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        setToAdvancedEditor();
        reloadVariables();
        reload();
    }

    private void setToAdvancedEditor() {
        editorPanel.removeAll();
        editorPanel.add(expressionEditorPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void initialize() {
        setLayout(new BorderLayout());
//        add(availableModes, BorderLayout.WEST);
        add(editorPanel, BorderLayout.CENTER);

        JPanel optionPanel = new JPanel();
        optionPanel.setOpaque(false);
        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.X_AXIS));

        variableOptions = new JButton(UIUtils.getIconFromResources("actions/variable.png"));
        UIUtils.makeFlat25x25(variableOptions);
        optionPanel.add(variableOptions);
        variableOptions.addActionListener(e -> insertVariable());

        JButton operatorOptions = new JButton(UIUtils.getIconFromResources("actions/equals.png"));
        UIUtils.makeFlat25x25(operatorOptions);
        optionPanel.add(operatorOptions);
        operatorOptions.addActionListener(e -> insertOperator());

        JButton functionOptions = new JButton(UIUtils.getIconFromResources("actions/insert-math-expression.png"));
        UIUtils.makeFlat25x25(functionOptions);
        optionPanel.add(functionOptions);
        functionOptions.addActionListener(e -> insertFunction());

        expressionEditorPanel.add(optionPanel, BorderLayout.EAST);

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
        RSyntaxDocument document = new RSyntaxDocument(tokenMakerFactory, "text/expression");
        expressionEditor = new RSyntaxTextArea(document);
        expressionEditor.setLineWrap(true);
        expressionEditor.setBorder(BorderFactory.createEmptyBorder(5,4,1,4));
        expressionEditor.setHighlightCurrentLine(false);
        expressionEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                DefaultExpressionParameter parameter = getParameter(DefaultExpressionParameter.class);
                if(!Objects.equals(parameter.getExpression(), expressionEditor.getText())) {
                    parameter.setExpression(expressionEditor.getText());
                    setParameter(parameter, false);
                }
            }
        });
        expressionEditorPanel.setBorder(BorderFactory.createEtchedBorder());
        expressionEditorPanel.setOpaque(true);
        expressionEditorPanel.setBackground(Color.WHITE);
        expressionEditorPanel.add(expressionEditor, BorderLayout.CENTER);
    }

    private void insertOperator() {
        ExpressionOperatorEntry operator = OperatorSelectorList.showDialog(this, DefaultExpressionParameter.EVALUATOR);
        if(operator != null) {
            String template = operator.getTemplate();
            insertIntoExpression(template);
        }
    }

    private void insertVariable() {
        ExpressionParameterVariable variable = VariableSelectorList.showDialog(this, variables);
        if(variable != null) {
            String template = variable.getKey();
            insertIntoExpression(template);
        }
    }

    private void insertIntoExpression(String template) {
        int caret = expressionEditor.getCaretPosition();
        if(caret != 0)
            template = " " + template;
        try {
            if(!Objects.equals(expressionEditor.getText(caret, 1), " "))
                template = template + " ";
        } catch (BadLocationException e) {
        }
        expressionEditor.insert(template, expressionEditor.getCaretPosition());
    }

    private void insertFunction() {
        JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = FunctionSelectorList.showDialog(this);
        if(functionEntry != null) {
            String template = functionEntry.getFunction().getTemplate();
            insertIntoExpression(template);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        DefaultExpressionParameter parameter = getParameter(DefaultExpressionParameter.class);
        if(!Objects.equals(parameter.getExpression(), expressionEditor.getText())) {
            expressionEditor.setText(parameter.getExpression());
        }
    }

    private void reloadVariables() {
        variables.clear();
        ExpressionParameterSettings settings = getParameterAccess().getAnnotationOfType(ExpressionParameterSettings.class);
        if(settings != null) {
            ExpressionParameterVariableSource variableSource = (ExpressionParameterVariableSource) ReflectionUtils.newInstance(settings.variableSource());
            variables.addAll(variableSource.getVariables(getParameterAccess()));
        }
        variableOptions.setVisible(!variables.isEmpty());
        tokenMaker.setDynamicVariables(variables);
    }
}
