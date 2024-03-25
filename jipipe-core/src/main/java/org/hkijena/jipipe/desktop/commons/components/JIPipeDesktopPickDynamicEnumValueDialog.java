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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicEnumParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicEnumDesktopParameterEditorUI;
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
public class JIPipeDesktopPickDynamicEnumValueDialog<T> extends JDialog {
    private final DynamicEnumParameter<T> dynamicEnumParameter;
    private List<T> availableItems;
    private JIPipeDesktopSearchTextField searchField;
    private JList<Object> itemJList;
    private T selectedItem;
    private JButton confirmButton;

    private JScrollPane scrollPane;
    private boolean canceled = true;

    public JIPipeDesktopPickDynamicEnumValueDialog(Window parent, DynamicEnumParameter<T> dynamicEnumParameter, T preSelected) {
        super(parent);
        this.dynamicEnumParameter = dynamicEnumParameter;
        this.availableItems = dynamicEnumParameter.getAllowedValues();
        initialize();
        reloadItemList();
        if (preSelected == null) {
            preSelected = (T) itemJList.getSelectedValue();
        }
        setSelectedItem(preSelected);
        itemJList.setSelectedValue(preSelected, true);
    }

    public static <T> T showDialog(Component parent, DynamicEnumParameter<T> dynamicEnumParameter, Object preSelected, String title) {
        JIPipeDesktopPickDynamicEnumValueDialog<T> dialog = new JIPipeDesktopPickDynamicEnumValueDialog(SwingUtilities.getWindowAncestor(parent), dynamicEnumParameter, preSelected);
        dialog.setTitle(title);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500, 500));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
        if (!dialog.canceled)
            return (T) dialog.getSelectedItem();
        else
            return null;
    }

    private void initialize() {
        setContentPane(new JPanel(new BorderLayout(8, 8)));
        initializeToolBar();

        itemJList = new JList<>();
        itemJList.setCellRenderer(new DynamicEnumDesktopParameterEditorUI.Renderer(dynamicEnumParameter));
        itemJList.addListSelectionListener(e -> {
            if (itemJList.getSelectedValue() != null) {
                setSelectedItem((T) itemJList.getSelectedValue());
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

        searchField = new JIPipeDesktopSearchTextField();
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

    private List<T> getFilteredAndSortedInfos() {
        Predicate<T> filterFunction = info -> searchField.test(dynamicEnumParameter.getSearchString(info));
        return availableItems.stream().filter(filterFunction).sorted(Comparator.comparing(dynamicEnumParameter::renderLabel)).collect(Collectors.toList());
    }

    private void reloadItemList() {
        setSelectedItem(null);
        List<T> available = getFilteredAndSortedInfos();
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

    public T getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(T selectedItem) {
        this.selectedItem = selectedItem;
    }
}
