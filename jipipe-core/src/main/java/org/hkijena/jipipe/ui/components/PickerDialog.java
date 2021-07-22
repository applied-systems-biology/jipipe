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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.extensions.parameters.primitives.EnumItemInfo;
import org.hkijena.jipipe.extensions.parameters.primitives.EnumParameterEditorUI;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * UI for picking a set of values
 */
public abstract class PickerDialog<T> extends JDialog {
    private List<T> availableItems;
    private SearchTextField searchField;
    private JList<T> itemJList;
    private T selectedItem;
    private JButton confirmButton;
    private boolean canceled = true;
    private Comparator<T> itemComparator;

    public PickerDialog(Window parent) {
        super(parent);
        initialize();
    }

    public List<T> getAvailableItems() {
        return availableItems;
    }

    public void setAvailableItems(List<T> availableItems) {
        this.availableItems = availableItems;
        reloadItemList();
    }

    private void initialize() {
        setContentPane(new JPanel(new BorderLayout(8, 8)));
        initializeToolBar();

        itemJList = new JList<>();
        itemJList.addListSelectionListener(e -> {
            if (itemJList.getSelectedValue() != null) {
                setSelectedItem(itemJList.getSelectedValue());
            }
        });
        JScrollPane scrollPane = new JScrollPane(itemJList);
        add(scrollPane, BorderLayout.CENTER);
        initializeButtonPanel();
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            canceled = true;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        confirmButton = new JButton("Select", UIUtils.getIconFromResources("actions/color-select.png"));
        confirmButton.addActionListener(e -> pickAlgorithm());
        confirmButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                pickAlgorithm();
            }
        });
        buttonPanel.add(confirmButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void pickAlgorithm() {
        canceled = false;
        setVisible(false);
    }

    private void initializeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadItemList());
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
            if (selectedItem != null && e.getKeyCode() == KeyEvent.VK_ENTER) {
                confirmButton.requestFocusInWindow();
            }
            }
        });
        toolBar.add(searchField);

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * Gets the search string for the specified icon
     * @param item the item
     * @return string that contains the properties of this item
     */
    protected abstract String getSearchString(T item);

    public List<T> getFilteredAndSortedItems() {
        Predicate<T> filterFunction = item -> searchField.test(getSearchString(item));
        Comparator<T> comparator = itemComparator;
        if(comparator == null)
            comparator = new NaturalOrderComparator<>();
        return availableItems.stream().filter(filterFunction).sorted(comparator).collect(Collectors.toList());
    }

    private void reloadItemList() {
        setSelectedItem(null);
        List<T> available = getFilteredAndSortedItems();
        DefaultListModel<T> listModel = new DefaultListModel<>();
        int selectedIndex = -1;
        int index = 0;
        for (T type : available) {
            listModel.addElement(type);
            if (type == selectedItem)
                selectedIndex = index;
            ++index;
        }
        itemJList.setModel(listModel);
        if (selectedIndex >= 0) {
            itemJList.setSelectedIndex(selectedIndex);
        } else {
            if (!listModel.isEmpty()) {
                itemJList.setSelectedIndex(0);
            }
        }
    }

    public T getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(T selectedItem) {
        this.selectedItem = selectedItem;
        itemJList.setSelectedValue(selectedItem, true);
    }

    public Comparator<T> getItemComparator() {
        return itemComparator;
    }

    public void setItemComparator(Comparator<T> itemComparator) {
        this.itemComparator = itemComparator;
    }

    public ListCellRenderer<? super T> getCellRenderer() {
        return itemJList.getCellRenderer();
    }

    public void setCellRenderer(ListCellRenderer<? super T> cellRenderer) {
        itemJList.setCellRenderer(cellRenderer);
    }

    public T showDialog() {
        setModal(true);
        pack();
        setSize(new Dimension(500, 500));
        setLocationRelativeTo(getParent());
        UIUtils.addEscapeListener(this);
        setVisible(true);
        if (!canceled)
            return getSelectedItem();
        else
            return null;
    }
}
