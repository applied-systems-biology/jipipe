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

package org.hkijena.jipipe.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.primitives.Ints;
import ij.IJ;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import org.apache.commons.lang3.SystemUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.registries.JIPipeApplicationSettingsRegistry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPathEditorComponent;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopUserFriendlyErrorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopValidityReportUI;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopHTMLEditor;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.window.JIPipeDesktopAlwaysOnTopToggle;
import org.hkijena.jipipe.desktop.commons.notifications.JIPipeDesktopGenericNotificationInboxUI;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralDataApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.ui.ListSelectionMode;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;
import org.jdesktop.swingx.JXTable;
import org.scijava.Disposable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
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
    public static final FileNameExtensionFilter EXTENSION_FILTER_OME_TIFF = new FileNameExtensionFilter("OME TIFF image (*.ome.tif, *.ome.tiff)", "ome.tif", "ome.tiff");
    public static final FileNameExtensionFilter EXTENSION_FILTER_JIP = new FileNameExtensionFilter("JIPipe project (*.jip)", "jip");
    public static final FileNameExtensionFilter EXTENSION_FILTER_JIPE = new FileNameExtensionFilter("JIPipe extension (*.jipe)", "jipe");
    public static final FileNameExtensionFilter EXTENSION_FILTER_JIPC = new FileNameExtensionFilter("JIPipe compartment (*.jipc)", "jipc");
    public static final FileNameExtensionFilter EXTENSION_FILTER_JSON = new FileNameExtensionFilter("JSON file (*.json)", "json");
    public static final FileNameExtensionFilter EXTENSION_FILTER_TXT = new FileNameExtensionFilter("Text file (*.txt)", "txt", "log");
    public static final FileNameExtensionFilter EXTENSION_FILTER_ZIP = new FileNameExtensionFilter("ZIP file (*.zip)", "zip");
    public static final FileNameExtensionFilter EXTENSION_FILTER_TAR_GZ = new FileNameExtensionFilter("GZipped TAR file (*.tar.gz)", "tar.gz");
    public static final FileNameExtensionFilter EXTENSION_FILTER_ARCHIVE = new FileNameExtensionFilter("Archive (*.zip, *.tar.gz)", "zip", "gz");
    public static final FileNameExtensionFilter EXTENSION_FILTER_ROI_ZIP = new FileNameExtensionFilter("ImageJ ROIs (*.zip)", "zip");
    public static final FileNameExtensionFilter EXTENSION_FILTER_ROI = new FileNameExtensionFilter("ImageJ ROI (*.roi)", "roi");
    public static final FileNameExtensionFilter EXTENSION_FILTER_ROIS = new FileNameExtensionFilter("ImageJ ROI (*.roi, *.zip)", "roi", "zip");
    public static final FileNameExtensionFilter EXTENSION_FILTER_AVI = new FileNameExtensionFilter("Video file (*.avi)", "avi");
    public static final FileNameExtensionFilter EXTENSION_FILTER_HDF5 = new FileNameExtensionFilter("HDF5 data (*.hdf5, *.h5)", "hdf5", "h5");
    public static final FileNameExtensionFilter EXTENSION_FILTER_ZARR_ZIP = new FileNameExtensionFilter("ZARR ZIP (*.zarr.zip)", "zarr.zip");
    public static final Insets UI_PADDING = new Insets(4, 4, 4, 4);
    public static final Map<String, ImageIcon> ICON_FROM_RESOURCES_CACHE = new HashMap<>();
    public static final Map<String, ImageIcon> ICON_INVERTED_FROM_RESOURCES_CACHE = new HashMap<>();
    public static final Map<String, BufferedImage> IMAGE_FROM_RESOURCES_CACHE = new HashMap<>();
    public static final JMenuItem MENU_ITEM_SEPARATOR = null;
    public static final Color COLOR_ERROR = new Color(0xa51d2d);
    public static final Color COLOR_SUCCESS = new Color(0x5CB85C);
    public static boolean DARK_THEME = false;
    private static Theme RSYNTAX_THEME_DEFAULT;
    private static Theme RSYNTAX_THEME_DARK;
    private static Border CONTROL_BORDER;
    private static Border PANEL_BORDER;
    private static Border CONTROL_ERROR_BORDER;

    public static void addPanningToScrollPane(JScrollPane scrollPane) {
        JViewport viewport = scrollPane.getViewport();

        MouseAdapter panListener = new MouseAdapter() {
            private Point origin;

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    origin = e.getPoint();
                    // Change cursor for feedback (optional)
                    viewport.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    origin = null;
                    viewport.setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e) && origin != null) {
                    Point current = e.getPoint();
                    Point viewPos = viewport.getViewPosition();
                    // Calculate the new view position by adding the difference
                    viewPos.translate(origin.x - current.x, origin.y - current.y);

                    // Optionally, check for bounds if needed:
                    Component view = viewport.getView();
                    int maxX = view.getWidth() - viewport.getWidth();
                    int maxY = view.getHeight() - viewport.getHeight();
                    viewPos.x = Math.max(0, Math.min(viewPos.x, maxX));
                    viewPos.y = Math.max(0, Math.min(viewPos.y, maxY));

                    viewport.setViewPosition(viewPos);
                    // Update the origin for the next drag event
                    origin = current;
                }
            }
        };

        viewport.addMouseListener(panListener);
        viewport.addMouseMotionListener(panListener);
    }

    public static void rebuildMenu(JPopupMenu menu, List<Component> items) {
        menu.removeAll();
        if (items.isEmpty()) {
            return;
        }
        items = new ArrayList<>(items);
        while (items.get(items.size() - 1) == null) {
            items.remove(items.size() - 1);
        }
        boolean canAddSeparator = false;
        for (Component item : items) {
            if (item == null) {
                if (canAddSeparator) {
                    menu.addSeparator();
                    canAddSeparator = false;
                }
            } else {
                menu.add(item);
                canAddSeparator = true;
            }
        }
    }

    public static void rebuildMenu(JMenu menu, List<Component> items) {
        menu.removeAll();
        if (items.isEmpty()) {
            return;
        }
        items = new ArrayList<>(items);
        while (items.get(items.size() - 1) == null) {
            items.remove(items.size() - 1);
        }
        boolean canAddSeparator = false;
        for (Component item : items) {
            if (item == null) {
                if (canAddSeparator) {
                    menu.addSeparator();
                    canAddSeparator = false;
                }
            } else {
                menu.add(item);
                canAddSeparator = true;
            }
        }
    }

    public static JLabel createInfoLabel(String text, String subtext) {
        JLabel label = new JLabel("<html><strong>" + text + "</strong><br/>" + subtext + "</html>",
                UIUtils.getIcon32FromResources("info.png"), JLabel.LEFT);
        label.setAlignmentX(0f);
        label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return label;
    }

    public static JLabel createInfoLabel(String text, String subtext, Icon icon) {
        JLabel label = new JLabel("<html><strong>" + text + "</strong><br/>" + subtext + "</html>",
                icon, JLabel.LEFT);
        label.setAlignmentX(0f);
        label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return label;
    }

    public static Border createControlErrorBorder() {
        if (CONTROL_ERROR_BORDER == null) {
            CONTROL_ERROR_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                    new RoundedLineBorder(COLOR_ERROR, 1, 5));
        }
        return CONTROL_ERROR_BORDER;
    }

    public static Color getControlBorderColor() {
        if (!DARK_THEME) {
            return JIPipeDesktopModernMetalTheme.MEDIUM_GRAY;
        } else {
            return Color.DARK_GRAY;
        }
    }

    public static Border createControlBorder() {
        if (CONTROL_BORDER == null) {
            if (!DARK_THEME) {
                CONTROL_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                        new RoundedLineBorder(JIPipeDesktopModernMetalTheme.MEDIUM_GRAY, 1, 5));
            } else {
                CONTROL_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                        new RoundedLineBorder(Color.DARK_GRAY, 1, 5));
            }
        }
        return CONTROL_BORDER;
    }

    public static Border createControlBorder(Color color) {
        return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                new RoundedLineBorder(color, 1, 5));
    }

    public static void addToStatusBarWithSeparator(JComponent target, JComponent control) {
        if (target.getComponentCount() > 0) {
            target.add(Box.createHorizontalStrut(4));
            target.add(createVerticalSeparator());
            target.add(Box.createHorizontalStrut(4));
        }
        target.add(control);
    }

    public static Border createPanelBorder() {
        if (PANEL_BORDER == null) {
            if (!DARK_THEME) {
                PANEL_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(JIPipeDesktopModernMetalTheme.MEDIUM_GRAY, 1),
                        BorderFactory.createEmptyBorder(1, 1, 1, 1));
            } else {
                PANEL_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                        BorderFactory.createEmptyBorder(1, 1, 1, 1));
            }
        }
        return PANEL_BORDER;
    }

    public static Border createPanelBorder(int left, int top, int right, int bottom) {
        if (!DARK_THEME) {
            return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(JIPipeDesktopModernMetalTheme.MEDIUM_GRAY, 1),
                    BorderFactory.createEmptyBorder(top, left, bottom, right));
        } else {
            return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                    BorderFactory.createEmptyBorder(top, left, bottom, right));
        }
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

    public static Image getJIPipeIcon128() {
        return UIUtils.getIcon128FromResources("jipipe.png").getImage();
    }

    public static void addBalloonToComponent(AbstractButton button, String text) {
        EdgedBalloonStyle style = new EdgedBalloonStyle(UIManager.getColor("TextField.background"), JIPipeDesktopModernMetalTheme.PRIMARY5);
        final BalloonTip balloonTip = new BalloonTip(
                button,
                new JLabel(text.startsWith("<html>") ? text : StringUtils.wordWrappedHTML(text, 100)),
                style,
                BalloonTip.Orientation.LEFT_ABOVE,
                BalloonTip.AttachLocation.ALIGNED,
                30, 10,
                true
        );
        balloonTip.setVisible(false);

        JButton closeButton = new JButton(UIUtils.getIconFromResources("actions/window-close.png"));
        closeButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        closeButton.setOpaque(false);

        balloonTip.setCloseButton(closeButton, false);
        button.addActionListener(e -> {
            balloonTip.refreshLocation();
            balloonTip.setVisible(true);
        });
    }

    public static JButton createPopupHelpButton(String text) {
        JButton helpButton = new JButton(UIUtils.getIconFromResources("actions/help.png"));
        UIUtils.makeButtonFlat25x25(helpButton);
        helpButton.addActionListener(e -> {
            MarkdownText document = new MarkdownText(text);
            JIPipeDesktopMarkdownReader.showDialog(document, false, "Info", SwingUtilities.getWindowAncestor(helpButton), false);
        });
        helpButton.setOpaque(false);
        return helpButton;
    }

    public static JButton createBalloonHelpButton(String text) {
        JButton helpButton = new JButton(UIUtils.getIconFromResources("actions/help.png"));
        UIUtils.makeButtonFlat25x25(helpButton);
        UIUtils.addBalloonToComponent(helpButton, text);
        helpButton.setOpaque(false);
        return helpButton;
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

    public static Color getIconBaseColor() {
        if (DARK_THEME) {
            return new Color(0xDFDFDF);
        } else {
            return new Color(0x333333);
        }
    }

    public static JSeparator createHorizontalFillingSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);

        // Force the separator to fill horizontally
        separator.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
        separator.setPreferredSize(new Dimension(50, 1)); // Default pref size
        separator.setAlignmentY(Component.CENTER_ALIGNMENT); // Critical: vertical centering

        return separator;
    }

    /**
     * packAll() for a data table (with a limit)
     *
     * @param table the table
     */
    public static void packDataTable(JXTable table) {
        int max = Math.max(-1, JIPipeGeneralDataApplicationSettings.getInstance().getMaxTableColumnSize());
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
        JIPipeDesktopUITheme theme = getThemeFromRawSettings();
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

    public static JIPipeDesktopUITheme getThemeFromRawSettings() {
        Path propertyFile = JIPipeApplicationSettingsRegistry.getPropertyFile();
        JIPipeDesktopUITheme theme = JIPipeDesktopUITheme.ModernLight;
        if (Files.exists(propertyFile)) {
            try {
                JsonNode node = JsonUtils.getObjectMapper().readValue(propertyFile.toFile(), JsonNode.class);
                JsonNode themeNode = node.path(JIPipeGeneralUIApplicationSettings.ID).path("theme");
                if (!themeNode.isMissingNode())
                    theme = JsonUtils.getObjectMapper().readerFor(JIPipeDesktopUITheme.class).readValue(themeNode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return theme;
    }

    public static BufferedImage getImageFromResources(String path) {
        BufferedImage image = IMAGE_FROM_RESOURCES_CACHE.getOrDefault(path, null);
        if (image == null) {
            try {
                image = ImageIO.read(ResourceUtils.getPluginResource(path));
                IMAGE_FROM_RESOURCES_CACHE.put(path, image);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return image;
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
    public static JPopupMenu addPopupMenuToButton(AbstractButton target) {
        return addPopupMenuToButton(target, new JPopupMenu());
    }

    /**
     * Adds an existing popup menu to a button
     *
     * @param target    target button
     * @param popupMenu the popup menu
     * @return the popup menu
     */
    public static JPopupMenu addPopupMenuToButton(AbstractButton target, JPopupMenu popupMenu) {
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
    public static JPopupMenu addReloadablePopupMenuToButton(AbstractButton target, JPopupMenu popupMenu, Runnable reloadFunction) {
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

    public static void addRightClickPopupMenuToJList(JList<?> target, JPopupMenu popupMenu, Runnable reloadFunction) {
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    e.consume();
                    int i = target.locationToIndex(e.getPoint());
                    if (i >= 0) {
                        if (target.getSelectedValuesList().isEmpty()) {
                            target.addSelectionInterval(i, i);
                        } else if (!Ints.contains(target.getSelectedIndices(), i)) {
                            target.clearSelection();
                            target.addSelectionInterval(i, i);
                        }
                    }
                    reloadFunction.run();
                    if (popupMenu.getComponentCount() > 0) {
                        popupMenu.show(target, e.getX(), e.getY());
                    }
                }
            }
        });
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
    public static JPopupMenu addReloadableRightClickPopupMenuToButton(AbstractButton target, JPopupMenu popupMenu, Runnable reloadFunction) {
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                    reloadFunction.run();
                    if (popupMenu.getComponentCount() > 0) {
                        popupMenu.revalidate();
                        popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
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
                    if (popupMenu.getComponentCount() > 0) {
                        popupMenu.revalidate();
                        popupMenu.show(target, 0, target.getHeight());
                    }
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
    public static JPopupMenu addRightClickPopupMenuToComponent(Component target) {
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
        return popupMenu;
    }

    /**
     * Adds an existing popup menu to a button
     * Adds a function that is run before the popup is shown
     *
     * @param target target button
     */
    public static void addRightClickPopupMenuToComponent(Component target, JPopupMenu popupMenu) {
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
    }

    /**
     * Adds an existing popup menu to a button
     * Adds a function that is run before the popup is shown
     *
     * @param target target button
     */
    public static void addReloadableRightClickPopupMenuToComponent(Component target, JPopupMenu popupMenu, Consumer<JPopupMenu> reloadFunction) {
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                    popupMenu.revalidate();
                    reloadFunction.accept(popupMenu);
                    popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
    }

    /**
     * Adds an existing popup menu to a button
     * Adds a function that is run before the popup is shown
     *
     * @param target target button
     * @return the popup menu
     */
    public static JPopupMenu addRightClickPopupMenuToButton(AbstractButton target) {
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
    public static JPopupMenu addRightClickPopupMenuToButton(AbstractButton target, JPopupMenu popupMenu) {
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
            URL url = ResourceUtils.getPluginResourceInverted(path);
            if(url == null) {
                System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
                url = ResourceUtils.getPluginResource("icons/missing.png");
            }
            icon = new ImageIcon(url);
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
            URL url = ResourceUtils.getPluginResourceInverted(path);
            if(url == null) {
                System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
                url = ResourceUtils.getPluginResource("icons-32/missing.png");
            }
            icon = new ImageIcon(url);
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
            URL url = ResourceUtils.getPluginResourceInverted(path);
            if(url == null) {
                System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
                url = ResourceUtils.getPluginResource("icons-12/missing.png");
            }
            icon = new ImageIcon(url);
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
            URL url = ResourceUtils.getPluginResource(path);
            if(url == null) {
                System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
                url = ResourceUtils.getPluginResource("icons/missing.png");
            }
            icon = new ImageIcon(url);
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
            URL url = ResourceUtils.getPluginResource(path);
            if(url == null) {
                System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
                url = ResourceUtils.getPluginResource("icons-32/missing.png");
            }
            icon = new ImageIcon(url);
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
            URL url = ResourceUtils.getPluginResource(path);
            if(url == null) {
                System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
                url = ResourceUtils.getPluginResource("icons-12/missing.png");
            }
            icon = new ImageIcon(url);
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
            URL url = ResourceUtils.getPluginResource(path);
            if(url == null) {
                System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
                url = ResourceUtils.getPluginResource("icons-64/missing.png");
            }
            icon = new ImageIcon(url);
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
            URL url = ResourceUtils.getPluginResource(path);
            if(url == null) {
                System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
                url = ResourceUtils.getPluginResource("icons-128/missing.png");
            }
            icon = new ImageIcon(url);
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
            URL url = ResourceUtils.getPluginResource(path);
            if(url == null) {
                System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
                url = ResourceUtils.getPluginResource("icons-8/missing.png");
            }
            icon = new ImageIcon(url);
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
        String path = "icons/" + iconName;
        URL url = ResourceUtils.getPluginResource(path);
        if(url == null) {
            System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
            url = ResourceUtils.getPluginResource("icons/missing.png");
        }
        return url;
    }

    /**
     * Returns an icon from JIPipe resources
     * If you want to utilize resources from your Java extension, use {@link JIPipeResourceManager}
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public static URL getIcon16URLFromResources(String iconName) {
        return getIconURLFromResources(iconName);
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
//        Border compound = new CompoundBorder(UIUtils.createControlBorder(), margin);
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
     * @return the component
     */
    public static <T extends AbstractButton> T makeButtonFlat25x25(T component) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
        component.setPreferredSize(new Dimension(25, 25));
        component.setMinimumSize(new Dimension(25, 25));
        component.setMaximumSize(new Dimension(25, 25));
//        Border margin = new EmptyBorder(2, 2, 2, 2);
//        Border compound = new CompoundBorder(new RoundedLineBorder(ModernMetalTheme.GRAY2, 1, 2), margin);
//        component.setBorder(compound);
        component.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return component;
    }

    /**
     * Makes a button flat and 25x25 size
     *
     * @param component the button
     * @return the component
     */
    public static <T extends AbstractButton> T makeButtonFlatWithSize(T component, int size) {
        return makeButtonFlatWithSize(component, size, 3);
    }

    /**
     * Makes a button flat and 25x25 size
     *
     * @param component the button
     */
    public static <T extends AbstractButton> T makeButtonFlatWithSize(T component, int size, int borderSize) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
        component.setPreferredSize(new Dimension(size, size));
        component.setMinimumSize(new Dimension(size, size));
        component.setMaximumSize(new Dimension(size, size));
        component.setBorder(BorderFactory.createEmptyBorder(borderSize, borderSize, borderSize, borderSize));
        return component;
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
    public static <T extends AbstractButton> T makeButtonFlat(T component) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
//        component.setPreferredSize(new Dimension(component.getPreferredSize().width, 25));
//        component.setMinimumSize(new Dimension(25, 25));
//        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        Border margin = new EmptyBorder(3, 3, 3, 3);
//        Border compound = new CompoundBorder(UIUtils.createControlBorder(), margin);
        component.setBorder(margin);
        return component;
    }

    /**
     * Makes a component borderless
     *
     * @param component the component
     */
    public static <T extends AbstractButton> T makeButtonBorderlessWithoutMargin(T component) {
        component.setBackground(Color.WHITE);
        component.setOpaque(false);
        component.setBorder(null);
        return component;
    }

    /**
     * Installs an event to the window that asks the user before the window is closes
     *
     * @param window  the window
     * @param message the close message
     * @param title   the close message title
     */
    public static void setToAskOnClose(JDialog window, String message, String title) {
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
     * @param askOnClose only ask if condition is true
     */
    public static void setToAskOnClose(JFrame window, String message, String title, Supplier<Boolean> askOnClose) {
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if(!askOnClose.get()) {
                    windowEvent.getWindow().dispose();
                }
                else if (JOptionPane.showConfirmDialog(windowEvent.getComponent(), message, title,
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
     * @param workbench the workbench
     * @param parent    the parent component
     * @param exception the exception
     */
    public static void showErrorDialog(JIPipeDesktopWorkbench workbench, Component parent, Throwable exception) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Error");
        JIPipeDesktopUserFriendlyErrorUI errorUI = new JIPipeDesktopUserFriendlyErrorUI(workbench, null, JIPipeDesktopUserFriendlyErrorUI.WITH_SCROLLING);
        errorUI.displayErrors(exception);
        errorUI.addVerticalGlue();
        dialog.setContentPane(errorUI);
        dialog.setModal(false);
        dialog.pack();
        dialog.setSize(new Dimension(800, 600));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public static void openNotificationsDialog(JIPipeDesktopWorkbench workbench, Component parent, JIPipeNotificationInbox notifications, String title, String infoText, boolean autoClose) {

        if (autoClose && notifications.isEmpty()) {
            return;
        }

        JPanel contentPanel = new JPanel(new BorderLayout(8, 8));

        JIPipeDesktopGenericNotificationInboxUI inboxUI = new JIPipeDesktopGenericNotificationInboxUI(workbench, notifications);
        contentPanel.add(inboxUI, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new GridBagLayout());
        messagePanel.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));
        messagePanel.add(new JLabel(UIUtils.getIcon32FromResources("dialog-warning.png")),
                new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        messagePanel.add(createReadonlyBorderlessTextArea(infoText),
                new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
        contentPanel.add(messagePanel, BorderLayout.NORTH);

        JDialog dialog = new JDialog();
        addEscapeListener(dialog);
        dialog.setIconImage(getJIPipeIcon128());
        dialog.setTitle(title);
        dialog.setContentPane(contentPanel);
        dialog.setModal(false);
        dialog.pack();
        dialog.setSize(new Dimension(800, 600));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        if (autoClose) {
            notifications.getUpdatedEventEmitter().subscribeLambda((emitter, event) -> {
                if (notifications.isEmpty()) {
                    dialog.setVisible(false);
                }
            });
        }
    }

    public static <T> void selectItemsInJList(JList<T> list, List<T> itemsToSelect) {
        if (itemsToSelect.isEmpty()) {
            list.clearSelection();
            return;
        }

        ListModel<T> model = list.getModel();
        List<Integer> indicesToSelect = new ArrayList<>();

        for (int i = 0; i < model.getSize(); i++) {
            T element = model.getElementAt(i);
            if (itemsToSelect.contains(element)) {
                indicesToSelect.add(i);
            }
        }

        if (indicesToSelect.isEmpty()) {
            list.clearSelection();
        } else {
            int[] indicesArray = indicesToSelect.stream().mapToInt(Integer::intValue).toArray();
            list.setSelectedIndices(indicesArray);
        }
    }

    /**
     * Opens a dialog showing a validity report
     *
     * @param workbench the workbench
     * @param parent    the parent component
     * @param report    the report
     * @param title     the title
     * @param infoText  the info text
     * @param modal     make the dialog modal
     */
    public static void showValidityReportDialog(JIPipeDesktopWorkbench workbench, Component parent, JIPipeValidationReport report, String title, String infoText, boolean modal) {
        JPanel contentPanel = new JPanel(new BorderLayout(8, 8));

        JIPipeDesktopValidityReportUI ui = new JIPipeDesktopValidityReportUI(workbench, false);
        ui.setReport(report);

        contentPanel.add(ui, BorderLayout.CENTER);

        if (infoText != null) {
            JPanel messagePanel = new JPanel(new GridBagLayout());
            messagePanel.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));
            messagePanel.add(new JLabel(UIUtils.getIcon32FromResources("dialog-error.png")),
                    new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
            messagePanel.add(createReadonlyBorderlessTextArea(infoText),
                    new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
            contentPanel.add(messagePanel, BorderLayout.NORTH);
        }

        if (modal) {
            JDialog dialog = new JDialog();
            dialog.setTitle(title);
            dialog.setContentPane(contentPanel);
            dialog.setModal(modal);
            dialog.pack();
            dialog.setSize(new Dimension(800, 600));
            dialog.setLocationRelativeTo(parent);
            dialog.setIconImage(UIUtils.getJIPipeIcon128());

            JIPipeDesktopAlwaysOnTopToggle topToggle = new JIPipeDesktopAlwaysOnTopToggle(dialog);
            ui.getErrorToolbar().add(topToggle);

            dialog.setVisible(true);
        } else {
            JFrame frame = new JFrame();
            frame.setTitle(title);
            frame.setContentPane(contentPanel);
            frame.pack();
            frame.setSize(new Dimension(800, 600));
            frame.setLocationRelativeTo(parent);
            frame.setIconImage(UIUtils.getJIPipeIcon128());

            JIPipeDesktopAlwaysOnTopToggle topToggle = new JIPipeDesktopAlwaysOnTopToggle(frame);
            ui.getErrorToolbar().add(topToggle);

            frame.setVisible(true);
        }
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
        dialog.setIconImage(UIUtils.getJIPipeIcon128());

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
    public static HTMLText getHTMLByDialog(JIPipeDesktopWorkbench workbench, Component parent, String title, String message, HTMLText initialValue) {
        JIPipeDesktopHTMLEditor area = new JIPipeDesktopHTMLEditor(workbench, JIPipeDesktopHTMLEditor.Mode.Full, JIPipeDesktopHTMLEditor.WITH_SCROLL_BAR);
        area.setText(initialValue.getHtml());

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title);
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

        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);

        if (confirmation.get()) {
            return new HTMLText(area.getHTML());
        }
        return null;
    }

    public static boolean showConfirmDialog(Component parent, String title, Dimension size, Component content) {

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title);
        dialog.setLayout(new BorderLayout());
        dialog.add(content, BorderLayout.CENTER);

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
        dialog.pack();
        dialog.setSize(size);
        dialog.setModal(true);
        dialog.setVisible(true);

        return confirmation.get();
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
     * Gets an integer by dialog
     *
     * @param parent     parent component
     * @param title      title
     * @param message    message
     * @param initialMin initial minimum value
     * @param initialMax initial maximum value
     * @param min        minimum value
     * @param max        maximum value
     * @return the selected double or null if cancelled
     */
    public static Optional<double[]> getMinMaxByDialog(Component parent, String title, String message, double initialMin, double initialMax, double min, double max) {
        JSpinner spinnerMin = new JSpinner(new SpinnerNumberModel(initialMin, min, max, 1));
        JSpinner spinnerMax = new JSpinner(new SpinnerNumberModel(initialMax, min, max, 1));

        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.NONE);
        formPanel.addToForm(spinnerMin, new JLabel("Min"));
        formPanel.addToForm(spinnerMax, new JLabel("Max"));

        int result = JOptionPane.showOptionDialog(
                parent,
                new Object[]{message, formPanel},
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            return Optional.of(new double[]{((SpinnerNumberModel) spinnerMin.getModel()).getNumber().doubleValue(),
                    ((SpinnerNumberModel) spinnerMax.getModel()).getNumber().doubleValue()});
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
    public static void makeToggleButtonReadonly(JToggleButton toggleButton) {
        toggleButton.addActionListener(e -> toggleButton.setSelected(!toggleButton.isSelected()));
    }

    /**
     * Creates a readonly text field
     *
     * @param value text
     * @return textfield
     */
    public static JTextField createReadonlyTextField(String value) {
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
    public static JTextArea createReadonlyTextArea(String text) {
        JTextArea textArea = new JTextArea();
        textArea.setBorder(UIUtils.createControlBorder());
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
    public static JTextPane createReadonlyTextPane(String text) {
        JTextPane textPane = new JTextPane();
        textPane.setBorder(UIUtils.createControlBorder());
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
    public static JTextPane createBorderlessReadonlyTextPane(String text, boolean opaque) {
        JTextPane textPane = new JTextPane();
        textPane.setBorder(UIUtils.createControlBorder());
        textPane.setEditable(false);
        textPane.setOpaque(opaque);
        textPane.setContentType("text/html");
        setTextPaneFont(textPane, Font.DIALOG, 12);
        textPane.setText(text);
        textPane.setBorder(null);
        addHyperlinkListener(textPane);
        return textPane;
    }

    public static void setTextPaneFont(JTextPane textPane, String fontFamily, int fontSize) {
        HTMLEditorKit kit = new HTMLEditorKit();
        HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
        textPane.setEditorKit(kit);
        textPane.setDocument(doc);

        // Create a new style sheet
        StyleSheet styleSheet = kit.getStyleSheet();
        Style defaultStyle = styleSheet.getStyle(StyleContext.DEFAULT_STYLE);

        // Set the default font family and size
        styleSheet.addRule("body { font-family: " + fontFamily + "; font-size: " + fontSize + "pt; }");

        // Set the default style in the document
        doc.getStyleSheet().addStyleSheet(styleSheet);
        doc.setLogicalStyle(0, defaultStyle);
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
    public static JTextField createReadonlyBorderlessTextField(String value) {
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
    public static JTextArea createReadonlyBorderlessTextArea(String text) {
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
    public static JTextPane createMarkdownReader(MarkdownText document, String[] css) {
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

    public static JMenuItem getMenuItem(JComponent menu, int index) {
        if (menu instanceof JMenu) {
            return ((JMenu) menu).getItem(index);
        } else {
            return (JMenuItem) menu.getComponent(index);
        }
    }

    public static int getMenuItemCount(JComponent menu) {
        if (menu instanceof JMenu) {
            return ((JMenu) menu).getItemCount();
        } else {
            return menu.getComponentCount();
        }
    }

    /**
     * Creates a hierarchy of menus based on the menu paths vector
     *
     * @param rootMenu  the root menu
     * @param menuPaths strings that have menu items separated by newlines
     * @return map from menu path to submenu
     */
    public static Map<String, JComponent> createMenuTree(JPopupMenu rootMenu, Set<String> menuPaths) {
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

        Map<String, JComponent> result = new HashMap<>();
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
                    JComponent parentMenu = result.get(parentMenuPath);
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
    public static JLabel createURLLabel(String url) {
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
        ArrayList<TreeNode> list = Collections.list(node.children());
        for (TreeNode treeNode : list) {
            if (treeNode instanceof DefaultMutableTreeNode) {
                setNodeExpandedState(tree, (DefaultMutableTreeNode) treeNode, expanded);
            }
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
        List<JIPipeDesktopMenuExtension> extensions = JIPipe.getCustomMenus()
                .getMenuExtensionsTargeting(targetMenuType, workbench);
        if (!extensions.isEmpty()) {
            if (withSeparator)
                targetMenu.addSeparator();
            for (Map.Entry<String, JMenu> entry : createMenuTree(targetMenu, extensions.stream()
                    .map(JIPipeDesktopMenuExtension::getMenuPath).collect(Collectors.toSet())).entrySet()) {
                for (JIPipeDesktopMenuExtension extension : extensions) {
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

    public static void showConnectionErrorMessage(Component parent, JIPipeDataSlot source, JIPipeDataSlot target) {
        if (!JIPipe.getDataTypes().isConvertible(source.getAcceptedDataType(), target.getAcceptedDataType())) {
            JOptionPane.showMessageDialog(parent,
                    String.format("Unable to convert data type '%s' to '%s'!\nPlease refer to the data type documentation (top right [?] button) for info about which data types are compatible.", JIPipeDataInfo.getInstance(source.getAcceptedDataType()).getName(), JIPipeDataInfo.getInstance(target.getAcceptedDataType())),
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

    public static void invokeScrollToBottom(JScrollPane scrollPane) {
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }

    public static void redirectDragEvents(Component component, Component target) {
        component.setFocusable(false);
        DragThroughMouseListener listener = new DragThroughMouseListener(component, target);
        component.addMouseListener(listener);
        component.addMouseMotionListener(listener);
    }

    public static JMenuItem createMenuItem(String label, String description, Icon icon, Runnable action) {
//        System.out.println(label + " " + description + " " + icon);
//        description = "";
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
        if (function != null) {
            button.addActionListener(e -> function.run());
        }
        return button;
    }

    public static JButton createButton(String text, Icon icon, Runnable function) {
        JButton button = new JButton(text, icon);
        if (function != null) {
            button.addActionListener(e -> function.run());
        }
        return button;
    }

    public static JButton createIconOnlyButton(String text, Icon icon, Runnable function) {
        JButton button = new JButton(icon);
        button.setToolTipText(text);
        if (function != null) {
            button.addActionListener(e -> function.run());
        }
        return button;
    }

    public static String keyStrokeToString(KeyStroke key) {
        int m = key.getModifiers();
        List<String> keyNames = new ArrayList<>();

        if ((m & (InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK)) != 0) {
            keyNames.add("Shift");
        }
        if ((m & (InputEvent.CTRL_DOWN_MASK | InputEvent.CTRL_MASK)) != 0) {
            keyNames.add("Ctrl");
        }
        if ((m & (InputEvent.META_DOWN_MASK | InputEvent.META_MASK)) != 0) {
            keyNames.add("Meta");
        }
        if ((m & (InputEvent.ALT_DOWN_MASK | InputEvent.ALT_MASK)) != 0) {
            keyNames.add("Alt");
        }
        if ((m & (InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON1_MASK)) != 0) {
            keyNames.add("Mouse Left");
        }
        if ((m & (InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON2_MASK)) != 0) {
            keyNames.add("Mouse Middle");
        }
        if ((m & (InputEvent.BUTTON3_DOWN_MASK | InputEvent.BUTTON3_MASK)) != 0) {
            keyNames.add("Mouse Right");
        }
        switch (key.getKeyEventType()) {
            case KeyEvent.KEY_TYPED:
                keyNames.add(String.valueOf(key.getKeyChar()));
                break;
            case KeyEvent.KEY_PRESSED:
                keyNames.add(KeyEvent.getKeyText(key.getKeyCode()));
                break;
            case KeyEvent.KEY_RELEASED:
                keyNames.add(KeyEvent.getKeyText(key.getKeyCode()));
                break;
        }

        return String.join("+", keyNames);
    }

    public static JButton createStandardButton(String text, ImageIcon icon, Runnable action) {
        JButton button = createButton(text, icon, action);
        setStandardButtonBorder(button);
        return button;
    }

    public static JButton createLeftAlignedButton(String text, ImageIcon icon, Runnable action) {
        JButton button = createButton(text, icon, action);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        return button;
    }

    public static Border createSuccessBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createCompoundBorder(new RoundedLineBorder(JIPipeNotificationAction.Style.Success.getBackground(), 1, 5),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)));
    }

    public static <T extends AbstractButton> T makeButtonHighlightedSuccess(T button) {
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createCompoundBorder(new RoundedLineBorder(JIPipeNotificationAction.Style.Success.getBackground(), 1, 5),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3))));
        return button;
    }

    public static <T extends JComponent> T setFontSize(T component, int fontSize) {
        component.setFont(new Font(Font.DIALOG, Font.PLAIN, fontSize));
        return component;
    }

    public static Border createButtonBorder(Color color) {
        return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createCompoundBorder(new RoundedLineBorder(color, 1, 5),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)));
    }

    public static Border createEmptyBorder(int i) {
        return BorderFactory.createEmptyBorder(i, i, i, i);
    }

    public static JIPipeDesktopUITheme getTheme() {
        if (JIPipe.isInstantiated()) {
            return JIPipeGeneralUIApplicationSettings.getInstance().getTheme();
        } else {
            return JIPipeDesktopUITheme.ModernLight;
        }
    }

    public static void repaintLater(Component component) {
        SwingUtilities.invokeLater(() -> component.repaint(50));
    }

    public static void requestFocusOnShown(JComponent component) {
        component.addAncestorListener(new AncestorListener() {

            public void ancestorRemoved(AncestorEvent event) {
            }

            public void ancestorMoved(AncestorEvent event) {
            }

            public void ancestorAdded(final AncestorEvent event) {
                AncestorListener listener = this;
                SwingUtilities.invokeLater(() -> {
                    event.getComponent().requestFocusInWindow();
                    event.getComponent().removeAncestorListener(listener);
                });
            }
        });
    }

    public static URL getIcon32URLFromResources(String iconName) {
        String path = "icons-32/" + iconName;
        URL url = ResourceUtils.getPluginResource(path);
        if(url == null) {
            System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
            url = ResourceUtils.getPluginResource("icons-32/missing.png");
        }
        return url;
    }

    public static URL getIcon64URLFromResources(String iconName) {
        String path = "icons-64/" + iconName;
        URL url = ResourceUtils.getPluginResource(path);
        if(url == null) {
            System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
            url = ResourceUtils.getPluginResource("icons-64/missing.png");
        }
        return url;
    }

    public static URL getIcon128URLFromResources(String iconName) {
        String path = "icons-128/" + iconName;
        URL url = ResourceUtils.getPluginResource(path);
        if(url == null) {
            System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
            url = ResourceUtils.getPluginResource("icons-128/missing.png");
        }
        return url;
    }

    public static URL getIcon8URLFromResources(String iconName) {
        String path = "icons-8/" + iconName;
        URL url = ResourceUtils.getPluginResource(path);
        if(url == null) {
            System.err.println("Unable to find icon " + path + " (replacing with missing.png)");
            url = ResourceUtils.getPluginResource("icons-8/missing.png");
        }
        return url;
    }

    public static void makeNonOpaque(Component component, boolean recursive) {
        if (recursive) {
            Stack<JComponent> stack = new Stack<>();
            if (component instanceof JComponent) {
                stack.push((JComponent) component);
            }
            while (!stack.isEmpty()) {
                JComponent component1 = stack.pop();
                component1.setOpaque(false);
                for (Component child : component1.getComponents()) {
                    if (child instanceof JComponent) {
                        stack.push((JComponent) child);
                    }
                }
            }
        } else {
            if (component instanceof JComponent) {
                ((JComponent) component).setOpaque(false);
            }
        }
    }

    public static void invokeMuchLater(int timeout, Runnable run) {
        Timer timer = new Timer(timeout, e -> {
            SwingUtilities.invokeLater(run);
        });
        timer.setRepeats(false);
        timer.start();
    }

    public static JButton makeButtonTransparent(JButton button) {
        button.setOpaque(false);
        button.setBackground(new Color(0, 0, 0, 0));
        return button;
    }

    public static JPanel borderNorthSouth(JComponent north, JComponent south) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(north, BorderLayout.NORTH);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    public static JPanel borderNorthCenter(JComponent north, JComponent center) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(north, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    public static <T> DefaultComboBoxModel<T> toComboBoxModel(Collection<T> elements) {
        DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
        for (T element : elements) {
            model.addElement(element);
        }
        return model;
    }


    public static <T> DefaultListModel<T> toListModel(Collection<T> elements) {
        DefaultListModel<T> model = new DefaultListModel<>();
        for (T element : elements) {
            model.addElement(element);
        }
        return model;
    }

    public static <T extends JComponent> T setStandardControlBorder(T component) {
        component.setBorder(createControlBorder());
        return component;
    }

    public static JPanel wrapInEmptyBorder(JComponent component, int borderSize) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(createEmptyBorder(borderSize));
        panel.add(component, BorderLayout.CENTER);
        return panel;
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
