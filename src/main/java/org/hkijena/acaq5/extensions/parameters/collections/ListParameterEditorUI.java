package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * Generic parameter for {@link ListParameter}
 */
public class ListParameterEditorUI extends ACAQParameterEditorUI {
    private FormPanel formPanel;

    /**
     * Creates new instance
     *
     * @param context         SciJava context
     * @param parameterAccess Parameter
     */
    public ListParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(new JLabel(getParameterAccess().getName()));

        toolBar.add(Box.createHorizontalGlue());

        JButton addButton = new JButton("Add", UIUtils.getIconFromResources("add.png"));
        addButton.addActionListener(e -> addNewEntry());
        toolBar.add(addButton);
        add(toolBar, BorderLayout.NORTH);

        formPanel = new FormPanel(null, FormPanel.NONE);
        add(formPanel, BorderLayout.CENTER);
    }

    private void addNewEntry() {
        getParameter().addNewInstance();
        reload();
    }

    private ListParameter<?> getParameter() {
        return getParameterAccess().get(ListParameter.class);
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        formPanel.clear();
        for (int i = 0; i < getParameter().size(); ++i) {
            Object entry = getParameter().get(i);
            JButton removeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
            removeButton.setToolTipText("Remove entry");
            UIUtils.makeBorderlessWithoutMargin(removeButton);
            removeButton.addActionListener(e -> removeEntry(entry));

            ListParameterItemParameterAccess<?> access = new ListParameterItemParameterAccess(getParameterAccess(),
                    getParameter(),
                    getParameter().getContentClass(),
                    i);
            ACAQParameterEditorUI ui = ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getContext(), access);
            formPanel.addToForm(ui, removeButton, null);
        }
    }

    private void removeEntry(Object entry) {
        getParameter().remove(entry);
        reload();
    }
}
