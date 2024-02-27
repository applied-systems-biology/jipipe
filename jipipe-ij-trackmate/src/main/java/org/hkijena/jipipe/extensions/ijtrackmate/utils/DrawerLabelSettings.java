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

package org.hkijena.jipipe.extensions.ijtrackmate.utils;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;

import java.awt.*;

public class DrawerLabelSettings extends AbstractJIPipeParameterCollection {
    private boolean drawLabels = true;
    private Color labelForeground = Color.WHITE;
    private OptionalColorParameter labelBackground = new OptionalColorParameter(Color.BLACK, true);
    private int labelSize = 12;

    public DrawerLabelSettings() {
    }

    public DrawerLabelSettings(DrawerLabelSettings other) {
        this.drawLabels = other.drawLabels;
        this.labelForeground = other.labelForeground;
        this.labelBackground = new OptionalColorParameter(other.labelBackground);
        this.labelSize = other.labelSize;
    }

    @SetJIPipeDocumentation(name = "Label foreground", description = "The text color of the label (if enabled)")
    @JIPipeParameter("label-foreground")
    public Color getLabelForeground() {
        return labelForeground;
    }

    @JIPipeParameter("label-foreground")
    public void setLabelForeground(Color labelForeground) {
        this.labelForeground = labelForeground;
    }

    @SetJIPipeDocumentation(name = "Label background", description = "The background color of the label (if enabled)")
    @JIPipeParameter("label-background")
    public OptionalColorParameter getLabelBackground() {
        return labelBackground;
    }

    @JIPipeParameter("label-background")
    public void setLabelBackground(OptionalColorParameter labelBackground) {
        this.labelBackground = labelBackground;
    }

    @SetJIPipeDocumentation(name = "Label size", description = "Font size of drawn labels")
    @JIPipeParameter("label-size")
    public int getLabelSize() {
        return labelSize;
    }

    @JIPipeParameter("label-size")
    public boolean setLabelSize(int labelSize) {
        if (labelSize < 1)
            return false;
        this.labelSize = labelSize;
        return true;
    }

    @SetJIPipeDocumentation(name = "Draw labels", description = "If enabled, draw labels on top of the objects")
    @JIPipeParameter("draw-labels")
    public boolean isDrawLabels() {
        return drawLabels;
    }

    @JIPipeParameter("draw-labels")
    public void setDrawLabels(boolean drawLabels) {
        this.drawLabels = drawLabels;
    }
}
