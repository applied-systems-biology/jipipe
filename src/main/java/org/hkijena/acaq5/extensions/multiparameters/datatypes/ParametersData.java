/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.extensions.multiparameters.datatypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the parameters of an algorithm
 */
@ACAQDocumentation(name = "Parameters", description = "Contains algorithm parameters")
@JsonSerialize(using = ParametersData.Serializer.class)
public class ParametersData implements ACAQData {

    private Map<String, Object> parameterData = new HashMap<>();

    @Override
    public void saveTo(Path storageFilePath, String name) {
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValue(storageFilePath.resolve(name + ".json").toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ACAQData duplicate() {
        ParametersData data = new ParametersData();
        data.parameterData = new HashMap<>(parameterData);
        return data;
    }

    @Override
    public void display(String displayName, ACAQWorkbench workbench) {
        try {
            Path outputFile = Files.createTempFile("ACAQTempParameters-" + StringUtils.makeFilesystemCompatible(displayName), ".json");
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValue(outputFile.toFile(), this);
            Desktop.getDesktop().open(outputFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getParameterData() {
        return parameterData;
    }

    public void setParameterData(Map<String, Object> parameterData) {
        this.parameterData = parameterData;
    }

    @Override
    public String toString() {
        return "Parameters (" + String.join(", ", parameterData.keySet()) + ")";
    }

    /**
     * Serializes {@link ParametersData}
     */
    public static class Serializer extends JsonSerializer<ParametersData> {
        @Override
        public void serialize(ParametersData value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeStartObject();
            gen.writeObjectField("data", value.parameterData);
            gen.writeEndObject();
        }
    }
}
