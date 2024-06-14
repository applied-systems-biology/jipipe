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

package org.hkijena.jipipe.plugins.multiparameters.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.api.nodes.AddJIPipeDesktopNodeQuickAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterKeyPickerUI;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.plugins.parameters.library.table.ParameterTable;

import javax.swing.*;
import java.util.List;

/**
 * Generates {@link org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData} objects
 */
@SetJIPipeDocumentation(name = "Define multiple parameters", description = "Defines algorithm parameters that can be consumed by a multi-parameter algorithm")
@AddJIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
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
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
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
    @SetJIPipeDocumentation(name = "Parameters", description = "Parameters that are generated")
    public ParameterTable getParameterTable() {
        return parameterTable;
    }

    @JIPipeParameter("parameter-table")
    public void setParameterTable(ParameterTable parameterTable) {
        this.parameterTable = parameterTable;
    }

    @AddJIPipeDesktopNodeQuickAction(name = "Select parameters",
            description = "Auto-configure the node with parameters from nodes",
            icon = "data-types/parameters.png",
            buttonIcon = "actions/color-select.png",
            buttonText = "Select parameters")
    public void autoConfigureDesktopQuickAction(JIPipeDesktopGraphCanvasUI canvasUI) {
        List<JIPipeDesktopParameterKeyPickerUI.ParameterEntry> selectedParameters = JIPipeDesktopParameterKeyPickerUI.showPickerDialog(canvasUI.getDesktopWorkbench().getWindow(),
                "Select parameters",
                canvasUI.getVisibleNodes(),
                null);
        for (JIPipeDesktopParameterKeyPickerUI.ParameterEntry selectedParameter : selectedParameters) {
            if(!parameterTable.containsColumn(selectedParameter.getKey())) {
                parameterTable.addColumn(new ParameterTable.ParameterColumn(selectedParameter.getName(), selectedParameter.getKey(), selectedParameter.getFieldClass()), selectedParameter.getValue());
            }
            if(parameterTable.getRowCount() <= 0) {
                parameterTable.addRow();
            }
        }
        emitParameterChangedEvent("parameter-table");
    }
}
