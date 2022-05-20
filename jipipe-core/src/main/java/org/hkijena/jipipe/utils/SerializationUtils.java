package org.hkijena.jipipe.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.json.JsonUtils;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;

/**
 * Utils for serializing objects
 */
public class SerializationUtils {
    /**
     * Serializes an object to a JSON string
     * Must be serializable via the Jackson JSON library.
     * @param obj the object
     * @return the JSON string
     */
    public static String objectToJsonString(Object obj) {
        return JsonUtils.toJsonString(obj);
    }

    /**
     * Deserailizes a JSON string into an object via Jackson
     * @param json the JSON string
     * @param klass the object class
     * @return the object
     * @param <T> the object class
     */
    public static <T> T jsonStringToObject(String json, Class<T> klass) {
        return JsonUtils.readFromString(json, klass);
    }

    /**
     * Serializes a {@link JIPipeParameterCollection} into a JSON string
     * @param parameterCollection the parameter collection
     * @return the JSON string
     */
    public static String parameterCollectionToJsonString(JIPipeParameterCollection parameterCollection) {
        JsonFactory factory = JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().getFactory();
        try(StringWriter writer = new StringWriter()) {
            JsonGenerator generator = factory.createGenerator(writer);
            ParameterUtils.serializeParametersToJson(parameterCollection, generator);
            writer.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserializes JSON data into a {@link JIPipeParameterCollection}
     * @param target the target collection
     * @param json the JSON string
     */
    public static void jsonStringToParameterCollection(JIPipeParameterCollection target, String json) {
        JsonNode node = jsonStringToObject(json, JsonNode.class);
        ParameterUtils.deserializeParametersFromJson(target, node, new JIPipeIssueReport());
    }

    /**
     * Serializes a Java object into its binary representation (via {@link ObjectOutputStream}) as base64
     * @param obj the object
     * @return the base64 string
     */
    public static String objectToBase64(Object obj) {
        String base64;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(bos);
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            objectOutputStream.close();
            byte[] objectBytes = bos.toByteArray();
            BASE64Encoder encoder = new BASE64Encoder();
            base64 = encoder.encode(objectBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return base64;
    }

    /**
     * Deserializes a Java object from its binary representation (as base64 string)
     * @param base64 the base64 string
     * @return the object
     */
    public static Object base64ToObject(String base64) {
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            byte[] bytes = decoder.decodeBuffer(base64);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
                ObjectInputStream ois = new ObjectInputStream(bis);
                return ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}