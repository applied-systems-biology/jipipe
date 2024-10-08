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

package org.hkijena.jipipe.plugins.tables.nodes.split;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.List;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Split table into rows", description = "Splits a table into individual rows")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Split")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class SplitTableIntoRowsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean annotateWithValues = false;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public SplitTableIntoRowsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SplitTableIntoRowsAlgorithm(SplitTableIntoRowsAlgorithm other) {
        super(other);
        this.annotateWithValues = other.annotateWithValues;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        for (int row = 0; row < input.getRowCount(); row++) {
            ResultsTableData output = input.getRow(row);
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            if (annotateWithValues) {
                for (int col = 0; col < output.getColumnCount(); col++) {
                    annotations.add(new JIPipeTextAnnotation(output.getColumnName(col), output.getValueAsString(0, col)));
                }
            }
            iterationStep.addOutputData(getFirstOutputSlot(), output, annotations, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Annotate with values", description = "If enabled, output tables are annotated with the values. " +
            "The annotation name is the column name, while the annotation value is the value of the row.")
    @JIPipeParameter("annotate-with-values")
    public boolean isAnnotateWithValues() {
        return annotateWithValues;
    }

    @JIPipeParameter("annotate-with-values")
    public void setAnnotateWithValues(boolean annotateWithValues) {
        this.annotateWithValues = annotateWithValues;
    }
}
