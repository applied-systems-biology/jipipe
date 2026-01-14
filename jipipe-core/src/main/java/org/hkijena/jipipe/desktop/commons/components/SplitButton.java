package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class SplitButton extends JPanel {
    private JButton mainButton;
    private JButton arrowButton;
    private JPopupMenu popupMenu;
    private Consumer<JPopupMenu> popupMenuReloadFunction;

    public SplitButton() {
        initialize();
    }

    public SplitButton(String text) {
        initialize();
        mainButton.setText(text);
    }

    public SplitButton(String text, Icon icon) {
        initialize();
        mainButton.setText(text);
        mainButton.setIcon(icon);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        Color borderColor = ThemeUtils.getCurrentStyle().getBorderColor();
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1,1,1,1),
                new RoundedLineBorder(borderColor, 1, 5)));

        // Main button
        mainButton = new JButton();
        mainButton.setOpaque(false);
        mainButton.setFocusPainted(false);
        mainButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,5));

        // Arrow button
        arrowButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/caret-down.png"));
        arrowButton.setFocusPainted(false);
        arrowButton.setOpaque(false);
        arrowButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,1,0,0, borderColor),
                BorderFactory.createEmptyBorder(0,5,0,0)));
        arrowButton.setMargin(new Insets(2, 5, 2, 5));

        // Popup menu
        popupMenu = new JPopupMenu();

        // Add components
        add(mainButton, BorderLayout.CENTER);
        add(arrowButton, BorderLayout.EAST);

        // Event handling
        UIUtils.addReloadablePopupMenuToButton(arrowButton, popupMenu, this::reloadPopupMenu);
    }

    protected void reloadPopupMenu() {
        if(popupMenuReloadFunction != null) {
            popupMenuReloadFunction.accept(popupMenu);
        }
    }

    public void setText(String  text) {
        mainButton.setText(text);
    }

    public void setIcon(Icon icon) {
        mainButton.setIcon(icon);
    }

    public JButton getMainButton() {
        return mainButton;
    }

    public JButton getArrowButton() {
        return arrowButton;
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void addMenuItem(String text, ActionListener listener) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(listener);
        popupMenu.add(menuItem);
    }

    public void addSeparator() {
        popupMenu.addSeparator();
    }

    public void addActionListener(ActionListener listener) {
        mainButton.addActionListener(listener);
    }

    @Override
    public Dimension getPreferredSize() {
        // Calculate main button size
        Dimension mainSize = calculateButtonSize(mainButton);

        // Calculate arrow button size
        Dimension arrowSize = calculateButtonSize(arrowButton);

        // Calculate combined width and maximum height
        int width = mainSize.width + arrowSize.width;
        int height = Math.max(mainSize.height, arrowSize.height);

        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        // Use preferred size as minimum size
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(getPreferredSize().width * 2, Integer.MAX_VALUE);
    }

    private Dimension calculateButtonSize(JButton button) {
        // Get font metrics
        Font font = button.getFont();
        FontMetrics fm = button.getFontMetrics(font);

        // Calculate text width
        String text = button.getText();
        int textWidth = fm.stringWidth(text);

        // Add icon width if present
        Icon icon = button.getIcon();
        int iconWidth = 0;
        if (icon != null) {
            iconWidth = icon.getIconWidth();
        }

        // Calculate total content width
        int contentWidth = textWidth + iconWidth;

        // Add margins
        Insets margin = button.getMargin();
        int totalWidth = contentWidth + margin.left + margin.right;

        // Calculate height
        int textHeight = fm.getHeight();
        int iconHeight = 0;
        if (icon != null) {
            iconHeight = icon.getIconHeight();
        }

        int contentHeight = Math.max(textHeight, iconHeight);
        int totalHeight = contentHeight + margin.top + margin.bottom;

        // Add border insets
        Border border = button.getBorder();
        if (border != null) {
            Insets borderInsets = border.getBorderInsets(button);
            totalWidth += borderInsets.left + borderInsets.right;
            totalHeight += borderInsets.top + borderInsets.bottom;
        }

        return new Dimension(totalWidth, totalHeight);
    }

    public void setPopupMenuReloadFunction(Consumer<JPopupMenu> popupMenuReloadFunction) {
        this.popupMenuReloadFunction = popupMenuReloadFunction;
    }

    public Consumer<JPopupMenu> getPopupMenuReloadFunction() {
        return popupMenuReloadFunction;
    }
}
