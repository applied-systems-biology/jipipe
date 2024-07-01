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
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Split into individual 3D ROI lists", description = "Splits the 3D ROI lists into individual 3D ROI lists.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Split")
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, name = "Output", create = true)
public class ExplodeRoi3DListAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private OptionalStringParameter generatedAnnotation = new OptionalStringParameter();
    private JIPipeExpressionParameter annotationValue = new JIPipeExpressionParameter("\"index=\" + index + \";name=\" + name");

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ExplodeRoi3DListAlgorithm(JIPipeNodeInfo info) {
        super(info);
        generatedAnnotation.setContent("ROI index");
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ExplodeRoi3DListAlgorithm(ExplodeRoi3DListAlgorithm other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
        this.annotationValue = new JIPipeExpressionParameter(other.annotationValue);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData data = iterationStep.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap()
                .putAnnotations(iterationStep.getMergedTextAnnotations())
                .putCustomVariables(getDefaultCustomExpressionVariables());

        for (int i = 0; i < data.size(); i++) {
            ROI3D roi = data.get(i);
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            if (generatedAnnotation.isEnabled() && !StringUtils.isNullOrEmpty(generatedAnnotation.getContent())) {

                // Make metadata accessible
                Map<String, String> roiProperties = roi.getMetadata();
                variables.set("metadata", roiProperties);
                for (Map.Entry<String, String> entry : roiProperties.entrySet()) {
                    variables.set("metadata." + entry.getKey(), entry.getValue());
                }

                variables.set("index", i);
                variables.set("num_roi", data.size());
                variables.set("name", StringUtils.orElse(roi.getObject3D().getName(), "unnamed"));

                annotations.add(new JIPipeTextAnnotation(generatedAnnotation.getContent(), annotationValue.evaluateToString(variables)));
            }
            ROI3DListData output = new ROI3DListData();
            output.add(roi);
            iterationStep.addOutputData(getFirstOutputSlot(), output, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "ROI index annotation", description = "Optional. Annotation that is added to each individual ROI list. Contains the value index=[index];name=[name].")
    @JIPipeParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalStringParameter getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @JIPipeParameter("generated-annotation")
    public void setGeneratedAnnotation(OptionalStringParameter generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    @AddJIPipeExpressionParameterVariable(key = "name", name = "ROI name", description = "The name of the ROI")
    @AddJIPipeExpressionParameterVariable(key = "index", name = "ROI index", description = "The index of the ROI")
    @AddJIPipeExpressionParameterVariable(key = "num_roi", name = "Number of ROI", description = "The number of ROI in the list")
    public JIPipeExpressionParameter getAnnotationValue() {
        return annotationValue;
    }

    @JIPipeParameter("roi-name")
    public void setAnnotationValue(JIPipeExpressionParameter annotationValue) {
        this.annotationValue = annotationValue;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
