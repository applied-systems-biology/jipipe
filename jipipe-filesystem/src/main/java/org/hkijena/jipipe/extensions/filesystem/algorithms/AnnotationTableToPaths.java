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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Filters input files
 */
@JIPipeDocumentation(name = "Annotation table to paths", description = "Converts an annotation table to path data. If available, annotation are added to the output.")
@JIPipeOrganization(menuPath = "Filesystem", algorithmCategory = JIPipeAlgorithmCategory.Converter)

// Algorithm flow
@AlgorithmInputSlot(value = AnnotationTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)

// Traits
public class AnnotationTableToPaths extends JIPipeSimpleIteratingAlgorithm {

    private StringPredicate column = new StringPredicate(StringPredicate.Mode.Equals, "data");

    /**
     * Instantiates the algorithm
     *
     * @param declaration Algorithm declaration
     */
    public AnnotationTableToPaths(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public AnnotationTableToPaths(AnnotationTableToPaths other) {
        super(other);
        this.column = new StringPredicate(other.column);
    }

    @Override
    protected void runIteration(JIPipeDataInterface dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        AnnotationTableData tableData = dataInterface.getInputData(getFirstInputSlot(), AnnotationTableData.class);
        String dataColumn = tableData.getColumnNames().stream().filter(column).findFirst().orElse(null);
        if (dataColumn == null) {
            throw new UserFriendlyRuntimeException("Could not find column that matches '" + column.toString() + "'!",
                    "Could not find column!",
                    "Algorithm '" + getName() + "'",
                    "No column matching the rule '" + column.toString() + "' was found. " +
                            "The table contains only following columns: " + String.join(", ", tableData.getColumnNames()),
                    "Please check if your input columns are set up with valid filters. Please check the input of the algorithm " +
                            "via the quick run to see if the input data is correct.");
        }
        Set<String> annotationColumns = tableData.getAnnotationColumns();
        for (int row = 0; row < tableData.getRowCount(); row++) {
            List<JIPipeAnnotation> annotations = new ArrayList<>();
            for (String annotationColumn : annotationColumns) {
                String declaration = AnnotationTableData.getAnnotationTypeFromColumnName(annotationColumn);
                if (declaration != null)
                    annotations.add(new JIPipeAnnotation(declaration, tableData.getValueAsString(row, annotationColumn)));
            }

            String data = tableData.getValueAsString(row, dataColumn);
            dataInterface.addOutputData(getFirstOutputSlot(), new PathData(Paths.get(data)), annotations);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @JIPipeDocumentation(name = "Column", description = "The column that contains the paths")
    @JIPipeParameter("column")
    public StringPredicate getColumn() {
        return column;
    }

    @JIPipeParameter("column")
    public void setColumn(StringPredicate column) {
        this.column = column;
    }
}
