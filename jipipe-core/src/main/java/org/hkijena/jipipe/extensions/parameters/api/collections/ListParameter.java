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

package org.hkijena.jipipe.extensions.parameters.api.collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.parameters.JIPipeCustomTextDescriptionParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A parameter that is a collection of another parameter type
 */
@JsonSerialize(using = ListParameter.Serializer.class)
@JsonDeserialize(using = ListParameter.Deserializer.class)
public abstract class ListParameter<T> extends ArrayList<T> implements JIPipeValidatable, JIPipeCustomTextDescriptionParameter {
    private Class<T> contentClass;
    private Supplier<T> customInstanceGenerator;

    /**
     * @param contentClass the stored content
     */
    public ListParameter(Class<T> contentClass) {
        this.contentClass = contentClass;
    }

    public Class<T> getContentClass() {
        return contentClass;
    }

    /**
     * Adds a new instance of the content class
     * Override this method for types that cannot be default-constructed
     *
     * @return the instance
     */
    public T addNewInstance() {
        if (customInstanceGenerator != null) {
            T instance = customInstanceGenerator.get();
            add(instance);
            return instance;
        }
        try {
            T instance = getContentClass().newInstance();
            add(instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (JIPipeValidatable.class.isAssignableFrom(contentClass)) {
            for (int i = 0; i < size(); i++) {
                JIPipeValidatable validatable = (JIPipeValidatable) get(i);
                report.report(new CustomValidationReportContext("Item #" + (i + 1)), validatable);
            }
        }
    }

    @Override
    public String getTextDescription() {
        return JsonUtils.toJsonString(stream().map(JIPipeCustomTextDescriptionParameter::getTextDescriptionOf).collect(Collectors.toList()));
    }

    public Supplier<T> getCustomInstanceGenerator() {
        return customInstanceGenerator;
    }

    public void setCustomInstanceGenerator(Supplier<T> customInstanceGenerator) {
        this.customInstanceGenerator = customInstanceGenerator;
    }

    /**
     * Serializes the parameter
     */
    public static class Serializer extends JsonSerializer<ListParameter<?>> {
        @Override
        public void serialize(ListParameter<?> objects, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartArray();
            for (Object object : objects) {
                jsonGenerator.writeObject(object);
            }
            jsonGenerator.writeEndArray();
        }
    }

    /**
     * Deserializes the parameter
     */
    public static class Deserializer<T> extends JsonDeserializer<ListParameter<T>> implements ContextualDeserializer {

        private JavaType deserializedType;

        @Override
        public ListParameter<T> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode root = jsonParser.readValueAsTree();

            ListParameter<T> listParameter;
            try {
                listParameter = (ListParameter<T>) deserializedType.getRawClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            ObjectReader objectReader = JsonUtils.getObjectMapper().readerFor(listParameter.getContentClass());
            for (JsonNode element : ImmutableList.copyOf(root.elements())) {
                listParameter.add(objectReader.readValue(element));
            }

            return listParameter;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            //beanProperty is null when the type to deserialize is the top-level type or a generic type, not a type of a bean property
            JavaType type = ctxt.getContextualType() != null
                    ? ctxt.getContextualType()
                    : property.getMember().getType();
            Deserializer<?> deserializer = new Deserializer<>();
            deserializer.deserializedType = type;
            return deserializer;
        }
    }
}
