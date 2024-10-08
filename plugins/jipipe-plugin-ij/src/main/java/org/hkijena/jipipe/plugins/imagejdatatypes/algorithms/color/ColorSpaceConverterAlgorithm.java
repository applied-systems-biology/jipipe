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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.color;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;

public abstract class ColorSpaceConverterAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final Class<? extends ImagePlusData> outputDataType;
    private final ColorSpace outputColorSpace;
    private boolean reinterpret = false;

    public ColorSpaceConverterAlgorithm(JIPipeNodeInfo info, Class<? extends ImagePlusData> outputDataType) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", "", ImagePlusData.class)
                .addOutputSlot("Output", "", outputDataType)
                .seal()
                .build());
        this.outputDataType = outputDataType;
        this.outputColorSpace = ImagePlusData.getColorSpaceOf(outputDataType);
    }

    public ColorSpaceConverterAlgorithm(ColorSpaceConverterAlgorithm other) {
        super(other);
        this.outputDataType = other.outputDataType;
        this.reinterpret = other.reinterpret;
        this.outputColorSpace = other.outputColorSpace;
    }

    @SetJIPipeDocumentation(name = "Reinterpret channels", description = "If enabled, the image channels are reinterpreted instead of converted.")
    @JIPipeParameter("reinterpret-channels")
    public boolean isReinterpret() {
        return reinterpret;
    }

    @JIPipeParameter("reinterpret-channels")
    public void setReinterpret(boolean reinterpret) {
        this.reinterpret = reinterpret;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData input = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus image = input.getImage();
        if (image.getType() != ImagePlus.COLOR_RGB || reinterpret) {
            // Convert to RGB via native method or reinterpretation method
            ImagePlusData outputData = JIPipe.createData(outputDataType, image);
            iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
            return;
        }

        ColorSpace inputColorSpace = input.getColorSpace();

        // Use color space to convert
        image = input.getDuplicateImage();
        outputColorSpace.convert(image, inputColorSpace, progressInfo.resolve("Converting between color spaces"));
        ImagePlusData outputData = JIPipe.createData(outputDataType, image);
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
