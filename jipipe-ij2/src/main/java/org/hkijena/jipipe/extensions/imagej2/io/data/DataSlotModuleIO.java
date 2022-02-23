package org.hkijena.jipipe.extensions.imagej2.io.data;

import net.imagej.Dataset;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.service.AbstractService;

public abstract class DataSlotModuleIO<ModuleDataType,JIPipeDataType extends JIPipeData> extends AbstractService implements ImageJ2ModuleIO {

    @Override
    public Class<?> getAcceptedModuleFieldClass() {
        return getModuleDataType();
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
    public boolean transferFromJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        String slotName = node.getModuleNodeInfo().getOutputSlotName(moduleItem);
        JIPipeDataType obj = node.getOutputSlot(slotName).getData(0, getJIPipeDataType(), progressInfo);
        ModuleDataType converted = convertJIPipeToModuleData(obj);
        moduleItem.setValue(module, converted);
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        ModuleDataType obj = (ModuleDataType) moduleItem.getValue(module);
        JIPipeDataType converted = convertModuleToJIPipeData(obj);
        String slotName = node.getModuleNodeInfo().getInputSlotName(moduleItem);
        JIPipeDataSlot slot = node.getInputSlot(slotName);
        slot.clearData();
        slot.addData(converted, progressInfo);
        return true;
    }

    public abstract ModuleDataType convertJIPipeToModuleData(JIPipeDataType obj);

    public abstract JIPipeDataType convertModuleToJIPipeData(ModuleDataType obj);

    public abstract Class<ModuleDataType> getModuleDataType();

    public abstract Class<JIPipeDataType> getJIPipeDataType();
}
