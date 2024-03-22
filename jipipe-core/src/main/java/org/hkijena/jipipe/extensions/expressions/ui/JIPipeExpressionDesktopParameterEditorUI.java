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

package org.hkijena.jipipe.extensions.expressions.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.UndefinedExpressionParameterVariablesInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopRSyntaxTextField;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;
import java.util.*;

public class JIPipeExpressionDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private final JPanel expressionEditorPanel = new JPanel(new BorderLayout());

    private final JIPipeExpressionEvaluatorSyntaxTokenMaker tokenMaker = new JIPipeExpressionEvaluatorSyntaxTokenMaker();
    private final Set<JIPipeExpressionParameterVariableInfo> variables = new HashSet<>();
    private RSyntaxTextArea expressionEditor;

    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterTree   the parameter tree
     * @param parameterAccess Parameter
     */
    public JIPipeExpressionDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
//        reloadVariables();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
//        add(availableModes, BorderLayout.WEST);

//        JPanel optionPanel = new JPanel();
//        optionPanel.setOpaque(true);
//        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.X_AXIS));
//        optionPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Button.borderColor")));

        JButton functionBuilder = new JButton("Edit", UIUtils.getIconFromResources("actions/edit.png"));
//        UIUtils.makeFlat25x25(functionBuilder);
//        functionBuilder.setPreferredSize(new Dimension(80, 25));
//        functionBuilder.setMaximumSize(new Dimension(80, 25));
//        optionPanel.add(functionBuilder);
        functionBuilder.addActionListener(e -> editInFunctionBuilder());

        expressionEditorPanel.add(functionBuilder, BorderLayout.EAST);

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
        expressionEditor = new JIPipeDesktopRSyntaxTextField(document);
        expressionEditor.setFocusTraversalKeysEnabled(true);
        UIUtils.applyThemeToCodeEditor(expressionEditor);
        expressionEditor.setBackground(UIManager.getColor("TextArea.background"));
        expressionEditor.setLineWrap(true);
        expressionEditor.setHighlightCurrentLine(false);
        expressionEditor.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                JIPipeExpressionParameter parameter = getParameter(JIPipeExpressionParameter.class);
                if (!Objects.equals(parameter.getExpression(), expressionEditor.getText())) {
                    parameter.setExpression(expressionEditor.getText());
                    setParameter(parameter, false);
                }
            }
        });
        expressionEditorPanel.setBorder(UIUtils.createControlBorder());
        expressionEditorPanel.setOpaque(true);
        expressionEditorPanel.setBackground(UIManager.getColor("TextArea.background"));
        JPanel borderPanel = new JPanel(new BorderLayout());
        borderPanel.setBackground(UIManager.getColor("TextArea.background"));
        borderPanel.setBorder(BorderFactory.createEmptyBorder(5, 4, 4, 4));
        borderPanel.add(expressionEditor, BorderLayout.CENTER);

        JLabel expressionHintLabel = new JLabel("Expression");
        expressionHintLabel.setForeground(Color.GRAY);
        expressionHintLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 10));
        expressionHintLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        borderPanel.add(expressionHintLabel, BorderLayout.NORTH);

        expressionEditorPanel.add(borderPanel, BorderLayout.CENTER);

        add(expressionEditorPanel, BorderLayout.CENTER);

        JIPipeExpressionParameterSettings settings = getParameterAccess().getAnnotationOfType(JIPipeExpressionParameterSettings.class);
        if (settings != null) {
            if (!StringUtils.isNullOrEmpty(settings.hint())) {
                expressionHintLabel.setText("Expression: " + settings.hint());
            }
        }
    }

    private void editInFunctionBuilder() {
        reloadVariables();
        String expression = ExpressionBuilderUI.showDialog(getDesktopWorkbench().getWindow(), expressionEditor.getText(), variables);
        if (expression != null)
            expressionEditor.setText(expression);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        JIPipeExpressionParameter parameter = getParameter(JIPipeExpressionParameter.class);
        if (!Objects.equals(parameter.getExpression(), expressionEditor.getText())) {
            expressionEditor.setText(parameter.getExpression());
        }
    }

    private void reloadVariables() {
        variables.clear();

        // Read from parameter
        Class<?> fieldClass = getParameterAccess().getFieldClass();
        {
            JIPipeExpressionParameterSettings settings = getParameterAccess().getAnnotationOfType(JIPipeExpressionParameterSettings.class);
            if (settings == null) {
                settings = fieldClass.getAnnotation(JIPipeExpressionParameterSettings.class);
            }
            if (settings != null && settings.variableSource() != UndefinedExpressionParameterVariablesInfo.class) {
                ExpressionParameterVariablesInfo variableSource = (ExpressionParameterVariablesInfo) ReflectionUtils.newInstance(settings.variableSource());
                variables.addAll(variableSource.getVariables(getParameterTree(), getParameterAccess()));
            }
            List<JIPipeExpressionParameterVariable> variableAnnotations = getParameterAccess().getAnnotationsOfType(JIPipeExpressionParameterVariable.class);
            if (variableAnnotations.isEmpty()) {
                // Maybe the repeatable is not resolved
                JIPipeExpressionParameterVariables container = getParameterAccess().getAnnotationOfType(JIPipeExpressionParameterVariables.class);
                if (container != null) {
                    variableAnnotations.addAll(Arrays.asList(container.value()));
                }
            }
            for (JIPipeExpressionParameterVariable variable : variableAnnotations) {
                if (!StringUtils.isNullOrEmpty(variable.name()) || !StringUtils.isNullOrEmpty(variable.description()) || !StringUtils.isNullOrEmpty(variable.key())) {
                    variables.add(new JIPipeExpressionParameterVariableInfo(variable.key(), variable.name(), variable.description()));
                }
                if (variable.fromClass() != UndefinedExpressionParameterVariablesInfo.class) {
                    ExpressionParameterVariablesInfo variableSource = (ExpressionParameterVariablesInfo) ReflectionUtils.newInstance(variable.fromClass());
                    variables.addAll(variableSource.getVariables(getParameterTree(), getParameterAccess()));
                }
            }
        }
        // Read from field class
        {
            for (JIPipeExpressionParameterVariable variable : fieldClass.getAnnotationsByType(JIPipeExpressionParameterVariable.class)) {
                if (!StringUtils.isNullOrEmpty(variable.name()) || !StringUtils.isNullOrEmpty(variable.description()) || !StringUtils.isNullOrEmpty(variable.key())) {
                    variables.add(new JIPipeExpressionParameterVariableInfo(variable.key(), variable.name(), variable.description()));
                }
                if (variable.fromClass() != UndefinedExpressionParameterVariablesInfo.class) {
                    ExpressionParameterVariablesInfo variableSource = (ExpressionParameterVariablesInfo) ReflectionUtils.newInstance(variable.fromClass());
                    variables.addAll(variableSource.getVariables(getParameterTree(), getParameterAccess()));
                }
            }
        }
        // Read from parameter
        variables.addAll(getParameter(JIPipeExpressionParameter.class).getAdditionalUIVariables());

    }
}
