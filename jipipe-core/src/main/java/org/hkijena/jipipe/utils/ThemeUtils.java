package org.hkijena.jipipe.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ThemeUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ThemeUtils.class);
    public static final String DEFAULT_STYLE_ID = "JIPipe Light";
    private static final AvailableThemesChangedEventEmitter CHANGED_EVENT_EMITTER = new AvailableThemesChangedEventEmitter();
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

        Path propertyFile = JIPipeApplicationSettingsServiceComponent.getSheetsFile(true);
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

    public static AvailableThemesChangedEventEmitter getAvailableThemesChangedEventEmitter() {
        return CHANGED_EVENT_EMITTER;
    }

    public static JIPipeDesktopModernThemeStyle getStyleFromId(String id) {

        // JIPipe light is the default style of the configuration
        if (DEFAULT_STYLE_ID.equals(id)) {
            return new JIPipeDesktopModernThemeStyle();
        }

        // Try loading from resources
        try {
            URL url = ResourceUtils.getPluginResource("styles/" + id + ".json");
            if (url != null) {
                JIPipeDesktopModernThemeStyle style = JsonUtils.getObjectMapper().readValue(url, JIPipeDesktopModernThemeStyle.class);
                style.setId(id);
                return style;
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        // Try loading from user dir
        try {
            Path path = getUserStylesDirectory().resolve(id + ".json");
            if (Files.exists(path)) {
                JIPipeDesktopModernThemeStyle style = JsonUtils.getObjectMapper().readValue(path.toFile(), JIPipeDesktopModernThemeStyle.class);
                style.setId(id);
                style.setSavePath(path);
                return style;
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        // Fall back to the default "JIPipe Light" style
        return new JIPipeDesktopModernThemeStyle();
    }

    public static JIPipeDesktopModernThemeStyle saveStyle(JIPipeDesktopModernThemeStyle style, String id) {
        id = StringUtils.makeFilesystemCompatible(id);
        Path path = getUserStylesDirectory().resolve(id + ".json");

        JsonUtils.saveToFile(style, path);

        if (!AVAILABLE_STYLE_IDS.contains(id)) {
            AVAILABLE_STYLE_IDS.add(id);
        }

        JIPipeDesktopModernThemeStyle copy = new JIPipeDesktopModernThemeStyle(style);
        copy.setId(id);
        copy.setSavePath(path);

        CHANGED_EVENT_EMITTER.emit(new AvailableThemesChangedEvent(null));

        return copy;
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
            AVAILABLE_STYLE_IDS.add("JIPipe Classic Light");
            AVAILABLE_STYLE_IDS.add("JIPipe Dark");
            AVAILABLE_STYLE_IDS.add("JIPipe Dark Neon");
            AVAILABLE_STYLE_IDS.add("JIPipe Dark High Contrast");

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
        JIPipeNodeTaxonomyFunction fn = info.getPrimaryFunction();
        if (fn != null) {
            // Converted node: resolve the hue from the taxonomy function tree.
            return functionNodeFillColor(fn, CURRENT_STYLE, JIPipe.getNodes().getFunctions());
        }
        // fn-null defensive state (only reachable via JIPipeEmptyNodeInfo,
        // never a registered node): the legacy negative-hue fill channel.
        return CURRENT_STYLE.getPanelBackground();
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
        JIPipeNodeTaxonomyFunction fn = info.getPrimaryFunction();
        if (fn != null) {
            // Converted node: resolve the hue from the taxonomy function tree.
            return functionNodeBorderColor(fn, CURRENT_STYLE, JIPipe.getNodes().getFunctions());
        }
        // fn-null defensive state (only reachable via JIPipeEmptyNodeInfo,
        // never a registered node): the legacy negative-hue border channel.
        return CURRENT_STYLE.getNodeHighlightBorder();
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

    // ------------------------------------------------------------------
    // Function-hue dual-read (taxonomy conversion, Phase 1b)
    //
    // The pure color math lives here so it can be unit tested without a JIPipe
    // instance: it only reads the function's own parent chain via the registry
    // (baseHue/shadeDepth are stateless) and the supplied style's
    // saturation/brightness. The public facades above delegate here with the
    // live style + registry; the node browser's function tree also calls these
    // directly to give each function row the exact fill/border of its canvas nodes.
    // ------------------------------------------------------------------

    /**
     * Fill color for a converted node, derived from its primary taxonomy
     * function's resolved hue, shaded by tree depth.
     *
     * @param fn       the primary function (non-null; the facades guard this)
     * @param style    the theme style supplying saturation/brightness and the
     *                 transparent-channel colors
     * @param registry the function registry for hue/depth resolution
     * @return the fill color
     */
    public static Color functionNodeFillColor(JIPipeNodeTaxonomyFunction fn,
                                              JIPipeDesktopModernThemeStyle style,
                                              FunctionRegistry registry) {
        FunctionHue hue = resolveFunctionHue(fn, registry);
        if (hue.mode == HueMode.Transparent) {
            // Transparent branch: exactly the legacy negative-hue fill channel.
            return style.getPanelBackground();
        }
        float[] hsb = FunctionRegistry.shadeHsb(hue.baseHue,
                style.getNodeFillSaturation(), style.getNodeFillBrightness(), hue.depth);
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * Border color for a converted node, derived from its primary taxonomy
     * function's resolved hue, shaded by tree depth.
     *
     * @param fn       the primary function (non-null; the facades guard this)
     * @param style    the theme style supplying saturation/brightness and the
     *                 transparent-channel colors
     * @param registry the function registry for hue/depth resolution
     * @return the border color
     */
    public static Color functionNodeBorderColor(JIPipeNodeTaxonomyFunction fn,
                                                JIPipeDesktopModernThemeStyle style,
                                                FunctionRegistry registry) {
        FunctionHue hue = resolveFunctionHue(fn, registry);
        if (hue.mode == HueMode.Transparent) {
            // Transparent branch: exactly the legacy negative-hue border channel.
            return style.getNodeHighlightBorder();
        }
        float[] hsb = FunctionRegistry.shadeHsb(hue.baseHue,
                style.getNodeBorderSaturation(), style.getNodeBorderBrightness(), hue.depth);
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    /** The three hue-resolution outcomes for a function. */
    private enum HueMode {
        /** The parent chain ends at a hue-null root: render transparently. */
        Transparent,
        /** A cycle or other resolution failure: fall back to a neutral hue. */
        Broken,
        /** A hue was resolved normally from the chain. */
        Normal
    }

    /** The outcome of resolving a function's hue and shading depth. */
    private record FunctionHue(HueMode mode, float baseHue, int depth) {
    }

    /**
     * Resolves the base hue and shading depth for a function, handling the
     * three edge cases:
     * <ul>
     * <li><b>Transparent</b> — the chain's root has a null hue: render with
     * the theme's panel background / highlight border (the legacy negative-hue
     * channels).</li>
     * <li><b>Broken</b> — a true cycle (or any {@code IllegalStateException}
     * from the registry that is not a transparent root): fall back to base
     * hue 0.5 at depth 0, and log a WARN once with the function id.</li>
     * <li><b>Normal</b> — the nearest non-null hue in the chain, shaded by the
     * number of edges below it.</li>
     * </ul>
     * The transparent root is detected by re-walking the parent chain (with a
     * cycle guard) after the registry throws, so a genuine cycle is never
     * mistaken for a transparent branch.
     *
     * @param fn       the function
     * @param registry the function registry
     * @return the resolved hue outcome
     */
    private static FunctionHue resolveFunctionHue(JIPipeNodeTaxonomyFunction fn, FunctionRegistry registry) {
        try {
            float baseHue = registry.baseHue(fn);
            int depth = registry.shadeDepth(fn);
            return new FunctionHue(HueMode.Normal, baseHue, depth);
        } catch (IllegalStateException e) {
            // Distinguish a transparent root (valid) from a broken/cyclic
            // chain (invalid) by re-walking the parent chain to its root.
            if (isTransparentRoot(fn)) {
                return new FunctionHue(HueMode.Transparent, 0f, 0);
            }
            LOG.warn("Could not resolve a color hue for taxonomy function '{}' ({}); "
                            + "falling back to a neutral hue. Reason: {}",
                    fn.getId(), fn.getName(), e.getMessage());
            return new FunctionHue(HueMode.Broken, 0.5f, 0);
        }
    }

    /**
     * Determines whether the function's parent chain ends at a root whose
     * {@code getColorHue()} is {@code null} (a transparent branch).
     * Cycle-safe: guards against infinite loops on a cyclic chain.
     *
     * @param fn the function
     * @return true if the chain's root has a null hue
     */
    private static boolean isTransparentRoot(JIPipeNodeTaxonomyFunction fn) {
        Set<String> visited = new LinkedHashSet<>();
        JIPipeNodeTaxonomyFunction current = fn;
        while (current != null) {
            if (!visited.add(current.getId())) {
                // Cycle: not a transparent root.
                return false;
            }
            JIPipeNodeTaxonomyFunction parent = current.getParent();
            if (parent == null) {
                return current.getColorHue() == null;
            }
            current = parent;
        }
        return false;
    }

    public static void deleteStyle(String id) {
        JIPipeDesktopModernThemeStyle style = getStyleFromId(id);
        if (style.isBuiltIn()) {
            return;
        }

        try {
            Files.delete(style.getSavePath());
            AVAILABLE_STYLE_IDS.remove(style.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        CHANGED_EVENT_EMITTER.emit(new AvailableThemesChangedEvent(null));
    }

    public interface AvailableThemesChangedEventListener {
        void onAvailableThemesChanged(AvailableThemesChangedEvent event);
    }

    public static class AvailableThemesChangedEvent extends AbstractJIPipeEvent {

        public AvailableThemesChangedEvent(Object source) {
            super(source);
        }
    }

    public static class AvailableThemesChangedEventEmitter extends JIPipeEventEmitter<AvailableThemesChangedEvent, AvailableThemesChangedEventListener> {

        @Override
        protected void call(AvailableThemesChangedEventListener availableThemesChangedEventListener, AvailableThemesChangedEvent event) {
            availableThemesChangedEventListener.onAvailableThemesChanged(event);
        }
    }
}
