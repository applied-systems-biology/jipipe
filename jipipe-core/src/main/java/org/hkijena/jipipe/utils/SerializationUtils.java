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

package org.hkijena.jipipe.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Utils for serializing objects
 */
public class SerializationUtils {
    /**
     * Serializes an object to a JSON string
     * Must be serializable via the Jackson JSON library.
     *
     * @param obj the object
     * @return the JSON string
     */
    public static String objectToJsonString(Object obj) {
        return JsonUtils.toJsonString(obj);
    }

    /**
     * Deserailizes a JSON string into an object via Jackson
     *
     * @param json  the JSON string
     * @param klass the object class
     * @param <T>   the object class
     * @return the object
     */
    public static <T> T jsonStringToObject(String json, Class<T> klass) {
        return JsonUtils.readFromString(json, klass);
    }

    /**
     * Serializes a {@link JIPipeParameterCollection} into a JSON string
     *
     * @param parameterCollection the parameter collection
     * @return the JSON string
     */
    public static String parameterCollectionToJsonString(JIPipeParameterCollection parameterCollection) {
        JsonFactory factory = JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().getFactory();
        try (StringWriter writer = new StringWriter()) {
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
     *
     * @param target the target collection
     * @param json   the JSON string
     */
    public static void jsonStringToParameterCollection(JIPipeParameterCollection target, String json) {
        JsonNode node = jsonStringToObject(json, JsonNode.class);
        ParameterUtils.deserializeParametersFromJson(target, node, new UnspecifiedValidationReportContext(), new JIPipeValidationReport());
    }

    /**
     * Serializes a Java object into a file via {@link ObjectOutputStream}
     *
     * @param obj  the object
     * @param file the file
     */
    public static void objectToBinaryFile(Object obj, Path file) {
        try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos);
            objectOutputStream.writeObject(obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserializes a Java object from a binary file via {@link ObjectInputStream}
     *
     * @param file the file
     * @return the object
     */
    public static Object binaryFileToObject(Path file) {
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            ObjectInputStream objectInputStream = new ObjectInputStream(fis);
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serializes a Java object into its binary representation (via {@link ObjectOutputStream}) as base64
     *
     * @param obj the object
     * @return the base64 string
     */

    public static String objectToBase64(Object obj) {
        String base64;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(bos);
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            byte[] objectBytes = bos.toByteArray();
            base64 = Base64.getEncoder().encodeToString(objectBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return base64;
    }

    /**
     * Deserializes a Java object from its binary representation (as base64 string)
     *
     * @param base64 the base64 string
     * @return the object
     */
    public static Object base64ToObject(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
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
