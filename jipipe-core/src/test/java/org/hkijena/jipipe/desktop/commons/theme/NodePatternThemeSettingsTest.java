package org.hkijena.jipipe.desktop.commons.theme;

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class NodePatternThemeSettingsTest {

    private static float shippedThemeOpacity(String themeId) throws Exception {
        try (java.io.InputStream in = NodePatternThemeSettingsTest.class
                .getResourceAsStream("/org/hkijena/jipipe/styles/" + themeId + ".json")) {
            assertNotNull(in, "bundled theme file not found: " + themeId);
            JIPipeDesktopModernThemeStyle style = JsonUtils.getObjectMapper()
                    .readValue(in, JIPipeDesktopModernThemeStyle.class);
            return style.getNodePatternOpacity();
        }
    }

    // Defaults on a fresh style - the base "JIPipe Light" theme is built from
    // exactly this, so these defaults ARE the light theme's values
    @Test
    void defaultsMatchSpec() {
        JIPipeDesktopModernThemeStyle s = new JIPipeDesktopModernThemeStyle();
        assertTrue(s.isShowNodeFrameworkPattern());
        assertEquals(Color.WHITE, s.getNodePatternInk());
        assertEquals(0.3f, s.getNodePatternOpacity(), 1e-6);
        assertTrue(s.isShowNodeFrameworkBadge());
    }

    // Copy constructor preserves the new fields (style duplication path)
    @Test
    void copyConstructorPreservesPatternSettings() {
        JIPipeDesktopModernThemeStyle s = new JIPipeDesktopModernThemeStyle();
        s.setShowNodeFrameworkPattern(false);
        s.setNodePatternInk(new Color(0x123456));
        s.setNodePatternOpacity(0.3f);
        s.setShowNodeFrameworkBadge(false);
        JIPipeDesktopModernThemeStyle c = new JIPipeDesktopModernThemeStyle(s);
        assertFalse(c.isShowNodeFrameworkPattern());
        assertEquals(new Color(0x123456), c.getNodePatternInk());
        assertEquals(0.3f, c.getNodePatternOpacity(), 1e-6);
        assertFalse(c.isShowNodeFrameworkBadge());
    }

    // Jackson round-trip: serialize -> deserialize preserves the fields
    @Test
    void jacksonRoundTripPreservesPatternSettings() throws Exception {
        JIPipeDesktopModernThemeStyle s = new JIPipeDesktopModernThemeStyle();
        s.setNodePatternOpacity(0.22f);
        s.setNodePatternInk(new Color(0xFF8800));
        s.setShowNodeFrameworkPattern(false);
        s.setShowNodeFrameworkBadge(false);
        byte[] bytes = JsonUtils.getObjectMapper().writeValueAsBytes(s);
        JIPipeDesktopModernThemeStyle back = JsonUtils.getObjectMapper().readValue(bytes, JIPipeDesktopModernThemeStyle.class);
        assertEquals(0.22f, back.getNodePatternOpacity(), 1e-6);
        assertEquals(new Color(0xFF8800), back.getNodePatternInk());
        assertFalse(back.isShowNodeFrameworkPattern());
        assertFalse(back.isShowNodeFrameworkBadge());
    }

    // Back-compat: a theme JSON without the new keys deserializes to the defaults
    // (the light-theme 0.3 - which is why the dark themes pin 0.15 explicitly)
    @Test
    void missingKeysFallBackToDefaults() throws Exception {
        // "brightness" is the JIPipeDesktopUIThemeBrightness enum (values: Light, Dark)
        byte[] bytes = "{\"name\":\"test\",\"brightness\":\"Light\"}".getBytes();
        JIPipeDesktopModernThemeStyle back = JsonUtils.getObjectMapper().readValue(bytes, JIPipeDesktopModernThemeStyle.class);
        assertTrue(back.isShowNodeFrameworkPattern());
        assertEquals(0.3f, back.getNodePatternOpacity(), 1e-6);
    }

    // Ship-set pin: the bundled theme files carry the tuned per-theme opacity.
    // Guard against "cleanup" of the dark themes' explicit 0.15 - it looks
    // redundant, but deleting it would silently switch them to the light
    // default (0.3), which is far too visible on dark backgrounds.
    @Test
    void shippedThemesPinPatternOpacity() throws Exception {
        assertEquals(0.3f, shippedThemeOpacity("JIPipe Classic Light"), 1e-6);
        assertEquals(0.15f, shippedThemeOpacity("JIPipe Dark"), 1e-6);
        assertEquals(0.15f, shippedThemeOpacity("JIPipe Dark Neon"), 1e-6);
        assertEquals(0.15f, shippedThemeOpacity("JIPipe Dark High Contrast"), 1e-6);
    }
}
