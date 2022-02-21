package org.hkijena.jipipe.extensions.imagej2.io;

import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * Dummy IO for service parameters
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class ServiceImageJ2ModuleIO extends AbstractService implements ImageJ2ModuleIO {
    @Override
    public Class<?> getAcceptedModuleFieldClass() {
        return Service.class;
    }

    @Override
    public boolean transferFromJIPipe(JIPipeAlgorithm algorithm, ModuleItem<?> moduleItem, Module module) {
        return true;
    }

    @Override
    public boolean transferToJIPipe(JIPipeAlgorithm algorithm, ModuleItem<?> moduleItem, Module module) {
        return true;
    }
}
