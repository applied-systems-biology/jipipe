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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.modify;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.ij3d.utils.AllROI3DMeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.utils.ROI3DMeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.utils.ROI3DMeasurementSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Change 3D ROI properties from expressions", description = "Sets the properties of all 3D ROI via expressions")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", create = true)
public class ChangeRoi3DPropertiesFromExpressionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private OptionalJIPipeExpressionParameter roiName = new OptionalJIPipeExpressionParameter(false, "Name");
    private OptionalJIPipeExpressionParameter roiComment = new OptionalJIPipeExpressionParameter(false, "Comment");
    private OptionalJIPipeExpressionParameter centerX = new OptionalJIPipeExpressionParameter(false, "CenterX");
    private OptionalJIPipeExpressionParameter centerY = new OptionalJIPipeExpressionParameter(false, "CenterY");
    private OptionalJIPipeExpressionParameter centerZ = new OptionalJIPipeExpressionParameter(false, "CenterZ");
    private OptionalJIPipeExpressionParameter channelLocation = new OptionalJIPipeExpressionParameter(false, "Channel");
    private OptionalJIPipeExpressionParameter frameLocation = new OptionalJIPipeExpressionParameter(false, "Frame");
    private OptionalJIPipeExpressionParameter fillColor = new OptionalJIPipeExpressionParameter(false, "FillColor");
    private ParameterCollectionList metadataEntries = ParameterCollectionList.containingCollection(MetadataEntry.class);
    private ROI3DMeasurementSetParameter measurements = new ROI3DMeasurementSetParameter();
    private boolean measureInPhysicalUnits = true;

    public ChangeRoi3DPropertiesFromExpressionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ChangeRoi3DPropertiesFromExpressionsAlgorithm(ChangeRoi3DPropertiesFromExpressionsAlgorithm other) {
        super(other);
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData outputROI = new ROI3DListData(iterationStep.getInputData("Input", ROI3DListData.class, progressInfo));
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        // Create variables
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap()
                .putAnnotations(iterationStep.getMergedTextAnnotations())
                .putCustomVariables(getDefaultCustomExpressionVariables());

        // Obtain statistics
        ResultsTableData statistics = outputROI.measure(IJ3DUtils.wrapImage(inputReference), measurements.getNativeValue(), measureInPhysicalUnits, "", progressInfo.resolve("Measuring ROIs"));

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
                if (centerX.isEnabled()) {
                    x = centerX.getContent().evaluateToDouble(variableSet);
                }
                if (centerY.isEnabled()) {
                    y = centerY.getContent().evaluateToDouble(variableSet);
                }
                if (centerZ.isEnabled()) {
                    z = centerZ.getContent().evaluateToDouble(variableSet);
                }
                roi.getObject3D().setNewCenter(x, y, z);
            }
            if (channelLocation.isEnabled()) {
                roi.setChannel(channelLocation.getContent().evaluateToInteger(variableSet));
            }
            if (frameLocation.isEnabled()) {
                roi.setFrame(frameLocation.getContent().evaluateToInteger(variableSet));
            }
            if (fillColor.isEnabled()) {
                roi.setFillColor(fillColor.getContent().evaluateToColor(variableSet));
            }
            for (MetadataEntry entry : evaluatedMetadataEntries) {
                if (entry.getMetadataValue().isEnabled()) {
                    roi.getMetadata().put(entry.getMetadataName(),
                            entry.getMetadataValue().getContent().evaluateToString(variableSet));
                }
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputROI, progressInfo);
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "If true, override the ROI name")
    @JIPipeParameter("roi-name")
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @JIPipeExpressionParameterVariable(fromClass = ROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = AllROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalJIPipeExpressionParameter roiName) {
        this.roiName = roiName;
    }

    @SetJIPipeDocumentation(name = "ROI comment", description = "If true, override the ROI's comment field")
    @JIPipeParameter("roi-comment")
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @JIPipeExpressionParameterVariable(fromClass = ROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = AllROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getRoiComment() {
        return roiComment;
    }

    @JIPipeParameter("roi-comment")
    public void setRoiComment(OptionalJIPipeExpressionParameter roiComment) {
        this.roiComment = roiComment;
    }

    @SetJIPipeDocumentation(name = "Center (X)", description = "If true, override the ROI's center X location")
    @JIPipeParameter("center-x")
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @JIPipeExpressionParameterVariable(fromClass = ROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = AllROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    public void setCenterX(OptionalJIPipeExpressionParameter centerX) {
        this.centerX = centerX;
    }

    @SetJIPipeDocumentation(name = "Center (Y)", description = "If true, override the ROI's center Y location")
    @JIPipeParameter("center-y")
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @JIPipeExpressionParameterVariable(fromClass = ROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = AllROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    public void setCenterY(OptionalJIPipeExpressionParameter centerY) {
        this.centerY = centerY;
    }

    @SetJIPipeDocumentation(name = "Center (Z)", description = "If true, override the ROI's center Z location")
    @JIPipeParameter("center-z")
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @JIPipeExpressionParameterVariable(fromClass = ROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = AllROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getCenterZ() {
        return centerZ;
    }

    @JIPipeParameter("center-z")
    public void setCenterZ(OptionalJIPipeExpressionParameter centerZ) {
        this.centerZ = centerZ;
    }

    @SetJIPipeDocumentation(name = "Channel", description = "If true, override the ROI's channel location (0 = all channels)")
    @JIPipeParameter("channel-location")
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @JIPipeExpressionParameterVariable(fromClass = ROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = AllROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getChannelLocation() {
        return channelLocation;
    }

    @JIPipeParameter("channel-location")
    public void setChannelLocation(OptionalJIPipeExpressionParameter channelLocation) {
        this.channelLocation = channelLocation;
    }

    @SetJIPipeDocumentation(name = "Frame", description = "If true, override the ROI's frame location (0 = all frames)")
    @JIPipeParameter("frame-location")
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @JIPipeExpressionParameterVariable(fromClass = ROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = AllROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getFrameLocation() {
        return frameLocation;
    }

    @JIPipeParameter("frame-location")
    public void setFrameLocation(OptionalJIPipeExpressionParameter frameLocation) {
        this.frameLocation = frameLocation;
    }

    @SetJIPipeDocumentation(name = "Fill color", description = "If true, override the ROI's fill color (you can return a color or a HEX string)")
    @JIPipeParameter("fill-color")
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @JIPipeExpressionParameterVariable(fromClass = ROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = AllROI3DMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public OptionalJIPipeExpressionParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalJIPipeExpressionParameter fillColor) {
        this.fillColor = fillColor;
    }

    @SetJIPipeDocumentation(name = "Metadata", description = "Allows to set/override additional metadata items")
    @JIPipeParameter("metadata-entries")
    @ParameterCollectionListTemplate(MetadataEntry.class)
    public ParameterCollectionList getMetadataEntries() {
        return metadataEntries;
    }

    @JIPipeParameter("metadata-entries")
    public void setMetadataEntries(ParameterCollectionList metadataEntries) {
        this.metadataEntries = metadataEntries;
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements to calculate.")
    @JIPipeParameter(value = "measurements", important = true)
    public ROI3DMeasurementSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ROI3DMeasurementSetParameter measurements) {
        this.measurements = measurements;
    }

    @SetJIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
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

        private OptionalJIPipeExpressionParameter metadataValue = new OptionalJIPipeExpressionParameter(false, "metadata.?");

        public MetadataEntry() {
        }

        public MetadataEntry(MetadataEntry other) {
            this.metadataName = other.metadataName;
            this.metadataValue = new OptionalJIPipeExpressionParameter(other.metadataValue);
        }

        @SetJIPipeDocumentation(name = "Metadata name/key")
        @JIPipeParameter("metadata-name")
        public String getMetadataName() {
            return metadataName;
        }

        @JIPipeParameter("metadata-name")
        public void setMetadataName(String metadataName) {
            this.metadataName = metadataName;
        }

        @SetJIPipeDocumentation(name = "Metadata value")
        @JIPipeParameter("metadata-value")
        @JIPipeExpressionParameterSettings(hint = "per ROI")
        @JIPipeExpressionParameterVariable(fromClass = ROI3DMeasurementExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
        @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
        @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
        public OptionalJIPipeExpressionParameter getMetadataValue() {
            return metadataValue;
        }

        @JIPipeParameter("metadata-value")
        public void setMetadataValue(OptionalJIPipeExpressionParameter metadataValue) {
            this.metadataValue = metadataValue;
        }
    }
}
