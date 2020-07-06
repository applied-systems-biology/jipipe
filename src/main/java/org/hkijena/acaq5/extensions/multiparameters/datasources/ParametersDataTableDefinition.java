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

package org.hkijena.acaq5.extensions.multiparameters.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.acaq5.extensions.parameters.table.ParameterTable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generates {@link org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData} objects
 */
@ACAQDocumentation(name = "Define multiple parameters", description = "Defines algorithm parameters that can be consumed by a multi-parameter algorithm")
@AlgorithmOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
public class ParametersDataTableDefinition extends ACAQAlgorithm {

    private ACAQDynamicParameterCollection parameters;
    private ParameterTable parameterTable = new ParameterTable();

    /**
     * Creates a new instance
     *
     * @param declaration the algorithm declaration
     */
    public ParametersDataTableDefinition(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        this.parameters = new ACAQDynamicParameterCollection(true, ACAQParameterTypeRegistry.getInstance().getRegisteredParameters().values());
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
        this.parameters = new ACAQDynamicParameterCollection(other.parameters);
        registerSubParameter(parameters);
        this.parameterTable = new ParameterTable(other.parameterTable);
        parameterTable.setRowGenerator(this::generateRow);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ACAQDataSlot outputSlot = getFirstOutputSlot();
        for (int row = 0; row < parameterTable.getRowCount(); ++row) {
            ParametersData data = new ParametersData();
            for (int col = 0; col < parameterTable.getColumnCount(); ++col) {
                ParameterTable.ParameterColumn column = parameterTable.getColumn(col);
                data.getParameterData().put(column.getKey(), parameterTable.getValueAt(row, col));
            }
            outputSlot.addData(data);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Parameters").report(parameters);
    }

    @ACAQParameter("parameter-table")
    @ACAQDocumentation(name = "Parameters", description = "Parameters that are generated")
    public ParameterTable getParameterTable() {
        return parameterTable;
    }

    @ACAQParameter("parameter-table")
    public void setParameterTable(ParameterTable parameterTable) {
        this.parameterTable = parameterTable;
        parameterTable.setRowGenerator(this::generateRow);
    }

    @ACAQDocumentation(name = "Parameters", description = "Following parameters are generated:")
    @ACAQParameter("parameters")
    public ACAQDynamicParameterCollection getParameters() {
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
            ACAQParameterAccess access = parameters.get(key);
            if(access != null)
                row.add(access.get(Object.class));
        }
        return row;
    }

    private void updateParameterTable() {
        if(parameterTable == null)
            return;

        for (int col = parameterTable.getColumnCount() - 1; col >= 0; col--) {
            ParameterTable.ParameterColumn column = parameterTable.getColumn(col);
            ACAQParameterAccess existing = parameters.getParameters().getOrDefault(column.getKey(), null);
            if(existing == null || existing.getFieldClass() != column.getFieldClass()) {
                parameterTable.removeColumn(col);
            }
        }

        outer: for (Map.Entry<String, ACAQParameterAccess> entry : parameters.getParameters().entrySet()) {
            ACAQParameterAccess access = entry.getValue();
            for (int col = 0; col < parameterTable.getColumnCount(); col++) {
                ParameterTable.ParameterColumn column = parameterTable.getColumn(col);
                if(column.getFieldClass() == entry.getValue().getFieldClass()
                && Objects.equals(entry.getKey(), column.getKey())) {
                    continue outer;
                }
            }
            if (!access.getVisibility().isVisibleIn(ACAQParameterVisibility.TransitiveVisible))
                continue;
            parameterTable.addColumn(new ParameterTable.ParameterColumn(
                    access.getName(),
                    entry.getKey(),
                    access.getFieldClass()
            ), ACAQParameterTypeRegistry.getInstance().getDeclarationByFieldClass(access.getFieldClass()).newInstance());
        }
    }
}
