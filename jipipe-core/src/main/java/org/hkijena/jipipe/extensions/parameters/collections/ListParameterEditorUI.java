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

package org.hkijena.jipipe.extensions.parameters.collections;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIParameterTypeRegistry;
import org.hkijena.jipipe.utils.ModernMetalTheme;
import org.hkijena.jipipe.utils.RoundedLineBorder;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * Generic parameter for {@link ListParameter}
 */
public class ListParameterEditorUI extends JIPipeParameterEditorUI {
    private final JLabel emptyLabel = new JLabel("<html><strong>This list is empty</strong><br/>Click 'Add' to add items.</html>",
            UIUtils.getIcon32FromResources("info.png"), JLabel.LEFT);
    private FormPanel formPanel;

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public ListParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(new RoundedLineBorder(ModernMetalTheme.MEDIUM_GRAY, 1, 2));
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalStrut(4));
        toolBar.add(new JLabel(getParameterAccess().getName()));

        toolBar.add(Box.createHorizontalGlue());

        JButton addButton = new JButton("Add", UIUtils.getIconFromResources("actions/list-add.png"));
        addButton.addActionListener(e -> addNewEntry());
        toolBar.add(addButton);
        add(toolBar, BorderLayout.NORTH);

        formPanel = new FormPanel(null, FormPanel.NONE);
        add(formPanel, BorderLayout.CENTER);
        add(emptyLabel, BorderLayout.SOUTH);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private void addNewEntry() {
        getParameter(ListParameter.class).addNewInstance();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        formPanel.clear();
        ListParameter<?> parameter = getParameter(ListParameter.class);
        for (int i = 0; i < parameter.size(); ++i) {
            Object entry = parameter.get(i);
            JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
            removeButton.setToolTipText("Remove entry");
            UIUtils.makeBorderlessWithoutMargin(removeButton);
            removeButton.addActionListener(e -> removeEntry(entry));

            ListParameterItemParameterAccess<?> access = new ListParameterItemParameterAccess(getParameterAccess(),
                    parameter,
                    parameter.getContentClass(),
                    i);
            JIPipeParameterEditorUI ui = JIPipeUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), access);
            formPanel.addToForm(ui, removeButton, null);
        }
        emptyLabel.setVisible(parameter.isEmpty());
    }

    private void removeEntry(Object entry) {
        ListParameter<?> parameter = getParameter(ListParameter.class);
        parameter.remove(entry);
        setParameter(parameter, true);
    }
}
