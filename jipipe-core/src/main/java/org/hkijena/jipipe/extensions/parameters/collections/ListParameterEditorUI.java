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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

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
        setBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2));
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalStrut(4));
        JLabel nameLabel = new JLabel(getParameterAccess().getName());
        if(getParameterAccess().isImportant()) {
            nameLabel.setIcon(UIUtils.getIconFromResources("emblems/important.png"));
        }
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton addButton = new JButton("Add", UIUtils.getIconFromResources("actions/list-add.png"));
        addButton.addActionListener(e -> addNewEntry());
        toolBar.add(addButton);

        JButton menuButton = new JButton(UIUtils.getIconFromResources("actions/open-menu.png"));
        menuButton.setToolTipText("Show additional options");
        JPopupMenu menu = UIUtils.addPopupMenuToComponent(menuButton);
        toolBar.add(menuButton);

        JMenuItem clearItem = new JMenuItem("Clear", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearItem.setToolTipText("Removes all items");
        clearItem.addActionListener(e -> clearList());
        menu.add(clearItem);

        add(toolBar, BorderLayout.NORTH);

        formPanel = new FormPanel(null, FormPanel.NONE);
        add(formPanel, BorderLayout.CENTER);
        add(emptyLabel, BorderLayout.SOUTH);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private void clearList() {
        ListParameter<?> parameter = getParameter(ListParameter.class);
        parameter.clear();
        setParameter(parameter, true);
    }

    private void addNewEntry() {
        ListParameter<?> parameter = getParameter(ListParameter.class);
        parameter.addNewInstance();
        setParameter(parameter, true);
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public boolean isUIImportantLabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        formPanel.clear();
        ListParameter<?> parameter = getParameter(ListParameter.class);
        for (int i = 0; i < parameter.size(); ++i) {
            Object entry = parameter.get(i);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

            JButton moveUpButton = new JButton(UIUtils.getIconFromResources("actions/draw-triangle3.png"));
            moveUpButton.setToolTipText("Move entry up");
            UIUtils.makeBorderlessWithoutMargin(moveUpButton);
            moveUpButton.addActionListener(e -> moveEntryUp(entry));
            buttonPanel.add(moveUpButton);

            JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
            removeButton.setToolTipText("Remove entry");
            UIUtils.makeBorderlessWithoutMargin(removeButton);
            removeButton.addActionListener(e -> removeEntry(entry));
            buttonPanel.add(removeButton);

            JButton moveDownButton = new JButton(UIUtils.getIconFromResources("actions/draw-triangle4.png"));
            moveDownButton.setToolTipText("Move entry down");
            UIUtils.makeBorderlessWithoutMargin(moveDownButton);
            moveDownButton.addActionListener(e -> moveEntryDown(entry));
            buttonPanel.add(moveDownButton);

            ListParameterItemParameterAccess<?> access = new ListParameterItemParameterAccess(getParameterAccess(),
                    parameter,
                    parameter.getContentClass(),
                    i);
            JIPipeParameterEditorUI ui = JIPipe.getParameterTypes().createEditorFor(getWorkbench(), access);
            formPanel.addToForm(ui, buttonPanel, null);
        }
        emptyLabel.setVisible(parameter.isEmpty());
    }

    private void moveEntryDown(Object entry) {
        ListParameter<Object> parameter = getParameter(ListParameter.class);
        int i = parameter.indexOf(entry);
        if (i >= 0) {
            int j = (i + 1) % parameter.size();
            Object next = parameter.get(j);
            parameter.set(j, entry);
            parameter.set(i, next);
            setParameter(parameter, true);
        }
    }

    private void moveEntryUp(Object entry) {
        ListParameter<Object> parameter = getParameter(ListParameter.class);
        int i = parameter.indexOf(entry);
        if (i >= 0) {
            if (i == 0) {
                Object previous = parameter.get(parameter.size() - 1);
                parameter.set(parameter.size() - 1, entry);
                parameter.set(i, previous);
            } else {
                Object previous = parameter.get(i - 1);
                parameter.set(i - 1, entry);
                parameter.set(i, previous);
            }
            setParameter(parameter, true);
        }
    }

    private void removeEntry(Object entry) {
        ListParameter<?> parameter = getParameter(ListParameter.class);
        parameter.remove(entry);
        setParameter(parameter, true);
    }
}
