package org.hkijena.jipipe.extensions.ilastik.nodes;

import ij.IJ;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.extensions.ilastik.IlastikSettings;
import org.hkijena.jipipe.extensions.ilastik.datatypes.IlastikModelData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.processes.OptionalProcessEnvironment;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Ilastik pixel classification", description = "Assigns labels to pixels based on pixel features and user annotations")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Ilastik")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = IlastikModelData.class, slotName = "Project", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Probabilities", description = "Multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Simple segmentation", description = "A single-channel image where the (integer) pixel values indicate the class to which a pixel belongs. " +
        "For this image, every pixel with the same value should belong to the same class of pixels")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Uncertainty", description = "Image where pixel intensity is proportional to the uncertainty found when trying to classify that pixel")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Features", description = "Multi-channel image where each channel represents one of the computed pixel features")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Labels", description = "Image representing the users’ manually created annotations")
public class IlastikPixelClassificationAlgorithm extends JIPipeSingleIterationAlgorithm {

    public static final JIPipeDataSlotInfo OUTPUT_SLOT_PROBABILITIES = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Probabilities",
            "Multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_SIMPLE_SEGMENTATION = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Simple segmentation",
            "A single-channel image where the (integer) pixel values indicate the class to which a pixel belongs. " +
                    "For this image, every pixel with the same value should belong to the same class of pixels");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_UNCERTAINTY = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Uncertainty",
            "Image where pixel intensity is proportional to the uncertainty found when trying to classify that pixel");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_FEATURES = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Features",
            "Multi-channel image where each channel represents one of the computed pixel features");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_LABELS = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Labels",
            "Image representing the users’ manually created annotations");

    private final OutputParameters outputParameters;
    private boolean cleanUpAfterwards = true;
    private OptionalProcessEnvironment overrideEnvironment = new OptionalProcessEnvironment();

    public IlastikPixelClassificationAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.outputParameters = new OutputParameters();
        registerSubParameter(outputParameters);
        updateSlots();
    }

    public IlastikPixelClassificationAlgorithm(IlastikPixelClassificationAlgorithm other) {
        super(other);
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.overrideEnvironment = new OptionalProcessEnvironment(other.overrideEnvironment);
        this.outputParameters = new OutputParameters(other.outputParameters);
        registerSubParameter(outputParameters);
        updateSlots();
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path workDirectory = getNewScratch();
        progressInfo.log("Work directory is " + workDirectory);

        // Collect the parameters
        List<String> exportSources = new ArrayList<>();
        if(outputParameters.outputFeatures)
            exportSources.add("Features");
        if(outputParameters.outputLabels)
            exportSources.add("Labels");
        if(outputParameters.outputProbabilities)
            exportSources.add("Probabilities");
        if(outputParameters.outputUncertainty)
            exportSources.add("Uncertainty");
        if(outputParameters.outputSimpleSegmentation)
            exportSources.add("Simple Segmentation");

        // Export the projects
        List<Path> exportedModelPaths = new ArrayList<>();
        List<IlastikModelData> modelDataList = dataBatch.getInputData("Project", IlastikModelData.class, progressInfo);
        for (int i = 0; i < modelDataList.size(); i++) {
            progressInfo.resolveAndLog("Exporting project", i, modelDataList.size());
            IlastikModelData project = modelDataList.get(i);
            Path exportedPath = workDirectory.resolve("project_" + i);
            exportedModelPaths.add(exportedPath);
            try {
                Files.write(exportedPath, project.getData(), StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Export images
        List<Path> exportedImagePaths = new ArrayList<>();
        List<ImagePlusData> imageDataList = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
        for (int i = 0; i < imageDataList.size(); i++) {
            progressInfo.resolveAndLog("Exporting input image", i, imageDataList.size());
            ImagePlusData imagePlusData = imageDataList.get(i);
            Path exportedPath = workDirectory.resolve("img_" + i);
            exportedModelPaths.add(exportedPath);
            IJ.saveAsTiff(imagePlusData.getImage(), exportedPath.toString());
            exportedImagePaths.add(exportedPath);
        }


        // Run analysis
        for (int i = 0; i < exportSources.size(); i++) {
            String exportSource = exportSources.get(i);
            JIPipeProgressInfo exportSourceProgress = progressInfo.resolveAndLog(exportSource, i, exportSources.size());
        }

        // Cleanup
        if (cleanUpAfterwards) {
            PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        if (!isPassThrough()) {
            if (overrideEnvironment.isEnabled()) {
                report.report(context, overrideEnvironment.getContent());
            } else {
                IlastikSettings.checkIlastikSettings(context, report);
            }
        }
    }

    @JIPipeDocumentation(name = "Generated outputs", description = "Select which outputs should be generated. " +
            "Please note that the number of outputs impacts the performance.")
    @JIPipeParameter("output-parameters")
    public OutputParameters getOutputParameters() {
        return outputParameters;
    }

    @JIPipeDocumentation(name = "Clean up data after processing", description = "If enabled, data is deleted from temporary directories after " +
            "the processing was finished. Disable this to make it possible to debug your scripts. The directories are accessible via the logs (Tools &gt; Logs).")
    @JIPipeParameter("cleanup-afterwards")
    public boolean isCleanUpAfterwards() {
        return cleanUpAfterwards;
    }

    @JIPipeParameter("cleanup-afterwards")
    public void setCleanUpAfterwards(boolean cleanUpAfterwards) {
        this.cleanUpAfterwards = cleanUpAfterwards;
    }

    @JIPipeDocumentation(name = "Override Ilastik environment", description = "If enabled, a different Ilastik environment is used for this node. Otherwise " +
            "the one in the Project > Application settings > Extensions > Ilastik is used.")
    @JIPipeParameter("override-environment")
    public OptionalProcessEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalProcessEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        super.onParameterChanged(event);
        if(event.getSource() == outputParameters) {
            updateSlots();
        }
    }

    private void updateSlots() {
        toggleSlot(OUTPUT_SLOT_FEATURES, outputParameters.outputFeatures);
        toggleSlot(OUTPUT_SLOT_PROBABILITIES, outputParameters.outputProbabilities);
        toggleSlot(OUTPUT_SLOT_UNCERTAINTY, outputParameters.outputUncertainty);
        toggleSlot(OUTPUT_SLOT_LABELS, outputParameters.outputLabels);
        toggleSlot(OUTPUT_SLOT_SIMPLE_SEGMENTATION, outputParameters.outputSimpleSegmentation);
    }

    public static class OutputParameters extends AbstractJIPipeParameterCollection {
        private boolean outputProbabilities = false;
        private boolean outputSimpleSegmentation = true;
        private boolean outputUncertainty = false;
        private boolean outputFeatures = false;
        private boolean outputLabels = false;

        public OutputParameters() {
        }

        public OutputParameters(OutputParameters other) {
            this.outputProbabilities = other.outputProbabilities;
            this.outputSimpleSegmentation = other.outputSimpleSegmentation;
            this.outputUncertainty = other.outputUncertainty;
            this.outputFeatures = other.outputFeatures;
            this.outputLabels = other.outputLabels;
        }

        @JIPipeDocumentation(name = "Output probabilities", description = "Exports a multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel")
        @JIPipeParameter("output-probabilities")
        public boolean isOutputProbabilities() {
            return outputProbabilities;
        }

        @JIPipeParameter("output-probabilities")
        public void setOutputProbabilities(boolean outputProbabilities) {
            this.outputProbabilities = outputProbabilities;
        }

        @JIPipeDocumentation(name = "Output simple segmentation", description = "Produces a single-channel image where the (integer) pixel values indicate the class to which a pixel belongs. " +
                "For this image, every pixel with the same value should belong to the same class of pixels")
        @JIPipeParameter("output-simple-segmentation")
        public boolean isOutputSimpleSegmentation() {
            return outputSimpleSegmentation;
        }

        @JIPipeParameter("output-simple-segmentation")
        public void setOutputSimpleSegmentation(boolean outputSimpleSegmentation) {
            this.outputSimpleSegmentation = outputSimpleSegmentation;
        }

        @JIPipeDocumentation(name = "Output uncertainty", description = "Produces an image where pixel intensity is proportional to the uncertainty found when trying to classify that pixel")
        @JIPipeParameter("output-uncertainty")
        public boolean isOutputUncertainty() {
            return outputUncertainty;
        }

        @JIPipeParameter("output-uncertainty")
        public void setOutputUncertainty(boolean outputUncertainty) {
            this.outputUncertainty = outputUncertainty;
        }

        @JIPipeDocumentation(name = "Output features", description = "Outputs a multi-channel image where each channel represents one of the computed pixel features")
        @JIPipeParameter("output-features")
        public boolean isOutputFeatures() {
            return outputFeatures;
        }

        @JIPipeParameter("output-features")
        public void setOutputFeatures(boolean outputFeatures) {
            this.outputFeatures = outputFeatures;
        }

        @JIPipeDocumentation(name = "Output labels", description = "Outputs an image representing the users’ manually created annotations")
        @JIPipeParameter("output-labels")
        public boolean isOutputLabels() {
            return outputLabels;
        }

        @JIPipeParameter("output-labels")
        public void setOutputLabels(boolean outputLabels) {
            this.outputLabels = outputLabels;
        }
    }
}
