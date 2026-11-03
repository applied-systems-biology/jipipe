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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the UI-scale semantics that the settings UI relies on. The effective
 * scale rules are pinned against the ACTUAL bundled JDK (Zulu 21, the snapshot
 * instance): on X11 the AWT scale factor is an {@code int} by design
 * ({@code X11GraphicsDevice#initScaleFactor} truncates), so
 * {@code -Dsun.java2d.uiScale=1.5} applies 1.0, {@code 2.5} applies 2.0, and
 * values below 1 fall back to the automatic (system) scale. The toml-rewrite
 * rule pins what is actually written into the jaunch configs.
 */
class UIUtilsUiScaleTest {

    private static final String TOML_LINE_TRUE =
            "    '-Dsun.java2d.uiScale=true',\n";

    @Test
    void theRewriteReplacesTheAutoValue() {
        String config = "jvm-args = [\n" + TOML_LINE_TRUE + "    '-Dsome.other=1',\n]\n";
        String rewritten = UIUtils.applyUiScaleToJaunchConfig(config, 2.0f);
        assertTrue(rewritten.contains("'-Dsun.java2d.uiScale=2.0',"), rewritten);
        assertTrue(rewritten.contains("'-Dsome.other=1',"), "unrelated args stay untouched");
        assertEquals(1, countOccurrences(rewritten, "-Dsun.java2d.uiScale"), "exactly one uiScale line remains");
    }

    @Test
    void theRewriteReplacesStoredFractionalValues() {
        // The old 25%-step slider wrote values like 1.5 that the JDK silently
        // ignores - the rewrite must clear them.
        String rewritten = UIUtils.applyUiScaleToJaunchConfig(TOML_LINE_TRUE.replace("=true", "=1.5"), 3.0f);
        assertTrue(rewritten.contains("'-Dsun.java2d.uiScale=3.0',"), rewritten);
    }

    @Test
    void theRewriteReplacesIntegralAndPercentageForms() {
        assertTrue(UIUtils.applyUiScaleToJaunchConfig(lineWith("2"), 2.0f).contains("=2.0"));
        assertTrue(UIUtils.applyUiScaleToJaunchConfig(lineWith("150%"), 2.0f).contains("=2.0"));
        assertTrue(UIUtils.applyUiScaleToJaunchConfig(lineWith("2.0"), 3.0f).contains("=3.0"));
    }

    @Test
    void theRewriteLeavesAConfigWithoutThePropertyUntouched() {
        String config = "jvm-args = [\n    '-Dfoo=bar',\n]\n";
        assertEquals(config, UIUtils.applyUiScaleToJaunchConfig(config, 2.0f));
    }

    /**
     * X11 truncation: a stored 1.5 effectively renders at 1.0, a stored 2.5 at 2.0.
     */
    @Test
    void theEffectiveScaleTruncatesStoredFractionalValues() {
        assertEquals(1.0, UIUtils.effectiveUiScaleForStoredValue(1.5, 1.0), "1.5 truncates to 1.0 on X11");
        assertEquals(1.0, UIUtils.effectiveUiScaleForStoredValue(1.25, 1.0));
        assertEquals(2.0, UIUtils.effectiveUiScaleForStoredValue(2.5, 1.0), "2.5 truncates to 2.0 on X11");
        assertEquals(3.0, UIUtils.effectiveUiScaleForStoredValue(3.0, 1.0));
        assertEquals(1.0, UIUtils.effectiveUiScaleForStoredValue(1.0, 2.0));
    }

    /**
     * Stored values below 1 (and "auto" values handled by the caller) fall back
     * to the automatic system scale.
     */
    @Test
    void theEffectiveScaleFallsBackToAutoBelowOne() {
        assertEquals(2.0, UIUtils.effectiveUiScaleForStoredValue(0.5, 2.0));
        assertEquals(1.0, UIUtils.effectiveUiScaleForStoredValue(0.5, 1.0));
    }

    @Test
    void theEffectiveScaleIsAutoWhenNothingIsStored() {
        // "true" (the current toml default) and an absent property both parse
        // to null -> the automatic (system) scale applies.
        assertEquals(2.0, UIUtils.effectiveUiScaleForStoredValue(null, 2.0));
        assertEquals(1.0, UIUtils.effectiveUiScaleForStoredValue(null, 1.0));
    }

    @Test
    void theStoredValueParserMatchesTheJdksAcceptedForms() {
        // The parser must accept every form the bundled JDK's
        // SunGraphicsEnvironment#getScaleFactor accepts (users can edit the
        // tomls by hand), and returns null for the auto markers.
        assertStored("1.5", 1.5);
        assertStored("2", 2.0);
        assertStored("150%", 1.5);
        assertStored("2x", 2.0);
        assertStored("192dpi", 2.0);
        assertStored("true", null);
        assertStored("", null);
        assertStored(null, null);
        assertStored("not-a-number", null);
    }

    private static void assertStored(String property, Double expected) {
        setUiScaleProperty(property);
        try {
            assertEquals(expected, UIUtils.parseStoredUiScaleProperty(),
                    "stored value " + property);
        } finally {
            setUiScaleProperty(null);
        }
    }

    private static void setUiScaleProperty(String value) {
        if (value == null) {
            System.clearProperty("sun.java2d.uiScale");
        } else {
            System.setProperty("sun.java2d.uiScale", value);
        }
    }

    /**
     * The offered segments are 100/200/300; the display maps an effective
     * scale to the nearest offered one (ties toward the lower segment).
     */
    @Test
    void theNearestOfferedPercentClampsToTheSegments() {
        assertEquals(100, UIUtils.nearestOfferedUiScalePercent(1.0));
        assertEquals(100, UIUtils.nearestOfferedUiScalePercent(1.5));
        assertEquals(200, UIUtils.nearestOfferedUiScalePercent(2.0));
        assertEquals(200, UIUtils.nearestOfferedUiScalePercent(2.4));
        assertEquals(300, UIUtils.nearestOfferedUiScalePercent(2.6));
        assertEquals(300, UIUtils.nearestOfferedUiScalePercent(3.0));
        assertEquals(300, UIUtils.nearestOfferedUiScalePercent(4.0), "out of range clamps to the last segment");
        assertEquals(100, UIUtils.nearestOfferedUiScalePercent(0.5), "out of range clamps to the first segment");
    }

    private static String lineWith(String value) {
        return "    '-Dsun.java2d.uiScale=" + value + "',\n";
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
