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
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
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
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
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

import static org.hkijena.jipipe.extensions.ilastik.utils.ImgUtils.*;

@JIPipeDocumentation(name = "Ilastik object classification", description = "The object classification workflow aims to classify full objects, based on object-level features and user annotations. " +
        "Supports 'Pixel Classification + Object Classification' projects.")
@JIPipeCitation("Object classification documentation: https://www.ilastik.org/documentation/objects/objects")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Ilastik")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true, description = "The image(s) to classify.")
@JIPipeInputSlot(value = IlastikModelData.class, slotName = "Project", autoCreate = true, description = "The Ilastik project. Must support pixel classification.")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Object Predictions", description = "A label image of the object class predictions")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Object Probabilities", description = "A multi-channel image volume of object prediction probabilities instead of a label image (one channel for each prediction class)")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Blockwise Object Predictions", description = "A label image of the object class predictions. " +
        "The image will be processed in independent blocks. To configure the block size and halo, use the Ilastik GUI.")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Blockwise Object Probabilities", description = "A multi-channel image volume of object prediction probabilities instead of a label image (one channel for each prediction class) " +
        "The image will be processed in independent blocks. To configure the block size and halo, use the Ilastik GUI.")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Pixel Probabilities", description = "Pixel prediction images of the pixel classification part of that workflow")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Features", description = "Table of the computed object features that were used during classification, indexed by object id")
public class IlastikObjectClassificationAlgorithm extends JIPipeSingleIterationAlgorithm {

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
    private OptionalProcessEnvironment overrideEnvironment = new OptionalProcessEnvironment();

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
        this.overrideEnvironment = new OptionalProcessEnvironment(other.overrideEnvironment);
        this.outputParameters = new OutputParameters(other.outputParameters);
        registerSubParameter(outputParameters);
        updateSlots();
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
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
        if(outputParameters.outputPixelProbabilities)
            exportSources.add("Pixel Probabilities");

        // If the user only requests features
        if(outputParameters.outputFeatures && exportSources.isEmpty()) {
            exportSources.add("Object Predictions");
        }

        // Export the projects
        List<Path> exportedModelPaths = new ArrayList<>();
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
                    if(!IlastikUtils.projectSupports(exportedPath, projectType)) {
                        if(projectValidationMode == IlastikProjectValidationMode.CrashOnError) {
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
        }

        // Export images
        List<Path> exportedImagePaths = new ArrayList<>();
        for (int i = 0; i < imageInputSlot.getRowCount(); i++) {
            JIPipeProgressInfo exportProgress = progressInfo.resolveAndLog("Exporting input image", i, imageInputSlot.getRowCount());
            ImagePlusData imagePlusData = imageInputSlot.getData(i, ImagePlusData.class, exportProgress);
            DefaultDataset dataset = new DefaultDataset(JIPipe.getInstance().getContext(), new ImgPlus(ImageJFunctions.wrap(imagePlusData.getImage())));
            Path exportedPath = workDirectory.resolve("img_" + i + ".h5");
            Hdf5.writeDataset(exportedPath.toFile(), "data", (ImgPlus) dataset.getImgPlus(), 1, DEFAULT_AXES, value -> {
            });
            exportedImagePaths.add(exportedPath);
        }


        // Run analysis
        for (int i = 0; i < exportedModelPaths.size(); i++) {
            Path modelPath = exportedModelPaths.get(i);
            Path modelResultPath = PathUtils.resolveAndMakeSubDirectory(workDirectory, "project_" + i);
            JIPipeProgressInfo modelProgress = progressInfo.resolveAndLog("Project", i, exportedModelPaths.size());

            for (int j = 0; j < exportSources.size(); j++) {
                String exportSource = exportSources.get(j);
                JIPipeProgressInfo exportSourceProgress = modelProgress.resolveAndLog(exportSource, j, exportSources.size());
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
                if(i == 0 && outputParameters.outputFeatures) {
                    // Special handling for the features table
                    args.add("--table_filename=" + modelResultPath + "/features__{nickname}.csv");
                }
                args.add("--raw_data=" + workDirectory.toAbsolutePath() + "/*.h5");

                // Run ilastik
                IlastikExtension.runIlastik(overrideEnvironment.getContentOrDefault(IlastikSettings.getInstance().getEnvironment()),
                        args,
                        exportSourceProgress.resolve("Run Ilastik"),
                        false);

                // Extract results
                for (int k = 0; k < exportedImagePaths.size(); k++) {
                    Path inputImagePath = exportedImagePaths.get(k);
                    List<JIPipeTextAnnotation> textAnnotations = new ArrayList<>(imageInputSlot.getTextAnnotations(k));
                    textAnnotations.addAll(projectInputSlot.getTextAnnotations(i));
                    List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>(imageInputSlot.getDataAnnotations(k));
                    dataAnnotations.addAll(projectInputSlot.getDataAnnotations(i));

                    JIPipeDataSlot outputImageSlot = getOutputSlot(exportSource);
                    if(outputImageSlot != null) {
                        Path outputImagePath = modelResultPath.resolve(exportSource + "__" + FilenameUtils.removeExtension(inputImagePath.getFileName().toString()) + ".h5");
                        exportSourceProgress.log("Extracting result: " + outputImagePath);

                        ImgPlus dataset = Hdf5.readDataset(outputImagePath.toFile(), "exported_data");
                        ImagePlus imagePlus = ImageJFunctions.wrap(dataset, exportSource);
                        ImageJUtils.calibrate(imagePlus, ImageJCalibrationMode.MinMax, 0, 0);

                        outputImageSlot.addData(new ImagePlusData(imagePlus), textAnnotations, JIPipeTextAnnotationMergeMode.Merge, dataAnnotations, JIPipeDataAnnotationMergeMode.Merge, exportSourceProgress);
                    }

                    // Extract features
                    if(i == 0 && outputParameters.outputFeatures) {
                        Path tablePath = modelResultPath.resolve("features__" + inputImagePath.getFileName().toString() + ".csv");
                        exportSourceProgress.log("Extracting result: " + tablePath);
                        ResultsTableData tableData = ResultsTableData.fromCSV(tablePath);
                        getOutputSlot(OUTPUT_SLOT_FEATURES.getName()).addData(tableData, textAnnotations, JIPipeTextAnnotationMergeMode.Merge, dataAnnotations, JIPipeDataAnnotationMergeMode.Merge, exportSourceProgress);
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

        @JIPipeDocumentation(name = "Output object predictions", description = "If enabled, output a label image of the object class predictions")
        @JIPipeParameter("output-object-predictions")
        public boolean isOutputObjectPredictions() {
            return outputObjectPredictions;
        }

        @JIPipeParameter("output-object-predictions")
        public void setOutputObjectPredictions(boolean outputObjectPredictions) {
            this.outputObjectPredictions = outputObjectPredictions;
        }

        @JIPipeDocumentation(name = "Output object probabilities", description = "If enabled, output a multi-channel image volume of object prediction probabilities instead of a label image (one channel for each prediction class)")
        @JIPipeParameter("output-object-probabilities")
        public boolean isOutputObjectProbabilities() {
            return outputObjectProbabilities;
        }

        @JIPipeParameter("output-object-probabilities")
        public void setOutputObjectProbabilities(boolean outputObjectProbabilities) {
            this.outputObjectProbabilities = outputObjectProbabilities;
        }

        @JIPipeDocumentation(name = "Output object predictions (blockwise)", description = "If enabled, output a label image of the object class predictions. " +
                "The image will be processed in independent blocks. To configure the block size and halo, use the Ilastik GUI.")
        @JIPipeParameter("output-blockwise-object-predictions")
        public boolean isOutputBlockwiseObjectPredictions() {
            return outputBlockwiseObjectPredictions;
        }

        @JIPipeParameter("output-blockwise-object-predictions")
        public void setOutputBlockwiseObjectPredictions(boolean outputBlockwiseObjectPredictions) {
            this.outputBlockwiseObjectPredictions = outputBlockwiseObjectPredictions;
        }

        @JIPipeDocumentation(name = "Output object probabilities (blockwise)", description = "If enabled, output a multi-channel image volume of object prediction probabilities instead of a label image (one channel for each prediction class). " +
                "The image will be processed in independent blocks. To configure the block size and halo, use the Ilastik GUI.")
        @JIPipeParameter("output-blockwise-object-probabilities")
        public boolean isOutputBlockwiseObjectProbabilities() {
            return outputBlockwiseObjectProbabilities;
        }

        @JIPipeParameter("output-blockwise-object-probabilities")
        public void setOutputBlockwiseObjectProbabilities(boolean outputBlockwiseObjectProbabilities) {
            this.outputBlockwiseObjectProbabilities = outputBlockwiseObjectProbabilities;
        }

        @JIPipeDocumentation(name = "Output pixel probabilities", description = "If enabled, output pixel prediction images of the pixel classification part of that workflow")
        @JIPipeParameter("output-pixel-probabilities")
        public boolean isOutputPixelProbabilities() {
            return outputPixelProbabilities;
        }

        @JIPipeParameter("output-pixel-probabilities")
        public void setOutputPixelProbabilities(boolean outputPixelProbabilities) {
            this.outputPixelProbabilities = outputPixelProbabilities;
        }

        @JIPipeDocumentation(name = "Output features table", description = "If enabled, output a aable of the computed object features that were used during classification, indexed by object id")
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