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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.split;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.Roi2DPropertiesExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageJMeasurementsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageJMeasurementsSetParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SetJIPipeDocumentation(name = "Split 2D ROI lists by statistics", description = "Splits the incoming 2D ROI lists by a classifier value that is calculated based on statistics.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Split")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true, description = "Optional image that is the basis for the measurements. If not set, an empty image is generated.")
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Split ROI", create = true)
public class SplitRoi2DByStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageJMeasurementsSetParameter measurements = new ImageJMeasurementsSetParameter();
    private boolean measureInPhysicalUnits = true;
    private JIPipeExpressionParameter classifier = new JIPipeExpressionParameter("index");
    private OptionalTextAnnotationNameParameter classifierAnnotation = new OptionalTextAnnotationNameParameter("Classifier", true);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public SplitRoi2DByStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public SplitRoi2DByStatisticsAlgorithm(SplitRoi2DByStatisticsAlgorithm other) {
        super(other);
        this.measurements = new ImageJMeasurementsSetParameter(other.measurements);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.classifier = new JIPipeExpressionParameter(other.classifier);
        this.classifierAnnotation = new OptionalTextAnnotationNameParameter(other.classifierAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData roiList = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
        ImagePlus reference = getReferenceImage(iterationStep, progressInfo);
        if (roiList.isEmpty()) {
            iterationStep.addOutputData(getFirstOutputSlot(), new ResultsTableData(), progressInfo);
            return;
        }

        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap(iterationStep);
        ResultsTableData measured = roiList.measure(reference, measurements, true, measureInPhysicalUnits);
        Map<String, ROI2DListData> groups = new HashMap<>();

        for (int i = 0; i < roiList.size(); i++) {
            Roi roi = roiList.get(i);
            Roi2DPropertiesExpressionVariablesInfo.putVariables(roi, roiList.size(), measured, i, variablesMap);
            String group = classifier.evaluateToString(variablesMap);
            ROI2DListData target = groups.getOrDefault(group, null);
            if(target == null) {
                target = new ROI2DListData();
                groups.put(group, target);
            }
            target.add(roi);
        }

        for (Map.Entry<String, ROI2DListData> entry : groups.entrySet()) {
            List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
            classifierAnnotation.addAnnotationIfEnabled(annotationList, entry.getKey());
            iterationStep.addOutputData(getFirstOutputSlot(), entry.getValue(), annotationList, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    private ImagePlus getReferenceImage(JIPipeSingleIterationStep iterationStep, JIPipeProgressInfo progressInfo) {
        ImagePlus reference = null;
        {
            ImagePlusData data = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);
            if (data != null) {
                reference = data.getDuplicateImage();
            }
        }
        return reference;
    }

    @SetJIPipeDocumentation(name = "Extracted measurements", description = "Please select which measurements should be extracted. " +
            "Each measurement will be assigned to one or multiple output table columns.<br/><br/>" + ImageJMeasurementsSetParameter.ALL_DESCRIPTIONS)
    @JIPipeParameter(value = "measurements", important = true)
    public ImageJMeasurementsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageJMeasurementsSetParameter measurements) {
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
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = ImageJMeasurementsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = Roi2DPropertiesExpressionVariablesInfo.class)
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
