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

package org.hkijena.jipipe.utils.ui;

import com.fathzer.soft.javaluator.Function;
import com.fathzer.soft.javaluator.Operator;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.expressions.ui.JIPipeExpressionDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A developer tool to test {@link JIPipeExpressionEvaluator}
 */
public class ExpressionTesterUI extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeExpressionEvaluator evaluator = new JIPipeExpressionEvaluator();
    private final JIPipeDesktopMarkdownReader resultReader = new JIPipeDesktopMarkdownReader(false);
    private final StringBuilder resultOutput = new StringBuilder();
    private JIPipeExpressionDesktopParameterEditorUI expressionEditor;
    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter();

    public ExpressionTesterUI(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new BorderLayout());

        // Creatr the main panel
        centerPanel.add(resultReader, BorderLayout.CENTER);

        JIPipeManualParameterAccess access = JIPipeManualParameterAccess.builder().setName("Expression").setFieldClass(JIPipeParameter.class)
                .setGetter(this::getExpression).setSetter(o -> {
                    setExpression((JIPipeExpressionParameter) o);
                    return true;
                }).setSource(new JIPipeDummyParameterCollection()).build();
        expressionEditor = new JIPipeExpressionDesktopParameterEditorUI(new JIPipeDesktopParameterEditorUI.InitializationParameters(getDesktopWorkbench(), new JIPipeParameterTree(access.getSource()), access));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(expressionEditor, BorderLayout.CENTER);

        JButton runButton = new JButton("Evaluate", UIUtils.getIconFromResources("actions/run-build.png"));
        runButton.addActionListener(e -> evaluate());
        bottomPanel.add(runButton, BorderLayout.EAST);

        centerPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Generate a help panel
        StringBuilder helpText = new StringBuilder();
        helpText.append("<table>");
        for (Operator operator : evaluator.getOperators()) {
            ExpressionOperatorEntry info = new ExpressionOperatorEntry(operator);
            helpText.append("<tr><td><pre>").append(HtmlEscapers.htmlEscaper().escape(info.getSignature())).append("</pre></td>")
                    .append("<td>").append(info.getName()).append("</td><td>").append(info.getDescription()).append("</td></tr>");
        }
        for (Function function : evaluator.getFunctions()) {
            String signature = function.getName() + "(...)";
            String name = "";
            String description = "";
            if (function instanceof ExpressionFunction) {
                JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = JIPipe.getInstance().getExpressionRegistry().getRegisteredExpressionFunctions().getOrDefault(function.getName(), null);
                if (functionEntry != null) {
                    signature = functionEntry.getFunction().getSignature();
                    name = functionEntry.getName();
                    description = functionEntry.getDescription();
                }
            }
            helpText.append("<tr><td><pre>").append(HtmlEscapers.htmlEscaper().escape(signature)).append("</pre></td>")
                    .append("<td>").append(name).append("</td><td>").append(description).append("</td></tr>");
        }
        helpText.append("<table>\n\n");

        JIPipeDesktopMarkdownReader helpReader = new JIPipeDesktopMarkdownReader(true, new MarkdownText(helpText.toString()));

        // Add everything into a split panel
        JSplitPane splitPane = new JIPipeDesktopSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                centerPanel,
                helpReader, JIPipeDesktopSplitPane.RATIO_3_TO_1);
        add(splitPane, BorderLayout.CENTER);
    }

    public void evaluate() {
        String expressionToEvaluate = expression.getExpression();
        resultOutput.append("<table><tr><td>Input</td>");
        resultOutput.append("<td><pre>").append(HtmlEscapers.htmlEscaper().escape(expressionToEvaluate)).append("</pre></td></tr>");
        Object result;
        try {
            result = evaluator.evaluate(expressionToEvaluate, new JIPipeExpressionVariablesMap());
            if (result instanceof Collection) {
                List<Object> values = new ArrayList<>();
                for (Object item : (Collection<?>) result) {
                    if (item instanceof Boolean || item instanceof Number)
                        values.add(item);
                    else
                        values.add("" + item);
                }
                result = JsonUtils.toJsonString(values);
            }
            if (result instanceof Map) {
                Map<String, Object> values = new HashMap<>();
                for (Map.Entry<?, ?> item : ((Map<?, ?>) result).entrySet()) {
                    String key = "" + item.getKey();
                    Object value;
                    if (item.getValue() instanceof Boolean || item.getValue() instanceof Number)
                        value = item.getValue();
                    else
                        value = "" + item.getValue();
                    values.put(key, value);
                }
                result = JsonUtils.toJsonString(values);
            }
        } catch (Throwable e) {
            result = e;
        }
        resultOutput.append("<tr><td>Output</td><td><pre>").append(HtmlEscapers.htmlEscaper().escape("" + result)).append("</pre></td></tr></table>");
        resultOutput.append("<hr/>\n");
        resultReader.setDocument(new MarkdownText(resultOutput.toString()));
        SwingUtilities.invokeLater(() -> resultReader.getScrollPane().getVerticalScrollBar().setValue(resultReader.getScrollPane().getVerticalScrollBar().getMaximum()));
    }

    public JIPipeExpressionParameter getExpression() {
        return expression;
    }

    public void setExpression(JIPipeExpressionParameter expression) {
        this.expression = expression;
    }
}
