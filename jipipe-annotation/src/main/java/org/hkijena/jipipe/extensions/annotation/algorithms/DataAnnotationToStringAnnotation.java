package org.hkijena.jipipe.extensions.annotation.algorithms;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Convert data annotation to string annotation", description = "Convert data annotations to a strings and generates a string annotation based on it. The string annotations have the same names as the data annotations.")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Generate")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")
public class DataAnnotationToStringAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression nameFilter = new StringQueryExpression();
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<JIPipeAnnotation> annotationList = new ArrayList<>();
        for (String name : ImmutableList.copyOf(dataBatch.getGlobalDataAnnotations().keySet())) {
            if(nameFilter.test(name)) {
                annotationList.add(new JIPipeAnnotation(name, dataBatch.getGlobalDataAnnotation(name).getVirtualData().getStringRepresentation()));
                if(!keepDataAnnotations)
                    dataBatch.getGlobalAnnotations().remove(name);
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), annotationList, annotationMergeStrategy, progressInfo);
    }

    @JIPipeDocumentation(name = "Keep data annotations", description = "If enabled, data annotations are not deleted after converting them to a string annotation.")
    @JIPipeParameter("keep-data-annotations")
    public boolean isKeepDataAnnotations() {
        return keepDataAnnotations;
    }

    @JIPipeParameter("keep-data-annotations")
    public void setKeepDataAnnotations(boolean keepDataAnnotations) {
        this.keepDataAnnotations = keepDataAnnotations;
    }

    @JIPipeDocumentation(name = "Name filter", description = "Determines which data annotations are converted. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("name-filter")
    public StringQueryExpression getNameFilter() {
        return nameFilter;
    }

    @JIPipeParameter("name-filter")
    public void setNameFilter(StringQueryExpression nameFilter) {
        this.nameFilter = nameFilter;
    }

    @JIPipeDocumentation(name = "Merge same annotation values", description = "Determines how the newly created string annotations are merged into existing ones. ")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
