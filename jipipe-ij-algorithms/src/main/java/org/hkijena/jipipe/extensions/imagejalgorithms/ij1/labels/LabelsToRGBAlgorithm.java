/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.labels;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.color.ColorMaps;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

import java.awt.Color;

@JIPipeDocumentation(name = "Convert labels to RGB", description = "Visualizes a labels image by assigning a color to each label component")
@JIPipeOrganization(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", autoCreate = true)
public class LabelsToRGBAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ColorMaps.CommonLabelMaps colorMap = ColorMaps.CommonLabelMaps.MAIN_COLORS;
    private Color backgroundColor = Color.WHITE;
    private boolean shuffleLut = true;

    public LabelsToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LabelsToRGBAlgorithm(LabelsToRGBAlgorithm other) {
        super(other);
        this.colorMap = other.colorMap;
        this.backgroundColor = other.backgroundColor;
        this.shuffleLut = other.shuffleLut;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        int maxLabel = computeMaxLabel(inputImage);
        byte[][] lut = colorMap.computeLut(maxLabel, shuffleLut);
        ImagePlus outputImage = LabelImages.labelToRgb(inputImage, lut, backgroundColor);
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(outputImage), progressInfo);
    }

    @JIPipeDocumentation(name = "Color map", description = "The color map that assigns colors to labels")
    @JIPipeParameter("color-map")
    public ColorMaps.CommonLabelMaps getColorMap() {
        return colorMap;
    }

    @JIPipeParameter("color-map")
    public void setColorMap(ColorMaps.CommonLabelMaps colorMap) {
        this.colorMap = colorMap;
    }

    @JIPipeDocumentation(name = "Background color", description = "The background color for non-labels (zero values)")
    @JIPipeParameter("background-color")
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @JIPipeDocumentation(name = "Shuffle LUT", description = "If enabled, shuffles the LUT randomly")
    @JIPipeParameter("shuffle-lut")
    public boolean isShuffleLut() {
        return shuffleLut;
    }

    @JIPipeParameter("shuffle-lut")
    public void setShuffleLut(boolean shuffleLut) {
        this.shuffleLut = shuffleLut;
    }

    private static int computeMaxLabel(ImagePlus imagePlus)
    {
        if (imagePlus.getImageStackSize() == 1)
        {
            return computeMaxLabel(imagePlus.getProcessor());
        }
        else
        {
            return computeMaxLabel(imagePlus.getStack());
        }
    }

    private static int computeMaxLabel(ImageProcessor image)
    {
        int labelMax = 0;
        if (image instanceof FloatProcessor)
        {
            for (int i = 0; i < image.getPixelCount(); i++)
            {
                labelMax = Math.max(labelMax, (int) image.getf(i));
            }
        }
        else
        {
            for (int i = 0; i < image.getPixelCount(); i++)
            {
                labelMax = Math.max(labelMax, image.get(i));
            }
        }

        return labelMax;
    }

    private static int computeMaxLabel(ImageStack image)
    {
        int labelMax = 0;
        for (int i = 1; i <= image.getSize(); i++)
        {
            ImageProcessor slice = image.getProcessor(i);
            labelMax = Math.max(labelMax, computeMaxLabel(slice));
        }

        return labelMax;
    }
}
