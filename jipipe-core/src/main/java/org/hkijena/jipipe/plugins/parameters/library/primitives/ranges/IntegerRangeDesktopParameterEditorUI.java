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

package org.hkijena.jipipe.plugins.parameters.library.primitives.ranges;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.plugins.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.lang.annotation.Annotation;

/**
 * Editor for {@link IntegerRange}
 */
public class IntegerRangeDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private JToggleButton expressionModeToggle;
    private JTextField rangeStringEditor;
    private boolean isUpdating = false;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public IntegerRangeDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
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
//        UIUtils.makeFlat25x25(expressionModeToggle);
//        expressionModeToggle.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 0, UIManager.getColor("Button.borderColor")));

        rangeStringEditor = new JTextField();
        rangeStringEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rangeStringEditor.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!isUpdating) {
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
                        .setFieldClass(JIPipeExpressionParameter.class)
                        .setGetter(rangeString::getExpression)
                        .addAnnotation(new IntegerRangeExpressionVariablesAnnotationImpl())
                        .setSetter(expression -> {
                            rangeString.setExpression((JIPipeExpressionParameter) expression);
                            setParameter(rangeString, false);
                            checkParameter();
                        }).build();
                add(JIPipe.getParameterTypes().createEditorFor(getDesktopWorkbench(), new JIPipeParameterTree(access), access), BorderLayout.CENTER);
            } else {
                rangeStringEditor.setText(rangeString.getValue());
                add(rangeStringEditor, BorderLayout.CENTER);
            }

            revalidate();
            repaint();
            checkParameter();
        } finally {
            isUpdating = false;
        }
    }

    private void checkParameter() {
        IntegerRange rangeString = getParameter(IntegerRange.class);
        try {
            if (!rangeString.isUseExpression()) {
                rangeString.getIntegers(0, 0, new JIPipeExpressionVariablesMap());
            }
            rangeStringEditor.setBorder(UIUtils.createControlBorder());
            rangeStringEditor.setToolTipText("Valid!");
        } catch (Exception e) {
            rangeStringEditor.setBorder(UIUtils.createControlErrorBorder());
            rangeStringEditor.setToolTipText("Invalid: " + e.getMessage());
        }
    }

    private static class IntegerRangeExpressionVariablesAnnotationImpl implements JIPipeExpressionParameterSettings {
        @Override
        public Class<? extends ExpressionParameterVariablesInfo> variableSource() {
            return IntegerRange.VariablesInfo.class;
        }

        @Override
        public String hint() {
            return "";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return JIPipeExpressionParameterSettings.class;
        }
    }
}
