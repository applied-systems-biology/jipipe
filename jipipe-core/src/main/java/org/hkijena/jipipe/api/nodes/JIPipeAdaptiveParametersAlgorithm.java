package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.expressions.ui.DefaultExpressionParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.lang.annotation.Annotation;

/**
 * An algorithm that has support for adaptive parameters
 */
public interface JIPipeAdaptiveParametersAlgorithm extends JIPipeParameterCollection {
    JIPipeAdaptiveParameterSettings getAdaptiveParameterSettings();

    @Override
    default void installUIParameterOptions(ParameterPanel parameterPanel, JIPipeParameterEditorUI parameterEditorUI, JPopupMenu menu) {
        JIPipeParameterCollection.super.installUIParameterOptions(parameterPanel, parameterEditorUI, menu);

        if (getAdaptiveParameterSettings().isEnabled()) {

            menu.addSeparator();

            String key = parameterEditorUI.getParameterAccess().getKey();
            DefaultExpressionParameter adaptiveParameter = getAdaptiveParameterSettings().getAdaptiveParameter(key);

            if (adaptiveParameter != null) {
                JMenuItem removeAdaptiveParameter = new JMenuItem("Make parameter static", UIUtils.getIconFromResources("actions/lock.png"));
                removeAdaptiveParameter.addActionListener(e -> {
                    getAdaptiveParameterSettings().removeAdaptiveParameter(key);
                });
                menu.add(removeAdaptiveParameter);
            } else {
                JMenuItem addAdaptiveParameter = new JMenuItem("Make parameter adaptive", UIUtils.getIconFromResources("actions/insert-math-expression.png"));
                addAdaptiveParameter.addActionListener(e -> {
                    getAdaptiveParameterSettings().addAdaptiveParameter(key);
                });
                menu.add(addAdaptiveParameter);
            }
        }
    }

    @Override
    default JComponent installUIOverrideParameterEditor(ParameterPanel parameterPanel, JIPipeParameterEditorUI parameterEditorUI) {

        if (getAdaptiveParameterSettings().isEnabled()) {
            String key = parameterEditorUI.getParameterAccess().getKey();
            DefaultExpressionParameter adaptiveParameter = getAdaptiveParameterSettings().getAdaptiveParameter(key);

            if (adaptiveParameter != null) {
                JIPipeManualParameterAccess dummy = JIPipeManualParameterAccess.builder()
                        .setFieldClass(StringQueryExpression.class)
                        .addAnnotation(new ExpressionParameterSettingsVariable() {
                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return ExpressionParameterSettingsVariable.class;
                            }

                            @Override
                            public String name() {
                                return "";
                            }

                            @Override
                            public String description() {
                                return "";
                            }

                            @Override
                            public String key() {
                                return "";
                            }

                            @Override
                            public Class<? extends ExpressionParameterVariableSource> fromClass() {
                                return JIPipeAdaptiveParameterSettings.VariableSource.class;
                            }
                        })
                        .setKey(key).setGetter(() -> adaptiveParameter)
                        .setSetter(t -> {
                        }).setSource(new JIPipeDummyParameterCollection()).build();

                DefaultExpressionParameterEditorUI newEditorUI = new DefaultExpressionParameterEditorUI(parameterEditorUI.getWorkbench(), dummy);
                JLabel label = new JLabel("Adaptive", UIUtils.getIconFromResources("emblems/emblem-important-blue.png"), JLabel.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4),
                        new RoundedLineBorder(new Color(0xE6E6E6), 1, 3)));
                newEditorUI.add(label, BorderLayout.WEST);

                return newEditorUI;
            }
        }

        return JIPipeParameterCollection.super.installUIOverrideParameterEditor(parameterPanel, parameterEditorUI);
    }
}
