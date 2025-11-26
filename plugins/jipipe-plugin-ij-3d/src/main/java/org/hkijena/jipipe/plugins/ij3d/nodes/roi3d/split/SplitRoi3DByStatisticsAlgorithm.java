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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.split;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.ij3d.datatypes.IJ3DROI;
import org.hkijena.jipipe.plugins.ij3d.datatypes.IJ3DROIListData;
import org.hkijena.jipipe.plugins.ij3d.utils.ROI3DMeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.utils.ROI3DMeasurementSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Split IJ3D ROI lists by statistics", description = "Splits the incoming 3D ROI lists by a classifier value that is calculated based on statistics.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Measure")
@AddJIPipeInputSlot(value = IJ3DROIListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true, description = "Optional image that is the basis for the measurements. If not set, all affected measurements are set to NaN.")
@AddJIPipeOutputSlot(value = IJ3DROIListData.class, name = "Split ROI", create = true)
@MarkNodeAsUnstable
public class SplitRoi3DByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean measureInPhysicalUnits = true;
    private ROI3DMeasurementSetParameter measurements = new ROI3DMeasurementSetParameter();
    private JIPipeExpressionParameter classifier = new JIPipeExpressionParameter("index");
    private OptionalTextAnnotationNameParameter classifierAnnotation = new OptionalTextAnnotationNameParameter("Classifier", true);

    public SplitRoi3DByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SplitRoi3DByStatisticsAlgorithm(SplitRoi3DByStatisticsAlgorithm other) {
        super(other);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.measurements = new ROI3DMeasurementSetParameter(other.measurements);
        this.classifier = new JIPipeExpressionParameter(other.classifier);
        this.classifierAnnotation = new OptionalTextAnnotationNameParameter(other.classifierAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        IJ3DROIListData inputRois = iterationStep.getInputData("ROI", IJ3DROIListData.class, progressInfo);
        ImagePlusData inputReference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        // Create variables
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap(iterationStep);

        // Obtain statistics
        ResultsTableData statistics = inputRois.measure(IJ3DUtils.wrapImage(inputReference), measurements.getNativeValue(), measureInPhysicalUnits, "", progressInfo.resolve("Measuring ROIs"));

        // Write statistics into variables
        variableSet.set("num_roi", inputRois.size());

        // Apply filter
        Map<String, IJ3DROIListData> groups = new HashMap<>();

        for (int row = 0; row < statistics.getRowCount(); row++) {
            IJ3DROI roi = inputRois.get(row);

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

            String group = classifier.evaluateToString(variableSet);
            IJ3DROIListData target = groups.getOrDefault(group, null);
            if (target == null) {
                target = new IJ3DROIListData();
                groups.put(group, target);
            }
            target.add(roi);
        }

          for (Map.Entry<String, IJ3DROIListData> entry : groups.entrySet()) {
            List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
            classifierAnnotation.addAnnotationIfEnabled(annotationList, entry.getKey());
            iterationStep.addOutputData(getFirstOutputSlot(), entry.getValue(), annotationList, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements to generate")
    @JIPipeParameter("measurements")
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

    @SetJIPipeDocumentation(name = "Classifier", description = "An expression that returns a string that is used to classify the ROIs")
    @JIPipeParameter("classifier")
    @AddJIPipeExpressionParameterVariable(fromClass = ROI3DMeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "ROI number", key = "num_roi", description = "The number of ROI")
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public JIPipeExpressionParameter getClassifier() {
        return classifier;
    }

    @JIPipeParameter("classifier")
    public void setClassifier(JIPipeExpressionParameter classifier) {
        this.classifier = classifier;
    }

    @SetJIPipeDocumentation(name = "Annotate with classifier", description = "If enabled, add an annotation containing the classifier to each output")
    @JIPipeParameter("classifier-annotation")
    public OptionalTextAnnotationNameParameter getClassifierAnnotation() {
        return classifierAnnotation;
    }

    @JIPipeParameter("classifier-annotation")
    public void setClassifierAnnotation(OptionalTextAnnotationNameParameter classifierAnnotation) {
        this.classifierAnnotation = classifierAnnotation;
    }
}
