package org.hkijena.jipipe.extensions.imagej2.io.parameters.input;

import org.apache.commons.lang.WordUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2OpNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2OpNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.io.parameters.ParameterModuleIO;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.utils.StringUtils;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.service.AbstractService;

/**
 * Handles input parameters (passed from/to JIPipe parameters)
 *
 * @param <ModuleType> the type used in the module
 * @param <JIPipeType> the type used in JIPipe
 */
public abstract class ParameterImageJ2ModuleInput<ModuleType, JIPipeType> extends AbstractService implements ParameterModuleIO {
    @Override
    public Class<?> getAcceptedModuleFieldClass() {
        return getModuleClass();
    }

    @Override
    public void install(ImageJ2OpNodeInfo nodeInfo, ModuleItem<?> moduleItem) {
    }

    @Override
    public void install(ImageJ2OpNode node, ModuleItem<?> moduleItem) {
        if (!node.moduleItemIsParameter(moduleItem)) {
            String parameterKey = StringUtils.makeUniqueString(StringUtils.orElse(moduleItem.getPersistKey(), moduleItem.getName()).toLowerCase().replace(' ', '-'),
                    "-",
                    node.getModuleParameters().getParameters().keySet());
            String parameterName = WordUtils.capitalize(String.join(" ", org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase(moduleItem.getName())));
            node.addParameterForModuleItem(parameterKey, parameterName, moduleItem.getDescription(), getJIPipeParameterClass(), moduleItem);
            Object result = moduleItem.getValue(node.getReferenceModuleInstance());
            if (result != null && getModuleClass().isAssignableFrom(result.getClass())) {
                node.getModuleParameters().get(parameterKey).set(convertFromModuleToJIPipe((ModuleType) result));
            }
        }
    }

    @Override
    public boolean transferFromJIPipe(ImageJ2OpNode node, JIPipeSingleIterationStep iterationStep, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        JIPipeParameterAccess access = node.getModuleItemParameterAccess(moduleItem);
        JIPipeType value = access.get(getJIPipeParameterClass());
        moduleItem.setValue(module, convertFromJIPipeToModule(value));
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2OpNode node, JIPipeSingleIterationStep iterationStep, ParametersData moduleOutputParameters, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
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
