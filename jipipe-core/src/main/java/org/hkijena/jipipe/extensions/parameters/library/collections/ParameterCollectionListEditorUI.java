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

package org.hkijena.jipipe.extensions.parameters.library.collections;

import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameterSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

/**
 * Generic parameter for {@link ListParameter}
 */
public class ParameterCollectionListEditorUI extends JIPipeParameterEditorUI {
    private final JLabel emptyLabel = UIUtils.createInfoLabel("This list is empty", "Click <i>Add</i> to add a new item.");
    private final List<EntryComponents> entryComponentsList = new ArrayList<>();
    private final Set<EntryComponents> selectedEntryComponents = new HashSet<>();
    private final JToggleButton reorderModeButton = new JToggleButton("Reorder", UIUtils.getIconFromResources("actions/object-order-lower.png"));
    private FormPanel formPanel;
    private int lastClickedIndex = -1;

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public ParameterCollectionListEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
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
        if (getParameterAccess().isImportant()) {
            nameLabel.setIcon(UIUtils.getIconFromResources("emblems/important.png"));
        }
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton addButton = new JButton("Add", UIUtils.getIconFromResources("actions/list-add.png"));
        addButton.addActionListener(e -> addNewEntry());
        toolBar.add(addButton);

        JButton removeButton = new JButton("Remove", UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.addActionListener(e -> removeSelectedEntries());
        toolBar.add(removeButton);

        reorderModeButton.addActionListener(e -> reload());
        toolBar.add(reorderModeButton);

        JButton menuButton = new JButton(UIUtils.getIconFromResources("actions/open-menu.png"));
        menuButton.setToolTipText("Show additional options");
        JPopupMenu menu = UIUtils.addPopupMenuToButton(menuButton);
        toolBar.add(menuButton);

        initializeMoreMenu(menu);

        add(toolBar, BorderLayout.NORTH);

        JIPipeParameterAccess parameterAccess = getParameterAccess();
        int flags;
        ListParameterSettings settings = parameterAccess.getAnnotationOfType(ListParameterSettings.class);
        if (settings != null) {
            if (settings.withScrollBar()) {
                flags = FormPanel.WITH_SCROLLING;
                setPreferredSize(new Dimension(Short.MAX_VALUE, settings.scrollableHeight()));
                setMinimumSize(new Dimension(0, settings.scrollableHeight()));
                setMaximumSize(new Dimension(Short.MAX_VALUE, settings.scrollableHeight()));
            } else {
                flags = FormPanel.NONE;
            }
        } else {
            flags = FormPanel.NONE;
        }

        formPanel = new FormPanel(null, flags);
        add(formPanel, BorderLayout.CENTER);
        add(emptyLabel, BorderLayout.SOUTH);

    }

    private void initializeMoreMenu(JPopupMenu menu) {

        JMenuItem selectAllItem = new JMenuItem("Select all", UIUtils.getIconFromResources("actions/edit-select-all.png"));
        selectAllItem.addActionListener(e -> selectAll());
        menu.add(selectAllItem);

        JMenuItem selectNoneItem = new JMenuItem("Clear selection", UIUtils.getIconFromResources("actions/edit-select-none.png"));
        selectNoneItem.addActionListener(e -> selectNone());
        menu.add(selectNoneItem);

        JMenuItem invertSelectionItem = new JMenuItem("Invert selection", UIUtils.getIconFromResources("actions/edit-select-invert.png"));
        invertSelectionItem.addActionListener(e -> invertSelection());
        menu.add(invertSelectionItem);

        menu.addSeparator();

        JMenuItem clearItem = new JMenuItem("Clear", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearItem.setToolTipText("Removes all items");
        clearItem.addActionListener(e -> clearList());
        menu.add(clearItem);
    }

    private void invertSelection() {
        Set<EntryComponents> toSelect = new HashSet<>();
        for (EntryComponents entryComponents : entryComponentsList) {
            if (!selectedEntryComponents.contains(entryComponents)) {
                toSelect.add(entryComponents);
            }
        }
        selectedEntryComponents.clear();
        selectedEntryComponents.addAll(toSelect);
        updateSelectionVisualizations();
    }

    @Override
    public int getUIControlStyleType() {
        return CONTROL_STYLE_LIST;
    }

    private void selectNone() {
        selectedEntryComponents.clear();
        lastClickedIndex = -1;
        updateSelectionVisualizations();
    }

    private void selectAll() {
        selectedEntryComponents.addAll(entryComponentsList);
        lastClickedIndex = -1;
        updateSelectionVisualizations();
    }

    private void removeSelectedEntries() {
        ListParameter<?> parameter = getParameter(ListParameter.class);
        for (int i = entryComponentsList.size() - 1; i >= 0; i--) {
            if (selectedEntryComponents.contains(entryComponentsList.get(i))) {
                parameter.remove(i);
            }
        }
        setParameter(parameter, true);
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
        entryComponentsList.clear();
        selectedEntryComponents.clear();
        lastClickedIndex = -1;
        ParameterCollectionList parameter = getParameter(ParameterCollectionList.class);

        // Workaround for ParameterCollectionList template issues
        // Recreates the template on creating the UI to enforce the class-based
        ParameterCollectionListTemplate annotation = getParameterAccess().getAnnotationOfType(ParameterCollectionListTemplate.class);
        if(annotation != null) {
            JIPipeDynamicParameterCollection template = parameter.getTemplate();
            template.clear();
            JIPipeParameterCollection templateCollection = (JIPipeParameterCollection) ReflectionUtils.newInstance(annotation.value());
            JIPipeParameterTree tree = new JIPipeParameterTree(templateCollection);
            for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                JIPipeMutableParameterAccess parameterAccess = new JIPipeMutableParameterAccess(entry.getValue());
                template.addParameter(parameterAccess);
            }

            // Apply the template to the items
            parameter.applyTemplateToItems();
        }

        // Generate entries
        for (int i = 0; i < parameter.size(); ++i) {
            Object entry = parameter.get(i);

            JPanel buttonPanel = new JPanel(new GridBagLayout());
            buttonPanel.setOpaque(false);
            EntryComponents entryComponents = new EntryComponents();

            if (reorderModeButton.isSelected()) {
                JButton moveUpButton = new JButton(UIUtils.getIconFromResources("actions/sort-up.png"));
                moveUpButton.setToolTipText("Move entry up");
                UIUtils.makeFlat25x25(moveUpButton);
                moveUpButton.addActionListener(e -> moveEntryUp(entry));
                buttonPanel.add(moveUpButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

//                JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
//                removeButton.setToolTipText("Remove entry");
//                UIUtils.makeBorderlessWithoutMargin(removeButton);
//                removeButton.addActionListener(e -> removeEntry(entry));
//                buttonPanel.add(removeButton, BorderLayout.CENTER);

                JButton moveDownButton = new JButton(UIUtils.getIconFromResources("actions/sort-down.png"));
                moveDownButton.setToolTipText("Move entry down");
                UIUtils.makeFlat25x25(moveDownButton);
                moveDownButton.addActionListener(e -> moveEntryDown(entry));
                buttonPanel.add(moveDownButton, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            }

            JButton handleButton = new JButton(UIUtils.getIconInvertedFromResources("actions/grip-lines.png"));
            handleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            handleButton.setOpaque(false);
            UIUtils.makeFlat25x25(handleButton);
            handleButton.setToolTipText("Select/Deselect this entry");
            int finalI = i;
            handleButton.addActionListener(e -> {
                handleSelection(finalI, (e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK, (e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
            });
            buttonPanel.add(handleButton, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

            JPopupMenu handleMenu = UIUtils.addRightClickPopupMenuToButton(handleButton);
            handleMenu.add(UIUtils.createMenuItem("Delete", "Removes this item", UIUtils.getIconFromResources("actions/delete.png"), () -> {
                selectedEntryComponents.clear();
                selectedEntryComponents.add(entryComponentsList.get(finalI));
                removeSelectedEntries();
            }));
            handleMenu.addSeparator();
            handleMenu.add(UIUtils.createMenuItem("Move up", "Moves this item one position up", UIUtils.getIconFromResources("actions/sort-up.png"), () -> {
                moveEntryUp(entry);
            }));
            handleMenu.add(UIUtils.createMenuItem("Move down", "Moves this item one position down", UIUtils.getIconFromResources("actions/sort-down.png"), () -> {
                moveEntryUp(entry);
            }));

            ParameterPanel ui = new ParameterPanel(getWorkbench(), parameter.get(i), getParameterTree(), null, ParameterPanel.NO_EMPTY_GROUP_HEADERS);
            ui.setBorder(UIUtils.createControlBorder());
            ui.setOpaque(false);

            JPanel entryPanel = new JPanel(new GridBagLayout());
            entryPanel.setOpaque(true);
            entryPanel.add(buttonPanel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
            entryPanel.add(ui, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));

            formPanel.addWideToForm(entryPanel);

            entryComponents.entryPanel = entryPanel;
            entryComponentsList.add(entryComponents);
        }
        formPanel.addVerticalGlue();
        emptyLabel.setVisible(parameter.isEmpty());
    }

    private void handleSelection(int entryIndex, boolean shiftPressed, boolean ctrlPressed) {
        EntryComponents entryComponents = entryComponentsList.get(entryIndex);
        if (ctrlPressed || (shiftPressed && lastClickedIndex < 0)) {
            if (selectedEntryComponents.contains(entryComponents)) {
                selectedEntryComponents.remove(entryComponents);
            } else {
                selectedEntryComponents.add(entryComponents);
            }
        } else if (shiftPressed) {
            int i0;
            int i1;
            if (lastClickedIndex < entryIndex) {
                i0 = lastClickedIndex;
                i1 = entryIndex;
            } else {
                i0 = entryIndex;
                i1 = lastClickedIndex;
            }
            for (int i = i0; i <= i1; ++i) {
                selectedEntryComponents.add(entryComponentsList.get(i));
            }
        } else {
            selectedEntryComponents.clear();
            selectedEntryComponents.add(entryComponents);
        }
        lastClickedIndex = entryIndex;
        updateSelectionVisualizations();
    }

    private void updateSelectionVisualizations() {
        for (EntryComponents entryComponents : entryComponentsList) {
            Color background;
            if (selectedEntryComponents.contains(entryComponents)) {
                background = UIManager.getColor("List.selectionBackground");
            } else {
                background = UIManager.getColor("Panel.background");
            }
            entryComponents.entryPanel.setBackground(background);
        }
        repaint();
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

//    private void removeEntry(Object entry) {
//        ListParameter<?> parameter = getParameter(ListParameter.class);
//        parameter.remove(entry);
//        setParameter(parameter, true);
//    }

    private static class EntryComponents {
        private JPanel entryPanel;
    }
}
