package org.hkijena.jipipe.extensions.strings.nodes.xml;


import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.extensions.strings.StringData;
import org.hkijena.jipipe.extensions.strings.XMLData;
import org.hkijena.jipipe.utils.xml.XmlUtils;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Extract text from XML", description = "Extracts text data from from the input XML data (via XPath). " +
        "Please visit https://www.w3schools.com/xml/xpath_intro.asp to learn about XPath.")
@JIPipeCitation("XPath: https://www.w3schools.com/xml/xpath_intro.asp")
@JIPipeNode(menuPath = "XML", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = XMLData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = StringData.class, slotName = "Output", autoCreate = true)
public class ExtractTextFromXMLAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private DefaultExpressionParameter xPath = new DefaultExpressionParameter("\"/\"");
    private StringAndStringPairParameter.List namespaceMap = new StringAndStringPairParameter.List();

    public ExtractTextFromXMLAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractTextFromXMLAlgorithm(ExtractTextFromXMLAlgorithm other) {
        super(other);
        this.namespaceMap = new StringAndStringPairParameter.List(other.namespaceMap);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        XMLData data = dataBatch.getInputData(getFirstInputSlot(), XMLData.class, progressInfo);
        Document document = XmlUtils.readFromString(data.getData());

        Map<String, String> namespaces = new HashMap<>();
        for (StringAndStringPairParameter parameter : namespaceMap) {
            namespaces.put(parameter.getKey(), parameter.getValue());
        }

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

        String path = xPath.evaluateToString(variables);
        String text = XmlUtils.extractStringFromXPath(document, path, namespaces);

        dataBatch.addOutputData(getFirstOutputSlot(), new StringData(text), progressInfo);
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

    @JIPipeDocumentation(name = "XPath", description = "An expression that returns the XPath of the XML entries. Please visit https://www.w3schools.com/xml/xpath_intro.asp to learn more about XPath.")
    @JIPipeParameter(value = "xpath")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getxPath() {
        return xPath;
    }

    @JIPipeParameter("xpath")
    public void setxPath(DefaultExpressionParameter xPath) {
        this.xPath = xPath;
    }
}
