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

package org.hkijena.jipipe.plugins.imp.nodes;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imp.datatypes.ImpImageData;
import org.hkijena.jipipe.plugins.imp.utils.ImpImageUtils;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

@SetJIPipeDocumentation(name = "Convert IMP to ImageJ", description = "Converts an IMP image (which supports transparency) to " +
        "an ImageJ1 image")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "IMP\nConvert")
@AddJIPipeInputSlot(value = ImpImageData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
public class ConvertImpImageToImagePlusAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private boolean createCheckerboard = true;
    private int checkerboardSize = 10;
    private Color checkerboardColor1 = Color.WHITE;
    private Color checkerboardColor2 = JIPipeDesktopModernMetalTheme.GRAY;

    public ConvertImpImageToImagePlusAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertImpImageToImagePlusAlgorithm(ConvertImpImageToImagePlusAlgorithm other) {
        super(other);
        this.createCheckerboard = other.createCheckerboard;
        this.checkerboardSize = other.checkerboardSize;
        this.checkerboardColor1 = other.checkerboardColor1;
        this.checkerboardColor2 = other.checkerboardColor2;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImpImageData inputData = iterationStep.getInputData(getFirstInputSlot(), ImpImageData.class, progressInfo);
        ImageStack stack = new ImageStack(inputData.getWidth(), inputData.getHeight(), inputData.getSize());
        ImpImageUtils.forEachIndexedZCTSlice(inputData, (ip, index) -> {
            BufferedImage image;
            if (createCheckerboard) {
                image = BufferedImageUtils.convertAlphaToCheckerboard(ip, checkerboardSize, checkerboardColor1, checkerboardColor2);
            } else {
                image = ip;
            }
            ColorProcessor processor = new ColorProcessor(image);
            stack.setProcessor(processor, index.zeroSliceIndexToOneStackIndex(inputData.getNumChannels(),
                    inputData.getNumSlices(),
                    inputData.getNumFrames()));
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(new ImagePlus("Image", stack)), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Replace transparency by checkerboard", description = "If enabled, the transparent parts are replaced by a checkerboard")
    @JIPipeParameter("create-checkerboard")
    public boolean isCreateCheckerboard() {
        return createCheckerboard;
    }

    @JIPipeParameter("create-checkerboard")
    public void setCreateCheckerboard(boolean createCheckerboard) {
        this.createCheckerboard = createCheckerboard;
    }

    @SetJIPipeDocumentation(name = "Checkerboard tile size", description = "The size of one checkerboard tile")
    @JIPipeParameter("checkerboard-size")
    public int getCheckerboardSize() {
        return checkerboardSize;
    }

    @JIPipeParameter("checkerboard-size")
    public void setCheckerboardSize(int checkerboardSize) {
        this.checkerboardSize = checkerboardSize;
    }

    @SetJIPipeDocumentation(name = "Checkerboard color 1", description = "The first color of the checkerboard pattern")
    @JIPipeParameter("checkerboard-color-1")
    public Color getCheckerboardColor1() {
        return checkerboardColor1;
    }

    @JIPipeParameter("checkerboard-color-1")
    public void setCheckerboardColor1(Color checkerboardColor1) {
        this.checkerboardColor1 = checkerboardColor1;
    }

    @SetJIPipeDocumentation(name = "Checkerboard color 2", description = "The second color of the checkerboard pattern")
    @JIPipeParameter("checkerboard-color-2")
    public Color getCheckerboardColor2() {
        return checkerboardColor2;
    }

    @JIPipeParameter("checkerboard-color-2")
    public void setCheckerboardColor2(Color checkerboardColor2) {
        this.checkerboardColor2 = checkerboardColor2;
    }
}
