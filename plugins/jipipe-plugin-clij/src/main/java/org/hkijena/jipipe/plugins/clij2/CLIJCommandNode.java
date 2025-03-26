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

import ij.ImagePlus;
import ij.process.ImageProcessor;
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
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.BooleanParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.scijava.InstantiableException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CLIJCommandNode extends JIPipeIteratingAlgorithm {

    public static final String RESULTS_TABLE_SLOT_NAME = "Results table";
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

        // Handle the case where there are no image inputs
        boolean hasImageInputs = getInputSlots().stream().anyMatch(slot -> slot.getAcceptedDataType() == ImagePlusData.class);

        if (avoidGPUMemory && hasImageInputs) {

            Map<String, ImagePlus> inputImages = new HashMap<>();
            int numC = 0;
            int numZ = 0;
            int numT = 0;
            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                ImagePlus imp = iterationStep.getInputData(inputSlot, ImagePlusData.class, progressInfo).getImage();
                inputImages.put(inputSlot.getName(), imp);
                numC = Math.max(numC, imp.getNChannels());
                numZ = Math.max(numZ, imp.getNSlices());
                numT = Math.max(numT, imp.getNFrames());
            }

            Map<String, Map<ImageSliceIndex, ImagePlus>> splitInputImages = new HashMap<>();
            if (splitMode == CLIJSplitMode.None) {
                for (Map.Entry<String, ImagePlus> entry : inputImages.entrySet()) {
                    Map<ImageSliceIndex, ImagePlus> splitMap = new HashMap<>();
                    splitMap.put(new ImageSliceIndex(0, 0, 0), entry.getValue());
                    splitInputImages.put(entry.getKey(), splitMap);
                }
            } else if (splitMode == CLIJSplitMode.To3D) {
                for (Map.Entry<String, ImagePlus> entry : inputImages.entrySet()) {
                    Map<ImageSliceIndex, ImagePlus> splitMap = splitInputImages.getOrDefault(entry.getKey(), null);
                    if (splitMap == null) {
                        splitMap = new HashMap<>();
                        splitInputImages.put(entry.getKey(), splitMap);
                    }
                    Map<ImageSliceIndex, ImagePlus> finalSplitMap = splitMap;
                    ImagePlus expandedImage = ImageJUtils.ensureSize(entry.getValue(), numC, numZ, numT, equalizeReplicateLast);
                    ImageJIterationUtils.forEachIndexedCTStack(expandedImage, (imp, index, impProgress) -> {
                        finalSplitMap.put(new ImageSliceIndex(index.getC(), 0, index.getT()), imp);
                    }, progressInfo.resolve("Split into 3D"));
                }
            } else if (splitMode == CLIJSplitMode.To2D) {
                for (Map.Entry<String, ImagePlus> entry : inputImages.entrySet()) {
                    Map<ImageSliceIndex, ImagePlus> splitMap = splitInputImages.getOrDefault(entry.getKey(), null);
                    if (splitMap == null) {
                        splitMap = new HashMap<>();
                        splitInputImages.put(entry.getKey(), splitMap);
                    }
                    Map<ImageSliceIndex, ImagePlus> finalSplitMap = splitMap;
                    ImagePlus expandedImage = ImageJUtils.ensureSize(entry.getValue(), numC, numZ, numT, equalizeReplicateLast);
                    ImageJIterationUtils.forEachIndexedZCTSlice(expandedImage, (ip, index) -> {
                        finalSplitMap.put(index, new ImagePlus("slice", ip));
                    }, progressInfo.resolve("Split into 2D"));
                }
            } else {
                throw new UnsupportedOperationException();
            }

            Map<String, Map<ImageSliceIndex, ImageProcessor>> outputImages = new HashMap<>();
            ResultsTableData outputTable = new ResultsTableData();

            // Prepare output table
            for (CLIJCommandNodeInfo.OutputTableColumnInfo columnInfo : info.getOutputTableColumnInfos()) {
                outputTable.addColumn(columnInfo.getName(), columnInfo.isStringColumn());
            }

            List<ImageSliceIndex> splitSliceIndices = new ArrayList<>(splitInputImages.get(splitInputImages.keySet().iterator().next()).keySet());
            for (int j = 0; j < splitSliceIndices.size(); j++) {
                ImageSliceIndex splitSliceIndex = splitSliceIndices.get(j);
                JIPipeProgressInfo splitProgress = progressInfo.resolveAndLog(splitSliceIndex.toString(), j, splitSliceIndices.size());
                try {
                    Map<String, ClearCLBuffer> outputs = new HashMap<>();

                    for (JIPipeDataSlot inputSlot : getInputSlots()) {
                        int argIndex = info.getInputSlotToArgIndexMap().get(inputSlot.getName());

                        ImagePlus splitImage = splitInputImages.get(inputSlot.getName()).get(splitSliceIndex);
                        CLIJ2 clij = CLIJ2.getInstance();
                        if (info.getIoInputSlots().contains(inputSlot.getName())) {
                            outputs.put(inputSlot.getName(), clij.push(splitImage));
                        }
                        args[argIndex] = clij.push(splitImage);
                    }

                    // Prepare outputs (dst buffer)
                    ClearCLBuffer referenceBuffer = null;
                    for (Object arg : args) {
                        if (arg instanceof ClearCLBuffer) {
                            referenceBuffer = (ClearCLBuffer) arg;
                            break;
                        }
                    }

                    for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                        if (outputs.containsKey(outputSlot.getName()))
                            continue;
                        if (outputSlot.getName().equals(RESULTS_TABLE_SLOT_NAME))
                            continue;
                        int argIndex = info.getOutputSlotToArgIndexMap().get(outputSlot.getName());
                        ClearCLBuffer buffer = pluginInstance.createOutputBufferFromSource(referenceBuffer);
                        args[argIndex] = buffer;
                        outputs.put(outputSlot.getName(), buffer);
                    }

                    // Run algorithm
                    splitProgress.log("Running " + pluginInstance);
                    if (pluginInstance instanceof CLIJOpenCLProcessor) {
                        ((CLIJOpenCLProcessor) pluginInstance).executeCL();
                    } else if (pluginInstance instanceof CLIJImageJProcessor) {
                        ((CLIJImageJProcessor) pluginInstance).executeIJ();
                    } else {
                        throw new UnsupportedOperationException("Unable to run CLIJ plugin of type " + pluginInstance.getClass());
                    }
                    splitProgress.log("Extracting outputs");

                    // Extract image outputs and store them into the global storage
                    for (JIPipeOutputDataSlot outputSlot : getOutputSlots()) {
                        ClearCLBuffer buffer = outputs.get(outputSlot.getName());
                        CLIJImageData imageData = new CLIJImageData(buffer);
                        ImagePlus img = imageData.pull().getImage();
                        Map<ImageSliceIndex, ImageProcessor> outputSlices = outputImages.getOrDefault(outputSlot.getName(), null);
                        if (outputSlices == null) {
                            outputSlices = new HashMap<>();
                            outputImages.put(outputSlot.getName(), outputSlices);
                        }
                        Map<ImageSliceIndex, ImageProcessor> finalOutputSlices = outputSlices;
                        ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                            ImageSliceIndex globalSliceIndex = new ImageSliceIndex(splitSliceIndex.getC() + index.getC(),
                                    splitSliceIndex.getZ() + index.getZ(),
                                    splitSliceIndex.getT() + index.getT());
                            finalOutputSlices.put(globalSliceIndex, ip);
                        }, splitProgress);
                    }

                    // Extract results table
                    if (!info.getOutputTableColumnInfos().isEmpty()) {
                        outputTable.addRow();
                        for (CLIJCommandNodeInfo.OutputTableColumnInfo columnInfo : info.getOutputTableColumnInfos()) {
                            Object value = args[columnInfo.getArgIndex()];
                            outputTable.setValueAt(value, 0, columnInfo.getName());
                        }
                    }

                } finally {

                    // Cleanup all images
                    for (int i = 0; i < args.length; i++) {
                        Object arg = args[i];
                        if (arg instanceof ClearCLBuffer) {
                            ((ClearCLBuffer) arg).close();
                            args[i] = null;
                        }
                    }
                }
            }

            progressInfo.log("Writing merged outputs");

            // Output images
            ImagePlus referenceInputImage = null;
            for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
                if (inputSlot.getAcceptedDataType() == ImagePlusData.class) {
                    referenceInputImage = iterationStep.getInputData(inputSlot, ImagePlusData.class, progressInfo).getImage();
                    break;
                }
            }
            for (Map.Entry<String, Map<ImageSliceIndex, ImageProcessor>> entry : outputImages.entrySet()) {
                ImagePlus merged = ImageJUtils.mergeMappedSlices(entry.getValue());
                if (referenceInputImage != null) {
                    merged.copyScale(referenceInputImage);
                }
                iterationStep.addOutputData(entry.getKey(), new ImagePlusData(merged), progressInfo);
            }

            // Write output table into slot
            if (!info.getOutputTableColumnInfos().isEmpty()) {
                iterationStep.addOutputData(RESULTS_TABLE_SLOT_NAME, outputTable, progressInfo);
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
                if (outputSlot.getName().equals(RESULTS_TABLE_SLOT_NAME))
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
                iterationStep.addOutputData(RESULTS_TABLE_SLOT_NAME, resultsTableData, progressInfo);
            }
        }


    }
}
