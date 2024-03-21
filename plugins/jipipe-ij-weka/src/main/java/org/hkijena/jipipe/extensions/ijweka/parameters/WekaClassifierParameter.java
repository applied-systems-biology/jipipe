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

package org.hkijena.jipipe.extensions.ijweka.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import hr.irb.fastRandomForest.FastRandomForest;
import org.hkijena.jipipe.utils.SerializationUtils;
import weka.classifiers.Classifier;
import weka.gui.GenericObjectEditor;

import java.io.IOException;

/**
 * A parameter type that stores a Weka classifier
 */
@JsonSerialize(using = WekaClassifierParameter.Serializer.class)
@JsonDeserialize(using = WekaClassifierParameter.Deserializer.class)
public class WekaClassifierParameter {
    private Classifier classifier = new FastRandomForest();


    public WekaClassifierParameter() {
    }

    public WekaClassifierParameter(WekaClassifierParameter other) {
        try {
            this.classifier = (Classifier) GenericObjectEditor.makeCopy(other.classifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Classifier getClassifier() {
        return classifier;
    }

    public void setClassifier(Classifier classifier) {
        this.classifier = classifier;
    }

    public static class Serializer extends JsonSerializer<WekaClassifierParameter> {
        @Override
        public void serialize(WekaClassifierParameter value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("serialized-base64", SerializationUtils.objectToBase64(value.classifier));
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<WekaClassifierParameter> {
        @Override
        public WekaClassifierParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.readValueAsTree();
            String base64 = node.get("serialized-base64").textValue();
            WekaClassifierParameter parameter = new WekaClassifierParameter();
            parameter.classifier = (Classifier) SerializationUtils.base64ToObject(base64);
            return parameter;
        }
    }
}
