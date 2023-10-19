package org.hkijena.jipipe.extensions.ilastik.nodes;

import ij.ImagePlus;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.apache.commons.io.FilenameUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.ilastik.IlastikExtension;
import org.hkijena.jipipe.extensions.ilastik.IlastikSettings;
import org.hkijena.jipipe.extensions.ilastik.datatypes.IlastikModelData;
import org.hkijena.jipipe.extensions.ilastik.parameters.IlastikProjectValidationMode;
import org.hkijena.jipipe.extensions.ilastik.utils.IlastikUtils;
import org.hkijena.jipipe.extensions.ilastik.utils.hdf5.Hdf5;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.processes.OptionalProcessEnvironment;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static org.hkijena.jipipe.extensions.ilastik.utils.ImgUtils.*;

@JIPipeDocumentation(name = "Ilastik autocontext", description = "Assigns labels to pixels based on pixel features and user annotations. " +
        "Improves the Pixel Classification workflow by running it in multiple stages and showing each pixel the results of the previous stage. " +
        "Please note that results will be generated for each image and each project (pairwise).")
@JIPipeCitation("Autocontext documentation: https://www.ilastik.org/documentation/autocontext/autocontext")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Ilastik")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true, description = "The image(s) to classify.")
@JIPipeInputSlot(value = IlastikModelData.class, slotName = "Project", autoCreate = true, description = "The Ilastik project. Must support pixel classification.")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Probabilities Stage 1", description = "Multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel (Stage 1)")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Probabilities Stage 2", description = "Multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel (Stage 2)")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Probabilities All Stages", description = "Contains both \"Probabilities Stage 1\" and \"Probabilities Stage 2\" in a single multi-channel image")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Simple Segmentation Stage 1", description = "A single-channel image where the (integer) pixel values indicate the class to which a pixel belongs. " +
        "For this image, every pixel with the same value should belong to the same class of pixels")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Simple Segmentation Stage 2", description = "A single-channel image where the (integer) pixel values indicate the class to which a pixel belongs. " +
        "For this image, every pixel with the same value should belong to the same class of pixels")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Uncertainty Stage 1", description = "Image where pixel intensity is proportional to the uncertainty found when trying to classify that pixel")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Uncertainty Stage 2", description = "Image where pixel intensity is proportional to the uncertainty found when trying to classify that pixel")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Features Stage 1", description = "Multi-channel image where each channel represents one of the computed pixel features")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Features Stage 2", description = "Multi-channel image where each channel represents one of the computed pixel features")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Labels Stage 1", description = "Image representing the users’ manually created annotations")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Labels Stage 2", description = "Image representing the users’ manually created annotations")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Input Stage 1", description = "Raw input image that was fed into the first stage of the workflow")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Input Stage 2", description = "Input received by the second Pixel Classification stage in the workflow")
public class IlastikAutoContextAlgorithm extends JIPipeSingleIterationAlgorithm {

    public static final String PROJECT_TYPE = "PixelClassification01";

    public static final JIPipeDataSlotInfo OUTPUT_SLOT_PROBABILITIES_STAGE_1 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Probabilities Stage 1",
            "Multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_PROBABILITIES_STAGE_2 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Probabilities Stage 2",
            "Multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_PROBABILITIES_ALL_STAGES = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Probabilities All Stages",
            "Contains both \"Probabilities Stage 1\" and \"Probabilities Stage 2\" in a single multi-channel image");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_SIMPLE_SEGMENTATION_STAGE_1 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Simple Segmentation Stage 1",
            "A single-channel image where the (integer) pixel values indicate the class to which a pixel belongs. " +
                    "For this image, every pixel with the same value should belong to the same class of pixels");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_SIMPLE_SEGMENTATION_STAGE_2 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Simple Segmentation Stage 2",
            "A single-channel image where the (integer) pixel values indicate the class to which a pixel belongs. " +
                    "For this image, every pixel with the same value should belong to the same class of pixels");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_UNCERTAINTY_STAGE_1 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Uncertainty Stage 1",
            "Image where pixel intensity is proportional to the uncertainty found when trying to classify that pixel");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_UNCERTAINTY_STAGE_2 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Uncertainty Stage 2",
            "Image where pixel intensity is proportional to the uncertainty found when trying to classify that pixel");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_FEATURES_STAGE_1 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Features Stage 1",
            "Multi-channel image where each channel represents one of the computed pixel features");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_FEATURES_STAGE_2 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Features Stage 2",
            "Multi-channel image where each channel represents one of the computed pixel features");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_LABELS_STAGE_1 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Labels Stage 1",
            "Image representing the users’ manually created annotations");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_LABELS_STAGE_2 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Labels Stage 2",
            "Image representing the users’ manually created annotations");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_INPUT_STAGE_1 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Input Stage 1",
            "Image representing the users’ manually created annotations");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_INPUT_STAGE_2 = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Input Stage 2",
            "Image representing the users’ manually created annotations");

    private final OutputParameters outputParameters;
    private boolean cleanUpAfterwards = true;
    private OptionalProcessEnvironment overrideEnvironment = new OptionalProcessEnvironment();

    private IlastikProjectValidationMode projectValidationMode = IlastikProjectValidationMode.CrashOnError;

    public IlastikAutoContextAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.outputParameters = new OutputParameters();
        registerSubParameter(outputParameters);
        updateSlots();
    }

    public IlastikAutoContextAlgorithm(IlastikAutoContextAlgorithm other) {
        super(other);
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.projectValidationMode = other.projectValidationMode;
        this.overrideEnvironment = new OptionalProcessEnvironment(other.overrideEnvironment);
        this.outputParameters = new OutputParameters(other.outputParameters);
        registerSubParameter(outputParameters);
        updateSlots();
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Path workDirectory = getNewScratch();
        progressInfo.log("Work directory is " + workDirectory);

        JIPipeInputDataSlot projectInputSlot = getInputSlot("Project");
        JIPipeInputDataSlot imageInputSlot = getInputSlot("Image");

        // Collect the parameters
        List<String> exportSources = new ArrayList<>();
        if (outputParameters.outputFeaturesStage1)
            exportSources.add("Features Stage 1");
        if (outputParameters.outputFeaturesStage2)
            exportSources.add("Features Stage 2");
        if (outputParameters.outputLabelsStage1)
            exportSources.add("Labels Stage 1");
        if (outputParameters.outputLabelsStage2)
            exportSources.add("Labels Stage 2");
        if (outputParameters.outputProbabilitiesStage1)
            exportSources.add("Probabilities Stage 1");
        if (outputParameters.outputProbabilitiesStage2)
            exportSources.add("Probabilities Stage 2");
        if (outputParameters.outputProbabilitiesAllStages)
            exportSources.add("Probabilities All Stages");
        if (outputParameters.outputUncertaintyStage1)
            exportSources.add("Uncertainty Stage 1");
        if (outputParameters.outputUncertaintyStage2)
            exportSources.add("Uncertainty Stage 2");
        if (outputParameters.outputSimpleSegmentationStage1)
            exportSources.add("Simple Segmentation Stage 1");
        if (outputParameters.outputSimpleSegmentationStage2)
            exportSources.add("Simple Segmentation Stage 2");
        if (outputParameters.outputInputStage1)
            exportSources.add("Input Stage 1");
        if (outputParameters.outputInputStage2)
            exportSources.add("Input Stage 2");

        // Export the projects
        List<Path> exportedModelPaths = new ArrayList<>();
        List<JIPipeDataContext> exportedModelContexts = new ArrayList<>();
        for (int i = 0; i < projectInputSlot.getRowCount(); i++) {
            JIPipeProgressInfo exportProgress = progressInfo.resolveAndLog("Exporting project", i, projectInputSlot.getRowCount());
            IlastikModelData project = projectInputSlot.getData(i, IlastikModelData.class, exportProgress);

            Path exportedPath = workDirectory.resolve("project_" + i + ".ilp");
            try {
                Files.write(exportedPath, project.getData(), StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Check the project
            if (projectValidationMode != IlastikProjectValidationMode.Ignore) {
                exportProgress.log("Checking if " + exportedPath + " supports project type '" + PROJECT_TYPE + "'");
                if(!IlastikUtils.projectSupports(exportedPath, PROJECT_TYPE)) {
                    if(projectValidationMode == IlastikProjectValidationMode.CrashOnError) {
                        throw new JIPipeValidationRuntimeException(new GraphNodeValidationReportContext(this),
                                new IllegalArgumentException("Project does not support '" + PROJECT_TYPE + "'"),
                                "Provided project is not supported by '" + getInfo().getName() + "'!",
                                "The node tried to load project (row " + i + ") with annotations " + JsonUtils.toJsonString(projectInputSlot.getTextAnnotations(i)) + ", but the project does not support " + PROJECT_TYPE,
                                "Check if the inputs are correct or set the validation mode to 'Skip on error'.");
                    }
                }
            }

            exportedModelPaths.add(exportedPath);
            exportedModelContexts.add(projectInputSlot.getDataContext(i));
        }

        // Export images
        List<Path> exportedImagePaths = new ArrayList<>();
        List<JIPipeDataContext> exportedImageContexts = new ArrayList<>();
        for (int i = 0; i < imageInputSlot.getRowCount(); i++) {
            JIPipeProgressInfo exportProgress = progressInfo.resolveAndLog("Exporting input image", i, imageInputSlot.getRowCount());
            ImagePlusData imagePlusData = imageInputSlot.getData(i, ImagePlusData.class, exportProgress);

            DefaultDataset dataset = new DefaultDataset(JIPipe.getInstance().getContext(), new ImgPlus(ImageJFunctions.wrap(imagePlusData.getImage())));
            Path exportedPath = workDirectory.resolve("img_" + i + ".h5");
            Hdf5.writeDataset(exportedPath.toFile(), "data", (ImgPlus) dataset.getImgPlus(), 1, DEFAULT_AXES, value -> {
            });

            exportedImagePaths.add(exportedPath);
            exportedImageContexts.add(imageInputSlot.getDataContext(i));
        }


        // Run analysis
        for (int modelIndex = 0; modelIndex < exportedModelPaths.size(); modelIndex++) {
            Path modelPath = exportedModelPaths.get(modelIndex);
            Path modelResultPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "project_" + modelIndex);
            JIPipeProgressInfo modelProgress = progressInfo.resolveAndLog("Project", modelIndex, exportedModelPaths.size());

            for (int exportedSourceIndex = 0; exportedSourceIndex < exportSources.size(); exportedSourceIndex++) {
                String exportSource = exportSources.get(exportedSourceIndex);
                JIPipeProgressInfo exportSourceProgress = modelProgress.resolveAndLog(exportSource, exportedSourceIndex, exportSources.size());
                List<String> args = new ArrayList<>();
                String axes = reversed(DEFAULT_STRING_AXES);
                args.add("--headless");
                args.add("--readonly");
                args.add("--project=" + modelPath);
                args.add("--output_format=hdf5");
                args.add("--export_source=" + exportSource);
                args.add("--output_filename_format=" + modelResultPath + "/{result_type}__{nickname}.tiff");
                args.add("--output_axis_order=" + axes);
                args.add("--input_axes=" + axes);
                for (Path exportedImagePath : exportedImagePaths) {
                    args.add(exportedImagePath.toAbsolutePath().toString());
                }

                // Run ilastik
                IlastikExtension.runIlastik(overrideEnvironment.getContentOrDefault(IlastikSettings.getInstance().getEnvironment()),
                        args,
                        exportSourceProgress.resolve("Run Ilastik"),
                        false);

                // Extract results
                for (int imageIndex = 0; imageIndex < exportedImagePaths.size(); imageIndex++) {
                    Path inputImagePath = exportedImagePaths.get(imageIndex);
                    Path outputImagePath = modelResultPath.resolve(exportSource + "__" + FilenameUtils.removeExtension(inputImagePath.getFileName().toString()) + ".h5");
                    exportSourceProgress.log("Extracting result: " + outputImagePath);

                    ImgPlus dataset = Hdf5.readDataset(outputImagePath.toFile(), "exported_data");
                    ImagePlus imagePlus = ImageJFunctions.wrap(dataset, exportSource);
                    ImageJUtils.calibrate(imagePlus, ImageJCalibrationMode.MinMax, 0, 0);

                    List<JIPipeTextAnnotation> textAnnotations = new ArrayList<>(imageInputSlot.getTextAnnotations(imageIndex));
                    textAnnotations.addAll(projectInputSlot.getTextAnnotations(modelIndex));
                    List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>(imageInputSlot.getDataAnnotations(imageIndex));
                    dataAnnotations.addAll(projectInputSlot.getDataAnnotations(modelIndex));
                    JIPipeDataContext newContext = JIPipeDataContext.create(this, exportedImageContexts.get(imageIndex), exportedModelContexts.get(modelIndex));

                    getOutputSlot(exportSource).addData(new ImagePlusData(imagePlus),
                            textAnnotations,
                            JIPipeTextAnnotationMergeMode.Merge,
                            dataAnnotations,
                            JIPipeDataAnnotationMergeMode.Merge,
                            newContext,
                            exportSourceProgress);
                }
            }
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

    @JIPipeDocumentation(name = "Validate Ilastik project", description = "Determines how/if the node validates the input projects. This is done to check if the project is supported by this node.")
    @JIPipeParameter("project-validation-mode")
    public IlastikProjectValidationMode getProjectValidationMode() {
        return projectValidationMode;
    }

    @JIPipeParameter("project-validation-mode")
    public void setProjectValidationMode(IlastikProjectValidationMode projectValidationMode) {
        this.projectValidationMode = projectValidationMode;
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
        if (event.getSource() == outputParameters) {
            updateSlots();
        }
    }

    private void updateSlots() {
        toggleSlot(OUTPUT_SLOT_FEATURES_STAGE_1, outputParameters.outputFeaturesStage1);
        toggleSlot(OUTPUT_SLOT_FEATURES_STAGE_2, outputParameters.outputFeaturesStage2);
        toggleSlot(OUTPUT_SLOT_PROBABILITIES_STAGE_1, outputParameters.outputProbabilitiesStage1);
        toggleSlot(OUTPUT_SLOT_PROBABILITIES_STAGE_2, outputParameters.outputProbabilitiesStage2);
        toggleSlot(OUTPUT_SLOT_PROBABILITIES_ALL_STAGES, outputParameters.outputProbabilitiesAllStages);
        toggleSlot(OUTPUT_SLOT_UNCERTAINTY_STAGE_1, outputParameters.outputUncertaintyStage1);
        toggleSlot(OUTPUT_SLOT_UNCERTAINTY_STAGE_2, outputParameters.outputUncertaintyStage2);
        toggleSlot(OUTPUT_SLOT_LABELS_STAGE_1, outputParameters.outputLabelsStage1);
        toggleSlot(OUTPUT_SLOT_LABELS_STAGE_2, outputParameters.outputLabelsStage2);
        toggleSlot(OUTPUT_SLOT_SIMPLE_SEGMENTATION_STAGE_1, outputParameters.outputSimpleSegmentationStage1);
        toggleSlot(OUTPUT_SLOT_SIMPLE_SEGMENTATION_STAGE_2, outputParameters.outputSimpleSegmentationStage2);
        toggleSlot(OUTPUT_SLOT_INPUT_STAGE_1, outputParameters.outputInputStage1);
        toggleSlot(OUTPUT_SLOT_INPUT_STAGE_2, outputParameters.outputInputStage2);
    }

    public static class OutputParameters extends AbstractJIPipeParameterCollection {
        private boolean outputProbabilitiesStage1 = false;
        private boolean outputProbabilitiesStage2 = false;
        private boolean outputProbabilitiesAllStages = false;
        private boolean outputSimpleSegmentationStage1 = true;
        private boolean outputSimpleSegmentationStage2 = true;
        private boolean outputUncertaintyStage1 = false;
        private boolean outputUncertaintyStage2 = false;
        private boolean outputFeaturesStage1 = false;
        private boolean outputFeaturesStage2 = false;
        private boolean outputLabelsStage1 = false;
        private boolean outputLabelsStage2 = false;
        private boolean outputInputStage1 = false;
        private boolean outputInputStage2 = false;

        public OutputParameters() {
        }

        public OutputParameters(OutputParameters other) {
            this.outputProbabilitiesStage1 = other.outputProbabilitiesStage1;
            this.outputProbabilitiesStage2 = other.outputProbabilitiesStage2;
            this.outputProbabilitiesAllStages = other.outputProbabilitiesAllStages;
            this.outputSimpleSegmentationStage1 = other.outputSimpleSegmentationStage1;
            this.outputSimpleSegmentationStage2 = other.outputSimpleSegmentationStage2;
            this.outputUncertaintyStage1 = other.outputUncertaintyStage1;
            this.outputUncertaintyStage2 = other.outputUncertaintyStage2;
            this.outputFeaturesStage1 = other.outputFeaturesStage1;
            this.outputFeaturesStage2 = other.outputFeaturesStage2;
            this.outputLabelsStage1 = other.outputLabelsStage1;
            this.outputLabelsStage2 = other.outputLabelsStage2;
            this.outputInputStage1 = other.outputInputStage1;
            this.outputInputStage2 = other.outputInputStage2;
        }

        @JIPipeDocumentation(name = "Output probabilities (Stage 1)", description = "Exports a multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel")
        @JIPipeParameter("output-probabilities-stage-1")
        public boolean isOutputProbabilitiesStage1() {
            return outputProbabilitiesStage1;
        }

        @JIPipeParameter("output-probabilities-stage-1")
        public void setOutputProbabilitiesStage1(boolean outputProbabilitiesStage1) {
            this.outputProbabilitiesStage1 = outputProbabilitiesStage1;
        }

        @JIPipeDocumentation(name = "Output probabilities (Stage 2)", description = "Exports a multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel")
        @JIPipeParameter("output-probabilities-stage-2")
        public boolean isOutputProbabilitiesStage2() {
            return outputProbabilitiesStage2;
        }

        @JIPipeParameter("output-probabilities-stage-2")
        public void setOutputProbabilitiesStage2(boolean outputProbabilitiesStage2) {
            this.outputProbabilitiesStage2 = outputProbabilitiesStage2;
        }

        @JIPipeDocumentation(name = "Output probabilities (All stages)", description = "Exports a multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel")
        @JIPipeParameter("output-probabilities-all-stages")
        public boolean isOutputProbabilitiesAllStages() {
            return outputProbabilitiesAllStages;
        }

        @JIPipeParameter("output-probabilities-all-stages")
        public void setOutputProbabilitiesAllStages(boolean outputProbabilitiesAllStages) {
            this.outputProbabilitiesAllStages = outputProbabilitiesAllStages;
        }

        @JIPipeDocumentation(name = "Output simple segmentation (Stage 1)", description = "Produces a single-channel image where the (integer) pixel values indicate the class to which a pixel belongs. " +
                "For this image, every pixel with the same value should belong to the same class of pixels")
        @JIPipeParameter("output-simple-segmentation-stage-1")
        public boolean isOutputSimpleSegmentationStage1() {
            return outputSimpleSegmentationStage1;
        }

        @JIPipeParameter("output-simple-segmentation-stage-1")
        public void setOutputSimpleSegmentationStage1(boolean outputSimpleSegmentationStage1) {
            this.outputSimpleSegmentationStage1 = outputSimpleSegmentationStage1;
        }

        @JIPipeDocumentation(name = "Output simple segmentation (Stage 2)", description = "Produces a single-channel image where the (integer) pixel values indicate the class to which a pixel belongs. " +
                "For this image, every pixel with the same value should belong to the same class of pixels")
        @JIPipeParameter("output-simple-segmentation-stage-2")
        public boolean isOutputSimpleSegmentationStage2() {
            return outputSimpleSegmentationStage2;
        }

        @JIPipeParameter("output-simple-segmentation-stage-2")
        public void setOutputSimpleSegmentationStage2(boolean outputSimpleSegmentationStage2) {
            this.outputSimpleSegmentationStage2 = outputSimpleSegmentationStage2;
        }

        @JIPipeDocumentation(name = "Output uncertainty (Stage 1)", description = "Produces an image where pixel intensity is proportional to the uncertainty found when trying to classify that pixel")
        @JIPipeParameter("output-uncertainty-stage-1")
        public boolean isOutputUncertaintyStage1() {
            return outputUncertaintyStage1;
        }

        @JIPipeParameter("output-uncertainty-stage-1")
        public void setOutputUncertaintyStage1(boolean outputUncertaintyStage1) {
            this.outputUncertaintyStage1 = outputUncertaintyStage1;
        }

        @JIPipeDocumentation(name = "Output uncertainty (Stage 2)", description = "Produces an image where pixel intensity is proportional to the uncertainty found when trying to classify that pixel")
        @JIPipeParameter("output-uncertainty-stage-2")
        public boolean isOutputUncertaintyStage2() {
            return outputUncertaintyStage2;
        }

        @JIPipeParameter("output-uncertainty-stage-2")
        public void setOutputUncertaintyStage2(boolean outputUncertaintyStage2) {
            this.outputUncertaintyStage2 = outputUncertaintyStage2;
        }

        @JIPipeDocumentation(name = "Output features (Stage 1)", description = "Outputs a multi-channel image where each channel represents one of the computed pixel features")
        @JIPipeParameter("output-features-stage-1")
        public boolean isOutputFeaturesStage1() {
            return outputFeaturesStage1;
        }

        @JIPipeParameter("output-features-stage-1")
        public void setOutputFeaturesStage1(boolean outputFeaturesStage1) {
            this.outputFeaturesStage1 = outputFeaturesStage1;
        }

        @JIPipeDocumentation(name = "Output features (Stage 2)", description = "Outputs a multi-channel image where each channel represents one of the computed pixel features")
        @JIPipeParameter("output-features-stage-2")
        public boolean isOutputFeaturesStage2() {
            return outputFeaturesStage2;
        }

        @JIPipeParameter("output-features-stage-2")
        public void setOutputFeaturesStage2(boolean outputFeaturesStage2) {
            this.outputFeaturesStage2 = outputFeaturesStage2;
        }

        @JIPipeDocumentation(name = "Output labels (Stage 1)", description = "Outputs an image representing the users’ manually created annotations")
        @JIPipeParameter("output-labels-stage-1")
        public boolean isOutputLabelsStage1() {
            return outputLabelsStage1;
        }

        @JIPipeParameter("output-labels-stage-1")
        public void setOutputLabelsStage1(boolean outputLabelsStage1) {
            this.outputLabelsStage1 = outputLabelsStage1;
        }

        @JIPipeDocumentation(name = "Output labels (Stage 2)", description = "Outputs an image representing the users’ manually created annotations")
        @JIPipeParameter("output-labels-stage-2")
        public boolean isOutputLabelsStage2() {
            return outputLabelsStage2;
        }

        @JIPipeParameter("output-labels-stage-2")
        public void setOutputLabelsStage2(boolean outputLabelsStage2) {
            this.outputLabelsStage2 = outputLabelsStage2;
        }

        @JIPipeDocumentation(name = "Output input (Stage 1)", description = "Outputs the raw input image that was fed into the first stage of the workflow")
        @JIPipeParameter("output-labels-stage-1")
        public boolean isOutputInputStage1() {
            return outputInputStage1;
        }

        @JIPipeParameter("output-labels-stage-1")
        public void setOutputInputStage1(boolean outputInputStage1) {
            this.outputInputStage1 = outputInputStage1;
        }

        @JIPipeDocumentation(name = "Output input (Stage 2)", description = "Output the input received by the second Pixel Classification stage in the workflow")
        @JIPipeParameter("output-labels-stage-2")
        public boolean isOutputInputStage2() {
            return outputInputStage2;
        }

        @JIPipeParameter("output-labels-stage-2")
        public void setOutputInputStage2(boolean outputInputStage2) {
            this.outputInputStage2 = outputInputStage2;
        }
    }
}
