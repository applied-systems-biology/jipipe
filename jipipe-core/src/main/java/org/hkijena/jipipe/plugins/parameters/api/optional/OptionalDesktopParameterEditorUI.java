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

package org.hkijena.jipipe.plugins.parameters.api.optional;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;

/**
 * Generic parameter for {@link OptionalParameter}
 */
public class OptionalDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {
    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public OptionalDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        OptionalParameter<?> parameter = getParameter(OptionalParameter.class);
        removeAll();

        // Create toggle button
        JButton toggle = new JButton("Enabled", UIUtils.getIconFromResources("emblems/checkbox-checked.png"));
        toggle.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 16));
        toggle.setToolTipText("If enabled, the parameter is not ignored.");
        boolean selected = parameter.isEnabled();
//        toggle.setSelected(parameter.isEnabled());
        toggle.setIcon(selected ? UIUtils.getIconFromResources("emblems/checkbox-checked.png") :
                UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"));
        toggle.addActionListener(e -> {
            parameter.setEnabled(!selected);
            setParameter(parameter, true);
        });
        if (selected) {
            setBorder(new RoundedLineBorder(new Color(0x5CB85C), 1, 3));
        } else {
            setBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 3));
        }
        add(toggle, BorderLayout.WEST);

        OptionalParameterContentAccess<?> access = new OptionalParameterContentAccess(getParameterAccess(), parameter);
        JIPipeDesktopParameterEditorUI ui = JIPipe.getParameterTypes().createEditorFor(getDesktopWorkbench(), getParameterTree(), access);
        add(ui, BorderLayout.CENTER);

        // Listen for changes inside the parameter content
        access.getParameterChangedEventEmitter().subscribeLambda((emitter, event) -> {
            // We trigger the change event
            setParameter(parameter, false);
        });

        revalidate();
        repaint();

    }
}
