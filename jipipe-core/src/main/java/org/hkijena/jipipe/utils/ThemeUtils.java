package org.hkijena.jipipe.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.api.service.components.JIPipeApplicationSettingsServiceComponent;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUIThemeBrightness;
import org.hkijena.jipipe.desktop.commons.theme.ui.*;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ThemeUtils {
    public static Theme RSYNTAX_THEME_LIGHT;
    public static Theme RSYNTAX_THEME_DARK;
    private static boolean INSTALLED_LISTENER;
    private static boolean IS_UPDATING_THEME;
    private static JIPipeDesktopUITheme CURRENT_THEME = JIPipeDesktopUITheme.Modern;
    private static JIPipeDesktopModernThemeStyle CURRENT_STYLE = new JIPipeDesktopModernThemeStyle();
    private static List<String> AVAILABLE_STYLE_IDS;

    public static boolean isUsingDarkTheme() {
        return CURRENT_THEME == JIPipeDesktopUITheme.Modern && CURRENT_STYLE.getBrightness() == JIPipeDesktopUIThemeBrightness.Dark;
    }

    public static JIPipeDesktopModernThemeStyle getCurrentStyle() {
        return CURRENT_STYLE;
    }

    public static void applyThemeFromSettings() {

        // Fix for macOS
        System.setProperty("apple.laf.useScreenMenuBar", "false");

        Path propertyFile = JIPipeApplicationSettingsServiceComponent.getPropertyFile(true);
        if (Files.exists(propertyFile)) {
            try {
                JsonNode node = JsonUtils.getObjectMapper().readValue(propertyFile.toFile(), JsonNode.class);
                JsonNode themeNode = node.path(JIPipeGeneralUIApplicationSettings.ID).path("theme");
                JsonNode styleNode = node.path(JIPipeGeneralUIApplicationSettings.ID).path("theme-style");
                if (!themeNode.isMissingNode()) {
                    // Read the theme
                    try {
                        CURRENT_THEME = JsonUtils.getObjectMapper().readerFor(JIPipeDesktopUITheme.class).readValue(themeNode);
                    } catch (Exception ignored) {
                    }
                }
                if (!styleNode.isMissingNode()) {
                    // Read the style
                    if (CURRENT_THEME == JIPipeDesktopUITheme.Modern) {
                        CURRENT_STYLE = getStyleFromId(styleNode.asText());
                    } else {
                        // We automatically load the "Metal" colors
                        CURRENT_STYLE = getStyleFromId("Metal");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Must always be run
        reapplyCurrentTheme();
    }

    public static JIPipeDesktopModernThemeStyle getStyleFromId(String id) {

        // JIPipe light is the default style of the configuration
        if ("JIPipe Light".equals(id)) {
            return new JIPipeDesktopModernThemeStyle();
        }

        // Try loading from resources
        try {
            URL url = ResourceUtils.getPluginResource("styles/" + id + ".json");
            if (url != null) {
                return JsonUtils.getObjectMapper().readValue(url, JIPipeDesktopModernThemeStyle.class);
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        // Try loading from user dir
        try {
            Path path = getUserStylesDirectory().resolve(id + ".json");
            if (Files.exists(path)) {
                return JsonUtils.getObjectMapper().readValue(path.toFile(), JIPipeDesktopModernThemeStyle.class);
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        // Fall back to the default "JIPipe Light" style
        return new JIPipeDesktopModernThemeStyle();
    }

    public static void switchTheme(JIPipeDesktopUITheme theme, JIPipeDesktopModernThemeStyle style) {
        if (theme != null) {
            CURRENT_THEME = theme;
        }
        if (style != null) {
            CURRENT_STYLE = style;
        }
        reapplyCurrentTheme();
    }

    public static void reapplyCurrentTheme() {
        IS_UPDATING_THEME = true;
        switch (CURRENT_THEME) {
            case Metal:
                reapplyMetalTheme();
                break;
            case Modern:
                reapplyModernTheme();
                break;
            default:
                UIManager.put("Button.borderColor", CURRENT_STYLE.getBorderColor());
                break;
        }
        IS_UPDATING_THEME = false;

        // Prevent external theme changes
        preventExternalThemeChanges();
    }

    private static void reapplyModernTheme() {
        try {
            MetalLookAndFeel.setCurrentTheme(new JIPipeDesktopModernMetalTheme(CURRENT_STYLE));
            UIManager.put("style", CURRENT_STYLE);

            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            installModernUIs();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    private static void reapplyMetalTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            UIManager.put("Button.borderColor", CURRENT_STYLE.getBorderColor());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    private static void preventExternalThemeChanges() {
        if (!INSTALLED_LISTENER) {
            UIManager.addPropertyChangeListener(evt -> {
                if ("lookAndFeel".equals(evt.getPropertyName())) {
                    if (!IS_UPDATING_THEME) {
                        reapplyCurrentTheme();
                    }
                }
            });
            INSTALLED_LISTENER = true;
        }
    }

    private static void installModernUIs() {
        UIManager.put("ScrollBarUI", JIPipeDesktopModernScrollBarUI.class.getName());
        UIManager.put("SliderUI", JIPipeDesktopModernSliderUI.class.getName());
        UIManager.put("SpinnerUI", JIPipeDesktopModernSpinnerUI.class.getName());
        UIManager.put("SplitPaneUI", JIPipeDesktopModernSplitPaneUI.class.getName());
        UIManager.put("ToggleButtonUI", JIPipeDesktopModernToggleButtonUI.class.getName());
        UIManager.put("ProgressBarUI", JIPipeDesktopModernProgressBarUI.class.getName());
    }

    public static void applyThemeToCodeEditor(RSyntaxTextArea textArea) {
        if (isUsingDarkTheme()) {
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
                if (RSYNTAX_THEME_LIGHT == null) {
                    RSYNTAX_THEME_LIGHT = Theme.load(ResourceUtils.class.getResourceAsStream(
                            "/org/hkijena/jipipe/rsyntaxtextarea/themes/default.xml"));
                }
                RSYNTAX_THEME_LIGHT.apply(textArea);
            } catch (IOException ioe) { // Never happens
                ioe.printStackTrace();
            }
        }
    }

    public static Path getUserStylesDirectory() {
        Path directory = PathUtils.getJIPipeUserDir().resolve("theme-styles");
        PathUtils.createDirectories(directory);
        return directory;
    }

    public static List<String> getAvailableStyleIds() {
        if (AVAILABLE_STYLE_IDS == null) {
            AVAILABLE_STYLE_IDS = new ArrayList<>();
            AVAILABLE_STYLE_IDS.add("JIPipe Light");
            AVAILABLE_STYLE_IDS.add("JIPipe Dark");
            AVAILABLE_STYLE_IDS.add("JIPipe Dark Neon");

            // List styles in the profile directory
            try {
                Path stylesDirectory = getUserStylesDirectory();
                for (Path path : PathUtils.findFilesByExtensionIn(stylesDirectory, ".json")) {
                    String id = path.getFileName().toString();
                    id = id.substring(0, id.length() - 5);
                    if (!AVAILABLE_STYLE_IDS.contains(id)) {
                        AVAILABLE_STYLE_IDS.add(id);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return AVAILABLE_STYLE_IDS;
    }

    public static JIPipeDesktopUITheme getCurrentTheme() {
        return CURRENT_THEME;
    }

    public static boolean isUsingModernTheme() {
        return CURRENT_THEME.isModern();
    }

    /**
     * Returns a fill color for {@link JIPipeNodeInfo}
     *
     * @param info the algorithm type
     * @return the fill color
     */
    public static Color getNodeFillColor(JIPipeNodeInfo info) {
        return getNodeFillColor(info.getCategory());
    }

    public static Color getNodeFillColor(JIPipeNodeTypeCategory category) {
        float colorHue = category.getColorHue();
        return getNodeFillColor(colorHue);
    }

    public static Color getNodeFillColor(float colorHue) {
        if (colorHue < 0) {
            return CURRENT_STYLE.getPanelBackground();
        } else {
            return Color.getHSBColor(colorHue,
                    CURRENT_STYLE.getNodeFillSaturation(),
                    CURRENT_STYLE.getNodeFillBrightness());
        }
    }

    /**
     * Returns a border color for {@link JIPipeNodeInfo}
     *
     * @param info the algorithm type
     * @return the border color
     */
    public static Color getNodeBorderColor(JIPipeNodeInfo info) {
        return getNodeBorderColor(info.getCategory());
    }

    private static Color getNodeBorderColor(JIPipeNodeTypeCategory category) {
        float colorHue = category.getColorHue();
        return getNodeBorderColor(colorHue);
    }

    public static Color getNodeBorderColor(float colorHue) {
        if (colorHue < 0) {
            return CURRENT_STYLE.getNodeHighlightBorder();
        } else {
            return Color.getHSBColor(colorHue,
                    CURRENT_STYLE.getNodeBorderSaturation(),
                    CURRENT_STYLE.getNodeBorderBrightness());
        }
    }
}
