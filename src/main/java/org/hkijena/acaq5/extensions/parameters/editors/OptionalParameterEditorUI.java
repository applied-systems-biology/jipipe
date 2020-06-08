package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.OptionalParameter;
import org.hkijena.acaq5.extensions.parameters.OptionalParameterContentAccess;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * Generic parameter for {@link org.hkijena.acaq5.extensions.parameters.OptionalParameter}
 */
public class OptionalParameterEditorUI extends ACAQParameterEditorUI {
    /**
     * Creates new instance
     *
     * @param context         SciJava context
     * @param parameterAccess Parameter
     */
    public OptionalParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
    }

    private OptionalParameter<?> getParameter() {
        return getParameterAccess().get(OptionalParameter.class);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        OptionalParameter<?> parameter = getParameter();
        removeAll();

        // Create toggle button
        JToggleButton toggle = new JToggleButton("Change", UIUtils.getIconFromResources("eye.png"));
        UIUtils.makeFlat(toggle);
        toggle.setToolTipText("If enabled, the parameter is not ignored.");
        toggle.setSelected(parameter.isEnabled());
        toggle.setIcon(toggle.isSelected() ? UIUtils.getIconFromResources("eye.png") :
                UIUtils.getIconFromResources("eye-slash.png"));
        toggle.addActionListener(e -> {
            parameter.setEnabled(toggle.isSelected());
            reload();
        });
        add(toggle, BorderLayout.WEST);

        OptionalParameterContentAccess<?> access = new OptionalParameterContentAccess(getParameterAccess(), parameter);
        ACAQParameterEditorUI ui = ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getContext(), access);
        add(ui, BorderLayout.CENTER);

        revalidate();
        repaint();

    }
}
