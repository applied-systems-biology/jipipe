package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.modify;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.AllMeasurement3DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ij3d.utils.Measurement3DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ij3d.utils.Measurements3DSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Change 3D ROI properties from expressions", description = "Sets the properties of all 3D ROI via expressions")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
public class ChangeRoi3DPropertiesFromExpressionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customVariables;
    private OptionalDefaultExpressionParameter roiName = new OptionalDefaultExpressionParameter(false, "Name");
    private OptionalDefaultExpressionParameter roiComment = new OptionalDefaultExpressionParameter(false, "Comment");
    private OptionalDefaultExpressionParameter centerX = new OptionalDefaultExpressionParameter(false, "CenterX");
    private OptionalDefaultExpressionParameter centerY = new OptionalDefaultExpressionParameter(false, "CenterY");
    private OptionalDefaultExpressionParameter centerZ = new OptionalDefaultExpressionParameter(false, "CenterZ");
    private OptionalDefaultExpressionParameter channelLocation = new OptionalDefaultExpressionParameter(false, "Channel");
    private OptionalDefaultExpressionParameter frameLocation = new OptionalDefaultExpressionParameter(false, "Frame");
    private OptionalDefaultExpressionParameter fillColor = new OptionalDefaultExpressionParameter(false, "FillColor");

    private ParameterCollectionList metadataEntries = ParameterCollectionList.containingCollection(MetadataEntry.class);
    private Measurements3DSetParameter measurements = new Measurements3DSetParameter();
    private boolean measureInPhysicalUnits = true;

    public ChangeRoi3DPropertiesFromExpressionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customVariables = new CustomExpressionVariablesParameter(this);
    }

    public ChangeRoi3DPropertiesFromExpressionsAlgorithm(ChangeRoi3DPropertiesFromExpressionsAlgorithm other) {
        super(other);
        this.customVariables = new CustomExpressionVariablesParameter(other.customVariables, this);
        this.roiName = other.roiName;
        this.roiComment = other.roiComment;
        this.centerX = other.centerX;
        this.centerY = other.centerY;
        this.centerZ = other.centerZ;
        this.channelLocation = other.channelLocation;
        this.frameLocation = other.frameLocation;
        this.fillColor = other.fillColor;
        this.measurements = other.measurements;
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.metadataEntries = new ParameterCollectionList(other.metadataEntries);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData outputROI = new ROI3DListData(dataBatch.getInputData("Input", ROI3DListData.class, progressInfo));
        ImagePlusData inputReference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);

        // Create variables
        ExpressionVariables variableSet = new ExpressionVariables();
        variableSet.putAnnotations(dataBatch.getMergedTextAnnotations());
        customVariables.writeToVariables(variableSet, true, "custom.", true, "custom");

        // Obtain statistics
        ResultsTableData statistics = outputROI.measure(IJ3DUtils.wrapImage(inputReference), measurements.getNativeValue(), measureInPhysicalUnits, progressInfo.resolve("Measuring ROIs"));

        // Write statistics into variables
        for (int col = 0; col < statistics.getColumnCount(); col++) {
            TableColumn column = statistics.getColumnReference(col);
            if (column.isNumeric()) {
                variableSet.set("all." + column.getLabel(), new ArrayList<>(Doubles.asList(column.getDataAsDouble(column.getRows()))));
            } else {
                variableSet.set("all." + column.getLabel(), new ArrayList<>(Arrays.asList(column.getDataAsString(column.getRows()))));
            }
        }
        variableSet.set("num_roi", outputROI.size());

        List<MetadataEntry> evaluatedMetadataEntries = metadataEntries.mapToCollection(MetadataEntry.class);

        for (int row = 0; row < statistics.getRowCount(); row++) {
            ROI3D roi = outputROI.get(row);

            // Write metadata
            Map<String, String> roiProperties = roi.getMetadata();
            variableSet.set("metadata", roiProperties);
            for (Map.Entry<String, String> entry : roiProperties.entrySet()) {
                variableSet.set("metadata." + entry.getKey(), entry.getValue());
            }

            // Write statistics
            for (int col = 0; col < statistics.getColumnCount(); col++) {
                variableSet.set(statistics.getColumnName(col), statistics.getValueAt(row, col));
            }

            // Filter
            if (roiName.isEnabled()) {
                roi.getObject3D().setName(StringUtils.nullToEmpty(roiName.getContent().evaluateToString(variableSet)));
            }
            if (roiComment.isEnabled()) {
                roi.getObject3D().setComment(StringUtils.nullToEmpty(roiComment.getContent().evaluateToString(variableSet)));
            }
            if (centerX.isEnabled() || centerY.isEnabled() || centerZ.isEnabled()) {
                double x = roi.getObject3D().getCenterX();
                double y = roi.getObject3D().getCenterY();
                double z = roi.getObject3D().getCenterZ();
                if(centerX.isEnabled()) {
                    x = centerX.getContent().evaluateToDouble(variableSet);
                }
                if(centerY.isEnabled()) {
                    y = centerY.getContent().evaluateToDouble(variableSet);
                }
                if(centerZ.isEnabled()) {
                    z = centerZ.getContent().evaluateToDouble(variableSet);
                }
                roi.getObject3D().setNewCenter(x,y,z);
            }
            if(channelLocation.isEnabled()) {
                roi.setChannel(channelLocation.getContent().evaluateToInteger(variableSet));
            }
            if(frameLocation.isEnabled()) {
                roi.setFrame(frameLocation.getContent().evaluateToInteger(variableSet));
            }
            if(fillColor.isEnabled()) {
                roi.setFillColor(fillColor.getContent().evaluateToColor(variableSet));
            }
            for (MetadataEntry entry : evaluatedMetadataEntries) {
                if(entry.getMetadataValue().isEnabled()) {
                    roi.getMetadata().put(entry.getMetadataName(),
                            entry.getMetadataValue().getContent().evaluateToString(variableSet));
                }
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputROI, progressInfo);
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomVariables() {
        return customVariables;
    }

    @JIPipeDocumentation(name = "ROI name", description = "If true, override the ROI name")
    @JIPipeParameter("roi-name")
    @ExpressionParameterSettings(hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = Measurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AllMeasurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalDefaultExpressionParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalDefaultExpressionParameter roiName) {
        this.roiName = roiName;
    }

    @JIPipeDocumentation(name = "ROI comment", description = "If true, override the ROI's comment field")
    @JIPipeParameter("roi-comment")
    @ExpressionParameterSettings(hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = Measurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AllMeasurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalDefaultExpressionParameter getRoiComment() {
        return roiComment;
    }

    @JIPipeParameter("roi-comment")
    public void setRoiComment(OptionalDefaultExpressionParameter roiComment) {
        this.roiComment = roiComment;
    }

    @JIPipeDocumentation(name = "Center (X)", description = "If true, override the ROI's center X location")
    @JIPipeParameter("center-x")
    @ExpressionParameterSettings(hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = Measurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AllMeasurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalDefaultExpressionParameter getCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    public void setCenterX(OptionalDefaultExpressionParameter centerX) {
        this.centerX = centerX;
    }

    @JIPipeDocumentation(name = "Center (Y)", description = "If true, override the ROI's center Y location")
    @JIPipeParameter("center-y")
    @ExpressionParameterSettings(hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = Measurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AllMeasurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalDefaultExpressionParameter getCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    public void setCenterY(OptionalDefaultExpressionParameter centerY) {
        this.centerY = centerY;
    }

    @JIPipeDocumentation(name = "Center (Z)", description = "If true, override the ROI's center Z location")
    @JIPipeParameter("center-z")
    @ExpressionParameterSettings(hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = Measurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AllMeasurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalDefaultExpressionParameter getCenterZ() {
        return centerZ;
    }

    @JIPipeParameter("center-z")
    public void setCenterZ(OptionalDefaultExpressionParameter centerZ) {
        this.centerZ = centerZ;
    }

    @JIPipeDocumentation(name = "Channel", description = "If true, override the ROI's channel location (0 = all channels)")
    @JIPipeParameter("channel-location")
    @ExpressionParameterSettings(hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = Measurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AllMeasurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalDefaultExpressionParameter getChannelLocation() {
        return channelLocation;
    }

    @JIPipeParameter("channel-location")
    public void setChannelLocation(OptionalDefaultExpressionParameter channelLocation) {
        this.channelLocation = channelLocation;
    }

    @JIPipeDocumentation(name = "Frame", description = "If true, override the ROI's frame location (0 = all frames)")
    @JIPipeParameter("frame-location")
    @ExpressionParameterSettings(hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = Measurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AllMeasurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalDefaultExpressionParameter getFrameLocation() {
        return frameLocation;
    }

    @JIPipeParameter("frame-location")
    public void setFrameLocation(OptionalDefaultExpressionParameter frameLocation) {
        this.frameLocation = frameLocation;
    }

    @JIPipeDocumentation(name = "Fill color", description = "If true, override the ROI's fill color (you can return a color or a HEX string)")
    @JIPipeParameter("fill-color")
    @ExpressionParameterSettings(hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = Measurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AllMeasurement3DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalDefaultExpressionParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalDefaultExpressionParameter fillColor) {
        this.fillColor = fillColor;
    }

    @JIPipeDocumentation(name = "Metadata", description = "Allows to set/override additional metadata items")
    @JIPipeParameter("metadata-entries")
    public ParameterCollectionList getMetadataEntries() {
        return metadataEntries;
    }

    @JIPipeParameter("metadata-entries")
    public void setMetadataEntries(ParameterCollectionList metadataEntries) {
        this.metadataEntries = metadataEntries;
    }

    @JIPipeDocumentation(name = "Measurements", description = "The measurements to calculate.")
    @JIPipeParameter(value = "measurements", important = true)
    public Measurements3DSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(Measurements3DSetParameter measurements) {
        this.measurements = measurements;
    }

    @JIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }

    public static class MetadataEntry extends AbstractJIPipeParameterCollection {
        private String metadataName;

        private OptionalDefaultExpressionParameter metadataValue = new OptionalDefaultExpressionParameter(false, "metadata.?");

        public MetadataEntry() {
        }

        public MetadataEntry(MetadataEntry other) {
            this.metadataName = other.metadataName;
            this.metadataValue = new OptionalDefaultExpressionParameter(other.metadataValue);
        }

        @JIPipeDocumentation(name = "Metadata name/key")
        @JIPipeParameter("metadata-name")
        public String getMetadataName() {
            return metadataName;
        }

        @JIPipeParameter("metadata-name")
        public void setMetadataName(String metadataName) {
            this.metadataName = metadataName;
        }

        @JIPipeDocumentation(name = "Metadata value")
        @JIPipeParameter("metadata-value")
        @ExpressionParameterSettings(hint = "per ROI")
        @ExpressionParameterSettingsVariable(fromClass = Measurement3DExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
        @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
        @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
        public OptionalDefaultExpressionParameter getMetadataValue() {
            return metadataValue;
        }

        @JIPipeParameter("metadata-value")
        public void setMetadataValue(OptionalDefaultExpressionParameter metadataValue) {
            this.metadataValue = metadataValue;
        }
    }
}
