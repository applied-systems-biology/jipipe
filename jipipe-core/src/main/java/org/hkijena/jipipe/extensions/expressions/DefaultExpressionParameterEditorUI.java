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

package org.hkijena.jipipe.extensions.expressions;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.RSyntaxTextField;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DefaultExpressionParameterEditorUI extends JIPipeParameterEditorUI {

    private final JPanel expressionEditorPanel = new JPanel(new BorderLayout());
    private RSyntaxTextArea expressionEditor;
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
        reloadVariables();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
//        add(availableModes, BorderLayout.WEST);

        JPanel optionPanel = new JPanel();
        optionPanel.setOpaque(true);
        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.X_AXIS));
        optionPanel.setBorder(BorderFactory.createMatteBorder(0,1,0,0, UIManager.getColor("Button.borderColor")));

        JButton functionBuilder = new JButton("Edit", UIUtils.getIconFromResources("actions/insert-math-expression.png"));
        UIUtils.makeFlat25x25(functionBuilder);
        functionBuilder.setPreferredSize(new Dimension(80, 25));
        functionBuilder.setMaximumSize(new Dimension(80, 25));
        optionPanel.add(functionBuilder);
        functionBuilder.addActionListener(e -> editInFunctionBuilder());

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
        expressionEditor = new RSyntaxTextField(document);
        expressionEditor.setFocusTraversalKeysEnabled(true);
        UIUtils.applyThemeToCodeEditor(expressionEditor);
        expressionEditor.setBackground(UIManager.getColor("TextArea.background"));
        expressionEditor.setLineWrap(true);
        expressionEditor.setHighlightCurrentLine(false);
        expressionEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                DefaultExpressionParameter parameter = getParameter(DefaultExpressionParameter.class);
                if (!Objects.equals(parameter.getExpression(), expressionEditor.getText())) {
                    parameter.setExpression(expressionEditor.getText());
                    setParameter(parameter, false);
                }
            }
        });
        expressionEditorPanel.setBorder(BorderFactory.createEtchedBorder());
        expressionEditorPanel.setOpaque(true);
        expressionEditorPanel.setBackground(UIManager.getColor("TextArea.background"));
        JPanel borderPanel = new JPanel(new BorderLayout());
        borderPanel.setBackground(UIManager.getColor("TextArea.background"));
        borderPanel.setBorder(BorderFactory.createEmptyBorder(5, 4, 0, 4));
        borderPanel.add(expressionEditor, BorderLayout.CENTER);
        expressionEditorPanel.add(borderPanel, BorderLayout.CENTER);

        add(expressionEditorPanel, BorderLayout.CENTER);
    }

    private void editInFunctionBuilder() {
        String expression = ExpressionBuilderUI.showDialog(getWorkbench().getWindow(), expressionEditor.getText(), variables);
        if (expression != null)
            expressionEditor.setText(expression);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        DefaultExpressionParameter parameter = getParameter(DefaultExpressionParameter.class);
        if (!Objects.equals(parameter.getExpression(), expressionEditor.getText())) {
            expressionEditor.setText(parameter.getExpression());
        }
    }

    private void reloadVariables() {
        variables.clear();
        ExpressionParameterSettings settings = getParameterAccess().getAnnotationOfType(ExpressionParameterSettings.class);
        if (settings == null) {
            settings = getParameterAccess().getFieldClass().getAnnotation(ExpressionParameterSettings.class);
        }
        if (settings != null) {
            ExpressionParameterVariableSource variableSource = (ExpressionParameterVariableSource) ReflectionUtils.newInstance(settings.variableSource());
            variables.addAll(variableSource.getVariables(getParameterAccess()));
        }
    }
}
