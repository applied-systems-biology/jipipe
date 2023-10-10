package org.hkijena.jipipe.extensions.clij2;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.macro.CLIJImageJProcessor;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import net.haesleinhuepf.clij2.CLIJ2;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.scijava.InstantiableException;

import java.util.HashMap;
import java.util.Map;

public class CLIJCommandNode extends JIPipeIteratingAlgorithm {

    private final JIPipeDynamicParameterCollection clijParameters;
    private CLIJMacroPlugin pluginInstance;
    private boolean avoidGPUMemory = true;

    public CLIJCommandNode(JIPipeNodeInfo info) {
        super(info);
        this.clijParameters = new JIPipeDynamicParameterCollection(((CLIJCommandNodeInfo) info).getNodeParameters());
        updateSlots();
    }

    public CLIJCommandNode(CLIJCommandNode other) {
        super(other);
        this.clijParameters = new JIPipeDynamicParameterCollection(other.clijParameters);
        this.avoidGPUMemory = other.avoidGPUMemory;
        updateSlots();
    }

    private void updateSlots() {
        if (avoidGPUMemory) {
            for (JIPipeDataSlotInfo info : getSlotConfiguration().getInputSlots().values()) {
                if (info.getDataClass().equals(CLIJImageData.class)) {
                    info.setDataClass(ImagePlusData.class);
                }
            }
            for (JIPipeDataSlotInfo info : getSlotConfiguration().getOutputSlots().values()) {
                if (info.getDataClass().equals(CLIJImageData.class)) {
                    info.setDataClass(ImagePlusData.class);
                }
            }
            for (JIPipeDataSlot slot : getInputSlots()) {
                if (slot.getAcceptedDataType().equals(CLIJImageData.class)) {
                    slot.setAcceptedDataType(ImagePlusData.class);
                }
            }
            for (JIPipeDataSlot slot : getOutputSlots()) {
                if (slot.getAcceptedDataType().equals(CLIJImageData.class)) {
                    slot.setAcceptedDataType(ImagePlusData.class);
                }
            }
            updateGraphNodeSlots();
            emitNodeSlotsChangedEvent();
        } else {
            for (JIPipeDataSlotInfo info : getSlotConfiguration().getInputSlots().values()) {
                if (info.getDataClass().equals(ImagePlusData.class)) {
                    info.setDataClass(CLIJImageData.class);
                }
            }
            for (JIPipeDataSlotInfo info : getSlotConfiguration().getOutputSlots().values()) {
                if (info.getDataClass().equals(ImagePlusData.class)) {
                    info.setDataClass(CLIJImageData.class);
                }
            }
            for (JIPipeDataSlot slot : getInputSlots()) {
                if (slot.getAcceptedDataType().equals(ImagePlusData.class)) {
                    slot.setAcceptedDataType(CLIJImageData.class);
                }
            }
            for (JIPipeDataSlot slot : getOutputSlots()) {
                if (slot.getAcceptedDataType().equals(ImagePlusData.class)) {
                    slot.setAcceptedDataType(CLIJImageData.class);
                }
            }
            updateGraphNodeSlots();
            emitNodeSlotsChangedEvent();
        }
    }

    @JIPipeDocumentation(name = "Avoid allocating GPU memory", description = "If enabled, the node will be reconfigured to only allocate data into the GPU memory if absolutely necessary. Please note that " +
            "the application of multiple GPU-based operations will be slower due to the repeated allocation and de-allocation of the images.")
    @JIPipeParameter("avoid-gpu-memory")
    public boolean isAvoidGPUMemory() {
        return avoidGPUMemory;
    }

    @JIPipeParameter("avoid-gpu-memory")
    public void setAvoidGPUMemory(boolean avoidGPUMemory) {
        this.avoidGPUMemory = avoidGPUMemory;
        updateSlots();
    }

    @JIPipeDocumentation(name = "CLIJ parameters", description = "Following parameters were extracted from the CLIJ2 operation:")
    @JIPipeParameter("clij-parameters")
    public JIPipeDynamicParameterCollection getClijParameters() {
        return clijParameters;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        CLIJCommandNodeInfo info = (CLIJCommandNodeInfo) getInfo();
        try {
            this.pluginInstance = info.getPluginInfo().createInstance();
        } catch (InstantiableException e) {
            throw new RuntimeException(e);
        }
        CLIJ2 clij2 = CLIJ2.getInstance();
        if (this.pluginInstance instanceof AbstractCLIJ2Plugin) {
            ((AbstractCLIJ2Plugin) this.pluginInstance).setCLIJ2(clij2);
        }
        this.pluginInstance.setClij(clij2.getCLIJ());

        super.run(progressInfo);
        this.pluginInstance = null;
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJCommandNodeInfo info = (CLIJCommandNodeInfo) getInfo();

        // Prepare inputs
        Object[] args = new Object[info.getNumArgs()];
        pluginInstance.setArgs(args);

        for (String key : info.getNodeParameters().getParameters().keySet()) {
            int argIndex = info.getParameterIdToArgIndexMap().get(key);
            args[argIndex] = clijParameters.getParameter(key).get(Object.class);
        }
        Map<String, ClearCLBuffer> outputs = new HashMap<>();


        try {
            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                int argIndex = info.getInputSlotToArgIndexMap().get(inputSlot.getName());
                if (!avoidGPUMemory) {
                    CLIJImageData imageData = dataBatch.getInputData(inputSlot, CLIJImageData.class, progressInfo);
                    if (info.getIoInputSlots().contains(inputSlot.getName())) {
                        imageData = (CLIJImageData) imageData.duplicate(progressInfo);
                        outputs.put(inputSlot.getName(), imageData.getImage());
                    }
                    args[argIndex] = imageData.getImage();
                } else {
                    ImagePlusData cpuImageData = dataBatch.getInputData(inputSlot, ImagePlusData.class, progressInfo);
                    CLIJ2 clij = CLIJ2.getInstance();
                    if (info.getIoInputSlots().contains(inputSlot.getName())) {
                        outputs.put(inputSlot.getName(), clij.push(cpuImageData.getImage()));
                    }
                    args[argIndex] = clij.push(cpuImageData.getImage());
                }
            }

            // Prepare outputs (dst buffer)
            ClearCLBuffer referenceImage = null;
            for (Object arg : args) {
                if (arg instanceof ClearCLBuffer) {
                    referenceImage = (ClearCLBuffer) arg;
                    break;
                }
            }

            for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                if (outputs.containsKey(outputSlot.getName()))
                    continue;
                if (outputSlot.getName().equals("Results table"))
                    continue;
                int argIndex = info.getOutputSlotToArgIndexMap().get(outputSlot.getName());
                ClearCLBuffer buffer = pluginInstance.createOutputBufferFromSource(referenceImage);
                args[argIndex] = buffer;
                outputs.put(outputSlot.getName(), buffer);
            }

            // Run algorithm
            if (pluginInstance instanceof CLIJOpenCLProcessor) {
                ((CLIJOpenCLProcessor) pluginInstance).executeCL();
            } else if (pluginInstance instanceof CLIJImageJProcessor) {
                ((CLIJImageJProcessor) pluginInstance).executeIJ();
            } else {
                throw new UnsupportedOperationException("Unable to run CLIJ plugin of type " + pluginInstance.getClass());
            }

            // Extract outputs
            for (JIPipeOutputDataSlot outputSlot : getOutputSlots()) {
                ClearCLBuffer buffer = outputs.get(outputSlot.getName());
                CLIJImageData imageData = new CLIJImageData(buffer);
                if (!avoidGPUMemory) {
                    dataBatch.addOutputData(outputSlot, imageData, progressInfo);
                } else {
                    dataBatch.addOutputData(outputSlot, imageData.pull(), progressInfo);
                }
            }

            // Extract outputs table
            if (!info.getOutputTableColumnInfos().isEmpty()) {
                ResultsTableData resultsTableData = new ResultsTableData();
                for (CLIJCommandNodeInfo.OutputTableColumnInfo columnInfo : info.getOutputTableColumnInfos()) {
                    resultsTableData.addColumn(columnInfo.getName(), columnInfo.isStringColumn());
                }
                resultsTableData.addRow();
                for (CLIJCommandNodeInfo.OutputTableColumnInfo columnInfo : info.getOutputTableColumnInfos()) {
                    Object value = args[columnInfo.getArgIndex()];
                    resultsTableData.setValueAt(value, 0, columnInfo.getName());
                }
                dataBatch.addOutputData("Results table", resultsTableData, progressInfo);
            }
        } finally {
            // Close inputs and outputs
            if (avoidGPUMemory) {
                for (Object arg : args) {
                    if (arg instanceof ClearCLBuffer) {
                        ((ClearCLBuffer) arg).close();
                    }
                }
                for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                    ClearCLBuffer buffer = outputs.get(outputSlot.getName());
                    if (buffer != null) {
                        buffer.close();
                    }
                }
            }
        }
    }
}
