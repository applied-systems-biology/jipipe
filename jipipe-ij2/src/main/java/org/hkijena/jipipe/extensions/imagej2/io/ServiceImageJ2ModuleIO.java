package org.hkijena.jipipe.extensions.imagej2.io;

import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNodeInfo;
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
    public void install(ImageJ2ModuleNodeInfo nodeInfo, ModuleItem<?> moduleItem) {

    }

    @Override
    public void install(ImageJ2ModuleNode node, ModuleItem<?> moduleItem) {

    }

    @Override
    public boolean transferFromJIPipe(ImageJ2ModuleNode node, ModuleItem<?> moduleItem, Module module) {
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2ModuleNode node, ModuleItem<?> moduleItem, Module module) {
        return true;
    }
}
