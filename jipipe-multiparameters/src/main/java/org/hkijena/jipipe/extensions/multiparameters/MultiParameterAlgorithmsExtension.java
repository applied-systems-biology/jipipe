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

package org.hkijena.jipipe.extensions.multiparameters;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.registries.JIPipeAlgorithmRegistry;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.multiparameters.algorithms.MultiParameterAlgorithmDeclaration;
import org.hkijena.jipipe.extensions.multiparameters.datasources.ParametersDataDefinition;
import org.hkijena.jipipe.extensions.multiparameters.datasources.ParametersDataTableDefinition;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides capabilities to run multiple parameters
 */
@Plugin(type = JIPipeJavaExtension.class)
public class MultiParameterAlgorithmsExtension extends JIPipePrepackagedDefaultJavaExtension {

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
        JIPipeAlgorithmRegistry.getInstance().getEventBus().register(this);

        // Register algorithms
        registerAlgorithm("parameters-define", ParametersDataDefinition.class);
        registerAlgorithm("parameters-define-table", ParametersDataTableDefinition.class);
        registerAlgorithm(new MultiParameterAlgorithmDeclaration());
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:multi-parameters-algorithms";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }
}
