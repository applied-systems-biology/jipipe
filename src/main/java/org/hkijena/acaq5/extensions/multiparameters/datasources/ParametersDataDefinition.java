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
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.acaq5.extensions.parameters.references.ACAQAlgorithmDeclarationRef;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generates {@link org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData} objects
 */
@ACAQDocumentation(name = "Define parameter", description = "Defines an algorithm parameter that can be consumed by a multi-parameter algorithm")
@AlgorithmOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
public class ParametersDataDefinition extends ACAQAlgorithm {

    private ACAQDynamicParameterCollection parameters;

    /**
     * Creates a new instance
     *
     * @param declaration the algorithm declaration
     */
    public ParametersDataDefinition(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        this.parameters = new ACAQDynamicParameterCollection(true, ACAQParameterTypeRegistry.getInstance().getRegisteredParameters().values());
        registerSubParameter(parameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ParametersDataDefinition(ParametersDataDefinition other) {
        super(other);
        this.parameters = new ACAQDynamicParameterCollection(other.parameters);
        registerSubParameter(parameters);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ParametersData result = new ParametersData();
        for (Map.Entry<String, ACAQParameterAccess> entry : parameters.getParameters().entrySet()) {
            if (entry.getValue().getVisibility().isVisibleIn(ACAQParameterVisibility.TransitiveVisible)) {
                result.getParameterData().put(entry.getKey(), entry.getValue().get(Object.class));
            }
        }
        getFirstOutputSlot().addData(result);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Parameters").report(parameters);
    }

    @ACAQDocumentation(name = "Parameters", description = "Following parameters are generated:")
    @ACAQParameter("parameters")
    public ACAQDynamicParameterCollection getParameters() {
        return parameters;
    }
}
