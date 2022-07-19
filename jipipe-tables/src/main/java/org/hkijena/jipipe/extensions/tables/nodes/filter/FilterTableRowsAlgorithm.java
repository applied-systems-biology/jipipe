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

package org.hkijena.jipipe.extensions.tables.nodes.filter;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.AnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Filter table rows", description = "Filters tables by iterating through each row and testing a filter expression.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class FilterTableRowsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter filters = new DefaultExpressionParameter();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public FilterTableRowsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FilterTableRowsAlgorithm(FilterTableRowsAlgorithm other) {
        super(other);
        this.filters = new DefaultExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        List<Integer> selectedRows = new ArrayList<>();
        ExpressionVariables variableSet = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : dataBatch.getMergedTextAnnotations().values()) {
            variableSet.set(annotation.getName(), annotation.getValue());
        }
        for (int row = 0; row < input.getRowCount(); row++) {
            for (int col = 0; col < input.getColumnCount(); col++) {
                variableSet.set(input.getColumnName(col), input.getValueAt(row, col));
            }
            variableSet.set("index", "" + row);
            if (filters.test(variableSet)) {
                selectedRows.add(row);
            }
        }

        ResultsTableData output = input.getRows(selectedRows);
        dataBatch.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @JIPipeDocumentation(name = "Filters", description = "Allows you to select how to filter the values. " +
            "Each row is iterated individually and its columns are available as variables inside the expression. For example there are columns 'Area' and 'X'. " +
            "Then you can filter the table via an expression 'AREA > 100 AND X > 200 AND X < 1000'." +
            "Annotations are available as variables.")
    @JIPipeParameter("filters")
    @ExpressionParameterSettingsVariable(fromClass = AnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "<Columns>", description = "The column values are available as variables")
    @ExpressionParameterSettingsVariable(name = "Row index", description = "The index of the table row. The first row is indexed with zero.", key = "index")
    public DefaultExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(DefaultExpressionParameter filters) {
        this.filters = filters;
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            setFilters(new DefaultExpressionParameter("Area > 1000 AND Circulary >= 0.6"));
            getEventBus().post(new ParameterChangedEvent(this, "filters"));
        }
    }
}
