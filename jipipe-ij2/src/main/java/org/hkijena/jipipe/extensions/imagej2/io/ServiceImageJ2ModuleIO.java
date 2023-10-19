package org.hkijena.jipipe.extensions.imagej2.io;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2OpNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2OpNodeInfo;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
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
    public void install(ImageJ2OpNodeInfo nodeInfo, ModuleItem<?> moduleItem) {

    }

    @Override
    public void install(ImageJ2OpNode node, ModuleItem<?> moduleItem) {

    }

    @Override
    public boolean handlesInput() {
        return true;
    }

    @Override
    public boolean handlesOutput() {
        return true;
    }

    @Override
    public boolean transferFromJIPipe(ImageJ2OpNode node, JIPipeSingleIterationStep iterationStep, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2OpNode node, JIPipeSingleIterationStep iterationStep, ParametersData moduleOutputParameters, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        return true;
    }
}
