package org.hkijena.jipipe.extensions.imagej2;

import net.imagej.ops.Op;
import net.imagej.ops.OpInfo;
import net.imagej.ops.OpService;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.scijava.Initializable;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleService;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class ImageJ2OpNode extends JIPipeIteratingAlgorithm {

    private final Module referenceModuleInstance;
    private final JIPipeDynamicParameterCollection moduleParameters;
    private final Map<String, ModuleItem<?>> parameterToModuleItemAssignment = new HashMap<>();
    private final Map<ModuleItem<?>, String> moduleItemToParameterAssignment = new IdentityHashMap<>();
    private final Map<ModuleItem<?>, Object> moduleItemDefaults = new IdentityHashMap<>();

    public ImageJ2OpNode(JIPipeNodeInfo info) {
        super(info, createSlotConfiguration(info));
        this.moduleParameters = new JIPipeDynamicParameterCollection(false);

        // Initialize the module
        ImageJ2OpNodeInfo opNodeInfo = (ImageJ2OpNodeInfo) info;
        OpInfo opInfo = opNodeInfo.getOpInfo();
        OpService opService = opNodeInfo.getContext().getService(OpService.class);
        ModuleService moduleService = opNodeInfo.getContext().getService(ModuleService.class);
        this.referenceModuleInstance = moduleService.createModule(opInfo.cInfo());
        Op op = (Op) referenceModuleInstance.getDelegateObject();
        op.setEnvironment(opService);

        // Install the module IOs
        for (Map.Entry<ModuleItem<?>, ImageJ2ModuleIO> entry : opNodeInfo.getInputModuleIO().entrySet()) {
            entry.getValue().install(this, entry.getKey());
            moduleItemDefaults.put(entry.getKey(), entry.getKey().getValue(referenceModuleInstance));
        }
        for (Map.Entry<ModuleItem<?>, ImageJ2ModuleIO> entry : opNodeInfo.getOutputModuleIO().entrySet()) {
            entry.getValue().install(this, entry.getKey());
            moduleItemDefaults.put(entry.getKey(), entry.getKey().getValue(referenceModuleInstance));
        }
    }

    public ImageJ2OpNode(ImageJ2OpNode other) {
        super(other);
        this.referenceModuleInstance = other.getReferenceModuleInstance();
        this.moduleParameters = new JIPipeDynamicParameterCollection(other.moduleParameters);
        this.moduleItemToParameterAssignment.putAll(other.moduleItemToParameterAssignment);
        this.parameterToModuleItemAssignment.putAll(other.parameterToModuleItemAssignment);
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

    public ImageJ2OpNodeInfo getModuleNodeInfo() {
        return (ImageJ2OpNodeInfo) getInfo();
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        try {
            ParametersData moduleOutputParameters = new ParametersData();

            // Create new module instance
            ImageJ2OpNodeInfo opNodeInfo = getModuleNodeInfo();
            OpInfo opInfo = opNodeInfo.getOpInfo();
            OpService opService = opNodeInfo.getContext().getService(OpService.class);
            ModuleService moduleService = opNodeInfo.getContext().getService(ModuleService.class);
            Module moduleInstance = moduleService.createModule(opInfo.cInfo());
            Op op = (Op) moduleInstance.getDelegateObject();
            op.setEnvironment(opService);

            // Copy from JIPipe
            for (Map.Entry<ModuleItem<?>, ImageJ2ModuleIO> entry : getModuleNodeInfo().getInputModuleIO().entrySet()) {
                entry.getValue().transferFromJIPipe(this, iterationStep, entry.getKey(), moduleInstance, progressInfo);
            }

            // Run the initializer function
            if (moduleInstance.getDelegateObject() instanceof Initializable) {
                ((Initializable) moduleInstance.getDelegateObject()).initialize();
            }
            if (moduleInstance.getDelegateObject() instanceof net.imagej.ops.Initializable) {
                ((net.imagej.ops.Initializable) moduleInstance.getDelegateObject()).initialize();
            }

            // Run operation
            moduleInstance.run();

            // Copy back to JIPipe
            for (Map.Entry<ModuleItem<?>, ImageJ2ModuleIO> entry : getModuleNodeInfo().getOutputModuleIO().entrySet()) {
                entry.getValue().transferToJIPipe(this, iterationStep, moduleOutputParameters, entry.getKey(), moduleInstance, progressInfo);
            }
            if (getModuleNodeInfo().hasParameterDataOutputSlot()) {
                iterationStep.addOutputData(getModuleNodeInfo().getOrCreateParameterDataOutputSlot().slotName(), moduleOutputParameters, progressInfo);
            }
        }
        finally {
            for (Map.Entry<ModuleItem<?>, Object> entry : moduleItemDefaults.entrySet()) {
                ModuleItem item = entry.getKey();
                item.setValue(referenceModuleInstance, entry.getValue());
            }
        }
    }

    public boolean moduleItemIsParameter(ModuleItem<?> moduleItem) {
        return moduleItemToParameterAssignment.containsKey(moduleItem);
    }

    public JIPipeParameterAccess getModuleItemParameterAccess(ModuleItem<?> moduleItem) {
        return moduleParameters.getParameterAccess(moduleItemToParameterAssignment.get(moduleItem));
    }

    public void addParameterForModuleItem(String key, String name, String description, Class<?> type, ModuleItem<?> moduleItem) {
        moduleParameters.addParameter(key, type, name, description);
        moduleItemToParameterAssignment.put(moduleItem, key);
        parameterToModuleItemAssignment.put(key, moduleItem);
    }

    @JIPipeDocumentation(name = "ImageJ2 parameters", description = "Following parameters were extracted from the ImageJ2 interface:")
    @JIPipeParameter("module-parameters")
    public JIPipeDynamicParameterCollection getModuleParameters() {
        return moduleParameters;
    }

    public Module getReferenceModuleInstance() {
        return referenceModuleInstance;
    }
}
