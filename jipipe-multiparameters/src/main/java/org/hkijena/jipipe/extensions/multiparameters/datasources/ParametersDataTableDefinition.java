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

package org.hkijena.jipipe.extensions.multiparameters.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.events.ParameterStructureChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeDeclaration;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;
import org.hkijena.jipipe.api.registries.JIPipeParameterTypeRegistry;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.parameters.table.ParameterTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generates {@link org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData} objects
 */
@JIPipeDocumentation(name = "Define multiple parameters", description = "Defines algorithm parameters that can be consumed by a multi-parameter algorithm")
@JIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.DataSource)
public class ParametersDataTableDefinition extends JIPipeAlgorithm {

    private GeneratedParameters parameters;
    private ParameterTable parameterTable = new ParameterTable();

    /**
     * Creates a new instance
     *
     * @param declaration the algorithm declaration
     */
    public ParametersDataTableDefinition(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
        this.parameters = new GeneratedParameters(this);
        registerSubParameter(parameters);
        parameterTable.setRowGenerator(this::generateRow);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ParametersDataTableDefinition(ParametersDataTableDefinition other) {
        super(other);
        this.parameters = new GeneratedParameters(other.parameters);
        this.parameters.setParent(this);
        registerSubParameter(parameters);
        this.parameterTable = new ParameterTable(other.parameterTable);
        parameterTable.setRowGenerator(this::generateRow);
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        JIPipeDataSlot outputSlot = getFirstOutputSlot();
        if (parameterTable.getRowCount() > 0) {
            for (int row = 0; row < parameterTable.getRowCount(); ++row) {
                ParametersData data = new ParametersData();
                for (int col = 0; col < parameterTable.getColumnCount(); ++col) {
                    ParameterTable.ParameterColumn column = parameterTable.getColumn(col);
                    data.getParameterData().put(column.getKey(), parameterTable.getValueAt(row, col));
                }
                outputSlot.addData(data);
            }
        } else {
            algorithmProgress.accept(subProgress.resolve("Info: Please add rows to '" + getName() + "'. Falling back to adding the default parameters."));
            ParametersData data = new ParametersData();
            for (Map.Entry<String, JIPipeParameterAccess> entry : parameters.getParameters().entrySet()) {
                JIPipeParameterTypeDeclaration declaration = JIPipeParameterTypeRegistry.getInstance().getDeclarationByFieldClass(entry.getValue().getFieldClass());
                data.getParameterData().put(entry.getKey(), declaration.duplicate(entry.getValue().get(Object.class)));
            }
            outputSlot.addData(data);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Parameters").report(parameters);
    }

    @JIPipeParameter("parameter-table")
    @JIPipeDocumentation(name = "Parameters", description = "Parameters that are generated")
    public ParameterTable getParameterTable() {
        return parameterTable;
    }

    @JIPipeParameter("parameter-table")
    public void setParameterTable(ParameterTable parameterTable) {
        this.parameterTable = parameterTable;
        parameterTable.setRowGenerator(this::generateRow);
    }

    @JIPipeDocumentation(name = "Parameters", description = "Following parameters are generated:")
    @JIPipeParameter("parameters")
    public GeneratedParameters getParameters() {
        return parameters;
    }

    @Override
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        super.onParameterStructureChanged(event);
        updateParameterTable();
    }

    private List<Object> generateRow() {
        List<Object> row = new ArrayList<>();
        for (int col = 0; col < parameterTable.getColumnCount(); ++col) {
            String key = parameterTable.getColumn(col).getKey();
            JIPipeParameterAccess access = parameters.get(key);
            if (access != null)
                row.add(access.get(Object.class));
        }
        return row;
    }

    private void updateParameterTable() {
        if (parameterTable == null)
            return;

        for (int col = parameterTable.getColumnCount() - 1; col >= 0; col--) {
            ParameterTable.ParameterColumn column = parameterTable.getColumn(col);
            JIPipeParameterAccess existing = parameters.getParameters().getOrDefault(column.getKey(), null);
            if (existing == null || existing.getFieldClass() != column.getFieldClass()) {
                parameterTable.removeColumn(col);
            }
        }

        outer:
        for (Map.Entry<String, JIPipeParameterAccess> entry : parameters.getParameters().entrySet()) {
            JIPipeParameterAccess access = entry.getValue();
            for (int col = 0; col < parameterTable.getColumnCount(); col++) {
                ParameterTable.ParameterColumn column = parameterTable.getColumn(col);
                if (column.getFieldClass() == entry.getValue().getFieldClass()
                        && Objects.equals(entry.getKey(), column.getKey())) {
                    continue outer;
                }
            }
            if (!access.getVisibility().isVisibleIn(JIPipeParameterVisibility.TransitiveVisible))
                continue;
            parameterTable.addColumn(new ParameterTable.ParameterColumn(
                    access.getName(),
                    entry.getKey(),
                    access.getFieldClass()
            ), JIPipeParameterTypeRegistry.getInstance().getDeclarationByFieldClass(access.getFieldClass()).newInstance());
        }
    }
}
