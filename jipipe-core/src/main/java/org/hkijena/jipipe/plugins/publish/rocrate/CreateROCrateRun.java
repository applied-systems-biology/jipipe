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

package org.hkijena.jipipe.plugins.publish.rocrate;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CreateROCrateRun extends DefaultJIPipeRunnable {

    private final JIPipeProject project;
    private final Path projectFile;
    private final Path roCrateFile;

    public CreateROCrateRun(JIPipeProject project, Path projectFile, Path roCrateFile) {
        this.project = project;
        this.projectFile = projectFile;
        this.roCrateFile = roCrateFile;
    }

    @Override
    public String getTaskLabel() {
        return "Create RO-Crate";
    }

    @Override
    public void run() {
        getProgressInfo().setLogToStdOut(true);

        Path tmpPath = JIPipeRuntimeApplicationSettings.getTemporaryDirectory("RO-Crate");
        getProgressInfo().log("Creating RO-Crate for project " + project.getWorkDirectory() + " in temporary directory " + tmpPath);

        // Create RO-Create metadata file
        createROCrate(tmpPath);

        // Compress the directory
        try {
            ArchiveUtils.compressDirectoryToZip(tmpPath, "", roCrateFile, getProgressInfo().resolve("Compress"));
            PathUtils.deleteDirectoryRecursively(tmpPath, getProgressInfo().resolve("Cleanup"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createROCrate(Path tmpPath) {
        RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder(getProject().getMetadata().getName(),
                getProject().getMetadata().getSummary().toPlainText(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                getProject().getMetadata().getLicense());

        createReadme(tmpPath, builder);

        RoCrate crate = builder.build();
        System.out.println(crate.getJsonMetadata());
    }

    private void createReadme(Path tmpPath, RoCrate.RoCrateBuilder builder) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("# JIPipe workflow\n\n");
        stringBuilder.append("JIPipe version: ").append(JIPipe.getJIPipeVersion()).append("\n\n");
        stringBuilder.append(getProject().getMetadata().getDescription().getBody());
        stringBuilder.append("\n");

        try {
            Files.writeString(tmpPath.resolve("README.md"), stringBuilder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        FileEntity entity = new FileEntity.FileEntityBuilder()
                .setId("README.md")
                .setLocation(tmpPath.resolve("README.md"))
                .addType("File")
                .addProperty("encodingFormat", "text/markdown")
                .addProperty("about", "./")
                .build();
        builder.addDataEntity(entity);
    }

    public JIPipeProject getProject() {
        return project;
    }

    public Path getProjectFile() {
        return projectFile;
    }

    public Path getRoCrateFile() {
        return roCrateFile;
    }
}
