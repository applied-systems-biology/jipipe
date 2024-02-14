package org.hkijena.jipipe.extensions.strings.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.strings.XMLData;
import org.hkijena.jipipe.utils.xml.XmlUtils;

import java.io.IOException;
import java.nio.file.Files;

@JIPipeDocumentation(name = "Import XML", description = "Imports XML data from a file")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "File", autoCreate = true)
@JIPipeOutputSlot(value = XMLData.class, slotName = "XML", autoCreate = true)
public class ImportXMLAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean validateXml = true;

    public ImportXMLAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportXMLAlgorithm(ImportXMLAlgorithm other) {
        super(other);
        this.validateXml = other.validateXml;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FileData fileData = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        try {
            String data = new String(Files.readAllBytes(fileData.toPath()));
            if (validateXml) {
                XmlUtils.readFromString(data);
            }
            iterationStep.addOutputData(getFirstOutputSlot(), new XMLData(data), progressInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JIPipeDocumentation(name = "Validate XML", description = "If enabled, validate if the imported file contains valid XML")
    @JIPipeParameter("validate-xml")
    public boolean isValidateXml() {
        return validateXml;
    }

    @JIPipeParameter("validate-xml")
    public void setValidateXml(boolean validateXml) {
        this.validateXml = validateXml;
    }
}
