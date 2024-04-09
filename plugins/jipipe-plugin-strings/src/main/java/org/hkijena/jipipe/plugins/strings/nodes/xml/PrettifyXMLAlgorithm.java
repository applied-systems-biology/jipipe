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

package org.hkijena.jipipe.plugins.strings.nodes.xml;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.strings.XMLData;
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
