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

package org.hkijena.jipipe.extensions.parameters.functions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;

/**
 * A parameter that allows to model a function with an input, an output, and parameters.
 * This can be used if {@link PairParameter} is not sufficient
 */
@JsonSerialize(using = FunctionParameter.Serializer.class)
@JsonDeserialize(using = FunctionParameter.Deserializer.class)
public abstract class FunctionParameter<I, P, O> implements JIPipeValidatable {
    private Class<I> inputClass;
    private Class<P> parameterClass;
    private Class<O> outputClass;
    private I input;
    private P parameter;
    private O output;


    /**
     * Creates a new instance
     *
     * @param inputClass     the input class
     * @param parameterClass the parameter class
     * @param outputClass    the output class
     */
    public FunctionParameter(Class<I> inputClass, Class<P> parameterClass, Class<O> outputClass) {
        this.inputClass = inputClass;
        this.parameterClass = parameterClass;
        this.outputClass = outputClass;
    }

    public FunctionParameter(FunctionParameter<I, P, O> other) {
        this.inputClass = other.inputClass;
        this.parameterClass = other.parameterClass;
        this.outputClass = other.outputClass;
        this.input = other.input;
        this.parameter = other.parameter;
        this.output = other.output;
    }


    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (input instanceof JIPipeValidatable)
            report.resolve("Input").report((JIPipeValidatable) input);
        if (parameter instanceof JIPipeValidatable)
            report.resolve("Parameter").report((JIPipeValidatable) parameter);
        if (output instanceof JIPipeValidatable)
            report.resolve("Output").report((JIPipeValidatable) output);
    }

    public I getInput() {
        return input;
    }

    public void setInput(I input) {
        this.input = input;
    }

    public P getParameter() {
        return parameter;
    }

    public void setParameter(P parameter) {
        this.parameter = parameter;
    }

    public O getOutput() {
        return output;
    }

    public void setOutput(O output) {
        this.output = output;
    }

    public Class<I> getInputClass() {
        return inputClass;
    }

    public Class<P> getParameterClass() {
        return parameterClass;
    }

    public Class<O> getOutputClass() {
        return outputClass;
    }

    /**
     * @return The name used for the input in the UI
     */
    public String renderInputName() {
        return "Input";
    }

    /**
     * @return The name used for the parameters in the UI
     */
    public String renderParameterName() {
        return "Settings";
    }

    /**
     * @return The name used for the output in the UI
     */
    public String renderOutputName() {
        return "Output";
    }

    @Override
    public String toString() {
        return getInput() + " -> " + getParameter() + " -> " + getOutput();
    }

    /**
     * Serializes the parameter
     */
    public static class Serializer extends JsonSerializer<FunctionParameter<?, ?, ?>> {
        @Override
        public void serialize(FunctionParameter<?, ?, ?> objects, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("input", objects.input);
            jsonGenerator.writeObjectField("parameter", objects.parameter);
            jsonGenerator.writeObjectField("output", objects.output);
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes the parameter
     */
    public static class Deserializer<I, P, O> extends JsonDeserializer<FunctionParameter<?, ?, ?>> implements ContextualDeserializer {

        private JavaType deserializedType;

        @Override
        public FunctionParameter<I, P, O> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode root = jsonParser.readValueAsTree();
            FunctionParameter<I, P, O> keyValuePairParameter;
            try {
                keyValuePairParameter = (FunctionParameter<I, P, O>) deserializedType.getRawClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            keyValuePairParameter.input = JsonUtils.getObjectMapper().readerFor(keyValuePairParameter.getInputClass()).readValue(root.get("input"));
            keyValuePairParameter.parameter = JsonUtils.getObjectMapper().readerFor(keyValuePairParameter.getParameterClass()).readValue(root.get("parameter"));
            keyValuePairParameter.output = JsonUtils.getObjectMapper().readerFor(keyValuePairParameter.getOutputClass()).readValue(root.get("output"));

            return keyValuePairParameter;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            //beanProperty is null when the type to deserialize is the top-level type or a generic type, not a type of a bean property
            JavaType type = ctxt.getContextualType() != null
                    ? ctxt.getContextualType()
                    : property.getMember().getType();
            Deserializer<I, P, O> deserializer = new Deserializer<>();
            deserializer.deserializedType = type;
            return deserializer;
        }
    }
}
