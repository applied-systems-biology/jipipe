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

package org.hkijena.jipipe.plugins.ilastik.nodes;

import ij.ImagePlus;
import org.apache.commons.io.FilenameUtils;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.ilastik.IlastikPlugin;
import org.hkijena.jipipe.plugins.ilastik.datatypes.IlastikModelData;
import org.hkijena.jipipe.plugins.ilastik.environments.IlastikEnvironment;
import org.hkijena.jipipe.plugins.ilastik.environments.IlastikEnvironmentAccessNode;
import org.hkijena.jipipe.plugins.ilastik.environments.OptionalIlastikEnvironment;
import org.hkijena.jipipe.plugins.ilastik.parameters.IlastikProjectValidationMode;
import org.hkijena.jipipe.plugins.ilastik.utils.IlastikUtils;
import org.hkijena.jipipe.plugins.ilastik.utils.hdf5.IJ1Hdf5;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.hkijena.jipipe.plugins.ilastik.utils.ImgUtils.*;

@SetJIPipeDocumentation(name = "Ilastik pixel classification", description = "Assigns labels to pixels based on pixel features and user annotations. " +
        "Please note that results will be generated for each image and each project (pairwise).")
@AddJIPipeCitation("Pixel classification documentation: https://www.ilastik.org/documentation/pixelclassification/pixelclassification")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Ilastik")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image", create = true, description = "The image(s) to classify.")
@AddJIPipeInputSlot(value = IlastikModelData.class, name = "Project", create = true, description = "The Ilastik project. Must support pixel classification.", role = JIPipeDataSlotRole.ParametersLooping)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Probabilities", description = "Multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Simple Segmentation", description = "A single-channel image where the (integer) pixel values indicate the class to which a pixel belongs. " +
        "For this image, every pixel with the same value should belong to the same class of pixels")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Uncertainty", description = "Image where pixel intensity is proportional to the uncertainty found when trying to classify that pixel")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Features", description = "Multi-channel image where each channel represents one of the computed pixel features")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Labels", description = "Image representing the users’ manually created annotations")
public class IlastikPixelClassificationAlgorithm extends JIPipeSingleIterationAlgorithm implements IlastikEnvironmentAccessNode {

    public static final String PROJECT_TYPE = "PixelClassification";

    public static final JIPipeDataSlotInfo OUTPUT_SLOT_PROBABILITIES = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Probabilities",
            "Multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_SIMPLE_SEGMENTATION = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Simple Segmentation",
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
    private OptionalIlastikEnvironment overrideEnvironment = new OptionalIlastikEnvironment();

    private IlastikProjectValidationMode projectValidationMode = IlastikProjectValidationMode.CrashOnError;

    public IlastikPixelClassificationAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.outputParameters = new OutputParameters();
        registerSubParameter(outputParameters);
        updateSlots();
    }

    public IlastikPixelClassificationAlgorithm(IlastikPixelClassificationAlgorithm other) {
        super(other);
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.projectValidationMode = other.projectValidationMode;
        this.overrideEnvironment = new OptionalIlastikEnvironment(other.overrideEnvironment);
        this.outputParameters = new OutputParameters(other.outputParameters);
        registerSubParameter(outputParameters);
        updateSlots();
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path workDirectory = getNewScratch();
        progressInfo.log("Work directory is " + workDirectory);

        JIPipeInputDataSlot projectInputSlot = getInputSlot("Project");
        JIPipeInputDataSlot imageInputSlot = getInputSlot("Image");

        // Collect the parameters
        List<String> exportSources = new ArrayList<>();
        if (outputParameters.outputFeatures)
            exportSources.add("Features");
        if (outputParameters.outputLabels)
            exportSources.add("Labels");
        if (outputParameters.outputProbabilities)
            exportSources.add("Probabilities");
        if (outputParameters.outputUncertainty)
            exportSources.add("Uncertainty");
        if (outputParameters.outputSimpleSegmentation)
            exportSources.add("Simple Segmentation");

        // Export the projects
        List<Path> exportedModelPaths = new ArrayList<>();
        List<JIPipeDataContext> exportedModelContexts = new ArrayList<>();
        for (int i = 0; i < projectInputSlot.getRowCount(); i++) {
            JIPipeProgressInfo exportProgress = progressInfo.resolveAndLog("Exporting project", i, projectInputSlot.getRowCount());
            IlastikModelData project = projectInputSlot.getData(i, IlastikModelData.class, exportProgress);
            Path exportedPath = project.exportOrGetLink(workDirectory.resolve("project_" + i + ".ilp"));

            // Check the project
            if (projectValidationMode != IlastikProjectValidationMode.Ignore) {
                exportProgress.log("Checking if " + exportedPath + " supports project type '" + PROJECT_TYPE + "'");
                if (!IlastikUtils.projectSupports(exportedPath, PROJECT_TYPE)) {
                    if (projectValidationMode == IlastikProjectValidationMode.CrashOnError) {
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

            Path exportedPath = workDirectory.resolve("img_" + i + ".h5");
            IJ1Hdf5.writeImage(imagePlusData.getImage(), exportedPath, "data", DEFAULT_AXES, progressInfo);

            exportedImagePaths.add(exportedPath);
            exportedImageContexts.add(imageInputSlot.getDataContext(i));
        }


        // Run analysis
        for (int modelIndex = 0; modelIndex < exportedModelPaths.size(); modelIndex++) {
            Path modelPath = exportedModelPaths.get(modelIndex);
            Path modelResultPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "project_" + modelIndex);
            JIPipeProgressInfo modelProgress = progressInfo.resolveAndLog("Project", modelIndex, exportedModelPaths.size());

            for (int exportSourceIndex = 0; exportSourceIndex < exportSources.size(); exportSourceIndex++) {
                String exportSource = exportSources.get(exportSourceIndex);
                JIPipeProgressInfo exportSourceProgress = modelProgress.resolveAndLog(exportSource, exportSourceIndex, exportSources.size());
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
                IlastikEnvironment environment = getConfiguredIlastikEnvironment();
                IlastikPlugin.runIlastik(environment,
                        args,
                        false, exportSourceProgress.resolve("Run Ilastik")
                );

                // Extract results
                for (int imageIndex = 0; imageIndex < exportedImagePaths.size(); imageIndex++) {
                    Path inputImagePath = exportedImagePaths.get(imageIndex);
                    Path outputImagePath = modelResultPath.resolve(exportSource + "__" + FilenameUtils.removeExtension(inputImagePath.getFileName().toString()) + ".h5");
                    exportSourceProgress.log("Extracting result: " + outputImagePath);

                    ImagePlus imagePlus = IJ1Hdf5.readImage(outputImagePath, "exported_data", null, progressInfo);
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
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (!isPassThrough()) {
            reportConfiguredIlastikEnvironmentValidity(reportContext, report);
        }
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        super.getEnvironmentDependencies(target);
        target.add(getConfiguredIlastikEnvironment());
    }

    @SetJIPipeDocumentation(name = "Validate Ilastik project", description = "Determines how/if the node validates the input projects. This is done to check if the project is supported by this node.")
    @JIPipeParameter("project-validation-mode")
    public IlastikProjectValidationMode getProjectValidationMode() {
        return projectValidationMode;
    }

    @JIPipeParameter("project-validation-mode")
    public void setProjectValidationMode(IlastikProjectValidationMode projectValidationMode) {
        this.projectValidationMode = projectValidationMode;
    }

    @SetJIPipeDocumentation(name = "Generated outputs", description = "Select which outputs should be generated. " +
            "Please note that the number of outputs impacts the performance.")
    @JIPipeParameter("output-parameters")
    public OutputParameters getOutputParameters() {
        return outputParameters;
    }

    @SetJIPipeDocumentation(name = "Clean up data after processing", description = "If enabled, data is deleted from temporary directories after " +
            "the processing was finished. Disable this to make it possible to debug your scripts. The directories are accessible via the logs (Tools &gt; Logs).")
    @JIPipeParameter("cleanup-afterwards")
    public boolean isCleanUpAfterwards() {
        return cleanUpAfterwards;
    }

    @JIPipeParameter("cleanup-afterwards")
    public void setCleanUpAfterwards(boolean cleanUpAfterwards) {
        this.cleanUpAfterwards = cleanUpAfterwards;
    }

    @SetJIPipeDocumentation(name = "Override Ilastik environment", description = "If enabled, a different Ilastik environment is used for this node. Otherwise " +
            "the one in the Project > Application settings > Extensions > Ilastik is used.")
    @JIPipeParameter("override-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"org.embl.ilastik:*"})
    public OptionalIlastikEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalIlastikEnvironment overrideEnvironment) {
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

        @SetJIPipeDocumentation(name = "Output probabilities", description = "Exports a multi-channel image where pixel values represent the probability that that pixel belongs to the class represented by that channel")
        @JIPipeParameter("output-probabilities")
        public boolean isOutputProbabilities() {
            return outputProbabilities;
        }

        @JIPipeParameter("output-probabilities")
        public void setOutputProbabilities(boolean outputProbabilities) {
            this.outputProbabilities = outputProbabilities;
        }

        @SetJIPipeDocumentation(name = "Output simple segmentation", description = "Produces a single-channel image where the (integer) pixel values indicate the class to which a pixel belongs. " +
                "For this image, every pixel with the same value should belong to the same class of pixels")
        @JIPipeParameter("output-simple-segmentation")
        public boolean isOutputSimpleSegmentation() {
            return outputSimpleSegmentation;
        }

        @JIPipeParameter("output-simple-segmentation")
        public void setOutputSimpleSegmentation(boolean outputSimpleSegmentation) {
            this.outputSimpleSegmentation = outputSimpleSegmentation;
        }

        @SetJIPipeDocumentation(name = "Output uncertainty", description = "Produces an image where pixel intensity is proportional to the uncertainty found when trying to classify that pixel")
        @JIPipeParameter("output-uncertainty")
        public boolean isOutputUncertainty() {
            return outputUncertainty;
        }

        @JIPipeParameter("output-uncertainty")
        public void setOutputUncertainty(boolean outputUncertainty) {
            this.outputUncertainty = outputUncertainty;
        }

        @SetJIPipeDocumentation(name = "Output features", description = "Outputs a multi-channel image where each channel represents one of the computed pixel features")
        @JIPipeParameter("output-features")
        public boolean isOutputFeatures() {
            return outputFeatures;
        }

        @JIPipeParameter("output-features")
        public void setOutputFeatures(boolean outputFeatures) {
            this.outputFeatures = outputFeatures;
        }

        @SetJIPipeDocumentation(name = "Output labels", description = "Outputs an image representing the users’ manually created annotations")
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
