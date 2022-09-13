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

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A reference to a template project
 */
public class JIPipeProjectTemplate {

    private final String id;

    private final JsonNode node;

    private final JIPipeProjectMetadata metadata;

    private final Path templateFile;
    private final Path zipFile;

    public JIPipeProjectTemplate(String id, JsonNode node, JIPipeProjectMetadata metadata, Path templateFile, Path zipFile) {
        this.id = id;
        this.node = node;
        this.metadata = metadata;
        this.templateFile = templateFile;
        this.zipFile = zipFile;
    }

    public static String getFallbackTemplateId() {
        return "resource:/org.hkijena.jipipe:core//org/hkijena/jipipe/templates/Empty (1 compartment).jip";
    }

    /**
     * Loads the project
     *
     * @return the project
     * @throws IOException thrown by project loading
     */
    public JIPipeProject loadAsProject() throws IOException {
        JIPipeProject project = new JIPipeProject();
        project.fromJson(node, new JIPipeIssueReport(), new JIPipeNotificationInbox());
        // Apply selected default style
        project.getGraph().attachAdditionalMetadata("jipipe:graph:view-mode", GraphEditorUISettings.getInstance().getDefaultViewMode());
        project.getCompartmentGraph().attachAdditionalMetadata("jipipe:graph:view-mode", GraphEditorUISettings.getInstance().getDefaultViewMode());
        return project;
    }

    /**
     * Returns the metadata name if the template is resource-based or registered via the Java API.
     * Returns the metadata name if the metadata name is not used by any other Java-registered template; otherwise returns the file name of the template
     * @return the metadata name if appropriate, otherwise the filename
     */
    public String getFixedName() {
        if(templateFile == null)
            return metadata.getName();
        if(JIPipe.getInstance().getProjectTemplateRegistry().getBlockedTemplateNames().contains(metadata.getName())) {
            return templateFile.getFileName().toString();
        }
        else {
            return metadata.getName();
        }
    }

    public String getId() {
        return id;
    }

    public JsonNode getNode() {
        return node;
    }

    public JIPipeProjectMetadata getMetadata() {
        return metadata;
    }

    public Path getZipFile() {
        return zipFile;
    }

    public Path getTemplateFile() {
        return templateFile;
    }
}
