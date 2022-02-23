package org.hkijena.jipipe.extensions.imagej2.io.data.output;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.service.AbstractService;

public abstract class DataSlotModuleOutput<ModuleDataType,JIPipeDataType extends JIPipeData> extends AbstractService implements ImageJ2ModuleIO {

    @Override
    public Class<?> getAcceptedModuleFieldClass() {
        return getModuleDataType();
    }

    @Override
    public boolean handlesInput() {
        return false;
    }

    @Override
    public boolean handlesOutput() {
        return true;
    }

    @Override
    public void install(ImageJ2ModuleNode node, ModuleItem<?> moduleItem) {

    }

    @Override
    public void install(ImageJ2ModuleNodeInfo nodeInfo, ModuleItem<?> moduleItem) {
        if(moduleItem.isInput()) {
            nodeInfo.addInputSlotForModuleItem(moduleItem, getJIPipeDataType());
        }
        if(moduleItem.isOutput()) {
            nodeInfo.addOutputSlotForModuleItem(moduleItem, getJIPipeDataType());
        }
    }

    @Override
    public boolean transferFromJIPipe(ImageJ2ModuleNode node, JIPipeDataBatch dataBatch, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2ModuleNode node, JIPipeDataBatch dataBatch, ParametersData moduleOutputParameters, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        ModuleDataType obj = (ModuleDataType) moduleItem.getValue(module);
        JIPipeDataType converted = convertModuleToJIPipeData(obj);
        String slotName = node.getModuleNodeInfo().getInputSlotName(moduleItem);
        dataBatch.addOutputData(slotName, converted, progressInfo);
        return true;
    }

    public abstract ModuleDataType convertJIPipeToModuleData(JIPipeDataType obj);

    public abstract JIPipeDataType convertModuleToJIPipeData(ModuleDataType obj);

    public abstract Class<ModuleDataType> getModuleDataType();

    public abstract Class<JIPipeDataType> getJIPipeDataType();
}
