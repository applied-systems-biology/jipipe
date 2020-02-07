/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.utils;

import org.hkijena.acaq5.ui.components.ColorIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class UIUtils {

    public static final Insets UI_PADDING = new Insets(4,4,4,4);

    public static JLabel createDescriptionLabelUI(JPanel panel, String text, int row, int column) {
        JLabel description = new JLabel(text);
        panel.add(description, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = column;
                gridy = row;
                insets = UI_PADDING;
            }
        });
        return description;
    }

    public static JTextField createDescriptionTextFieldUI(JPanel panel, String text, int row, int column) {
        JTextField description = new JTextField(text);
        description.setEditable(false);
        description.setBorder(null);
        panel.add(description, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = column;
                gridy = row;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        });
        return description;
    }

    public static JTextArea createDescriptionTextAreaUI(JPanel panel, String text, int row, int column) {
        JTextArea description = new JTextArea(text);
        description.setEditable(false);
        description.setBorder(null);
        description.setOpaque(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);
        panel.add(description, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = column;
                gridy = row;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        });
        return description;
    }

    public static void addToGridBag(JPanel panel, JButton component, int row, int column) {
        panel.add(component, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = column;
                gridy = row;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
            }
        });
    }

    public static JLabel backgroundColorJLabel(JLabel label, Color color) {
        label.setOpaque(true);
        label.setBackground(color);

        // Set the text color to white if needed
        double r = color.getRed() / 255.0;
        double g = color.getGreen() / 255.0;
        double b = color.getBlue() / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));

        double v = (max + min) / 2.0;
        if(v < 0.5) {
            label.setForeground(Color.WHITE);
        }

        return label;
    }

    public static JLabel borderedJLabel(JLabel label) {
        label.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        return label;
    }

    public static void addFillerGridBagComponent(Container component, int row) {
        component.add(new JPanel(), new GridBagConstraints() {
            {
                anchor = GridBagConstraints.PAGE_START;
                gridx = 0;
                gridy = row;
                fill = GridBagConstraints.HORIZONTAL | GridBagConstraints.VERTICAL;
                weightx = 1;
                weighty = 1;
            }
        });
    }

    public static void addFillerGridBagComponent(Container component, int row, int column) {
        component.add(new JPanel(), new GridBagConstraints() {
            {
                anchor = GridBagConstraints.PAGE_START;
                gridx = column;
                gridy = row;
                fill = GridBagConstraints.HORIZONTAL | GridBagConstraints.VERTICAL;
                weightx = 1;
                weighty = 1;
            }
        });
    }

    public static JPopupMenu addPopupMenuToComponent(AbstractButton target) {
        JPopupMenu popupMenu = new JPopupMenu();
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        });
        target.addActionListener(e -> {

            if(MouseInfo.getPointerInfo().getLocation().x >= target.getLocationOnScreen().x
                    && MouseInfo.getPointerInfo().getLocation().x <= target.getLocationOnScreen().x + target.getWidth()
                    && MouseInfo.getPointerInfo().getLocation().y >= target.getLocationOnScreen().y
                    && MouseInfo.getPointerInfo().getLocation().y <= target.getLocationOnScreen().y + target.getHeight()) {

            }
            else {
                popupMenu.show(target, 0, target.getHeight());
            }
        });
        return popupMenu;
    }

    public static ImageIcon getIconFromResources(String iconName) {
        return new ImageIcon(ResourceUtils.getPluginResource("icons/" + iconName));
    }

    public static ColorIcon getIconFromColor(Color color) {
        return new ColorIcon(16, 16, color);
    }

    public static void makeFlat(AbstractButton component) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
        Border margin = new EmptyBorder(5, 15, 5, 15);
        Border compound = new CompoundBorder( BorderFactory.createEtchedBorder(), margin);
        component.setBorder(compound);
    }

    public static void makeFlatWithoutMargin(AbstractButton component) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
        component.setBorder(null);
    }

    public static void setToAskOnClose(JFrame window, String message, String title) {
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if(JOptionPane.showConfirmDialog(windowEvent.getComponent(), message, title,
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    windowEvent.getWindow().dispose();
                }
            }
        });
    }

    public static Color stringToColor(String string, float s, float b) {
        float h = Math.abs(string.hashCode() % 256) / 255.0f;
        return Color.getHSBColor(h, s, b);
    }

    /**
     * Expands the whole tree
     * @param tree
     */
    public static void expandAllTree(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

}
