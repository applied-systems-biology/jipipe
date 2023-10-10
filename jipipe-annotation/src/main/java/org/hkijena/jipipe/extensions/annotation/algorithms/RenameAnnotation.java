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

package org.hkijena.jipipe.extensions.annotation.algorithms;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates annotations from filenames
 */
@JIPipeDocumentation(name = "Rename annotation", description = "Renames one or multiple annotations.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true)
public class RenameAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpressionAndStringPairParameter.List renamingItems = new StringQueryExpressionAndStringPairParameter.List();
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * New instance
     *
     * @param info Algorithm info
     */
    public RenameAnnotation(JIPipeNodeInfo info) {
        super(info);
        renamingItems.addNewInstance();
        renamingItems.get(0).setKey(new StringQueryExpression("\"#Dataset\""));
        renamingItems.get(0).setValue("New name");
    }

    /**
     * Copies the algorithm
     *
     * @param other Original algorithm
     */
    public RenameAnnotation(RenameAnnotation other) {
        super(other);
        this.renamingItems = new StringQueryExpressionAndStringPairParameter.List(other.renamingItems);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        for (JIPipeTextAnnotation annotation : ImmutableList.copyOf(dataBatch.getMergedTextAnnotations().values())) {
            for (StringQueryExpressionAndStringPairParameter renamingItem : renamingItems) {
                if (renamingItem.getKey().test(annotation.getName())) {
                    annotations.add(new JIPipeTextAnnotation(renamingItem.getValue(), annotation.getValue()));
                    dataBatch.getMergedTextAnnotations().remove(annotation.getName());
                }
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(),
                dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo),
                annotations,
                annotationMergeStrategy,
                progressInfo);
    }

    @JIPipeDocumentation(name = "Renaming items", description = "Determines which annotation columns are renamed.")
    @JIPipeParameter("renaming-items")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @PairParameterSettings(keyLabel = "From", valueLabel = "To")
    public StringQueryExpressionAndStringPairParameter.List getRenamingItems() {
        return renamingItems;
    }

    @JIPipeParameter("renaming-items")
    public void setRenamingItems(StringQueryExpressionAndStringPairParameter.List renamingItems) {
        this.renamingItems = renamingItems;
    }

    @JIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if an annotation already exists.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
