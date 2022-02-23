package org.hkijena.jipipe.extensions.imagej2.io.parameters;

import org.apache.commons.lang.WordUtils;
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
public abstract class ParameterImageJ2ModuleInput<ModuleType, JIPipeType> extends AbstractService implements ImageJ2ModuleIO {
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
            String parameterName = WordUtils.capitalize(String.join(" ", org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase(moduleItem.getName())));
            node.getModuleParameters().addParameter(parameterKey, getJIPipeParameterClass(), parameterName, moduleItem.getDescription());
            Object result = moduleItem.getValue(node.getModuleInstance());
            if(result != null && getModuleClass().isAssignableFrom(result.getClass())) {
                node.getModuleParameters().get(parameterKey).set(convertFromModuleToJIPipe((ModuleType) result));
            }
            node.getModuleParameterAssignment().put(parameterKey, moduleItem);
        }
    }

    @Override
    public boolean transferFromJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        JIPipeType value = node.getParameter(node.getModuleParameterAssignment().inverse().get(moduleItem), getJIPipeParameterClass());
        moduleItem.setValue(module, convertFromJIPipeToModule(value));
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

    public abstract JIPipeType convertFromModuleToJIPipe(ModuleType obj);

    public abstract ModuleType convertFromJIPipeToModule(JIPipeType obj);

    public abstract Class<JIPipeType> getJIPipeParameterClass();

    public abstract Class<ModuleType> getModuleClass();
}
