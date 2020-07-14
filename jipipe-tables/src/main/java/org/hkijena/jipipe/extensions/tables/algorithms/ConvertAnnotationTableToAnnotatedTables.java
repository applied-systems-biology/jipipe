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

package org.hkijena.jipipe.extensions.tables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Annotation table to annotated table", description = "Extracts annotation columns from an annotation table and " +
        "converts them into data annotations. All non-annotation items are stored in the resulting tables.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Tables")
@JIPipeInputSlot(value = AnnotationTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ConvertAnnotationTableToAnnotatedTables extends JIPipeSimpleIteratingAlgorithm {

    private boolean keepAnnotationColumns = false;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public ConvertAnnotationTableToAnnotatedTables(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ConvertAnnotationTableToAnnotatedTables(ConvertAnnotationTableToAnnotatedTables other) {
        super(other);
        this.keepAnnotationColumns = other.keepAnnotationColumns;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        AnnotationTableData inputData = dataInterface.getInputData(getFirstInputSlot(), AnnotationTableData.class);
        TableColumn mergedColumn = inputData.getMergedColumn(inputData.getAnnotationColumns(), ", ", "=");
        for (Map.Entry<String, ResultsTableData> entry : inputData.splitBy(mergedColumn).entrySet()) {
            ResultsTableData data = entry.getValue();
            if (!keepAnnotationColumns) {
                data.removeColumns(inputData.getAnnotationColumns());
            }
            dataInterface.addOutputData(getFirstOutputSlot(), data);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @JIPipeDocumentation(name = "Keep annotation columns", description = "If enabled, annotation columns are copied into the result tables.")
    @JIPipeParameter("keep-annotation-columns")
    public boolean isKeepAnnotationColumns() {
        return keepAnnotationColumns;
    }

    @JIPipeParameter("keep-annotation-columns")
    public void setKeepAnnotationColumns(boolean keepAnnotationColumns) {
        this.keepAnnotationColumns = keepAnnotationColumns;
    }
}
