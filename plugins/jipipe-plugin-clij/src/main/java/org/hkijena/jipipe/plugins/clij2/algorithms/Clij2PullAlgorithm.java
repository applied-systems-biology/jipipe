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

package org.hkijena.jipipe.plugins.clij2.algorithms;

import org.hkijena.jipipe.JIPipe;
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
import org.hkijena.jipipe.plugins.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "CLIJ2 Pull from GPU", description = "Converts a GPU image into a non-CLIJ image")
@AddJIPipeInputSlot(name = "Input", value = CLIJImageData.class, create = true)
@AddJIPipeOutputSlot(slotName = "Output", value = ImagePlusData.class, create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "CLIJ")
public class Clij2PullAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean deallocate = false;

    public Clij2PullAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Clij2PullAlgorithm(Clij2PullAlgorithm other) {
        super(other);
        this.deallocate = other.deallocate;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        CLIJImageData inputData = iterationStep.getInputData(getFirstInputSlot(), CLIJImageData.class, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), JIPipe.getDataTypes().convert(inputData, ImagePlusData.class, progressInfo), progressInfo);
        if (deallocate) {
            inputData.getImage().close();
        }
    }

    @SetJIPipeDocumentation(name = "Deallocate", description = "Removes the image from the GPU memory afterwards. Please be ensure that the image is not used anywhere else.")
    @JIPipeParameter("deallocate")
    public boolean isDeallocate() {
        return deallocate;
    }

    @JIPipeParameter("deallocate")
    public void setDeallocate(boolean deallocate) {
        this.deallocate = deallocate;
    }
}
