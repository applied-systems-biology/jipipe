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
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Ints;
import ij.IJ;
import org.apache.commons.lang3.SystemUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.registries.JIPipeSettingsRegistry;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.JIPipeValidityReportUI;
import org.hkijena.jipipe.ui.components.UserFriendlyErrorUI;
import org.hkijena.jipipe.ui.components.html.HTMLEditor;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.ui.notifications.GenericNotificationInboxUI;
import org.hkijena.jipipe.ui.theme.JIPipeUITheme;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.ui.ListSelectionMode;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;
import org.jdesktop.swingx.JXTable;
import org.scijava.Disposable;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utils for UI
 */
public class UIUtils {

    public static final FileNameExtensionFilter EXTENSION_FILTER_CSV = new FileNameExtensionFilter("CSV table (*.csv)", "csv");

    public static final FileNameExtensionFilter EXTENSION_FILTER_TSV = new FileNameExtensionFilter("TSV table (*.tsv)", "tsv");
    public static final FileNameExtensionFilter EXTENSION_FILTER_XLSX = new FileNameExtensionFilter("Excel table (*.xlsx)", "xlsx");
    public static final FileNameExtensionFilter EXTENSION_FILTER_PNG = new FileNameExtensionFilter("PNG image (*.png)", "png");
    public static final FileNameExtensionFilter EXTENSION_FILTER_IMAGEIO_IMAGES = new FileNameExtensionFilter("Image file (*.png, *.jpg, *.jpeg, *.bmp)", "png", "jpg", "jpeg", "bmp");
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
    public static final FileNameExtensionFilter EXTENSION_FILTER_JSON = new FileNameExtensionFilter("JSON file (*.json)", "json");
    public static final FileNameExtensionFilter EXTENSION_FILTER_TXT = new FileNameExtensionFilter("Text file (*.txt)", "txt", "log");
    public static final FileNameExtensionFilter EXTENSION_FILTER_ZIP = new FileNameExtensionFilter("ZIP file (*.zip)", "zip");
    public static final FileNameExtensionFilter EXTENSION_FILTER_ROI_ZIP = new FileNameExtensionFilter("ImageJ ROIs (*.zip)", "zip");
    public static final FileNameExtensionFilter EXTENSION_FILTER_ROI = new FileNameExtensionFilter("ImageJ ROI (*.roi)", "roi");
    public static final FileNameExtensionFilter EXTENSION_FILTER_ROIS = new FileNameExtensionFilter("ImageJ ROI (*.roi, *.zip)", "roi", "zip");
    public static final FileNameExtensionFilter EXTENSION_FILTER_AVI = new FileNameExtensionFilter("Video file (*.avi)", "avi");
    public static final Insets UI_PADDING = new Insets(4, 4, 4, 4);
    public static final Map<String, ImageIcon> ICON_FROM_RESOURCES_CACHE = new HashMap<>();

    public static final Map<String, ImageIcon> ICON_INVERTED_FROM_RESOURCES_CACHE = new HashMap<>();
    public static boolean DARK_THEME = false;
    private static Theme RSYNTAX_THEME_DEFAULT;
    private static Theme RSYNTAX_THEME_DARK;

    public static JLabel createInfoLabel(String text, String subtext) {
        JLabel label = new JLabel("<html><strong>" + text + "</strong><br/>" + subtext + "</html>",
                UIUtils.getIcon32FromResources("info.png"), JLabel.LEFT);
        label.setAlignmentX(0f);
        label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return label;
    }

    public static void registerHyperlinkHandler(JTextPane content) {
        content.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (e.getDescription() != null && e.getDescription().startsWith("#")) {
                    // Not supported
                } else {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (Exception e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                }
            }
        });
    }

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
     * packAll() for a data table (with a limit)
     *
     * @param table the table
     */
    public static void packDataTable(JXTable table) {
        int max = Math.max(-1, GeneralDataSettings.getInstance().getMaxTableColumnSize());
        for (int c = 0; c < table.getColumnCount(); c++) {
            try {
                table.packColumn(c, -1, max);
            } catch (Throwable e) {
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
                if (RSYNTAX_THEME_DARK == null) {
                    RSYNTAX_THEME_DARK = Theme.load(ResourceUtils.class.getResourceAsStream(
                            "/org/hkijena/jipipe/rsyntaxtextarea/themes/dark.xml"));
                }
                RSYNTAX_THEME_DARK.apply(textArea);
            } catch (IOException ioe) { // Never happens
                ioe.printStackTrace();
            }
        } else {
            try {
                if (RSYNTAX_THEME_DEFAULT == null) {
                    RSYNTAX_THEME_DEFAULT = Theme.load(ResourceUtils.class.getResourceAsStream(
                            "/org/hkijena/jipipe/rsyntaxtextarea/themes/default.xml"));
                }
                RSYNTAX_THEME_DEFAULT.apply(textArea);
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
     * Adds an existing popup menu to a button
     * Adds a function that is run before the popup is shown
     *
     * @param target         target button
     * @param popupMenu      the popup menu
     * @param reloadFunction the function that is run before showing the popup
     * @return the popup menu
     */
    public static JPopupMenu addReloadableRightClickPopupMenuToComponent(AbstractButton target, JPopupMenu popupMenu, Runnable reloadFunction) {
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                    reloadFunction.run();
                    popupMenu.revalidate();
                    popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
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
     * Adds an existing popup menu to a button
     * Adds a function that is run before the popup is shown
     *
     * @param target target button
     * @return the popup menu
     */
    public static JPopupMenu addRightClickPopupMenuToComponent(AbstractButton target) {
        JPopupMenu popupMenu = new JPopupMenu();
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                    popupMenu.revalidate();
                    popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        target.addActionListener(e -> {
            try {
                if (target.isDisplayable() && MouseInfo.getPointerInfo().getLocation().x < target.getLocationOnScreen().x
                        || MouseInfo.getPointerInfo().getLocation().x > target.getLocationOnScreen().x + target.getWidth()
                        || MouseInfo.getPointerInfo().getLocation().y < target.getLocationOnScreen().y
                        || MouseInfo.getPointerInfo().getLocation().y > target.getLocationOnScreen().y + target.getHeight()) {
                    popupMenu.revalidate();
                    popupMenu.show(target, 0, target.getHeight());
                }
            } catch (IllegalComponentStateException e1) {
            }
        });
        return popupMenu;
    }

    /**
     * Adds an existing popup menu to a button
     * Adds a function that is run before the popup is shown
     *
     * @param target target button
     * @return the popup menu
     */
    public static JPopupMenu addRightClickPopupMenuToComponent(AbstractButton target, JPopupMenu popupMenu) {
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                    popupMenu.revalidate();
                    popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        target.addActionListener(e -> {
            try {
                if (target.isDisplayable() && MouseInfo.getPointerInfo().getLocation().x < target.getLocationOnScreen().x
                        || MouseInfo.getPointerInfo().getLocation().x > target.getLocationOnScreen().x + target.getWidth()
                        || MouseInfo.getPointerInfo().getLocation().y < target.getLocationOnScreen().y
                        || MouseInfo.getPointerInfo().getLocation().y > target.getLocationOnScreen().y + target.getHeight()) {
                    popupMenu.revalidate();
                    popupMenu.show(target, 0, target.getHeight());
                }
            } catch (IllegalComponentStateException e1) {
            }
        });
        return popupMenu;
    }

    public static JSeparator createVerticalSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setMaximumSize(new Dimension(1, Integer.MAX_VALUE));
        return separator;
    }

    /**
     * Returns an icon from JIPipe resources
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static ImageIcon getIcon16FromResources(String iconName) {
        return getIconFromResources(iconName);
    }

    /**
     * Returns an icon from JIPipe resources
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static ImageIcon getIconInvertedFromResources(String iconName) {
        String path = "icons/" + iconName;
        ImageIcon icon = ICON_INVERTED_FROM_RESOURCES_CACHE.getOrDefault(path, null);
        if (icon == null) {
            icon = new ImageIcon(ResourceUtils.getPluginResourceInverted(path));
            ICON_INVERTED_FROM_RESOURCES_CACHE.put(path, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static ImageIcon getIconInverted32FromResources(String iconName) {
        String path = "icons-32/" + iconName;
        ImageIcon icon = ICON_INVERTED_FROM_RESOURCES_CACHE.getOrDefault(path, null);
        if (icon == null) {
            icon = new ImageIcon(ResourceUtils.getPluginResourceInverted(path));
            ICON_INVERTED_FROM_RESOURCES_CACHE.put(path, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static ImageIcon getIconInverted12FromResources(String iconName) {
        String path = "icons-12/" + iconName;
        ImageIcon icon = ICON_INVERTED_FROM_RESOURCES_CACHE.getOrDefault(path, null);
        if (icon == null) {
            icon = new ImageIcon(ResourceUtils.getPluginResourceInverted(path));
            ICON_INVERTED_FROM_RESOURCES_CACHE.put(path, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
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
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
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
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static ImageIcon getIcon12FromResources(String iconName) {
        String path = "icons-12/" + iconName;
        ImageIcon icon = ICON_FROM_RESOURCES_CACHE.getOrDefault(path, null);
        if (icon == null) {
            icon = new ImageIcon(ResourceUtils.getPluginResource(path));
            ICON_FROM_RESOURCES_CACHE.put(path, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
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
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
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
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
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
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
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
    public static SolidColorIcon getIconFromColor(Color color) {
        return new SolidColorIcon(16, 16, color);
    }

    /**
     * Makes a button flat
     *
     * @param component the button
     */
    public static void setStandardButtonBorder(AbstractButton component) {
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
    public static void setStandardButtonBorder(AbstractButton component, Color borderColor, int top, int left, int right, int bottom) {
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

    public static String getAWTWindowTitle(Window window) {
        String windowTitle;
        if (window instanceof JFrame) {
            windowTitle = ((JFrame) window).getTitle();
        } else if (window instanceof JDialog) {
            windowTitle = ((JDialog) window).getTitle();
        } else {
            windowTitle = "Unnamed window";
        }
        return windowTitle;
    }

    /**
     * Makes a button flat and have a height of 25px
     *
     * @param component the button
     */
    public static void makeFlat(AbstractButton component) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
//        component.setPreferredSize(new Dimension(component.getPreferredSize().width, 25));
//        component.setMinimumSize(new Dimension(25, 25));
//        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        Border margin = new EmptyBorder(3, 3, 3, 3);
//        Border compound = new CompoundBorder(BorderFactory.createEtchedBorder(), margin);
        component.setBorder(margin);
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

    public static JLabel createJLabel(String text, Icon icon) {
        return createJLabel(text, icon, 12);
    }

    public static JLabel createJLabel(String text, int fontSize) {
        return createJLabel(text, null, fontSize);
    }

    public static JLabel createJLabel(String text, Icon icon, int fontSize) {
        JLabel label = new JLabel(text);
        label.setIcon(icon);
        label.setFont(new Font(Font.DIALOG, Font.PLAIN, fontSize));
        return label;
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
     * Checks if a file exists and asks the user if it should be overwritten
     *
     * @param parent the parent component
     * @param path   the path. can be null, will return false
     * @param title  the title
     * @return true if the path is OK or the user confirmed, otherwise false
     */
    public static boolean checkAndAskIfFileExists(Component parent, Path path, String title) {
        if (path != null) {
            if (Files.isRegularFile(path)) {
                return JOptionPane.showConfirmDialog(parent,
                        "The file '" + path + "' already exists. Do you want to overwrite the file?",
                        title,
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) != JOptionPane.NO_OPTION;
            }
            return true;
        }
        return false;
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

    public static void openNotificationsDialog(JIPipeWorkbench workbench, Component parent, JIPipeNotificationInbox notifications, String title, String infoText, boolean autoClose) {

        if (autoClose && notifications.isEmpty()) {
            return;
        }

        JPanel contentPanel = new JPanel(new BorderLayout(8, 8));

        GenericNotificationInboxUI inboxUI = new GenericNotificationInboxUI(workbench, notifications);
        contentPanel.add(inboxUI, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new GridBagLayout());
        messagePanel.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));
        messagePanel.add(new JLabel(UIUtils.getIcon32FromResources("dialog-warning.png")),
                new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        messagePanel.add(makeReadonlyBorderlessTextArea(infoText),
                new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
        contentPanel.add(messagePanel, BorderLayout.NORTH);

        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setContentPane(contentPanel);
        dialog.setModal(false);
        dialog.pack();
        dialog.setSize(new Dimension(800, 600));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        if (autoClose) {
            notifications.getEventBus().register(new Object() {
                @Subscribe
                public void onUpdated(JIPipeNotificationInbox.UpdatedEvent event) {
                    if (notifications.isEmpty()) {
                        dialog.setVisible(false);
                    }
                }
            });
        }
    }

    /**
     * Opens a dialog showing a validity report
     *
     * @param parent   the parent component
     * @param report   the report
     * @param title    the title
     * @param infoText the info text
     * @param modal    make the dialog modal
     */
    public static void openValidityReportDialog(Component parent, JIPipeIssueReport report, String title, String infoText, boolean modal) {
        JPanel contentPanel = new JPanel(new BorderLayout(8, 8));

        JIPipeValidityReportUI ui = new JIPipeValidityReportUI(false);
        ui.setReport(report);
        contentPanel.add(ui, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new GridBagLayout());
        messagePanel.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));
        messagePanel.add(new JLabel(UIUtils.getIcon32FromResources("dialog-error.png")),
                new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        messagePanel.add(makeReadonlyBorderlessTextArea(infoText),
                new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
        contentPanel.add(messagePanel, BorderLayout.NORTH);

        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setContentPane(contentPanel);
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
     * Shows a component in a JIPipe-styled dialog with OK and Cancel button
     *
     * @param parent    the parent component
     * @param component the component
     * @param title     the title
     * @return true of OK was pressed
     */
    public static boolean showOKCancelDialog(Component parent, Component component, String title) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        dialog.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(component, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        AtomicBoolean clickedOK = new AtomicBoolean(false);

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            clickedOK.set(false);
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("OK", UIUtils.getIconFromResources("actions/checkmark.png"));
        confirmButton.addActionListener(e -> {
            clickedOK.set(true);
            dialog.setVisible(false);
        });
        buttonPanel.add(confirmButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setTitle(title);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(800, 600));
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(parent));
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);

        return clickedOK.get();
    }

    /**
     * Gets a formatted HTML string by dialog
     *
     * @param workbench    the workbench
     * @param parent       the parent component
     * @param title        the title
     * @param message      message
     * @param initialValue initial value
     * @return value or null
     */
    public static HTMLText getHTMLByDialog(JIPipeWorkbench workbench, Component parent, String title, String message, HTMLText initialValue) {
        HTMLEditor area = new HTMLEditor(workbench, HTMLEditor.Mode.Full, HTMLEditor.WITH_SCROLL_BAR);
        area.setText(initialValue.getHtml());

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title);
        dialog.setSize(1024, 768);
        dialog.setLayout(new BorderLayout());
        if (!StringUtils.isNullOrEmpty(message)) {
            dialog.add(new JLabel(message), BorderLayout.NORTH);
        }
        dialog.add(area, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        AtomicBoolean confirmation = new AtomicBoolean(false);

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            confirmation.set(false);
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("OK", UIUtils.getIconFromResources("actions/ok.png"));
        confirmButton.addActionListener(e -> {
            confirmation.set(true);
            dialog.setVisible(false);
        });
        buttonPanel.add(confirmButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.revalidate();
        dialog.repaint();
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);

        if (confirmation.get()) {
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
        JTextArea area = new JTextArea();
        area.setText(initialValue);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title);
        dialog.setSize(1024, 768);
        dialog.setLayout(new BorderLayout());
        if (!StringUtils.isNullOrEmpty(message)) {
            dialog.add(new JLabel(message), BorderLayout.NORTH);
        }
        dialog.add(area, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        AtomicBoolean confirmation = new AtomicBoolean(false);

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            confirmation.set(false);
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("OK", UIUtils.getIconFromResources("actions/ok.png"));
        confirmButton.addActionListener(e -> {
            confirmation.set(true);
            dialog.setVisible(false);
        });
        buttonPanel.add(confirmButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.revalidate();
        dialog.repaint();
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);

        if (confirmation.get()) {
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
    public static Optional<Integer> getIntegerByDialog(Component parent, String title, String message, int initialValue, int min, int max) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(initialValue, min, max, 1));
        int result = JOptionPane.showOptionDialog(
                parent,
                new Object[]{message, spinner},
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            return Optional.of(((SpinnerNumberModel) spinner.getModel()).getNumber().intValue());
        }
        return Optional.empty();
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
     * @return the selected double or null if cancelled
     */
    public static Optional<Double> getDoubleByDialog(Component parent, String title, String message, double initialValue, double min, double max) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(initialValue, min, max, 1));
        int result = JOptionPane.showOptionDialog(
                parent,
                new Object[]{message, spinner},
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            return Optional.of(((SpinnerNumberModel) spinner.getModel()).getNumber().doubleValue());
        }
        return Optional.empty();
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
     * Utility method for setting the font and color of a JTextPane. The
     * result is roughly equivalent to calling setFont(...) and
     * setForeground(...) on an AWT TextArea.
     */
    public static void setJTextPaneFont(JTextPane jtp, Font font, Color c) {
        // Start with the current input attributes for the JTextPane. This
        // should ensure that we do not wipe out any existing attributes
        // (such as alignment or other paragraph attributes) currently
        // set on the text area.
        MutableAttributeSet attrs = jtp.getInputAttributes();

        // Set the font family, size, and style, based on properties of
        // the Font object. Note that JTextPane supports a number of
        // character attributes beyond those supported by the Font class.
        // For example, underline, strike-through, super- and sub-script.
        StyleConstants.setFontFamily(attrs, font.getFamily());
        StyleConstants.setFontSize(attrs, font.getSize());
        StyleConstants.setItalic(attrs, (font.getStyle() & Font.ITALIC) != 0);
        StyleConstants.setBold(attrs, (font.getStyle() & Font.BOLD) != 0);

        // Set the font color
        StyleConstants.setForeground(attrs, c);

        // Retrieve the pane's document object
        StyledDocument doc = jtp.getStyledDocument();

        // Replace the style for the entire document. We exceed the length
        // of the document by 1 so that text entered at the end of the
        // document uses the attributes.
        doc.setCharacterAttributes(0, doc.getLength() + 1, attrs, false);
    }

    /**
     * Creates a readonly text pane (that can do HTML)
     *
     * @param text   text
     * @param opaque if the area should be opaque
     * @return text area
     */
    public static JTextPane makeBorderlessReadonlyTextPane(String text, boolean opaque) {
        JTextPane textPane = new JTextPane();
        textPane.setBorder(BorderFactory.createEtchedBorder());
        textPane.setEditable(false);
        textPane.setOpaque(opaque);
        textPane.setContentType("text/html");
        textPane.setText(text);
        textPane.setBorder(null);
        addHyperlinkListener(textPane);
        return textPane;
    }

    private static void addHyperlinkListener(JTextPane textPane) {
        textPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
        });
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

    public static JPanel boxVertical(Component... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (Component component : components) {
            if (component != null) {
                panel.add(component);
            }
        }
        return panel;
    }

    public static JPanel boxHorizontal(Component... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        for (Component component : components) {
            if (component != null) {
                panel.add(component);
            }
        }
        return panel;
    }

    public static JPanel gridVertical(Component... components) {
        JPanel panel = new JPanel(new GridBagLayout());
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            if (component != null) {
                panel.add(component, new GridBagConstraints(0, i, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            }
        }
        return panel;
    }

    public static void setTreeExpandedState(JTree tree, boolean expanded) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getModel().getRoot();
        setNodeExpandedState(tree, node, expanded);
    }

    public static void setNodeExpandedState(JTree tree, DefaultMutableTreeNode node, boolean expanded) {
        ArrayList<DefaultMutableTreeNode> list = Collections.list(node.children());
        for (DefaultMutableTreeNode treeNode : list) {
            setNodeExpandedState(tree, treeNode, expanded);
        }
        if (!expanded && node.isRoot()) {
            return;
        }
        TreePath path = new TreePath(node.getPath());
        if (expanded) {
            tree.expandPath(path);
        } else {
            tree.collapsePath(path);
        }
    }

    public static JPanel gridHorizontal(Component... components) {
        JPanel panel = new JPanel(new GridBagLayout());
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            if (component != null) {
                panel.add(component, new GridBagConstraints(i, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
            }
        }
        return panel;
    }


    /**
     * Installs an extension menu
     *
     * @param workbench      the workbench
     * @param targetMenu     the menu
     * @param targetMenuType the menu type
     * @param withSeparator  if a separator should be prepended if items are installed
     */
    public static void installMenuExtension(JIPipeWorkbench workbench, JMenu targetMenu, JIPipeMenuExtensionTarget targetMenuType, boolean withSeparator) {
        List<JIPipeMenuExtension> extensions = JIPipe.getCustomMenus()
                .getMenuExtensionsTargeting(targetMenuType, workbench);
        if (!extensions.isEmpty()) {
            if (withSeparator)
                targetMenu.addSeparator();
            for (Map.Entry<String, JMenu> entry : createMenuTree(targetMenu, extensions.stream()
                    .map(JIPipeMenuExtension::getMenuPath).collect(Collectors.toSet())).entrySet()) {
                for (JIPipeMenuExtension extension : extensions) {
                    if (StringUtils.getCleanedMenuPath(entry.getKey()).equals(StringUtils.getCleanedMenuPath(extension.getMenuPath()))) {
                        entry.getValue().add(extension);
                    }
                }
            }
        }
    }

    /**
     * Copies the string to the clipboard
     *
     * @param string the string
     */
    public static void copyToClipboard(String string) {
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
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

    /**
     * Adds a separator if there is at least one component in the menu and the last item is not a separator
     *
     * @param menu the menu
     */
    public static void addSeparatorIfNeeded(JPopupMenu menu) {
        if (menu.getComponentCount() == 0)
            return;
        Component lastComponent = menu.getComponent(menu.getComponentCount() - 1);
        if (lastComponent instanceof JSeparator)
            return;
        menu.addSeparator();
    }

    /**
     * Adds a separator if there is at least one component in the menu and the last item is not a separator
     *
     * @param menu the menu
     */
    public static void addSeparatorIfNeeded(JMenu menu) {
        if (menu.getComponentCount() == 0)
            return;
        Component lastComponent = menu.getComponent(menu.getComponentCount() - 1);
        if (lastComponent instanceof JSeparator)
            return;
        menu.addSeparator();
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

    public static void invokeScrollToTop(JScrollPane scrollPane) {
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(0);
        });
    }

    public static void redirectDragEvents(Component component, Component target) {
        component.setFocusable(false);
        DragThroughMouseListener listener = new DragThroughMouseListener(component, target);
        component.addMouseListener(listener);
        component.addMouseMotionListener(listener);
    }

    public static JMenuItem createMenuItem(String label, String description, Icon icon, Runnable action) {
        JMenuItem item = new JMenuItem(label, icon);
        item.setToolTipText(description);
        item.addActionListener(e -> action.run());
        return item;
    }

    public static void drawStringVerticallyCentered(Graphics2D g2, String text, int x, int y, FontMetrics fontMetrics) {
        int metricHeight = fontMetrics.getAscent() - fontMetrics.getLeading();
        g2.drawString(text, x, y + metricHeight / 2);
    }

    public static void removeAllWithDispose(JComponent component) {
        for (Component child : component.getComponents()) {
            if (child instanceof Disposable) {
                ((Disposable) child).dispose();
            }
        }
        component.removeAll();
    }

    public static Map<?, ?> getDesktopRenderingHints() {
        Map<?, ?> result = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (result == null) {
            return Collections.emptyMap();
        } else {
            return result;
        }
    }

    public static JButton createFlatButton(String text, Icon icon, Runnable function) {
        JButton button = new JButton(text, icon);
        UIUtils.setStandardButtonBorder(button);
        button.addActionListener(e -> function.run());
        return button;
    }

    public static class DragThroughMouseListener implements MouseListener, MouseMotionListener {
        private final Component component;
        private final Component target;

        public DragThroughMouseListener(Component component, Component target) {
            this.component = component;
            this.target = target;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            target.dispatchEvent(SwingUtilities.convertMouseEvent(component, e, target));
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            target.dispatchEvent(SwingUtilities.convertMouseEvent(component, e, target));
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            target.dispatchEvent(SwingUtilities.convertMouseEvent(component, e, target));
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            target.dispatchEvent(SwingUtilities.convertMouseEvent(component, e, target));
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            target.dispatchEvent(SwingUtilities.convertMouseEvent(component, e, target));
        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }
}
