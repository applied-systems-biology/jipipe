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

package org.hkijena.jipipe.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.primitives.Ints;
import ij.IJ;
import org.apache.commons.lang3.SystemUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.registries.JIPipeSettingsRegistry;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.*;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.extension.MenuTarget;
import org.hkijena.jipipe.ui.theme.JIPipeUITheme;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utils for UI
 */
public class UIUtils {

    public static final FileNameExtensionFilter EXTENSION_FILTER_CSV = new FileNameExtensionFilter("CSV table (*.csv)", "csv");
    public static final FileNameExtensionFilter EXTENSION_FILTER_PNG = new FileNameExtensionFilter("PNG image (*.png)", "png");
    public static final FileNameExtensionFilter EXTENSION_FILTER_SVG = new FileNameExtensionFilter("SVG image (*.svg)", "svg");
    public static final FileNameExtensionFilter EXTENSION_FILTER_MD = new FileNameExtensionFilter("Markdown text (*.md)", "md");
    public static final FileNameExtensionFilter EXTENSION_FILTER_PDF = new FileNameExtensionFilter("Portable document format (*.pdf)", "pdf");
    public static final FileNameExtensionFilter EXTENSION_FILTER_HTML = new FileNameExtensionFilter("HTML document (*.html, *.htm)", "html", "htm");
    public static final FileNameExtensionFilter EXTENSION_FILTER_JPEG = new FileNameExtensionFilter("JPEG image (*.jpg, *.jpeg)", "jpg", "jpeg");
    public static final FileNameExtensionFilter EXTENSION_FILTER_BMP = new FileNameExtensionFilter("Bitmap image (*.bmp)", "bmp");
    public static final FileNameExtensionFilter EXTENSION_FILTER_TIFF = new FileNameExtensionFilter("TIFF image (*.tif, *.tiff)", "tif", "tiff");
    public static final FileNameExtensionFilter EXTENSION_FILTER_JIP = new FileNameExtensionFilter("JIPipe project (*.jip)", "jip");
    public static final FileNameExtensionFilter EXTENSION_FILTER_JIPE = new FileNameExtensionFilter("JIPipe extension (*.jipe)", "jipe");
    public static final FileNameExtensionFilter EXTENSION_FILTER_JIPC = new FileNameExtensionFilter("JIPipe compartment (*.jipc)", "jipc");
    public static final Insets UI_PADDING = new Insets(4, 4, 4, 4);
    public static final Map<String, ImageIcon> ICON_FROM_RESOURCES_CACHE = new HashMap<>();
    public static boolean DARK_THEME = false;

    public static void sendTrayNotification(String caption, String message, TrayIcon.MessageType messageType) {
        if (SystemUtils.IS_OS_LINUX) {
            // SystemTray does not work well on Linux
            // Try notify-send, first
            String notifySendPath = StringUtils.nullToEmpty(ProcessUtils.queryFast(Paths.get("/usr/bin/which"), new JIPipeProgressInfo(), "notify-send")).trim();
            if (!StringUtils.isNullOrEmpty(notifySendPath)) {
                Path exePath = Paths.get(notifySendPath);
                try {
                    new ProcessBuilder(exePath.toString(), caption, message).start();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Try zenity
            String zenityPath = StringUtils.nullToEmpty(ProcessUtils.queryFast(Paths.get("/usr/bin/which"), new JIPipeProgressInfo(), "zenity")).trim();
            if (!StringUtils.isNullOrEmpty(zenityPath)) {
                Path exePath = Paths.get(zenityPath);
                try {
                    new ProcessBuilder(exePath.toString(), "--notification", "--text=" + caption + "\\n" + message).start();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        if (SystemTray.isSupported()) {
            SystemTray systemTray = SystemTray.getSystemTray();
            TrayIcon trayIcon = new TrayIcon(UIUtils.getIcon32FromResources("apps/jipipe.png").getImage(), "JIPipe");
            trayIcon.setImageAutoSize(true);
            try {
                systemTray.add(trayIcon);
                Timer timer = new Timer(15000, e -> {
                    systemTray.remove(trayIcon);
                });
                timer.setRepeats(false);
                timer.start();

                trayIcon.displayMessage(caption, message, messageType);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Attempts to override the look and feel based on the JIPipe settings
     */
    public static void loadLookAndFeelFromSettings() {
        JIPipeUITheme theme = getThemeFromRawSettings();
        theme.install();
    }

    public static void applyThemeToCodeEditor(RSyntaxTextArea textArea) {
        if (DARK_THEME) {
            try {
                Theme theme = Theme.load(ResourceUtils.class.getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
                theme.apply(textArea);
            } catch (IOException ioe) { // Never happens
                ioe.printStackTrace();
            }
        }
    }

    public static JIPipeUITheme getThemeFromRawSettings() {
        Path propertyFile = JIPipeSettingsRegistry.getPropertyFile();
        JIPipeUITheme theme = JIPipeUITheme.ModernLight;
        if (Files.exists(propertyFile)) {
            try {
                JsonNode node = JsonUtils.getObjectMapper().readValue(propertyFile.toFile(), JsonNode.class);
                JsonNode themeNode = node.path("general-ui/theme");
                if (!themeNode.isMissingNode())
                    theme = JsonUtils.getObjectMapper().readerFor(JIPipeUITheme.class).readValue(themeNode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return theme;
    }

    public static BufferedImage getExtensionBuilderLogo400() {
        if (DARK_THEME) {
            try {
                return ImageIO.read(ResourceUtils.getPluginResource("logo-extension-builder-400-dark.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                return ImageIO.read(ResourceUtils.getPluginResource("logo-extension-builder-400.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static BufferedImage getLogo400() {
        if (DARK_THEME) {
            try {
                return ImageIO.read(ResourceUtils.getPluginResource("logo-400-dark.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                return ImageIO.read(ResourceUtils.getPluginResource("logo-400.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static BufferedImage getLogo() {
        if (DARK_THEME) {
            try {
                return ImageIO.read(ResourceUtils.getPluginResource("logo-dark.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                return ImageIO.read(ResourceUtils.getPluginResource("logo.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static BufferedImage getHeaderPanelBackground() {
        if (DARK_THEME) {
            try {
                return ImageIO.read(ResourceUtils.getPluginResource("infoui-background-dark.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                return ImageIO.read(ResourceUtils.getPluginResource("infoui-background.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Equivalent of {@link Box}.verticalGlue() for {@link GridBagLayout}
     *
     * @param component the target component that has {@link GridBagLayout}
     * @param row       the row
     * @param column    the column
     */
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

    /**
     * Adds a popup menu to a button.
     * Creates a new popup menu instance.
     *
     * @param target target button
     * @return the popup menu
     */
    public static JPopupMenu addPopupMenuToComponent(AbstractButton target) {
        return addPopupMenuToComponent(target, new JPopupMenu());
    }

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
     * Adds an existing popup menu to a button
     * Adds a function that is run before the popup is shown
     *
     * @param target         target button
     * @param popupMenu      the popup menu
     * @param reloadFunction the function that is run before showing the popup
     * @return the popup menu
     */
    public static JPopupMenu addReloadablePopupMenuToComponent(AbstractButton target, JPopupMenu popupMenu, Runnable reloadFunction) {
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                reloadFunction.run();
                popupMenu.revalidate();
                popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        });
        target.addActionListener(e -> {
            try {
                if (target.isDisplayable() && MouseInfo.getPointerInfo().getLocation().x < target.getLocationOnScreen().x
                        || MouseInfo.getPointerInfo().getLocation().x > target.getLocationOnScreen().x + target.getWidth()
                        || MouseInfo.getPointerInfo().getLocation().y < target.getLocationOnScreen().y
                        || MouseInfo.getPointerInfo().getLocation().y > target.getLocationOnScreen().y + target.getHeight()) {
                    reloadFunction.run();
                    popupMenu.revalidate();
                    popupMenu.show(target, 0, target.getHeight());
                }
            } catch (IllegalComponentStateException e1) {
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
     * Creates 16x16 a color icon
     *
     * @param color the color
     * @return the icon
     */
    public static ColorIcon getIconFromColor(Color color) {
        return new ColorIcon(16, 16, color);
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
                new CompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), margin));
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
                    windowEvent.getWindow().dispose();
                }
            }
        });
    }

    /**
     * Installs an event to the window that asks the user before the window is closes
     *
     * @param window  the window
     * @param message the close message
     * @param title   the close message title
     */
    public static void setToAskOnClose(JFrame window, Supplier<String> message, String title) {
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if (JOptionPane.showConfirmDialog(windowEvent.getComponent(), message.get(), title,
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    windowEvent.getWindow().dispose();
                }
            }
        });
    }

    /**
     * Expands the whole tree
     *
     * @param tree the tree
     */
    public static void expandAllTree(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * Asks the user how to open a project: In the current window or a new window
     *
     * @param parent parent component
     * @param title  window title
     * @return {@link JOptionPane}.YES_OPTION to open in this window; {@link JOptionPane}.NO_OPTION to open in a new window; {@link JOptionPane}.CANCEL_OPTION to cancel.
     */
    public static int askOpenInCurrentWindow(Component parent, String title) {
        return JOptionPane.showOptionDialog(parent,
                "Projects can either be opened in a new window or replace the project in an existing window.",
                title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"This window", "New window", "Cancel"},
                "New window");
    }

    /**
     * Creates a read-only "star-rating" label
     *
     * @param title   the label title
     * @param stars   the number of stars
     * @param maximum the maximum number of stars
     * @return the generated label
     */
    public static JLabel createStarRatingLabel(String title, double stars, int maximum) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<table><tr>");
        if (title != null && !title.isEmpty())
            builder.append("<td>").append(title).append("</td>");
        builder.append("<td>");
        for (int i = 0; i < maximum; ++i) {
            if (stars >= i + 1)
                builder.append("<img style=\"vertical-align:middle\" src=\"").append(ResourceUtils.getPluginResource("icons/star.png")).append("\" />");
            else if (stars >= i + 0.5)
                builder.append("<img style=\"vertical-align:middle\" src=\"").append(ResourceUtils.getPluginResource("icons/star-half-o.png")).append("\" />");
            else
                builder.append("<img style=\"vertical-align:middle\" src=\"").append(ResourceUtils.getPluginResource("icons/star-o.png")).append("\" />");
        }
        builder.append("</td>");
        builder.append("</tr></table>");
        builder.append("</html>");
        return new JLabel(builder.toString());
    }

    /**
     * Opens a dialog showing an exception
     *
     * @param parent    the parent component
     * @param exception the exception
     */
    public static void openErrorDialog(Component parent, Exception exception) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Error");
        UserFriendlyErrorUI errorUI = new UserFriendlyErrorUI(null, UserFriendlyErrorUI.WITH_SCROLLING);
        errorUI.displayErrors(exception);
        errorUI.addVerticalGlue();
        dialog.setContentPane(errorUI);
        dialog.setModal(false);
        dialog.pack();
        dialog.setSize(new Dimension(800, 600));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /**
     * Opens a dialog showing a validity report
     *
     * @param parent the parent component
     * @param report the report
     * @param modal  make the dialog modal
     */
    public static void openValidityReportDialog(Component parent, JIPipeValidityReport report, boolean modal) {
        JIPipeValidityReportUI ui = new JIPipeValidityReportUI(false);
        ui.setReport(report);
        JDialog dialog = new JDialog();
        dialog.setTitle("Error");
        dialog.setContentPane(ui);
        dialog.setModal(modal);
        dialog.pack();
        dialog.setSize(new Dimension(800, 600));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /**
     * Lets the user select one or multiple items from a list
     *
     * @param <T>           type
     * @param parent        parent component
     * @param input         list of input objects
     * @param selected      pre-selected items
     * @param title         the title
     * @param message       the message (can be null to disable message)
     * @param renderer      the renderer for the items (null to use default renderer)
     * @param selectionMode the selection mode
     * @return selected items or empty list if none was selected
     */
    public static <T> List<T> getSelectionByDialog(Component parent, Collection<T> input, Collection<T> selected, String title, String message, ListCellRenderer<T> renderer, ListSelectionMode selectionMode) {
        JList<T> jList = new JList<>();
        jList.setSelectionMode(selectionMode.getNativeValue());
        if (renderer != null)
            jList.setCellRenderer(renderer);
        DefaultListModel<T> model = new DefaultListModel<>();
        List<Integer> selectedIndices = new ArrayList<>();
        int index = 0;
        for (T t : input) {
            model.addElement(t);
            if (selected.contains(t))
                selectedIndices.add(index);
            ++index;
        }
        jList.setModel(model);
        jList.setSelectedIndices(Ints.toArray(selectedIndices));
        JScrollPane scrollPane = new JScrollPane(jList);
        List<Object> content = new ArrayList<>();
        if (message != null)
            content.add(message);
        content.add(scrollPane);
        int result = JOptionPane.showOptionDialog(
                parent,
                content.toArray(),
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            return jList.getSelectedValuesList();
        }
        return Collections.emptyList();
    }

    /**
     * Gets a formatted HTML string by dialog
     *
     * @param parent       the parent component
     * @param title        the title
     * @param message      message
     * @param initialValue initial value
     * @return value or null
     */
    public static HTMLText getHTMLByDialog(Component parent, String title, String message, HTMLText initialValue) {
        HTMLEditor area = new HTMLEditor(HTMLEditor.NONE);
        area.setText(initialValue.getHtml());
        JScrollPane scrollPane = new JScrollPane(area);
        int result = JOptionPane.showOptionDialog(
                parent,
                new Object[]{message, scrollPane},
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            return new HTMLText(area.getHTML());
        }
        return null;
    }

    /**
     * Gets a multiline string by dialog
     *
     * @param parent       the parent component
     * @param title        the title
     * @param message      message
     * @param initialValue initial value
     * @return value or null
     */
    public static String getMultiLineStringByDialog(Component parent, String title, String message, String initialValue) {
        JTextArea area = new JTextArea(5, 10);
        area.setText(initialValue);
        JScrollPane scrollPane = new JScrollPane(area);
        int result = JOptionPane.showOptionDialog(
                parent,
                new Object[]{message, scrollPane},
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            return area.getText();
        }
        return null;
    }

    /**
     * Gets an integer by dialog
     *
     * @param parent       parent component
     * @param title        title
     * @param message      message
     * @param initialValue initial value
     * @param min          minimum value
     * @param max          maximum value
     * @return the selected integer or null if cancelled
     */
    public static Integer getIntegerByDialog(Component parent, String title, String message, int initialValue, int min, int max) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(initialValue, min, max, 1));
        int result = JOptionPane.showOptionDialog(
                parent,
                new Object[]{message, spinner},
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            return ((SpinnerNumberModel) spinner.getModel()).getNumber().intValue();
        }
        return null;
    }

    /**
     * Continuously asks for an unique string
     *
     * @param parent       parent component
     * @param message      the message
     * @param initialValue the initial string
     * @param exists       function that returns true if the value exists
     * @return unique string or null if canceled
     */
    public static String getUniqueStringByDialog(Component parent, String message, String initialValue, Predicate<String> exists) {
        if (initialValue != null)
            initialValue = StringUtils.makeUniqueString(initialValue, " ", exists);
        String value = null;
        while (value == null) {
            String newValue = JOptionPane.showInputDialog(parent, message, initialValue);
            if (newValue == null || newValue.trim().isEmpty())
                return null;
            if (exists.test(newValue))
                continue;
            value = newValue;
        }
        return value;
    }

    /**
     * Returns a fill color for {@link JIPipeNodeInfo}
     *
     * @param info the algorithm type
     * @return the fill color
     */
    public static Color getFillColorFor(JIPipeNodeInfo info) {
        if (DARK_THEME)
            return info.getCategory().getDarkFillColor();
        else
            return info.getCategory().getFillColor();
    }

    /**
     * Returns a border color for {@link JIPipeNodeInfo}
     *
     * @param info the algorithm type
     * @return the border color
     */
    public static Color getBorderColorFor(JIPipeNodeInfo info) {
        if (DARK_THEME)
            return info.getCategory().getDarkBorderColor();
        else
            return info.getCategory().getBorderColor();
    }

    /**
     * Makes a toggle button readonly without greying it out
     *
     * @param toggleButton toggle button
     */
    public static void makeToggleReadonly(JToggleButton toggleButton) {
        toggleButton.addActionListener(e -> toggleButton.setSelected(!toggleButton.isSelected()));
    }

    /**
     * Creates a readonly text field
     *
     * @param value text
     * @return textfield
     */
    public static JTextField makeReadonlyTextField(String value) {
        JTextField textField = new JTextField();
        textField.setText(value);
        textField.setEditable(false);
        return textField;
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
     * Creates a readonly text pane (that can do HTML)
     *
     * @param text text
     * @return text area
     */
    public static JTextPane makeReadonlyTextPane(String text) {
        JTextPane textPane = new JTextPane();
        textPane.setBorder(BorderFactory.createEtchedBorder());
        textPane.setEditable(false);
        textPane.setContentType("text/html");
        textPane.setText(text);
        return textPane;
    }

    /**
     * Creates a readonly text pane (that can do HTML)
     *
     * @param text text
     * @return text area
     */
    public static JTextPane makeBorderlessReadonlyTextPane(String text) {
        JTextPane textPane = new JTextPane();
        textPane.setBorder(BorderFactory.createEtchedBorder());
        textPane.setEditable(false);
        textPane.setContentType("text/html");
        textPane.setText(text);
        textPane.setBorder(null);
        return textPane;
    }

    /**
     * Creates a readonly text field
     *
     * @param value text
     * @return textfield
     */
    public static JTextField makeReadonlyBorderlessTextField(String value) {
        JTextField textField = new JTextField();
        textField.setText(value);
        textField.setBorder(null);
        textField.setOpaque(false);
        textField.setEditable(false);
        return textField;
    }

    /**
     * Creates a readonly text area
     *
     * @param text text
     * @return text area
     */
    public static JTextArea makeReadonlyBorderlessTextArea(String text) {
        JTextArea textArea = new JTextArea();
        textArea.setBorder(null);
        textArea.setOpaque(false);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(text);
        return textArea;
    }

    /**
     * Creates a {@link JTextPane} that displays Markdown content
     *
     * @param document the markdown document
     * @param css      style sheets
     * @return text pane
     */
    public static JTextPane makeMarkdownReader(MarkdownDocument document, String[] css) {
        JTextPane content = new JTextPane();
        HTMLEditorKit kit = new HTMLEditorKit();
        for (String rule : css) {
            kit.getStyleSheet().addRule(rule);
        }
        content.setEditorKit(kit);
        content.setContentType("text/html");
        content.setText(document.getRenderedHTML());
        return content;
    }

    /**
     * Fits the row heights of a {@link JTable}
     *
     * @param table the table
     */
    public static void fitRowHeights(JTable table) {
        for (int row = 0; row < table.getRowCount(); row++) {
            int rowHeight = table.getRowHeight();

            for (int column = 0; column < table.getColumnCount(); column++) {
                Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
                rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
            }

            table.setRowHeight(row, rowHeight);
        }
    }

    /**
     * Creates a hierarchy of menus based on the menu paths vector
     *
     * @param rootMenu  the root menu
     * @param menuPaths strings that have menu items separated by newlines
     * @return map from menu path to submenu
     */
    public static Map<String, JMenu> createMenuTree(JMenu rootMenu, Set<String> menuPaths) {
        Set<String> decomposedPaths = new HashSet<>();
        for (String menuPath : menuPaths) {
            String[] components = menuPath.split("\n");
            String path = null;
            for (String component : components) {
                if (path == null)
                    path = component;
                else
                    path += "\n" + component;
                decomposedPaths.add(path);
            }
        }

        Map<String, JMenu> result = new HashMap<>();
        List<String> sortedMenuPaths = decomposedPaths.stream().sorted().collect(Collectors.toList());
        result.put("", rootMenu);
        for (String menuPath : sortedMenuPaths) {
            if (menuPath != null && !menuPath.isEmpty()) {
                if (!menuPath.contains("\n")) {
                    JMenu menu = new JMenu(menuPath);
                    rootMenu.add(menu);
                    result.put(menuPath, menu);
                } else {
                    int lastNewLine = menuPath.lastIndexOf("\n");
                    String parentMenuPath = menuPath.substring(0, lastNewLine);
                    String lastComponent = menuPath.substring(lastNewLine + 1);
                    JMenu parentMenu = result.get(parentMenuPath);
                    JMenu menu = new JMenu(lastComponent);
                    parentMenu.add(menu);
                    result.put(menuPath, menu);
                }
            }
        }
        return result;
    }

    /**
     * Makes a {@link JDialog} close when the escape key is hit
     *
     * @param dialog the dialog
     */
    public static void addEscapeListener(final JDialog dialog) {
        ActionListener escListener = e -> dialog.setVisible(false);
        dialog.getRootPane().registerKeyboardAction(escListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

    }

    /**
     * Makes a label that contains a URL and opens the URL when clicked
     *
     * @param url the URL
     * @return the label
     */
    public static JLabel makeURLLabel(String url) {
        JLabel label = new JLabel("<html><a href=\"" + url + "\">" + url + "</a></html>");
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (URISyntaxException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        return label;
    }


    /**
     * Installs an extension menu
     *
     * @param workbench      the workbench
     * @param targetMenu     the menu
     * @param targetMenuType the menu type
     * @param withSeparator  if a separator should be prepended if items are installed
     */
    public static void installMenuExtension(JIPipeWorkbench workbench, JMenu targetMenu, MenuTarget targetMenuType, boolean withSeparator) {
        List<MenuExtension> extensions = JIPipe.getCustomMenus()
                .getMenuExtensionsTargeting(targetMenuType, workbench);
        if (!extensions.isEmpty()) {
            if (withSeparator)
                targetMenu.addSeparator();
            for (Map.Entry<String, JMenu> entry : createMenuTree(targetMenu, extensions.stream()
                    .map(MenuExtension::getMenuPath).collect(Collectors.toSet())).entrySet()) {
                for (MenuExtension extension : extensions) {
                    if (StringUtils.getCleanedMenuPath(entry.getKey()).equals(StringUtils.getCleanedMenuPath(extension.getMenuPath()))) {
                        entry.getValue().add(extension);
                    }
                }
            }
        }
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
            IJ.handleException(e);
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
            IJ.handleException(e);
        }
    }

    public static boolean confirmResetParameters(JIPipeWorkbench parent, String title) {
        return JOptionPane.showConfirmDialog(parent.getWindow(),
                "This will reset most of the properties. Continue?",
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static void showConnectionErrorMessage(Component parent, JIPipeDataSlot source, JIPipeDataSlot target) {
        if (!JIPipe.getDataTypes().isConvertible(source.getAcceptedDataType(), target.getAcceptedDataType())) {
            JOptionPane.showMessageDialog(parent,
                    String.format("Unable to convert data type '%s' to '%s'!\nPlease refer to the data type compendium (top right [?] button) for info about which data types are compatible.", JIPipeDataInfo.getInstance(source.getAcceptedDataType()).getName(), JIPipeDataInfo.getInstance(target.getAcceptedDataType())),
                    "Unable to connect slots",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent,
                    "The connection is not valid. Please check if it a loop/cycle.",
                    "Unable to connect slots",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
