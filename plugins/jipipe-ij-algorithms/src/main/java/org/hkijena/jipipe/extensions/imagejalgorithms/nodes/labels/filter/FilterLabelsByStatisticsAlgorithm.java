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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels.filter;

import com.google.common.primitives.Doubles;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.AllMeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Filter labels by statistics 2D", description = "Filters the ROI list elements via statistics. Statistics are extracted over an image (optional). " +
        "If no image is supplied, the label itself will be used as the image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Labels\nFilter")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Image", create = true, optional = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nLabel Images")
public class FilterLabelsByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter();
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private boolean measureInPhysicalUnits = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public FilterLabelsByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public FilterLabelsByStatisticsAlgorithm(FilterLabelsByStatisticsAlgorithm other) {
        super(other);
        this.filters = new JIPipeExpressionParameter(other.filters);
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus labels = iterationStep.getInputData("Labels", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        ImagePlus reference;
        if (iterationStep.getInputRow("Image") >= 0) {
            reference = iterationStep.getInputData("Image", ImagePlusGreyscaleData.class, progressInfo).getImage();
        } else {
            reference = labels;
        }

        Calibration calibration = measureInPhysicalUnits ? labels.getCalibration() : null;

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        TIntSet labelsToKeep = new TIntHashSet();
        ImageJUtils.forEachIndexedZCTSlice(labels, (labelProcessor, index) -> {

            // Measure
            int z = Math.min(index.getZ(), labels.getNSlices() - 1);
            int c = Math.min(index.getC(), labels.getNChannels() - 1);
            int t = Math.min(index.getT(), labels.getNFrames() - 1);
            ImageProcessor referenceProcessor = ImageJUtils.getSliceZero(reference, c, z, t);
            final ResultsTableData statistics = ImageJAlgorithmUtils.measureLabels(labelProcessor, referenceProcessor, this.measurements, index, calibration, progressInfo);

            // Find labels to keep
            labelsToKeep.clear();
            variables.clear();
            for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
                variables.set(annotation.getName(), annotation.getValue());
            }
            variables.putCustomVariables(getDefaultCustomExpressionVariables());

            // Write statistics into variables
            for (int col = 0; col < statistics.getColumnCount(); col++) {
                TableColumn column = statistics.getColumnReference(col);
                if (column.isNumeric()) {
                    variables.set("all." + column.getLabel(), new ArrayList<>(Doubles.asList(column.getDataAsDouble(column.getRows()))));
                } else {
                    variables.set("all." + column.getLabel(), new ArrayList<>(Arrays.asList(column.getDataAsString(column.getRows()))));
                }
            }

            for (int row = 0; row < statistics.getRowCount(); row++) {
                for (int col = 0; col < statistics.getColumnCount(); col++) {
                    variables.set(statistics.getColumnName(col), statistics.getValueAt(row, col));
                }
                if (filters.test(variables)) {
                    labelsToKeep.add((int) statistics.getValueAsDouble(row, "label_id"));
                }
            }

            // Apply
            ImageJAlgorithmUtils.removeLabelsExcept(labelProcessor, labelsToKeep.toArray());

        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(labels), progressInfo);
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @JIPipeParameter(value = "filter", important = true)
    @SetJIPipeDocumentation(name = "Filter", description = "Filtering expression. This is applied per label. " +
            "Click the 'Edit' button to see all available variables you can test for (note: requires from you to enable the corresponding measurement!)." +
            "An example for an expression would be 'Area > 200 AND Mean > 10'." +
            "Annotations are available as variables.")
    @JIPipeExpressionParameterSettings(variableSource = MeasurementExpressionParameterVariablesInfo.class, hint = "per label")
    @JIPipeExpressionParameterVariable(fromClass = AllMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public JIPipeExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filter")
    public void setFilters(JIPipeExpressionParameter filters) {
        this.filters = filters;
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements to calculate.")
    @JIPipeParameter(value = "measurements", important = true)
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
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
}
