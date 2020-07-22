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

package org.hkijena.jipipe;

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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.events.ExtensionContentAddedEvent;
import org.hkijena.jipipe.api.events.ExtensionContentRemovedEvent;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.grouping.JsonNodeRegistrationTask;
import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A JSON-serializable extension
 */
@JsonDeserialize(as = JIPipeJsonExtension.class, using = JIPipeJsonExtension.Deserializer.class)
public class JIPipeJsonExtension implements JIPipeDependency, JIPipeValidatable {
    private EventBus eventBus = new EventBus();
    private String id;
    private String version = "1.0.0";
    private JIPipeMetadata metadata = new JIPipeMetadata();
    private Path jsonFilePath;
    private JIPipeDefaultRegistry registry;

    private Set<JsonNodeInfo> nodeInfos = new HashSet<>();
    private JsonNode serializedJson;

    /**
     * Creates a new instance
     */
    public JIPipeJsonExtension() {
    }

    @Override
    @JsonGetter("metadata")
    @JIPipeParameter("metadata")
    @JIPipeDocumentation(name = "Metadata", description = "Additional extension metadata")
    public JIPipeMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets metadata
     *
     * @param metadata Metadata
     */
    @JsonSetter("metadata")
    public void setMetadata(JIPipeMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    @JsonGetter("id")
    @JIPipeParameter("id")
    @StringParameterSettings(monospace = true)
    @JIPipeDocumentation(name = "Unique extension ID", description = "A unique identifier for the extension. It must have following format: " +
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
    @JIPipeParameter("id")
    public void setId(String id) {
        this.id = id;
    }

    @Override
    @JsonGetter("version")
    @JIPipeParameter("version")
    @StringParameterSettings(monospace = true)
    @JIPipeDocumentation(name = "Version", description = "The version of this extension. A common format is x.y.z or x.y.z.w")
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
    @JIPipeParameter("version")
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return The project type
     */
    @JsonGetter("jipipe:project-type")
    public String getProjectType() {
        return "json-extension";
    }

    /**
     * @return The dependencies of this extension
     */
    @JsonGetter("dependencies")
    public Set<JIPipeDependency> getDependencies() {
        Set<JIPipeDependency> result = new HashSet<>();
        for (JIPipeNodeInfo info : getNodeInfos()) {
            result.addAll(info.getDependencies());
        }
        return result.stream().map(JIPipeMutableDependency::new).collect(Collectors.toSet());
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
    public JIPipeDefaultRegistry getRegistry() {
        return registry;
    }

    /**
     * Sets the registry instance
     *
     * @param registry The registry
     */
    public void setRegistry(JIPipeDefaultRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registers the content.
     * We cannot be sure if there are internal dependencies, so schedule tasks
     */
    public void register() {
        for (JsonNode entry : ImmutableList.copyOf(serializedJson.get("algorithms").elements())) {
            JIPipeNodeRegistry.getInstance().scheduleRegister(new JsonNodeRegistrationTask(entry, this));
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
     * Adds a new node type of specified type
     *
     * @param nodeInfo The algorithm type
     */
    public void addAlgorithm(JsonNodeInfo nodeInfo) {
        if (nodeInfos == null)
            deserializeNodeInfos();
        nodeInfos.add(nodeInfo);
        eventBus.post(new ExtensionContentAddedEvent(this, nodeInfo));
    }

    /**
     * Responsible for deserializing the algorithms
     */
    private void deserializeNodeInfos() {
        if (nodeInfos == null) {
            TypeReference<HashSet<JsonNodeInfo>> typeReference = new TypeReference<HashSet<JsonNodeInfo>>() {
            };
            try {
                nodeInfos = JsonUtils.getObjectMapper().readerFor(typeReference).readValue(serializedJson.get("algorithms"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return Algorithm infos
     */
    @JsonGetter("algorithms")
    public Set<JsonNodeInfo> getNodeInfos() {
        if (nodeInfos == null)
            deserializeNodeInfos();
        return Collections.unmodifiableSet(nodeInfos);
    }

    /**
     * Sets algorithm infos
     *
     * @param nodeInfos Infos
     */
    @JsonSetter("algorithms")
    private void setNodeInfos(Set<JsonNodeInfo> nodeInfos) {
        this.nodeInfos = nodeInfos;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (StringUtils.isNullOrEmpty(getDependencyId())) {
            report.forCategory("ID").reportIsInvalid("The ID is empty!",
                    "A JSON extension must be identified with a unique ID to allow JIPipe to find dependencies.",
                    " Please provide a valid ID.",
                    this);
        } else if (!getDependencyId().contains(":")) {
            report.forCategory("ID").reportIsInvalid("Malformed ID!",
                    "The ID should contain some information about the plugin author (organization, ...) to prevent future collisions.",
                    "The ID must have following structure: <Organization>:<Name> e.g. org.hkijena.jipipe:my-plugin",
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
        if (nodeInfos == null)
            deserializeNodeInfos();
        for (JsonNodeInfo info : nodeInfos) {
            report.forCategory("Algorithms").forCategory(info.getName()).report(info);
        }
        if (nodeInfos.size() != nodeInfos.stream().map(JsonNodeInfo::getId).collect(Collectors.toSet()).size()) {
            report.forCategory("Algorithms").reportIsInvalid("Duplicate IDs found!",
                    "Algorithm IDs must be unique",
                    "Please make sure that IDs are unique.",
                    this);
        }
    }

    /**
     * Removes an algorithm
     *
     * @param info Algorithm type
     */
    public void removeAlgorithm(JsonNodeInfo info) {
        if (nodeInfos == null)
            deserializeNodeInfos();
        if (nodeInfos.remove(info)) {
            eventBus.post(new ExtensionContentRemovedEvent(this, info));
        }
    }

    /**
     * Loads a {@link JIPipeJsonExtension} from JSON
     *
     * @param jsonData JSON data
     * @return Loaded instance
     */
    public static JIPipeJsonExtension loadProject(JsonNode jsonData) {
        try {
            return JsonUtils.getObjectMapper().readerFor(JIPipeJsonExtension.class).readValue(jsonData);
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not load JSON plugin.",
                    "JIPipe JSON extension loader", "The plugin file was corrupted, so JIPipe does not know how to load some essential information. Or you are using an older JIPipe version.",
                    "Try to update JIPipe. If this does not work, contact the plugin's author.");
        }
    }

    /**
     * Deserializer for the {@link JIPipeJsonExtension}.
     * This is a special deserializer that leaves the algorithmInfos field alone, as we cannot be sure if algorithmInfos is deserialized
     * during registration (leading to missing algorithm Ids)
     */
    public static class Deserializer extends JsonDeserializer<JIPipeJsonExtension> {
        @Override
        public JIPipeJsonExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JIPipeJsonExtension extension = new JIPipeJsonExtension();
            extension.nodeInfos = null;
            JsonNode node = jsonParser.readValueAsTree();

            extension.serializedJson = node;
            extension.id = node.get("id").textValue();
            extension.version = node.get("version").textValue();
            extension.metadata = JsonUtils.getObjectMapper().readerFor(JIPipeMetadata.class).readValue(node.get("metadata"));

            return extension;
        }
    }
}
