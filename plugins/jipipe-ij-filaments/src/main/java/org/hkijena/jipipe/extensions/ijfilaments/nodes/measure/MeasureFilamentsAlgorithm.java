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

package org.hkijena.jipipe.extensions.ijfilaments.nodes.measure;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Measure filaments", description = "Measures all components in the filament graph. " +
        "Produces the following measurements: " +
        "<ul>" +
        "<li>number of vertices</li>" +
        "<li>number of edges</li>" +
        "<li>length (sum of edge lengths)</li>" +
        "<li>skeletonization-corrected length (adding 2 * radius for each vertex with degree 0 and 1 x radius for each vertex with degree 1)</li>" +
        "<li>confinement ratio (length of the simplified graph divided by the length)</li>" +
        "<li>number of vertices with specific degrees</li>" +
        "<li>min/max centroid x/y/z</li>" +
        "<li>min/max x/y/z (with sphere radius)</li>" +
        "<li>min/max/avg radius/value</li>" +
        "</ul>")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Measure")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class MeasureFilamentsAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public MeasureFilamentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MeasureFilamentsAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        ResultsTableData outputData = inputData.measureComponents();
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
