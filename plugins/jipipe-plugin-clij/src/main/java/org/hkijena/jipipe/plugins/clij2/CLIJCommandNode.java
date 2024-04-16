/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.clij2;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.macro.CLIJImageJProcessor;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import net.haesleinhuepf.clij2.CLIJ2;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.BooleanParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.scijava.InstantiableException;

import java.util.HashMap;
import java.util.Map;

public class CLIJCommandNode extends JIPipeIteratingAlgorithm {

    private final JIPipeDynamicParameterCollection clijParameters;
    private CLIJMacroPlugin pluginInstance;
    private boolean avoidGPUMemory = true;
    private CLIJSplitMode splitMode = CLIJSplitMode.To3D;
    private boolean equalizeReplicateLast = false;

    public CLIJCommandNode(JIPipeNodeInfo info) {
        super(info);
        this.clijParameters = new JIPipeDynamicParameterCollection(((CLIJCommandNodeInfo) info).getNodeParameters());
        updateSlots();
    }

    public CLIJCommandNode(CLIJCommandNode other) {
        super(other);
        this.clijParameters = new JIPipeDynamicParameterCollection(other.clijParameters);
        this.avoidGPUMemory = other.avoidGPUMemory;
        this.splitMode = other.splitMode;
        this.equalizeReplicateLast = other.equalizeReplicateLast;
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

    @SetJIPipeDocumentation(name = "Avoid allocating GPU memory", description = "If enabled, the node will be reconfigured to only allocate data into the GPU memory if absolutely necessary. Please note that " +
            "the application of multiple GPU-based operations will be slower due to the repeated allocation and de-allocation of the images.")
    @JIPipeParameter("avoid-gpu-memory")
    public boolean isAvoidGPUMemory() {
        return avoidGPUMemory;
    }

    @JIPipeParameter("avoid-gpu-memory")
    public void setAvoidGPUMemory(boolean avoidGPUMemory) {
        this.avoidGPUMemory = avoidGPUMemory;
        updateSlots();
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Split image", description = "Allows to split the image into 2D slices or 3D cubes before passing them to CLIJ. " +
            "The results are automatically merged into an image with the same number of channels/frames/slices as the input image(s). " +
            "Please note that if multiple input images are provided, the node will equalize the number of channels/frames/slices prior to the algorithm execution. " +
            "If you split into 2D slices, channels/frames/slices will be equalized, while for 3D cubes channels and frames will be equalized. " +
            "The equalization process will add new black cubes/slices if necessary (based on the maximum over all inputs), unless you set the equalization mode to 'Replicate'. " +
            "Not applicable if 'Avoid allocating GPU memory' is disabled.")
    @JIPipeParameter(value = "split-mode", important = true)
    public CLIJSplitMode getSplitMode() {
        return splitMode;
    }

    @JIPipeParameter("split-mode")
    public void setSplitMode(CLIJSplitMode splitMode) {
        this.splitMode = splitMode;
    }

    @SetJIPipeDocumentation(name = "Equalization mode", description = "Determines how multiple input images are equalized. Only applicable if the split mode is not set to 'None' " +
            "and 'Avoid allocating GPU memory' is enabled. The option 'Empty' will add black slices/cubes if needed, while 'Replicate' will replicate the last cube/slice.")
    @JIPipeParameter("equalize-replicate-last")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Replicate", falseLabel = "Empty")
    public boolean isEqualizeReplicateLast() {
        return equalizeReplicateLast;
    }

    @JIPipeParameter("equalize-replicate-last")
    public void setEqualizeReplicateLast(boolean equalizeReplicateLast) {
        this.equalizeReplicateLast = equalizeReplicateLast;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if ("equalize-replicate-last".equals(access.getKey())) {
            return avoidGPUMemory && getInputSlots().stream().filter(slot -> slot.getAcceptedDataType().equals(ImagePlusData.class)).count() > 1;
        }
        if ("split-mode".equals(access.getKey())) {
            return avoidGPUMemory;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @SetJIPipeDocumentation(name = "CLIJ parameters", description = "Following parameters were extracted from the CLIJ2 operation:")
    @JIPipeParameter("clij-parameters")
    public JIPipeDynamicParameterCollection getClijParameters() {
        return clijParameters;
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
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

        super.run(runContext, progressInfo);
        this.pluginInstance = null;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        CLIJCommandNodeInfo info = (CLIJCommandNodeInfo) getInfo();

        // Prepare inputs
        Object[] args = new Object[info.getNumArgs()];
        pluginInstance.setArgs(args);

        for (String key : info.getNodeParameters().getParameters().keySet()) {
            int argIndex = info.getParameterIdToArgIndexMap().get(key);
            args[argIndex] = clijParameters.getParameter(key).get(Object.class);
        }

        if (avoidGPUMemory) {
            Map<String, ClearCLBuffer> outputs = new HashMap<>();

            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                int argIndex = info.getInputSlotToArgIndexMap().get(inputSlot.getName());

                // TODO: splitting
                ImagePlusData cpuImageData = iterationStep.getInputData(inputSlot, ImagePlusData.class, progressInfo);
                CLIJ2 clij = CLIJ2.getInstance();
                if (info.getIoInputSlots().contains(inputSlot.getName())) {
                    outputs.put(inputSlot.getName(), clij.push(cpuImageData.getImage()));
                }
                args[argIndex] = clij.push(cpuImageData.getImage());
            }

            // TODO: loop through splits

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
                    iterationStep.addOutputData(outputSlot, imageData, progressInfo);
                } else {
                    iterationStep.addOutputData(outputSlot, imageData.pull(), progressInfo);
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
                iterationStep.addOutputData("Results table", resultsTableData, progressInfo);
            }

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

        } else {
            // Pass through natively
            Map<String, ClearCLBuffer> outputs = new HashMap<>();
            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                int argIndex = info.getInputSlotToArgIndexMap().get(inputSlot.getName());

                CLIJImageData imageData = iterationStep.getInputData(inputSlot, CLIJImageData.class, progressInfo);
                if (info.getIoInputSlots().contains(inputSlot.getName())) {
                    imageData = (CLIJImageData) imageData.duplicate(progressInfo);
                    outputs.put(inputSlot.getName(), imageData.getImage());
                }
                args[argIndex] = imageData.getImage();
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
                    iterationStep.addOutputData(outputSlot, imageData, progressInfo);
                } else {
                    iterationStep.addOutputData(outputSlot, imageData.pull(), progressInfo);
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
                iterationStep.addOutputData("Results table", resultsTableData, progressInfo);
            }
        }


    }
}
