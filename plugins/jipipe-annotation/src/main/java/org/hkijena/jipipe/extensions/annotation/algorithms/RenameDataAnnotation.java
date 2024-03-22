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

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
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
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates annotations from filenames
 */
@SetJIPipeDocumentation(name = "Rename data annotation", description = "Renames one or multiple data annotations.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
public class RenameDataAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpressionAndStringPairParameter.List renamingItems = new StringQueryExpressionAndStringPairParameter.List();
    private JIPipeDataAnnotationMergeMode annotationMergeStrategy = JIPipeDataAnnotationMergeMode.OverwriteExisting;

    /**
     * New instance
     *
     * @param info Algorithm info
     */
    public RenameDataAnnotation(JIPipeNodeInfo info) {
        super(info);
        renamingItems.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other Original algorithm
     */
    public RenameDataAnnotation(RenameDataAnnotation other) {
        super(other);
        this.renamingItems = new StringQueryExpressionAndStringPairParameter.List(other.renamingItems);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<JIPipeDataAnnotation> annotations = new ArrayList<>();
        for (JIPipeDataAnnotation annotation : ImmutableList.copyOf(iterationStep.getMergedDataAnnotations().values())) {
            for (StringQueryExpressionAndStringPairParameter renamingItem : renamingItems) {
                if (renamingItem.getKey().test(annotation.getName())) {
                    annotations.add(new JIPipeDataAnnotation(renamingItem.getValue(), annotation.getDataItemStore()));
                    iterationStep.getMergedTextAnnotations().remove(annotation.getName());
                }
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(),
                iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo),
                Collections.emptyList(),
                JIPipeTextAnnotationMergeMode.OverwriteExisting,
                annotations,
                annotationMergeStrategy,
                progressInfo);
    }

    @SetJIPipeDocumentation(name = "Renaming items", description = "Determines which annotation columns are renamed.")
    @JIPipeParameter("renaming-items")
    public StringQueryExpressionAndStringPairParameter.List getRenamingItems() {
        return renamingItems;
    }

    @JIPipeParameter("renaming-items")
    public void setRenamingItems(StringQueryExpressionAndStringPairParameter.List renamingItems) {
        this.renamingItems = renamingItems;
    }

    @SetJIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if an annotation already exists.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeDataAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeDataAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
