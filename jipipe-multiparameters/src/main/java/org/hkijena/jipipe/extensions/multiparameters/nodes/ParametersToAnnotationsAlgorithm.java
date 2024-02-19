package org.hkijena.jipipe.extensions.multiparameters.nodes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterlessSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Parameters to annotations", description = "Converts parameter data into annotations. " +
        "Parameters are converted into JSON data.")
@DefineJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For parameters")
@AddJIPipeInputSlot(value = ParametersData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ParametersData.class, slotName = "Output", create = true)
public class ParametersToAnnotationsAlgorithm extends JIPipeParameterlessSimpleIteratingAlgorithm {

    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public ParametersToAnnotationsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ParametersToAnnotationsAlgorithm(ParametersToAnnotationsAlgorithm other) {
        super(other);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ParametersData data = iterationStep.getInputData(getFirstInputSlot(), ParametersData.class, progressInfo);
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.getParameterData().entrySet()) {
            annotationList.add(new JIPipeTextAnnotation(entry.getKey(), JsonUtils.toJsonString(entry.getValue())));
        }
        iterationStep.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeStrategy, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Merge existing annotations", description = "Determines how existing annotations are merged")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
