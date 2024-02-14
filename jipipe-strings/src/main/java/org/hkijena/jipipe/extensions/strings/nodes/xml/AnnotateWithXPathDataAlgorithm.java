package org.hkijena.jipipe.extensions.strings.nodes.xml;


import org.hkijena.jipipe.api.JIPipeCitation;
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
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.extensions.strings.XMLData;
import org.hkijena.jipipe.utils.xml.XmlUtils;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Annotate with XML values", description = "Extracts a value from the input text data (via XPath) and annotates the data with the result. " +
        "Please visit https://www.w3schools.com/xml/xpath_intro.asp to learn about XPath.")
@JIPipeCitation("XPath: https://www.w3schools.com/xml/xpath_intro.asp")
@JIPipeNode(menuPath = "For XML", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@JIPipeInputSlot(value = XMLData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = XMLData.class, slotName = "Output", autoCreate = true)
public class AnnotateWithXPathDataAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(Entry.class);
    private JIPipeTextAnnotationMergeMode annotationMergeMode = JIPipeTextAnnotationMergeMode.Merge;
    private StringAndStringPairParameter.List namespaceMap = new StringAndStringPairParameter.List();

    public AnnotateWithXPathDataAlgorithm(JIPipeNodeInfo info) {
        super(info);
        entries.addNewInstance();
    }

    public AnnotateWithXPathDataAlgorithm(AnnotateWithXPathDataAlgorithm other) {
        super(other);
        this.namespaceMap = new StringAndStringPairParameter.List(other.namespaceMap);
        this.entries = new ParameterCollectionList(other.entries);
        this.annotationMergeMode = other.annotationMergeMode;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        XMLData data = iterationStep.getInputData(getFirstInputSlot(), XMLData.class, progressInfo);
        Document document = XmlUtils.readFromString(data.getData());
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();

        Map<String, String> namespaces = new HashMap<>();
        for (StringAndStringPairParameter parameter : namespaceMap) {
            namespaces.put(parameter.getKey(), parameter.getValue());
        }

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (Entry entry : entries.mapToCollection(Entry.class)) {
            String path = entry.getxPath().evaluateToString(variables);
            String annotationName = entry.getAnnotationName().evaluateToString(variables);

            String annotationValue = XmlUtils.extractStringFromXPath(document, path, namespaces);
            annotationList.add(new JIPipeTextAnnotation(annotationName, annotationValue));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeMode, progressInfo);
    }

    @JIPipeDocumentation(name = "Generated annotations", description = "The list of generated annotations. Please visit https://www.w3schools.com/xml/xpath_intro.asp to learn more about XPath.")
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

    @JIPipeDocumentation(name = "Namespace map", description = "Allows to map namespaces to shortcuts for more convenient access")
    @JIPipeParameter("namespace-map")
    @PairParameterSettings(keyLabel = "Shortcut", valueLabel = "Namespace")
    public StringAndStringPairParameter.List getNamespaceMap() {
        return namespaceMap;
    }

    @JIPipeParameter("namespace-map")
    public void setNamespaceMap(StringAndStringPairParameter.List namespaceMap) {
        this.namespaceMap = namespaceMap;
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter xPath = new JIPipeExpressionParameter("\"/\"");
        private JIPipeExpressionParameter annotationName = new JIPipeExpressionParameter("\"Annotation name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.xPath = new JIPipeExpressionParameter(other.xPath);
            this.annotationName = new JIPipeExpressionParameter(other.annotationName);
        }

        @JIPipeDocumentation(name = "XPath", description = "An expression that returns the XPath of the XML entries. Please visit https://www.w3schools.com/xml/xpath_intro.asp to learn more about XPath.")
        @JIPipeParameter(value = "xpath", uiOrder = -100)
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getxPath() {
            return xPath;
        }

        @JIPipeParameter("xpath")
        public void setxPath(JIPipeExpressionParameter xPath) {
            this.xPath = xPath;
        }

        @JIPipeDocumentation(name = "Annotation name", description = "The name of the output annotation.")
        @JIPipeParameter(value = "annotation-name", uiOrder = -90)
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getAnnotationName() {
            return annotationName;
        }

        @JIPipeParameter("annotation-name")
        public void setAnnotationName(JIPipeExpressionParameter annotationName) {
            this.annotationName = annotationName;
        }
    }
}
