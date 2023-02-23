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

import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicEnumParameter;
import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicEnumParameterEditorUI;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * UI that adds slots to an algorithm
 */
public class PickDynamicEnumValueDialog extends JDialog {
    private final DynamicEnumParameter<Object> dynamicEnumParameter;
    private List<Object> availableItems;
    private SearchTextField searchField;
    private JList<Object> itemJList;
    private Object selectedItem;
    private JButton confirmButton;

    private JScrollPane scrollPane;
    private boolean canceled = true;

    public PickDynamicEnumValueDialog(Window parent, DynamicEnumParameter<Object> dynamicEnumParameter, Object preSelected) {
        super(parent);
        this.dynamicEnumParameter = dynamicEnumParameter;
        this.availableItems = dynamicEnumParameter.getAllowedValues();
        initialize();
        reloadItemList();
        if (preSelected == null) {
            preSelected = itemJList.getSelectedValue();
        }
        setSelectedItem(preSelected);
        itemJList.setSelectedValue(preSelected, true);
    }

    public static Object showDialog(Component parent, DynamicEnumParameter<Object> dynamicEnumParameter, Object preSelected, String title) {
        PickDynamicEnumValueDialog dialog = new PickDynamicEnumValueDialog(SwingUtilities.getWindowAncestor(parent), dynamicEnumParameter, preSelected);
        dialog.setTitle(title);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500, 500));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
        if (!dialog.canceled)
            return dialog.getSelectedItem();
        else
            return null;
    }

    private void initialize() {
        setContentPane(new JPanel(new BorderLayout(8, 8)));
        initializeToolBar();

        itemJList = new JList<>();
        itemJList.setCellRenderer(new DynamicEnumParameterEditorUI.Renderer(dynamicEnumParameter));
        itemJList.addListSelectionListener(e -> {
            if (itemJList.getSelectedValue() != null) {
                setSelectedItem(itemJList.getSelectedValue());
            }
        });
        itemJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    pickValue();
                }
            }
        });

        scrollPane = new JScrollPane(itemJList);
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
        confirmButton.addActionListener(e -> pickValue());
        confirmButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                pickValue();
            }
        });
        buttonPanel.add(confirmButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void pickValue() {
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

    private List<Object> getFilteredAndSortedInfos() {
        Predicate<Object> filterFunction = info -> searchField.test(dynamicEnumParameter.getSearchString(info));
        return availableItems.stream().filter(filterFunction).sorted(Comparator.comparing(dynamicEnumParameter::renderLabel)).collect(Collectors.toList());
    }

    private void reloadItemList() {
        setSelectedItem(null);
        List<Object> available = getFilteredAndSortedInfos();
        DefaultListModel<Object> listModel = new DefaultListModel<>();
        int selectedIndex = -1;
        int index = 0;
        for (Object type : available) {
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
        UIUtils.invokeScrollToTop(scrollPane);
    }

    public Object getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(Object selectedItem) {
        this.selectedItem = selectedItem;
    }
}
