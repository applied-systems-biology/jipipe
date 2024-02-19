package org.hkijena.jipipe.extensions.strings.datasources;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.strings.JsonData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;

@SetJIPipeDocumentation(name = "Import JSON", description = "Imports JSON data from a file")
@DefineJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, slotName = "File", create = true)
@AddJIPipeOutputSlot(value = JsonData.class, slotName = "Json", create = true)
public class ImportJsonAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean validateJson = true;

    public ImportJsonAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportJsonAlgorithm(ImportJsonAlgorithm other) {
        super(other);
        this.validateJson = other.validateJson;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FileData fileData = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        try {
            String data = new String(Files.readAllBytes(fileData.toPath()));
            if (validateJson) {
                JsonUtils.readFromString(data, JsonNode.class);
            }
            iterationStep.addOutputData(getFirstOutputSlot(), new JsonData(data), progressInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Validate JSON", description = "If enabled, validate if the imported file contains valid JSON")
    @JIPipeParameter("validate-json")
    public boolean isValidateJson() {
        return validateJson;
    }

    @JIPipeParameter("validate-json")
    public void setValidateJson(boolean validateJson) {
        this.validateJson = validateJson;
    }
}
