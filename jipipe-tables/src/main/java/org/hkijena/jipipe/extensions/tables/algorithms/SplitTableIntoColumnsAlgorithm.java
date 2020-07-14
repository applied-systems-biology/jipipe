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
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Split table into columns", description = "Splits a table into individual columns")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Tables")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = TableColumn.class, slotName = "Output", autoCreate = true)
public class SplitTableIntoColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private String generatedAnnotation = "Column header";
    private StringPredicate.List filters = new StringPredicate.List();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public SplitTableIntoColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SplitTableIntoColumnsAlgorithm(SplitTableIntoColumnsAlgorithm other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
        this.filters = new StringPredicate.List(other.filters);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData input = dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class);
        for (String columnName : input.getColumnNames()) {
            if (filters.isEmpty() || filters.test(columnName)) {
                TableColumn column = input.getColumnCopy(input.getColumnIndex(columnName));
                List<JIPipeAnnotation> traitList = new ArrayList<>();
                if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
                    traitList.add(new JIPipeAnnotation(generatedAnnotation, columnName));
                }
                dataInterface.addOutputData(getFirstOutputSlot(), column, traitList);
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Filters").report(filters);
    }

    @JIPipeDocumentation(name = "Generated annotation", description = "Optional. The annotation that is created for each table column. The column header will be stored inside it.")
    @JIPipeParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public String getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @JIPipeParameter("generated-annotation")
    public void setGeneratedAnnotation(String generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    @JIPipeDocumentation(name = "Filters", description = "Allows you to filter only specific columns that will be extracted. The filters are connected via OR.")
    @JIPipeParameter("filters")
    public StringPredicate.List getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(StringPredicate.List filters) {
        this.filters = filters;
    }
}
