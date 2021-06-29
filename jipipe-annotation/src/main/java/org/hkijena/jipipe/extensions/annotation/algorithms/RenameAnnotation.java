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
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates annotations from filenames
 */
@JIPipeDocumentation(name = "Rename annotation", description = "Renames one or multiple annotations.")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class RenameAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpressionAndStringPairParameter.List renamingItems = new StringQueryExpressionAndStringPairParameter.List();
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;

    /**
     * New instance
     *
     * @param info Algorithm info
     */
    public RenameAnnotation(JIPipeNodeInfo info) {
        super(info);
        renamingItems.addNewInstance();
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<JIPipeAnnotation> annotations = new ArrayList<>();
        for (JIPipeAnnotation annotation : ImmutableList.copyOf(dataBatch.getGlobalAnnotations().values())) {
            for (StringQueryExpressionAndStringPairParameter renamingItem : renamingItems) {
                if(renamingItem.getKey().test(annotation.getName())) {
                    annotations.add(new JIPipeAnnotation(renamingItem.getValue(), annotation.getValue()));
                    dataBatch.getGlobalAnnotations().remove(annotation.getName());
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
    public StringQueryExpressionAndStringPairParameter.List getRenamingItems() {
        return renamingItems;
    }

    @JIPipeParameter("renaming-items")
    public void setRenamingItems(StringQueryExpressionAndStringPairParameter.List renamingItems) {
        this.renamingItems = renamingItems;
    }

    @JIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if an annotation already exists.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
