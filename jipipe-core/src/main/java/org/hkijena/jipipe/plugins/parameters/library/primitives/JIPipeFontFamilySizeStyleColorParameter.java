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

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The full font parameter (#1339, Task 2): family + size + style + color in
 * one value, so a text-rendering node exposes a single coherent "font" instead
 * of three separate parameters. The heaviest member of the font family — it
 * carries every capability, so the adaptive editor shows all four sections for
 * it.
 * <p>
 * All four fields are always present (never null) — missing JSON keys fall
 * back to defaults on deserialization, which keeps old projects (that stored
 * the fields separately, if at all) loadable. The {@code Color} field uses the
 * mapper's standard hex-string codec, like the {@code color} parameter type.
 * <p>
 * The family's shared statics ({@code defaultFamily()},
 * {@code styleNameToFlags(String)}, {@code legacyColor(JsonNode)}) live in
 * {@link JIPipeFontFamilySizeStyleParameter}; call sites reference them there
 * directly (the earlier delegating statics were removed in the family
 * cleanup).
 */
public class JIPipeFontFamilySizeStyleColorParameter implements JIPipeFontValue, JIPipeFontStyleCapable, JIPipeFontColorCapable {

    private String family = JIPipeFontFamilySizeStyleParameter.defaultFamily();
    private int size = 12;
    private int style = Font.PLAIN;
    private Color color = Color.WHITE;

    /**
     * Creates a new instance with the default font.
     */
    public JIPipeFontFamilySizeStyleColorParameter() {
    }

    /**
     * Copies another font parameter.
     */
    public JIPipeFontFamilySizeStyleColorParameter(JIPipeFontFamilySizeStyleColorParameter other) {
        this.family = other.family;
        this.size = other.size;
        this.style = other.style;
        this.color = other.color;
    }

    /**
     * Composes the legacy split font fields (from a raw node/collection JSON
     * object) into a single {@link JIPipeFontFamilySizeStyleColorParameter}
     * for a given set of parameter keys. Absent keys fall back to defaults
     * (family, {@code defaultSize}, plain, white). This is the idempotent
     * migration path: a node's {@code onDeserialized} (or a nested collection's
     * {@code fromJson}) calls this when the legacy keys are still present in
     * the loaded JSON (a project saved before the migration).
     *
     * @param json        the raw JSON object that holds the parameters
     * @param familyKey   the legacy family key (a textual node, e.g. {@code "font-family"})
     * @param sizeKey     the legacy size key (a number, e.g. {@code "font-size"})
     * @param styleKey    the legacy style key (a textual node, e.g. {@code "font-style"}); may be null
     * @param colorKey    the legacy color key (a hex/OptionalColor node); may be null
     * @param defaultSize the size to use when {@code sizeKey} is absent
     * @return the composed font parameter
     */
    public static JIPipeFontFamilySizeStyleColorParameter fromLegacyKeys(JsonNode json,
                                                                         String familyKey, String sizeKey,
                                                                         String styleKey, String colorKey,
                                                                         int defaultSize) {
        JIPipeFontFamilySizeStyleColorParameter result = new JIPipeFontFamilySizeStyleColorParameter();
        String family = null;
        int size = defaultSize;
        int style = Font.PLAIN;
        Color color = Color.WHITE;

        if (familyKey != null && json.has(familyKey) && json.get(familyKey).isTextual()) {
            family = json.get(familyKey).textValue();
        }
        if (sizeKey != null && json.has(sizeKey)) {
            size = json.get(sizeKey).asInt(defaultSize);
        }
        if (styleKey != null && json.has(styleKey) && json.get(styleKey).isTextual()) {
            style = JIPipeFontFamilySizeStyleParameter.styleNameToFlags(json.get(styleKey).textValue());
        }
        if (colorKey != null && json.has(colorKey)) {
            Color c = JIPipeFontFamilySizeStyleParameter.legacyColor(json.get(colorKey));
            if (c != null) {
                color = c;
            }
        }
        result.migrateFrom(family, size, style, color);
        return result;
    }

    private static List<String> availableFamilies() {
        try {
            return Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /**
     * Explicit field setter used by node migrations (#1339, Task 10) to move
     * a node's former separate family/size/style/color parameters into one.
     */
    public void migrateFrom(String family, int size, int style, Color color) {
        this.family = family != null ? family : JIPipeFontFamilySizeStyleParameter.defaultFamily();
        this.size = size;
        this.style = style;
        this.color = color != null ? color : Color.WHITE;
    }

    /**
     * @return the font as an {@link Font} (the color is <em>not</em> part of
     * a {@link Font} — use {@link #getColor()} for it).
     */
    public Font toFont() {
        String f = family;
        if (f == null) {
            f = JIPipeFontFamilySizeStyleParameter.defaultFamily();
        } else if (!availableFamilies().contains(f)) {
            f = JIPipeFontFamilySizeStyleParameter.defaultFamily();
        }
        return new Font(f, style, size);
    }

    @Override
    public JIPipeFontValue copy() {
        return new JIPipeFontFamilySizeStyleColorParameter(this);
    }

    @JsonGetter("family")
    public String getFamily() {
        return family;
    }

    /**
     * @param family the font family name (null resets to the default family)
     */
    @JsonSetter("family")
    public void setFamily(String family) {
        this.family = family != null ? family : JIPipeFontFamilySizeStyleParameter.defaultFamily();
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

    /**
     * @param style the {@link Font} style bit flags
     */
    @JsonSetter("style")
    public void setStyle(int style) {
        this.style = style;
    }

    @JsonGetter("color")
    public Color getColor() {
        return color;
    }

    /**
     * @param color the text color (null resets to white)
     */
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
        JIPipeFontFamilySizeStyleColorParameter that = (JIPipeFontFamilySizeStyleColorParameter) o;
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
