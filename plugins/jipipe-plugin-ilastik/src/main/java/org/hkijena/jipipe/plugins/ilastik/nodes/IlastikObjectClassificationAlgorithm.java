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
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.apache.commons.io.FilenameUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
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
import org.hkijena.jipipe.plugins.ilastik.utils.hdf5.Hdf5;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hkijena.jipipe.plugins.ilastik.utils.ImgUtils.*;

@SetJIPipeDocumentation(name = "Ilastik object classification", description = "The object classification workflow aims to classify full objects, based on object-level features and user annotations. " +
        "Supports 'Pixel Classification + Object Classification' projects.")
@AddJIPipeCitation("Object classification documentation: https://www.ilastik.org/documentation/objects/objects")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Ilastik")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image", create = true, description = "The image(s) to classify.")
@AddJIPipeInputSlot(value = IlastikModelData.class, name = "Project", create = true, description = "The Ilastik project. Must support pixel classification.")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Object Predictions", description = "A label image of the object class predictions")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Object Probabilities", description = "A multi-channel image volume of object prediction probabilities instead of a label image (one channel for each prediction class)")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Blockwise Object Predictions", description = "A label image of the object class predictions. " +
        "The image will be processed in independent blocks. To configure the block size and halo, use the Ilastik GUI.")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Blockwise Object Probabilities", description = "A multi-channel image volume of object prediction probabilities instead of a label image (one channel for each prediction class) " +
        "The image will be processed in independent blocks. To configure the block size and halo, use the Ilastik GUI.")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Pixel Probabilities", description = "Pixel prediction images of the pixel classification part of that workflow")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Features", description = "Table of the computed object features that were used during classification, indexed by object id")
public class IlastikObjectClassificationAlgorithm extends JIPipeSingleIterationAlgorithm implements IlastikEnvironmentAccessNode {

    public static final List<String> PROJECT_TYPES = Arrays.asList("ObjectClassification", "PixelClassification");

    public static final JIPipeDataSlotInfo OUTPUT_SLOT_OBJECT_PREDICTIONS = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Object Predictions",
            "A label image of the object class predictions");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_OBJECT_PROBABILITIES = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Object Probabilities",
            "A multi-channel image volume of object prediction probabilities instead of a label image (one channel for each prediction class)");

    public static final JIPipeDataSlotInfo OUTPUT_SLOT_BLOCKWISE_OBJECT_PREDICTIONS = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Blockwise Object Predictions",
            "A label image of the object class predictions. " +
                    "The image will be processed in independent blocks. To configure the block size and halo, use the Ilastik GUI.");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_BLOCKWISE_OBJECT_PROBABILITIES = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Blockwise Object Probabilities",
            "A multi-channel image volume of object prediction probabilities instead of a label image (one channel for each prediction class). " +
                    "The image will be processed in independent blocks. To configure the block size and halo, use the Ilastik GUI.");

    public static final JIPipeDataSlotInfo OUTPUT_SLOT_PIXEL_PROBABILITIES = new JIPipeDataSlotInfo(ImagePlusData.class,
            JIPipeSlotType.Output,
            "Pixel Probabilities",
            "Pixel prediction images of the pixel classification part of that workflow");
    public static final JIPipeDataSlotInfo OUTPUT_SLOT_FEATURES = new JIPipeDataSlotInfo(ResultsTableData.class,
            JIPipeSlotType.Output,
            "Features",
            "Table of the computed object features that were used during classification, indexed by object id");

    private final OutputParameters outputParameters;
    private boolean cleanUpAfterwards = true;
    private OptionalIlastikEnvironment overrideEnvironment = new OptionalIlastikEnvironment();

    private IlastikProjectValidationMode projectValidationMode = IlastikProjectValidationMode.CrashOnError;

    public IlastikObjectClassificationAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.outputParameters = new OutputParameters();
        registerSubParameter(outputParameters);
        updateSlots();
    }

    public IlastikObjectClassificationAlgorithm(IlastikObjectClassificationAlgorithm other) {
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
        if (outputParameters.outputObjectPredictions)
            exportSources.add("Object Predictions");
        if (outputParameters.outputObjectProbabilities)
            exportSources.add("Object Probabilities");
        if (outputParameters.outputBlockwiseObjectPredictions)
            exportSources.add("Blockwise Object Predictions");
        if (outputParameters.outputBlockwiseObjectProbabilities)
            exportSources.add("Blockwise Object Probabilities");
        if (outputParameters.outputPixelProbabilities)
            exportSources.add("Pixel Probabilities");

        // If the user only requests features
        if (outputParameters.outputFeatures && exportSources.isEmpty()) {
            exportSources.add("Object Predictions");
        }

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
                for (String projectType : PROJECT_TYPES) {
                    exportProgress.log("Checking if " + exportedPath + " supports project type '" + projectType + "'");
                    if (!IlastikUtils.projectSupports(exportedPath, projectType)) {
                        if (projectValidationMode == IlastikProjectValidationMode.CrashOnError) {
                            throw new JIPipeValidationRuntimeException(new GraphNodeValidationReportContext(this),
                                    new IllegalArgumentException("Project does not support '" + projectType + "'"),
                                    "Provided project is not supported by '" + getInfo().getName() + "'!",
                                    "The node tried to load project (row " + i + ") with annotations " + JsonUtils.toJsonString(projectInputSlot.getTextAnnotations(i)) + ", but the project does not support " + projectType,
                                    "Check if the inputs are correct or set the validation mode to 'Skip on error'.");
                        }
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
                if (modelIndex == 0 && outputParameters.outputFeatures) {
                    // Special handling for the features table
                    args.add("--table_filename=" + modelResultPath + "/features__{nickname}.csv");
                }
                args.add("--raw_data=" + workDirectory.toAbsolutePath() + "/*.h5");

                // Run ilastik
                IlastikEnvironment environment = getConfiguredIlastikEnvironment();
                IlastikPlugin.runIlastik(environment,
                        args,
                        false, exportSourceProgress.resolve("Run Ilastik")
                );

                // Extract results
                for (int imageIndex = 0; imageIndex < exportedImagePaths.size(); imageIndex++) {
                    Path inputImagePath = exportedImagePaths.get(imageIndex);
                    List<JIPipeTextAnnotation> textAnnotations = new ArrayList<>(imageInputSlot.getTextAnnotations(imageIndex));
                    textAnnotations.addAll(projectInputSlot.getTextAnnotations(modelIndex));
                    List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>(imageInputSlot.getDataAnnotations(imageIndex));
                    dataAnnotations.addAll(projectInputSlot.getDataAnnotations(modelIndex));
                    JIPipeDataContext newContext = JIPipeDataContext.create(this, exportedImageContexts.get(imageIndex), exportedModelContexts.get(modelIndex));

                    JIPipeDataSlot outputImageSlot = getOutputSlot(exportSource);
                    if (outputImageSlot != null) {
                        Path outputImagePath = modelResultPath.resolve(exportSource + "__" + FilenameUtils.removeExtension(inputImagePath.getFileName().toString()) + ".h5");
                        exportSourceProgress.log("Extracting result: " + outputImagePath);

                        ImgPlus dataset = Hdf5.readDataset(outputImagePath.toFile(), "exported_data");
                        ImagePlus imagePlus = ImageJFunctions.wrap(dataset, exportSource);
                        ImageJUtils.calibrate(imagePlus, ImageJCalibrationMode.MinMax, 0, 0);

                        outputImageSlot.addData(new ImagePlusData(imagePlus),
                                textAnnotations,
                                JIPipeTextAnnotationMergeMode.Merge,
                                dataAnnotations,
                                JIPipeDataAnnotationMergeMode.Merge,
                                newContext,
                                exportSourceProgress);
                    }

                    // Extract features
                    if (modelIndex == 0 && outputParameters.outputFeatures) {
                        Path tablePath = modelResultPath.resolve("features__" + inputImagePath.getFileName().toString() + ".csv");
                        exportSourceProgress.log("Extracting result: " + tablePath);
                        ResultsTableData tableData = ResultsTableData.fromCSV(tablePath);
                        getOutputSlot(OUTPUT_SLOT_FEATURES.getName()).addData(tableData,
                                textAnnotations,
                                JIPipeTextAnnotationMergeMode.Merge,
                                dataAnnotations,
                                JIPipeDataAnnotationMergeMode.Merge,
                                newContext,
                                exportSourceProgress);
                    }
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

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        super.getEnvironmentDependencies(target);
        target.add(getConfiguredIlastikEnvironment());
    }

    private void updateSlots() {
        toggleSlot(OUTPUT_SLOT_OBJECT_PREDICTIONS, outputParameters.outputObjectPredictions);
        toggleSlot(OUTPUT_SLOT_OBJECT_PROBABILITIES, outputParameters.outputPixelProbabilities);
        toggleSlot(OUTPUT_SLOT_BLOCKWISE_OBJECT_PREDICTIONS, outputParameters.outputBlockwiseObjectPredictions);
        toggleSlot(OUTPUT_SLOT_BLOCKWISE_OBJECT_PROBABILITIES, outputParameters.outputBlockwiseObjectProbabilities);
        toggleSlot(OUTPUT_SLOT_PIXEL_PROBABILITIES, outputParameters.outputPixelProbabilities);
        toggleSlot(OUTPUT_SLOT_FEATURES, outputParameters.outputFeatures);
    }

    public static class OutputParameters extends AbstractJIPipeParameterCollection {
        private boolean outputObjectPredictions = true;
        private boolean outputObjectProbabilities = false;
        private boolean outputBlockwiseObjectPredictions = false;
        private boolean outputBlockwiseObjectProbabilities = false;
        private boolean outputPixelProbabilities = false;
        private boolean outputFeatures = false;

        public OutputParameters() {
        }

        public OutputParameters(OutputParameters other) {
            this.outputObjectPredictions = other.outputObjectPredictions;
            this.outputObjectProbabilities = other.outputObjectProbabilities;
            this.outputBlockwiseObjectPredictions = other.outputBlockwiseObjectPredictions;
            this.outputBlockwiseObjectProbabilities = other.outputBlockwiseObjectProbabilities;
            this.outputPixelProbabilities = other.outputPixelProbabilities;
            this.outputFeatures = other.outputFeatures;
        }

        @SetJIPipeDocumentation(name = "Output object predictions", description = "If enabled, output a label image of the object class predictions")
        @JIPipeParameter("output-object-predictions")
        public boolean isOutputObjectPredictions() {
            return outputObjectPredictions;
        }

        @JIPipeParameter("output-object-predictions")
        public void setOutputObjectPredictions(boolean outputObjectPredictions) {
            this.outputObjectPredictions = outputObjectPredictions;
        }

        @SetJIPipeDocumentation(name = "Output object probabilities", description = "If enabled, output a multi-channel image volume of object prediction probabilities instead of a label image (one channel for each prediction class)")
        @JIPipeParameter("output-object-probabilities")
        public boolean isOutputObjectProbabilities() {
            return outputObjectProbabilities;
        }

        @JIPipeParameter("output-object-probabilities")
        public void setOutputObjectProbabilities(boolean outputObjectProbabilities) {
            this.outputObjectProbabilities = outputObjectProbabilities;
        }

        @SetJIPipeDocumentation(name = "Output object predictions (blockwise)", description = "If enabled, output a label image of the object class predictions. " +
                "The image will be processed in independent blocks. To configure the block size and halo, use the Ilastik GUI.")
        @JIPipeParameter("output-blockwise-object-predictions")
        public boolean isOutputBlockwiseObjectPredictions() {
            return outputBlockwiseObjectPredictions;
        }

        @JIPipeParameter("output-blockwise-object-predictions")
        public void setOutputBlockwiseObjectPredictions(boolean outputBlockwiseObjectPredictions) {
            this.outputBlockwiseObjectPredictions = outputBlockwiseObjectPredictions;
        }

        @SetJIPipeDocumentation(name = "Output object probabilities (blockwise)", description = "If enabled, output a multi-channel image volume of object prediction probabilities instead of a label image (one channel for each prediction class). " +
                "The image will be processed in independent blocks. To configure the block size and halo, use the Ilastik GUI.")
        @JIPipeParameter("output-blockwise-object-probabilities")
        public boolean isOutputBlockwiseObjectProbabilities() {
            return outputBlockwiseObjectProbabilities;
        }

        @JIPipeParameter("output-blockwise-object-probabilities")
        public void setOutputBlockwiseObjectProbabilities(boolean outputBlockwiseObjectProbabilities) {
            this.outputBlockwiseObjectProbabilities = outputBlockwiseObjectProbabilities;
        }

        @SetJIPipeDocumentation(name = "Output pixel probabilities", description = "If enabled, output pixel prediction images of the pixel classification part of that workflow")
        @JIPipeParameter("output-pixel-probabilities")
        public boolean isOutputPixelProbabilities() {
            return outputPixelProbabilities;
        }

        @JIPipeParameter("output-pixel-probabilities")
        public void setOutputPixelProbabilities(boolean outputPixelProbabilities) {
            this.outputPixelProbabilities = outputPixelProbabilities;
        }

        @SetJIPipeDocumentation(name = "Output features table", description = "If enabled, output a aable of the computed object features that were used during classification, indexed by object id")
        @JIPipeParameter("output-features")
        public boolean isOutputFeatures() {
            return outputFeatures;
        }

        @JIPipeParameter("output-features")
        public void setOutputFeatures(boolean outputFeatures) {
            this.outputFeatures = outputFeatures;
        }
    }
}
