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

package org.hkijena.jipipe.plugins.utils.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;

@SetJIPipeDocumentation(name = "Duplicate data", description = "Creates a duplicate of the input data. Useful for debugging purposes.")
@AddJIPipeInputSlot(slotName = "Input", value = JIPipeData.class, create = true)
@AddJIPipeOutputSlot(slotName = "Output", value = JIPipeData.class, create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class DuplicateDataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public DuplicateDataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DuplicateDataAlgorithm(DuplicateDataAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeData duplicate = iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo).duplicate(progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), duplicate, progressInfo);
    }
}
