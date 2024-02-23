package org.hkijena.jipipe.extensions.strings.nodes.xml;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.strings.XMLData;
import org.hkijena.jipipe.utils.xml.XmlUtils;

@SetJIPipeDocumentation(name = "Prettify XML", description = "Prettifies/formats the input XML data")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "XML")
@AddJIPipeInputSlot(value = XMLData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = XMLData.class, slotName = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        String xml = iterationStep.getInputData(getFirstInputSlot(), XMLData.class, progressInfo).getData();
        String transformed = XmlUtils.prettyPrint(xml, indent, ignoreDeclaration);
        iterationStep.addOutputData(getFirstOutputSlot(), new XMLData(transformed), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Indent", description = "The indent of a level")
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
