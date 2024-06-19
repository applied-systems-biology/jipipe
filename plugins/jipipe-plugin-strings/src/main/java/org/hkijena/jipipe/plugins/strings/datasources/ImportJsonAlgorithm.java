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

package org.hkijena.jipipe.plugins.strings.datasources;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.strings.JsonData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;

@SetJIPipeDocumentation(name = "Import JSON", description = "Imports JSON data from a file")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, name = "File", create = true)
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
