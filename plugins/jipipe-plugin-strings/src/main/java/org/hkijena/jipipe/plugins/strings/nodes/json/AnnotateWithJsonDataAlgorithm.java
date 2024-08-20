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

package org.hkijena.jipipe.plugins.strings.nodes.json;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.plugins.strings.JsonData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate with JSON values", description = "Extracts a value from the input JSON data (via JsonPath) and annotates the data with the result. " +
        "Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath")
@AddJIPipeCitation("JsonPath: https://goessner.net/articles/JsonPath/")
@ConfigureJIPipeNode(menuPath = "For JSON", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JsonData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = JsonData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JsonData data = iterationStep.getInputData(getFirstInputSlot(), JsonData.class, progressInfo);
        DocumentContext documentContext = JsonPath.parse(data.getData());
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (Entry entry : entries.mapToCollection(Entry.class)) {
            String path = entry.getJsonPath().evaluateToString(variables);
            String annotationName = entry.getAnnotationName().evaluateToString(variables);
            String annotationValue = StringUtils.nullToEmpty(documentContext.read(path));

            annotationList.add(new JIPipeTextAnnotation(annotationName, annotationValue));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeMode, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Generated annotations", description = "The list of generated annotations. Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath.")
    @JIPipeParameter("entries")
    @ParameterCollectionListTemplate(Entry.class)
    public ParameterCollectionList getEntries() {
        return entries;
    }

    @JIPipeParameter("entries")
    public void setEntries(ParameterCollectionList entries) {
        this.entries = entries;
    }

    @SetJIPipeDocumentation(name = "Annotation merge mode", description = "Determines how newly generated annotations are merged with existing ones")
    @JIPipeParameter("annotation-merge-mode")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeMode() {
        return annotationMergeMode;
    }

    @JIPipeParameter("annotation-merge-mode")
    public void setAnnotationMergeMode(JIPipeTextAnnotationMergeMode annotationMergeMode) {
        this.annotationMergeMode = annotationMergeMode;
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter jsonPath = new JIPipeExpressionParameter("\"$\"");
        private JIPipeExpressionParameter annotationName = new JIPipeExpressionParameter("\"Annotation name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.jsonPath = new JIPipeExpressionParameter(other.jsonPath);
            this.annotationName = new JIPipeExpressionParameter(other.annotationName);
        }

        @SetJIPipeDocumentation(name = "JSON path", description = "An expression that returns the JsonPath of the JSON entries. Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath.")
        @JIPipeParameter(value = "json-path", uiOrder = -100)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getJsonPath() {
            return jsonPath;
        }

        @JIPipeParameter("json-path")
        public void setJsonPath(JIPipeExpressionParameter jsonPath) {
            this.jsonPath = jsonPath;
        }

        @SetJIPipeDocumentation(name = "Annotation name", description = "The name of the output annotation.")
        @JIPipeParameter(value = "annotation-name", uiOrder = -90)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getAnnotationName() {
            return annotationName;
        }

        @JIPipeParameter("annotation-name")
        public void setAnnotationName(JIPipeExpressionParameter annotationName) {
            this.annotationName = annotationName;
        }
    }
}
