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

package org.hkijena.jipipe.utils;

import com.fathzer.soft.javaluator.Function;
import com.fathzer.soft.javaluator.Operator;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionEvaluator;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameterEditorUI;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionOperatorEntry;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A developer tool to test {@link org.hkijena.jipipe.extensions.expressions.DefaultExpressionEvaluator}
 */
public class ExpressionTesterUI extends JIPipeWorkbenchPanel {
    private final DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();
    private final MarkdownReader resultReader = new MarkdownReader(false);
    private final StringBuilder resultOutput = new StringBuilder();
    private DefaultExpressionParameterEditorUI expressionEditor;
    private DefaultExpressionParameter expression = new DefaultExpressionParameter();

    public ExpressionTesterUI(JIPipeWorkbench workbench) {
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
                    setExpression((DefaultExpressionParameter) o);
                    return true;
                }).setSource(new JIPipeDummyParameterCollection()).build();
        expressionEditor = new DefaultExpressionParameterEditorUI(getWorkbench(), access);

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

        MarkdownReader helpReader = new MarkdownReader(true, new MarkdownDocument(helpText.toString()));

        // Add everything into a split panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                centerPanel,
                helpReader);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.66);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });
        add(splitPane, BorderLayout.CENTER);
    }

    public void evaluate() {
        String expressionToEvaluate = expression.getExpression();
        resultOutput.append("<table><tr><td>Input</td>");
        resultOutput.append("<td><pre>").append(HtmlEscapers.htmlEscaper().escape(expressionToEvaluate)).append("</pre></td></tr>");
        Object result;
        try {
            result = evaluator.evaluate(expressionToEvaluate, new ExpressionVariables());
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
        } catch (Exception e) {
            result = e;
        }
        resultOutput.append("<tr><td>Output</td><td><pre>").append(HtmlEscapers.htmlEscaper().escape("" + result)).append("</pre></td></tr></table>");
        resultOutput.append("<hr/>\n");
        resultReader.setDocument(new MarkdownDocument(resultOutput.toString()));
        SwingUtilities.invokeLater(() -> resultReader.getScrollPane().getVerticalScrollBar().setValue(resultReader.getScrollPane().getVerticalScrollBar().getMaximum()));
    }

    public DefaultExpressionParameter getExpression() {
        return expression;
    }

    public void setExpression(DefaultExpressionParameter expression) {
        this.expression = expression;
    }
}
