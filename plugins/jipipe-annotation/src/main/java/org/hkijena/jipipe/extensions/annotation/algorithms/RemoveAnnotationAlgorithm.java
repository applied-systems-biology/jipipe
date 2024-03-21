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

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.AnnotationQueryExpression;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.List;

/**
 * Removes a specified annotation
 */
@SetJIPipeDocumentation(name = "Remove annotation", description = "Removes annotations by name or value")
@ConfigureJIPipeNode(menuPath = "Remove", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
public class RemoveAnnotationAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private AnnotationQueryExpression annotationExpression = new AnnotationQueryExpression("key == \"#Dataset\"");

    /**
     * @param info algorithm info
     */
    public RemoveAnnotationAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public RemoveAnnotationAlgorithm(RemoveAnnotationAlgorithm other) {
        super(other);
        this.annotationExpression = new AnnotationQueryExpression(other.annotationExpression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<JIPipeTextAnnotation> matches = annotationExpression.queryAll(iterationStep.getMergedTextAnnotations().values());
        for (JIPipeTextAnnotation match : matches) {
            iterationStep.removeMergedTextAnnotation(match.getName());
        }
        iterationStep.addOutputData(getFirstOutputSlot(), iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Removed annotations", description = "Annotations that match any of the filters are removed.")
    @JIPipeParameter("annotation-type")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public AnnotationQueryExpression getAnnotationExpression() {
        return annotationExpression;
    }

    @JIPipeParameter("annotation-type")
    public void setAnnotationExpression(AnnotationQueryExpression annotationExpression) {
        this.annotationExpression = annotationExpression;

    }
}
