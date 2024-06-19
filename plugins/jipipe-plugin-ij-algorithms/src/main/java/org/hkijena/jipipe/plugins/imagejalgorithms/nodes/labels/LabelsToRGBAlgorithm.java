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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.labels;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.color.ColorMaps;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

import java.awt.*;

@SetJIPipeDocumentation(name = "Convert labels to RGB", description = "Visualizes a labels image by assigning a color to each label component")
@ConfigureJIPipeNode(menuPath = "Labels\nConvert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Labels", create = true)
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nLabel Images", aliasName = "Labels to RGB")
public class LabelsToRGBAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ColorMaps.CommonLabelMaps colorMap = ColorMaps.CommonLabelMaps.MAIN_COLORS;
    private Color backgroundColor = Color.BLACK;
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

    private static int computeMaxLabel(ImagePlus imagePlus) {
        if (imagePlus.getImageStackSize() == 1) {
            return computeMaxLabel(imagePlus.getProcessor());
        } else {
            return computeMaxLabel(imagePlus.getStack());
        }
    }

    private static int computeMaxLabel(ImageProcessor image) {
        int labelMax = 0;
        if (image instanceof FloatProcessor) {
            for (int i = 0; i < image.getPixelCount(); i++) {
                labelMax = Math.max(labelMax, (int) image.getf(i));
            }
        } else {
            for (int i = 0; i < image.getPixelCount(); i++) {
                labelMax = Math.max(labelMax, image.get(i));
            }
        }

        return labelMax;
    }

    private static int computeMaxLabel(ImageStack image) {
        int labelMax = 0;
        for (int i = 1; i <= image.getSize(); i++) {
            ImageProcessor slice = image.getProcessor(i);
            labelMax = Math.max(labelMax, computeMaxLabel(slice));
        }

        return labelMax;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        int maxLabel = computeMaxLabel(inputImage);
        byte[][] lut = colorMap.computeLut(maxLabel, shuffleLut);
        ImagePlus outputImage = LabelImages.labelToRgb(inputImage, lut, backgroundColor);
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(outputImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Color map", description = "The color map that assigns colors to labels")
    @JIPipeParameter("color-map")
    @EnumParameterSettings(itemInfo = LabelColorMapEnumItemInfo.class)
    public ColorMaps.CommonLabelMaps getColorMap() {
        return colorMap;
    }

    @JIPipeParameter("color-map")
    public void setColorMap(ColorMaps.CommonLabelMaps colorMap) {
        this.colorMap = colorMap;
    }

    @SetJIPipeDocumentation(name = "Background color", description = "The background color for non-labels (zero values)")
    @JIPipeParameter("background-color")
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @SetJIPipeDocumentation(name = "Shuffle LUT", description = "If enabled, shuffles the LUT randomly")
    @JIPipeParameter("shuffle-lut")
    public boolean isShuffleLut() {
        return shuffleLut;
    }

    @JIPipeParameter("shuffle-lut")
    public void setShuffleLut(boolean shuffleLut) {
        this.shuffleLut = shuffleLut;
    }
}
