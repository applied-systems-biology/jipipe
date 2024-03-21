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

package org.hkijena.jipipe.extensions.tables.nodes.columns;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.NaturalOrderComparator;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Sort table columns", description = "Sorts the table columns. Also allows to provide a manual sorting.")
@AddJIPipeNodeAlias(aliasName = "Reorder table columns")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class SortTableColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean useNaturalSortOrder = true;
    private boolean reverseSortOrder = false;
    private StringList manualOrder = new StringList();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public SortTableColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SortTableColumnsAlgorithm(SortTableColumnsAlgorithm other) {
        super(other);
        this.useNaturalSortOrder = other.useNaturalSortOrder;
        this.reverseSortOrder = other.reverseSortOrder;
        this.manualOrder = new StringList(other.manualOrder);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        Comparator<String> comparator = Comparator.naturalOrder();
        if (useNaturalSortOrder) {
            comparator = new NaturalOrderComparator<>();
        }
        if (reverseSortOrder) {
            comparator = comparator.reversed();
        }
        ArrayList<String> columnNames = new ArrayList<>(input.getColumnNames());
        columnNames.sort(comparator);

        ArrayList<String> orderedColumnNames = new ArrayList<>();
        for (String columnName : manualOrder) {
            if(columnNames.contains(columnName)) {
                orderedColumnNames.add(columnName);
            }
        }
        for (String columnName : columnNames) {
            if(!orderedColumnNames.contains(columnName)) {
                orderedColumnNames.add(columnName);
            }
        }

        ResultsTableData output = new ResultsTableData();
        for (String columnName : orderedColumnNames) {
            output.addColumn(columnName, input.isStringColumn(columnName));
        }
        output.addRows(input);
        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Manual order", description = "Columns are first sorted according to this manual order. All other columns will be sorted after this list.")
    @JIPipeParameter("manual-order")
    public StringList getManualOrder() {
        return manualOrder;
    }

    @JIPipeParameter("manual-order")
    public void setManualOrder(StringList manualOrder) {
        this.manualOrder = manualOrder;
    }

    @SetJIPipeDocumentation(name = "Sort strings by natural order", description = "If enabled, strings are sorted by natural order (e.g. 1, 2, 15 100, ...). If disabled, " +
            "strings are sorted lexicographically (e.g. 1, 15, 100, 2)")
    @JIPipeParameter("use-natural-sort-order")
    public boolean isUseNaturalSortOrder() {
        return useNaturalSortOrder;
    }

    @JIPipeParameter("use-natural-sort-order")
    public void setUseNaturalSortOrder(boolean useNaturalSortOrder) {
        this.useNaturalSortOrder = useNaturalSortOrder;
    }

    @SetJIPipeDocumentation(name = "Reverse sort order", description = "If enabled, the sort order is reversed.")
    @JIPipeParameter("reverse-sort-order")
    public boolean isReverseSortOrder() {
        return reverseSortOrder;
    }

    @JIPipeParameter("reverse-sort-order")
    public void setReverseSortOrder(boolean reverseSortOrder) {
        this.reverseSortOrder = reverseSortOrder;
    }
}
