package org.hkijena.jipipe.extensions.strings.nodes.json;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.hkijena.jipipe.api.JIPipeCitation;
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
import org.hkijena.jipipe.extensions.strings.JsonData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Annotate with JSON values", description = "Extracts a value from the input JSON data (via JsonPath) and annotates the data with the result. " +
        "Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath")
@JIPipeCitation("JsonPath: https://goessner.net/articles/JsonPath/")
@JIPipeNode(menuPath = "For JSON", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@JIPipeInputSlot(value = JsonData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JsonData.class, slotName = "Output", autoCreate = true)
public class AnnotateWithJsonDataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(Entry.class);
    private JIPipeTextAnnotationMergeMode annotationMergeMode = JIPipeTextAnnotationMergeMode.Merge;

    public AnnotateWithJsonDataAlgorithm(JIPipeNodeInfo info) {
        super(info);
        entries.addNewInstance();
    }

    public AnnotateWithJsonDataAlgorithm(AnnotateWithJsonDataAlgorithm other) {
        super(other);
        this.entries = new ParameterCollectionList(other.entries);
        this.annotationMergeMode = other.annotationMergeMode;
    }


    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JsonData data = dataBatch.getInputData(getFirstInputSlot(), JsonData.class, progressInfo);
        DocumentContext documentContext = JsonPath.parse(data.getData());
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        for (Entry entry : entries.mapToCollection(Entry.class)) {
            String path = entry.getJsonPath().evaluateToString(variables);
            String annotationName = entry.getAnnotationName().evaluateToString(variables);
            String annotationValue = StringUtils.nullToEmpty(documentContext.read(path));

            annotationList.add(new JIPipeTextAnnotation(annotationName, annotationValue));
        }

        dataBatch.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeMode, progressInfo);
    }

    @JIPipeDocumentation(name = "Generated annotations", description = "The list of generated annotations. Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath.")
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
        private DefaultExpressionParameter jsonPath = new DefaultExpressionParameter("\"$\"");
        private DefaultExpressionParameter annotationName = new DefaultExpressionParameter("\"Annotation name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.jsonPath = new DefaultExpressionParameter(other.jsonPath);
            this.annotationName = new DefaultExpressionParameter(other.annotationName);
        }

        @JIPipeDocumentation(name = "JSON path", description = "An expression that returns the JsonPath of the JSON entries. Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath.")
        @JIPipeParameter(value = "json-path", uiOrder = -100)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        public DefaultExpressionParameter getJsonPath() {
            return jsonPath;
        }

        @JIPipeParameter("json-path")
        public void setJsonPath(DefaultExpressionParameter jsonPath) {
            this.jsonPath = jsonPath;
        }

        @JIPipeDocumentation(name = "Annotation name", description = "The name of the output annotation.")
        @JIPipeParameter(value = "annotation-name", uiOrder = -90)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        public DefaultExpressionParameter getAnnotationName() {
            return annotationName;
        }

        @JIPipeParameter("annotation-name")
        public void setAnnotationName(DefaultExpressionParameter annotationName) {
            this.annotationName = annotationName;
        }
    }
}
