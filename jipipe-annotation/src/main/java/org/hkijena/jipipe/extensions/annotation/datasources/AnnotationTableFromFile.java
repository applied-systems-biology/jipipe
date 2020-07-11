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

package org.hkijena.jipipe.extensions.annotation.datasources;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Imports {@link AnnotationTableData} from a file
 */
@JIPipeDocumentation(name = "Annotation table from file")
@AlgorithmInputSlot(value = FileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = AnnotationTableData.class, slotName = "Annotation table", autoCreate = true)
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.DataSource)
public class AnnotationTableFromFile extends JIPipeSimpleIteratingAlgorithm {

    /**
     * @param declaration algorithm declaration
     */
    public AnnotationTableFromFile(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public AnnotationTableFromFile(AnnotationTableFromFile other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataInterface dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FileData fileData = dataInterface.getInputData(getFirstInputSlot(), FileData.class);
        try {
            ResultsTable resultsTable = ResultsTable.open(fileData.getPath().toString());
            dataInterface.addOutputData(getFirstOutputSlot(), new AnnotationTableData(resultsTable));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }
}
