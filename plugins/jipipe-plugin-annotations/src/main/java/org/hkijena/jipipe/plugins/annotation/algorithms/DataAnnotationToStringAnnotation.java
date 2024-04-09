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

package org.hkijena.jipipe.plugins.annotation.algorithms;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
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
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Convert data annotation to string annotation", description = "Convert data annotations to a strings and generates a string annotation based on it. The string annotations have the same names as the data annotations.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For all data")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
public class DataAnnotationToStringAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression nameFilter = new StringQueryExpression();
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;
    private boolean keepDataAnnotations = true;

    public DataAnnotationToStringAnnotation(JIPipeNodeInfo info) {
        super(info);
    }

    public DataAnnotationToStringAnnotation(DataAnnotationToStringAnnotation other) {
        super(other);
        other.nameFilter = new StringQueryExpression(other.nameFilter);
        this.keepDataAnnotations = other.keepDataAnnotations;
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
        for (String name : ImmutableList.copyOf(iterationStep.getMergedDataAnnotations().keySet())) {
            if (nameFilter.test(name)) {
                annotationList.add(new JIPipeTextAnnotation(name, iterationStep.getMergedDataAnnotation(name).getDataItemStore().getStringRepresentation()));
                if (!keepDataAnnotations)
                    iterationStep.getMergedTextAnnotations().remove(name);
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), annotationList, annotationMergeStrategy, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Keep data annotations", description = "If enabled, data annotations are not deleted after converting them to a string annotation.")
    @JIPipeParameter("keep-data-annotations")
    public boolean isKeepDataAnnotations() {
        return keepDataAnnotations;
    }

    @JIPipeParameter("keep-data-annotations")
    public void setKeepDataAnnotations(boolean keepDataAnnotations) {
        this.keepDataAnnotations = keepDataAnnotations;
    }

    @SetJIPipeDocumentation(name = "Name filter", description = "Determines which data annotations are converted. ")
    @JIPipeParameter("name-filter")
    public StringQueryExpression getNameFilter() {
        return nameFilter;
    }

    @JIPipeParameter("name-filter")
    public void setNameFilter(StringQueryExpression nameFilter) {
        this.nameFilter = nameFilter;
    }

    @SetJIPipeDocumentation(name = "Merge same annotation values", description = "Determines how the newly created string annotations are merged into existing ones. ")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
