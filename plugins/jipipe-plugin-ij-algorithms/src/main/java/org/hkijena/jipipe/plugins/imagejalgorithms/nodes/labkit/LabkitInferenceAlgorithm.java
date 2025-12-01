package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.labkit;

import ij.ImagePlus;
import net.imglib2.img.VirtualStackAdapter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.JIPipeBDVProgressWriter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.strings.JsonData;
import sc.fiji.labkit.ui.segmentation.SegmentationTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Labkit inference", description = "Uses Labkit to segment or calculate the probability map of the input images")
@AddJIPipeNodeAlias(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Plugins\nLabkit\nMacro Recordable\nSegment Image With Labkit")
@AddJIPipeNodeAlias(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Plugins\nLabkit\nMacro Recordable\nCalculate Probability Map With Labkit")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Labkit")
@AddJIPipeInputSlot(name = "Raw", value = ImagePlusData.class, description = "The input image", create = true)
@AddJIPipeInputSlot(name = "Model", value = JsonData.class, description = "The Labkit model", create = true)
@AddJIPipeOutputSlot(name = "Labels", value = ImagePlusData.class, description = "The output labels")
@AddJIPipeOutputSlot(name = "Probabilities", value = ImagePlusData.class, description = "The output probabilities")
public class LabkitInferenceAlgorithm extends JIPipeIteratingAlgorithm {

    public static final JIPipeDataSlotInfo OUTPUT_SLOT_LABELS = new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Output, "Labels", "The output labels");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_PROBABILITIES = new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Output, "Probabilities", "The output probabilities");

    private final OutputSettings outputSettings;
    private boolean useGPU = false;

    public LabkitInferenceAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.outputSettings = new OutputSettings();
        registerSubParameter(outputSettings);
        updateSlots();
    }

    public LabkitInferenceAlgorithm(LabkitInferenceAlgorithm other) {
        super(other);
        this.useGPU = other.useGPU;
        this.outputSettings = new OutputSettings(other.outputSettings);
        registerSubParameter(outputSettings);
        updateSlots();
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = ImageJUtils.unwrap(iterationStep.getInputData("Raw", ImagePlusData.class, progressInfo));
        JsonData model = iterationStep.getInputData("Model", JsonData.class, progressInfo);

        // Write the model into a temporary file
        Path tmpPath;
        try {
            tmpPath = Files.createTempFile("jipipe-", ".classifier");
            Files.writeString(tmpPath, model.getData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        progressInfo.log("Parsing Labkit model ...");

        SegmentationTool segmenter = new SegmentationTool();
        segmenter.setContext(JIPipe.getInstance().getContext());
        segmenter.setUseGpu(useGPU);
        segmenter.setProgressWriter(new JIPipeBDVProgressWriter(progressInfo));
        segmenter.openModel(tmpPath.toString());


        if (outputSettings.outputProbabilities) {
            progressInfo.log("Calculating probabilities ...");
            ImagePlus output = ImageJFunctions.wrap(segmenter.probabilityMap(VirtualStackAdapter.wrap(img)), "");
            iterationStep.addOutputData("Probabilities", new ImagePlusData(output), progressInfo);
        }

        if (outputSettings.outputLabels) {
            progressInfo.log("Calculating labels ...");
            ImagePlus output = ImageJFunctions.wrap(segmenter.segment(VirtualStackAdapter.wrap(img)), "");
            iterationStep.addOutputData("Labels", new ImagePlusData(output), progressInfo);
        }

        try {
            Files.deleteIfExists(tmpPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Outputs", description = "Controls the outputs of this node")
    @JIPipeParameter("output-settings")
    public OutputSettings getOutputSettings() {
        return outputSettings;
    }

    @SetJIPipeDocumentation(name = "Use GPU", description = "If enabled, use GPU acceleration for the processing")
    @JIPipeParameter("use-gpu")
    public boolean isUseGPU() {
        return useGPU;
    }

    @JIPipeParameter("use-gpu")
    public void setUseGPU(boolean useGPU) {
        this.useGPU = useGPU;
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        super.onParameterChanged(event);

        if (event.getSource() == outputSettings) {
            updateSlots();
        }
    }

    private void updateSlots() {
        toggleSlot(OUTPUT_SLOT_LABELS, outputSettings.outputLabels);
        toggleSlot(OUTPUT_SLOT_PROBABILITIES, outputSettings.outputProbabilities);
    }

    public static class OutputSettings extends AbstractJIPipeParameterCollection {
        private boolean outputLabels = true;
        private boolean outputProbabilities = true;

        public OutputSettings() {
        }

        public OutputSettings(OutputSettings other) {
            this.outputLabels = other.outputLabels;
            this.outputProbabilities = other.outputProbabilities;
        }

        @SetJIPipeDocumentation(name = "Labels", description = "If enabled, segmented labels are generated")
        @JIPipeParameter("output-labels")
        public boolean isOutputLabels() {
            return outputLabels;
        }

        @JIPipeParameter("output-labels")
        public void setOutputLabels(boolean outputLabels) {
            this.outputLabels = outputLabels;
        }

        @SetJIPipeDocumentation(name = "Probabilities", description = "If enabled, probability maps are generated")
        @JIPipeParameter("output-probabilities")
        public boolean isOutputProbabilities() {
            return outputProbabilities;
        }

        @JIPipeParameter("output-probabilities")
        public void setOutputProbabilities(boolean outputProbabilities) {
            this.outputProbabilities = outputProbabilities;
        }
    }
}
