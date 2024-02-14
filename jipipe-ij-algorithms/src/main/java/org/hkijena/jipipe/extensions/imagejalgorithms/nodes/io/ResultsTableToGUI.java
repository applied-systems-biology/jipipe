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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.io;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.stream.Collectors;

/**
 * Imports {@link ImagePlusData} from the GUI
 */
@JIPipeDocumentation(name = "Table to ImageJ", description = "Opens all incoming tables in ImageJ")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nExport")
public class ResultsTableToGUI extends JIPipeSimpleIteratingAlgorithm {

    public ResultsTableToGUI(JIPipeNodeInfo info) {
        super(info);
    }

    public ResultsTableToGUI(ResultsTableToGUI other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputData = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        String title = iterationStep.getMergedTextAnnotations().values().stream().map(a -> a.getName() + "=" + a.getValue()).collect(Collectors.joining(", "));
        ((ResultsTableData) inputData.duplicate(progressInfo)).getTable().show(title);
        iterationStep.addOutputData(getFirstOutputSlot(), inputData, progressInfo);
    }
}
