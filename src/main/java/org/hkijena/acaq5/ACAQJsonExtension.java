package org.hkijena.acaq5;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.events.ExtensionContentAddedEvent;
import org.hkijena.acaq5.api.events.ExtensionContentRemovedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQJsonTraitRegistrationTask;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQMutableTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.parameters.editors.StringParameterSettings;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A JSON-serializable extension
 */
@JsonDeserialize(as = ACAQJsonExtension.class)
public class ACAQJsonExtension implements ACAQDependency, ACAQValidatable {
    private EventBus eventBus = new EventBus();
    private String id;
    private String version;
    private ACAQProjectMetadata metadata = new ACAQProjectMetadata();
    private Path jsonFilePath;
    private ACAQDefaultRegistry registry;

    private Set<GraphWrapperAlgorithmDeclaration> algorithmDeclarations = new HashSet<>();
    private Set<ACAQJsonTraitDeclaration> traitDeclarations = new HashSet<>();

    /**
     * Creates a new instance
     */
    public ACAQJsonExtension() {
    }

    @Override
    @JsonGetter("metadata")
    @ACAQParameter("metadata")
    @ACAQDocumentation(name = "Metadata", description = "Additional extension metadata")
    public ACAQProjectMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets metadata
     *
     * @param metadata Metadata
     */
    @JsonSetter("metadata")
    public void setMetadata(ACAQProjectMetadata metadata) {
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
    @ACAQDocumentation(name = "ID", description = "A unique identifier")
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
    @ACAQDocumentation(name = "Version", description = "The version of this extension")
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
        for (ACAQAlgorithmDeclaration declaration : algorithmDeclarations) {
            result.addAll(declaration.getDependencies());
        }
        for (ACAQTraitDeclaration declaration : traitDeclarations) {
            for (ACAQDependency dependency : declaration.getDependencies()) {
                if (!Objects.equals(dependency.getDependencyId(), getDependencyId())) {
                    result.add(dependency);
                }
            }
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
     * Registers the content
     */
    public void register() {
        for (ACAQJsonTraitDeclaration declaration : traitDeclarations) {
            // There can be internal dependencies; we require a scheduler task
            ACAQTraitRegistry.getInstance().scheduleRegister(new ACAQJsonTraitRegistrationTask(declaration, this));
        }
        for (GraphWrapperAlgorithmDeclaration declaration : algorithmDeclarations) {
            // No internal dependencies: The plugin system can handle this
            ACAQAlgorithmRegistry.getInstance().register(declaration, this);
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
    public void addAlgorithm(GraphWrapperAlgorithmDeclaration algorithmDeclaration) {
        algorithmDeclarations.add(algorithmDeclaration);
        eventBus.post(new ExtensionContentAddedEvent(this, algorithmDeclaration));
    }

    /**
     * Adds a new trait fo specified type
     *
     * @param traitDeclaration The trait type
     */
    public void addTrait(ACAQJsonTraitDeclaration traitDeclaration) {
        traitDeclarations.add(traitDeclaration);
        eventBus.post(new ExtensionContentAddedEvent(this, traitDeclaration));
    }

    /**
     * @return Algorithm declarations
     */
    @JsonGetter("algorithms")
    public Set<GraphWrapperAlgorithmDeclaration> getAlgorithmDeclarations() {
        return Collections.unmodifiableSet(algorithmDeclarations);
    }

    /**
     * Sets algorithm declarations
     *
     * @param algorithmDeclarations Declarations
     */
    @JsonSetter("algorithms")
    private void setAlgorithmDeclarations(Set<GraphWrapperAlgorithmDeclaration> algorithmDeclarations) {
        this.algorithmDeclarations = algorithmDeclarations;
    }

    /**
     * Gets trait types
     *
     * @return Trait types
     */
    @JsonGetter("annotations")
    public Set<ACAQJsonTraitDeclaration> getTraitDeclarations() {
        return Collections.unmodifiableSet(traitDeclarations);
    }

    /**
     * Sets trait types
     *
     * @param traitDeclarations Trait types
     */
    @JsonSetter("annotations")
    private void setTraitDeclarations(Set<ACAQJsonTraitDeclaration> traitDeclarations) {
        this.traitDeclarations = traitDeclarations;
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
        for (ACAQJsonTraitDeclaration declaration : traitDeclarations) {
            report.forCategory("Annotations").forCategory(declaration.getName()).report(declaration);
        }
        if (traitDeclarations.size() != traitDeclarations.stream().map(ACAQMutableTraitDeclaration::getId).collect(Collectors.toSet()).size()) {
            report.forCategory("Annotations").reportIsInvalid("Duplicate IDs found!",
                    "Annotation IDs must be unique.",
                    "Please make sure that IDs are unique.",
                    this);
        }
        for (GraphWrapperAlgorithmDeclaration declaration : algorithmDeclarations) {
            report.forCategory("Algorithms").forCategory(declaration.getName()).report(declaration);
        }
        if (algorithmDeclarations.size() != algorithmDeclarations.stream().map(GraphWrapperAlgorithmDeclaration::getId).collect(Collectors.toSet()).size()) {
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
    public void removeAlgorithm(GraphWrapperAlgorithmDeclaration declaration) {
        if (algorithmDeclarations.remove(declaration)) {
            eventBus.post(new ExtensionContentRemovedEvent(this, declaration));
        }
    }

    /**
     * Removes a trait type
     *
     * @param declaration Trait type
     */
    public void removeAnnotation(ACAQJsonTraitDeclaration declaration) {
        if (traitDeclarations.remove(declaration)) {
            eventBus.post(new ExtensionContentRemovedEvent(this, declaration));
        }
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
}
