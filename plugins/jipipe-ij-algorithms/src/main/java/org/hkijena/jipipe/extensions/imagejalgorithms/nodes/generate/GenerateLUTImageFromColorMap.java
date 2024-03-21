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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.generate;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.colors.ColorMap;

import java.awt.image.BufferedImage;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Render color map", description = "Creates a new image that renders the contents of a color map as RGB image.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
public class GenerateLUTImageFromColorMap extends JIPipeSimpleIteratingAlgorithm {

    private int width = 256;
    private int height = 1;
    private ColorMap colorMap = ColorMap.viridis;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GenerateLUTImageFromColorMap(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GenerateLUTImageFromColorMap(GenerateLUTImageFromColorMap other) {
        super(other);
        this.width = other.width;
        this.height = other.height;
        this.colorMap = other.colorMap;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = IJ.createImage(colorMap.name(), width, height, 1, 24);
        BufferedImage mapImage = colorMap.getMapImage();
        ColorProcessor processor = (ColorProcessor) img.getProcessor();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int lutIndex = (int) (x * 1.0 / img.getWidth() * mapImage.getWidth());
                processor.set(x, y, mapImage.getRGB(lutIndex, 0));
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Width", description = "The width of the generated image")
    @JIPipeParameter(value = "width", uiOrder = -20)
    public int getWidth() {
        return width;
    }

    @JIPipeParameter("width")
    public void setWidth(int width) {
        this.width = width;
    }

    @SetJIPipeDocumentation(name = "Height", description = "The height of the generated image")
    @JIPipeParameter(value = "height", uiOrder = -15)
    public int getHeight() {
        return height;
    }

    @JIPipeParameter("height")
    public void setHeight(int height) {
        this.height = height;
    }

    @SetJIPipeDocumentation(name = "Color map", description = "The color map that should be rendered")
    @JIPipeParameter("color-map")
    public ColorMap getColorMap() {
        return colorMap;
    }

    @JIPipeParameter("color-map")
    public void setColorMap(ColorMap colorMap) {
        this.colorMap = colorMap;
    }
}
