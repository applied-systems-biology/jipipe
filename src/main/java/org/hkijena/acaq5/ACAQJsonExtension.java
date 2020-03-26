package org.hkijena.acaq5;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.events.ExtensionContentAddedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQSubParameters;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQJsonTraitRegistrationTask;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.extensions.standardalgorithms.api.registries.GraphWrapperAlgorithmRegistrationTask;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    public ACAQJsonExtension() {
    }

    @Override
    @JsonGetter("metadata")
    @ACAQSubParameters("metadata")
    @ACAQDocumentation(name = "Metadata", description = "Additional extension metadata")
    public ACAQProjectMetadata getMetadata() {
        return metadata;
    }

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
    @ACAQDocumentation(name = "ID", description = "A unique identifier")
    public String getDependencyId() {
        return id;
    }

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

    @JsonSetter("version")
    @ACAQParameter("version")
    public void setVersion(String version) {
        this.version = version;
    }

    @JsonGetter("acaq:project-type")
    public String getProjectType() {
        return "json-extension";
    }

    @JsonGetter("dependencies")
    public Set<ACAQDependency> getDependencies() {
        Set<ACAQDependency> result = new HashSet<>();
        for (ACAQAlgorithmDeclaration declaration : algorithmDeclarations) {
            result.addAll(declaration.getDependencies());
        }
        for (ACAQTraitDeclaration declaration : traitDeclarations) {
            for (ACAQDependency dependency : declaration.getDependencies()) {
                if(!Objects.equals(dependency.getDependencyId(), getDependencyId())) {
                    result.add(dependency);
                }
            }
        }
        return result;
    }

    public Path getJsonFilePath() {
        return jsonFilePath;
    }

    public ACAQDefaultRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(ACAQDefaultRegistry registry) {
        this.registry = registry;
    }

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

    public void saveProject(Path savePath) throws IOException {
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(savePath.toFile(), this);
        jsonFilePath = savePath;
    }

    public void addAlgorithm(GraphWrapperAlgorithmDeclaration algorithmDeclaration) {
        algorithmDeclarations.add(algorithmDeclaration);
        eventBus.post(new ExtensionContentAddedEvent(this, algorithmDeclaration));
    }

    public void addTrait(ACAQJsonTraitDeclaration traitDeclaration) {
        traitDeclarations.add(traitDeclaration);
        eventBus.post(new ExtensionContentAddedEvent(this, traitDeclaration));
    }

    @JsonGetter("algorithms")
    public Set<GraphWrapperAlgorithmDeclaration> getAlgorithmDeclarations() {
        return Collections.unmodifiableSet(algorithmDeclarations);
    }

    @JsonSetter("algorithms")
    private void setAlgorithmDeclarations(Set<GraphWrapperAlgorithmDeclaration> algorithmDeclarations) {
        this.algorithmDeclarations = algorithmDeclarations;
    }

    @JsonGetter("annotations")
    public Set<ACAQJsonTraitDeclaration> getTraitDeclarations() {
        return Collections.unmodifiableSet(traitDeclarations);
    }

    @JsonSetter("annotations")
    private void setTraitDeclarations(Set<ACAQJsonTraitDeclaration> traitDeclarations) {
        this.traitDeclarations = traitDeclarations;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (ACAQJsonTraitDeclaration declaration : traitDeclarations) {
            report.forCategory("Annotations").forCategory(declaration.getName()).report(declaration);
        }
        for (GraphWrapperAlgorithmDeclaration declaration : algorithmDeclarations) {
            report.forCategory("Algorithms").forCategory(declaration.getName()).report(declaration);
        }
    }

    public static ACAQJsonExtension loadProject(JsonNode jsonData) {
        try {
            return JsonUtils.getObjectMapper().readerFor(ACAQJsonExtension.class).readValue(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
