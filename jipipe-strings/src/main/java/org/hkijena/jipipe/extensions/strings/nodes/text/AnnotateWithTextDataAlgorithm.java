package org.hkijena.jipipe.extensions.strings.nodes.text;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
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
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        StringData data = dataBatch.getInputData(getFirstInputSlot(), StringData.class, progressInfo);
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        for (Entry entry : entries.mapToCollection(Entry.class)) {
            variables.set("text", data.getData());
            String preprocessed = entry.getPreprocessor().evaluateToString(variables);
            String annotationName = entry.getAnnotationName().evaluateToString(variables);

            annotationList.add(new JIPipeTextAnnotation(annotationName, preprocessed));
        }

        dataBatch.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeMode, progressInfo);
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
        private DefaultExpressionParameter preprocessor = new DefaultExpressionParameter("text");
        private DefaultExpressionParameter annotationName = new DefaultExpressionParameter("\"Annotation name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.preprocessor = new DefaultExpressionParameter(other.preprocessor);
            this.annotationName = new DefaultExpressionParameter(other.annotationName);
        }

        @JIPipeDocumentation(name = "Preprocessor", description = "An expression that allows to preprocess the text.")
        @JIPipeParameter(value = "preprocessor", uiOrder = -100)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(name = "Text", key = "text", description = "The input text")
        public DefaultExpressionParameter getPreprocessor() {
            return preprocessor;
        }

        @JIPipeParameter("preprocessor")
        public void setPreprocessor(DefaultExpressionParameter preprocessor) {
            this.preprocessor = preprocessor;
        }

        @JIPipeDocumentation(name = "Annotation name", description = "The name of the output annotation.")
        @JIPipeParameter(value = "annotation-name", uiOrder = -90)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(name = "Text", key = "text", description = "The input text")
        public DefaultExpressionParameter getAnnotationName() {
            return annotationName;
        }

        @JIPipeParameter("annotation-name")
        public void setAnnotationName(DefaultExpressionParameter annotationName) {
            this.annotationName = annotationName;
        }
    }
}
