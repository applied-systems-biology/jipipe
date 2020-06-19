package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Compound type that contains all metadata of an {@link ACAQData} type
 */
@JsonSerialize(using = ACAQDataDeclaration.Serializer.class, keyUsing = ACAQDataDeclaration.Serializer.class)
@JsonDeserialize(using = ACAQDataDeclaration.Deserializer.class, keyUsing = ACAQDataDeclaration.KeyDeserializer.class)
public class ACAQDataDeclaration implements Comparable<ACAQDataDeclaration> {
    private static Map<Class<? extends ACAQData>, ACAQDataDeclaration> cache = new HashMap<>();

    private Class<? extends ACAQData> dataClass;
    private String name;
    private String description;
    private String menuPath;
    private boolean hidden;

    private ACAQDataDeclaration(Class<? extends ACAQData> dataClass) {
        this.dataClass = dataClass;
        this.name = ACAQData.getNameOf(dataClass);
        this.description = ACAQData.getDescriptionOf(dataClass);
        this.menuPath = ACAQData.getMenuPathOf(dataClass);
        this.hidden = ACAQData.isHidden(dataClass);
    }

    /**
     * Returns a {@link ACAQDataDeclaration} instance for the data class.
     * Does not require the data type to be registered.
     * Instances are cached.
     *
     * @param klass The data class
     * @return The declaration instance
     */
    public static ACAQDataDeclaration getInstance(Class<? extends ACAQData> klass) {
        ACAQDataDeclaration declaration = cache.getOrDefault(klass, null);
        if (declaration == null) {
            declaration = new ACAQDataDeclaration(klass);
            cache.put(klass, declaration);
        }
        return declaration;
    }

    /**
     * Returns a {@link ACAQDataDeclaration} instance for the data type ID.
     * Requires that the data type ID is registered.
     * Instances are cached.
     *
     * @param id Data type ID
     * @return The declaration instance
     */
    public static ACAQDataDeclaration getInstance(String id) {
        return ACAQDataDeclaration.getInstance(ACAQDatatypeRegistry.getInstance().getById(id));
    }

    /**
     * @return The data class
     */
    public Class<? extends ACAQData> getDataClass() {
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
    public int compareTo(ACAQDataDeclaration o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ACAQDataDeclaration that = (ACAQDataDeclaration) o;
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
        return ACAQDatatypeRegistry.getInstance().getIdOf(dataClass);
    }

    /**
     * Serializes a declaration as data type ID
     */
    public static class Serializer extends JsonSerializer<ACAQDataDeclaration> {
        @Override
        public void serialize(ACAQDataDeclaration declaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(declaration.getId());
        }
    }

    /**
     * Deserializes a declaration from a data type ID.
     * Requires that the ID is registered.
     */
    public static class Deserializer extends JsonDeserializer<ACAQDataDeclaration> {
        @Override
        public ACAQDataDeclaration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            return ACAQDataDeclaration.getInstance(ACAQDatatypeRegistry.getInstance().getById(jsonParser.getValueAsString()));
        }
    }

    /**
     * Deserializer for a Map key
     */
    public static class KeyDeserializer extends com.fasterxml.jackson.databind.KeyDeserializer {
        @Override
        public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            return ACAQDataDeclaration.getInstance(ACAQDatatypeRegistry.getInstance().getById(s));
        }
    }
}
