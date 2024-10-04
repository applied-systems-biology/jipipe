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

package org.hkijena.jipipe.plugins.parameters.library.jipipe;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicEnumParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicEnumParameterSettings;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Parameter that acts as dynamic enum.
 * Use {@link DynamicEnumParameterSettings} to define a supplier for the
 * items.
 */
@JsonSerialize(using = DynamicDataDisplayOperationIdEnumParameter.Serializer.class)
@JsonDeserialize(using = DynamicDataDisplayOperationIdEnumParameter.Deserializer.class)
public class DynamicDataDisplayOperationIdEnumParameter extends DynamicEnumParameter<String> {

    private String dataTypeId;

    /**
     * Creates a new instance with null value
     */
    public DynamicDataDisplayOperationIdEnumParameter() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public DynamicDataDisplayOperationIdEnumParameter(DynamicDataDisplayOperationIdEnumParameter other) {
        this.dataTypeId = other.dataTypeId;
        setValue(other.getValue());
        setAllowedValues(new ArrayList<>(other.getAllowedValues()));
    }

    /**
     * Creates a new instance
     *
     * @param value initial value
     */
    public DynamicDataDisplayOperationIdEnumParameter(String value) {
        super(value);
    }

    @Override
    public String renderLabel(String value) {
        if (JIPipe.getInstance() != null && dataTypeId != null) {
            JIPipeDesktopDataDisplayOperation operation = JIPipe.getDataTypes().getAllRegisteredDisplayOperations(dataTypeId).getOrDefault(value, null);
            if (operation != null)
                return operation.getName();
        }
        return super.renderLabel(value);
    }

    @Override
    public String renderTooltip(String value) {
        if (JIPipe.getInstance() != null && dataTypeId != null) {
            JIPipeDesktopDataDisplayOperation operation = JIPipe.getDataTypes().getAllRegisteredDisplayOperations(dataTypeId).getOrDefault(value, null);
            if (operation != null)
                return operation.getDescription();
        }
        return super.renderTooltip(value);
    }

    @Override
    public Icon renderIcon(String value) {
        if (JIPipe.getInstance() != null && dataTypeId != null) {
            JIPipeDesktopDataDisplayOperation operation = JIPipe.getDataTypes().getAllRegisteredDisplayOperations(dataTypeId).getOrDefault(value, null);
            if (operation != null)
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
     * Serializes {@link DynamicDataDisplayOperationIdEnumParameter}
     */
    public static class Serializer extends JsonSerializer<DynamicDataDisplayOperationIdEnumParameter> {
        @Override
        public void serialize(DynamicDataDisplayOperationIdEnumParameter value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeString("" + value.getValue());
        }
    }

    /**
     * Deserializes {@link DynamicDataDisplayOperationIdEnumParameter}
     */
    public static class Deserializer extends JsonDeserializer<DynamicDataDisplayOperationIdEnumParameter> {
        @Override
        public DynamicDataDisplayOperationIdEnumParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return new DynamicDataDisplayOperationIdEnumParameter(((JsonNode) p.readValueAsTree()).textValue());
        }
    }
}
