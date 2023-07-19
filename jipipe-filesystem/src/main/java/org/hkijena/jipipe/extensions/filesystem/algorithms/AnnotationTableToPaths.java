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

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filters input files
 */
@JIPipeDocumentation(name = "Annotation table to paths", description = "Converts an annotation table to path data. If available, annotation are added to the output.")
@JIPipeNode(menuPath = "Convert", nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = AnnotationTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)
public class AnnotationTableToPaths extends JIPipeSimpleIteratingAlgorithm {

    private TableColumnSourceExpressionParameter column = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"data\"");

    /**
     * Instantiates the algorithm
     *
     * @param info Algorithm info
     */
    public AnnotationTableToPaths(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public AnnotationTableToPaths(AnnotationTableToPaths other) {
        super(other);
        this.column = new TableColumnSourceExpressionParameter(other.column);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        AnnotationTableData tableData = dataBatch.getInputData(getFirstInputSlot(), AnnotationTableData.class, progressInfo);
        TableColumn tableColumn = column.pickOrGenerateColumn(tableData);
        if (tableColumn == null) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this),
                    "Could not find column that matches '" + column.toString() + "'!",
                    "No column matching the rule '" + column.toString() + "' was found. " +
                            "The table contains only following columns: " + String.join(", ", tableData.getColumnNames()),
                    "Please check if your input columns are set up with valid filters. Please check the input of the algorithm " +
                            "via the quick run to see if the input data is correct."));
        }
        Set<String> annotationColumns = new HashSet<>(tableData.getColumnNames());
        for (int row = 0; row < tableData.getRowCount(); row++) {
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            for (String annotationColumn : annotationColumns) {
                annotations.add(new JIPipeTextAnnotation(annotationColumn, tableData.getValueAsString(row, annotationColumn)));
            }

            String data = tableColumn.getRowAsString(row);
            dataBatch.addOutputData(getFirstOutputSlot(), new PathData(Paths.get(data)), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Column", description = "The column that contains the paths")
    @JIPipeParameter("column")
    public TableColumnSourceExpressionParameter getColumn() {
        return column;
    }

    @JIPipeParameter("column")
    public void setColumn(TableColumnSourceExpressionParameter column) {
        this.column = column;
    }
}
