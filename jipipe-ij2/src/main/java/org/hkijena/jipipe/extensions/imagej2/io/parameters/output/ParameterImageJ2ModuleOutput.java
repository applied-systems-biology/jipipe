package org.hkijena.jipipe.extensions.imagej2.io.parameters.output;

import net.imglib2.type.numeric.RealType;
import org.apache.commons.lang.WordUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.multiparameters.algorithms.ConvertParametersToTableAlgorithm;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.utils.StringUtils;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.service.AbstractService;

/**
 * Handles input parameters (passed from/to JIPipe parameters)
 * @param <ModuleType> the type used in the module
 * @param <JIPipeType> the type used in JIPipe
 */
public abstract class ParameterImageJ2ModuleOutput<ModuleType, JIPipeType> extends AbstractService implements ImageJ2ModuleIO {
    @Override
    public Class<?> getAcceptedModuleFieldClass() {
        return getModuleClass();
    }

    @Override
    public void install(ImageJ2ModuleNodeInfo nodeInfo, ModuleItem<?> moduleItem) {
        nodeInfo.getOrCreateParameterDataOutputSlot();
    }

    @Override
    public void install(ImageJ2ModuleNode node, ModuleItem<?> moduleItem) {
    }

    @Override
    public boolean transferFromJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot outputSlot = node.getOutputSlot(node.getModuleNodeInfo().getOrCreateParameterDataOutputSlot().slotName());
        if(outputSlot.isEmpty()) {
            outputSlot.addData(new ParametersData(), progressInfo);
        }
        ParametersData parametersData = outputSlot.getData(0, ParametersData.class, progressInfo);
        parametersData.getParameterData().put(moduleItem.getName(), convertFromModuleToJIPipe((ModuleType) moduleItem.getValue(module)));
        return true;
    }

    @Override
    public boolean handlesInput() {
        return false;
    }

    @Override
    public boolean handlesOutput() {
        return true;
    }

    public abstract JIPipeType convertFromModuleToJIPipe(ModuleType obj);

    public abstract ModuleType convertFromJIPipeToModule(JIPipeType obj);

    public abstract Class<JIPipeType> getJIPipeParameterClass();

    public abstract Class<ModuleType> getModuleClass();
}
