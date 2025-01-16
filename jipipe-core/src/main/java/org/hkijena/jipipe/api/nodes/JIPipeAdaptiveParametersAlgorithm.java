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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.expressions.ui.JIPipeExpressionDesktopParameterEditorUI;
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
    default void installUIParameterOptions(JIPipeDesktopParameterFormPanel parameterPanel, JIPipeDesktopParameterEditorUI parameterEditorUI, JPopupMenu menu) {
        JIPipeParameterCollection.super.installUIParameterOptions(parameterPanel, parameterEditorUI, menu);

        if (getAdaptiveParameterSettings().isEnabled()) {

            menu.addSeparator();

            String key = parameterEditorUI.getParameterAccess().getKey();
            JIPipeExpressionParameter adaptiveParameter = getAdaptiveParameterSettings().getAdaptiveParameter(key);

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
    default JComponent installUIOverrideParameterEditor(JIPipeDesktopParameterFormPanel parameterPanel, JIPipeDesktopParameterEditorUI parameterEditorUI) {

        if (getAdaptiveParameterSettings().isEnabled()) {
            String key = parameterEditorUI.getParameterAccess().getKey();
            JIPipeExpressionParameter adaptiveParameter = getAdaptiveParameterSettings().getAdaptiveParameter(key);

            if (adaptiveParameter != null) {
                JIPipeManualParameterAccess dummy = JIPipeManualParameterAccess.builder()
                        .setFieldClass(StringQueryExpression.class)
                        .addAnnotation(new AddJIPipeExpressionParameterVariable() {
                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return AddJIPipeExpressionParameterVariable.class;
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
                            public Class<? extends JIPipeExpressionVariablesInfo> fromClass() {
                                return JIPipeAdaptiveParameterSettings.VariablesInfo.class;
                            }
                        })
                        .setKey(key).setGetter(() -> adaptiveParameter)
                        .setSetter(t -> {
                        }).setSource(new JIPipeDummyParameterCollection()).build();

                JIPipeExpressionDesktopParameterEditorUI newEditorUI = new JIPipeExpressionDesktopParameterEditorUI(
                        new JIPipeDesktopParameterEditorUI.InitializationParameters(parameterEditorUI.getDesktopWorkbench(), parameterPanel.getParameterTree(), dummy));
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
