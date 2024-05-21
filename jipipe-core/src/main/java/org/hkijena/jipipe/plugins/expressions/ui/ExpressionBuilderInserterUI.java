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

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ExpressionBuilderInserterUI extends JPanel {
    private final ExpressionBuilderUI expressionBuilderUI;
    private final Object insertedObject;
    private boolean inserterCommitted = false;

    // Inserter
    private List<ExpressionBuilderParameterUI> inserterParameterEditorUIList;

    public ExpressionBuilderInserterUI(ExpressionBuilderUI expressionBuilderUI, Object insertedObject) {
        this.expressionBuilderUI = expressionBuilderUI;
        this.insertedObject = insertedObject;
        initialize(insertedObject);
    }

    private void initialize(Object insertedObject) {
        setLayout(new BorderLayout());

        JIPipeDesktopFormPanel inserterForm = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
        add(inserterForm, BorderLayout.CENTER);

        JPanel inserterButtonPanel = new JPanel();
        inserterButtonPanel.setLayout(new BoxLayout(inserterButtonPanel, BoxLayout.X_AXIS));
        add(inserterButtonPanel, BorderLayout.SOUTH);

        if (insertedObject instanceof JIPipeExpressionParameterVariableInfo) {
            JIPipeExpressionParameterVariableInfo variable = (JIPipeExpressionParameterVariableInfo) insertedObject;
            JIPipeDesktopFormPanel.GroupHeaderPanel groupHeader = inserterForm.addGroupHeader(String.format("<html><i style=\"color: #ffaf0a; \">Variable</i> %s (<code><strong>%s</strong></code>)</html>",
                    HtmlEscapers.htmlEscaper().escape(variable.getName()),
                    HtmlEscapers.htmlEscaper().escape(variable.getKey())), UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription(variable.getDescription());

            if (!StringUtils.isNullOrEmpty(((JIPipeExpressionParameterVariableInfo) insertedObject).getKey())) {
                JButton insertButton = new JButton("Insert", UIUtils.getIconFromResources("actions/insert-object.png"));
                insertButton.addActionListener(e -> {
                    inserterCommitted = true;
                    expressionBuilderUI.insertVariableAtCaret(variable.getKey());
                });
                inserterButtonPanel.add(Box.createHorizontalGlue());
                inserterButtonPanel.add(insertButton);
            }
        } else if (insertedObject instanceof ExpressionConstantEntry) {
            ExpressionConstantEntry constantEntry = (ExpressionConstantEntry) insertedObject;
            JIPipeDesktopFormPanel.GroupHeaderPanel groupHeader = inserterForm.addGroupHeader(String.format("<html><i style=\"color: #0000ff; \">Constant</i> %s (<code><strong>%s</strong></code>)</html>",
                    HtmlEscapers.htmlEscaper().escape(constantEntry.getName()),
                    HtmlEscapers.htmlEscaper().escape(constantEntry.getConstant().getName())), UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription(constantEntry.getDescription());

            inserterButtonPanel.add(Box.createHorizontalGlue());

            JButton insertSimilarVariable = new JButton("Insert variable with same name", UIUtils.getIconFromResources("actions/variable.png"));
            insertSimilarVariable.setToolTipText("Inserts a variable that has the same name as the constant.");
            insertSimilarVariable.addActionListener(e -> {
                inserterCommitted = true;
                expressionBuilderUI.insertAtCaret("$\"" + constantEntry.getConstant().getName() + "\"", true);
            });
            inserterButtonPanel.add(insertSimilarVariable);

            JButton insertButton = new JButton("Insert", UIUtils.getIconFromResources("actions/insert-object.png"));
            insertButton.setToolTipText("Inserts the constant.");
            insertButton.addActionListener(e -> {
                inserterCommitted = true;
                expressionBuilderUI.insertAtCaret(constantEntry.getConstant().getName(), true);
            });
            inserterButtonPanel.add(insertButton);
        } else if (insertedObject instanceof ExpressionOperatorEntry) {
            ExpressionOperatorEntry operatorEntry = (ExpressionOperatorEntry) insertedObject;
            JIPipeDesktopFormPanel.GroupHeaderPanel groupHeader = inserterForm.addGroupHeader(String.format("<html><i style=\"color: #854745; \">Operator</i> %s (<code><strong>%s</strong></code>)</html>",
                    HtmlEscapers.htmlEscaper().escape(operatorEntry.getName()),
                    HtmlEscapers.htmlEscaper().escape(operatorEntry.getSignature())), UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription(operatorEntry.getDescription());

            inserterParameterEditorUIList = new ArrayList<>();
            for (int i = 0; i < operatorEntry.getOperator().getOperandCount(); i++) {
                ParameterInfo info = operatorEntry.getParameterInfo(i);
                ExpressionBuilderParameterUI parameterUI = new ExpressionBuilderParameterUI();
                JLabel infoLabel = new JLabel(info.getName());
                infoLabel.setToolTipText(info.getDescription());
                appendTooltipForParameterLabel(info, infoLabel);
                inserterForm.addToForm(parameterUI, infoLabel, null);
                inserterParameterEditorUIList.add(parameterUI);
            }

            inserterButtonPanel.add(Box.createHorizontalGlue());

            JButton insertButton = new JButton("Insert", UIUtils.getIconFromResources("actions/insert-object.png"));
            insertButton.setToolTipText("Inserts the operator with parameters.");
            insertButton.addActionListener(e -> {
                inserterCommitted = true;
                expressionBuilderUI.insertOperator(operatorEntry, inserterParameterEditorUIList);
            });
            inserterButtonPanel.add(insertButton);

            JButton insertSymbolButton = new JButton("Insert only symbol", UIUtils.getIconFromResources("actions/format-text-symbol.png"));
            insertSymbolButton.setToolTipText("Inserts the operator symbol.");
            insertSymbolButton.addActionListener(e -> {
                inserterCommitted = true;
                expressionBuilderUI.insertAtCaret(operatorEntry.getOperator().getSymbol(), true);
            });
            inserterButtonPanel.add(insertSymbolButton);
        } else if (insertedObject instanceof JIPipeExpressionRegistry.ExpressionFunctionEntry) {
            JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = (JIPipeExpressionRegistry.ExpressionFunctionEntry) insertedObject;
            ExpressionFunction function = functionEntry.getFunction();
            JIPipeDesktopFormPanel.GroupHeaderPanel groupHeader = inserterForm.addGroupHeader(String.format("<html><i style=\"color: #14A0B3; \">Function</i> %s (<code><strong>%s</strong></code>)</html>",
                    HtmlEscapers.htmlEscaper().escape(functionEntry.getName()),
                    HtmlEscapers.htmlEscaper().escape(function.getSignature())), UIUtils.getIconFromResources("actions/insert-function.png"));

            StringBuilder descriptionBuilder = new StringBuilder();
            descriptionBuilder.append("<html>");
            descriptionBuilder.append("<div>").append(functionEntry.getDescription()).append("</div>");
            if (function.getMaximumArgumentCount() > 0) {
//                descriptionBuilder.append("<h2>Parameters</h2>");
                descriptionBuilder.append("<table>");
                for (int i = 0; i < Math.min(5, function.getMaximumArgumentCount()); i++) {
                    ParameterInfo parameterInfo = function.getParameterInfo(i);
                    descriptionBuilder.append("<tr>");
                    if (parameterInfo != null) {
                        descriptionBuilder.append("<td><code>").append(parameterInfo.getName()).append("</code></td><td>")
                                .append(StringUtils.orElse(parameterInfo.getDescription(), "No description provided")).append("</td>");
                    } else {
                        descriptionBuilder.append("<td><code>").append("x").append(i + 1).append("</code></td><td>No description provided</td>");
                    }
                    descriptionBuilder.append("</tr>");
                }
                if (function.getMaximumArgumentCount() > 5) {
                    descriptionBuilder.append("<tr><td>...</td></tr>");
                }
                descriptionBuilder.append("</table>");
            }
            descriptionBuilder.append("</html>");

            groupHeader.setDescription(descriptionBuilder.toString());

            inserterParameterEditorUIList = new ArrayList<>();
            if (function.getMinimumArgumentCount() < function.getMaximumArgumentCount()) {
                JButton addParameterButton = new JButton("Add parameter", UIUtils.getIconFromResources("actions/list-add.png"));
                addParameterButton.addActionListener(e -> {
                    if (inserterParameterEditorUIList.size() < function.getMaximumArgumentCount()) {
                        ParameterInfo info = function.getParameterInfo(inserterParameterEditorUIList.size());
                        ExpressionBuilderParameterUI parameterUI = new ExpressionBuilderParameterUI();
                        JLabel infoLabel = new JLabel(info.getName());
                        infoLabel.setToolTipText(info.getDescription());
                        appendTooltipForParameterLabel(info, infoLabel);
                        inserterForm.removeLastRow();
                        inserterForm.addToForm(parameterUI, infoLabel, null);
                        inserterForm.addVerticalGlue();
                        inserterParameterEditorUIList.add(parameterUI);
                        inserterForm.revalidate();
                        inserterForm.repaint();
                        inserterCommitted = false;
                    }
                });
                groupHeader.addColumn(addParameterButton);
            }
            for (int i = 0; i < function.getMinimumArgumentCount(); i++) {
                ParameterInfo info = function.getParameterInfo(i);
                ExpressionBuilderParameterUI parameterUI = new ExpressionBuilderParameterUI();
                JLabel infoLabel = new JLabel(info.getName());
                appendTooltipForParameterLabel(info, infoLabel);
                inserterForm.addToForm(parameterUI, infoLabel, null);
                inserterParameterEditorUIList.add(parameterUI);
            }

            inserterButtonPanel.add(Box.createHorizontalGlue());

            JButton insertButton = new JButton("Insert", UIUtils.getIconFromResources("actions/insert-object.png"));
            insertButton.setToolTipText("Inserts the function with parameters.");
            insertButton.addActionListener(e -> {
                inserterCommitted = true;
                expressionBuilderUI.insertFunction(functionEntry, inserterParameterEditorUIList);
            });
            inserterButtonPanel.add(insertButton);

            JButton insertSymbolButton = new JButton("Insert only symbol", UIUtils.getIconFromResources("actions/format-text-symbol.png"));
            insertSymbolButton.setToolTipText("Inserts the function symbol.");
            insertSymbolButton.addActionListener(e -> {
                inserterCommitted = true;
                expressionBuilderUI.insertAtCaret(function.getName(), true);
            });
            inserterButtonPanel.add(insertSymbolButton);
        } else {
            inserterCommitted = true;
            JIPipeDesktopFormPanel.GroupHeaderPanel groupHeader = inserterForm.addGroupHeader("Expression builder", UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription("Welcome to the expression builder that simplifies the creation of expressions. On the right-hand side you will find a list of all available functions and operators. " +
                    "Please note that the list of variables can be incomplete depending on various factors.");
        }
        inserterForm.addVerticalGlue();
    }

    /**
     * Checks if parameters were edited. Returns true if no parameters are available.
     *
     * @return true if no parameters are available.
     */
    public boolean parametersWereEdited() {
        if (getInserterParameterEditorUIList() != null && getInserterParameterEditorUIList().size() > 0) {
            for (ExpressionBuilderParameterUI ui : getInserterParameterEditorUIList()) {
                if (!StringUtils.isNullOrEmpty(ui.getCurrentExpressionValue())) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

    public List<ExpressionBuilderParameterUI> getInserterParameterEditorUIList() {
        return inserterParameterEditorUIList;
    }

    private void appendTooltipForParameterLabel(ParameterInfo info, JLabel infoLabel) {
        if (!StringUtils.isNullOrEmpty(info.getDescription()) && !info.getTypes().isEmpty())
            return;
        StringBuilder tooltipBuilder = new StringBuilder();
        tooltipBuilder.append("<html>");
        if (!StringUtils.isNullOrEmpty(info.getDescription()))
            tooltipBuilder.append(info.getDescription()).append("<br/><br/>");
        for (Class<?> type : info.getTypes()) {
            if (type == String.class) {
                tooltipBuilder.append("Accepts: Strings<br/>");
            } else if (Number.class.isAssignableFrom(type)) {
                tooltipBuilder.append("Accepts: Numbers<br/>");
            } else if (Collection.class.isAssignableFrom(type)) {
                tooltipBuilder.append("Accepts: Arrays<br/>");
            } else if (Map.class.isAssignableFrom(type)) {
                tooltipBuilder.append("Accepts: Maps<br/>");
            } else {
                tooltipBuilder.append("Accepts: ").append(type.getSimpleName()).append("<br/>");
            }
        }
        tooltipBuilder.append("</html>");
        infoLabel.setToolTipText(tooltipBuilder.toString());
    }

    public boolean isInserterCommitted() {
        return inserterCommitted;
    }

    public Object getInsertedObject() {
        return insertedObject;
    }
}
