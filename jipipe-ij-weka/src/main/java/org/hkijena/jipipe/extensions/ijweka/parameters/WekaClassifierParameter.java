package org.hkijena.jipipe.extensions.ijweka.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import hr.irb.fastRandomForest.FastRandomForest;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.gui.GenericObjectEditor;

import java.io.*;

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
            String base64;
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bos);
                objectOutputStream.writeObject(value.classifier);
                objectOutputStream.flush();
                objectOutputStream.close();
                byte[] objectBytes = bos.toByteArray();
                BASE64Encoder encoder = new BASE64Encoder();
                base64 = encoder.encode(objectBytes);
            }
            gen.writeStringField("serialized-base64", base64);
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<WekaClassifierParameter> {
        @Override
        public WekaClassifierParameter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.readValueAsTree();
            String base64 = node.get("serialized-base64").textValue();
            WekaClassifierParameter parameter = new WekaClassifierParameter();
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] bytes = decoder.decodeBuffer(base64);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
                ObjectInputStream ois = new ObjectInputStream(bis);
                parameter.classifier = (AbstractClassifier) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            return parameter;
        }
    }
}
