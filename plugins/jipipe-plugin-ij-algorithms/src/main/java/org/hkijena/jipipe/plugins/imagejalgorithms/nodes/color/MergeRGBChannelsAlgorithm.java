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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Merge RGB channels", description = "Merges three greyscale images into one RGB image.")
@ConfigureJIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "R", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "G", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "B", create = true)
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, name = "RGB", create = true)
public class MergeRGBChannelsAlgorithm extends JIPipeIteratingAlgorithm {


    public MergeRGBChannelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeRGBChannelsAlgorithm(MergeRGBChannelsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imageR = iterationStep.getInputData("R", ImagePlusGreyscale8UData.class, progressInfo).getImage();
        ImagePlus imageG = iterationStep.getInputData("G", ImagePlusGreyscale8UData.class, progressInfo).getImage();
        ImagePlus imageB = iterationStep.getInputData("B", ImagePlusGreyscale8UData.class, progressInfo).getImage();
        if (!ImageJUtils.imagesHaveSameSize(imageR, imageG, imageB)) {
            throw new IllegalArgumentException("Images do not have the same size!");
        }
        ImagePlus output = IJ.createHyperStack("RGB", imageR.getWidth(), imageR.getHeight(), imageR.getNChannels(), imageR.getNSlices(), imageR.getNFrames(), 24);
        output.copyScale(imageR);
        ImageJIterationUtils.forEachIndexedZCTSlice(output, (targetIp, index) -> {
            byte[] sourceR = (byte[]) ImageJUtils.getSliceZero(imageR, index).getPixels();
            byte[] sourceG = (byte[]) ImageJUtils.getSliceZero(imageG, index).getPixels();
            byte[] sourceB = (byte[]) ImageJUtils.getSliceZero(imageB, index).getPixels();
            int[] target = (int[]) targetIp.getPixels();
            for (int i = 0; i < target.length; i++) {
                int c0 = Byte.toUnsignedInt(sourceR[i]);
                int c1 = Byte.toUnsignedInt(sourceG[i]);
                int c2 = Byte.toUnsignedInt(sourceB[i]);
                target[i] = (c0 << 16) + (c1 << 8) + c2;
            }

        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(output), progressInfo);
    }

}
