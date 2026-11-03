package org.hkijena.jipipe.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.service.components.JIPipeApplicationSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.SettingsKind;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUIThemeBrightness;
import org.hkijena.jipipe.desktop.commons.theme.ui.*;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ThemeUtils {
    public static final String DEFAULT_STYLE_ID = "JIPipe Light";
    /**
     * The style id that selects the plain Metal colours (used by the
     * non-modern themes, which ignore the modern styles). It is not a style
     * file of its own.
     */
    public static final String METAL_STYLE_ID = "Metal";
    private static final Logger LOG = LoggerFactory.getLogger(ThemeUtils.class);
    private static final AvailableThemesChangedEventEmitter CHANGED_EVENT_EMITTER = new AvailableThemesChangedEventEmitter();
    public static Theme RSYNTAX_THEME_LIGHT;
    public static Theme RSYNTAX_THEME_DARK;
    private static boolean INSTALLED_LISTENER;
    private static boolean IS_UPDATING_THEME;
    private static JIPipeDesktopUITheme CURRENT_THEME = JIPipeDesktopUITheme.Modern;
    private static JIPipeDesktopModernThemeStyle CURRENT_STYLE = new JIPipeDesktopModernThemeStyle();
    private static List<String> AVAILABLE_STYLE_IDS;
    private static boolean CARET_MOTION_POLICY_INSTALLED = false;

    public static boolean isUsingDarkTheme() {
        return CURRENT_THEME == JIPipeDesktopUITheme.Modern && CURRENT_STYLE.getBrightness() == JIPipeDesktopUIThemeBrightness.Dark;
    }

    public static JIPipeDesktopModernThemeStyle getCurrentStyle() {
        return CURRENT_STYLE;
    }

    /**
     * @return true when the user has enabled reduced motion. Safe to call at any time:
     * returns false (animations on) when JIPipe is not initialized or the setting is unavailable.
     */
    public static boolean isReducedMotion() {
        if (JIPipe.getInstance() == null) {
            return false;
        }
        try {
            return JIPipeGeneralUIApplicationSettings.getInstance().isReducedMotion();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Applies or clears reduced-motion behavior on a text component's caret.
     * Reduced motion: the caret stays solid (no blink) while its component is focused; it is
     * hidden when unfocused, as usual. Otherwise: restore the standard 500 ms blink.
     * No-op if the component's caret is not a {@link DefaultCaret}.
     */
    public static void applyCaretMotion(JTextComponent component, boolean reducedMotion) {
        Caret caret = component.getCaret();
        if (!(caret instanceof DefaultCaret defaultCaret)) {
            return;
        }
        defaultCaret.setBlinkRate(reducedMotion ? 0 : 500);
    }

    /**
     * Installs the global reduced-motion caret policy: a single "focusOwner" listener on the
     * current keyboard focus manager re-applies {@link #applyCaretMotion(JTextComponent, boolean)}
     * to whatever text component gains focus. A caret can only blink while its component is
     * focused, so applying the policy on every focus change covers every text component -
     * including ones created later by plugins - without per-component wiring.
     * <p>
     * The setting is read on every focus change rather than captured at install time: this
     * policy is installed from {@link #applyThemeFromSettings()}, which runs before the JIPipe
     * instance exists, and the app prompts a restart after changing the setting.
     * <p>
     * The listener runs before the caret's own focus handling, and {@code setBlinkRate(0)} on an
     * editable component clears the caret's saved rate, so the caret cannot re-arm the blink
     * afterwards (verified against the Azul Zulu 21 sources shipped in the JIPipe distribution).
     * Idempotent.
     */
    public static void installCaretMotionPolicy() {
        if (!CARET_MOTION_POLICY_INSTALLED) {
            CARET_MOTION_POLICY_INSTALLED = true;
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .addPropertyChangeListener("focusOwner", e -> {
                        if (e.getNewValue() instanceof JTextComponent component) {
                            applyCaretMotion(component, isReducedMotion());
                        }
                    });
        }
    }

    /**
     * The settings sheet key of the theme. Frozen: it is persisted in profiles
     * (the sheet renamed "theme" to "theme-v2" in 6.0.0, the old key is still
     * honoured on read).
     */
    private static final String THEME_KEY = "theme-v2";

    /**
     * The pre-6.0.0 key of {@link #THEME_KEY}. Only read, never written.
     */
    private static final String LEGACY_THEME_KEY = "theme";

    /**
     * The settings sheet key of the theme style.
     */
    private static final String THEME_STYLE_KEY = "theme-style";

    /**
     * Applies the stored theme to the running UI. Must be called before the
     * first window is created (the look-and-feel is global).
     * <p>
     * Reads the stored values directly from the settings file, so it works
     * before the settings service exists (splash screen, screenshot command).
     * The file is the profile of the <em>current</em> version only: falling back
     * to an older profile's file (which {@code getSheetsFile(true)} does when
     * the current profile directory is still empty, e.g. on a fresh launcher
     * instance) would apply a foreign theme, so it is deliberately never done.
     * <p>
     * Whatever happens - no file, a corrupt file, an unknown style id - the
     * default light theme stays in effect, which is the fail-closed choice: a
     * partially loaded style would render the dark-theme code paths (icons,
     * editor schemes) over a broken light UI.
     */
    public static void applyThemeFromSettings() {

        // Global reduced-motion caret policy - must be in place before any window can receive focus
        installCaretMotionPolicy();

        // Fix for macOS
        System.setProperty("apple.laf.useScreenMenuBar", "false");

        Path propertyFile = JIPipeApplicationSettingsServiceComponent.getSheetsFile(false);
        ThemeSettings applied = new ThemeSettings(CURRENT_THEME, CURRENT_STYLE);
        if (Files.exists(propertyFile)) {
            try {
                JsonNode node = JsonUtils.getObjectMapper().readValue(propertyFile.toFile(), JsonNode.class);
                applied = resolveThemeFromSettingsNode(node, true);
            } catch (Exception e) {
                LOG.warn("Could not read the theme from " + propertyFile + "; using the default theme", e);
            }
        }

        // This session's theme is the one the UI is built with: it is what the
        // restart hint compares the stored value against.
        recordAppliedTheme(applied);

        // Must always be run
        reapplyCurrentTheme();
    }

    /**
     * Resolves the theme and the theme style from a settings JSON node into
     * {@link #CURRENT_THEME} / {@link #CURRENT_STYLE}.
     * <p>
     * Three node shapes are accepted:
     * <ul>
     * <li>a full settings document: the canonical 6.0 layout puts the values
     * into the {@code sheet:} block of the general UI sheet, and the bare
     * block is still read as a fallback because migrated profiles can hold a
     * stale, un-prefixed duplicate of it. Where both blocks carry a key, the
     * {@code sheet:} block wins;</li>
     * <li>the flat {@code sheetId -> block} view the raw readers
     * ({@code getRawSheetsNode()}) return, which has the same shape as a legacy
     * 5.x document;</li>
     * <li>a bare sheet block (what {@code SettingsDocument#getBlock} returns):
     * the general UI block's own contents, so the keys are read from the
     * node itself.</li>
     * </ul>
     *
     * @param node   one of the shapes above, or {@code null}
     * @param commit whether a resolved value is committed to the current theme
     *               / style; {@code false} only resolves them (preview, dirty
     *               check) and leaves the running theme untouched
     * @return the resolved values
     */
    public static ThemeSettings resolveThemeFromSettingsNode(JsonNode node, boolean commit) {
        JIPipeDesktopUITheme theme = CURRENT_THEME;
        JIPipeDesktopModernThemeStyle style = CURRENT_STYLE;
        if (node != null && node.isObject()) {
            String canonicalKey = SettingsKind.SHEET.prefix() + ":" + JIPipeGeneralUIApplicationSettings.ID;
            JsonNode canonicalBlock = node.path(canonicalKey);
            JsonNode bareBlock = node.path(JIPipeGeneralUIApplicationSettings.ID);
            JsonNode themeNode;
            JsonNode styleNode;
            if (canonicalBlock.isMissingNode() && bareBlock.isMissingNode()) {
                // A bare sheet block (the sheet's own contents): read the keys
                // from the node itself.
                themeNode = firstPresent(node, THEME_KEY, LEGACY_THEME_KEY);
                styleNode = firstPresent(node, THEME_STYLE_KEY);
            } else {
                // A document (or the flat raw view): the sheet block wins.
                themeNode = firstPresent(canonicalBlock, bareBlock, THEME_KEY, LEGACY_THEME_KEY);
                styleNode = firstPresent(canonicalBlock, bareBlock, THEME_STYLE_KEY);
            }
            if (themeNode != null) {
                try {
                    theme = JsonUtils.getObjectMapper().readerFor(JIPipeDesktopUITheme.class).readValue(themeNode);
                } catch (Exception e) {
                    LOG.warn("Unknown theme '{}' in the settings; keeping the current theme", themeNode, e);
                }
            }
            if (theme == JIPipeDesktopUITheme.Modern) {
                String styleId = styleIdOf(styleNode);
                if (styleId != null) {
                    JIPipeDesktopModernThemeStyle resolved = getStyleFromId(styleId);
                    if (resolved != null) {
                        // getStyleFromId always yields a fresh object, so
                        // committing it can never alias a live style.
                        style = resolved;
                    }
                }
            } else {
                // We automatically load the "Metal" colors
                JIPipeDesktopModernThemeStyle metal = getStyleFromId(METAL_STYLE_ID);
                if (metal != null) {
                    style = metal;
                }
            }
        }
        ThemeSettings result = new ThemeSettings(theme, style);
        if (commit) {
            CURRENT_THEME = theme;
            CURRENT_STYLE = style;
        }
        return result;
    }

    /**
     * Reads a style id from a settings value. The stored form is the plain id
     * ({@code "JIPipe Dark"}, how {@code JIPipeModernThemeStyleParameter}
     * serializes), but older documents can hold the parameter object
     * ({@code {"value": "JIPipe Dark"}}), so both are accepted.
     *
     * @param node the settings value
     * @return the style id, or {@code null} if there is none
     */
    private static String styleIdOf(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            JsonNode value = node.path("value");
            return value.isTextual() ? value.asText() : null;
        }
        return node.isTextual() ? node.asText() : null;
    }

    /**
     * @param canonicalBlock the {@code sheet:<id>} block (preferred)
     * @param bareBlock      the bare {@code <id>} block (fallback)
     * @param keys           the keys to try, in order
     * @return the first present, non-null value, or {@code null}
     */
    private static JsonNode firstPresent(JsonNode canonicalBlock, JsonNode bareBlock, String... keys) {
        for (String key : keys) {
            JsonNode value = canonicalBlock.path(key);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        for (String key : keys) {
            JsonNode value = bareBlock.path(key);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    /**
     * @param block the node whose keys are to be tried, in order
     * @param keys  the keys to try
     * @return the first present, non-null value, or {@code null}
     */
    private static JsonNode firstPresent(JsonNode block, String... keys) {
        for (String key : keys) {
            JsonNode value = block.path(key);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    /**
     * The blocks/keys whose change only takes effect after a restart, for the
     * restart hint of the settings tab (work item #1351).
     * <p>
     * A value <b>needs a restart</b> when the stored value differs from the
     * value this session actually applied - not from the previous stored value.
     * That is why the effective values are recorded per field: the theme is
     * re-applied by the Appearance preview (so it can look "already applied"
     * without a restart at a glance), but switching it live does not repaint
     * existing components, so the hint must stay until the session that comes
     * up with the stored value is the running one.
     */
    private static final Map<String, String> EFFECTIVE_RESTART_VALUES = new LinkedHashMap<>();

    /**
     * The restart-needed state of the UI scale, which is not a settings key
     * (it lives in the jaunch launch configs, see
     * {@link UIUtils#saveUIScaleToJaunch(float, boolean)}), so the accessibility
     * control reports it here instead of the document watch.
     */
    private static volatile String uiScaleRestartReason;

    private static final RestartStateChangedEventEmitter RESTART_STATE_CHANGED_EVENT_EMITTER =
            new RestartStateChangedEventEmitter();

    /**
     * The settings keys of the general UI sheet that only take effect after a
     * restart, with their human-readable names.
     */
    public static final Map<String, String> RESTART_REQUIRED_SETTING_NAMES = Map.of(
            THEME_KEY, "Theme",
            THEME_STYLE_KEY, "Theme style");

    /**
     * Records the theme/style that this session has in effect. Only
     * {@link #applyThemeFromSettings()} should call this with the values it
     * applied; a live preview switch does not go through it (that is the point
     * of the restart hint).
     */
    private static void recordAppliedTheme(ThemeSettings settings) {
        recordAppliedTheme(settings.theme() == null ? "" : settings.theme().name(),
                effectiveStyleId(settings.theme(), settings.style()));
    }

    /**
     * Records what the session has in effect. Package-visible so the restart
     * rule is testable without a profile or a look-and-feel.
     *
     * @param appliedTheme the theme in effect ({@link JIPipeDesktopUITheme#name()})
     * @param appliedStyle the effective style id (see {@link #effectiveStyleId})
     */
    static void recordAppliedTheme(String appliedTheme, String appliedStyle) {
        EFFECTIVE_RESTART_VALUES.put(THEME_KEY, appliedTheme);
        EFFECTIVE_RESTART_VALUES.put(THEME_STYLE_KEY, appliedStyle);
        RESTART_STATE_CHANGED_EVENT_EMITTER.emit(new RestartStateChangedEvent(null));
    }

    /**
     * Sets the running theme/style without re-applying the look-and-feel.
     * Package-visible: only the theme tests use it, to pin what the resolver
     * keeps when it cannot read a style.
     *
     * @param theme the theme
     * @param style the style
     */
    static void setCurrentThemeForTesting(JIPipeDesktopUITheme theme, JIPipeDesktopModernThemeStyle style) {
        CURRENT_THEME = theme;
        CURRENT_STYLE = style;
    }

    /**
     * @param theme the theme
     * @param style the style
     * @return the style id that is <em>effective</em> for the given theme: the
     * {@code Metal} style for a non-modern theme (the modern style is not used
     * there), otherwise the style's id
     */
    private static String effectiveStyleId(JIPipeDesktopUITheme theme, JIPipeDesktopModernThemeStyle style) {
        if (theme != null && theme != JIPipeDesktopUITheme.Modern) {
            return METAL_STYLE_ID;
        }
        return style == null ? "" : StringUtils.nullToEmpty(style.getId());
    }

    /**
     * Updates the restart-needed state of the UI scale (not a settings key).
     * Called by the accessibility section when the user previews or applies a
     * scale; {@code null} or an empty value clears it.
     *
     * @param reason the restart hint to show (e.g. "UI scale"), or {@code null}
     */
    public static void setUiScaleRestartReason(String reason) {
        String normalized = StringUtils.isNullOrEmpty(reason) ? null : reason;
        if (!Objects.equals(normalized, uiScaleRestartReason)) {
            uiScaleRestartReason = normalized;
            RESTART_STATE_CHANGED_EVENT_EMITTER.emit(new RestartStateChangedEvent(null));
        }
    }

    /**
     * The restart hint of the settings tab: which settings of the given sheet
     * block are stored with a value other than the one this session applied.
     * <p>
     * Only what the user can observe is reported: the style is named only when
     * the stored and the applied theme are both {@code Modern} - a non-modern
     * theme does not use the modern style at all, so switching to it reports
     * "Theme" alone, and a style difference is reported iff it would apply.
     *
     * @param sheetId the general UI sheet id
     * @param node    the sheet's block (from the live settings document, or
     *                {@code null})
     * @return the affected settings (empty when nothing waits for a restart)
     */
    public static List<String> getRestartRequiredSettings(String sheetId, JsonNode node) {
        List<String> result = new ArrayList<>();
        if (!JIPipeGeneralUIApplicationSettings.ID.equals(sheetId)) {
            return result;
        }
        if (!EFFECTIVE_RESTART_VALUES.containsKey(THEME_KEY)) {
            // applyThemeFromSettings() never ran in this JVM (e.g. a headless
            // run), so there is no applied theme to compare against: do not
            // claim that a restart is needed.
            return result;
        }
        ThemeSettings stored = resolveThemeFromSettingsNode(node, false);
        String storedTheme = stored.theme() == null ? "" : stored.theme().name();
        String appliedTheme = EFFECTIVE_RESTART_VALUES.get(THEME_KEY);
        boolean themeDiffers = !Objects.equals(storedTheme, appliedTheme);
        if (themeDiffers) {
            result.add(RESTART_REQUIRED_SETTING_NAMES.get(THEME_KEY));
        }
        if (JIPipeDesktopUITheme.Modern.name().equals(storedTheme)
                && JIPipeDesktopUITheme.Modern.name().equals(appliedTheme)
                && !Objects.equals(effectiveStyleId(stored.theme(), stored.style()),
                EFFECTIVE_RESTART_VALUES.get(THEME_STYLE_KEY))) {
            result.add(RESTART_REQUIRED_SETTING_NAMES.get(THEME_STYLE_KEY));
        }
        return result;
    }

    /**
     * @return the restart-needed state of the UI scale, or {@code null}
     */
    public static String getUiScaleRestartReason() {
        return uiScaleRestartReason;
    }

    public static RestartStateChangedEventEmitter getRestartStateChangedEventEmitter() {
        return RESTART_STATE_CHANGED_EVENT_EMITTER;
    }

    public static AvailableThemesChangedEventEmitter getAvailableThemesChangedEventEmitter() {
        return CHANGED_EVENT_EMITTER;
    }

    /**
     * Loads a theme style by id: built-in {@code JIPipe Light}, then a bundled
     * style resource, then a style in the user's {@code theme-styles} directory.
     * <p>
     * A style that exists but cannot be parsed (corrupt file, an outdated
     * schema) is <b>not</b> silently replaced by the default light style: the
     * caller would then keep the stored dark brightness (see
     * {@link JIPipeGeneralUIApplicationSettings}), i.e. the dark-theme code
     * paths over a light, half-initialized skin. Returning {@code null} lets
     * the caller keep the style it already has.
     *
     * @param id the style id
     * @return the style, or {@code null} if it does not exist or cannot be read
     */
    public static JIPipeDesktopModernThemeStyle getStyleFromId(String id) {
        if (StringUtils.isNullOrEmpty(id)) {
            return null;
        }

        // JIPipe light is the default style of the configuration
        if (DEFAULT_STYLE_ID.equals(id)) {
            return new JIPipeDesktopModernThemeStyle();
        }

        // Try loading from resources. The style file is read directly: the
        // resource lookup is theme-dependent (it prefers dark variants of every
        // resource while the dark theme is active), so going through it would
        // load the DARK file when switching back to a light style.
        try {
            URL url = ResourceUtils.class.getResource(ResourceUtils.getResourcePath("styles/" + id + ".json"));
            if (url != null) {
                JIPipeDesktopModernThemeStyle style = JsonUtils.getObjectMapper().readValue(url, JIPipeDesktopModernThemeStyle.class);
                style.setId(id);
                return style;
            }
        } catch (Exception e) {
            LOG.warn("Could not read the bundled theme style '" + id + "'", e);
            return null;
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
        } catch (Exception e) {
            LOG.warn("Could not read the theme style '" + id + "' from the user directory", e);
            return null;
        }

        // "Metal" is not a style file: it is the marker for the plain Metal
        // colours that a non-modern theme applies. Resolved LAST, so a
        // user-authored "Metal" style file still wins.
        if (METAL_STYLE_ID.equals(id)) {
            JIPipeDesktopModernThemeStyle metal = new JIPipeDesktopModernThemeStyle();
            metal.setId(METAL_STYLE_ID);
            return metal;
        }

        LOG.warn("Unknown theme style '{}'; keeping the current style", id);
        return null;
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

    /**
     * Resolves the base hue and shading depth for a function, handling the
     * three edge cases:
     * <ul>
     * <li><b>Transparent</b> - the chain's root has a null hue: render with
     * the theme's panel background / highlight border (the legacy negative-hue
     * channels).</li>
     * <li><b>Broken</b> - a true cycle (or any {@code IllegalStateException}
     * from the registry that is not a transparent root): fall back to base
     * hue 0.5 at depth 0, and log a WARN once with the function id.</li>
     * <li><b>Normal</b> - the nearest non-null hue in the chain, shaded by the
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

    /**
     * The three hue-resolution outcomes for a function.
     */
    private enum HueMode {
        /**
         * The parent chain ends at a hue-null root: render transparently.
         */
        Transparent,
        /**
         * A cycle or other resolution failure: fall back to a neutral hue.
         */
        Broken,
        /**
         * A hue was resolved normally from the chain.
         */
        Normal
    }

    public interface AvailableThemesChangedEventListener {
        void onAvailableThemesChanged(AvailableThemesChangedEvent event);
    }

    /**
     * A resolved theme + style pair (see
     * {@link #resolveThemeFromSettingsNode(JsonNode, boolean)}).
     *
     * @param theme the theme
     * @param style the style
     */
    public record ThemeSettings(JIPipeDesktopUITheme theme, JIPipeDesktopModernThemeStyle style) {
    }

    public interface RestartStateChangedEventListener {
        void onRestartStateChanged(RestartStateChangedEvent event);
    }

    /**
     * Emitted when the restart-needed state changes (a theme/style edit, a UI
     * scale preview, or the startup application of the stored theme).
     */
    public static class RestartStateChangedEvent extends AbstractJIPipeEvent {

        public RestartStateChangedEvent(Object source) {
            super(source);
        }
    }

    public static class RestartStateChangedEventEmitter extends JIPipeEventEmitter<RestartStateChangedEvent, RestartStateChangedEventListener> {

        @Override
        protected void call(RestartStateChangedEventListener restartStateChangedEventListener, RestartStateChangedEvent event) {
            restartStateChangedEventListener.onRestartStateChanged(event);
        }
    }

    /**
     * The outcome of resolving a function's hue and shading depth.
     */
    private record FunctionHue(HueMode mode, float baseHue, int depth) {
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
