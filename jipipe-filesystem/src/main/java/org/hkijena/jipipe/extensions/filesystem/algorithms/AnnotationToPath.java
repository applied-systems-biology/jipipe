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

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
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
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Paths;

/**
 * Filters input files
 */
@JIPipeDocumentation(name = "Annotation to path", description = "Converts an annotation of to a path. " +
        "If the specified annotation is not present, an empty path is generated.")
@JIPipeNode(menuPath = "Convert", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        JIPipeTextAnnotation matchingAnnotation = annotationExpression.queryFirst(iterationStep.getMergedTextAnnotations().values());
        if (matchingAnnotation == null || StringUtils.isNullOrEmpty(matchingAnnotation.getValue())) {
            iterationStep.addOutputData(getFirstOutputSlot(), new PathData(Paths.get("")), progressInfo);
            return;
        }
        iterationStep.addOutputData(getFirstOutputSlot(), new PathData(Paths.get(StringUtils.orElse(matchingAnnotation.getValue(), ""))), progressInfo);
    }

    @JIPipeDocumentation(name = "Annotation", description = "An expression that determines which annotation is used. ")
    @JIPipeParameter("annotation")
    public AnnotationQueryExpression getAnnotationExpression() {
        return annotationExpression;
    }

    @JIPipeParameter("annotation")
    public void setAnnotationExpression(AnnotationQueryExpression annotationExpression) {
        this.annotationExpression = annotationExpression;
    }
}
