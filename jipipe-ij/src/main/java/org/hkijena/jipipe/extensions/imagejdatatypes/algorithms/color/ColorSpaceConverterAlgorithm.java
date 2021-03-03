package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ColoredImagePlusData;

public abstract class ColorSpaceConverterAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final Class<? extends ColoredImagePlusData> outputDataType;
    private final ColorSpace outputColorSpace;
    private boolean reinterpret = false;

    public ColorSpaceConverterAlgorithm(JIPipeNodeInfo info, Class<? extends ColoredImagePlusData> outputDataType) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
        .addInputSlot("Input", ImagePlusData.class)
        .addOutputSlot("Output", outputDataType, null)
        .seal()
        .build());
        this.outputDataType = outputDataType;
        try {
            this.outputColorSpace = (ColorSpace) outputDataType.getDeclaredField("COLOR_SPACE").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public ColorSpaceConverterAlgorithm(ColorSpaceConverterAlgorithm other) {
        super(other);
        this.outputDataType = other.outputDataType;
        this.reinterpret = other.reinterpret;
        this.outputColorSpace = other.outputColorSpace;
    }

    @JIPipeDocumentation(name = "Reinterpret channels", description = "If enabled, the image channels are reinterpreted instead of converted.")
    @JIPipeParameter("reinterpret-channels")
    public boolean isReinterpret() {
        return reinterpret;
    }

    @JIPipeParameter("reinterpret-channels")
    public void setReinterpret(boolean reinterpret) {
        this.reinterpret = reinterpret;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData input = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus image = input.getImage();
        if(image.getType() != ImagePlus.COLOR_RGB || reinterpret) {
            // Convert to RGB via native method or reinterpretation method
            ImagePlusData outputData = (ImagePlusData) JIPipe.createData(outputDataType, image);
            dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
            return;
        }

        ColorSpace inputColorSpace;

        if(input instanceof ColoredImagePlusData) {
            inputColorSpace = ((ColoredImagePlusData) input).getColorSpace();
        }
        else {
            // Fallback to RGB (default)
            inputColorSpace = new RGBColorSpace();
        }

        // Use color space to convert
        image = input.getDuplicateImage();
        outputColorSpace.convert(image, inputColorSpace, progressInfo.resolve("Converting between color spaces"));
        ImagePlusData outputData = (ImagePlusData) JIPipe.createData(outputDataType, image);
        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
