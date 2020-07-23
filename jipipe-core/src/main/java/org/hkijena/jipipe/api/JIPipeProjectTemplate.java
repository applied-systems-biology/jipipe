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
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A reference to a template project
 */
public class JIPipeProjectTemplate {

    private URL location;
    private JIPipeMetadata metadata;

    public JIPipeProjectTemplate() {
    }

    public JIPipeProjectTemplate(URL location) throws IOException {
        this.location = location;
        initialize();
    }

    private void initialize() throws IOException {
        JsonNode node = JsonUtils.getObjectMapper().readValue(location, JsonNode.class);
        metadata = JsonUtils.getObjectMapper().convertValue(node.get("metadata"), JIPipeMetadata.class);
    }

    private static List<JIPipeProjectTemplate> availableTemplatesFromResources;

    /**
     * Loads the project
     * @return the project
     * @throws IOException thrown by project loading
     */
    public JIPipeProject load() throws IOException {
        JsonNode node = JsonUtils.getObjectMapper().readValue(location, JsonNode.class);
        JIPipeProject project = new JIPipeProject();
        project.fromJson(node, new JIPipeValidityReport());
        return project;
    }

    /**
     * Lists all available templates
     * @return the templates
     */
    public static List<JIPipeProjectTemplate> listTemplates() {
        if(availableTemplatesFromResources == null) {
            availableTemplatesFromResources = new ArrayList<>();
            for (String resource : ResourceUtils.walkInternalResourceFolder("templates/")) {
                try {
                    JIPipeProjectTemplate template = new JIPipeProjectTemplate(ResourceUtils.class.getResource(resource));
                    availableTemplatesFromResources.add(template);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
        Path templatesDir = imageJDir.resolve("jipipe-templates");
        List<JIPipeProjectTemplate> result = new ArrayList<>(availableTemplatesFromResources);
        if(Files.exists(templatesDir)) {
            try {
                for (Path file : Files.list(templatesDir).collect(Collectors.toList())) {
                    try {
                        JIPipeProjectTemplate template = new JIPipeProjectTemplate(file.toUri().toURL());
                        result.add(template);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    @JsonGetter("location")
    public URL getLocation() {
        return location;
    }

    @JsonGetter("metadata")
    public JIPipeMetadata getMetadata() {
        return metadata;
    }

    @JsonSetter("metadata")
    public void setMetadata(JIPipeMetadata metadata) {
        this.metadata = metadata;
    }

    @JsonSetter("location")
    public void setLocation(URL location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeProjectTemplate template = (JIPipeProjectTemplate) o;
        return Objects.equals(location, template.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }
}
