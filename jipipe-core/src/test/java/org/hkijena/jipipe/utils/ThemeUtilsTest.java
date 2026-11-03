package org.hkijena.jipipe.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUIThemeBrightness;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the resolution of the stored theme and the restart-needed rule (work
 * item #1351). No profile, no Swing and no JIPipe instance is needed: the
 * resolver takes the settings JSON and the restart rule compares it against
 * the values the session recorded as applied.
 */
class ThemeUtilsTest {

    private static final String SHEET_ID = JIPipeGeneralUIApplicationSettings.ID;

    private static JsonNode parse(String json) throws Exception {
        return JsonUtils.getObjectMapper().readTree(json.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    class Resolution {

        /**
         * The production defect: the {@code sheet:} block (written since 6.0.0)
         * holds the theme, the bare block holds a stale pre-migration
         * duplicate. The sheet block must win, or the stale "ModernLight"
         * silently overrides the user's choice.
         */
        @Test
        void sheetBlockWinsOverAStaleBareBlock() throws Exception {
            JsonNode node = parse("""
                    {
                      "$schema": 2,
                      "sheet:org.hkijena.jipipe:general-ui": {"theme-v2": "Modern", "theme-style": "JIPipe Dark"},
                      "org.hkijena.jipipe:general-ui": {"theme": "ModernLight", "theme-style": "JIPipe Light"}
                    }
                    """);
            ThemeUtils.ThemeSettings settings = ThemeUtils.resolveThemeFromSettingsNode(node, false);
            assertEquals(JIPipeDesktopUITheme.Modern, settings.theme());
            assertEquals("JIPipe Dark", settings.style().getId());
        }

        /**
         * A bare block that carries a value the sheet block lacks (a profile
         * saved before the theme was keyified) must still be honoured.
         */
        @Test
        void bareBlockFillsInValuesTheSheetBlockLacks() throws Exception {
            JsonNode node = parse("""
                    {
                      "sheet:org.hkijena.jipipe:general-ui": {"theme-v2": "Modern"},
                      "org.hkijena.jipipe:general-ui": {"theme-style": "JIPipe Dark"}
                    }
                    """);
            ThemeUtils.ThemeSettings settings = ThemeUtils.resolveThemeFromSettingsNode(node, false);
            assertEquals("JIPipe Dark", settings.style().getId());
        }

        /**
         * Pre-6.0.0 profiles store the theme under "theme". It is still read
         * (never written), so a profile whose migration has not run yet does not
         * lose its theme.
         */
        @Test
        void legacyThemeKeyIsStillHonoured() throws Exception {
            JsonNode node = parse("""
                    {"org.hkijena.jipipe:general-ui": {"theme": "Metal", "theme-style": "JIPipe Dark"}}
                    """);
            ThemeUtils.ThemeSettings settings = ThemeUtils.resolveThemeFromSettingsNode(node, false);
            assertEquals(JIPipeDesktopUITheme.Metal, settings.theme());
            // A non-modern theme does not use the modern style: Metal colours apply.
            assertEquals("Metal", settings.style().getId());
        }

        /**
         * The parameter object form must be accepted: dynamic-enum parameters
         * serialize as objects in some documents, so a hand-edited or older
         * file can hold {"value": ...} instead of the plain id.
         */
        @Test
        void styleValueMayBeTheParameterObject() throws Exception {
            JsonNode node = parse("""
                    {"sheet:org.hkijena.jipipe:general-ui": {"theme-v2": "Modern", "theme-style": {"value": "JIPipe Dark Neon"}}}
                    """);
            ThemeUtils.ThemeSettings settings = ThemeUtils.resolveThemeFromSettingsNode(node, false);
            assertEquals("JIPipe Dark Neon", settings.style().getId());
        }

        /**
         * Fail closed: an unknown style id must not degrade into the default
         * LIGHT style, because the caller keeps the dark brightness and would
         * render the dark code paths (icons, editor schemes) over a light skin.
         * The style the session already has stays.
         */
        @Test
        void unknownStyleIdDoesNotProduceALightStyle() throws Exception {
            ThemeUtils.setCurrentThemeForTesting(JIPipeDesktopUITheme.Modern, ThemeUtils.getStyleFromId("JIPipe Dark"));
            assertTrue(ThemeUtils.isUsingDarkTheme(), "the session must be running the dark theme for this test");
            try {
                JsonNode node = parse("""
                        {"sheet:org.hkijena.jipipe:general-ui": {"theme-v2": "Modern", "theme-style": "No Such Style"}}
                        """);
                ThemeUtils.ThemeSettings settings = ThemeUtils.resolveThemeFromSettingsNode(node, false);
                assertNotNull(settings.style());
                assertNotEquals(JIPipeDesktopUIThemeBrightness.Light, settings.style().getBrightness(),
                        "an unresolvable style must not silently turn the UI light");
            } finally {
                ThemeUtils.setCurrentThemeForTesting(JIPipeDesktopUITheme.Modern, new org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle());
            }
        }

        @Test
        void missingStyleKeepsTheCurrentStyle() throws Exception {
            JsonNode node = parse("""
                    {"sheet:org.hkijena.jipipe:general-ui": {"theme-v2": "Modern"}}
                    """);
            assertEquals(ThemeUtils.getCurrentStyle().getId(),
                    ThemeUtils.resolveThemeFromSettingsNode(node, false).style().getId());
        }

        @Test
        void aMissingDocumentResolvesToTheCurrentValues() {
            ThemeUtils.ThemeSettings settings = ThemeUtils.resolveThemeFromSettingsNode(null, false);
            assertEquals(ThemeUtils.getCurrentTheme(), settings.theme());
            assertEquals(ThemeUtils.getCurrentStyle(), settings.style());
        }

        @Test
        void unresolvableStyleIdIsRefusedRatherThanReturnedAsLight() {
            assertEquals(null, ThemeUtils.getStyleFromId("No Such Style"),
                    "callers must keep the current style instead of a light fallback");
        }
    }

    @Nested
    class RestartRequired {

        /**
         * "Needs a restart" compares the STORED value against the value the
         * session APPLIED - never against the previous stored value, or a
         * second edit back to the running value would still warn.
         */
        @Test
        void aStoredValueOtherThanTheAppliedOneNeedsARestart() throws Exception {
            ThemeUtils.recordAppliedTheme(JIPipeDesktopUITheme.Modern.name(), "JIPipe Light");
            try {
                JsonNode unchanged = parse("""
                        {"sheet:org.hkijena.jipipe:general-ui": {"theme-v2": "Modern", "theme-style": "JIPipe Light"}}
                        """);
                assertTrue(ThemeUtils.getRestartRequiredSettings(SHEET_ID, unchanged).isEmpty());

                JsonNode restyled = parse("""
                        {"sheet:org.hkijena.jipipe:general-ui": {"theme-v2": "Modern", "theme-style": "JIPipe Dark"}}
                        """);
                assertEquals(List.of("Theme style"), ThemeUtils.getRestartRequiredSettings(SHEET_ID, restyled));

                JsonNode otherTheme = parse("""
                        {"sheet:org.hkijena.jipipe:general-ui": {"theme-v2": "Metal", "theme-style": "JIPipe Light"}}
                        """);
                assertEquals(List.of("Theme"), ThemeUtils.getRestartRequiredSettings(SHEET_ID, otherTheme),
                        "a non-modern theme does not use the modern style, so only the theme differs");

                JsonNode otherSheet = parse("""
                        {"sheet:some.other:sheet": {"theme-v2": "Metal"}}
                        """);
                assertTrue(ThemeUtils.getRestartRequiredSettings("some.other:sheet", otherSheet).isEmpty(),
                        "only the general UI sheet owns restart-required values");
            } finally {
                ThemeUtils.recordAppliedTheme("", "");
            }
        }
    }
}