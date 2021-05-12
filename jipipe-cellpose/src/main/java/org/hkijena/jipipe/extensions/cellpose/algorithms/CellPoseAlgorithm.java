package org.hkijena.jipipe.extensions.cellpose.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.cellpose.CellPoseModel;
import org.hkijena.jipipe.extensions.cellpose.parameters.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalDoubleParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Cellpose", description = "Runs Cellpose on the input image. If the input image is 3D, " +
        "Cellpose will be executed in 3D mode. Following outputs can be generated: " +
        "<ul>" +
        "<li><b>Labels:</b> A grayscale image where each connected component is assigned a unique value. " +
        "Please note that ImageJ will run into issues if too many objects are present and the labels are saved in int32. ImageJ would load them as float32.</li>" +
        "<li><b>Flows:</b> An HSB image that indicates the flow of each pixel.</li>" +
        "<li><b>Probabilities:</b> An image indicating the probabilities for each pixel.</li>" +
        "<li><b>Styles:</b> A vector summarizing each image.</li>" +
        "<li><b>ROI:</b> ROI of the segmented areas.</li>" +
        "</ul>")
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Labels")
@JIPipeOutputSlot(value = ImagePlus3DColorHSBData.class, slotName = "Flows")
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
    private OptionalAnnotationNameParameter diameterAnnotation = new OptionalAnnotationNameParameter("Diameter", true);


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
        this.performanceParameters = new PerformanceParameters(other.performanceParameters);
        this.enhancementParameters = new EnhancementParameters(other.enhancementParameters);
        this.thresholdParameters = new ThresholdParameters(other.thresholdParameters);
        updateOutputSlots();
        registerSubParameter(modelParameters);
        registerSubParameter(performanceParameters);
        registerSubParameter(enhancementParameters);
        registerSubParameter(thresholdParameters);
        registerSubParameter(outputParameters);
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        super.onParameterChanged(event);
        if(event.getSource() == outputParameters) {
            updateOutputSlots();
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

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
                slotConfiguration.addOutputSlot("Flows", ImagePlus3DColorHSBData.class, null, false);
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
