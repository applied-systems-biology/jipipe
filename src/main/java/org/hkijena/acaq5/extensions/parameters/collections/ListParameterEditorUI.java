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

package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * Generic parameter for {@link ListParameter}
 */
public class ListParameterEditorUI extends ACAQParameterEditorUI {
    private FormPanel formPanel;

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public ListParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
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
            JButton removeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
            removeButton.setToolTipText("Remove entry");
            UIUtils.makeBorderlessWithoutMargin(removeButton);
            removeButton.addActionListener(e -> removeEntry(entry));

            ListParameterItemParameterAccess<?> access = new ListParameterItemParameterAccess(getParameterAccess(),
                    parameter,
                    parameter.getContentClass(),
                    i);
            ACAQParameterEditorUI ui = ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), access);
            formPanel.addToForm(ui, removeButton, null);
        }
    }

    private void removeEntry(Object entry) {
        ListParameter<?> parameter = getParameter(ListParameter.class);
        parameter.remove(entry);
        reload();
    }
}
