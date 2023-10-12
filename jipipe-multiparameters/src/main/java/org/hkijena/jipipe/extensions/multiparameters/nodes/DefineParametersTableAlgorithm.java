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

package org.hkijena.jipipe.extensions.multiparameters.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.data.context.JIPipeMutableDataContext;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.parameters.library.table.ParameterTable;

/**
 * Generates {@link org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData} objects
 */
@JIPipeDocumentation(name = "Define multiple parameters", description = "Defines algorithm parameters that can be consumed by a multi-parameter algorithm")
@JIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class DefineParametersTableAlgorithm extends JIPipeAlgorithm {

    private ParameterTable parameterTable = new ParameterTable();

    /**
     * Creates a new instance
     *
     * @param info the algorithm info
     */
    public DefineParametersTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public DefineParametersTableAlgorithm(DefineParametersTableAlgorithm other) {
        super(other);
        this.parameterTable = new ParameterTable(other.parameterTable);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot outputSlot = getFirstOutputSlot();
        for (int row = 0; row < parameterTable.getRowCount(); ++row) {
            ParametersData data = new ParametersData();
            for (int col = 0; col < parameterTable.getColumnCount(); ++col) {
                ParameterTable.ParameterColumn column = parameterTable.getColumn(col);
                data.getParameterData().put(column.getKey(), parameterTable.getValueAt(row, col));
            }
            outputSlot.addData(data, JIPipeDataContext.create(this), progressInfo);
        }
    }

    @JIPipeParameter("parameter-table")
    @JIPipeDocumentation(name = "Parameters", description = "Parameters that are generated")
    public ParameterTable getParameterTable() {
        return parameterTable;
    }

    @JIPipeParameter("parameter-table")
    public void setParameterTable(ParameterTable parameterTable) {
        this.parameterTable = parameterTable;
    }
}
