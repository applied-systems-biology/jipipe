package org.hkijena.jipipe.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.hkijena.jipipe.api.registries.JIPipeApplicationSettingsRegistry;
import org.hkijena.jipipe.desktop.commons.theme.*;
import org.hkijena.jipipe.desktop.commons.theme.ui.*;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ThemeUtils {
    private static boolean INSTALLED_LISTENER;
    private static boolean IS_UPDATING_THEME;
    private static JIPipeDesktopUITheme CURRENT_THEME = JIPipeDesktopUITheme.Modern;
    private static JIPipeDesktopModernThemeStyle CURRENT_STYLE = new  JIPipeDesktopModernThemeStyle();
    public static Theme RSYNTAX_THEME_LIGHT;
    public static Theme RSYNTAX_THEME_DARK;
    private static List<String> AVAILABLE_STYLE_IDS;

    public static boolean isUsingDarkTheme() {
        return CURRENT_THEME == JIPipeDesktopUITheme.Modern && CURRENT_STYLE.getBrightness() == JIPipeDesktopUIThemeBrightness.Dark;
    }

    public static JIPipeDesktopModernThemeStyle getCurrentStyle() {
        return CURRENT_STYLE;
    }

    public static void applyThemeFromSettings() {
        Path propertyFile = JIPipeApplicationSettingsRegistry.getPropertyFile(true);
        if (Files.exists(propertyFile)) {
            try {
                JsonNode node = JsonUtils.getObjectMapper().readValue(propertyFile.toFile(), JsonNode.class);
                JsonNode themeNode = node.path(JIPipeGeneralUIApplicationSettings.ID).path("theme");
                JsonNode styleNode = node.path(JIPipeGeneralUIApplicationSettings.ID).path("theme-style");
                if (!themeNode.isMissingNode()) {
                    // Read the theme
                    try {
                        CURRENT_THEME = JsonUtils.getObjectMapper().readerFor(JIPipeDesktopUITheme.class).readValue(themeNode);
                    }
                    catch (Exception ignored) {}
                }
                if(!styleNode.isMissingNode()) {
                    // Read the style
                    if(CURRENT_THEME == JIPipeDesktopUITheme.Modern){
                        CURRENT_STYLE = getStyleFromId(styleNode.asText());
                    }
                    else {
                        // We automatically load the "Metal" colors
                        CURRENT_STYLE = getStyleFromId("Metal");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            reapplyCurrentTheme();
        }
    }

    public static JIPipeDesktopModernThemeStyle getStyleFromId(String id) {

        // JIPipe light is the default style of the configuration
        if("JIPipe Light".equals(id)) {
            return new  JIPipeDesktopModernThemeStyle();
        }

        // Fall back to the default "JIPipe Light" style
        return new JIPipeDesktopModernThemeStyle();
    }

    public static void switchTheme(JIPipeDesktopUITheme theme, JIPipeDesktopModernThemeStyle style) {
        if(theme != null) {
            CURRENT_THEME = theme;
        }
        if(style != null) {
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
                UIManager.put("Button.borderColor", JIPipeDesktopLegacyModernMetalTheme.MEDIUM_GRAY);
                break;
        }
        IS_UPDATING_THEME = false;

        // Prevent external theme changes
        preventExternalThemeChanges();
    }

    private static void reapplyModernTheme() {
        try {
            JIPipeDesktopModernThemeStyle style = new JIPipeDesktopModernThemeStyle();
            MetalLookAndFeel.setCurrentTheme(new JIPipeDesktopModernMetalTheme(style));
            UIManager.put("style", style);

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
            UIManager.put("Button.borderColor", JIPipeDesktopLegacyModernMetalTheme.MEDIUM_GRAY);
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
        if(AVAILABLE_STYLE_IDS == null) {
            AVAILABLE_STYLE_IDS = new ArrayList<>();
            AVAILABLE_STYLE_IDS.add("JIPipe Light");

            // List styles in the profile directory
            try {
                Path stylesDirectory = getUserStylesDirectory();
                for (Path path : PathUtils.findFileByExtensionIn(stylesDirectory, ".json")) {
                    String id = path.getFileName().toString();
                    id = id.substring(0, id.length() - 5);
                    if(!AVAILABLE_STYLE_IDS.contains(id)) {
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
}
