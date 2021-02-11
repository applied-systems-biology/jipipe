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

package org.hkijena.jipipe.installer.linux.ui.utils;


import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Utils for UI
 */
public class UIUtils {

    public static final Insets UI_PADDING = new Insets(4, 4, 4, 4);
    public static final Map<String, ImageIcon> ICON_FROM_RESOURCES_CACHE = new HashMap<>();

    /**
     * Adds an existing popup menu to a button
     *
     * @param target    target button
     * @param popupMenu the popup menu
     * @return the popup menu
     */
    public static JPopupMenu addPopupMenuToComponent(AbstractButton target, JPopupMenu popupMenu) {
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                if (target.isEnabled())
                    popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        });
        target.addActionListener(e -> {
            if (target.isEnabled()) {
                if (MouseInfo.getPointerInfo().getLocation().x < target.getLocationOnScreen().x
                        || MouseInfo.getPointerInfo().getLocation().x > target.getLocationOnScreen().x + target.getWidth()
                        || MouseInfo.getPointerInfo().getLocation().y < target.getLocationOnScreen().y
                        || MouseInfo.getPointerInfo().getLocation().y > target.getLocationOnScreen().y + target.getHeight()) {
                    popupMenu.show(target, 0, target.getHeight());
                }
            }
        });
        return popupMenu;
    }


    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static ImageIcon getIconFromResources(String iconName) {
        String path = "icons/" + iconName;
        ImageIcon icon = ICON_FROM_RESOURCES_CACHE.getOrDefault(path, null);
        if (icon == null) {
            icon = new ImageIcon(ResourceUtils.getPluginResource(path));
            ICON_FROM_RESOURCES_CACHE.put(path, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static ImageIcon getIcon32FromResources(String iconName) {
        String path = "icons-32/" + iconName;
        ImageIcon icon = ICON_FROM_RESOURCES_CACHE.getOrDefault(path, null);
        if (icon == null) {
            icon = new ImageIcon(ResourceUtils.getPluginResource(path));
            ICON_FROM_RESOURCES_CACHE.put(path, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static ImageIcon getIcon64FromResources(String iconName) {
        String path = "icons-64/" + iconName;
        ImageIcon icon = ICON_FROM_RESOURCES_CACHE.getOrDefault(path, null);
        if (icon == null) {
            icon = new ImageIcon(ResourceUtils.getPluginResource(path));
            ICON_FROM_RESOURCES_CACHE.put(path, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static ImageIcon getIcon128FromResources(String iconName) {
        String path = "icons-128/" + iconName;
        ImageIcon icon = ICON_FROM_RESOURCES_CACHE.getOrDefault(path, null);
        if (icon == null) {
            icon = new ImageIcon(ResourceUtils.getPluginResource(path));
            ICON_FROM_RESOURCES_CACHE.put(path, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static ImageIcon getIcon8FromResources(String iconName) {
        String path = "icons-8/" + iconName;
        ImageIcon icon = ICON_FROM_RESOURCES_CACHE.getOrDefault(path, null);
        if (icon == null) {
            icon = new ImageIcon(ResourceUtils.getPluginResource(path));
            ICON_FROM_RESOURCES_CACHE.put(path, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static URL getIconURLFromResources(String iconName) {
        return ResourceUtils.getPluginResource("icons/" + iconName);
    }

    /**
     * Makes a button flat
     *
     * @param component the button
     */
    public static void makeFlat(AbstractButton component) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
        Border margin = new EmptyBorder(5, 15, 5, 15);
//        Border compound = new CompoundBorder(BorderFactory.createEtchedBorder(), margin);
        //        Border margin = new EmptyBorder(2, 2, 2, 2);
        Border compound = new CompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                new CompoundBorder(new RoundedLineBorder(ModernMetalTheme.MEDIUM_GRAY, 1, 2), margin));
        component.setBorder(compound);
    }

    /**
     * Makes a button flat
     *
     * @param component   the button
     * @param borderColor border color
     * @param top         top border size
     * @param left        left border size
     * @param right       right border size
     * @param bottom      bottom border size
     */
    public static void makeFlat(AbstractButton component, Color borderColor, int top, int left, int right, int bottom) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
        Border margin = new EmptyBorder(5, 15, 5, 15);
        Border compound = new CompoundBorder(BorderFactory.createMatteBorder(top, left, bottom, right, borderColor), margin);
        component.setBorder(compound);
    }

    /**
     * Makes a button flat and 25x25 size
     *
     * @param component the button
     */
    public static void makeFlat25x25(AbstractButton component) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
        component.setPreferredSize(new Dimension(25, 25));
        component.setMinimumSize(new Dimension(25, 25));
        component.setMaximumSize(new Dimension(25, 25));
//        Border margin = new EmptyBorder(2, 2, 2, 2);
//        Border compound = new CompoundBorder(new RoundedLineBorder(ModernMetalTheme.GRAY2, 1, 2), margin);
//        component.setBorder(compound);
        component.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    }

    /**
     * Makes a button flat and have a height of 25px
     *
     * @param component the button
     */
    public static void makeFlatH25(AbstractButton component) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
//        component.setPreferredSize(new Dimension(component.getPreferredSize().width, 25));
//        component.setMinimumSize(new Dimension(25, 25));
//        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        Border margin = new EmptyBorder(2, 2, 2, 2);
        Border compound = new CompoundBorder(BorderFactory.createEtchedBorder(), margin);
        component.setBorder(compound);
    }

    /**
     * Makes a component borderless
     *
     * @param component the component
     */
    public static void makeBorderlessWithoutMargin(AbstractButton component) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
        component.setBorder(null);
    }

    /**
     * Installs an event to the window that asks the user before the window is closes
     *
     * @param window  the window
     * @param message the close message
     * @param title   the close message title
     */
    public static void setToAskOnClose(JFrame window, String message, String title) {
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if (JOptionPane.showConfirmDialog(windowEvent.getComponent(), message, title,
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });
    }

    /**
     * Creates a readonly text area
     * Cannot do HTML.
     *
     * @param text text
     * @return text area
     */
    public static JTextArea makeReadonlyTextArea(String text) {
        JTextArea textArea = new JTextArea();
        textArea.setBorder(BorderFactory.createEtchedBorder());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(text);
        return textArea;
    }


    /**
     * Returns a string from clipboard or an empty string
     *
     * @return clipboard string or empty
     */
    public static String getStringFromClipboard() {
        String ret = "";
        Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();

        Transferable clipTf = sysClip.getContents(null);

        if (clipTf != null) {

            if (clipTf.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    ret = (String) clipTf
                            .getTransferData(DataFlavor.stringFlavor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;
    }

    /**
     * Opens a website with given URL
     *
     * @param url the URL
     */
    public static void openWebsite(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens a website with given URL
     *
     * @param path the file
     */
    public static void openFileInNative(Path path) {
        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
