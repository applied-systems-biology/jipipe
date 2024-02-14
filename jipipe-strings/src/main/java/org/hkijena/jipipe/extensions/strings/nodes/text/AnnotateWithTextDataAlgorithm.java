package org.hkijena.jipipe.extensions.strings.nodes.text;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.strings.StringData;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Annotate with text values", description = "Extracts a value from the input text data (via an expression) and annotates the data with the result.")
@JIPipeNode(menuPath = "For text", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@JIPipeInputSlot(value = StringData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = StringData.class, slotName = "Output", autoCreate = true)
public class AnnotateWithTextDataAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(Entry.class);
    private JIPipeTextAnnotationMergeMode annotationMergeMode = JIPipeTextAnnotationMergeMode.Merge;

    public AnnotateWithTextDataAlgorithm(JIPipeNodeInfo info) {
        super(info);
        entries.addNewInstance();
    }

    public AnnotateWithTextDataAlgorithm(AnnotateWithTextDataAlgorithm other) {
        super(other);
        this.entries = new ParameterCollectionList(other.entries);
        this.annotationMergeMode = other.annotationMergeMode;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        StringData data = iterationStep.getInputData(getFirstInputSlot(), StringData.class, progressInfo);
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (Entry entry : entries.mapToCollection(Entry.class)) {
            variables.set("text", data.getData());
            String preprocessed = entry.getPreprocessor().evaluateToString(variables);
            String annotationName = entry.getAnnotationName().evaluateToString(variables);

            annotationList.add(new JIPipeTextAnnotation(annotationName, preprocessed));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeMode, progressInfo);
    }

    @JIPipeDocumentation(name = "Generated annotations", description = "The list of generated annotations.")
    @JIPipeParameter("entries")
    @ParameterCollectionListTemplate(Entry.class)
    public ParameterCollectionList getEntries() {
        return entries;
    }

    @JIPipeParameter("entries")
    public void setEntries(ParameterCollectionList entries) {
        this.entries = entries;
    }

    @JIPipeDocumentation(name = "Annotation merge mode", description = "Determines how newly generated annotations are merged with existing ones")
    @JIPipeParameter("annotation-merge-mode")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeMode() {
        return annotationMergeMode;
    }

    @JIPipeParameter("annotation-merge-mode")
    public void setAnnotationMergeMode(JIPipeTextAnnotationMergeMode annotationMergeMode) {
        this.annotationMergeMode = annotationMergeMode;
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter preprocessor = new JIPipeExpressionParameter("text");
        private JIPipeExpressionParameter annotationName = new JIPipeExpressionParameter("\"Annotation name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.preprocessor = new JIPipeExpressionParameter(other.preprocessor);
            this.annotationName = new JIPipeExpressionParameter(other.annotationName);
        }

        @JIPipeDocumentation(name = "Preprocessor", description = "An expression that allows to preprocess the text.")
        @JIPipeParameter(value = "preprocessor", uiOrder = -100)
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(name = "Text", key = "text", description = "The input text")
        public JIPipeExpressionParameter getPreprocessor() {
            return preprocessor;
        }

        @JIPipeParameter("preprocessor")
        public void setPreprocessor(JIPipeExpressionParameter preprocessor) {
            this.preprocessor = preprocessor;
        }

        @JIPipeDocumentation(name = "Annotation name", description = "The name of the output annotation.")
        @JIPipeParameter(value = "annotation-name", uiOrder = -90)
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(name = "Text", key = "text", description = "The input text")
        public JIPipeExpressionParameter getAnnotationName() {
            return annotationName;
        }

        @JIPipeParameter("annotation-name")
        public void setAnnotationName(JIPipeExpressionParameter annotationName) {
            this.annotationName = annotationName;
        }
    }
}
