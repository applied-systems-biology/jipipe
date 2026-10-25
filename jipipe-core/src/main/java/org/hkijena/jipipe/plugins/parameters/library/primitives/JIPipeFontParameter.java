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

package org.hkijena.jipipe.plugins.parameters.library.primitives;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A dedicated font parameter (#1339): family + size + style + color in one
 * value, so a text-rendering node exposes a single coherent "font" instead of
 * three separate parameters.
 * <p>
 * All four fields are always present (never null) — missing JSON keys fall
 * back to defaults on deserialization, which keeps old projects (that stored
 * the fields separately, if at all) loadable. The {@code Color} field uses the
 * mapper's standard hex-string codec, like the {@code color} parameter type.
 */
public class JIPipeFontParameter {

    /**
     * @return the default font family — the logical "Dialog" family resolved
     * to its concrete name (stable per platform, valid for {@link Font}).
     */
    public static String defaultFamily() {
        return new Font("Dialog", Font.PLAIN, 12).getFamily();
    }

    private String family = defaultFamily();
    private int size = 12;
    private int style = Font.PLAIN;
    private Color color = Color.WHITE;

    /**
     * Creates a new instance with the default font.
     */
    public JIPipeFontParameter() {
    }

    /**
     * Copies another font parameter.
     */
    public JIPipeFontParameter(JIPipeFontParameter other) {
        this.family = other.family;
        this.size = other.size;
        this.style = other.style;
        this.color = other.color;
    }

    /**
     * Explicit field setter used by node migrations (#1339, Task 10) to move
     * a node's former separate family/size/style/color parameters into one.
     */
    public void migrateFrom(String family, int size, int style, Color color) {
        this.family = family != null ? family : defaultFamily();
        this.size = size;
        this.style = style;
        this.color = color != null ? color : Color.WHITE;
    }

    // ------------------------------------------------------------------
    // Legacy migration helpers (#1339, Task 10)
    // ------------------------------------------------------------------

    /**
     * The legacy font-style enum name used by the old split parameters
     * (e.g. {@code "Bold"}), mapped to its {@link Font} bit flags.
     *
     * @param styleName the legacy enum name (nullable)
     * @return the style flags, or {@link Font#PLAIN} when absent/unrecognized
     */
    public static int styleNameToFlags(String styleName) {
        if (styleName == null) {
            return Font.PLAIN;
        }
        switch (styleName) {
            case "Bold":
                return Font.BOLD;
            case "Italic":
                return Font.ITALIC;
            case "BoldItalic":
                return Font.BOLD + Font.ITALIC;
            default:
                return Font.PLAIN;
        }
    }

    /**
     * Reads a legacy hex color value (a textual JSON node like
     * {@code "#FF0000"}) into a {@link Color}, or {@code null} when absent.
     */
    public static Color legacyColor(JsonNode colorNode) {
        if (colorNode == null || colorNode.isMissingNode() || colorNode.isNull()) {
            return null;
        }
        try {
            if (colorNode.isTextual()) {
                return JsonUtils.getObjectMapper().readerFor(Color.class).readValue(colorNode);
            }
            // A legacy OptionalColorParameter serializes as {content, enabled}.
            if (colorNode.isObject() && colorNode.has("content")) {
                return JsonUtils.getObjectMapper().readerFor(Color.class).readValue(colorNode.get("content"));
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Composes the legacy split font fields (from a raw node/collection JSON
     * object) into a single {@link JIPipeFontParameter} for a given set of
     * parameter keys. Absent keys fall back to defaults (family, 12, plain,
     * white). This is the idempotent migration path: a node's
     * {@code onDeserialized} calls this when the legacy keys are still present
     * in the loaded JSON (a project saved before the migration).
     *
     * @param json       the raw JSON object that holds the parameters
     * @param familyKey  the legacy family key (a textual node, e.g. {@code "font-family"})
     * @param sizeKey    the legacy size key (a number, e.g. {@code "font-size"})
     * @param styleKey   the legacy style key (a textual node, e.g. {@code "font-style"}); may be null
     * @param colorKey   the legacy color key (a hex/OptionalColor node); may be null
     * @return the composed font parameter
     */
    public static JIPipeFontParameter composeFromLegacy(JsonNode json,
                                                        String familyKey, String sizeKey,
                                                        String styleKey, String colorKey) {
        JIPipeFontParameter result = new JIPipeFontParameter();
        String family = null;
        int size = 12;
        int style = Font.PLAIN;
        Color color = Color.WHITE;

        if (familyKey != null && json.has(familyKey) && json.get(familyKey).isTextual()) {
            family = json.get(familyKey).textValue();
        }
        if (sizeKey != null && json.has(sizeKey)) {
            size = json.get(sizeKey).asInt(12);
        }
        if (styleKey != null && json.has(styleKey) && json.get(styleKey).isTextual()) {
            style = styleNameToFlags(json.get(styleKey).textValue());
        }
        if (colorKey != null && json.has(colorKey)) {
            Color c = legacyColor(json.get(colorKey));
            if (c != null) {
                color = c;
            }
        }
        result.migrateFrom(family, size, style, color);
        return result;
    }

    /**
     * @return the font as an {@link Font} (the color is <em>not</em> part of
     * a {@link Font} — use {@link #getColor()} for it).
     */
    public Font toFont() {
        String f = family;
        if (f == null) {
            f = defaultFamily();
        } else if (!availableFamilies().contains(f)) {
            f = defaultFamily();
        }
        return new Font(f, style, size);
    }

    private static List<String> availableFamilies() {
        try {
            return Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    public JIPipeFontParameter copy() {
        return new JIPipeFontParameter(this);
    }

    @JsonGetter("family")
    public String getFamily() {
        return family;
    }

    /** @param family the font family name (null resets to the default family) */
    @JsonSetter("family")
    public void setFamily(String family) {
        this.family = family != null ? family : defaultFamily();
    }

    @JsonGetter("size")
    public int getSize() {
        return size;
    }

    @JsonSetter("size")
    public void setSize(int size) {
        this.size = size;
    }

    @JsonGetter("style")
    public int getStyle() {
        return style;
    }

    @JsonSetter("style")
    public void setStyle(int style) {
        this.style = style;
    }

    @JsonGetter("color")
    public Color getColor() {
        return color;
    }

    /** @param color the text color (null resets to white) */
    @JsonSetter("color")
    public void setColor(Color color) {
        this.color = color != null ? color : Color.WHITE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JIPipeFontParameter that = (JIPipeFontParameter) o;
        return size == that.size
                && style == that.style
                && Objects.equals(family, that.family)
                && Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(family, size, style, color);
    }

    @Override
    public String toString() {
        return family + " " + size + " (" + ((style & Font.BOLD) != 0 ? "B" : "") + ((style & Font.ITALIC) != 0 ? "I" : "") + ")";
    }
}
