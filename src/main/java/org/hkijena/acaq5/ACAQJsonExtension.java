package org.hkijena.acaq5;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQMetadata;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.events.ExtensionContentAddedEvent;
import org.hkijena.acaq5.api.events.ExtensionContentRemovedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.grouping.JsonAlgorithmDeclaration;
import org.hkijena.acaq5.api.grouping.JsonAlgorithmRegistrationTask;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A JSON-serializable extension
 */
@JsonDeserialize(as = ACAQJsonExtension.class, using = ACAQJsonExtension.Deserializer.class)
public class ACAQJsonExtension implements ACAQDependency, ACAQValidatable {
    private EventBus eventBus = new EventBus();
    private String id;
    private String version = "1.0.0";
    private ACAQMetadata metadata = new ACAQMetadata();
    private Path jsonFilePath;
    private ACAQDefaultRegistry registry;

    private Set<JsonAlgorithmDeclaration> algorithmDeclarations = new HashSet<>();
    private JsonNode serializedJson;

    /**
     * Creates a new instance
     */
    public ACAQJsonExtension() {
    }

    /**
     * Loads a {@link ACAQJsonExtension} from JSON
     *
     * @param jsonData JSON data
     * @return Loaded instance
     */
    public static ACAQJsonExtension loadProject(JsonNode jsonData) {
        try {
            return JsonUtils.getObjectMapper().readerFor(ACAQJsonExtension.class).readValue(jsonData);
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not load JSON plugin.",
                    "ACAQ JSON extension loader", "The plugin file was corrupted, so ACAQ5 does not know how to load some essential information. Or you are using an older ACAQ5 version.",
                    "Try to update ACAQ5. If this does not work, contact the plugin's author.");
        }
    }

    @Override
    @JsonGetter("metadata")
    @ACAQParameter("metadata")
    @ACAQDocumentation(name = "Metadata", description = "Additional extension metadata")
    public ACAQMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets metadata
     *
     * @param metadata Metadata
     */
    @JsonSetter("metadata")
    public void setMetadata(ACAQMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    @JsonGetter("id")
    @ACAQParameter("id")
    @StringParameterSettings(monospace = true)
    @ACAQDocumentation(name = "Unique extension ID", description = "A unique identifier for the extension. It must have following format: " +
            "[Author]:[Extension] where [Author] contains information about who developed the extension. An example is <i>org.hkijena:my-extension</i>")
    public String getDependencyId() {
        return id;
    }

    /**
     * Sets the ID
     *
     * @param id ID
     */
    @JsonSetter("id")
    @ACAQParameter("id")
    public void setId(String id) {
        this.id = id;
    }

    @Override
    @JsonGetter("version")
    @ACAQParameter("version")
    @StringParameterSettings(monospace = true)
    @ACAQDocumentation(name = "Version", description = "The version of this extension. A common format is x.y.z or x.y.z.w")
    public String getDependencyVersion() {
        return version;
    }

    @Override
    public Path getDependencyLocation() {
        return jsonFilePath;
    }

    /**
     * Sets the version
     *
     * @param version Version
     */
    @JsonSetter("version")
    @ACAQParameter("version")
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return The project type
     */
    @JsonGetter("acaq:project-type")
    public String getProjectType() {
        return "json-extension";
    }

    /**
     * @return The dependencies of this extension
     */
    @JsonGetter("dependencies")
    public Set<ACAQDependency> getDependencies() {
        Set<ACAQDependency> result = new HashSet<>();
        for (ACAQAlgorithmDeclaration declaration : getAlgorithmDeclarations()) {
            result.addAll(declaration.getDependencies());
        }
        return result.stream().map(ACAQMutableDependency::new).collect(Collectors.toSet());
    }

    /**
     * @return The JSON file path of this extension. Can return null.
     */
    public Path getJsonFilePath() {
        return jsonFilePath;
    }

    /**
     * @return The registry instance
     */
    public ACAQDefaultRegistry getRegistry() {
        return registry;
    }

    /**
     * Sets the registry instance
     *
     * @param registry The registry
     */
    public void setRegistry(ACAQDefaultRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registers the content.
     * We cannot be sure if there are internal dependencies, so schedule tasks
     */
    public void register() {
        for (JsonNode entry : ImmutableList.copyOf(serializedJson.get("algorithms").elements())) {
            ACAQAlgorithmRegistry.getInstance().scheduleRegister(new JsonAlgorithmRegistrationTask(entry, this));
        }
    }

    /**
     * Saves the extension
     *
     * @param savePath The save path
     * @throws IOException Triggered by {@link com.fasterxml.jackson.databind.ObjectMapper}
     */
    public void saveProject(Path savePath) throws IOException {
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(savePath.toFile(), this);
        jsonFilePath = savePath;
    }

    /**
     * Adds a new algorithm of specified type
     *
     * @param algorithmDeclaration The algorithm type
     */
    public void addAlgorithm(JsonAlgorithmDeclaration algorithmDeclaration) {
        if (algorithmDeclarations == null)
            deserializeAlgorithmDeclarations();
        algorithmDeclarations.add(algorithmDeclaration);
        eventBus.post(new ExtensionContentAddedEvent(this, algorithmDeclaration));
    }

    /**
     * Responsible for deserializing the algorithms
     */
    private void deserializeAlgorithmDeclarations() {
        if (algorithmDeclarations == null) {
            TypeReference<HashSet<JsonAlgorithmDeclaration>> typeReference = new TypeReference<HashSet<JsonAlgorithmDeclaration>>() {
            };
            try {
                algorithmDeclarations = JsonUtils.getObjectMapper().readerFor(typeReference).readValue(serializedJson.get("algorithms"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return Algorithm declarations
     */
    @JsonGetter("algorithms")
    public Set<JsonAlgorithmDeclaration> getAlgorithmDeclarations() {
        if (algorithmDeclarations == null)
            deserializeAlgorithmDeclarations();
        return Collections.unmodifiableSet(algorithmDeclarations);
    }

    /**
     * Sets algorithm declarations
     *
     * @param algorithmDeclarations Declarations
     */
    @JsonSetter("algorithms")
    private void setAlgorithmDeclarations(Set<JsonAlgorithmDeclaration> algorithmDeclarations) {
        this.algorithmDeclarations = algorithmDeclarations;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (StringUtils.isNullOrEmpty(getDependencyId())) {
            report.forCategory("ID").reportIsInvalid("The ID is empty!",
                    "A JSON extension must be identified with a unique ID to allow ACAQ5 to find dependencies.",
                    " Please provide a valid ID.",
                    this);
        } else if (!getDependencyId().contains(":")) {
            report.forCategory("ID").reportIsInvalid("Malformed ID!",
                    "The ID should contain some information about the plugin author (organization, ...) to prevent future collisions.",
                    "The ID must have following structure: <Organization>:<Name> e.g. org.hkijena.acaq5:my-plugin",
                    this);
        }
        if (StringUtils.isNullOrEmpty(getDependencyVersion())) {
            report.forCategory("Version").reportIsInvalid("The version is empty!",
                    "This allows users of your extension to better get help if issues arise.",
                    "Please provide a valid version number. It has usually following format x.y.z.w",
                    this);
        }
        if (StringUtils.isNullOrEmpty(getMetadata().getName()) || "New project".equals(getMetadata().getName())) {
            report.forCategory("Name").reportIsInvalid("Invalid name!",
                    "Your plugin should have a short and meaningful name.",
                    "Please provide a meaningful name for your plugin.",
                    this);
        }
        if (algorithmDeclarations == null)
            deserializeAlgorithmDeclarations();
        for (JsonAlgorithmDeclaration declaration : algorithmDeclarations) {
            report.forCategory("Algorithms").forCategory(declaration.getName()).report(declaration);
        }
        if (algorithmDeclarations.size() != algorithmDeclarations.stream().map(JsonAlgorithmDeclaration::getId).collect(Collectors.toSet()).size()) {
            report.forCategory("Algorithms").reportIsInvalid("Duplicate IDs found!",
                    "Algorithm IDs must be unique",
                    "Please make sure that IDs are unique.",
                    this);
        }
    }

    /**
     * Removes an algorithm
     *
     * @param declaration Algorithm type
     */
    public void removeAlgorithm(JsonAlgorithmDeclaration declaration) {
        if (algorithmDeclarations == null)
            deserializeAlgorithmDeclarations();
        if (algorithmDeclarations.remove(declaration)) {
            eventBus.post(new ExtensionContentRemovedEvent(this, declaration));
        }
    }

    /**
     * Deserializer for the {@link ACAQJsonExtension}.
     * This is a special deserializer that leaves the algorithmDeclarations field alone, as we cannot be sure if algorithmDeclarations is deserialized
     * during registration (leading to missing algorithm Ids)
     */
    public static class Deserializer extends JsonDeserializer<ACAQJsonExtension> {
        @Override
        public ACAQJsonExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ACAQJsonExtension extension = new ACAQJsonExtension();
            extension.algorithmDeclarations = null;
            JsonNode node = jsonParser.readValueAsTree();

            extension.serializedJson = node;
            extension.id = node.get("id").textValue();
            extension.version = node.get("version").textValue();
            extension.metadata = JsonUtils.getObjectMapper().readerFor(ACAQMetadata.class).readValue(node.get("metadata"));

            return extension;
        }
    }
}
