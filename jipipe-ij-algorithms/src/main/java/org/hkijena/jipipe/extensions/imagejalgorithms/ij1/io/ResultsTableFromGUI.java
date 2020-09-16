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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io;

import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.util.LogicalOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Imports {@link ImagePlusData} from the GUI
 */
@JIPipeDocumentation(name = "Table from ImageJ", description = "Imports one or multiple active ImageJ results table windows into JIPipe")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ResultsTableFromGUI extends JIPipeSimpleIteratingAlgorithm {

    private StringPredicate.List titleFilters = new StringPredicate.List();
    private LogicalOperation titleFiltersOperation = LogicalOperation.LogicalOr;

    public ResultsTableFromGUI(JIPipeNodeInfo info) {
        super(info);
        titleFilters.add(new StringPredicate(StringPredicate.Mode.Equals, "Results", false));
    }

    public ResultsTableFromGUI(ResultsTableFromGUI other) {
        super(other);
        this.titleFilters = new StringPredicate.List(other.titleFilters);
        this.titleFiltersOperation = other.titleFiltersOperation;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (Window window : WindowManager.getAllNonImageWindows()) {
            if (window instanceof TextWindow) {
                ResultsTable resultsTable = ((TextWindow) window).getResultsTable();
                if (resultsTable != null) {
                    if (!titleFilters.isEmpty()) {
                        List<Boolean> predicateResults = new ArrayList<>();
                        for (StringPredicate filter : titleFilters) {
                            predicateResults.add(filter.test(((TextWindow) window).getTitle()));
                        }
                        if (!titleFiltersOperation.apply(predicateResults))
                            continue;
                    }
                    ResultsTableData tableData = new ResultsTableData(resultsTable);
                    dataBatch.addOutputData(getFirstOutputSlot(), tableData.duplicate());
                }
            }
        }
    }

    @JIPipeDocumentation(name = "Filter titles", description = "Predicates to filter the list of tables by their title. If no filter is in the list, tables will not be filtered.")
    @JIPipeParameter("title-filters")
    public StringPredicate.List getTitleFilters() {
        return titleFilters;
    }

    @JIPipeParameter("title-filters")
    public void setTitleFilters(StringPredicate.List titleFilters) {
        this.titleFilters = titleFilters;
    }

    @JIPipeDocumentation(name = "Filter titles operation", description = "Determines how the 'Filter titles' operations are connected.")
    @JIPipeParameter("title-filters-mode")
    public LogicalOperation getTitleFiltersOperation() {
        return titleFiltersOperation;
    }

    @JIPipeParameter("title-filters-mode")
    public void setTitleFiltersOperation(LogicalOperation titleFiltersOperation) {
        this.titleFiltersOperation = titleFiltersOperation;
    }
}
