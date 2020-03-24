package org.hkijena.acaq5.api.compartments;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.compartments.datatypes.ACAQCompartmentOutputData;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

@Plugin(type = ACAQJavaExtension.class)
public class ACAQCompartmentsExtensionService extends ACAQPrepackagedDefaultJavaExtension {

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
    public void register(ACAQDefaultRegistry registryService) {
        registryService.getAlgorithmRegistry().register(ACAQCompartmentOutput.class);
        registryService.getAlgorithmRegistry().register(ACAQProjectCompartment.class);

        registryService.getDatatypeRegistry().register("acaq:compartment-output", ACAQCompartmentOutputData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQCompartmentOutputData.class,
                ResourceUtils.getPluginResource("icons/data-types/graph-compartment.png"));
    }
}
