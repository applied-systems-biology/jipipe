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

package org.hkijena.jipipe.api.project.io;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.settings.application.JIPipeProjectDefaultsApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * The legacy .jip project reader.
 * Reproduces the load semantics of the former inline
 * {@code JIPipeProject.loadProject} implementation exactly:
 * JSON deserialization, work directory set to the file's parent folder,
 * user path validation, and — when enabled in the application settings —
 * automatic fixing of broken environments.
 */
public class JIPipeLegacyProjectReader implements JIPipeProjectReader {

    /**
     * The stable id of the legacy .jip format.
     * Persisted in saved projects — never derive from code.
     */
    public static final String FORMAT_ID = "org.hkijena.jipipe.core:jip";

    @Override
    public String getId() {
        return FORMAT_ID;
    }

    @Override
    public List<String> getFileExtensions() {
        return List.of("jip");
    }

    @Override
    public boolean supports(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jip");
    }

    @Override
    public JIPipeProject read(Path path, JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeNotificationInbox notifications, JIPipeProgressInfo progressInfo) throws IOException {
        JsonNode jsonData = JsonUtils.getObjectMapper().readValue(path.toFile(), JsonNode.class);
        JIPipeProject project = new JIPipeProject();
        project.fromJson(jsonData, context, report, notifications, progressInfo);
        project.setWorkDirectory(path.getParent());
        project.validateUserPaths(notifications);
        if (JIPipeProjectDefaultsApplicationSettings.getInstance().isAutoFixProjectEnvironments()) {
            project.fixBrokenEnvironments(progressInfo.resolve("Fix environments"));
        }
        project.setProjectFile(path);

        // Materialize the project workspace (derived from the model).
        // NEVER fails the legacy load: a legacy project is fully usable
        // without its workspace; a later .jipx save regenerates it.
        try {
            project.getWorkspace().materializeFromModel(progressInfo.resolve("Materialize workspace"));
        } catch (Exception e) {
            progressInfo.log("Workspace materialization failed (legacy project still loaded): " + e);
        }
        return project;
    }
}
