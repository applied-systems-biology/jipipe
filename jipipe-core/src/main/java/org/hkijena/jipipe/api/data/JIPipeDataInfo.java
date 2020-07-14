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
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    private JIPipeDataInfo(Class<? extends JIPipeData> dataClass) {
        this.dataClass = dataClass;
        this.name = JIPipeData.getNameOf(dataClass);
        this.description = JIPipeData.getDescriptionOf(dataClass);
        this.menuPath = JIPipeData.getMenuPathOf(dataClass);
        this.hidden = JIPipeData.isHidden(dataClass);
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
        return JIPipeDatatypeRegistry.getInstance().getIdOf(dataClass);
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
        return JIPipeDataInfo.getInstance(JIPipeDatatypeRegistry.getInstance().getById(id));
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
            return JIPipeDataInfo.getInstance(JIPipeDatatypeRegistry.getInstance().getById(jsonParser.getValueAsString()));
        }
    }

    /**
     * Deserializer for a Map key
     */
    public static class KeyDeserializer extends com.fasterxml.jackson.databind.KeyDeserializer {
        @Override
        public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            return JIPipeDataInfo.getInstance(JIPipeDatatypeRegistry.getInstance().getById(s));
        }
    }
}
