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
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionEvaluator;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterEditorUI;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * A developer tool to test {@link org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionEvaluator}
 */
public class ExpressionTesterUI extends JIPipeWorkbenchPanel {
    private final DefaultExpressionEvaluator evaluator = new DefaultExpressionEvaluator();
    private final MarkdownReader resultReader = new MarkdownReader(false);
    private final StringBuilder resultOutput = new StringBuilder();
    private ExpressionParameterEditorUI expressionEditor;
    private ExpressionParameter expression = new ExpressionParameter();

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
                .setGetter(this::getExpression).setSetter(o -> { setExpression((ExpressionParameter) o); return true; }).setSource(new JIPipeDummyParameterCollection()).build();
        expressionEditor = new ExpressionParameterEditorUI(getWorkbench(), access);

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
            JIPipeDocumentation documentation = operator.getClass().getAnnotation(JIPipeDocumentation.class);
            if(documentation == null) {
                documentation = new JIPipeDefaultDocumentation("", "");
            }
            String operatorInfo;
            if(operator.getOperandCount() == 1) {
                operatorInfo = operator.getSymbol() + " [value]";
            }
            else {
                operatorInfo = "[value] " + operator.getSymbol() + " [value]";
            }
           helpText.append("<tr><td><pre>").append(HtmlEscapers.htmlEscaper().escape(operatorInfo)).append("</pre></td>")
                   .append("<td>").append(documentation.name()).append("</td><td>").append(documentation.description()).append("</td></tr>");
        }
        for (Function function : evaluator.getFunctions()) {
            String name = "";
            String description = "";
            if(function instanceof ExpressionFunction) {
                JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = JIPipe.getInstance().getExpressionRegistry().getRegisteredExpressionFunctions().getOrDefault(function.getName(), null);
                if(functionEntry != null) {
                    name = functionEntry.getName();
                    description = functionEntry.getDescription();
                }
            }
            helpText.append("<tr><td><pre>").append(HtmlEscapers.htmlEscaper().escape(function.getName() + "(...)")).append("</pre></td>")
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
            result = evaluator.evaluate(expressionToEvaluate);
        }
        catch (Exception e) {
            result = e;
        }
        resultOutput.append("<tr><td>Output</td><td><pre>").append(HtmlEscapers.htmlEscaper().escape("" + result)).append("</pre></td></tr></table>");
        resultOutput.append("<hr/>\n");
        resultReader.setDocument(new MarkdownDocument(resultOutput.toString()));
        SwingUtilities.invokeLater(() ->  resultReader.getScrollPane().getVerticalScrollBar().setValue(resultReader.getScrollPane().getVerticalScrollBar().getMaximum()));
    }

    public ExpressionParameter getExpression() {
        return expression;
    }

    public void setExpression(ExpressionParameter expression) {
        this.expression = expression;
    }
}
