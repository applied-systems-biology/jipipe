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

package org.hkijena.jipipe.extensions.parameters.generators;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Editor for {@link IntegerRange}
 */
public class IntegerRangeParameterEditorUI extends JIPipeParameterEditorUI {

    private JToggleButton expressionModeToggle;
    private JTextField rangeStringEditor;
    private boolean isUpdating = false;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public IntegerRangeParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        expressionModeToggle = new JToggleButton(UIUtils.getIconFromResources("actions/insert-math-expression.png"));
        expressionModeToggle.setToolTipText("If enabled, use a math expression instead of a range string.");
        expressionModeToggle.addActionListener(e -> {
            IntegerRange rangeString = getParameter(IntegerRange.class);
            rangeString.setUseExpression(expressionModeToggle.isSelected());
            setParameter(rangeString, true);
        });
        UIUtils.makeFlat25x25(expressionModeToggle);
        expressionModeToggle.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 0, UIManager.getColor("Button.borderColor")));

        rangeStringEditor = new JTextField();
        rangeStringEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rangeStringEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if(!isUpdating) {
                    IntegerRange rangeString = getParameter(IntegerRange.class);
                    if (!rangeString.isUseExpression()) {
                        rangeString.setValue(rangeStringEditor.getText());
                        checkParameter();
                        setParameter(rangeString, false);
                    }
                }
            }
        });
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        try {
            isUpdating = true;
            removeAll();
            IntegerRange rangeString = getParameter(IntegerRange.class);

            add(expressionModeToggle, BorderLayout.WEST);
            expressionModeToggle.setSelected(rangeString.isUseExpression());
            if (rangeString.isUseExpression()) {
                JIPipeManualParameterAccess access = JIPipeManualParameterAccess.builder()
                        .setSource(new JIPipeDummyParameterCollection())
                        .setFieldClass(DefaultExpressionParameter.class)
                        .setGetter(rangeString::getExpression)
                        .addAnnotation(new IntegerRangeExpressionVariablesAnnotationImpl())
                        .setSetter(expression -> {
                            rangeString.setExpression((DefaultExpressionParameter) expression);
                            setParameter(rangeString, false);
                            checkParameter();
                        }).build();
                add(JIPipe.getParameterTypes().createEditorFor(getWorkbench(), access), BorderLayout.CENTER);
            } else {
                rangeStringEditor.setText(rangeString.getValue());
                add(rangeStringEditor, BorderLayout.CENTER);
            }

            revalidate();
            repaint();
            checkParameter();
        }
        finally {
            isUpdating = false;
        }
    }

    private void checkParameter() {
        IntegerRange rangeString = getParameter(IntegerRange.class);
        try {
            if(!rangeString.isUseExpression()) {
                rangeString.getIntegers(0, 0);
            }
            rangeStringEditor.setBorder(BorderFactory.createEtchedBorder());
            rangeStringEditor.setToolTipText("Valid!");
        } catch (Exception e) {
            rangeStringEditor.setBorder(BorderFactory.createLineBorder(Color.RED));
            rangeStringEditor.setToolTipText("Invalid: " + e.getMessage());
        }
    }

    private static class IntegerRangeExpressionVariablesAnnotationImpl implements ExpressionParameterSettings {
        @Override
        public Class<? extends ExpressionParameterVariableSource> variableSource() {
            return IntegerRange.VariableSource.class;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return ExpressionParameterSettings.class;
        }
    }
}
