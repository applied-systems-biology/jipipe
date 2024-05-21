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
import org.hkijena.jipipe.api.JIPipeStandardMetadata;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.api.grouping.JsonNodeRegistrationTask;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.JsonNodeInfoValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A JSON-serializable extension
 */
@JsonDeserialize(as = JIPipeJsonPlugin.class, using = JIPipeJsonPlugin.Deserializer.class)
public class JIPipeJsonPlugin extends AbstractJIPipeParameterCollection implements JIPipePlugin, JIPipeValidatable {
    private final JIPipeService.ExtensionContentAddedEventEmitter extensionContentAddedEventEmitter = new JIPipeService.ExtensionContentAddedEventEmitter();
    private final JIPipeService.ExtensionContentRemovedEventEmitter extensionContentRemovedEventEmitter = new JIPipeService.ExtensionContentRemovedEventEmitter();
    private String id;
    private String version = "1.0.0";

    private StringList provides = new StringList();
    private JIPipeStandardMetadata metadata = new JIPipeStandardMetadata();
    private Path jsonFilePath;
    private JIPipe registry;
    private JIPipeImageJUpdateSiteDependency.List updateSiteDependenciesParameter = new JIPipeImageJUpdateSiteDependency.List();
    private Set<JsonNodeInfo> nodeInfos = new HashSet<>();
    private JsonNode serializedJson;

    /**
     * Creates a new instance
     */
    public JIPipeJsonPlugin() {
    }

    /**
     * Loads an extension from JSON
     *
     * @param jsonData JSON data
     * @return Loaded instance
     */
    public static JIPipeJsonPlugin loadProject(JsonNode jsonData) {
        try {
            return JsonUtils.getObjectMapper().readerFor(JIPipeJsonPlugin.class).readValue(jsonData);
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e,
                    "Could not load JSON plugin.",
                    "The plugin file was corrupted, so JIPipe does not know how to load some essential information. Or you are using an older JIPipe version.",
                    "Try to update JIPipe. If this does not work, contact the plugin's author.");
        }
    }

    /**
     * Loads an extension from a file
     *
     * @param path the path to the project file
     * @return the project
     */
    public static JIPipeJsonPlugin loadProject(Path path) {
        try {
            JsonNode jsonData = JsonUtils.getObjectMapper().readValue(path.toFile(), JsonNode.class);
            return loadProject(jsonData);
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e,
                    "Could not load JSON plugin.",
                    "The plugin file was corrupted, so JIPipe does not know how to load some essential information. Or you are using an older JIPipe version.",
                    "Try to update JIPipe. If this does not work, contact the plugin's author.");
        }
    }

    public JIPipeService.ExtensionContentAddedEventEmitter getExtensionContentAddedEventEmitter() {
        return extensionContentAddedEventEmitter;
    }

    public JIPipeService.ExtensionContentRemovedEventEmitter getExtensionContentRemovedEventEmitter() {
        return extensionContentRemovedEventEmitter;
    }

    @Override
    @JsonGetter("metadata")
    @JIPipeParameter("metadata")
    @SetJIPipeDocumentation(name = "Metadata", description = "Additional extension metadata")
    public JIPipeStandardMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets metadata
     *
     * @param metadata Metadata
     */
    @JsonSetter("metadata")
    public void setMetadata(JIPipeStandardMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    @JsonGetter("id")
    @JIPipeParameter("id")
    @StringParameterSettings(monospace = true)
    @SetJIPipeDocumentation(name = "Unique extension ID", description = "A unique identifier for the extension. It must have following format: " +
            "[Author]:[Extension] where [Author] contains information about who developed the extension. An example is <i>org.hkijena:my-extension</i>")
    public String getDependencyId() {
        return id;
    }

    @SetJIPipeDocumentation(name = "Provides", description = "Set of alternative dependency IDs")
    @JIPipeParameter("provides")
    @JsonGetter("provides")
    @Override
    public StringList getDependencyProvides() {
        return provides;
    }

    @JIPipeParameter("provides")
    @JsonSetter("provides")
    public void setDependencyProvides(StringList dependencyProvides) {
        this.provides = dependencyProvides;
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
    @SetJIPipeDocumentation(name = "Version", description = "The version of this extension. A common format is x.y.z or x.y.z.w")
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
    @Override
    public Set<JIPipeDependency> getDependencies() {
        Set<JIPipeDependency> result = new HashSet<>();
        for (JIPipeNodeInfo info : getNodeInfos()) {
            result.addAll(info.getDependencies());
        }
        return result.stream().map(JIPipeMutableDependency::new).collect(Collectors.toSet());
    }

    @Override
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return updateSiteDependenciesParameter;
    }

    @SetJIPipeDocumentation(name = "ImageJ update site dependencies", description = "ImageJ update sites that should be enabled for the extension to work. " +
            "Users will get a notification if a site is not activated or found. Both name and URL should be set. The URL is only used if a site with the same name " +
            "does not already exist in the user's repository.")
    @JIPipeParameter(value = "update-site-dependencies", uiOrder = 10)
    @JsonGetter("update-site-dependencies")
    public JIPipeImageJUpdateSiteDependency.List getUpdateSiteDependenciesParameter() {
        return updateSiteDependenciesParameter;
    }

    @JIPipeParameter("update-site-dependencies")
    @JsonSetter("update-site-dependencies")
    public void setUpdateSiteDependenciesParameter(JIPipeImageJUpdateSiteDependency.List updateSiteDependenciesParameter) {
        this.updateSiteDependenciesParameter = updateSiteDependenciesParameter;
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
    public JIPipe getRegistry() {
        return registry;
    }

    /**
     * Sets the registry instance
     *
     * @param registry The registry
     */
    public void setRegistry(JIPipe registry) {
        this.registry = registry;
    }

    /**
     * Registers the content.
     * We cannot be sure if there are internal dependencies, so schedule tasks
     */
    public void register() {
        for (JsonNode entry : ImmutableList.copyOf(serializedJson.get("algorithms").elements())) {
            JIPipe.getNodes().scheduleRegister(new JsonNodeRegistrationTask(entry, this));
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
        extensionContentAddedEventEmitter.emit(new JIPipe.ExtensionContentAddedEvent(this, nodeInfo));
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
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (StringUtils.isNullOrEmpty(getDependencyId())) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Extension ID", "dependency-id"),
                    "The ID is empty!",
                    "A JSON extension must be identified with a unique ID to allow JIPipe to find dependencies.",
                    "Please provide a valid ID.",
                    JsonUtils.toPrettyJsonString(this)));
        } else if (!getDependencyId().contains(":")) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Extension ID", "dependency-id"),
                    "Malformed ID!",
                    "The ID should contain some information about the plugin author (organization, ...) to prevent future collisions.",
                    "The ID must have following structure: <Organization>:<Name> e.g. org.hkijena.jipipe:my-plugin",
                    JsonUtils.toPrettyJsonString(this)));
        }
        if (StringUtils.isNullOrEmpty(getDependencyVersion())) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Extension version", "version"),
                    "The version is empty!",
                    "This allows users of your extension to better get help if issues arise.",
                    "Please provide a valid version number. It has usually following format x.y.z.w",
                    JsonUtils.toPrettyJsonString(this)));
        }
        if (StringUtils.isNullOrEmpty(getMetadata().getName()) || "New project".equals(getMetadata().getName())) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Name", "name"),
                    "Invalid name!",
                    "Your plugin should have a short and meaningful name.",
                    "Please provide a meaningful name for your plugin.",
                    JsonUtils.toPrettyJsonString(this)));
        }
        if (nodeInfos == null)
            deserializeNodeInfos();
        for (JsonNodeInfo info : nodeInfos) {
            report.report(new JsonNodeInfoValidationReportContext(reportContext, info), info);
        }
        if (nodeInfos.size() != nodeInfos.stream().map(JsonNodeInfo::getId).collect(Collectors.toSet()).size()) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
                    "Duplicate IDs found!",
                    "Algorithm IDs must be unique",
                    "Please make sure that IDs are unique.",
                    JsonUtils.toPrettyJsonString(this)));
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
            extensionContentRemovedEventEmitter.emit(new JIPipe.ExtensionContentRemovedEvent(this, info));
        }
    }

    /**
     * Deserializer for the {@link JIPipeJsonPlugin}.
     * This is a special deserializer that leaves the algorithmInfos field alone, as we cannot be sure if algorithmInfos is deserialized
     * during registration (leading to missing algorithm Ids)
     */
    public static class Deserializer extends JsonDeserializer<JIPipeJsonPlugin> {
        @Override
        public JIPipeJsonPlugin deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JIPipeJsonPlugin extension = new JIPipeJsonPlugin();
            extension.nodeInfos = null;
            JsonNode node = jsonParser.readValueAsTree();

            extension.serializedJson = node;
            extension.id = node.get("id").textValue();
            extension.version = node.get("version").textValue();
            extension.metadata = JsonUtils.getObjectMapper().readerFor(JIPipeStandardMetadata.class).readValue(node.get("metadata"));

            return extension;
        }
    }
}
