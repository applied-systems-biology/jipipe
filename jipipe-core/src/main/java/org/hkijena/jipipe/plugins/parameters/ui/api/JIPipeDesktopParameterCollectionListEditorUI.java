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

package org.hkijena.jipipe.plugins.parameters.ui.api;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameterSettings;
import org.hkijena.jipipe.plugins.parameters.api.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Generic parameter for {@link JIPipeParameterCollectionList}
 * (#1339: compact footer + grip drag-reorder, identical chrome to
 * {@link JIPipeDesktopListParameterEditorUI}).
 */
public class JIPipeDesktopParameterCollectionListEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeParameterCollectionList> {
    /** Custom drag flavor so only grips drag onto list rows (in-JVM identity). */
    private static final DataFlavor GRIP_DRAG_FLAVOR =
            new DataFlavor(String.class, "JIPipeListGripDrag");

    private final JLabel emptyLabel = UIUtils.createInfoLabel("This list is empty", "Click <i>Add</i> to add a new item.");
    private final JLabel countLabel = new JLabel("");
    private final List<EntryComponents> entryComponentsList = new ArrayList<>();
    private final Set<EntryComponents> selectedEntryComponents = new HashSet<>();
    private JIPipeDesktopFormPanel formPanel;
    private int lastClickedIndex = -1;

    public JIPipeDesktopParameterCollectionListEditorUI(InitializationParameters parameters) {
        super(JIPipeParameterCollectionList.class, parameters);
        initialize();
        reload();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                reload();
                super.componentShown(e);
            }
        });
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2));

        JIPipeParameterAccess parameterAccess = getParameterAccess();
        int flags;
        ListParameterSettings settings = parameterAccess.getAnnotationOfType(ListParameterSettings.class);
        if (settings != null) {
            if (settings.withScrollBar()) {
                flags = JIPipeDesktopFormPanel.WITH_SCROLLING;
                setPreferredSize(new Dimension(Short.MAX_VALUE, settings.scrollableHeight()));
                setMinimumSize(new Dimension(0, settings.scrollableHeight()));
                setMaximumSize(new Dimension(Short.MAX_VALUE, settings.scrollableHeight()));
            } else {
                flags = JIPipeDesktopFormPanel.NONE;
            }
        } else {
            flags = JIPipeDesktopFormPanel.NONE;
        }

        formPanel = new JIPipeDesktopFormPanel(null, flags);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(formPanel, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        center.add(emptyLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        add(center, BorderLayout.CENTER);

        // #1339: compact footer instead of the always-visible toolbar
        JPanel footer = new JPanel(new BorderLayout(4, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));

        JButton addButton = new JButton("Add", JIPipe.RESOURCES.getIcon16("actions/list-add.png"));
        addButton.setOpaque(false);
        addButton.setContentAreaFilled(false);
        addButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        addButton.setFocusable(false);
        addButton.addActionListener(e -> addNewEntry());
        footer.add(addButton, BorderLayout.WEST);

        countLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        footer.add(countLabel, BorderLayout.CENTER);

        JButton menuButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/hamburger-menu.png"));
        menuButton.setOpaque(false);
        menuButton.setContentAreaFilled(false);
        menuButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        menuButton.setFocusable(false);
        menuButton.setToolTipText("List options");
        JPopupMenu menu = UIUtils.addPopupMenuToButton(menuButton);
        initializeMoreMenu(menu);
        footer.add(menuButton, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);

    }

    private void initializeMoreMenu(JPopupMenu menu) {

        JMenuItem selectAllItem = new JMenuItem("Select all", JIPipe.RESOURCES.getIcon16("actions/edit-select-all.png"));
        selectAllItem.addActionListener(e -> selectAll());
        menu.add(selectAllItem);

        JMenuItem selectNoneItem = new JMenuItem("Clear selection", JIPipe.RESOURCES.getIcon16("actions/edit-select-none.png"));
        selectNoneItem.addActionListener(e -> selectNone());
        menu.add(selectNoneItem);

        JMenuItem invertSelectionItem = new JMenuItem("Invert selection", JIPipe.RESOURCES.getIcon16("actions/edit-select-invert.png"));
        invertSelectionItem.addActionListener(e -> invertSelection());
        menu.add(invertSelectionItem);

        menu.addSeparator();

        JMenuItem clearItem = new JMenuItem("Clear list", JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"));
        clearItem.setToolTipText("Removes all items");
        clearItem.addActionListener(e -> clearList());
        menu.add(clearItem);
    }

    private void updateCountLabel() {
        int n = entryComponentsList.size();
        countLabel.setText(n + " " + (n == 1 ? "item" : "items"));
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
        JIPipeListParameter<?> parameter = getParameter();
        for (int i = entryComponentsList.size() - 1; i >= 0; i--) {
            if (selectedEntryComponents.contains(entryComponentsList.get(i))) {
                parameter.remove(i);
            }
        }
        setParameter(parameter, true);
    }

    private void clearList() {
        JIPipeListParameter<?> parameter = getParameter();
        parameter.clear();
        setParameter(parameter, true);
    }

    private void addNewEntry() {
        JIPipeListParameter<?> parameter = getParameter();
        parameter.addNewInstance();
        setParameter(parameter, true);
    }

    @Override
    public boolean isUIInline() {
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
        JIPipeParameterCollectionList parameter = getParameter();

        // Workaround for ParameterCollectionList template issues
        // Recreates the template on creating the UI to enforce the class-based
        ParameterCollectionListTemplate annotation = getParameterAccess().getAnnotationOfType(ParameterCollectionListTemplate.class);
        if (annotation != null) {
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
            int finalI = i;

            JButton handleButton = new JButton(JIPipe.RESOURCES.getIcon16Inverted("actions/grip-lines.png"));
            handleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            handleButton.setOpaque(false);
            UIUtils.makeButtonFlat25x25(handleButton);
            handleButton.setToolTipText("Select this entry (Ctrl: toggle, Shift: range). Drag to reorder.");
            handleButton.addActionListener(e -> {
                handleSelection(finalI, (e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK, (e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
            });
            buttonPanel.add(handleButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NORTH, new Insets(0, 0, 0, 0), 0, 0));

            JPopupMenu handleMenu = UIUtils.addRightClickPopupMenuToButton(handleButton);
            handleMenu.add(UIUtils.createMenuItem("Delete", "Removes this item", JIPipe.RESOURCES.getIcon16("actions/delete.png"), () -> {
                selectedEntryComponents.clear();
                selectedEntryComponents.add(entryComponentsList.get(finalI));
                removeSelectedEntries();
            }));
            handleMenu.addSeparator();
            handleMenu.add(UIUtils.createMenuItem("Move up", "Moves this item one position up", JIPipe.RESOURCES.getIcon16("actions/sort-up.png"), () -> {
                moveEntryUp(entry);
            }));
            handleMenu.add(UIUtils.createMenuItem("Move down", "Moves this item one position down", JIPipe.RESOURCES.getIcon16("actions/sort-down.png"), () -> {
                moveEntryDown(entry);
            }));

            // #1339: per-row delete on hover
            JButton deleteButton = new JButton(JIPipe.RESOURCES.getIcon16Inverted("actions/close-tab.png"));
            deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            deleteButton.setOpaque(false);
            deleteButton.setFocusable(false);
            UIUtils.makeButtonFlat25x25(deleteButton);
            deleteButton.setToolTipText("Remove this entry");
            deleteButton.setVisible(false);
            deleteButton.addActionListener(e -> {
                selectedEntryComponents.clear();
                selectedEntryComponents.add(entryComponents);
                removeSelectedEntries();
            });
            buttonPanel.add(deleteButton, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NORTH, new Insets(0, 0, 0, 0), 0, 0));

            JIPipeDesktopParameterFormPanel ui;

            if (getContextParent() instanceof JIPipeDesktopParameterFormPanel && ((JIPipeDesktopParameterFormPanel) getContextParent()).isWithExternalDocumentation()) {
                ui = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), parameter.get(i), getParameterTree(), null, JIPipeDesktopParameterFormPanel.NO_EMPTY_GROUP_HEADERS | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.DOCUMENTATION_EXTERNAL);
                ui.setRedirectDocumentationTarget((JIPipeDesktopFormPanel) getContextParent());
            } else {
                ui = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), parameter.get(i), getParameterTree(), null, JIPipeDesktopParameterFormPanel.NO_EMPTY_GROUP_HEADERS);
            }

            ui.setBorder(UIUtils.createControlBorder());
            ui.setOpaque(false);

            JPanel entryPanel = new JPanel(new GridBagLayout());
            entryPanel.setOpaque(true);
            entryPanel.add(buttonPanel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
            entryPanel.add(ui, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));

            // #1339: drag-reorder via the grip + per-row delete on hover
            entryPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    deleteButton.setVisible(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    deleteButton.setVisible(false);
                    entryPanel.revalidate();
                    entryPanel.repaint();
                }
            });
            GripDragTransferHandler dragHandler = new GripDragTransferHandler(entryComponents);
            handleButton.setTransferHandler(dragHandler);
            // The whole row accepts the drop (child components keep their own
            // handlers, so text fields etc. are unaffected).
            entryPanel.setTransferHandler(dragHandler);

            formPanel.addWideToForm(entryPanel);

            entryComponents.entryPanel = entryPanel;
            entryComponentsList.add(entryComponents);
        }
        formPanel.addVerticalGlue();
        emptyLabel.setVisible(parameter.isEmpty());
        updateCountLabel();
    }

    /**
     * Drags a list row onto another row to reorder it (identical to
     * {@link JIPipeDesktopListParameterEditorUI}): the handler is attached to
     * the grip button; before/after is decided by the drop y-position within
     * the target row.
     */
    private class GripDragTransferHandler extends TransferHandler {
        private final EntryComponents target;

        GripDragTransferHandler(EntryComponents target) {
            this.target = target;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(GRIP_DRAG_FLAVOR);
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new GripTransferable(target);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            EntryComponents source;
            try {
                source = (EntryComponents) support.getTransferable().getTransferData(GRIP_DRAG_FLAVOR);
            } catch (Exception ex) {
                return false;
            }
            if (source == null || source == target) {
                return false;
            }
            int sourceIndex = indexByEntry(source);
            int targetIndex = indexByEntry(target);
            if (sourceIndex < 0 || targetIndex < 0) {
                return false;
            }
            Point inTargetRow = SwingUtilities.convertPoint(support.getComponent(), support.getDropLocation().getDropPoint(), target.entryPanel);
            boolean before = inTargetRow.y < target.entryPanel.getHeight() / 2;
            moveEntryTo(sourceIndex, before ? targetIndex : targetIndex + 1);
            return true;
        }
    }

    /** Carries the dragged row's identity over the custom flavor (in-JVM). */
    private static class GripTransferable implements Transferable {
        private final EntryComponents entry;

        GripTransferable(EntryComponents entry) {
            this.entry = entry;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{GRIP_DRAG_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return GRIP_DRAG_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!GRIP_DRAG_FLAVOR.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return entry;
        }
    }

    private int indexByEntry(EntryComponents entryComponents) {
        return entryComponentsList.indexOf(entryComponents);
    }

    private void moveEntryTo(int from, int to) {
        if (JIPipeDesktopListParameterEditorUI.moveItem(getParameter(), from, to)) {
            setParameter(getParameter(), true);
        }
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
        JIPipeListParameter parameter = getParameter();
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
        JIPipeListParameter parameter = getParameter();
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
