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

import org.fife.ui.rsyntaxtextarea.*;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopRSyntaxTextField;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.UndefinedExpressionParameterVariablesInfo;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.*;
import java.util.List;

import static org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE;

public class JIPipeExpressionDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private final JPanel expressionEditorPanel = new JPanel(new BorderLayout());

    private final AbstractTokenMaker tokenMaker;
    private final Set<JIPipeExpressionParameterVariableInfo> variables = new HashSet<>();
    private final JPanel editPanel = UIUtils.boxHorizontal();
    private RSyntaxTextArea expressionEditor;

    public JIPipeExpressionDesktopParameterEditorUI(InitializationParameters parameters) {
        super(parameters);

        // Init the token maker
        JIPipeExpressionParameterSettings settings = getParameterAccess().getAnnotationOfType(JIPipeExpressionParameterSettings.class);
        if (settings != null) {
            tokenMaker = (AbstractTokenMaker) ReflectionUtils.newInstance(settings.tokenMaker());
        } else {
            tokenMaker = new JIPipeExpressionEvaluatorSyntaxTokenMaker();
        }

        initialize();
//        reloadVariables();
        reload();
    }

    private void initialize() {
        JIPipeExpressionParameterSettings settings = getParameterAccess().getAnnotationOfType(JIPipeExpressionParameterSettings.class);
        setLayout(new BorderLayout());

        JButton functionBuilder = new JButton("Edit", UIUtils.getIconFromResources("actions/edit.png"));
        functionBuilder.addActionListener(e -> editInFunctionBuilder());
        editPanel.setOpaque(false);

        if (settings == null || !settings.withoutEditorButton()) {
            editPanel.add(functionBuilder);
        }

        expressionEditorPanel.add(editPanel, BorderLayout.EAST);

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


        if (settings != null) {
            if (!StringUtils.isNullOrEmpty(settings.hint())) {
                expressionHintLabel.setText("Expression: " + settings.hint());
            }
        }
    }

    private void editInFunctionBuilder() {
        reloadVariables();
        String expression = ExpressionBuilderUI.showDialog(getDesktopWorkbench().getWindow(), getDesktopWorkbench(), expressionEditor.getText(), variables);
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

    private JIPipeGraphNode searchForNodeInParents() {
        for (JIPipeParameterCollection source : getParameterTree().getRegisteredSources()) {
            if (source instanceof JIPipeGraphNode) {
                return (JIPipeGraphNode) source;
            }
        }
        return null;
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
                JIPipeExpressionVariablesInfo variableSource = (JIPipeExpressionVariablesInfo) ReflectionUtils.newInstance(settings.variableSource());
                variables.addAll(variableSource.getVariables(getWorkbench(), getParameterTree(), getParameterAccess()));
            }
            List<AddJIPipeExpressionParameterVariable> variableAnnotations = getParameterAccess().getAnnotationsOfType(AddJIPipeExpressionParameterVariable.class);
            if (variableAnnotations.isEmpty()) {
                // Maybe the repeatable is not resolved
                JIPipeExpressionParameterVariables container = getParameterAccess().getAnnotationOfType(JIPipeExpressionParameterVariables.class);
                if (container != null) {
                    variableAnnotations.addAll(Arrays.asList(container.value()));
                }
            }
            for (AddJIPipeExpressionParameterVariable variable : variableAnnotations) {
                if (!StringUtils.isNullOrEmpty(variable.name()) || !StringUtils.isNullOrEmpty(variable.description()) || !StringUtils.isNullOrEmpty(variable.key())) {
                    variables.add(new JIPipeExpressionParameterVariableInfo(variable.key(), variable.name(), variable.description()));
                }
                if (variable.fromClass() != UndefinedExpressionParameterVariablesInfo.class) {
                    JIPipeExpressionVariablesInfo variableSource = (JIPipeExpressionVariablesInfo) ReflectionUtils.newInstance(variable.fromClass());
                    variables.addAll(variableSource.getVariables(getWorkbench(), getParameterTree(), getParameterAccess()));
                }
            }
        }
        // Read from field class
        {
            for (AddJIPipeExpressionParameterVariable variable : fieldClass.getAnnotationsByType(AddJIPipeExpressionParameterVariable.class)) {
                if (!StringUtils.isNullOrEmpty(variable.name()) || !StringUtils.isNullOrEmpty(variable.description()) || !StringUtils.isNullOrEmpty(variable.key())) {
                    variables.add(new JIPipeExpressionParameterVariableInfo(variable.key(), variable.name(), variable.description()));
                }
                if (variable.fromClass() != UndefinedExpressionParameterVariablesInfo.class) {
                    JIPipeExpressionVariablesInfo variableSource = (JIPipeExpressionVariablesInfo) ReflectionUtils.newInstance(variable.fromClass());
                    variables.addAll(variableSource.getVariables(getWorkbench(), getParameterTree(), getParameterAccess()));
                }
            }
        }
        // Read from parameter
        variables.addAll(getParameter(JIPipeExpressionParameter.class).getAdditionalUIVariables());

        // Special handling of global parameters (associated to nodes)
        JIPipeGraphNode graphNode = searchForNodeInParents();
        if (graphNode instanceof JIPipeAlgorithm) {
            Set<String> usedKeys = new HashSet<>();
            for (JIPipeExpressionParameterVariableInfo variable : variables) {
                usedKeys.add(variable.getKey());
            }

            // Add custom variables
            if (((JIPipeAlgorithm) graphNode).isEnableDefaultCustomExpressionVariables()) {
                for (JIPipeExpressionParameterVariableInfo variable : JIPipeCustomExpressionVariablesParameterVariablesInfo.VARIABLES) {
                    if (!usedKeys.contains(variable.getKey()) || StringUtils.isNullOrEmpty(variable.getKey())) {
                        variables.add(variable);
                        usedKeys.add(variable.getKey());
                    }
                }
            }

            // Add directories
            for (JIPipeExpressionParameterVariableInfo variable : JIPipeProjectDirectoriesVariablesInfo.VARIABLES) {
                if (!usedKeys.contains(variable.getKey()) || StringUtils.isNullOrEmpty(variable.getKey())) {
                    variables.add(variable);
                    usedKeys.add(variable.getKey());
                }
            }

            // Add custom global variables
            JIPipeProject project = getWorkbench().getProject();
            if (project != null) {
                for (Map.Entry<String, JIPipeParameterAccess> entry : project.getMetadata().getGlobalParameters().getParameters().entrySet()) {
                    variables.add(new JIPipeExpressionParameterVariableInfo("_global." + entry.getKey(), StringUtils.orElse(entry.getValue().getName(), entry.getKey()), entry.getValue().getDescription()));
                }
            }

            // Handling of iterating algorithms (adds annotations)
            if (graphNode instanceof JIPipeIterationStepAlgorithm) {
                // Generated annotations map
                variables.add(new JIPipeExpressionParameterVariableInfo("_local.annotations", "Text annotations (map)", "The text annotations of the current iteration step as map"));
                if (!usedKeys.contains(ANNOTATIONS_VARIABLE.getKey()) || StringUtils.isNullOrEmpty(ANNOTATIONS_VARIABLE.getKey())) {
                    variables.add(ANNOTATIONS_VARIABLE);
                }
            }

        }

    }

    public JPanel getEditPanel() {
        return editPanel;
    }
}
