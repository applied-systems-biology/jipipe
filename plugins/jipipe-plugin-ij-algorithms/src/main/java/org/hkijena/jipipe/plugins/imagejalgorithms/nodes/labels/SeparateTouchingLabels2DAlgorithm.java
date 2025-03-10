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
import ij.process.ImageProcessor;
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
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood2D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.BitDepth;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Separate touching labels", description = "Finds pixels that border two different label values and sets them to the background value. " +
        "Background pixels (value 0) are ignored.")
@ConfigureJIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Labels", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Labels", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Borders", create = true, description = "The detected borders")
public class SeparateTouchingLabels2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Neighborhood2D neighborhood = Neighborhood2D.EightConnected;

    public SeparateTouchingLabels2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SeparateTouchingLabels2DAlgorithm(SeparateTouchingLabels2DAlgorithm other) {
        super(other);
        this.neighborhood = other.neighborhood;
    }

    public static void apply(ImageProcessor ip, ImageProcessor outputIp, ImageProcessor borderIp, Neighborhood2D neighborhood) {
        final int height = ip.getHeight();
        final int width = ip.getWidth();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int current = (int) ip.getf(x, y);

                if (current == 0) {
                    continue;
                }

                outer:
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dy == 0 && dx == 0) {
                            continue;
                        }
                        if ((x + dx < 0 || y + dy < 0 || x + dx >= width || y + dy >= height)) {
                            continue;
                        }
                        if (neighborhood == Neighborhood2D.FourConnected) {
                            if (Math.abs(dy) + Math.abs(dx) != 1) {
                                continue; // skip diagonals (only horizontal or vertical steps allowed)
                            }
                        }

                        int other = (int) ip.getf(x + dx, y + dy);
                        if (other == 0) {
                            continue;
                        }

                        if (current != other) {
                            borderIp.set(x, y, 255);
                            outputIp.setf(x, y, 0);
                            break outer;
                        }
                    }
                }

            }
        }

    }

    @SetJIPipeDocumentation(name = "Neighborhood", description = "The neighborhood of each pixel that is checked")
    @JIPipeParameter("neighborhood")
    public Neighborhood2D getNeighborhood() {
        return neighborhood;
    }

    @JIPipeParameter("neighborhood")
    public void setNeighborhood(Neighborhood2D neighborhood) {
        this.neighborhood = neighborhood;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus outputImage = inputImage.duplicate();
        ImagePlus borders = ImageJUtils.newBlankOf(inputImage, BitDepth.Grayscale8u);

        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ImageProcessor outputIp = ImageJUtils.getSliceZero(outputImage, index);
            ImageProcessor borderIp = ImageJUtils.getSliceZero(borders, index);

            apply(ip, outputIp, borderIp, neighborhood);

        }, progressInfo);

        outputImage.copyScale(inputImage);
        borders.copyScale(outputImage);
        iterationStep.addOutputData("Labels", new ImagePlusGreyscaleData(outputImage), progressInfo);
        iterationStep.addOutputData("Borders", new ImagePlusGreyscaleMaskData(borders), progressInfo);
    }
}
