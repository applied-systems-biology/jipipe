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
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ResultsTableDataList;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that annotates all data with the same annotation
 */
@JIPipeDocumentation(name = "Define tables", description = "Defines one or multiple tables.")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class DefineTablesAlgorithm extends JIPipeAlgorithm {

    private ResultsTableDataList tables = new ResultsTableDataList();

    /**
     * @param info the info
     */
    public DefineTablesAlgorithm(JIPipeNodeInfo info) {
        super(info);
        tables.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public DefineTablesAlgorithm(DefineTablesAlgorithm other) {
        super(other);
        this.tables = new ResultsTableDataList(other.tables);
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (ResultsTableData table : tables) {
            getFirstOutputSlot().addData(new ResultsTableData(table));
        }
    }

    @JIPipeDocumentation(name = "Tables", description = "The tables are stored into the output slot.")
    @JIPipeParameter("tables")
    public ResultsTableDataList getTables() {
        return tables;
    }

    @JIPipeParameter("tables")
    public void setTables(ResultsTableDataList tables) {
        this.tables = tables;
    }
}
