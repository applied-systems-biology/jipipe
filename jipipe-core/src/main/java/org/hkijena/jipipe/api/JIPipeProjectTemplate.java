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

package org.hkijena.jipipe.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import ij.IJ;
import ij.Prefs;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A reference to a template project
 */
public class JIPipeProjectTemplate {

    private static List<JIPipeProjectTemplate> availableTemplatesFromResources;
    private String resourcePath;
    private Path relativeLocation;
    private boolean storedAsResource;
    private JIPipeProjectMetadata metadata;

    public JIPipeProjectTemplate() {
    }

    /**
     * Loads the project
     *
     * @return the project
     * @throws IOException thrown by project loading
     */
    public JIPipeProject load() throws IOException {
        JsonNode node = JsonUtils.getObjectMapper().readValue(getLocation(), JsonNode.class);
        JIPipeProject project = new JIPipeProject();
        project.fromJson(node, new JIPipeIssueReport());
        // Apply selected default style
        project.getGraph().attachAdditionalMetadata("jipipe:graph:view-mode", GraphEditorUISettings.getInstance().getDefaultViewMode());
        project.getCompartmentGraph().attachAdditionalMetadata("jipipe:graph:view-mode", GraphEditorUISettings.getInstance().getDefaultViewMode());
        return project;
    }

    @JsonGetter("resource-path")
    public String getResourcePath() {
        return resourcePath;
    }

    @JsonSetter("resource-path")
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @JsonGetter("stored-as-resource")
    public boolean isStoredAsResource() {
        return storedAsResource;
    }

    @JsonSetter("stored-as-resource")
    public void setStoredAsResource(boolean storedAsResource) {
        this.storedAsResource = storedAsResource;
    }

    @JsonGetter("relative-location")
    public Path getRelativeLocation() {
        return relativeLocation;
    }

    @JsonSetter("relative-location")
    public void setRelativeLocation(Path relativeLocation) {
        this.relativeLocation = relativeLocation;
    }

    public URL getLocation() {
        if (isStoredAsResource()) {
            if (StringUtils.isNullOrEmpty(resourcePath)) {
                return ResourceUtils.getPluginResource("templates/Empty (3 compartments).jip");
            }
            return ResourceUtils.class.getResource(resourcePath);
        } else {
            if (relativeLocation == null) {
                return ResourceUtils.getPluginResource("templates/Empty (3 compartments).jip");
            }
            Path imageJDir = Paths.get(Prefs.getImageJDir());
            if (!Files.isDirectory(imageJDir)) {
                try {
                    Files.createDirectories(imageJDir);
                } catch (IOException e) {
                    IJ.handleException(e);
                }
            }
            Path path = imageJDir.resolve(relativeLocation);
            if (!Files.exists(path)) {
                return ResourceUtils.getPluginResource("templates/Empty (3 compartments).jip");
            }
            try {
                return path.toUri().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return ResourceUtils.getPluginResource("templates/Empty (3 compartments).jip");
            }
        }
    }

    @JsonGetter("metadata")
    public JIPipeProjectMetadata getMetadata() {
        if (metadata == null) {
            JsonNode node = null;
            try {
                node = JsonUtils.getObjectMapper().readValue(getLocation(), JsonNode.class);
                metadata = JsonUtils.getObjectMapper().convertValue(node.get("metadata"), JIPipeProjectMetadata.class);
            } catch (IOException e) {
                e.printStackTrace();
                metadata = new JIPipeProjectMetadata();
                metadata.setName("Could not load!");
            }
        }
        return metadata;
    }

    @JsonSetter("metadata")
    public void setMetadata(JIPipeProjectMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeProjectTemplate that = (JIPipeProjectTemplate) o;
        return storedAsResource == that.storedAsResource &&
                Objects.equals(resourcePath, that.resourcePath) &&
                Objects.equals(relativeLocation, that.relativeLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourcePath, relativeLocation, storedAsResource);
    }

    /**
     * Lists all available templates
     *
     * @return the templates
     */
    public static List<JIPipeProjectTemplate> listTemplates() {
        if (availableTemplatesFromResources == null) {
            availableTemplatesFromResources = new ArrayList<>();
            for (String resource : ResourceUtils.walkInternalResourceFolder("templates/")) {
                try {
                    JIPipeProjectTemplate template = new JIPipeProjectTemplate();
                    template.setResourcePath(resource);
                    template.setStoredAsResource(true);
                    availableTemplatesFromResources.add(template);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            availableTemplatesFromResources.sort(Comparator.comparing(template -> template.getMetadata().getName()));
        }

        // Load templates from file name
        Path imageJDir = Paths.get(Prefs.getImageJDir());
        if (!Files.isDirectory(imageJDir)) {
            try {
                Files.createDirectories(imageJDir);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        Path templatesDir = imageJDir.resolve("jipipe").resolve("templates");
        List<JIPipeProjectTemplate> result = new ArrayList<>(availableTemplatesFromResources);
        if (Files.isDirectory(templatesDir)) {
            try {
                for (Path file : Files.list(templatesDir).collect(Collectors.toList())) {
                    try {
                        JIPipeProjectTemplate template = new JIPipeProjectTemplate();
                        template.setStoredAsResource(false);
                        template.setRelativeLocation(templatesDir.resolve(file));
                        result.add(template);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                Files.createDirectories(templatesDir);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }

        return result;
    }
}
