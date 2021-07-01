package org.hkijena.jipipe.extensions.cellpose.algorithms;

import com.google.common.collect.ImmutableList;
import ij.IJ;
import ij.ImagePlus;
import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.cellpose.CellPoseModel;
import org.hkijena.jipipe.extensions.cellpose.CellPoseSettings;
import org.hkijena.jipipe.extensions.cellpose.CellPoseUtils;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseModelData;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseSizeModelData;
import org.hkijena.jipipe.extensions.cellpose.parameters.EnhancementParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.ModelParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.OutputParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.PerformanceParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.ThresholdParameters;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalDoubleParameter;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Cellpose", description = "Runs Cellpose on the input image. This node supports both segmentation in 3D and executing " +
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
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Labels")
@JIPipeOutputSlot(value = ImagePlus3DColorRGBData.class, slotName = "Flows")
@JIPipeOutputSlot(value = ImagePlus3DGreyscale32FData.class, slotName = "Probabilities")
@JIPipeOutputSlot(value = ImagePlus2DGreyscale32FData.class, slotName = "Styles")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
public class CellPoseAlgorithm extends JIPipeMergingAlgorithm {

    private ModelParameters modelParameters = new ModelParameters();
    private PerformanceParameters performanceParameters = new PerformanceParameters();
    private EnhancementParameters enhancementParameters = new EnhancementParameters();
    private ThresholdParameters thresholdParameters = new ThresholdParameters();
    private OutputParameters outputParameters = new OutputParameters();
    private OptionalDoubleParameter diameter = new OptionalDoubleParameter(30.0, true);
    private boolean enable3DSegmentation = true;
    private OptionalAnnotationNameParameter diameterAnnotation = new OptionalAnnotationNameParameter("Diameter", true);
    private boolean cleanUpAfterwards = true;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();

    public CellPoseAlgorithm(JIPipeNodeInfo info) {
        super(info);
        updateOutputSlots();
        updateInputSlots();
        registerSubParameter(modelParameters);
        registerSubParameter(performanceParameters);
        registerSubParameter(enhancementParameters);
        registerSubParameter(thresholdParameters);
        registerSubParameter(outputParameters);
    }

    public CellPoseAlgorithm(CellPoseAlgorithm other) {
        super(other);
        this.diameter = new OptionalDoubleParameter(other.diameter);
        this.diameterAnnotation = new OptionalAnnotationNameParameter(other.diameterAnnotation);
        this.modelParameters = new ModelParameters(other.modelParameters);
        this.outputParameters = new OutputParameters(other.outputParameters);
        this.performanceParameters = new PerformanceParameters(other.performanceParameters);
        this.enhancementParameters = new EnhancementParameters(other.enhancementParameters);
        this.thresholdParameters = new ThresholdParameters(other.thresholdParameters);
        this.overrideEnvironment = new OptionalPythonEnvironment(other.overrideEnvironment);
        this.enable3DSegmentation = other.enable3DSegmentation;
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        updateOutputSlots();
        updateInputSlots();
        registerSubParameter(modelParameters);
        registerSubParameter(performanceParameters);
        registerSubParameter(enhancementParameters);
        registerSubParameter(thresholdParameters);
        registerSubParameter(outputParameters);
    }

    @JIPipeDocumentation(name = "Enable 3D segmentation", description = "If enabled, Cellpose will segment in 3D. Otherwise, " +
            "any 3D image will be processed per-slice. Please note that 3D segmentation requires large amounts of memory.")
    @JIPipeParameter("enable-3d-segmentation")
    public boolean isEnable3DSegmentation() {
        return enable3DSegmentation;
    }

    @JIPipeParameter("enable-3d-segmentation")
    public void setEnable3DSegmentation(boolean enable3DSegmentation) {
        this.enable3DSegmentation = enable3DSegmentation;
    }

    @JIPipeDocumentation(name = "Override Python environment", description = "If enabled, a different Python environment is used for this Node. Otherwise " +
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
        } else if (event.getSource() == modelParameters && event.getKey().equals("model")) {
            updateInputSlots();
        }
    }

    private void updateInputSlots() {
        if (modelParameters.getModel() != CellPoseModel.Custom) {
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
                slotConfiguration.addSlot("Pretrained model", new JIPipeDataSlotInfo(CellPoseModelData.class, JIPipeSlotType.Input, null), false);
            }
            if (!getInputSlotMap().containsKey("Size model")) {
                JIPipeDataSlotInfo slotInfo = new JIPipeDataSlotInfo(CellPoseSizeModelData.class, JIPipeSlotType.Input, null);
                slotInfo.setOptional(true);
                slotConfiguration.addSlot("Size model", slotInfo, false);
            }
        }
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        if (!isPassThrough()) {
            if (overrideEnvironment.isEnabled()) {
                report.resolve("Override Python environment").report(overrideEnvironment.getContent());
            } else {
                CellPoseSettings.checkPythonSettings(report.resolve("Python"));
            }
        }
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        ImmutableList<Integer> inputRowList= ImmutableList.copyOf(dataBatch.getInputRows("Input"));
        Path workDirectory = getNewScratch();

        // Save models if needed
        List<Path> customModelPaths = new ArrayList<>();
        Path customSizeModelPath = null;
        if (modelParameters.getModel() == CellPoseModel.Custom) {
            List<CellPoseModelData> models = dataBatch.getInputData("Pretrained model", CellPoseModelData.class, progressInfo);
            for (int i = 0; i < models.size(); i++) {
                CellPoseModelData modelData = models.get(i);
                Path customModelPath = workDirectory.resolve(i + "_" + modelData.getName());
                try {
                    Files.write(customModelPath, modelData.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                customModelPaths.add(customModelPath);
            }

            List<CellPoseSizeModelData> sizeModels = dataBatch.getInputData("Size model", CellPoseSizeModelData.class, progressInfo);
            if (sizeModels.size() > 1) {
                throw new UserFriendlyRuntimeException("Only 1 size model supported!",
                        "Only one size model is supported!",
                        getDisplayName(),
                        "Currently, the node supports only one size model.",
                        "Remove or modify inputs so that there is only one size model.");
            }
            if (!sizeModels.isEmpty()) {
                CellPoseSizeModelData sizeModelData = sizeModels.get(0);
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
        if (modelParameters.getModel() == CellPoseModel.Custom) {
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
                CellPoseSettings.getInstance().getPythonEnvironment(), Collections.emptyList(), progressInfo);


        for (int i = 0; i < inputRowList.size(); i++) {
            int inputRow = inputRowList.get(i);

            // Fetch original annotations and write them
            List<JIPipeAnnotation> annotationList = new ArrayList<>(getInputSlot("Input").getAnnotations(inputRow));

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
            CellPoseUtils.extractCellposeOutputs(dataBatch,
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
        // This is code that allows to embed a custom model
        code.append("\n\nclass CellposeCustom():\n" +
                "    def __init__(self, gpu=False, pretrained_model=None, diam_mean=None, pretrained_size=None, net_avg=True, device=None, torch=True):\n" +
                "        super(CellposeCustom, self).__init__()\n" +
                "        from cellpose.core import UnetModel, assign_device, check_mkl, use_gpu, MXNET_ENABLED, parse_model_string\n" +
                "        from cellpose.models import CellposeModel, SizeModel\n\n" +
                "        if not torch:\n" +
                "            if not MXNET_ENABLED:\n" +
                "                torch = True\n" +
                "        self.torch = torch\n" +
                "        torch_str = ['','torch'][self.torch]\n" +
                "        \n" +
                "        # assign device (GPU or CPU)\n" +
                "        sdevice, gpu = assign_device(self.torch, gpu)\n" +
                "        self.device = device if device is not None else sdevice\n" +
                "        self.gpu = gpu\n" +
                "        self.pretrained_model = pretrained_model\n" +
                "        self.pretrained_size = pretrained_size\n" +
                "        self.diam_mean = diam_mean\n" +
                "        \n" +
                "        if not net_avg:\n" +
                "            self.pretrained_model = self.pretrained_model[0]\n" +
                "\n" +
                "        self.cp = CellposeModel(device=self.device, gpu=self.gpu,\n" +
                "                                pretrained_model=self.pretrained_model,\n" +
                "                                diam_mean=self.diam_mean, torch=self.torch)\n" +
                "        if pretrained_size is not None:\n" +
                "            self.sz = SizeModel(device=self.device, pretrained_size=self.pretrained_size,\n" +
                "                            cp_model=self.cp)\n" +
                "        else:\n" +
                "            self.sz = None\n" +
                "\n" +
                "    def eval(self, x, batch_size=8, channels=None, channel_axis=None, z_axis=None,\n" +
                "             invert=False, normalize=True, diameter=30., do_3D=False, anisotropy=None,\n" +
                "             net_avg=True, augment=False, tile=True, tile_overlap=0.1, resample=False, interp=True,\n" +
                "             flow_threshold=0.4, cellprob_threshold=0.0, min_size=15, \n" +
                "              stitch_threshold=0.0, rescale=None, progress=None):\n" +
                "        from cellpose.models import models_logger\n" +
                "        tic0 = time.time()\n" +
                "\n" +
                "        estimate_size = True if (diameter is None or diameter==0) else False\n" +
                "        models_logger.info('Estimate size: ' + str(estimate_size))\n" +
                "        if estimate_size and self.pretrained_size is not None and not do_3D and x[0].ndim < 4:\n" +
                "            tic = time.time()\n" +
                "            models_logger.info('~~~ ESTIMATING CELL DIAMETER(S) ~~~')\n" +
                "            diams, _ = self.sz.eval(x, channels=channels, channel_axis=channel_axis, invert=invert, batch_size=batch_size, \n" +
                "                                    augment=augment, tile=tile)\n" +
                "            rescale = self.diam_mean / np.array(diams)\n" +
                "            diameter = None\n" +
                "            models_logger.info('estimated cell diameter(s) in %0.2f sec'%(time.time()-tic))\n" +
                "            models_logger.info('>>> diameter(s) = ')\n" +
                "            if isinstance(diams, list) or isinstance(diams, np.ndarray):\n" +
                "                diam_string = '[' + ''.join(['%0.2f, '%d for d in diams]) + ']'\n" +
                "            else:\n" +
                "                diam_string = '[ %0.2f ]'%diams\n" +
                "            models_logger.info(diam_string)\n" +
                "        elif estimate_size:\n" +
                "            if self.pretrained_size is None:\n" +
                "                reason = 'no pretrained size model specified in model Cellpose'\n" +
                "            else:\n" +
                "                reason = 'does not work on non-2D images'\n" +
                "            models_logger.warning(f'could not estimate diameter, {reason}')\n" +
                "            diams = self.diam_mean \n" +
                "        else:\n" +
                "            diams = diameter\n" +
                "\n" +
                "        tic = time.time()\n" +
                "        models_logger.info('~~~ FINDING MASKS ~~~')\n" +
                "        masks, flows, styles = self.cp.eval(x, \n" +
                "                                            batch_size=batch_size, \n" +
                "                                            invert=invert, \n" +
                "                                            diameter=diameter,\n" +
                "                                            rescale=rescale, \n" +
                "                                            anisotropy=anisotropy, \n" +
                "                                            channels=channels,\n" +
                "                                            channel_axis=channel_axis, \n" +
                "                                            z_axis=z_axis,\n" +
                "                                            augment=augment, \n" +
                "                                            tile=tile, \n" +
                "                                            do_3D=do_3D, \n" +
                "                                            net_avg=net_avg, \n" +
                "                                            progress=progress,\n" +
                "                                            tile_overlap=tile_overlap,\n" +
                "                                            resample=resample,\n" +
                "                                            interp=interp,\n" +
                "                                            flow_threshold=flow_threshold, \n" +
                "                                            cellprob_threshold=cellprob_threshold,\n" +
                "                                            min_size=min_size, \n" +
                "                                            stitch_threshold=stitch_threshold)\n" +
                "        models_logger.info('>>>> TOTAL TIME %0.2f sec'%(time.time()-tic0))\n" +
                "    \n" +
                "        return masks, flows, styles, diams\n\n");
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

    @JIPipeDocumentation(name = "Annotate with diameter", description = "If enabled, the diameter is attached as annotation. " +
            "Useful if you want to let Cellpose estimate the object diameters.")
    @JIPipeParameter("diameter-annotation")
    public OptionalAnnotationNameParameter getDiameterAnnotation() {
        return diameterAnnotation;
    }

    @JIPipeParameter("diameter-annotation")
    public void setDiameterAnnotation(OptionalAnnotationNameParameter diameterAnnotation) {
        this.diameterAnnotation = diameterAnnotation;
    }

    @JIPipeDocumentation(name = "Diameter", description = "If enabled, Cellpose will use the provided average diameter to find objects. " +
            "Otherwise, Cellpose will estimate the diameter by itself.")
    @JIPipeParameter("diameter")
    public OptionalDoubleParameter getDiameter() {
        return diameter;
    }

    @JIPipeParameter("diameter")
    public void setDiameter(OptionalDoubleParameter diameter) {
        this.diameter = diameter;
    }

    @JIPipeDocumentation(name = "Model", description = "The following settings are related to the model.")
    @JIPipeParameter("model-parameters")
    public ModelParameters getModelParameters() {
        return modelParameters;
    }

    @JIPipeDocumentation(name = "Performance", description = "The following settings are related to the performance of the operation (e.g., tiling).")
    @JIPipeParameter(value = "performance-parameters", collapsed = true)
    public PerformanceParameters getPerformanceParameters() {
        return performanceParameters;
    }

    @JIPipeDocumentation(name = "Tweaks", description = "Additional options like augmentation and averaging over multiple networks")
    @JIPipeParameter("enhancement-parameters")
    public EnhancementParameters getEnhancementParameters() {
        return enhancementParameters;
    }

    @JIPipeDocumentation(name = "Thresholds", description = "Parameters that control which objects are selected.")
    @JIPipeParameter("threshold-parameters")
    public ThresholdParameters getThresholdParameters() {
        return thresholdParameters;
    }

    @JIPipeDocumentation(name = "Outputs", description = "The following settings allow you to select which outputs are generated.")
    @JIPipeParameter(value = "output-parameters", collapsed = true)
    public OutputParameters getOutputParameters() {
        return outputParameters;
    }

    private void updateOutputSlots() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
        if (outputParameters.isOutputLabels()) {
            if (!getOutputSlotMap().containsKey("Labels")) {
                slotConfiguration.addOutputSlot("Labels", ImagePlus3DGreyscaleData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Labels")) {
                slotConfiguration.removeOutputSlot("Labels", false);
            }
        }
        if (outputParameters.isOutputFlows()) {
            if (!getOutputSlotMap().containsKey("Flows")) {
                slotConfiguration.addOutputSlot("Flows", ImagePlus3DColorRGBData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Flows")) {
                slotConfiguration.removeOutputSlot("Flows", false);
            }
        }
        if (outputParameters.isOutputProbabilities()) {
            if (!getOutputSlotMap().containsKey("Probabilities")) {
                slotConfiguration.addOutputSlot("Probabilities", ImagePlus3DGreyscale32FData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Probabilities")) {
                slotConfiguration.removeOutputSlot("Probabilities", false);
            }
        }
        if (outputParameters.isOutputStyles()) {
            if (!getOutputSlotMap().containsKey("Styles")) {
                slotConfiguration.addOutputSlot("Styles", ImagePlus3DGreyscale32FData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Styles")) {
                slotConfiguration.removeOutputSlot("Styles", false);
            }
        }
        if (outputParameters.isOutputROI()) {
            if (!getOutputSlotMap().containsKey("ROI")) {
                slotConfiguration.addOutputSlot("ROI", ROIListData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("ROI")) {
                slotConfiguration.removeOutputSlot("ROI", false);
            }
        }
    }
}

