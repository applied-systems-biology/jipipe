package org.hkijena.acaq5.extensions.filesystem.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.annotation.datatypes.AnnotationTableData;
import org.hkijena.acaq5.extensions.filesystem.dataypes.PathData;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Filters input files
 */
@ACAQDocumentation(name = "Annotation table to paths", description = "Converts an annotation table to path data. If available, annotation are added to the output.")
@ACAQOrganization(menuPath = "Filesystem", algorithmCategory = ACAQAlgorithmCategory.Converter)

// Algorithm flow
@AlgorithmInputSlot(value = AnnotationTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)

// Traits
public class AnnotationTableToPaths extends ACAQSimpleIteratingAlgorithm {

    private StringPredicate column = new StringPredicate(StringPredicate.Mode.Equals, "data");

    /**
     * Instantiates the algorithm
     *
     * @param declaration Algorithm declaration
     */
    public AnnotationTableToPaths(ACAQAlgorithmDeclaration declaration) {
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
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
            List<ACAQAnnotation> annotations = new ArrayList<>();
            for (String annotationColumn : annotationColumns) {
                String declaration = AnnotationTableData.getAnnotationTypeFromColumnName(annotationColumn);
                if (declaration != null)
                    annotations.add(new ACAQAnnotation(declaration, tableData.getValueAsString(row, annotationColumn)));
            }

            String data = tableData.getValueAsString(row, dataColumn);
            dataInterface.addOutputData(getFirstOutputSlot(), new PathData(Paths.get(data)), annotations);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Column", description = "The column that contains the paths")
    @ACAQParameter("column")
    public StringPredicate getColumn() {
        return column;
    }

    @ACAQParameter("column")
    public void setColumn(StringPredicate column) {
        this.column = column;
    }
}
