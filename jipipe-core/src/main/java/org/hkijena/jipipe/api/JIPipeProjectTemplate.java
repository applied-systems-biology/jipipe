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
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * A reference to a template project
 */
public class JIPipeProjectTemplate {

    private final String id;

    private final JsonNode node;

    private final JIPipeProjectMetadata metadata;

    public JIPipeProjectTemplate(String id, JsonNode node, JIPipeProjectMetadata metadata) {
        this.id = id;
        this.node = node;
        this.metadata = metadata;
    }

    public static String getFallbackTemplateId() {
        return ResourceUtils.getPluginResource("templates/Empty (1 compartment).jip") + "";
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

    public String getId() {
        return id;
    }

    public JsonNode getNode() {
        return node;
    }

    public JIPipeProjectMetadata getMetadata() {
        return metadata;
    }
}
