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

package org.hkijena.jipipe.desktop.commons.components.color;

import java.awt.*;
import java.util.Objects;

/**
 * A color with an optional user-visible name (e.g. "Blue 3" from the GNOME
 * HIG palette, or a name the user gave to a saved color).
 */
public final class NamedColor {

    private final Color color;
    private final String name;

    public NamedColor(Color color, String name) {
        this.color = color;
        this.name = name;
    }

    public Color getColor() {
        return color;
    }

    /**
     * The name, or null.
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedColor that = (NamedColor) o;
        return color.getRGB() == that.color.getRGB();
    }

    @Override
    public int hashCode() {
        return Objects.hash(color.getRGB());
    }

    @Override
    public String toString() {
        return name != null ? name : color.toString();
    }
}
