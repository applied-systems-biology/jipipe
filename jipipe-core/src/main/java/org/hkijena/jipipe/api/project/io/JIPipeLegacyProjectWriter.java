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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * The legacy .jip project writer.
 * Reproduces the save semantics of the former inline
 * {@code JIPipeProject.saveProject} implementation exactly: the Path
 * variant merges the global author list into the project metadata (when
 * enabled) and pretty-prints the project to the target file; the
 * {@code Writer}/{@code OutputStream} variants pretty-print without the
 * authors merge — an asymmetry of the legacy format, preserved on purpose.
 * <p>
 * The writer never updates the project's internally tracked file path —
 * that side effect stays in
 * {@link JIPipeProject#saveProject(Path, boolean)}.
 */
public class JIPipeLegacyProjectWriter implements JIPipeProjectWriter {

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
    public void write(JIPipeProject project, Path target, JIPipeProgressInfo progressInfo) throws IOException {
        // Add authors from global list
        if (project.getMetadata().isAutoAddAuthors()) {
            project.addAuthorsFromGlobalList();
        }

        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), project);
    }

    @Override
    public void write(JIPipeProject project, Writer writer) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(writer, project);
    }

    @Override
    public void write(JIPipeProject project, OutputStream outputStream) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, project);
    }
}
