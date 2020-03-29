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

    public static ACAQDataDeclaration getInstance(Class<? extends ACAQData> klass) {
        ACAQDataDeclaration declaration = cache.getOrDefault(klass, null);
        if (declaration == null) {
            declaration = new ACAQDataDeclaration(klass);
            cache.put(klass, declaration);
        }
        return declaration;
    }

    public static ACAQDataDeclaration getInstance(String id) {
        return ACAQDataDeclaration.getInstance(ACAQDatatypeRegistry.getInstance().getById(id));
    }

    public Class<? extends ACAQData> getDataClass() {
        return dataClass;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMenuPath() {
        return menuPath;
    }

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
    public int hashCode() {
        return Objects.hash(dataClass);
    }

    public String getId() {
        return ACAQDatatypeRegistry.getInstance().getIdOf(dataClass);
    }

    public static class Serializer extends JsonSerializer<ACAQDataDeclaration> {
        @Override
        public void serialize(ACAQDataDeclaration declaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(declaration.getId());
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQDataDeclaration> {
        @Override
        public ACAQDataDeclaration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            return ACAQDataDeclaration.getInstance(ACAQDatatypeRegistry.getInstance().getById(jsonParser.getValueAsString()));
        }
    }

    public static class KeyDeserializer extends com.fasterxml.jackson.databind.KeyDeserializer {
        @Override
        public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            return ACAQDataDeclaration.getInstance(ACAQDatatypeRegistry.getInstance().getById(s));
        }
    }
}
