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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.measure;

import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Measure filament vertices", description = "Stores all available information about the vertices into a table. " +
        "You can learn more about the generated measurements here: https://jipipe.hki-jena.de/documentation/filament-measurements.html")
@AddJIPipeCitation("https://jipipe.hki-jena.de/documentation/filament-measurements.html")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Measure")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class MeasureVerticesAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public MeasureVerticesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MeasureVerticesAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo);
        ResultsTableData outputData = inputData.measureVertices();
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
