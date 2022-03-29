package org.hkijena.jipipe.extensions.imagej2.io.data.input;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2OpNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2OpNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.io.data.DataSlotModuleIO;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.service.AbstractService;

public abstract class DataSlotModuleInput<ModuleDataType, JIPipeDataType extends JIPipeData> extends AbstractService implements DataSlotModuleIO {

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
        return false;
    }

    @Override
    public void install(ImageJ2OpNode node, ModuleItem<?> moduleItem) {

    }

    @Override
    public void install(ImageJ2OpNodeInfo nodeInfo, ModuleItem<?> moduleItem) {
        nodeInfo.addInputSlotForModuleItem(moduleItem, getJIPipeDataType());
    }

    @Override
    public boolean transferFromJIPipe(ImageJ2OpNode node, JIPipeDataBatch dataBatch, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        String slotName = node.getModuleNodeInfo().getInputSlotName(moduleItem);
        if (dataBatch.getInputRow(slotName) >= 0) {
            JIPipeDataType obj = dataBatch.getInputData(slotName, getJIPipeDataType(), progressInfo);
            ModuleDataType converted = convertJIPipeToModuleData(obj);
            moduleItem.setValue(module, converted);
        } else {
            progressInfo.log("Input slot " + slotName + " is empty. Skipping.");
        }
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2OpNode node, JIPipeDataBatch dataBatch, ParametersData moduleOutputParameters, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        return true;
    }

    public abstract ModuleDataType convertJIPipeToModuleData(JIPipeDataType obj);

    public abstract JIPipeDataType convertModuleToJIPipeData(ModuleDataType obj);

    public abstract Class<ModuleDataType> getModuleDataType();

    public abstract Class<JIPipeDataType> getJIPipeDataType();
}
