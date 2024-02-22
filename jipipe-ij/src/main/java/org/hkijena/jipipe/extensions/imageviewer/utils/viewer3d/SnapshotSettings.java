package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class SnapshotSettings extends AbstractJIPipeParameterCollection {
    private int width = 800;
    private int height = 600;

    @SetJIPipeDocumentation(name = "Width", description = "The width of the output image")
    @JIPipeParameter(value = "width", uiOrder = -100)
    public int getWidth() {
        return width;
    }

    @JIPipeParameter("width")
    public void setWidth(int width) {
        this.width = width;
    }

    @SetJIPipeDocumentation(name = "Height", description = "The height of the image")
    @JIPipeParameter(value = "height", uiOrder = -90)
    public int getHeight() {
        return height;
    }

    @JIPipeParameter("height")
    public void setHeight(int height) {
        this.height = height;
    }
}
