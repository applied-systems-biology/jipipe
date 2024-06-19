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

package org.hkijena.jipipe.plugins.utils.algorithms.datatable;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate with data table properties", description = "Annotates data table data with properties (e.g., the size)")
@AddJIPipeInputSlot(value = JIPipeDataTable.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeDataTable.class, slotName = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For data tables")
public class AnnotateWithDataTableProperties extends JIPipeSimpleIteratingAlgorithm {

    private OptionalTextAnnotationNameParameter numRowsAnnotation = new OptionalTextAnnotationNameParameter("Num Rows", true);
    private OptionalTextAnnotationNameParameter numTextAnnotationColumns = new OptionalTextAnnotationNameParameter("Num Text Annotation Columns", false);
    private OptionalTextAnnotationNameParameter numDataAnnotationColumns = new OptionalTextAnnotationNameParameter("Num Text Annotation Columns", false);

    private JIPipeTextAnnotationMergeMode mergeMode = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public AnnotateWithDataTableProperties(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateWithDataTableProperties(AnnotateWithDataTableProperties other) {
        super(other);
        this.numRowsAnnotation = new OptionalTextAnnotationNameParameter(other.numRowsAnnotation);
        this.numTextAnnotationColumns = new OptionalTextAnnotationNameParameter(other.numTextAnnotationColumns);
        this.numDataAnnotationColumns = new OptionalTextAnnotationNameParameter(other.numDataAnnotationColumns);
        this.mergeMode = other.mergeMode;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable inputData = iterationStep.getInputData(getFirstInputSlot(), JIPipeDataTable.class, progressInfo);
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
        numRowsAnnotation.addAnnotationIfEnabled(annotationList, inputData.getRowCount() + "");
        numTextAnnotationColumns.addAnnotationIfEnabled(annotationList, inputData.getTextAnnotationColumnNames().size() + "");
        numDataAnnotationColumns.addAnnotationIfEnabled(annotationList, inputData.getDataAnnotationColumnNames().size() + "");
        iterationStep.addOutputData(getFirstOutputSlot(), inputData, annotationList, mergeMode, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Annotate with number of rows", description = "If enabled, annotate the data table with its number of rows")
    @JIPipeParameter("num-rows-annotation")
    public OptionalTextAnnotationNameParameter getNumRowsAnnotation() {
        return numRowsAnnotation;
    }

    @JIPipeParameter("num-rows-annotation")
    public void setNumRowsAnnotation(OptionalTextAnnotationNameParameter numRowsAnnotation) {
        this.numRowsAnnotation = numRowsAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with number of text annotation columns", description = "If enabled, annotate the data table with the number of text annotation columns")
    @JIPipeParameter("num-text-annotation-columns-annotation")
    public OptionalTextAnnotationNameParameter getNumTextAnnotationColumns() {
        return numTextAnnotationColumns;
    }

    @JIPipeParameter("num-text-annotation-columns-annotation")
    public void setNumTextAnnotationColumns(OptionalTextAnnotationNameParameter numTextAnnotationColumns) {
        this.numTextAnnotationColumns = numTextAnnotationColumns;
    }

    @SetJIPipeDocumentation(name = "Annotate with number of data annotation columns", description = "If enabled, annotate the data table with the number of data annotation columns")
    @JIPipeParameter("num-data-annotation-columns-annotation")
    public OptionalTextAnnotationNameParameter getNumDataAnnotationColumns() {
        return numDataAnnotationColumns;
    }

    @JIPipeParameter("num-data-annotation-columns-annotation")
    public void setNumDataAnnotationColumns(OptionalTextAnnotationNameParameter numDataAnnotationColumns) {
        this.numDataAnnotationColumns = numDataAnnotationColumns;
    }

    @SetJIPipeDocumentation(name = "Annotation merge mode", description = "Determines what happens if there is already an existing annotation with the same name")
    @JIPipeParameter("merge-mode")
    public JIPipeTextAnnotationMergeMode getMergeMode() {
        return mergeMode;
    }

    @JIPipeParameter("merge-mode")
    public void setMergeMode(JIPipeTextAnnotationMergeMode mergeMode) {
        this.mergeMode = mergeMode;
    }
}
