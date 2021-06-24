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

package org.hkijena.jipipe.extensions.parameters.primitives;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Parameter that acts as dynamic enum.
 * Use {@link DynamicEnumParameterSettings} to define a supplier for the
 * items.
 */
@JsonSerialize(using = DynamicDataImportOperationIdEnumParameter.Serializer.class)
@JsonDeserialize(using = DynamicDataImportOperationIdEnumParameter.Deserializer.class)
public class DynamicDataImportOperationIdEnumParameter extends DynamicEnumParameter<String> {

    private String dataTypeId;

    /**
     * Creates a new instance with null value
     */
    public DynamicDataImportOperationIdEnumParameter() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public DynamicDataImportOperationIdEnumParameter(DynamicDataImportOperationIdEnumParameter other) {
        this.dataTypeId = other.dataTypeId;
        setValue(other.getValue());
        setAllowedValues(new ArrayList<>(other.getAllowedValues()));
    }

    /**
     * Creates a new instance
     *
     * @param value initial value
     */
    public DynamicDataImportOperationIdEnumParameter(String value) {
        super(value);
    }

    @Override
    public String renderLabel(String value) {
        if(JIPipe.getInstance() != null && dataTypeId != null) {
            JIPipeDataImportOperation operation = JIPipe.getDataTypes().getAllRegisteredImportOperations(dataTypeId).getOrDefault(value, null);
            if(operation != null)
                return operation.getName();
        }
        return super.renderLabel(value);
    }

    @Override
    public String renderTooltip(String value) {
        if(JIPipe.getInstance() != null && dataTypeId != null) {
            JIPipeDataImportOperation operation = JIPipe.getDataTypes().getAllRegisteredImportOperations(dataTypeId).getOrDefault(value, null);
            if(operation != null)
                return operation.getDescription();
        }
        return super.renderTooltip(value);
    }

    @Override
    public Icon renderIcon(String value) {
        if(JIPipe.getInstance() != null && dataTypeId != null) {
            JIPipeDataImportOperation operation = JIPipe.getDataTypes().getAllRegisteredImportOperations(dataTypeId).getOrDefault(value, null);
            if(operation != null)
                return operation.getIcon();
        }
        return super.renderIcon(value);
    }

    public String getDataTypeId() {
        return dataTypeId;
    }

    public void setDataTypeId(String dataTypeId) {
        this.dataTypeId = dataTypeId;
    }

    /**
     * Serializes {@link DynamicDataImportOperationIdEnumParameter}
     */
    public static class Serializer extends JsonSerializer<DynamicDataImportOperationIdEnumParameter> {
        @Override
        public void serialize(DynamicDataImportOperationIdEnumParameter value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeString("" + value.getValue());
        }
    }

    /**
     * Deserializes {@link DynamicDataImportOperationIdEnumParameter}
     */
    public static class Deserializer extends JsonDeserializer<DynamicDataImportOperationIdEnumParameter> {
        @Override
        public DynamicDataImportOperationIdEnumParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return new DynamicDataImportOperationIdEnumParameter(((JsonNode) p.readValueAsTree()).textValue());
        }
    }
}
