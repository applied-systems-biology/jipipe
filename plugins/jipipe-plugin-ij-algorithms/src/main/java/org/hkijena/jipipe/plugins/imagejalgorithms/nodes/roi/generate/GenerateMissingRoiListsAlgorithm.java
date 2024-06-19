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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.generate;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMissingDataGeneratorAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;

@SetJIPipeDocumentation(name = "Generate missing ROI lists", description = "Generates empty ROI lists for data that are not paired " +
        "with a matching ROI in the same data batch. ")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Data", create = true)
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", create = true)
public class GenerateMissingRoiListsAlgorithm extends JIPipeMissingDataGeneratorAlgorithm {
    public GenerateMissingRoiListsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public GenerateMissingRoiListsAlgorithm(GenerateMissingRoiListsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runGenerator(JIPipeMultiIterationStep iterationStep, JIPipeInputDataSlot inputSlot, JIPipeOutputDataSlot outputSlot, JIPipeProgressInfo progressInfo) {
        iterationStep.addOutputData(outputSlot, new ROIListData(), progressInfo);
    }
}
