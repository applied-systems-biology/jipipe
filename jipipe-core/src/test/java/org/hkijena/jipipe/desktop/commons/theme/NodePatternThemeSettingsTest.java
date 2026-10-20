package org.hkijena.jipipe.desktop.commons.theme;

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class NodePatternThemeSettingsTest {

    // Defaults on a fresh style (old theme files must load with these)
    @Test
    void defaultsMatchSpec() {
        JIPipeDesktopModernThemeStyle s = new JIPipeDesktopModernThemeStyle();
        assertTrue(s.isShowNodeFrameworkPattern());
        assertEquals(Color.WHITE, s.getNodePatternInk());
        assertEquals(0.15f, s.getNodePatternOpacity(), 1e-6);
        assertTrue(s.isShowNodeFrameworkBadge());
    }

    // Copy constructor preserves the new fields (style duplication path)
    @Test
    void copyConstructorPreservesPatternSettings() {
        JIPipeDesktopModernThemeStyle s = new JIPipeDesktopModernThemeStyle();
        s.setShowNodeFrameworkPattern(false);
        s.setNodePatternInk(new Color(0x123456));
        s.setNodePatternOpacity(0.4f);
        s.setShowNodeFrameworkBadge(false);
        JIPipeDesktopModernThemeStyle c = new JIPipeDesktopModernThemeStyle(s);
        assertFalse(c.isShowNodeFrameworkPattern());
        assertEquals(new Color(0x123456), c.getNodePatternInk());
        assertEquals(0.4f, c.getNodePatternOpacity(), 1e-6);
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
    @Test
    void missingKeysFallBackToDefaults() throws Exception {
        // "brightness" is the JIPipeDesktopUIThemeBrightness enum (values: Light, Dark)
        byte[] bytes = "{\"name\":\"test\",\"brightness\":\"Light\"}".getBytes();
        JIPipeDesktopModernThemeStyle back = JsonUtils.getObjectMapper().readValue(bytes, JIPipeDesktopModernThemeStyle.class);
        assertTrue(back.isShowNodeFrameworkPattern());
        assertEquals(0.15f, back.getNodePatternOpacity(), 1e-6);
    }
}
