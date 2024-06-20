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

package org.hkijena.jipipe.plugins.annotation.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.AnnotationTableData;

import java.util.Set;

/**
 * Removes a specified annotation
 */
@SetJIPipeDocumentation(name = "Convert to annotation table", description = "Converts data into an annotation table that contains " +
        "all annotations of the data row. You can also add a string representation of the data.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, aliasName = "Annotations to table")
@AddJIPipeNodeAlias(nodeTypeCategory = AnnotationsNodeTypeCategory.class, aliasName = "Annotations to table")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = AnnotationTableData.class, name = "Output", create = true)
public class ConvertToAnnotationTable extends JIPipeMergingAlgorithm {

    private boolean removeOutputAnnotations = false;
    private OptionalTextAnnotationNameParameter addDataToString = new OptionalTextAnnotationNameParameter("data_as_string", false);

    /**
     * @param info algorithm info
     */
    public ConvertToAnnotationTable(JIPipeNodeInfo info) {
        super(info);
//        getDataBatchGenerationSettings().setColumnMatching(JIPipeColumMatching.MergeAll);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ConvertToAnnotationTable(ConvertToAnnotationTable other) {
        super(other);
        this.removeOutputAnnotations = other.removeOutputAnnotations;
        this.addDataToString = other.addDataToString;
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Set<Integer> inputDataRows = iterationStep.getInputRows(getFirstInputSlot());

        AnnotationTableData output = new AnnotationTableData();
        int dataColumn = addDataToString.isEnabled() ? output.addColumn(addDataToString.getContent(), true) : -1;

        int row = 0;
        for (int sourceRow : inputDataRows) {
            output.addRow();
            if (dataColumn >= 0)
                output.setValueAt(getFirstInputSlot().getDataItemStore(sourceRow).getStringRepresentation(), row, dataColumn);
            for (JIPipeTextAnnotation annotation : getFirstInputSlot().getTextAnnotations(sourceRow)) {
                if (annotation != null) {
                    int col = output.addAnnotationColumn(annotation.getName());
                    output.setValueAt(annotation.getValue(), row, col);
                }
            }
            ++row;
        }

        if (removeOutputAnnotations)
            iterationStep.getMergedTextAnnotations().clear();

        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);

    }

    @SetJIPipeDocumentation(name = "Remove output annotations", description = "If enabled, annotations are removed from the output.")
    @JIPipeParameter("remove-output-annotations")
    public boolean isRemoveOutputAnnotations() {
        return removeOutputAnnotations;
    }

    @JIPipeParameter("remove-output-annotations")
    public void setRemoveOutputAnnotations(boolean removeOutputAnnotations) {
        this.removeOutputAnnotations = removeOutputAnnotations;
    }

    @SetJIPipeDocumentation(name = "Add data as string", description = "Adds the string representation of data as string")
    @JIPipeParameter("add-data-as-string")
    public OptionalTextAnnotationNameParameter getAddDataToString() {
        return addDataToString;
    }

    @JIPipeParameter("add-data-as-string")
    public void setAddDataToString(OptionalTextAnnotationNameParameter addDataToString) {
        this.addDataToString = addDataToString;
    }
}
