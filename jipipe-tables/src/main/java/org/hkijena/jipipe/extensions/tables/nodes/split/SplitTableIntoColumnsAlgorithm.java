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

package org.hkijena.jipipe.extensions.tables.nodes.split;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Split table into columns", description = "Splits a table into individual columns")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Split")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = TableColumn.class, slotName = "Output", autoCreate = true)
public class SplitTableIntoColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalStringParameter generatedAnnotation = new OptionalStringParameter();
    private StringQueryExpression columnFilter = new StringQueryExpression("");

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public SplitTableIntoColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        generatedAnnotation.setContent("Column header");
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SplitTableIntoColumnsAlgorithm(SplitTableIntoColumnsAlgorithm other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
        this.columnFilter = new StringQueryExpression(other.columnFilter);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        for (String columnName : input.getColumnNames()) {
            if (columnFilter.test(columnName)) {
                TableColumn column = input.getColumnCopy(input.getColumnIndex(columnName));
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                if (generatedAnnotation.isEnabled() && !StringUtils.isNullOrEmpty(generatedAnnotation.getContent())) {
                    annotations.add(new JIPipeTextAnnotation(generatedAnnotation.getContent(), columnName));
                }
                dataBatch.addOutputData(getFirstOutputSlot(), column, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
        }
    }

    @JIPipeDocumentation(name = "Generated annotation", description = "Optional. The annotation that is created for each table column. The column header will be stored inside it.")
    @JIPipeParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalStringParameter getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @JIPipeParameter("generated-annotation")
    public void setGeneratedAnnotation(OptionalStringParameter generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    @JIPipeDocumentation(name = "Filters", description = "Allows you to filter only specific columns that will be extracted. ")
    @JIPipeParameter("filters")
    public StringQueryExpression getColumnFilter() {
        return columnFilter;
    }

    @JIPipeParameter("filters")
    public void setColumnFilter(StringQueryExpression columnFilter) {
        this.columnFilter = columnFilter;
    }
}
