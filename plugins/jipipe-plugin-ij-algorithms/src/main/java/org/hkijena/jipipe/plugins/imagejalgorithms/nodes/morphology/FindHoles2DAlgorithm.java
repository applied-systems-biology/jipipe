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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.morphology;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Find holes 2D", description = "Find holes by extracting the ROI located in the image and utilizing the fact that ROI cannot have holes. " + "If a multi-channel image is provided, the operation is applied to each channel. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Morphology", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nBinary", aliasName = "Find Holes")
public class FindHoles2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean blackBackground = true;

    public FindHoles2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FindHoles2DAlgorithm(FindHoles2DAlgorithm other) {
        super(other);
        this.blackBackground = other.blackBackground;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo).getDuplicateImage();
        // Convert mask to ROI
        ROI2DListData roiList = new ROI2DListData();
        ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
            ImageProcessor ip2 = ip.duplicate();
            int threshold = ip2.isInvertedLut() ? 255 : 0;
            if (isBlackBackground())
                threshold = (threshold == 255) ? 0 : 255;
            ip2.setThreshold(threshold, threshold, ImageProcessor.NO_LUT_UPDATE);
            Roi roi = ThresholdToSelection.run(new ImagePlus("slice", ip2));
            if (roi != null) {
                roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
                roiList.add(roi);
            }
        }, progressInfo);

        // Split into their simple ROI
        roiList.splitAll();

        // Create a mask
        ImagePlus roiMask = roiList.toMask(img, true, false, 1);

        // Subtract mask from roi mask
        ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
            ImageProcessor ip2 = ImageJUtils.getSliceZero(roiMask, index);
            byte[] roiMaskPixels = (byte[]) ip2.getPixels();
            byte[] maskPixels = (byte[]) ip.getPixels();
            for (int i = 0; i < roiMaskPixels.length; i++) {
                maskPixels[i] = (byte) (Math.max(0, Byte.toUnsignedInt(roiMaskPixels[i]) - Byte.toUnsignedInt(maskPixels[i])));
            }
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Black background", description = "If enabled, the background is assumed to be black. Otherwise, white pixels are recognized as background.")
    @JIPipeParameter("black-background")
    public boolean isBlackBackground() {
        return blackBackground;
    }

    @JIPipeParameter("black-background")
    public void setBlackBackground(boolean blackBackground) {
        this.blackBackground = blackBackground;
    }
}
