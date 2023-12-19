/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.properties;

import com.google.common.primitives.Doubles;
import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.AllMeasurementExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Set ROI metadata by statistics (expression)", description = "Sets ROI metadata by statistics.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Metadata")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class SetRoiMetadataByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private final RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode(RoiStatisticsAlgorithm.class);
    private final CustomExpressionVariablesParameter customVariables;
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
        this.customVariables = new CustomExpressionVariablesParameter(this);
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
        this.customVariables = new CustomExpressionVariablesParameter(other.customVariables, this);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.clearBeforeWrite = other.clearBeforeWrite;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.setMeasurements(measurements);
        roiStatisticsAlgorithm.setMeasureInPhysicalUnits(measureInPhysicalUnits);

        // Continue with run
        super.run(progressInfo);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ROIListData rois = new ROIListData(iterationStep.getInputData("ROI", ROIListData.class, progressInfo));
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        // Create variables
        ExpressionVariables variableSet = new ExpressionVariables();
        variableSet.putAnnotations(iterationStep.getMergedTextAnnotations());
        customVariables.writeToVariables(variableSet, true, "custom.", true, "custom");

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData();
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(rois, progressInfo);
        if (inputReference != null) {
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(inputReference, progressInfo);
        }
        roiStatisticsAlgorithm.run(progressInfo);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
        roiStatisticsAlgorithm.clearSlotData();

        // Write statistics into variables
        for (int col = 0; col < statistics.getColumnCount(); col++) {
            TableColumn column = statistics.getColumnReference(col);
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

    @JIPipeDocumentation(name = "Clear properties before write", description = "If enabled, all existing ROI properties are deleted before writing the new properties")
    @JIPipeParameter("clear-before-write")
    public boolean isClearBeforeWrite() {
        return clearBeforeWrite;
    }

    @JIPipeParameter("clear-before-write")
    public void setClearBeforeWrite(boolean clearBeforeWrite) {
        this.clearBeforeWrite = clearBeforeWrite;
    }

    @JIPipeDocumentation(name = "Generated metadata", description = "Each entry contains an expression that is applied for each ROI. The generated value is written into the metadata key. <strong>Please note that ImageJ ROI metadata are subject to some limitations. For example, keys cannot have space characters, equal signs, and colons.</strong>")
    @JIPipeParameter(value = "metadata-generators", important = true)
    @ParameterCollectionListTemplate(MetadataProperty.class)
    public ParameterCollectionList getMetadataGenerators() {
        return metadataGenerators;
    }

    @JIPipeParameter("metadata-generators")
    public void setMetadataGenerators(ParameterCollectionList metadataGenerators) {
        this.metadataGenerators = metadataGenerators;
    }

    @JIPipeDocumentation(name = "Measurements", description = "The measurements to calculate.")
    @JIPipeParameter(value = "measurements", important = true)
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @JIPipeDocumentation(name = "Custom variables", description = "Here you can add parameters that will be included into the expressions as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(custom, \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomVariables() {
        return customVariables;
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
        @JIPipeDocumentation(name = "Value", description = "Expression that generates the value. This is applied per ROI. " +
                "Click the 'f' button to see all available variables you can test for (note: requires from you to enable the corresponding measurement!)." +
                "An example for an expression would be 'Area'." +
                "Annotations are available as variables.")
        @ExpressionParameterSettings(variableSource = MeasurementExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(fromClass = AllMeasurementExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the existing ROI metadata/properties (string keys, string values)")
        @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "Existing ROI metadata/properties accessible via their string keys")
        public JIPipeExpressionParameter getValue() {
            return value;
        }

        @JIPipeParameter("value")
        public void setValue(JIPipeExpressionParameter value) {
            this.value = value;
        }

        @JIPipeDocumentation(name = "Key", description = "The key")
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
