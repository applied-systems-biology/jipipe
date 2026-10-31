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
 * A family+size+color font parameter (#1339): the lighter member of the
 * family for nodes that draw plain-style text in a fixed color. The family's
 * shared statics live in {@link JIPipeFontFamilySizeStyleParameter}; the
 * {@link #toFont()} of this class always renders with {@link Font#PLAIN}.
 * <p>
 * All three fields are always present (never null) - missing JSON keys fall
 * back to defaults on deserialization, which keeps old projects (that stored
 * the fields separately, if at all) loadable. The {@code Color} field uses
 * the mapper's standard hex-string codec, like the {@code color} parameter
 * type.
 */
public class JIPipeFontFamilySizeColorParameter implements JIPipeFontValue, JIPipeFontColorCapable {

    private String family = JIPipeFontFamilySizeStyleParameter.defaultFamily();
    private int size = 12;
    private Color color = Color.WHITE;

    /**
     * Creates a new instance with the default font (default family, size 12,
     * white).
     */
    public JIPipeFontFamilySizeColorParameter() {
    }

    /**
     * Copies another font parameter.
     */
    public JIPipeFontFamilySizeColorParameter(JIPipeFontFamilySizeColorParameter other) {
        this.family = other.family;
        this.size = other.size;
        this.color = other.color;
    }

    /**
     * Composes the legacy split font fields (from a raw node/collection JSON
     * object) into a single {@link JIPipeFontFamilySizeColorParameter} for a
     * given set of parameter keys. Absent keys fall back to defaults (family,
     * {@code defaultSize}, white). This is the idempotent migration path: a
     * node's {@code onDeserialized} (or a nested collection's
     * {@code fromJson}) calls this when the legacy keys are still present in
     * the loaded JSON (a project saved before the migration). The
     * {@code styleKey} is ignored by this class (it carries no style).
     *
     * @param json        the raw JSON object that holds the parameters
     * @param familyKey   the legacy family key (a textual node, e.g. {@code "font-family"})
     * @param sizeKey     the legacy size key (a number, e.g. {@code "font-size"})
     * @param styleKey    the legacy style key (ignored by this class); may be null
     * @param colorKey    the legacy color key (a hex/OptionalColor node); may be null
     * @param defaultSize the size to use when {@code sizeKey} is absent
     * @return the composed font parameter
     */
    public static JIPipeFontFamilySizeColorParameter fromLegacyKeys(JsonNode json,
                                                                    String familyKey, String sizeKey,
                                                                    String styleKey, String colorKey,
                                                                    int defaultSize) {
        JIPipeFontFamilySizeColorParameter result = new JIPipeFontFamilySizeColorParameter();
        String family = null;
        int size = defaultSize;
        Color color = Color.WHITE;

        if (familyKey != null && json.has(familyKey) && json.get(familyKey).isTextual()) {
            family = json.get(familyKey).textValue();
        }
        if (sizeKey != null && json.has(sizeKey)) {
            size = json.get(sizeKey).asInt(defaultSize);
        }
        if (colorKey != null && json.has(colorKey)) {
            Color c = JIPipeFontFamilySizeStyleParameter.legacyColor(json.get(colorKey));
            if (c != null) {
                color = c;
            }
        }
        // styleKey is not a field of this class - intentionally ignored

        result.setFamily(family);
        result.setSize(size);
        result.setColor(color);
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
     * @return the font as an {@link Font} with {@link Font#PLAIN} style
     * (unavailable families fall back to the default family; the color is
     * <em>not</em> part of a {@link Font} - use {@link #getColor()} for it).
     */
    public Font toFont() {
        String f = family;
        if (f == null) {
            f = JIPipeFontFamilySizeStyleParameter.defaultFamily();
        } else if (!availableFamilies().contains(f)) {
            f = JIPipeFontFamilySizeStyleParameter.defaultFamily();
        }
        return new Font(f, Font.PLAIN, size);
    }

    @Override
    public JIPipeFontValue copy() {
        return new JIPipeFontFamilySizeColorParameter(this);
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
        JIPipeFontFamilySizeColorParameter that = (JIPipeFontFamilySizeColorParameter) o;
        return size == that.size
                && Objects.equals(family, that.family)
                && Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(family, size, color);
    }

    @Override
    public String toString() {
        return family + " " + size + " (" + color + ")";
    }
}
