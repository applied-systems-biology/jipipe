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

package org.hkijena.jipipe.plugins.cellpose.legacy.algorithms;

import com.google.common.collect.ImmutableList;
import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSingleIterationAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.cellpose.Cellpose2PluginApplicationSettings;
import org.hkijena.jipipe.plugins.cellpose.legacy.PretrainedLegacyCellpose2InferenceModel;
import org.hkijena.jipipe.plugins.cellpose.legacy.datatypes.LegacyCellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.legacy.datatypes.LegacyCellposeSizeModelData;
import org.hkijena.jipipe.plugins.cellpose.legacy.parameters.*;
import org.hkijena.jipipe.plugins.cellpose.utils.CellposeUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @deprecated New implementation will be CellposeAlgorithm2 that is based around the Cellpose CLI
 */
@SetJIPipeDocumentation(name = "Cellpose (Deprecated)", description =
        "Runs Cellpose on the input image. This node supports both segmentation in 3D and executing " +
                "Cellpose for each 2D image plane. " +
                "This node can generate a multitude of outputs, although only ROI is activated by default. " +
                "Go to the 'Outputs' parameter section to enable the other outputs." +
                "<ul>" +
                "<li><b>Labels:</b> A grayscale image where each connected component is assigned a unique value. " +
                "Please note that ImageJ will run into issues if too many objects are present and the labels are saved in int32. ImageJ would load them as float32.</li>" +
                "<li><b>Flows:</b> An RGB image that indicates the flow of each pixel. Convert it to HSB to extract information.</li>" +
                "<li><b>Probabilities:</b> An image indicating the probabilities for each pixel.</li>" +
                "<li><b>Styles:</b> A vector summarizing each image.</li>" +
                "<li><b>ROI:</b> ROI of the segmented areas.</li>" +
                "</ul>" +
                "Please note that you need to setup a valid Python environment with Cellpose installed. You can find the setting in Project &gt; Application settings &gt; Extensions &gt; Cellpose.")
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, name = "Labels")
@AddJIPipeOutputSlot(value = ImagePlus3DColorRGBData.class, name = "Flows")
@AddJIPipeOutputSlot(value = ImagePlus3DGreyscale32FData.class, name = "Probabilities")
@AddJIPipeOutputSlot(value = ImagePlus2DGreyscale32FData.class, name = "Styles")
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "ROI")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
@Deprecated
@LabelAsJIPipeHidden
public class Cellpose1InferenceAlgorithm extends JIPipeSingleIterationAlgorithm {

    private CellposeSegmentationModelSettings_Old segmentationModelSettings = new CellposeSegmentationModelSettings_Old();
    private CellposeSegmentationPerformanceSettings_Old SegmentationPerformanceSettings_Old = new CellposeSegmentationPerformanceSettings_Old();
    private CellposeSegmentationEnhancementSettings_Old segmentationEnhancementSettings = new CellposeSegmentationEnhancementSettings_Old();
    private CellposeSegmentationThresholdSettings_Old SegmentationThresholdSettings_Old = new CellposeSegmentationThresholdSettings_Old();
    private CellposeSegmentationOutputSettings_Old outputParameters = new CellposeSegmentationOutputSettings_Old();
    private OptionalDoubleParameter diameter = new OptionalDoubleParameter(30.0, true);
    private boolean enable3DSegmentation = true;
    private OptionalTextAnnotationNameParameter diameterAnnotation = new OptionalTextAnnotationNameParameter("Diameter", true);
    private boolean cleanUpAfterwards = true;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();

    public Cellpose1InferenceAlgorithm(JIPipeNodeInfo info) {
        super(info);
        updateOutputSlots();
        updateInputSlots();
        registerSubParameter(segmentationModelSettings);
        registerSubParameter(SegmentationPerformanceSettings_Old);
        registerSubParameter(segmentationEnhancementSettings);
        registerSubParameter(SegmentationThresholdSettings_Old);
        registerSubParameter(outputParameters);
    }

    public Cellpose1InferenceAlgorithm(Cellpose1InferenceAlgorithm other) {
        super(other);
        this.diameter = new OptionalDoubleParameter(other.diameter);
        this.diameterAnnotation = new OptionalTextAnnotationNameParameter(other.diameterAnnotation);
        this.segmentationModelSettings = new CellposeSegmentationModelSettings_Old(other.segmentationModelSettings);
        this.outputParameters = new CellposeSegmentationOutputSettings_Old(other.outputParameters);
        this.SegmentationPerformanceSettings_Old = new CellposeSegmentationPerformanceSettings_Old(other.SegmentationPerformanceSettings_Old);
        this.segmentationEnhancementSettings = new CellposeSegmentationEnhancementSettings_Old(other.segmentationEnhancementSettings);
        this.SegmentationThresholdSettings_Old = new CellposeSegmentationThresholdSettings_Old(other.SegmentationThresholdSettings_Old);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.enable3DSegmentation = other.enable3DSegmentation;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        updateOutputSlots();
        updateInputSlots();
        registerSubParameter(segmentationModelSettings);
        registerSubParameter(SegmentationPerformanceSettings_Old);
        registerSubParameter(segmentationEnhancementSettings);
        registerSubParameter(SegmentationThresholdSettings_Old);
        registerSubParameter(outputParameters);
    }

    public static void extractCellposeOutputs(JIPipeMultiIterationStep iterationStep, JIPipeProgressInfo progressInfo, Path outputRoiOutline, Path outputLabels, Path outputFlows, Path outputProbabilities, Path outputStyles, List<JIPipeTextAnnotation> annotationList, CellposeSegmentationOutputSettings_Old outputParameters) {
        if (outputParameters.isOutputROI()) {
            ROI2DListData rois = CellposeUtils.cellposeROIJsonToImageJ(outputRoiOutline);
            iterationStep.addOutputData("ROI", rois, annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputLabels()) {
            ImagePlus labels = IJ.openImage(outputLabels.toString());
            iterationStep.addOutputData("Labels", new ImagePlus3DGreyscaleData(labels), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputFlows()) {
            ImagePlus flows = IJ.openImage(outputFlows.toString());
            iterationStep.addOutputData("Flows", new ImagePlus3DColorRGBData(flows), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputProbabilities()) {
            ImagePlus probabilities = IJ.openImage(outputProbabilities.toString());
            iterationStep.addOutputData("Probabilities", new ImagePlus3DGreyscale32FData(probabilities), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
        if (outputParameters.isOutputStyles()) {
            ImagePlus styles = IJ.openImage(outputStyles.toString());
            iterationStep.addOutputData("Styles", new ImagePlus3DGreyscale32FData(styles), annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Enable 3D segmentation", description = "If enabled, Cellpose will segment in 3D. Otherwise, " +
            "any 3D image will be processed per-slice. Please note that 3D segmentation requires large amounts of memory.")
    @JIPipeParameter(value = "enable-3d-segmentation", important = true)
    public boolean isEnable3DSegmentation() {
        return enable3DSegmentation;
    }

    @JIPipeParameter("enable-3d-segmentation")
    public void setEnable3DSegmentation(boolean enable3DSegmentation) {
        this.enable3DSegmentation = enable3DSegmentation;
    }

    @SetJIPipeDocumentation(name = "Override Python environment", description = "If enabled, a different Python environment is used for this Node. Otherwise " +
            "the one in the Project > Application settings > Extensions > Cellpose is used.")
    @JIPipeParameter("override-environment")
    public OptionalPythonEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalPythonEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        super.onParameterChanged(event);
        if (event.getSource() == outputParameters) {
            updateOutputSlots();
        } else if (event.getSource() == segmentationModelSettings && event.getKey().equals("model")) {
            updateInputSlots();
        }
    }

    private void updateInputSlots() {
        if (segmentationModelSettings.getModel() != PretrainedLegacyCellpose2InferenceModel.Custom) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            if (getInputSlotMap().containsKey("Pretrained model")) {
                slotConfiguration.removeInputSlot("Pretrained model", false);
            }
            if (getInputSlotMap().containsKey("Size model")) {
                slotConfiguration.removeInputSlot("Size model", false);
            }
        } else {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            if (!getInputSlotMap().containsKey("Pretrained model")) {
                slotConfiguration.addSlot("Pretrained model", new JIPipeDataSlotInfo(LegacyCellposeModelData.class, JIPipeSlotType.Input, null, "", null), false);
            }
            if (!getInputSlotMap().containsKey("Size model")) {
                JIPipeDataSlotInfo slotInfo = new JIPipeDataSlotInfo(LegacyCellposeSizeModelData.class, JIPipeSlotType.Input, null, "", null);
                slotInfo.setOptional(true);
                slotConfiguration.addSlot("Size model", slotInfo, false);
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (!isPassThrough()) {
            if (overrideEnvironment.isEnabled()) {
                report.report(reportContext, overrideEnvironment.getContent());
            } else {
                Cellpose2PluginApplicationSettings.checkPythonSettings(reportContext, report);
            }
        }
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        super.getEnvironmentDependencies(target);
        if (overrideEnvironment.isEnabled()) {
            target.add(overrideEnvironment.getContent());
        } else {
            target.add(Cellpose2PluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment());
        }
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        ImmutableList<Integer> inputRowList = ImmutableList.copyOf(iterationStep.getInputRows("Input"));
        Path workDirectory = getNewScratch();

        // Save models if needed
        List<Path> customModelPaths = new ArrayList<>();
        Path customSizeModelPath = null;
        if (segmentationModelSettings.getModel() == PretrainedLegacyCellpose2InferenceModel.Custom) {
            List<LegacyCellposeModelData> models = iterationStep.getInputData("Pretrained model", LegacyCellposeModelData.class, progressInfo);
            for (int i = 0; i < models.size(); i++) {
                LegacyCellposeModelData modelData = models.get(i);
                Path customModelPath = workDirectory.resolve(i + "_" + modelData.getName());
                try {
                    Files.write(customModelPath, modelData.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                customModelPaths.add(customModelPath);
            }

            List<LegacyCellposeSizeModelData> sizeModels = iterationStep.getInputData("Size model", LegacyCellposeSizeModelData.class, progressInfo);
            if (sizeModels.size() > 1) {
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new GraphNodeValidationReportContext(this),
                        "Only one size model is supported!",
                        "Currently, the node supports only one size model.",
                        "Remove or modify inputs so that there is only one size model."));
            }
            if (!sizeModels.isEmpty()) {
                LegacyCellposeSizeModelData sizeModelData = sizeModels.get(0);
                Path customModelPath = workDirectory.resolve("sz" + sizeModelData.getName() + ".npy");
                try {
                    Files.write(customModelPath, sizeModelData.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                customSizeModelPath = customModelPath;
            }
        }

        // Generate code
        StringBuilder code = new StringBuilder();
        code.append("from cellpose import models\n");
        code.append("from cellpose import utils, io\n");
        code.append("import json\n");
        code.append("import time\n");
        code.append("import numpy as np\n\n");

        List<Path> inputImagePaths = new ArrayList<>();
        List<Path> outputRoiOutlinePaths = new ArrayList<>();
        List<Path> outputDiameterPaths = new ArrayList<>();
        List<Path> outputLabelsPaths = new ArrayList<>();
        List<Path> outputFlowsPaths = new ArrayList<>();
        List<Path> outputProbabilitiesPaths = new ArrayList<>();
        List<Path> outputStylesPaths = new ArrayList<>();
        List<Boolean> dataIs3DList = new ArrayList<>();
        List<Boolean> enable3DSegmentationList = new ArrayList<>();

        for (Integer inputRow : inputRowList) {
            Path rowWorkDirectory = workDirectory.resolve(inputRow + "");
            try {
                Files.createDirectories(rowWorkDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Path inputImagePath = rowWorkDirectory.resolve("input.tif");
            Path outputRoiOutline = rowWorkDirectory.resolve("rois.json");
            Path outputDiameters = rowWorkDirectory.resolve("diameters.txt");
            Path outputLabels = rowWorkDirectory.resolve("labels.tif");
            Path outputFlows = rowWorkDirectory.resolve("flows.tif");
            Path outputProbabilities = rowWorkDirectory.resolve("probabilities.tif");
            Path outputStyles = rowWorkDirectory.resolve("styles.tif");

            inputImagePaths.add(inputImagePath);
            outputRoiOutlinePaths.add(outputRoiOutline);
            outputDiameterPaths.add(outputDiameters);
            outputLabelsPaths.add(outputLabels);
            outputFlowsPaths.add(outputFlows);
            outputProbabilitiesPaths.add(outputProbabilities);
            outputStylesPaths.add(outputStyles);

            // Save raw image
            progressInfo.log("Saving input image to " + inputImagePath);

            ImagePlus img = getInputSlot("Input").getData(inputRow, ImagePlus3DGreyscaleData.class, progressInfo).getImage();
            IJ.saveAs(img, "TIFF", inputImagePath.toString());

            dataIs3DList.add(img.getNDimensions() > 2);
            enable3DSegmentationList.add(img.getNDimensions() > 2 && enable3DSegmentation);

        }

        // Write the input/output paths
        code.append("input_image_paths = ").append(PythonUtils.objectToPython(inputImagePaths)).append("\n");
        code.append("output_roi_outlines_paths = ").append(PythonUtils.objectToPython(outputRoiOutlinePaths)).append("\n");
        code.append("output_diameter_paths = ").append(PythonUtils.objectToPython(outputDiameterPaths)).append("\n");
        code.append("output_labels_paths = ").append(PythonUtils.objectToPython(outputLabelsPaths)).append("\n");
        code.append("output_flows_paths = ").append(PythonUtils.objectToPython(outputFlowsPaths)).append("\n");
        code.append("output_probabilities_paths = ").append(PythonUtils.objectToPython(outputProbabilitiesPaths)).append("\n");
        code.append("output_styles_paths = ").append(PythonUtils.objectToPython(outputStylesPaths)).append("\n");

        // Write 3D settings
        code.append("data_is_3d = ").append(PythonUtils.objectToPython(dataIs3DList)).append("\n");
        code.append("enable_3d_segmentation = ").append(PythonUtils.objectToPython(enable3DSegmentationList)).append("\n");


        // I we provide a custom model, we need to inject custom code (Why?)
        if (segmentationModelSettings.getModel() == PretrainedLegacyCellpose2InferenceModel.Custom) {
            injectCustomCellposeClass(code);
            setupCustomCellposeModel(code, customModelPaths, customSizeModelPath);
        } else {
            // We can use the combined Cellpose class
            setupCombinedCellposeModel(code);
        }

        code.append("for image_index in range(len(").append("input_image_paths").append(")):\n");

        // Load image
        code.append("    input_file = input_image_paths[image_index]\n");
        code.append("    img = io.imread(input_file)\n");
        code.append("    print(\"Read image with index\", image_index, \"shape\", img.shape)\n");

        // Add split code for 3D data in 2D plane mode
        code.append("    if data_is_3d[image_index] and not enable_3d_segmentation[image_index]:\n" +
                "        imgs = []\n" +
                "        for z in range(img.shape[0]):\n" +
                "            imgs.append(img[z,:,:])\n" +
                "        img = imgs\n\n");

        // Evaluate model
        setupModelEval(code);

        // Re-merge masks
        if (outputParameters.isOutputROI() || outputParameters.isOutputLabels()) {
            code.append("    if data_is_3d[image_index] and not enable_3d_segmentation[image_index]:\n" +
                    "        masks = np.stack(masks, 0)\n");
        }

        // Generate ROI output
        if (outputParameters.isOutputROI()) {
            setupGenerateOutputROI(code);
        }
        if (outputParameters.isOutputLabels()) {
            setupGenerateOutputLabels(code);
        }
        if (outputParameters.isOutputFlows()) {
            setupGenerateOutputFlows(code);
        }
        if (outputParameters.isOutputProbabilities()) {
            setupGenerateOutputProbabilities(code);
        }
        if (outputParameters.isOutputStyles()) {
            setupGenerateOutputStyles(code);
        }

        // Write diameters
        if (diameterAnnotation.isEnabled()) {
            code.append("    with open(output_diameter_paths[image_index], \"w\") as f:\n");
            code.append("        f.write(str(diams))\n");
        }

        // Run script
        PythonUtils.runPython(code.toString(), overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                Cellpose2PluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment(), Collections.emptyList(), false, progressInfo);


        for (int i = 0; i < inputRowList.size(); i++) {
            int inputRow = inputRowList.get(i);

            // Fetch original annotations and write them
            List<JIPipeTextAnnotation> annotationList = new ArrayList<>(getInputSlot("Input").getTextAnnotations(inputRow));

            // Read diameters
            if (diameterAnnotation.isEnabled()) {
                try {
                    String value = new String(Files.readAllBytes(outputDiameterPaths.get(i)), StandardCharsets.UTF_8);
                    diameterAnnotation.addAnnotationIfEnabled(annotationList, value);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Extract outputs
            extractCellposeOutputs(iterationStep,
                    progressInfo,
                    outputRoiOutlinePaths.get(i),
                    outputLabelsPaths.get(i),
                    outputFlowsPaths.get(i),
                    outputProbabilitiesPaths.get(i),
                    outputStylesPaths.get(i),
                    annotationList,
                    outputParameters);
        }

        // Cleanup
        if (cleanUpAfterwards) {
            PathUtils.deleteDirectoryRecursively(workDirectory, progressInfo.resolve("Cleanup"));
        }
    }

    private void setupGenerateOutputStyles(StringBuilder code) {
        code.append("    if data_is_3d[image_index] and not enable_3d_segmentation[image_index]:\n" +
                "        styles = np.stack(styles, 0)\n");
        code.append("    io.imsave(").append("output_styles_paths[image_index]").append(", styles)\n");
    }

    private void setupGenerateOutputProbabilities(StringBuilder code) {
        code.append("    if data_is_3d[image_index] and not enable_3d_segmentation[image_index]:\n" +
                "        probs = np.stack([x[2] for x in flows], 0)\n" +
                "    else:\n" +
                "        probs = flows[2]\n");
        code.append("    io.imsave(").append("output_probabilities_paths[image_index]").append(", probs)\n");
    }

    private void setupGenerateOutputFlows(StringBuilder code) {
        code.append("    if data_is_3d[image_index] and not enable_3d_segmentation[image_index]:\n" +
                "        flows_rgb = np.stack([x[0] for x in flows], 0)\n" +
                "    else:\n" +
                "        flows_rgb = flows[0]\n");
        code.append("    io.imsave(").append("output_flows_paths[image_index]").append(", flows_rgb)\n");
    }

    private void setupGenerateOutputLabels(StringBuilder code) {
        code.append("    if masks.dtype != np.short and masks.dtype != np.uint8:\n" +
                "        masks = masks.astype(np.float32)\n");
        code.append("    io.imsave(").append("output_labels_paths[image_index]").append(", masks)\n");
    }

    private void setupGenerateOutputROI(StringBuilder code) {
        code.append("    roi_list = []\n" +
                "    if masks.ndim == 3:\n" +
                "        for z in range(masks.shape[0]):\n" +
                "            coords_list = utils.outlines_list(masks[z,:,:])\n" +
                "            for coords in coords_list:\n" +
                "                roi = dict(z=z, coords=[ dict(x=int(x[0]), y=int(x[1])) for x in coords ])\n" +
                "                roi_list.append(roi)\n" +
                "    else:\n" +
                "        coords_list = utils.outlines_list(masks)\n" +
                "        for coords in coords_list:\n" +
                "            roi = dict(coords=[ dict(x=int(x[0]), y=int(x[1])) for x in coords ])\n" +
                "            roi_list.append(roi)\n");
        code.append("    with open(output_roi_outlines_paths[image_index], \"w\") as f:\n" +
                "        json.dump(roi_list, f, indent=4)\n\n");
    }

    private void setupModelEval(StringBuilder code) {
        Map<String, Object> evalParameterMap = new HashMap<>();
        evalParameterMap.put("x", PythonUtils.rawPythonCode("img"));
        evalParameterMap.put("diameter", getDiameter().isEnabled() ? getDiameter().getContent() : null);
        evalParameterMap.put("channels", PythonUtils.rawPythonCode("[[0, 0]]"));
        evalParameterMap.put("do_3D", PythonUtils.rawPythonCode("enable_3d_segmentation[image_index]"));
        evalParameterMap.put("normalize", getEnhancementParameters().isNormalize());
        evalParameterMap.put("anisotropy", getEnhancementParameters().getAnisotropy().isEnabled() ?
                getEnhancementParameters().getAnisotropy().getContent() : null);
        evalParameterMap.put("net_avg", getEnhancementParameters().isNetAverage());
        evalParameterMap.put("augment", getEnhancementParameters().isAugment());
        evalParameterMap.put("tile", getPerformanceParameters().isTile());
        evalParameterMap.put("tile_overlap", getPerformanceParameters().getTileOverlap());
        evalParameterMap.put("resample", getPerformanceParameters().isResample());
        evalParameterMap.put("interp", getEnhancementParameters().isInterpolate());
        evalParameterMap.put("flow_threshold", getThresholdParameters().getFlowThreshold());
        evalParameterMap.put("cellprob_threshold", getThresholdParameters().getCellProbabilityThreshold());
        evalParameterMap.put("min_size", getThresholdParameters().getMinSize());
        evalParameterMap.put("stitch_threshold", getThresholdParameters().getStitchThreshold());

        code.append(String.format("    masks, flows, styles, diams = model.eval(%s)\n",
                PythonUtils.mapToPythonArguments(evalParameterMap)
        ));
    }

    private void setupCombinedCellposeModel(StringBuilder code) {
        Map<String, Object> modelParameterMap = new HashMap<>();
        modelParameterMap.put("model_type", getModelParameters().getModel().getId());
        modelParameterMap.put("net_avg", getEnhancementParameters().isNetAverage());
        modelParameterMap.put("gpu", getModelParameters().isEnableGPU());
        code.append(String.format("model = models.Cellpose(%s)\n", PythonUtils.mapToPythonArguments(modelParameterMap)));
    }

    private void setupCustomCellposeModel(StringBuilder code, List<Path> customModelPaths, Path customSizeModelPath) {
        injectCustomCellposeClass(code);
        Map<String, Object> modelParameterMap = new HashMap<>();
        modelParameterMap.put("pretrained_model", customModelPaths.stream().map(Objects::toString).collect(Collectors.toList()));
        modelParameterMap.put("net_avg", getEnhancementParameters().isNetAverage());
        modelParameterMap.put("gpu", getModelParameters().isEnableGPU());
        modelParameterMap.put("diam_mean", getModelParameters().getMeanDiameter());
        if (customSizeModelPath != null)
            modelParameterMap.put("pretrained_size", customSizeModelPath.toString());
        code.append(String.format("model = CellposeCustom(%s)\n", PythonUtils.mapToPythonArguments(modelParameterMap)));
    }

    private void injectCustomCellposeClass(StringBuilder code) {
        if (code.indexOf("class CellposeCustom()") >= 0) {
            return;
        }
        // This is code that allows to embed a custom model
        code.append("\n\n").append(CellposeUtils.getCellposeCustomCode()).append("\n\n");
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

    @SetJIPipeDocumentation(name = "Annotate with diameter", description = "If enabled, the diameter is attached as annotation. " +
            "Useful if you want to let Cellpose estimate the object diameters.")
    @JIPipeParameter("diameter-annotation")
    public OptionalTextAnnotationNameParameter getDiameterAnnotation() {
        return diameterAnnotation;
    }

    @JIPipeParameter("diameter-annotation")
    public void setDiameterAnnotation(OptionalTextAnnotationNameParameter diameterAnnotation) {
        this.diameterAnnotation = diameterAnnotation;
    }

    @SetJIPipeDocumentation(name = "Average object diameter", description = "If enabled, Cellpose will use the provided average diameter to find objects. " +
            "Otherwise, Cellpose will estimate the diameter by itself.")
    @JIPipeParameter(value = "diameter", important = true)
    public OptionalDoubleParameter getDiameter() {
        return diameter;
    }

    @JIPipeParameter("diameter")
    public void setDiameter(OptionalDoubleParameter diameter) {
        this.diameter = diameter;
    }

    @SetJIPipeDocumentation(name = "Cellpose: Model", description = "The following settings are related to the model.")
    @JIPipeParameter(value = "model-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public CellposeSegmentationModelSettings_Old getModelParameters() {
        return segmentationModelSettings;
    }

    @SetJIPipeDocumentation(name = "Cellpose: Performance", description = "The following settings are related to the performance of the operation (e.g., tiling).")
    @JIPipeParameter(value = "performance-parameters", collapsed = true, iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public CellposeSegmentationPerformanceSettings_Old getPerformanceParameters() {
        return SegmentationPerformanceSettings_Old;
    }

    @SetJIPipeDocumentation(name = "Cellpose: Tweaks", description = "Additional options like augmentation and averaging over multiple networks")
    @JIPipeParameter(value = "enhancement-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public CellposeSegmentationEnhancementSettings_Old getEnhancementParameters() {
        return segmentationEnhancementSettings;
    }

    @SetJIPipeDocumentation(name = "Cellpose: Thresholds", description = "Parameters that control which objects are selected.")
    @JIPipeParameter(value = "threshold-parameters", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public CellposeSegmentationThresholdSettings_Old getThresholdParameters() {
        return SegmentationThresholdSettings_Old;
    }

    @SetJIPipeDocumentation(name = "Cellpose: Outputs", description = "The following settings allow you to select which outputs are generated.")
    @JIPipeParameter(value = "output-parameters", collapsed = true, iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/cellpose.png")
    public CellposeSegmentationOutputSettings_Old getOutputParameters() {
        return outputParameters;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == segmentationModelSettings && "mean-diameter".equals(access.getKey())) {
            // This is never needed
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    private void updateOutputSlots() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
        if (outputParameters.isOutputLabels()) {
            if (!getOutputSlotMap().containsKey("Labels")) {
                slotConfiguration.addOutputSlot("Labels", "", ImagePlus3DGreyscaleData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Labels")) {
                slotConfiguration.removeOutputSlot("Labels", false);
            }
        }
        if (outputParameters.isOutputFlows()) {
            if (!getOutputSlotMap().containsKey("Flows")) {
                slotConfiguration.addOutputSlot("Flows", "", ImagePlus3DColorRGBData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Flows")) {
                slotConfiguration.removeOutputSlot("Flows", false);
            }
        }
        if (outputParameters.isOutputProbabilities()) {
            if (!getOutputSlotMap().containsKey("Probabilities")) {
                slotConfiguration.addOutputSlot("Probabilities", "", ImagePlus3DGreyscale32FData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Probabilities")) {
                slotConfiguration.removeOutputSlot("Probabilities", false);
            }
        }
        if (outputParameters.isOutputStyles()) {
            if (!getOutputSlotMap().containsKey("Styles")) {
                slotConfiguration.addOutputSlot("Styles", "", ImagePlus3DGreyscale32FData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Styles")) {
                slotConfiguration.removeOutputSlot("Styles", false);
            }
        }
        if (outputParameters.isOutputROI()) {
            if (!getOutputSlotMap().containsKey("ROI")) {
                slotConfiguration.addOutputSlot("ROI", "", ROI2DListData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("ROI")) {
                slotConfiguration.removeOutputSlot("ROI", false);
            }
        }
    }
}

