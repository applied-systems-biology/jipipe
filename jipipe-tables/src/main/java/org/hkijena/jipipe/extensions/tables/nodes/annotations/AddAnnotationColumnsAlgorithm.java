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
 *
 */

package org.hkijena.jipipe.extensions.tables.nodes.annotations;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.Map;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Add annotations as columns", description = "Adds column annotations to the table as new columns.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Append")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class AddAnnotationColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private String annotationPrefix = "";
    private StringQueryExpression annotationNameFilter = new StringQueryExpression("");

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public AddAnnotationColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public AddAnnotationColumnsAlgorithm(AddAnnotationColumnsAlgorithm other) {
        super(other);
        this.annotationPrefix = other.annotationPrefix;
        this.annotationNameFilter = new StringQueryExpression(other.annotationNameFilter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData resultsTableData = new ResultsTableData(iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo));
        for (Map.Entry<String, JIPipeTextAnnotation> entry : iterationStep.getMergedTextAnnotations().entrySet()) {
            if (!annotationNameFilter.test(entry.getKey()))
                continue;
            int col = resultsTableData.addColumn(annotationPrefix + entry.getKey(), true);
            for (int row = 0; row < resultsTableData.getRowCount(); row++) {
                resultsTableData.setValueAt(entry.getValue() != null ? "" + entry.getValue().getValue() : "", row, col);
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);
    }

    @JIPipeDocumentation(name = "Annotation prefix", description = "Prefix added to columns generated from data annotations.")
    @JIPipeParameter("annotation-prefix")
    public String getAnnotationPrefix() {
        return annotationPrefix;
    }

    @JIPipeParameter("annotation-prefix")
    public void setAnnotationPrefix(String annotationPrefix) {
        this.annotationPrefix = annotationPrefix;
    }

    @JIPipeDocumentation(name = "Annotation name filter", description = "Filters the name of the added annotations. ")
    @JIPipeParameter("annotation-name-filter")
    public StringQueryExpression getAnnotationNameFilter() {
        return annotationNameFilter;
    }

    @JIPipeParameter("annotation-name-filter")
    public void setAnnotationNameFilter(StringQueryExpression annotationNameFilter) {
        this.annotationNameFilter = annotationNameFilter;
    }
}
