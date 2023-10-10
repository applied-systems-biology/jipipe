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

package org.hkijena.jipipe.extensions.tables.nodes.columns;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.NaturalOrderComparator;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Sort table columns", description = "Sorts the table columns")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class SortTableColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean useNaturalSortOrder = true;
    private boolean reverseSortOrder = false;

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
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        Comparator<String> comparator = Comparator.naturalOrder();
        if (useNaturalSortOrder) {
            comparator = new NaturalOrderComparator<>();
        }
        if (reverseSortOrder) {
            comparator = comparator.reversed();
        }
        ArrayList<String> columnNames = new ArrayList<>(input.getColumnNames());
        columnNames.sort(comparator);
        ResultsTableData output = new ResultsTableData();
        for (String columnName : columnNames) {
            output.addColumn(columnName, input.isStringColumn(columnName));
        }
        output.addRows(input);
        dataBatch.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @JIPipeDocumentation(name = "Sort strings by natural order", description = "If enabled, strings are sorted by natural order (e.g. 1, 2, 15 100, ...). If disabled, " +
            "strings are sorted lexicographically (e.g. 1, 15, 100, 2)")
    @JIPipeParameter("use-natural-sort-order")
    public boolean isUseNaturalSortOrder() {
        return useNaturalSortOrder;
    }

    @JIPipeParameter("use-natural-sort-order")
    public void setUseNaturalSortOrder(boolean useNaturalSortOrder) {
        this.useNaturalSortOrder = useNaturalSortOrder;
    }

    @JIPipeDocumentation(name = "Reverse sort order", description = "If enabled, the sort order is reversed.")
    @JIPipeParameter("reverse-sort-order")
    public boolean isReverseSortOrder() {
        return reverseSortOrder;
    }

    @JIPipeParameter("reverse-sort-order")
    public void setReverseSortOrder(boolean reverseSortOrder) {
        this.reverseSortOrder = reverseSortOrder;
    }
}
