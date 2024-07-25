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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorHSBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;


@SetJIPipeDocumentation(name = "Merge HSB channels", description = "Merges three greyscale images into one HSB image.")
@ConfigureJIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "H", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "S", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "B", create = true)
@AddJIPipeOutputSlot(value = ImagePlusColorHSBData.class, name = "HSB", create = true)
public class MergeHSBChannelsAlgorithm extends JIPipeIteratingAlgorithm {


    public MergeHSBChannelsAlgorithm(JIPipeNodeInfo info) {
      super(info);
    }

    public MergeHSBChannelsAlgorithm(MergeHSBChannelsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imageH = iterationStep.getInputData("H", ImagePlusGreyscale8UData.class, progressInfo).getImage();
        ImagePlus imageS = iterationStep.getInputData("S", ImagePlusGreyscale8UData.class, progressInfo).getImage();
        ImagePlus imageB = iterationStep.getInputData("B", ImagePlusGreyscale8UData.class, progressInfo).getImage();
        if(!ImageJUtils.imagesHaveSameSize(imageH, imageS, imageB)) {
            throw new IllegalArgumentException("Images do not have the same size!");
        }
        ImagePlus output = IJ.createHyperStack("HSB", imageH.getWidth(), imageH.getHeight(), imageH.getNChannels(), imageH.getNSlices(), imageH.getNFrames(), 24);
        output.copyScale(imageH);
        ImageJUtils.forEachIndexedZCTSlice(output, (targetIp, index) -> {
            byte[] sourceH = (byte[]) ImageJUtils.getSliceZero(imageH, index).getPixels();
            byte[] sourceS = (byte[]) ImageJUtils.getSliceZero(imageS, index).getPixels();
            byte[] sourceB = (byte[]) ImageJUtils.getSliceZero(imageB, index).getPixels();
            int[] target = (int[]) targetIp.getPixels();
            for (int i = 0; i < target.length; i++) {
                int c0 = Byte.toUnsignedInt(sourceH[i]);
                int c1 = Byte.toUnsignedInt(sourceS[i]);
                int c2 = Byte.toUnsignedInt(sourceB[i]);
                target[i] = (c0 << 16) + (c1 << 8) + c2;
            }

        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorHSBData(output), progressInfo);
    }

}
