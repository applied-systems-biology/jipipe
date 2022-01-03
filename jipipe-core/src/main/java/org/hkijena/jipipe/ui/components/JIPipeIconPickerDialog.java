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

import org.hkijena.jipipe.ui.components.renderers.PrefixedIconListCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Dialog that picks an icon
 */
public class JIPipeIconPickerDialog extends JDialog implements MouseListener {

    private String selectedIcon;
    private String prefix;
    private List<String> availableIcons;
    private SearchTextField searchField;
    private JList<String> iconList;

    /**
     * @param parent         The window parent
     * @param prefix         The resource prefix
     * @param availableIcons Icon names without resource prefix
     */
    public JIPipeIconPickerDialog(Window parent, String prefix, Set<String> availableIcons) {
        super(parent);
        this.prefix = prefix;
        this.availableIcons = new ArrayList<>(availableIcons);
        this.availableIcons.sort(String::compareTo);
        initialize();
        reloadIconList();
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));
        initializeToolbar();
        initializeButtonPanel();

        iconList = new JList<>();
        iconList.setModel(new DefaultListModel<>());
        iconList.setCellRenderer(new PrefixedIconListCellRenderer(prefix));
        iconList.setModel(new DefaultListModel<>());
        iconList.addListSelectionListener(e -> selectedIcon = iconList.getSelectedValue());
        iconList.addMouseListener(this);
        JScrollPane scrollPane = new JScrollPane(iconList);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadIconList());
        toolBar.add(searchField);

        add(toolBar, BorderLayout.NORTH);
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            this.selectedIcon = null;
            this.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Select", UIUtils.getIconFromResources("actions/color-select.png"));
        confirmButton.addActionListener(e -> this.setVisible(false));
        buttonPanel.add(confirmButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void reloadIconList() {
        DefaultListModel<String> model = (DefaultListModel<String>) iconList.getModel();
        model.clear();
        for (String icon : availableIcons) {
            if (searchField.test(icon)) {
                model.addElement(icon);
            }
        }
    }

    /**
     * @return The selected icon name without prefix
     */
    public String getSelectedIcon() {
        return selectedIcon;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() >= 2) {
            setVisible(false);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    /**
     * Picks an icon name using a dialog
     *
     * @param parent         The parent component
     * @param prefix         The prefix put in front of the icon names for rendering the icon
     * @param availableIcons Icon names without prefix
     * @return The selected icon or null
     */
    public static String showDialog(Component parent, String prefix, Set<String> availableIcons) {
        JIPipeIconPickerDialog dialog = new JIPipeIconPickerDialog(SwingUtilities.getWindowAncestor(parent),
                prefix,
                availableIcons);
        dialog.pack();
        dialog.setSize(new Dimension(500, 400));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setModal(true);
        dialog.setVisible(true);

        return dialog.getSelectedIcon();
    }
}
