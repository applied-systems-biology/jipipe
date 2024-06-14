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

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JIPipeDesktopNodeQuickAction {

    private final String name;
    private final String description;
    private final String icon;
    private final String buttonIcon;
    private final String buttonText;
    private final Method method;

    public JIPipeDesktopNodeQuickAction(String name, String description, String icon, String buttonIcon, String buttonText, Method method) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.buttonIcon = buttonIcon;
        this.buttonText = buttonText;
        this.method = method;
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

    public Method getMethod() {
        return method;
    }

    public static List<JIPipeDesktopNodeQuickAction> getQuickActions(Object object) {
        List<JIPipeDesktopNodeQuickAction> quickActions = new ArrayList<>();
        for (Method method : object.getClass().getMethods()) {
            AddJIPipeDesktopNodeQuickAction annotation = method.getAnnotation(AddJIPipeDesktopNodeQuickAction.class);
            if(annotation != null) {
               quickActions.add(new JIPipeDesktopNodeQuickAction(annotation.name(),
                       annotation.description(),
                       annotation.icon(),
                       annotation.buttonIcon(),
                       annotation.buttonText(),
                       method));
            }
        }
        quickActions.sort(Comparator.comparing(JIPipeDesktopNodeQuickAction::getName));
        return quickActions;
    }

    public void run(JIPipeGraphNode node, JIPipeDesktopGraphCanvasUI canvasUI) {
        try {
            getMethod().invoke(node, canvasUI);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
