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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.labels;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils2;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Filter labels by statistics 2D", description = "Filters the ROI list elements via statistics. Statistics are extracted over an image (optional). " +
        "If no image is supplied, the label itself will be used as the image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Labels\nFilter")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Image", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", autoCreate = true)
public class FilterLabelsByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private DefaultExpressionParameter filters = new DefaultExpressionParameter();
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();

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
        this.filters = new DefaultExpressionParameter(other.filters);
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus labels = dataBatch.getInputData("Labels", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        ImagePlus reference;
        if(dataBatch.getInputRow("Image") >= 0) {
            reference = dataBatch.getInputData("Image", ImagePlusGreyscaleData.class, progressInfo).getImage();
        }
        else {
            reference = labels;
        }

        ExpressionVariables variables = new ExpressionVariables();
        TIntSet labelsToKeep = new TIntHashSet();
        ImageJUtils.forEachIndexedZCTSlice(labels, (labelProcessor, index) -> {

            // Measure
            int z = Math.min(index.getZ(), labels.getNSlices() - 1);
            int c = Math.min(index.getC(), labels.getNChannels() - 1);
            int t = Math.min(index.getT(), labels.getNFrames() - 1);
            ImageProcessor referenceProcessor = ImageJUtils.getSliceZero(reference, c, z, t);
            ResultsTableData forRoi = ImageJUtils2.measureLabels(labelProcessor, referenceProcessor, this.measurements, index, progressInfo);

            // Find labels to keep
            labelsToKeep.clear();
            variables.clear();
            for (int row = 0; row < forRoi.getRowCount(); row++) {
                for (int col = 0; col < forRoi.getColumnCount(); col++) {
                    variables.set(forRoi.getColumnName(col), forRoi.getValueAt(row, col));
                }
                if(filters.test(variables)) {
                    labelsToKeep.add((int) forRoi.getValueAsDouble(row, "label_id"));
                }
            }

            // Apply
            ImageJUtils2.removeLabelsExcept(labelProcessor, labelsToKeep.toArray());

        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(labels), progressInfo);
    }

    @JIPipeParameter(value = "filter", important = true)
    @JIPipeDocumentation(name = "Filter", description = "Filtering expression. This is applied per label. " +
            "Click the 'Edit' button to see all available variables you can test for (note: requires from you to enable the corresponding measurement!)." +
            "An example for an expression would be 'Area > 200 AND Mean > 10'")
    @ExpressionParameterSettings(variableSource = MeasurementExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filter")
    public void setFilters(DefaultExpressionParameter filters) {
        this.filters = filters;
    }

    @JIPipeDocumentation(name = "Measurements", description = "The measurements to calculate.")
    @JIPipeParameter("measurements")
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            setFilters(new DefaultExpressionParameter("Area > 100"));
        }
    }
}
