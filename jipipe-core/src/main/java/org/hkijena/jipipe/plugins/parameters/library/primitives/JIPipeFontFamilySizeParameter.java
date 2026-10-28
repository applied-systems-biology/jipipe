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

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The barest member of the font parameter family (#1339): family + size
 * only. Nodes that neither style nor color their text (or take both from
 * elsewhere) use this. The family's shared statics live in
 * {@link JIPipeFontFamilySizeStyleParameter}; the {@link #toFont()} of this
 * class always renders with {@link Font#PLAIN}.
 * <p>
 * Both fields are always present (never null) — missing JSON keys fall back
 * to defaults on deserialization, which keeps old projects (that stored the
 * fields separately, if at all) loadable.
 */
public class JIPipeFontFamilySizeParameter implements JIPipeFontValue {

    private String family = JIPipeFontFamilySizeStyleParameter.defaultFamily();
    private int size = 12;

    /**
     * Creates a new instance with the default font (default family, size 12).
     */
    public JIPipeFontFamilySizeParameter() {
    }

    /**
     * Copies another font parameter.
     */
    public JIPipeFontFamilySizeParameter(JIPipeFontFamilySizeParameter other) {
        this.family = other.family;
        this.size = other.size;
    }

    /**
     * Composes the legacy split font fields (from a raw node/collection JSON
     * object) into a single {@link JIPipeFontFamilySizeParameter} for a
     * given set of parameter keys. Absent keys fall back to defaults (family,
     * {@code defaultSize}). This is the idempotent migration path: a node's
     * {@code onDeserialized} (or a nested collection's {@code fromJson})
     * calls this when the legacy keys are still present in the loaded JSON
     * (a project saved before the migration). The {@code styleKey} and
     * {@code colorKey} are ignored by this class (it carries neither field).
     *
     * @param json        the raw JSON object that holds the parameters
     * @param familyKey   the legacy family key (a textual node, e.g. {@code "font-family"})
     * @param sizeKey     the legacy size key (a number, e.g. {@code "font-size"})
     * @param styleKey    the legacy style key (ignored by this class); may be null
     * @param colorKey    the legacy color key (ignored by this class); may be null
     * @param defaultSize the size to use when {@code sizeKey} is absent
     * @return the composed font parameter
     */
    public static JIPipeFontFamilySizeParameter fromLegacyKeys(JsonNode json,
                                                               String familyKey, String sizeKey,
                                                               String styleKey, String colorKey,
                                                               int defaultSize) {
        JIPipeFontFamilySizeParameter result = new JIPipeFontFamilySizeParameter();
        String family = null;
        int size = defaultSize;

        if (familyKey != null && json.has(familyKey) && json.get(familyKey).isTextual()) {
            family = json.get(familyKey).textValue();
        }
        if (sizeKey != null && json.has(sizeKey)) {
            size = json.get(sizeKey).asInt(defaultSize);
        }
        // styleKey and colorKey are not fields of this class — intentionally ignored

        result.setFamily(family);
        result.setSize(size);
        return result;
    }

    /**
     * @return the font as an {@link Font} with {@link Font#PLAIN} style
     * (unavailable families fall back to the default family).
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

    private static List<String> availableFamilies() {
        try {
            return Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    @Override
    public JIPipeFontValue copy() {
        return new JIPipeFontFamilySizeParameter(this);
    }

    @JsonGetter("family")
    public String getFamily() {
        return family;
    }

    /** @param family the font family name (null resets to the default family) */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JIPipeFontFamilySizeParameter that = (JIPipeFontFamilySizeParameter) o;
        return size == that.size
                && Objects.equals(family, that.family);
    }

    @Override
    public int hashCode() {
        return Objects.hash(family, size);
    }

    @Override
    public String toString() {
        return family + " " + size;
    }
}
