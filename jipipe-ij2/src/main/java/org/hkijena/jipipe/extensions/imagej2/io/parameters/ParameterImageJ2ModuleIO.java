package org.hkijena.jipipe.extensions.imagej2.io.parameters;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.utils.StringUtils;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.service.AbstractService;

/**
 * Handles input parameters (passed from/to JIPipe parameters)
 * @param <ModuleType> the type used in the module
 * @param <JIPipeType> the type used in JIPipe
 */
public abstract class ParameterImageJ2ModuleIO<ModuleType, JIPipeType> extends AbstractService implements ImageJ2ModuleIO {
    @Override
    public Class<?> getAcceptedModuleFieldClass() {
        return getModuleClass();
    }

    @Override
    public void install(ImageJ2ModuleNodeInfo nodeInfo, ModuleItem<?> moduleItem) {
    }

    @Override
    public void install(ImageJ2ModuleNode node, ModuleItem<?> moduleItem) {
        if(!node.getModuleParameterAssignment().containsValue(moduleItem)) {
            String parameterKey = StringUtils.makeUniqueString(StringUtils.orElse(moduleItem.getPersistKey(), moduleItem.getName()).toLowerCase().replace(' ', '-'),
                    "-",
                    node.getModuleParameterAssignment().keySet());
            node.getModuleParameters().addParameter(parameterKey, Double.class, moduleItem.getName(), moduleItem.getDescription());
            node.getModuleParameterAssignment().put(parameterKey, moduleItem);
        }
    }

    @Override
    public boolean transferFromJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        JIPipeType value = node.getParameter(node.getModuleParameterAssignment().inverse().get(moduleItem), getJIPipeParameterClass());
        moduleItem.setValue(module, jiPipeToModule(value));
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        return true;
    }

    @Override
    public boolean handlesInput() {
        return true;
    }

    @Override
    public boolean handlesOutput() {
        return false;
    }

    public abstract JIPipeType moduleToJIPipe(ModuleType obj);

    public abstract ModuleType jiPipeToModule(JIPipeType obj);

    public abstract Class<JIPipeType> getJIPipeParameterClass();

    public abstract Class<ModuleType> getModuleClass();
}
