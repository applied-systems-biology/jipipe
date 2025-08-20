package org.hkijena.jipipe.contrib.ro_crate.entities.contextual;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.hkijena.jipipe.contrib.ro_crate.Crate;
import org.hkijena.jipipe.contrib.ro_crate.objectmapper.MyObjectMapper;
import org.hkijena.jipipe.contrib.ro_crate.special.CrateVersion;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class JsonDescriptor extends ContextualEntity {

    public static final String ID = "ro-crate-metadata.json";
    protected static final String CONFORMS_TO = "conformsTo";

    /**
     * Returns a JsonDescriptor with the conformsTo value set to the latest stable
     * version.
     */
    public JsonDescriptor() {
        super(
                staticPropertiesPrefilledBuilder()
                        .addIdProperty(CONFORMS_TO, CrateVersion.LATEST_STABLE.conformsTo));
    }

    private JsonDescriptor(ContextualEntityBuilder builder) {
        super(builder);
    }

    protected static ContextualEntityBuilder staticPropertiesPrefilledBuilder() {
        return new ContextualEntity.ContextualEntityBuilder()
                .setId(ID)
                .addType("CreativeWork")
                .addIdProperty("about", "./");
    }

    /**
     * Builder for the JsonDescriptor.
     * <p>
     * Defaults to the latest stable crate version and no other conformsTo values.
     */
    public static final class Builder {
        CrateVersion version = CrateVersion.LATEST_STABLE;
        Set<String> otherConformsToValues = new HashSet<>();

        public Builder() {
            // default
        }

        public Builder(Crate crate) {
            crate.getVersion().ifPresent(v -> this.version = v);
            this.otherConformsToValues.addAll(crate.getProfiles());
        }

        public Builder setVersion(CrateVersion version) {
            this.version = version;
            return this;
        }

        public Builder addConformsTo(String uri) {
            this.otherConformsToValues.add(uri);
            return this;
        }

        public Builder addConformsTo(URI uri) {
            this.otherConformsToValues.add(uri.toString());
            return this;
        }

        public JsonDescriptor build() {
            if (this.otherConformsToValues.isEmpty()) {
                // in this case we do not need an array
                ContextualEntityBuilder entityBuilder = staticPropertiesPrefilledBuilder()
                        .addIdProperty(CONFORMS_TO, this.version.conformsTo);
                return new JsonDescriptor(entityBuilder);
            } else {
                // use an array to collect all values into an array of simple @id objects.
                ObjectMapper mapper = MyObjectMapper.getMapper();
                ArrayNode array = mapper.createArrayNode();
                array.add(
                        mapper.createObjectNode().put("@id", this.version.conformsTo));
                for (String other : this.otherConformsToValues) {
                    array.add(
                            mapper.createObjectNode().put("@id", other));
                }
                ContextualEntityBuilder entityBuilder = staticPropertiesPrefilledBuilder()
                        .addProperty(CONFORMS_TO, array);
                return new JsonDescriptor(entityBuilder);
            }
        }
    }
}
