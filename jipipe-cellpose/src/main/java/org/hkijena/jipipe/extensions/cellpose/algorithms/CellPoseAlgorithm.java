package org.hkijena.jipipe.extensions.cellpose.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.cellpose.CellPoseModel;
import org.hkijena.jipipe.extensions.cellpose.parameters.EnhancementParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.ModelParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.PerformanceParameters;
import org.hkijena.jipipe.extensions.cellpose.parameters.ThresholdParameters;
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
    private boolean outputLabels = false;
    private boolean outputFlows = false;
    private boolean outputProbabilities = false;
    private boolean outputStyles = false;
    private boolean outputROI = true;
    private OptionalDoubleParameter diameter = new OptionalDoubleParameter(30.0, true);
    private OptionalAnnotationNameParameter diameterAnnotation = new OptionalAnnotationNameParameter("Diameter", true);


    public CellPoseAlgorithm(JIPipeNodeInfo info) {
        super(info);
        updateOutputSlots();
        registerSubParameter(modelParameters);
        registerSubParameter(performanceParameters);
        registerSubParameter(enhancementParameters);
        registerSubParameter(thresholdParameters);
    }

    public CellPoseAlgorithm(CellPoseAlgorithm other) {
        super(other);
        this.diameter = new OptionalDoubleParameter(other.diameter);
        this.diameterAnnotation = new OptionalAnnotationNameParameter(other.diameterAnnotation);
        this.modelParameters = new ModelParameters(other.modelParameters);
        this.performanceParameters = new PerformanceParameters(other.performanceParameters);
        this.enhancementParameters = new EnhancementParameters(other.enhancementParameters);
        this.thresholdParameters = new ThresholdParameters(other.thresholdParameters);
        this.outputLabels = other.outputLabels;
        this.outputFlows = other.outputFlows;
        this.outputProbabilities = other.outputProbabilities;
        this.outputStyles = other.outputStyles;
        this.outputROI = other.outputROI;
        updateOutputSlots();
        registerSubParameter(modelParameters);
        registerSubParameter(performanceParameters);
        registerSubParameter(enhancementParameters);
        registerSubParameter(thresholdParameters);
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

    @JIPipeDocumentation(name = "Output labels", description = "Output an image that contains a unique greyscale value for each detected object.")
    @JIPipeParameter("output-labels")
    public boolean isOutputLabels() {
        return outputLabels;
    }

    @JIPipeParameter("output-labels")
    public void setOutputLabels(boolean outputLabels) {
        this.outputLabels = outputLabels;
        updateOutputSlots();
    }

    @JIPipeDocumentation(name = "Output flows", description = "Output an HSB image that indicates the flow of each pixel.")
    @JIPipeParameter("output-flows")
    public boolean isOutputFlows() {
        return outputFlows;
    }

    @JIPipeParameter("output-flows")
    public void setOutputFlows(boolean outputFlows) {
        this.outputFlows = outputFlows;
        updateOutputSlots();
    }

    @JIPipeDocumentation(name = "Output probabilities", description = "Output a greyscale image indicating the probability of each pixel being an object.")
    @JIPipeParameter("output-probabilities")
    public boolean isOutputProbabilities() {
        return outputProbabilities;
    }

    @JIPipeParameter("output-probabilities")
    public void setOutputProbabilities(boolean outputProbabilities) {
        this.outputProbabilities = outputProbabilities;
        updateOutputSlots();
    }

    @JIPipeDocumentation(name = "Output styles",description = "Output a 1D vector that summarizes the image.")
    @JIPipeParameter("output-styles")
    public boolean isOutputStyles() {
        return outputStyles;
    }

    @JIPipeParameter("output-styles")
    public void setOutputStyles(boolean outputStyles) {
        this.outputStyles = outputStyles;
        updateOutputSlots();
    }

    @JIPipeDocumentation(name = "Output ROI", description = "Output a ROI list containing all detected objects")
    @JIPipeParameter("output-roi")
    public boolean isOutputROI() {
        return outputROI;
    }

    @JIPipeParameter("output-roi")
    public void setOutputROI(boolean outputROI) {
        this.outputROI = outputROI;
        updateOutputSlots();
    }

    @JIPipeDocumentation(name = "Model", description = "The following settings are related to the model.")
    @JIPipeParameter("model-parameters")
    public ModelParameters getModelParameters() {
        return modelParameters;
    }

    @JIPipeDocumentation(name = "Performance", description = "The following settings are related to the performance of the operation (e.g., tiling).")
    @JIPipeParameter("performance-parameters")
    public PerformanceParameters getPerformanceParameters() {
        return performanceParameters;
    }

    @JIPipeDocumentation(name = "Tweaks", description = "Additional options like augmentation and averaging over multiple networks")
    @JIPipeParameter("enhancement-parameters")
    public EnhancementParameters getEnhancementParameters() {
        return enhancementParameters;
    }

    @JIPipeDocumentation(name = "Thresholds", description = "Parameters that control which objects are selected")
    @JIPipeParameter("enhancement-parameters")
    public ThresholdParameters getThresholdParameters() {
        return thresholdParameters;
    }

    private void updateOutputSlots() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
        if(outputLabels) {
            if(!getOutputSlotMap().containsKey("Labels")) {
                slotConfiguration.addOutputSlot("Labels", ImagePlus3DGreyscaleData.class, null, false);
            }
        }
        else {
            if(getOutputSlotMap().containsKey("Labels")) {
                slotConfiguration.removeOutputSlot("Labels", false);
            }
        }
        if(outputFlows) {
            if(!getOutputSlotMap().containsKey("Flows")) {
                slotConfiguration.addOutputSlot("Flows", ImagePlus3DColorHSBData.class, null, false);
            }
        }
        else {
            if(getOutputSlotMap().containsKey("Flows")) {
                slotConfiguration.removeOutputSlot("Flows", false);
            }
        }
        if(outputProbabilities) {
            if(!getOutputSlotMap().containsKey("Probabilities")) {
                slotConfiguration.addOutputSlot("Probabilities", ImagePlus3DGreyscale32FData.class, null, false);
            }
        }
        else {
            if(getOutputSlotMap().containsKey("Probabilities")) {
                slotConfiguration.removeOutputSlot("Probabilities", false);
            }
        }
        if(outputStyles) {
            if(!getOutputSlotMap().containsKey("Styles")) {
                slotConfiguration.addOutputSlot("Styles", ImagePlus3DGreyscale32FData.class, null, false);
            }
        }
        else {
            if(getOutputSlotMap().containsKey("Styles")) {
                slotConfiguration.removeOutputSlot("Styles", false);
            }
        }
        if(outputROI) {
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
