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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.properties;

import com.google.common.primitives.Doubles;
import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.AllMeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.MeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Set 2D ROI metadata by statistics (expression)", description = "Sets ROI metadata by statistics.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Metadata")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class SetRoiMetadataByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private final RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode(RoiStatisticsAlgorithm.class);
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private ParameterCollectionList metadataGenerators = ParameterCollectionList.containingCollection(MetadataProperty.class);
    private boolean measureInPhysicalUnits = true;
    private boolean clearBeforeWrite = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public SetRoiMetadataByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public SetRoiMetadataByStatisticsAlgorithm(SetRoiMetadataByStatisticsAlgorithm other) {
        super(other);
        this.metadataGenerators = new ParameterCollectionList(other.metadataGenerators);
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.clearBeforeWrite = other.clearBeforeWrite;
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.setMeasurements(measurements);
        roiStatisticsAlgorithm.setMeasureInPhysicalUnits(measureInPhysicalUnits);

        // Continue with run
        super.run(runContext, progressInfo);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData rois = new ROI2DListData(iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo));
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        // Create variables
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        variableSet.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variableSet);

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData(false, progressInfo);
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(rois, progressInfo);
        if (inputReference != null) {
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(inputReference, progressInfo);
        }
        roiStatisticsAlgorithm.run(runContext, progressInfo);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
        roiStatisticsAlgorithm.clearSlotData(false, progressInfo);

        // Write statistics into variables
        for (int col = 0; col < statistics.getColumnCount(); col++) {
            TableColumnData column = statistics.getColumnReference(col);
            if (column.isNumeric()) {
                variableSet.set("all." + column.getLabel(), new ArrayList<>(Doubles.asList(column.getDataAsDouble(column.getRows()))));
            } else {
                variableSet.set("all." + column.getLabel(), new ArrayList<>(Arrays.asList(column.getDataAsString(column.getRows()))));
            }
        }

        // Apply filter
        for (int row = 0; row < statistics.getRowCount(); row++) {
            Roi roi = rois.get(row);
            Map<String, String> roiProperties = ImageJUtils.getRoiProperties(roi);
            variableSet.set("metadata", roiProperties);
            for (Map.Entry<String, String> entry : roiProperties.entrySet()) {
                variableSet.set("metadata." + entry.getKey(), entry.getValue());
            }
            for (int col = 0; col < statistics.getColumnCount(); col++) {
                variableSet.set(statistics.getColumnName(col), statistics.getValueAt(row, col));
            }
            if (clearBeforeWrite) {
                roiProperties.clear();
            }
            for (MetadataProperty property : metadataGenerators.mapToCollection(MetadataProperty.class)) {
                String value = property.value.evaluateToString(variableSet);
                roiProperties.put(property.key, value);
            }
            ImageJUtils.setRoiProperties(roi, roiProperties);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Clear properties before write", description = "If enabled, all existing ROI properties are deleted before writing the new properties")
    @JIPipeParameter("clear-before-write")
    public boolean isClearBeforeWrite() {
        return clearBeforeWrite;
    }

    @JIPipeParameter("clear-before-write")
    public void setClearBeforeWrite(boolean clearBeforeWrite) {
        this.clearBeforeWrite = clearBeforeWrite;
    }

    @SetJIPipeDocumentation(name = "Generated metadata", description = "Each entry contains an expression that is applied for each ROI. The generated value is written into the metadata key. <strong>Please note that ImageJ ROI metadata are subject to some limitations. For example, keys cannot have space characters, equal signs, and colons.</strong>")
    @JIPipeParameter(value = "metadata-generators", important = true)
    @ParameterCollectionListTemplate(MetadataProperty.class)
    public ParameterCollectionList getMetadataGenerators() {
        return metadataGenerators;
    }

    @JIPipeParameter("metadata-generators")
    public void setMetadataGenerators(ParameterCollectionList metadataGenerators) {
        this.metadataGenerators = metadataGenerators;
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements to calculate." + "<br/><br/>" + ImageStatisticsSetParameter.ALL_DESCRIPTIONS)
    @JIPipeParameter(value = "measurements", important = true)
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
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

    public static class MetadataProperty extends AbstractJIPipeParameterCollection {

        private String key;
        private JIPipeExpressionParameter value = new JIPipeExpressionParameter();

        public MetadataProperty() {

        }

        public MetadataProperty(MetadataProperty other) {
            this.key = other.key;
            this.value = new JIPipeExpressionParameter(other.value);
        }

        @JIPipeParameter(value = "value")
        @SetJIPipeDocumentation(name = "Value", description = "Expression that generates the value. This is applied per ROI. " +
                "Click the 'f' button to see all available variables you can test for (note: requires from you to enable the corresponding measurement!)." +
                "An example for an expression would be 'Area'." +
                "Annotations are available as variables.")
        @JIPipeExpressionParameterSettings(variableSource = MeasurementExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = AllMeasurementExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @AddJIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the existing ROI metadata/properties (string keys, string values)")
        @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Existing ROI metadata/properties accessible via their string keys")
        public JIPipeExpressionParameter getValue() {
            return value;
        }

        @JIPipeParameter("value")
        public void setValue(JIPipeExpressionParameter value) {
            this.value = value;
        }

        @SetJIPipeDocumentation(name = "Key", description = "The key")
        @JIPipeParameter("key")
        public String getKey() {
            return key;
        }

        @JIPipeParameter("key")
        public void setKey(String key) {
            this.key = key;
        }
    }
}
