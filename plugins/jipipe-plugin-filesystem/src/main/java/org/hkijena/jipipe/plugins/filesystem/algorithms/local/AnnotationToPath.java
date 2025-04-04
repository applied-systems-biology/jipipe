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

package org.hkijena.jipipe.plugins.filesystem.algorithms.local;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AnnotationQueryExpression;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Paths;

/**
 * Filters input files
 */
@SetJIPipeDocumentation(name = "Annotation to path", description = "Converts an annotation of to a path. " +
        "If the specified annotation is not present, an empty path is generated.")
@ConfigureJIPipeNode(menuPath = "Convert", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = PathData.class, name = "Output", create = true)
public class AnnotationToPath extends JIPipeSimpleIteratingAlgorithm {

    private AnnotationQueryExpression annotationExpression = new AnnotationQueryExpression("key == \"#Dataset\"");

    /**
     * Instantiates the algorithm
     *
     * @param info Algorithm info
     */
    public AnnotationToPath(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public AnnotationToPath(AnnotationToPath other) {
        super(other);
        this.annotationExpression = new AnnotationQueryExpression(other.annotationExpression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeTextAnnotation matchingAnnotation = annotationExpression.queryFirst(iterationStep.getMergedTextAnnotations().values());
        if (matchingAnnotation == null || StringUtils.isNullOrEmpty(matchingAnnotation.getValue())) {
            iterationStep.addOutputData(getFirstOutputSlot(), new PathData(Paths.get("")), progressInfo);
            return;
        }
        iterationStep.addOutputData(getFirstOutputSlot(), new PathData(Paths.get(StringUtils.orElse(matchingAnnotation.getValue(), ""))), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Annotation", description = "An expression that determines which annotation is used. ")
    @JIPipeParameter("annotation")
    public AnnotationQueryExpression getAnnotationExpression() {
        return annotationExpression;
    }

    @JIPipeParameter("annotation")
    public void setAnnotationExpression(AnnotationQueryExpression annotationExpression) {
        this.annotationExpression = annotationExpression;
    }
}
