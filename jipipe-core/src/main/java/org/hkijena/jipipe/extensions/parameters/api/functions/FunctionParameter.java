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

package org.hkijena.jipipe.extensions.parameters.api.functions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.api.validation.causes.CustomReportEntryCause;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * A parameter that allows to model a function with an input, an output, and parameters.
 * This can be used if {@link PairParameter} is not sufficient
 * We suggest to use {@link org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList} if applicable, due to its greater flexibility.
 */
@JsonSerialize(using = FunctionParameter.Serializer.class)
@JsonDeserialize(using = FunctionParameter.Deserializer.class)
public abstract class FunctionParameter<I, P, O> implements JIPipeValidatable {
    private final Class<I> inputClass;
    private final Class<P> parameterClass;
    private final Class<O> outputClass;
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
    public void reportValidity(JIPipeValidationReportEntryCause parentCause, JIPipeValidationReport report) {
        if (input instanceof JIPipeValidatable) {
            report.report(new CustomReportEntryCause(parentCause, "Input"), (JIPipeValidatable) input);
        }
        if (parameter instanceof JIPipeValidatable) {
            report.report(new CustomReportEntryCause(parentCause, "Parameter"), (JIPipeValidatable) parameter);
        }
        if (output instanceof JIPipeValidatable) {
            report.report(new CustomReportEntryCause(parentCause, "Output"), (JIPipeValidatable) output);
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionParameter<?, ?, ?> that = (FunctionParameter<?, ?, ?>) o;
        return Objects.equals(input, that.input) && Objects.equals(parameter, that.parameter) && Objects.equals(output, that.output);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, parameter, output);
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
