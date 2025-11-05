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

package org.hkijena.jipipe.desktop.commons.components.color.palette;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JIPipeDesktopSimpleColorPaletteUserColorStorage implements JIPipeDesktopColorPaletteUserColorStorage {
    @JsonProperty("colors")
    private List<JIPipeDesktopColorPaletteColor> colors = new ArrayList<>();

    @Override
    public void addColor(JIPipeDesktopColorPaletteColor color) {
        colors.add(new JIPipeDesktopColorPaletteColor(color));
    }

    @Override
    public List<JIPipeDesktopColorPaletteColor> getColors() {
        return Collections.unmodifiableList(colors);
    }

    @Override
    public void setColors(List<JIPipeDesktopColorPaletteColor> colors) {
        this.colors = new ArrayList<>(colors);
    }

    @Override
    public void removeColor(JIPipeDesktopColorPaletteColor color) {
        this.colors.remove(color);
    }

    @Override
    public void replaceColor(JIPipeDesktopColorPaletteColor oldColor, JIPipeDesktopColorPaletteColor newColor) {
        int i = this.colors.indexOf(oldColor);
        if (i >= 0) {
            this.colors.set(i, newColor);
        }
    }
}
