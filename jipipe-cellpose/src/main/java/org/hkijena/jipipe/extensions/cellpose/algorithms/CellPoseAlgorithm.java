package org.hkijena.jipipe.extensions.cellpose.algorithms;

import ij.IJ;
import ij.ImagePlus;
import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.cellpose.CellPoseSettings;
import org.hkijena.jipipe.extensions.cellpose.CellPoseUtils;
import org.hkijena.jipipe.extensions.cellpose.parameters.*;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalDoubleParameter;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.MacroUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
public class CellPoseAlgorithm extends JIPipeSimpleIteratingAlgorithm {

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
        if(event.getSource() == outputParameters) {
            updateOutputSlots();
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        if (!isPassThrough()) {
            if(overrideEnvironment.isEnabled()) {
                report.forCategory("Override Python environment").report(overrideEnvironment.getContent());
            }
            else {
                CellPoseSettings.checkPythonSettings(report.forCategory("Python"));
            }
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        Path workDirectory = RuntimeSettings.generateTempDirectory("cellpose");
        Path inputImagePath = workDirectory.resolve("input.tif");
        Path outputRoiOutline = workDirectory.resolve("rois.json");
        Path outputDiameters = workDirectory.resolve("diameters.txt");
        Path outputLabels = workDirectory.resolve("labels.tif");
        Path outputFlows = workDirectory.resolve("flows.tif");
        Path outputProbabilities = workDirectory.resolve("probabilities.tif");
        Path outputStyles = workDirectory.resolve("styles.tif");

        // Save raw image
        progressInfo.log("Saving input image to " + inputImagePath);
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlus3DGreyscaleData.class, progressInfo).getImage();
        IJ.saveAs(img, "TIFF", inputImagePath.toString());

        // Generate code
        StringBuilder code = new StringBuilder();
        code.append("from cellpose import models\n");
        code.append("from cellpose import utils, io\n");
        code.append("import json\n");
        code.append("import numpy as np\n\n");

        code.append("data_is_3d = ").append(PythonUtils.objectToPython(img.getNDimensions() > 2)).append("\n");
        code.append("enable_3d_segmentation = ").append(PythonUtils.objectToPython(img.getNDimensions() > 2 && enable3DSegmentation)).append("\n\n");

        Map<String, Object> modelParameterMap = new HashMap<>();
        modelParameterMap.put("model_type", getModelParameters().getModel().getId());
        modelParameterMap.put("net_avg", getEnhancementParameters().isNetAverage());
        modelParameterMap.put("gpu", getModelParameters().isEnableGPU());

        code.append(String.format("model = models.Cellpose(%s)\n", PythonUtils.mapToPythonArguments(modelParameterMap)));
        code.append("input_file = \"").append(MacroUtils.escapeString(inputImagePath.toString())).append("\"\n");
        code.append("img = io.imread(input_file)\n");

        // Add split code for 3D data in 2D plane mode
        code.append("if data_is_3d and not enable_3d_segmentation:\n" +
                "    imgs = []\n" +
                "    for z in range(img.shape[0]):\n" +
                "        imgs.append(img[z,:,:])\n" +
                "    img = imgs\n\n");

        Map<String, Object> evalParameterMap = new HashMap<>();
        evalParameterMap.put("x", PythonUtils.rawPythonCode("img"));
        evalParameterMap.put("diameter", getDiameter().isEnabled() ? getDiameter().getContent() : null);
        evalParameterMap.put("channels", PythonUtils.rawPythonCode("[[0, 0]]"));
        evalParameterMap.put("do_3D", PythonUtils.rawPythonCode("enable_3d_segmentation"));
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

        code.append(String.format("masks, flows, styles, diams = model.eval(%s)\n",
                PythonUtils.mapToPythonArguments(evalParameterMap)
                ));

        // Re-merge masks
        if(outputParameters.isOutputROI() || outputParameters.isOutputLabels()) {
            code.append("if data_is_3d and not enable_3d_segmentation:\n" +
                    "    masks = np.stack(masks, 0)\n");
        }

        // Generate ROI output
        if(outputParameters.isOutputROI()) {
            code.append("roi_list = []\n" +
                    "if masks.ndim == 3:\n" +
                    "    for z in range(masks.shape[0]):\n" +
                    "        coords_list = utils.outlines_list(masks[z,:,:])\n" +
                    "        for coords in coords_list:\n" +
                    "            roi = dict(z=z, coords=[ dict(x=int(x[0]), y=int(x[1])) for x in coords ])\n" +
                    "            roi_list.append(roi)\n" +
                    "else:\n" +
                    "    coords_list = utils.outlines_list(masks)\n" +
                    "    for coords in coords_list:\n" +
                    "        roi = dict(coords=[ dict(x=int(x[0]), y=int(x[1])) for x in coords ])\n" +
                    "        roi_list.append(roi)\n");
            code.append(String.format("with open(\"%s\", \"w\") as f:\n" +
                            "    json.dump(roi_list, f, indent=4)\n\n",
                    MacroUtils.escapeString(outputRoiOutline.toString())));
        }
        if(outputParameters.isOutputLabels()) {
            code.append("if masks.dtype != np.short and masks.dtype != np.uint8:\n" +
                    "    masks = masks.astype(np.float32)\n");
            code.append("io.imsave(").append(PythonUtils.objectToPython(outputLabels)).append(", masks)\n");
        }
        if(outputParameters.isOutputFlows()) {
            code.append("if data_is_3d and not enable_3d_segmentation:\n" +
                    "    flows_rgb = np.stack([x[0] for x in flows], 0)\n" +
                    "else:\n" +
                    "    flows_rgb = flows[0]\n");
            code.append("io.imsave(").append(PythonUtils.objectToPython(outputFlows)).append(", flows_rgb)\n");
        }
        if(outputParameters.isOutputProbabilities()) {
            code.append("if data_is_3d and not enable_3d_segmentation:\n" +
                    "    probs = np.stack([x[2] for x in flows], 0)\n" +
                    "else:\n" +
                    "    probs = flows[2]\n");
            code.append("io.imsave(").append(PythonUtils.objectToPython(outputProbabilities)).append(", probs)\n");
        }
        if(outputParameters.isOutputStyles()) {
            code.append("if data_is_3d and not enable_3d_segmentation:\n" +
                    "    styles = np.stack(styles, 0)\n");
            code.append("io.imsave(").append(PythonUtils.objectToPython(outputStyles)).append(", styles)\n");
        }

        // Write diameters
        if(diameterAnnotation.isEnabled()) {
            code.append(String.format("with open(\"%s\", \"w\") as f:\n", MacroUtils.escapeString(outputDiameters.toString())));
            code.append("    f.write(str(diams))\n");
        }

        // Run script
        PythonUtils.runPython(code.toString(), overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() :
                CellPoseSettings.getInstance().getPythonEnvironment(), progressInfo);

        // Read diameters
        List<JIPipeAnnotation> annotationList = new ArrayList<>();
        if(diameterAnnotation.isEnabled()) {
            try {
                String value = new String(Files.readAllBytes(outputDiameters), StandardCharsets.UTF_8);
                diameterAnnotation.addAnnotationIfEnabled(annotationList, value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Extract outputs
        if(outputParameters.isOutputROI()) {
            ROIListData rois = CellPoseUtils.cellPoseROIJsonToImageJ(outputRoiOutline);
            dataBatch.addOutputData("ROI", rois, annotationList, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        }
        if(outputParameters.isOutputLabels()) {
            ImagePlus labels = IJ.openImage(outputLabels.toString());
            dataBatch.addOutputData("Labels", new ImagePlus3DGreyscaleData(labels), annotationList, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        }
        if(outputParameters.isOutputFlows()) {
            ImagePlus flows = IJ.openImage(outputFlows.toString());
            dataBatch.addOutputData("Flows", new ImagePlus3DColorRGBData(flows), annotationList, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        }
        if(outputParameters.isOutputProbabilities()) {
            ImagePlus probabilities = IJ.openImage(outputProbabilities.toString());
            dataBatch.addOutputData("Probabilities", new ImagePlus3DGreyscale32FData(probabilities), annotationList, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        }
        if(outputParameters.isOutputStyles()) {
            ImagePlus styles = IJ.openImage(outputStyles.toString());
            dataBatch.addOutputData("Styles", new ImagePlus3DGreyscale32FData(styles), annotationList, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        }

        // Cleanup
        if(cleanUpAfterwards) {
            try {
                FileUtils.deleteDirectory(workDirectory.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        if(outputParameters.isOutputLabels()) {
            if(!getOutputSlotMap().containsKey("Labels")) {
                slotConfiguration.addOutputSlot("Labels", ImagePlus3DGreyscaleData.class, null, false);
            }
        }
        else {
            if(getOutputSlotMap().containsKey("Labels")) {
                slotConfiguration.removeOutputSlot("Labels", false);
            }
        }
        if(outputParameters.isOutputFlows()) {
            if(!getOutputSlotMap().containsKey("Flows")) {
                slotConfiguration.addOutputSlot("Flows", ImagePlus3DColorRGBData.class, null, false);
            }
        }
        else {
            if(getOutputSlotMap().containsKey("Flows")) {
                slotConfiguration.removeOutputSlot("Flows", false);
            }
        }
        if(outputParameters.isOutputProbabilities()) {
            if(!getOutputSlotMap().containsKey("Probabilities")) {
                slotConfiguration.addOutputSlot("Probabilities", ImagePlus3DGreyscale32FData.class, null, false);
            }
        }
        else {
            if(getOutputSlotMap().containsKey("Probabilities")) {
                slotConfiguration.removeOutputSlot("Probabilities", false);
            }
        }
        if(outputParameters.isOutputStyles()) {
            if(!getOutputSlotMap().containsKey("Styles")) {
                slotConfiguration.addOutputSlot("Styles", ImagePlus3DGreyscale32FData.class, null, false);
            }
        }
        else {
            if(getOutputSlotMap().containsKey("Styles")) {
                slotConfiguration.removeOutputSlot("Styles", false);
            }
        }
        if(outputParameters.isOutputROI()) {
            if(!getOutputSlotMap().containsKey("ROI")) {
                slotConfiguration.addOutputSlot("ROI", ROIListData.class, null, false);
            }
        }
        else {
            if(getOutputSlotMap().containsKey("ROI")) {
                slotConfiguration.removeOutputSlot("ROI", false);
            }
        }
    }
}
