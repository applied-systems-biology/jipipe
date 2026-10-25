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
