package org.hkijena.acaq5.api.compartments;

import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.compartments.datatypes.ACAQCompartmentOutputData;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Plugin(type = ACAQExtensionService.class)
public class ACAQCompartmentsExtensionService extends AbstractService implements ACAQExtensionService {
    @Override
    public String getName() {
        return "Compartments";
    }

    @Override
    public String getDescription() {
        return "Extension data types and algorithms for graph compartments";
    }

    @Override
    public List<String> getAuthors() {
        return Arrays.asList("Zoltán Cseresnyés", "Ruman Gerst");
    }

    @Override
    public String getURL() {
        return "https://applied-systems-biology.github.io/acaq5/";
    }

    @Override
    public String getLicense() {
        return "BSD-2";
    }

    @Override
    public URL getIconURL() {
        return ResourceUtils.getPluginResource("logo-400.png");
    }

    @Override
    public void register(ACAQRegistryService registryService) {
        registryService.getAlgorithmRegistry().register(ACAQCompartmentOutput.class);
        registryService.getAlgorithmRegistry().register(ACAQProjectCompartment.class);

        registryService.getDatatypeRegistry().register("acaq:compartment-output", ACAQCompartmentOutputData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQCompartmentOutputData.class,
                ResourceUtils.getPluginResource("icons/data-types/graph-compartment.png"));
    }
}
