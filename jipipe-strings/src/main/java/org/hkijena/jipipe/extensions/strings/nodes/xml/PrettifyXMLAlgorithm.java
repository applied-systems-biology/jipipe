package org.hkijena.jipipe.extensions.strings.nodes.xml;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.strings.XMLData;
import org.hkijena.jipipe.utils.xml.XmlUtils;

@JIPipeDocumentation(name = "Prettify XML", description = "Prettifies/formats the input XML data")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "XML")
@JIPipeInputSlot(value = XMLData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = XMLData.class, slotName = "Output", autoCreate = true)
public class PrettifyXMLAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int indent = 4;
    private boolean ignoreDeclaration = false;

    public PrettifyXMLAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public PrettifyXMLAlgorithm(PrettifyXMLAlgorithm other) {
        super(other);
        this.indent = other.indent;
        this.ignoreDeclaration = other.ignoreDeclaration;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        String xml = dataBatch.getInputData(getFirstInputSlot(), XMLData.class, progressInfo).getData();
        String transformed = XmlUtils.prettyPrint(xml, indent, ignoreDeclaration);
        dataBatch.addOutputData(getFirstOutputSlot(), new XMLData(transformed), progressInfo);
    }

    @JIPipeDocumentation(name = "Indent", description = "The indent of a level")
    @JIPipeParameter("indent")
    public int getIndent() {
        return indent;
    }

    @JIPipeParameter("indent")
    public void setIndent(int indent) {
        this.indent = indent;
    }

    public boolean isIgnoreDeclaration() {
        return ignoreDeclaration;
    }

    public void setIgnoreDeclaration(boolean ignoreDeclaration) {
        this.ignoreDeclaration = ignoreDeclaration;
    }
}
