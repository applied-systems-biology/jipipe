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

package org.hkijena.jipipe.desktop.api.nodes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

public class JIPipeDesktopNodeQuickAction {

    private final String name;
    private final String description;
    private final String icon;
    private final String buttonIcon;
    private final String buttonText;
    private final BiConsumer<JIPipeGraphNode, JIPipeDesktopGraphCanvasUI> workload;

    public JIPipeDesktopNodeQuickAction(String name, String description, String icon, String buttonIcon, String buttonText, BiConsumer<JIPipeGraphNode, JIPipeDesktopGraphCanvasUI> workload) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.buttonIcon = buttonIcon;
        this.buttonText = buttonText;
        this.workload = workload;
    }

    public static List<JIPipeDesktopNodeQuickAction> getQuickActions(Object object) {
        List<JIPipeDesktopNodeQuickAction> quickActions = new ArrayList<>();

        // Also add the examples into quick actions
        if (object instanceof JIPipeAlgorithm) {
            for (JIPipeNodeExample example : JIPipe.getNodes().getNodeExamples(((JIPipeGraphNode) object).getInfo().getId())) {
                quickActions.add(new JIPipeDesktopNodeQuickAction("Load example: " + example.getNodeTemplate().getName(),
                        example.getNodeTemplate().getDescription().toPlainText(),
                        "actions/graduation-cap.png",
                        "actions/graduation-cap.png",
                        "Load example: " + example.getNodeTemplate().getName(),
                        (node, canvasUI) -> {
                            if (JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(canvasUI), "Do you really want to load the example '" +
                                            example.getNodeTemplate().getName() + "'?\n" +
                                            "This will override all your existing settings.", "Load example", JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                                return;
                            }
                            ((JIPipeAlgorithm) node).loadExample(example);
                        }));
            }
        }

        // The quick actions
        for (Method method : object.getClass().getMethods()) {
            AddJIPipeDesktopNodeQuickAction annotation = method.getAnnotation(AddJIPipeDesktopNodeQuickAction.class);
            if (annotation != null) {
                quickActions.add(new JIPipeDesktopNodeQuickAction(annotation.name(),
                        annotation.description(),
                        annotation.icon(),
                        annotation.buttonIcon(),
                        annotation.buttonText(),
                        (node, canvasUI) -> {
                            try {
                                method.invoke(node, canvasUI);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        }));
            }
        }
        quickActions.sort(Comparator.comparing(JIPipeDesktopNodeQuickAction::getName));
        return quickActions;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public String getButtonIcon() {
        return buttonIcon;
    }

    public String getButtonText() {
        return buttonText;
    }

    public BiConsumer<JIPipeGraphNode, JIPipeDesktopGraphCanvasUI> getWorkload() {
        return workload;
    }
}
