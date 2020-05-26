package org.hkijena.acaq5.api.compartments;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.acaq5.api.compartments.datatypes.ACAQCompartmentOutputData;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.utils.ResourceUtils;
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
        registerAlgorithm("io-interface", IOInterfaceAlgorithm.class);
        registerAlgorithm("acaq:compartment-output", ACAQCompartmentOutput.class);
        registerAlgorithm("acaq:project-compartment", ACAQProjectCompartment.class);

        registerDatatype("acaq:compartment-output", ACAQCompartmentOutputData.class,
                ResourceUtils.getPluginResource("icons/data-types/graph-compartment.png"),
                null, null);
    }
}
