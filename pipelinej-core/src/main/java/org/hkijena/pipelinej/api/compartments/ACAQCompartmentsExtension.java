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

package org.hkijena.pipelinej.api.compartments;

import org.hkijena.pipelinej.ACAQJavaExtension;
import org.hkijena.pipelinej.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.pipelinej.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.pipelinej.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.pipelinej.api.compartments.datatypes.ACAQCompartmentOutputData;
import org.hkijena.pipelinej.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.pipelinej.utils.ResourceUtils;
import org.hkijena.pipelinej.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides compartment management functionality
 */
@Plugin(type = ACAQJavaExtension.class)
public class ACAQCompartmentsExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Compartment management";
    }

    @Override
    public String getDescription() {
        return "Data types required for graph compartment management";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:compartments";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {
        registerAlgorithm("io-interface", IOInterfaceAlgorithm.class, UIUtils.getAlgorithmIconURL("arrows-alt-h.png"));
        registerAlgorithm("acaq:compartment-output", ACAQCompartmentOutput.class, UIUtils.getAlgorithmIconURL("graph-compartment.png"));
        registerAlgorithm("acaq:project-compartment", ACAQProjectCompartment.class, UIUtils.getAlgorithmIconURL("graph-compartment.png"));

        registerDatatype("acaq:compartment-output", ACAQCompartmentOutputData.class,
                ResourceUtils.getPluginResource("icons/data-types/graph-compartment.png"),
                null, null);
    }
}
