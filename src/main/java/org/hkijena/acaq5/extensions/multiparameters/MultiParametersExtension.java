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

package org.hkijena.acaq5.extensions.multiparameters;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.multiparameters.algorithms.MultiParameterAlgorithmDeclaration;
import org.hkijena.acaq5.extensions.multiparameters.datasources.ParametersDataDefinition;
import org.hkijena.acaq5.extensions.multiparameters.datasources.ParametersDataTableDefinition;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides capabilities to run multiple parameters
 */
@Plugin(type = ACAQJavaExtension.class)
public class MultiParametersExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Multi parameter algorithms";
    }

    @Override
    public String getDescription() {
        return "Extension that provides capabilities to run multiple parameters";
    }

    @Override
    public void register() {
        ACAQAlgorithmRegistry.getInstance().getEventBus().register(this);

        // Register data types
        registerDatatype("parameters", ParametersData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type-parameters.png"),
                null,
                null);

        // Register algorithms
        registerAlgorithm("parameters-define", ParametersDataDefinition.class);
        registerAlgorithm("parameters-define-table", ParametersDataTableDefinition.class);
        registerAlgorithm(new MultiParameterAlgorithmDeclaration());
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:multi-parameters";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }
}
