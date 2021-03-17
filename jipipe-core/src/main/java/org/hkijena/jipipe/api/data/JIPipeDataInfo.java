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

package org.hkijena.jipipe.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.io.IOException;
import java.util.*;

/**
 * Compound type that contains all metadata of an {@link JIPipeData} type
 */
@JsonSerialize(using = JIPipeDataInfo.Serializer.class, keyUsing = JIPipeDataInfo.Serializer.class)
@JsonDeserialize(using = JIPipeDataInfo.Deserializer.class, keyUsing = JIPipeDataInfo.KeyDeserializer.class)
public class JIPipeDataInfo implements Comparable<JIPipeDataInfo> {
    private static Map<Class<? extends JIPipeData>, JIPipeDataInfo> cache = new HashMap<>();

    private Class<? extends JIPipeData> dataClass;
    private String name;
    private String description;
    private String menuPath;
    private boolean hidden;
    private boolean heavy;
    private HTMLText storageDocumentation;
    private List<String> additionalCitations = new ArrayList<>();

    private JIPipeDataInfo(Class<? extends JIPipeData> dataClass) {
        this.dataClass = dataClass;
        this.name = JIPipeData.getNameOf(dataClass);
        this.description = JIPipeData.getDescriptionOf(dataClass);
        this.menuPath = JIPipeData.getMenuPathOf(dataClass);
        this.hidden = JIPipeData.isHidden(dataClass);
        this.heavy = JIPipeData.isHeavy(dataClass);
        this.storageDocumentation = JIPipeData.getStorageDocumentation(dataClass);
        // Load additional citations
        for (JIPipeCitation citation : dataClass.getAnnotationsByType(JIPipeCitation.class)) {
            getAdditionalCitations().add(citation.value());
        }
    }

    /**
     * Returns a {@link JIPipeDataInfo} instance for the data class.
     * Does not require the data type to be registered.
     * Instances are cached.
     *
     * @param klass The data class
     * @return The info instance
     */
    public static JIPipeDataInfo getInstance(Class<? extends JIPipeData> klass) {
        JIPipeDataInfo info = cache.getOrDefault(klass, null);
        if (info == null) {
            info = new JIPipeDataInfo(klass);
            cache.put(klass, info);
        }
        return info;
    }

    /**
     * Returns a {@link JIPipeDataInfo} instance for the data type ID.
     * Requires that the data type ID is registered.
     * Instances are cached.
     *
     * @param id Data type ID
     * @return The info instance
     */
    public static JIPipeDataInfo getInstance(String id) {
        return JIPipeDataInfo.getInstance(JIPipe.getDataTypes().getById(id));
    }

    /**
     * @return The data class
     */
    public Class<? extends JIPipeData> getDataClass() {
        return dataClass;
    }

    /**
     * @return Name of the data type
     */
    public String getName() {
        return name;
    }

    /**
     * @return Description of the data type
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return Menu path of the data type
     */
    public String getMenuPath() {
        return menuPath;
    }

    /**
     * @return if the data type should be hidden
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @return if the data type is considered heavy
     */
    public boolean isHeavy() {
        return heavy;
    }

    public HTMLText getStorageDocumentation() {
        return storageDocumentation;
    }

    /**
     * A list of additional citations
     *
     * @return additional citations
     */
    public List<String> getAdditionalCitations() {
        return additionalCitations;
    }

    public void setAdditionalCitations(List<String> additionalCitations) {
        this.additionalCitations = additionalCitations;
    }

    @Override
    public int compareTo(JIPipeDataInfo o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeDataInfo that = (JIPipeDataInfo) o;
        return Objects.equals(dataClass, that.dataClass);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataClass);
    }

    /**
     * @return The data type ID
     */
    public String getId() {
        return JIPipe.getDataTypes().getIdOf(dataClass);
    }

    /**
     * Creates a new instance
     *
     * @param args arguments passed to the data type constructor
     * @return the instance
     */
    public JIPipeData newInstance(Object... args) {
        return (JIPipeData) ReflectionUtils.newInstance(dataClass, args);
    }

    /**
     * Serializes a info as data type ID
     */
    public static class Serializer extends JsonSerializer<JIPipeDataInfo> {
        @Override
        public void serialize(JIPipeDataInfo info, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(info.getId());
        }
    }

    /**
     * Deserializes a info from a data type ID.
     * Requires that the ID is registered.
     */
    public static class Deserializer extends JsonDeserializer<JIPipeDataInfo> {
        @Override
        public JIPipeDataInfo deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            return JIPipeDataInfo.getInstance(JIPipe.getDataTypes().getById(jsonParser.getValueAsString()));
        }
    }

    /**
     * Deserializer for a Map key
     */
    public static class KeyDeserializer extends com.fasterxml.jackson.databind.KeyDeserializer {
        @Override
        public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            return JIPipeDataInfo.getInstance(JIPipe.getDataTypes().getById(s));
        }
    }
}
