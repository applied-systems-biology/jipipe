package org.hkijena.jipipe.extensions.imagej2;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.scijava.Initializable;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleItem;

import java.util.Map;

public class ImageJ2ModuleNode extends JIPipeIteratingAlgorithm {

    private final Module moduleInstance;
    private final JIPipeDynamicParameterCollection moduleParameters = new JIPipeDynamicParameterCollection(false);
    private final BiMap<String, ModuleItem<?>> moduleParameterAssignment = HashBiMap.create();

    public ImageJ2ModuleNode(JIPipeNodeInfo info) {
        super(info, createSlotConfiguration(info));
        try {
            this.moduleInstance =((ImageJ2ModuleNodeInfo) info).getModuleInfo().createModule();
        } catch (ModuleException e) {
            throw new RuntimeException(e);
        }
        ImageJ2ModuleNodeInfo moduleNodeInfo = (ImageJ2ModuleNodeInfo) info;
        for (Map.Entry<ModuleItem<?>, ImageJ2ModuleIO> entry : moduleNodeInfo.getInputModuleIO().entrySet()) {
            entry.getValue().install(this, entry.getKey());
        }
        for (Map.Entry<ModuleItem<?>, ImageJ2ModuleIO> entry : moduleNodeInfo.getOutputModuleIO().entrySet()) {
            entry.getValue().install(this, entry.getKey());
        }
    }

    private static JIPipeSlotConfiguration createSlotConfiguration(JIPipeNodeInfo info) {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = new JIPipeDefaultMutableSlotConfiguration();
        for (JIPipeInputSlot inputSlot : info.getInputSlots()) {
            slotConfiguration.addSlot(new JIPipeDataSlotInfo(inputSlot), false);
        }
        for (JIPipeOutputSlot outputSlot : info.getOutputSlots()) {
            slotConfiguration.addSlot(new JIPipeDataSlotInfo(outputSlot), false);
        }
        slotConfiguration.setInputSealed(true);
        slotConfiguration.setOutputSealed(true);
        return slotConfiguration;
    }

    public ImageJ2ModuleNode(ImageJ2ModuleNode other) {
        super(other);
        this.moduleInstance = other.getModuleInstance();
    }

    public ImageJ2ModuleNodeInfo getModuleNodeInfo() {
        return (ImageJ2ModuleNodeInfo) getInfo();
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Module iterationInstance;
        ParametersData moduleOutputParameters = new ParametersData();
        try {
            iterationInstance = getModuleNodeInfo().getModuleInfo().createModule();
            iterationInstance.initialize();
        } catch (ModuleException e) {
            throw new RuntimeException(e);
        }

        // Copy from JIPipe
        for (Map.Entry<ModuleItem<?>, ImageJ2ModuleIO> entry : getModuleNodeInfo().getInputModuleIO().entrySet()) {
            entry.getValue().transferFromJIPipe(this, dataBatch, entry.getKey(), iterationInstance, progressInfo);
        }

        // Initialize Ops after setting parameters
        if(iterationInstance.getDelegateObject() instanceof net.imagej.ops.Initializable) {
            ((net.imagej.ops.Initializable) iterationInstance.getDelegateObject()).initialize();
        }

        // Run operation
        iterationInstance.run();

        // Copy back to JIPipe
        for (Map.Entry<ModuleItem<?>, ImageJ2ModuleIO> entry : getModuleNodeInfo().getInputModuleIO().entrySet()) {
            entry.getValue().transferToJIPipe(this, dataBatch, moduleOutputParameters, entry.getKey(), iterationInstance, progressInfo);
        }
        if(getModuleNodeInfo().hasParameterDataOutputSlot()) {
            dataBatch.addOutputData(getModuleNodeInfo().getOrCreateParameterDataOutputSlot().slotName(), moduleOutputParameters, progressInfo);
        }
    }

    public BiMap<String, ModuleItem<?>> getModuleParameterAssignment() {
        return moduleParameterAssignment;
    }

    @JIPipeDocumentation(name = "ImageJ2 parameters", description = "Following parameters were extracted from the ImageJ2 interface:")
    @JIPipeParameter("module-parameters")
    public JIPipeDynamicParameterCollection getModuleParameters() {
        return moduleParameters;
    }

    public Module getModuleInstance() {
        return moduleInstance;
    }
}
