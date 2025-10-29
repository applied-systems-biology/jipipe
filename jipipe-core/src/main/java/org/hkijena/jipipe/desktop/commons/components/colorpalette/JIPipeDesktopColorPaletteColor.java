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

package org.hkijena.jipipe.desktop.commons.components.colorpalette;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.*;
import java.util.Objects;

public class JIPipeDesktopColorPaletteColor {
    @JsonProperty("foreground")
    private Color foreground;
    @JsonProperty("background")
    private Color background;

    public JIPipeDesktopColorPaletteColor() {
    }

    public JIPipeDesktopColorPaletteColor(Color color) {
        this.foreground = color;
        this.background = color;
    }

    public JIPipeDesktopColorPaletteColor(Color foreground, Color background) {
        this.foreground = foreground;
        this.background = background;
    }

    public JIPipeDesktopColorPaletteColor(JIPipeDesktopColorPaletteColor other) {
        this.foreground = other.foreground;
        this.background = other.background;
    }

    public Color getForeground() {
        return foreground;
    }

    public void setForeground(Color foreground) {
        this.foreground = foreground;
    }

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeDesktopColorPaletteColor that = (JIPipeDesktopColorPaletteColor) o;
        return Objects.equals(foreground, that.foreground) && Objects.equals(background, that.background);
    }

    @Override
    public int hashCode() {
        return Objects.hash(foreground, background);
    }
}
