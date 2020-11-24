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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;

import java.util.Map;

/**
 * Generates {@link org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData} objects
 */
@JIPipeDocumentation(name = "Define parameter", description = "Defines an algorithm parameter that can be consumed by a multi-parameter algorithm")
@JIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ParametersDataDefinition extends JIPipeAlgorithm {

    private GeneratedParameters parameters;

    /**
     * Creates a new instance
     *
     * @param info the algorithm info
     */
    public ParametersDataDefinition(JIPipeNodeInfo info) {
        super(info);
        this.parameters = new GeneratedParameters(this);
        registerSubParameter(parameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ParametersDataDefinition(ParametersDataDefinition other) {
        super(other);
        this.parameters = new GeneratedParameters(other.parameters);
        this.parameters.setParent(this);
        registerSubParameter(parameters);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        ParametersData result = new ParametersData();
        for (Map.Entry<String, JIPipeParameterAccess> entry : parameters.getParameters().entrySet()) {
            if (entry.getValue().getVisibility().isVisibleIn(JIPipeParameterVisibility.TransitiveVisible)) {
                result.getParameterData().put(entry.getKey(), entry.getValue().get(Object.class));
            }
        }
        getFirstOutputSlot().addData(result, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Parameters").report(parameters);
    }

    @JIPipeDocumentation(name = "Parameters", description = "Following parameters are generated:")
    @JIPipeParameter(value = "parameters", persistence = JIPipeParameterPersistence.Object)
    public GeneratedParameters getParameters() {
        return parameters;
    }
}
