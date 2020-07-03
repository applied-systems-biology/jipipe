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
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.acaq5.extensions.parameters.references.ACAQAlgorithmDeclarationRef;
import org.hkijena.acaq5.extensions.parameters.table.ParameterTable;
import org.scijava.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generates {@link org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData} objects
 */
@ACAQDocumentation(name = "Define multiple parameters", description = "Defines algorithm parameters that can be consumed by a multi-parameter algorithm")
@AlgorithmOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
public class ParametersDataTableDefinition extends ACAQAlgorithm {

    private ACAQAlgorithmDeclarationRef targetAlgorithm = new ACAQAlgorithmDeclarationRef();
    private ParameterTable parameterTable;

    /**
     * Creates a new instance
     *
     * @param declaration the algorithm declaration
     */
    public ParametersDataTableDefinition(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ParametersDataTableDefinition(ParametersDataTableDefinition other) {
        super(other);
        if (other.targetAlgorithm != null)
            this.targetAlgorithm = new ACAQAlgorithmDeclarationRef(other.targetAlgorithm.getDeclaration());
        this.parameterTable = new ParameterTable(other.parameterTable);
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
        if (targetAlgorithm == null || targetAlgorithm.getDeclaration() == null) {
            report.reportIsInvalid("No algorithm selected!",
                    "Parameters are defined based on an algorithm.",
                    "Please select an algorithm.",
                    this);
        } else {
            for (int row = 0; row < parameterTable.getRowCount(); ++row) {
                ParametersData data = new ParametersData();
                for (int col = 0; col < parameterTable.getColumnCount(); ++col) {
                    ParameterTable.ParameterColumn column = parameterTable.getColumn(col);
                    data.getParameterData().put(column.getKey(), parameterTable.getValueAt(row, col));
                }

                ACAQGraphNode algorithm = targetAlgorithm.getDeclaration().newInstance();
                Map<String, ACAQParameterAccess> parameters = ACAQParameterTree.getParameters(algorithm);
                for (Map.Entry<String, Object> entry : data.getParameterData().entrySet()) {
                    parameters.get(entry.getKey()).set(entry.getValue());
                }
                report.forCategory("Parameter row " + (row + 1)).report(algorithm);
            }
        }
    }

    @ACAQParameter("parameter-table")
    @ACAQDocumentation(name = "Parameters", description = "Parameters that are generated")
    public ParameterTable getParameterTable() {
        return parameterTable;
    }

    @ACAQParameter("parameter-table")
    public void setParameterTable(ParameterTable parameterTable) {
        this.parameterTable = parameterTable;
        if (parameterTable.getRowGenerator() == null) {
            buildRowGenerator();
        }

    }

    private void buildRowGenerator() {
        if (parameterTable == null || targetAlgorithm == null || targetAlgorithm.getDeclaration() == null)
            return;
        parameterTable.setRowGenerator(() -> {
            ACAQGraphNode instance = targetAlgorithm.getDeclaration().newInstance();
            Map<String, ACAQParameterAccess> parameters = ACAQParameterTree.getParameters(instance);
            List<Object> row = new ArrayList<>();
            for (int col = 0; col < parameterTable.getColumnCount(); ++col) {
                String key = parameterTable.getColumn(col).getKey();
                row.add(parameters.get(key).get(Object.class));
            }
            return row;
        });
    }

    @ACAQParameter(value = "target-algorithm", priority = Priority.HIGH)
    @ACAQDocumentation(name = "Algorithm", description = "The algorithm the parameters are created for")
    public ACAQAlgorithmDeclarationRef getTargetAlgorithm() {
        return targetAlgorithm;
    }

    @ACAQParameter("target-algorithm")
    public void setTargetAlgorithm(ACAQAlgorithmDeclarationRef targetAlgorithm) {
        this.targetAlgorithm = targetAlgorithm;
        recreateParameterTable();
    }

    private void recreateParameterTable() {
        this.parameterTable = new ParameterTable();
        if (targetAlgorithm != null && targetAlgorithm.getDeclaration() != null) {
            ACAQGraphNode instance = targetAlgorithm.getDeclaration().newInstance();
            Map<String, ACAQParameterAccess> parameters = ACAQParameterTree.getParameters(instance);
            for (Map.Entry<String, ACAQParameterAccess> entry : parameters.entrySet()) {
                ACAQParameterAccess access = entry.getValue();
                if (!access.getVisibility().isVisibleIn(ACAQParameterVisibility.TransitiveVisible))
                    continue;
                parameterTable.addColumn(new ParameterTable.ParameterColumn(
                        access.getName(),
                        entry.getKey(),
                        access.getFieldClass()
                ), null);
            }
            buildRowGenerator();
            parameterTable.addRow();
        }

    }
}
