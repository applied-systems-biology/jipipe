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

package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class ImageBlendLayer extends AbstractJIPipeParameterCollection {

    private int priority = 0;

    private ImageBlendMode blendMode = ImageBlendMode.Screen;
    private double opacity = 1;

    public ImageBlendLayer() {
    }

    public ImageBlendLayer(ImageBlendLayer other) {
        this.priority = other.priority;
        this.blendMode = other.blendMode;
        this.opacity = other.opacity;
    }

    @SetJIPipeDocumentation(name = "Priority (lower = earlier)")
    @JIPipeParameter("order")
    public int getPriority() {
        return priority;
    }

    @JIPipeParameter("order")
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @SetJIPipeDocumentation(name = "Blend mode")
    @JIPipeParameter("blend-mode")
    public ImageBlendMode getBlendMode() {
        return blendMode;
    }

    @JIPipeParameter("blend-mode")
    public void setBlendMode(ImageBlendMode blendMode) {
        this.blendMode = blendMode;
    }

    @SetJIPipeDocumentation(name = "Opacity")
    @JIPipeParameter("opacity")
    @JsonGetter("opacity")
    public double getOpacity() {
        return opacity;
    }

    @JIPipeParameter("opacity")
    @JsonSetter("opacity")
    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }
}
