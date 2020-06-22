package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Dialog that picks an icon
 */
public class ACAQIconPickerDialog extends JDialog implements MouseListener {

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
    public ACAQIconPickerDialog(Window parent, String prefix, Set<String> availableIcons) {
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
        add(new JScrollPane(iconList), BorderLayout.CENTER);
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

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> {
            this.selectedIcon = null;
            this.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Select", UIUtils.getIconFromResources("pick.png"));
        confirmButton.addActionListener(e -> this.setVisible(false));
        buttonPanel.add(confirmButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void reloadIconList() {
        DefaultListModel<String> model = (DefaultListModel<String>) iconList.getModel();
        model.clear();
        String[] searchStrings = searchField.getSearchStrings();
        for (String icon : availableIcons) {
            boolean matches = true;
            if (searchStrings != null) {
                for (String searchString : searchStrings) {
                    if (!icon.toLowerCase().contains(searchString)) {
                        matches = false;
                        break;
                    }
                }
            }
            if (matches) {
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
        ACAQIconPickerDialog dialog = new ACAQIconPickerDialog(SwingUtilities.getWindowAncestor(parent),
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
