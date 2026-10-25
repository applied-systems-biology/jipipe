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
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameterItemParameterAccess;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameterSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared base for the inline list parameter editors (#1339, pass 2).
 * <p>
 * Holds the stateless, parameter-level chrome that was duplicated between
 * {@link JIPipeDesktopListParameterEditorUI} and
 * {@link JIPipeDesktopParameterCollectionListEditorUI}: the stacked layout,
 * the name-row header controls (Add, count, ⋯ = Clear list only), the
 * paginated row list with grip drag-reorder and per-row menu, and the
 * stateless list operations. The concrete editors only supply the per-item
 * content ({@link #buildItemContent}), the item text description
 * ({@link #itemDescription}) and — for collection lists — the template fixup
 * ({@link #onBeforeGenerateEntries}).
 * <p>
 * Pass 2: the inline view has no selection — the grip's left click opens the
 * per-row menu (Delete / Move up / Move down) just like the right click. The
 * row and menu factories ({@link #buildListRow}, {@link #buildRowMenu}) keep
 * no row state in the base: the caller passes its own row list (the row is
 * appended to it, and drag-reorder indices resolve against it) and, for
 * {@link RowGripBehavior#SELECT}, its own callbacks — so a modal editor can
 * rebuild the full list over its own state without touching the inline
 * view's rows.
 */
@SuppressWarnings("rawtypes")
public abstract class JIPipeListParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeListParameter> {
    /** Rows shown per page in the inline (non-scrolling) list view. */
    static final int PAGE_SIZE = 5;
    /** Custom drag flavor so only grips drag onto list rows (in-JVM identity). */
    private static final DataFlavor GRIP_DRAG_FLAVOR =
            new DataFlavor(String.class, "JIPipeListGripDrag");

    private final JLabel emptyLabel = UIUtils.createInfoLabel("This list is empty", "Click <i>Add</i> to insert a new item.");
    private final JLabel countLabel = new JLabel("");
    private JIPipeDesktopFormPanel formPanel;
    /** Current pagination state; rebuilt in {@link #reload()}. */
    ListPaginationModel pagination;
    /**
     * Invoked after a structural change to the row order (grip drag or the
     * row menu's Delete / Move up / Move down) has been written to the
     * parameter. The modal editor ({@link JIPipeDesktopListEditorDialog})
     * registers itself to rebuild its rows so the new order is visible — its
     * row list must be re-derived from the parameter. The inline view leaves
     * it null (the parameter write already reloads it).
     */
    Runnable afterRowOrderChanged;

    protected JIPipeListParameterEditorUI(Class<?> parameterClass, InitializationParameters parameters) {
        super((Class<JIPipeListParameter>) parameterClass, parameters);
        initializeBase();
        reload();
    }

    /**
     * Wires the stacked layout: the (paginated) row list in the center with
     * the "This list is empty" hint below it. No footer — the Add/count/⋯
     * controls live on the name row via {@link #createHeaderControls()}.
     */
    protected void initializeBase() {
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
    }

    @Override
    public int getUIControlStyleType() {
        return CONTROL_STYLE_LIST;
    }

    @Override
    public boolean isUIInline() {
        return false;
    }

    @Override
    public boolean isUIImportantLabelEnabled() {
        return false;
    }

    /**
     * Row-level chrome on the stacked row's name line — Add button,
     * item count, ⋯ menu (Clear list only; the per-row operations live in
     * the grip menu, #1339 pass 2).
     */
    @Override
    public JComponent createHeaderControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        controls.setOpaque(false);

        JButton addButton = new JButton("Add", JIPipe.RESOURCES.getIcon16("actions/list-add.png"));
        applyFlatHeaderButton(addButton);
        addButton.addActionListener(e -> addNewEntry());
        controls.add(addButton);

        JButton editButton = new JButton("Edit");
        applyFlatHeaderButton(editButton);
        editButton.setToolTipText("Open the list in a dialog with selection, search and bulk operations");
        editButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            new JIPipeDesktopListEditorDialog(window, this).setVisible(true);
        });
        controls.add(editButton);

        countLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        controls.add(countLabel);

        JButton menuButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/hamburger-menu.png"));
        applyFlatHeaderButton(menuButton);
        menuButton.setToolTipText("List options");
        JPopupMenu menu = UIUtils.addPopupMenuToButton(menuButton);
        JMenuItem clearItem = new JMenuItem("Clear list", JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"));
        clearItem.setToolTipText("Removes all items");
        clearItem.addActionListener(e -> clearList());
        menu.add(clearItem);
        controls.add(menuButton);

        return controls;
    }

    private static void applyFlatHeaderButton(JButton b) {
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        b.setFocusable(false);
    }

    private void updateCountLabel() {
        int n = getParameter().size();
        String text = n + " " + (n == 1 ? "item" : "items");
        countLabel.setText(text);
    }

    /**
     * Hook run after the form panel is cleared but before the rows are
     * generated. No-op by default; the collection list editor uses it for its
     * {@code ParameterCollectionListTemplate} fixup.
     */
    protected void onBeforeGenerateEntries() {
    }

    /**
     * Registers the modal's post-reorder hook (see {@link #afterRowOrderChanged}).
     */
    void setAfterRowOrderChanged(Runnable hook) {
        this.afterRowOrderChanged = hook;
    }

    /**
     * Builds the per-item content for the row at {@code index}: either the
     * parameter type's own editor UI (plain lists) or a nested mini form
     * panel (collection lists).
     *
     * @param index the item's index in the list
     * @param access the parameter access for this item
     * @return the content component (added at gridx=1 by {@link #buildListRow})
     */
    protected abstract JComponent buildItemContent(int index, JIPipeListParameterItemParameterAccess<?> access);

    /**
     * Short text description of the item at {@code index}, e.g. for a modal
     * editor's search filter.
     *
     * @param index the item's index in the list
     * @return a non-null description
     */
    public abstract String itemDescription(int index);

    @Override
    public void reload() {
        formPanel.clear();
        JIPipeListParameter<?> parameter = getParameter();
        onBeforeGenerateEntries();
        pagination = new ListPaginationModel(parameter.size(), PAGE_SIZE,
                paginationEnabled());
        int visibleCount = pagination.visibleCount();
        List<EntryComponents> rows = new ArrayList<>();
        for (int i = 0; i < visibleCount; ++i) {
            formPanel.addWideToForm(buildListRow(i, RowGripBehavior.MENU, null, rows).entryPanel);
        }
        formPanel.addWideToForm(buildPaginationFooter());
        formPanel.addVerticalGlue();
        emptyLabel.setVisible(parameter.isEmpty());
        updateCountLabel();
    }

    /** @return whether the inline view paginates (false for scroll-bar lists) */
    private boolean paginationEnabled() {
        ListParameterSettings settings = getParameterAccess().getAnnotationOfType(ListParameterSettings.class);
        return settings == null || !settings.withScrollBar();
    }

    /**
     * The centered footer below the visible rows: a "+k more" button (reveals
     * another page) and/or a "Show less" button (collapses to the first page)
     * — both may show while the list is partially expanded. The panel is
     * always added (invisible when there is nothing to paginate), so the body
     * keeps a constant child structure: rows → footer → glue.
     */
    private JComponent buildPaginationFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        footer.setOpaque(false);
        if (pagination.showLoadMore()) {
            JButton loadMoreButton = new JButton("+" + pagination.loadMoreRemaining() + " more", JIPipe.RESOURCES.getIcon16("actions/list-add.png"));
            applyFlatHeaderButton(loadMoreButton);
            loadMoreButton.addActionListener(e -> {
                pagination.loadMore();
                reload();
            });
            footer.add(loadMoreButton);
        }
        if (pagination.showShowLess()) {
            JButton showLessButton = new JButton("Show less");
            applyFlatHeaderButton(showLessButton);
            showLessButton.addActionListener(e -> {
                pagination.showLess();
                reload();
            });
            footer.add(showLessButton);
        }
        footer.setVisible(footer.getComponentCount() > 0);
        return footer;
    }

    /**
     * Builds one list row: a vertical grip (drag to reorder; left or right
     * click opens the per-row menu for {@link RowGripBehavior#MENU}) and the
     * item content at gridx=1. No row state is kept in the base — the row is
     * appended to the caller's {@code rows} list, and grip drags resolve
     * their indices against that same list, so a modal editor (SELECT
     * behavior) can rebuild the full list over its own state without
     * touching the inline view's rows.
     *
     * @param index    the item's index in the list
     * @param behavior how the grip's left click behaves
     * @param cbs      callbacks for SELECT behavior (may be null for MENU)
     * @param rows     the caller's row list; the built row is appended to it
     * @return the row's components
     */
    EntryComponents buildListRow(int index, RowGripBehavior behavior, ListRowCallbacks cbs, List<EntryComponents> rows) {
        JIPipeListParameter<?> parameter = getParameter();

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        EntryComponents entryComponents = new EntryComponents();

        JButton handleButton = new JButton(JIPipe.RESOURCES.getIcon16Inverted("actions/grip-lines-vertical.png"));
        handleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        handleButton.setOpaque(false);
        UIUtils.makeButtonFlat25x25(handleButton);
        handleButton.setToolTipText(behavior == RowGripBehavior.SELECT
                ? "Select this entry (Ctrl: toggle, Shift: range). Drag to reorder."
                : "Menu for this entry. Drag to reorder.");
        handleButton.addActionListener(e -> {
            boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK;
            boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK;
            if (behavior == RowGripBehavior.SELECT && cbs != null) {
                cbs.onGripActivate(index, shift, ctrl);
            } else {
                JPopupMenu menu = buildRowMenu(index);
                menu.revalidate();
                menu.show(handleButton, 0, handleButton.getHeight());
            }
        });
        buttonPanel.add(handleButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NORTH, new Insets(0, 0, 0, 0), 0, 0));

        // Right-click opens the same per-row menu for both behaviors
        JPopupMenu handleMenu = UIUtils.addRightClickPopupMenuToButton(handleButton);
        populateRowMenu(handleMenu, index);

        JComponent content = buildItemContent(index, new JIPipeListParameterItemParameterAccess(getParameterAccess(),
                parameter,
                parameter.getContentClass(),
                index));
        content.setOpaque(false);

        JPanel entryPanel = new JPanel(new GridBagLayout());
        entryPanel.setOpaque(true);
        entryPanel.add(buttonPanel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
        entryPanel.add(content, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));

        // Drag-reorder via the grip; the whole row accepts the drop (child
        // components keep their own handlers, so text fields etc. are unaffected).
        // Indices resolve against the caller's row list (the one this row is
        // appended to), so inline rows and a modal's own rows stay separate.
        GripDragTransferHandler dragHandler = new GripDragTransferHandler(entryComponents, rows);
        handleButton.setTransferHandler(dragHandler);
        entryPanel.setTransferHandler(dragHandler);

        entryComponents.entryPanel = entryPanel;
        entryComponents.grip = handleButton;
        rows.add(entryComponents);
        return entryComponents;
    }

    /**
     * The per-row menu, built purely from the parameter (no row or selection
     * state): Delete (removes just this item) and Move up / Move down
     * (single-step moves). Reused by the modal editor verbatim.
     *
     * @param index the item's index in the list
     * @return a new popup menu
     */
    JPopupMenu buildRowMenu(int index) {
        JPopupMenu menu = new JPopupMenu();
        populateRowMenu(menu, index);
        return menu;
    }

    private void populateRowMenu(JPopupMenu menu, int index) {
        JIPipeListParameter<?> parameter = getParameter();
        menu.add(UIUtils.createMenuItem("Delete", "Removes this item", JIPipe.RESOURCES.getIcon16("actions/delete.png"), () -> {
            parameter.remove(index);
            setParameter(parameter, true);
            if (afterRowOrderChanged != null) {
                afterRowOrderChanged.run();
            }
        }));
        menu.addSeparator();
        menu.add(UIUtils.createMenuItem("Move up", "Moves this item one position up", JIPipe.RESOURCES.getIcon16("actions/go-up.png"), () -> moveEntryTo(index, index - 1)));
        menu.add(UIUtils.createMenuItem("Move down", "Moves this item one position down", JIPipe.RESOURCES.getIcon16("actions/go-down.png"), () -> moveEntryTo(index, index + 1)));
    }

    /**
     * Removes all items.
     */
    public void clearList() {
        JIPipeListParameter<?> parameter = getParameter();
        parameter.clear();
        setParameter(parameter, true);
    }

    /**
     * Adds a new item.
     */
    public void addNewEntry() {
        JIPipeListParameter<?> parameter = getParameter();
        parameter.addNewInstance();
        setParameter(parameter, true);
    }

    /**
     * Moves the item at {@code from} to be at position {@code to} (the
     * position in the list <em>before</em> the removal; i.e. the row index the
     * drop would insert at). Adjusts for the shift caused by the removal and
     * clamps to the valid range.
     *
     * @return true if a move happened, false if the indices were out of bounds
     */
    public static boolean moveItem(JIPipeListParameter list, int from, int to) {
        if (list == null || from < 0 || from >= list.size()) {
            return false;
        }
        Object value = list.remove(from);
        int adjusted = to;
        if (adjusted > from) {
            adjusted--;
        }
        adjusted = Math.max(0, Math.min(adjusted, list.size()));
        list.add(adjusted, value);
        return true;
    }

    private void moveEntryTo(int from, int to) {
        if (moveItem(getParameter(), from, to)) {
            setParameter(getParameter(), true);
            if (afterRowOrderChanged != null) {
                afterRowOrderChanged.run();
            }
        }
    }

    /**
     * Drags a list row onto another row to reorder it. The handler is attached
     * to the grip button (the drag source); the drop target row is whichever
     * grip the drop lands on, and before/after is decided by the drop
     * y-position within that row (upper half = before). Drop indices are
     * resolved against the caller-supplied row list the dragged row belongs
     * to (captured per row when it was built). The right-click menu (Delete /
     * Move up / Move down) remains the guaranteed path.
     */
    private class GripDragTransferHandler extends TransferHandler {
        private final EntryComponents target;
        private final List<EntryComponents> rows;

        GripDragTransferHandler(EntryComponents target, List<EntryComponents> rows) {
            this.target = target;
            this.rows = rows;
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
            int sourceIndex = rows.indexOf(source);
            int targetIndex = rows.indexOf(target);
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

    /**
     * One built list row: its panel, the grip button, and a selection flag.
     * Package-visible so the modal editor (Task 4) can hold its own
     * {@code List<EntryComponents>} of rows built via {@link #buildListRow}.
     */
    static class EntryComponents {
        JPanel entryPanel;
        JButton grip;
        boolean selected;
    }

    /**
     * Grip activation behavior:
     * <ul>
     * <li>{@code MENU} — the inline view: the grip's left click opens the
     * per-row menu (Delete / Move up / Move down), like the right click.</li>
     * <li>{@code SELECT} — the modal view: the grip's left click is forwarded
     * to {@link ListRowCallbacks#onGripActivate} (single / Ctrl-toggle /
     * Shift-range selection over the caller's own state).</li>
     * </ul>
     * The right click opens the per-row menu for both.
     */
    enum RowGripBehavior {
        MENU, SELECT
    }

    /**
     * Callbacks a caller of {@link #buildListRow} supplies so the row stays
     * stateless — the modal editor owns its selection state.
     */
    interface ListRowCallbacks {
        /**
         * The grip's left click (SELECT behavior).
         *
         * @param index the row's item index in the list
         * @param shift Shift was pressed (range from the last clicked row)
         * @param ctrl  Ctrl was pressed (toggle)
         */
        void onGripActivate(int index, boolean shift, boolean ctrl);
    }
}
