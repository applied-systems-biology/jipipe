package org.hkijena.jipipe.extensions.strings.datasources;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.strings.JsonData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;

@JIPipeDocumentation(name = "Import JSON", description = "Imports JSON data from a file")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "File", autoCreate = true)
@JIPipeOutputSlot(value = JsonData.class, slotName = "Json", autoCreate = true)
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
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FileData fileData = dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        try {
            String data = new String(Files.readAllBytes(fileData.toPath()));
            if (validateJson) {
                JsonUtils.readFromString(data, JsonNode.class);
            }
            dataBatch.addOutputData(getFirstOutputSlot(), new JsonData(data), progressInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JIPipeDocumentation(name = "Validate JSON", description = "If enabled, validate if the imported file contains valid JSON")
    @JIPipeParameter("validate-json")
    public boolean isValidateJson() {
        return validateJson;
    }

    @JIPipeParameter("validate-json")
    public void setValidateJson(boolean validateJson) {
        this.validateJson = validateJson;
    }
}
