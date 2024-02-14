package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Annotate with data paths", description = "Annotates data with project-related paths")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For all data")
public class AnnotateByProjectPaths extends JIPipeSimpleIteratingAlgorithm {

    private ParameterCollectionList generatedAnnotations = ParameterCollectionList.containingCollection(Entry.class);
    private JIPipeTextAnnotationMergeMode mergeMode = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public AnnotateByProjectPaths(JIPipeNodeInfo info) {
        super(info);
        generatedAnnotations.addNewInstance();
    }

    public AnnotateByProjectPaths(AnnotateByProjectPaths other) {
        super(other);
        this.mergeMode = other.mergeMode;
        this.generatedAnnotations = new ParameterCollectionList(other.generatedAnnotations);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        JIPipeData data = iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);
        Path scratch = getNewScratch();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        Map<String, Path> projectDataDirs;
        if (getRuntimeProject() != null) {
            projectDataDirs = getRuntimeProject().getDirectoryMap();
        } else {
            projectDataDirs = Collections.emptyMap();
        }

        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
        for (Entry entry : generatedAnnotations.mapToCollection(Entry.class)) {
            String annotationName = entry.annotationName.evaluateToString(variables);
            String annotationValue = StringUtils.nullToEmpty(entry.annotationValue.generatePath(scratch,
                    getProjectDirectory(),
                    projectDataDirs,
                    data.toString(),
                    iterationStep.getInputRow(getFirstInputSlot()),
                    new ArrayList<>(iterationStep.getMergedTextAnnotations().values())));
            annotationList.add(new JIPipeTextAnnotation(annotationName, annotationValue));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), data, annotationList, mergeMode, progressInfo);
    }

    @JIPipeDocumentation(name = "Generated annotations", description = "The list of annotations that will be generated.")
    @JIPipeParameter("generated-annotations")
    @ParameterCollectionListTemplate(Entry.class)
    public ParameterCollectionList getGeneratedAnnotations() {
        return generatedAnnotations;
    }

    @JIPipeParameter("generated-annotations")
    public void setGeneratedAnnotations(ParameterCollectionList generatedAnnotations) {
        this.generatedAnnotations = generatedAnnotations;
    }

    @JIPipeDocumentation(name = "Annotation merge mode", description = "Determines how generated annotations are merged with existing ones")
    @JIPipeParameter("merge-mode")
    public JIPipeTextAnnotationMergeMode getMergeMode() {
        return mergeMode;
    }

    @JIPipeParameter("merge-mode")
    public void setMergeMode(JIPipeTextAnnotationMergeMode mergeMode) {
        this.mergeMode = mergeMode;
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter annotationName = new JIPipeExpressionParameter("\"Annotation name\"");
        private DataExportExpressionParameter annotationValue = new DataExportExpressionParameter();

        public Entry() {
        }

        @JIPipeDocumentation(name = "Annotation name", description = "An expression that generates the annotation name")
        @JIPipeParameter("annotation-name")
        public JIPipeExpressionParameter getAnnotationName() {
            return annotationName;
        }

        @JIPipeParameter("annotation-name")
        public void setAnnotationName(JIPipeExpressionParameter annotationName) {
            this.annotationName = annotationName;
        }

        @JIPipeDocumentation(name = "Annotation value", description = "An expression that generates the annotation value")
        @JIPipeParameter("annotation-value")
        public DataExportExpressionParameter getAnnotationValue() {
            return annotationValue;
        }

        @JIPipeParameter("annotation-value")
        public void setAnnotationValue(DataExportExpressionParameter annotationValue) {
            this.annotationValue = annotationValue;
        }
    }
}
