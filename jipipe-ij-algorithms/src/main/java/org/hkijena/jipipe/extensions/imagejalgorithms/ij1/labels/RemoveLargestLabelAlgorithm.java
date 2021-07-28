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
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

@JIPipeDocumentation(name = "Remove largest label", description = "Remove the label with the largest area")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class RemoveLargestLabelAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int iterations = 1;

    public RemoveLargestLabelAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveLargestLabelAlgorithm(RemoveLargestLabelAlgorithm other) {
        super(other);
        this.iterations = other.iterations;
    }

    @JIPipeDocumentation(name = "Iterations", description = "Number of removal iterations")
    @JIPipeParameter("iterations")
    public int getIterations() {
        return iterations;
    }

    @JIPipeParameter("iterations")
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus outputImage = inputImage.duplicate();
        outputImage.setTitle(inputImage.getTitle());
        for (int i = 0; i < iterations; i++) {
            LabelImages.removeLargestLabel(outputImage);
        }
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }
}
