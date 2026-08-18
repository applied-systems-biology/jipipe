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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeArchiveProjectToDirectoryRun;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectUserPaths;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.contrib.ro_crate.RoCrate;
import org.hkijena.jipipe.contrib.ro_crate.entities.contextual.ContextualEntity;
import org.hkijena.jipipe.contrib.ro_crate.entities.contextual.JsonDescriptor;
import org.hkijena.jipipe.contrib.ro_crate.entities.contextual.OrganizationEntity;
import org.hkijena.jipipe.contrib.ro_crate.entities.contextual.PersonEntity;
import org.hkijena.jipipe.contrib.ro_crate.entities.data.DataSetEntity;
import org.hkijena.jipipe.contrib.ro_crate.entities.data.FileEntity;
import org.hkijena.jipipe.contrib.ro_crate.writer.Writers;
import org.hkijena.jipipe.plugins.pipelinerender.RenderPipelineRun;
import org.hkijena.jipipe.plugins.pipelinerender.RenderPipelineRunSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CreateROCrateRun extends DefaultJIPipeRunnable {

    private final JIPipeProject project;
    private final Path projectFile;
    private final Path roCrateFile;
    private final Map<String, JIPipeProjectUserPaths.Role> archivedProjectUserPaths;
    private final ROCrateDockerSettings dockerSettings;

    public CreateROCrateRun(JIPipeProject project, Path projectFile, Path roCrateFile,
                            Map<String, JIPipeProjectUserPaths.Role> archivedProjectUserPaths,
                            ROCrateDockerSettings dockerSettings) {
        this.project = project;
        this.projectFile = projectFile;
        this.roCrateFile = roCrateFile;
        this.archivedProjectUserPaths = archivedProjectUserPaths;
        this.dockerSettings = dockerSettings;
    }

    public CreateROCrateRun(JIPipeProject project, Path projectFile, Path roCrateFile,
                            Map<String, JIPipeProjectUserPaths.Role> archivedProjectUserPaths) {
        this(project, projectFile, roCrateFile, archivedProjectUserPaths,
                ROCrateApplicationSettings.getInstance().toDockerSettings());
    }

    @Override
    public String getTaskLabel() {
        return "Create RO-Crate";
    }

    @Override
    public void run() {
//        getProgressInfo().setLogToStdOut(true);

        Path tmpPath = JIPipe.getTemporaryDirectory("RO-Crate");
        getProgressInfo().log("Creating RO-Crate for project " + projectFile + " using temporary directory " + tmpPath + " to be saved to " + roCrateFile);
        RoCrate.RoCrateBuilder builder = createROCrateBuilder();

        // Create the project archive
        createProjectArchive(tmpPath);
        Map<String, String> projectDirectories = copyProjectUserPaths(builder, tmpPath);
        addProjectToROCrate(builder, tmpPath, projectDirectories);

        // Create CWL file
        createWorkflowCwl(tmpPath);

        // Create RO-Create metadata
        addROCrateOrganizations(builder);
        addROCrateAuthors(builder);
        addROCrateCwlWorkflow(builder, tmpPath);
        addROCrateInputsDir(builder, tmpPath);
        createReadme(tmpPath, builder);
        createDiagram(tmpPath, builder);

        // Ensure that the input directory exists
        if (!Files.isDirectory(tmpPath.resolve("inputs"))) {
            PathUtils.createDirectories(tmpPath.resolve("inputs"));
            // Create something in there
            PathUtils.createKeepFile(tmpPath.resolve("inputs").resolve(".keep"));
        }

        // Compress the container
        RoCrate crate = builder.build();
        crate.getRootDataEntity().addProperty("keywords", "JIPipe");
        addROCrateMainEntity(crate);
        try {
            Writers.newZipPathWriter().withAutomaticProvenance(null).save(crate, roCrateFile.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Remove tmp directory
        PathUtils.deleteDirectoryRecursively(tmpPath, getProgressInfo().resolve("Cleanup"));
    }

    private void addProjectToROCrate(RoCrate.RoCrateBuilder builder, Path tmpPath, Map<String, String> projectUserPaths) {
        try {
            JIPipeProject copyProject = new JIPipeProject();
            copyProject.fromJson(JsonUtils.readFromFile(tmpPath.resolve("project.jip"), JsonNode.class),
                    new UnspecifiedValidationReportContext(),
                    new JIPipeValidationReport(),
                    new JIPipeNotificationInbox(), getProgressInfo().resolve("Load project copy"));

            // Modify user paths
            for (JIPipeDynamicParameterCollection parameterCollection : copyProject.getMetadata().getUserPaths().getPaths()) {
                String key = StringUtils.nullToEmpty(parameterCollection.get("key").get(String.class));
                String newValue = projectUserPaths.getOrDefault(key, null);
                if (newValue != null) {
                    parameterCollection.setParameter("path", Path.of(newValue));
                }
            }

            // Re-save project
            copyProject.saveProject(tmpPath.resolve("project.jip"), false);
            builder.addDataEntity(new FileEntity.FileEntityBuilder()
                    .setId("./project.jip")
                    .setLocation(tmpPath.resolve("project.jip"))
                    .addProperty("encodingFormat", "application/json")
                    .build());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> copyProjectUserPaths(RoCrate.RoCrateBuilder builder, Path tmpPath) {
        // Map that will be saved into project-user-paths.json
        Map<String, String> projectUserPathsRedirect = new HashMap<>();

        // Archive project directories
        Map<String, Path> directoryMap = getProject().getMetadata().getUserPaths().getDirectoryMap(getProject().getWorkDirectory());
        for (Map.Entry<String, Path> entry : directoryMap.entrySet()) {
            JIPipeProjectUserPaths.Role role = archivedProjectUserPaths.getOrDefault(entry.getKey(), JIPipeProjectUserPaths.Role.Ignored);
            if (role == JIPipeProjectUserPaths.Role.Unspecified) {
                // Auto-detect
                getProgressInfo().log("WARNING: Project user path " + entry.getKey() + " (" + entry.getValue() + ") is unspecified. Guessing type based on path properties.");

                if (Files.exists(entry.getValue())) {
                    getProgressInfo().log("INFO: path exists. guessing type is 'Input'");
                    role = JIPipeProjectUserPaths.Role.Input;
                } else {
                    getProgressInfo().log("INFO: path does not exist. guessing type is 'Output'");
                    role = JIPipeProjectUserPaths.Role.Output;
                }
            }
            switch (role) {
                case Ignored: {
                    getProgressInfo().log("WARNING: Project user path " + entry.getKey() + " is ignored by Ro-Crate!!!");
                }
                break;
                case Input: {
                    getProgressInfo().log("Archiving project user path " + entry.getKey() + " (" + entry.getValue() + ")");
                    if (Files.isDirectory(entry.getValue())) {
                        String newKey = StringUtils.makeFilesystemCompatible(entry.getKey());
                        Path relativeArchiveDirectory = Path.of("inputs", newKey);
                        PathUtils.createDirectories(tmpPath.resolve(relativeArchiveDirectory));
                        PathUtils.copyDirectory(entry.getValue(), tmpPath.resolve(relativeArchiveDirectory), getProgressInfo().resolve("Project directory " + entry.getKey()));
                        projectUserPathsRedirect.put(entry.getKey(), relativeArchiveDirectory.toString());
                    }
                    else if(Files.isRegularFile(entry.getValue())) {
                        // We will make a subdirectory based on the key and copy the file into that one
                        // Then apply the redirect accordingly
                        // This preserves the file name which may contain metadata
                        String newKey = StringUtils.makeFilesystemCompatible(entry.getKey());
                        Path relativeArchiveDirectory = Path.of("inputs", newKey);

                        getProgressInfo().log("Relocating input project user path " + entry.getKey() + " (" + entry.getValue() + ") into key-based storage " + relativeArchiveDirectory);
                        getProgressInfo().log("-> File will be " + relativeArchiveDirectory.resolve(entry.getValue().getFileName()));

                        PathUtils.createDirectories(tmpPath.resolve(relativeArchiveDirectory));
                        PathUtils.copyFile(entry.getValue(), tmpPath.resolve(relativeArchiveDirectory).resolve(entry.getValue().getFileName()));
                        projectUserPathsRedirect.put(entry.getKey(), relativeArchiveDirectory.resolve(entry.getValue().getFileName()).toString());
                    }
                    else {
                        getProgressInfo().log(new FileNotFoundException("Unable to archive project user path " + entry.getKey() + ": directory " + entry.getValue() + " does not exist"));
                    }
                }
                break;
                case Output: {
                    getProgressInfo().log("Project user path " + entry.getKey() + " (" + entry.getValue() + ") will be redirected into the output directory");

                    if(Files.isDirectory(entry.getValue())) {
                        // Outputs are stored into the outputs directory
                        projectUserPathsRedirect.put(entry.getKey(), Path.of("outputs", entry.getKey()).toString());
                    }
                    else if(Files.isRegularFile(entry.getValue())) {
                        // Preserve the file name
                        Path finalOutputPath = Path.of("outputs", entry.getKey(), entry.getValue().getFileName().toString());
                        getProgressInfo().log("Relocating output project user path " + entry.getKey() + " (" + entry.getValue() + ") into key-based storage " + finalOutputPath);
                        projectUserPathsRedirect.put(entry.getKey(), finalOutputPath.toString());
                    }
                    else {
                        getProgressInfo().warn("Unable to guess if the output is a file or directory (does not exist, so assuming a directory)");
                        // Outputs are stored into the outputs directory
                        projectUserPathsRedirect.put(entry.getKey(), Path.of("outputs", entry.getKey()).toString());
                    }
                }
                break;
            }
        }

        // Create and add the project-user-paths.json
        JsonUtils.saveToFile(projectUserPathsRedirect, tmpPath.resolve("project-user-paths.json"));
        builder.addDataEntity(new FileEntity.FileEntityBuilder()
                .setId("project-user-paths.json")
                .setLocation(tmpPath.resolve("project-user-paths.json"))
                .addProperty("encodingFormat", "application/json")
                .build());

        return projectUserPathsRedirect;
    }

    private void addROCrateMainEntity(RoCrate crate) {
        crate.getRootDataEntity().addIdProperty("mainEntity", "workflow.cwl");
    }

    private void addROCrateInputsDir(RoCrate.RoCrateBuilder builder, Path tmpPath) {
        var entityBuilder = new DataSetEntity.DataSetBuilder()
                .setId("./inputs")
                .setLocation(tmpPath.resolve("inputs"));
        builder.addDataEntity(entityBuilder.build());
    }

    private void addROCrateCwlWorkflow(RoCrate.RoCrateBuilder builder, Path tmpPath) {
        // Add the CWL itself
        var entityBuilder = new FileEntity.FileEntityBuilder()
                .setId("workflow.cwl")
                .setLocation(tmpPath.resolve("workflow.cwl"))
                .addType("File")
                .addType("SoftwareSourceCode")
                .addType("ComputationalWorkflow")
                .addIdProperty("programmingLanguage", "https://w3id.org/workflowhub/workflow-ro-crate#cwl")
                .addIdProperty("conformsTo", "https://bioschemas.org/profiles/ComputationalWorkflow/1.0-RELEASE")
                .addProperty("encodingFormat", "application/x-yaml")
                .addProperty("name", "CWL wrapper workflow");
        if (Files.isRegularFile(tmpPath.resolve("diagram.png"))) {
            entityBuilder.addIdProperty("image", "diagram.png");
        }
        builder.addDataEntity(entityBuilder.build());
    }

    private void createProjectArchive(Path tmpPath) {
        JIPipeArchiveProjectToDirectoryRun archiveProjectToDirectoryRun = new JIPipeArchiveProjectToDirectoryRun(getProject(), tmpPath, Path.of("inputs", "__nd"));
        archiveProjectToDirectoryRun.setProgressInfo(getProgressInfo().resolveAndLog("Creating project archive").detachProgress());
        archiveProjectToDirectoryRun.run();
    }

    private void createWorkflowCwl(Path tmpPath) {
        getProgressInfo().log("Creating workflow CWL");
        Path out = tmpPath.resolve("workflow.cwl");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("cwlVersion", "v1.2");
        root.put("class", "CommandLineTool");

        // requirements
        List<Object> requirements = new ArrayList<>();

        // Use the JIPipe docker releases
        Map<String, Object> dockerReq = new LinkedHashMap<>();
        dockerReq.put("class", "DockerRequirement");
        dockerReq.put("dockerPull", dockerSettings.getDockerImage() + ":" + dockerSettings.getDockerTag());
        requirements.add(dockerReq);

        // Stage all inputs and the project itself
        Map<String, Object> iwdr = new LinkedHashMap<>();
        iwdr.put("class", "InitialWorkDirRequirement");
        List<Object> listing = new ArrayList<>();
        listing.add(new LinkedHashMap<String, Object>() {{
            put("entryname", "$(inputs.project.basename)");
            put("entry", "$(inputs.project)");
        }});
        listing.add(new LinkedHashMap<String, Object>() {{
            put("entryname", "inputs");
            put("entry", "$(inputs.inputs_dir)");
        }});
        iwdr.put("listing", listing);
        requirements.add(iwdr);

        // Attach environment variables that configure a HOME for JIPipe to use
        Map<String, Object> envReq = new LinkedHashMap<>();
        envReq.put("class", "EnvVarRequirement");
        Map<String, Object> envDef = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : dockerSettings.getEnvVarsAsMap().entrySet()) {
            envDef.put(entry.getKey(), entry.getValue());
        }
        envReq.put("envDef", envDef);
        requirements.add(envReq);

        root.put("requirements", requirements);

        // The Docker containers are configured to be run with jipipe run as base command
        root.put("baseCommand", Arrays.asList("jipipe", "run"));

        // inputs
        Map<String, Object> inputs = new LinkedHashMap<>();

        inputs.put("project", new LinkedHashMap<String, Object>() {{
            put("type", "File");
            put("doc", "Exported JIPipe project (.jip) with relative paths into ./inputs");
        }});

        inputs.put("inputs_dir", new LinkedHashMap<String, Object>() {{
            put("type", "Directory");
            put("doc", "All staged input files and directories");
        }});

        inputs.put("output_dir", new LinkedHashMap<String, Object>() {{
            put("type", "string");
            put("default", "output/auto");
            put("inputBinding", new LinkedHashMap<String, Object>() {{
                put("prefix", "--output-folder");
            }});
            put("doc", "Single enforced export directory");
        }});

        inputs.put("user_directories_config", new LinkedHashMap<String, Object>() {{
            put("type", "string");
            put("default", "project-user-paths.json");
            put("inputBinding", new LinkedHashMap<String, Object>() {{
                put("prefix", "--overwrite-user-paths");
            }});
            put("doc", "Configuration to override user paths");
        }});

        // TODO: Parameters

        root.put("inputs", inputs);

        // Arguments
        List<Object> arguments = new ArrayList<>();
        arguments.add(new LinkedHashMap<String, Object>() {{
            put("prefix", "--project");
            put("valueFrom", "$(inputs.project.basename)");
        }});
        root.put("arguments", arguments);

        // Get the whole output directory and return it as result
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("results", new LinkedHashMap<String, Object>() {{
            put("type", "Directory");
            put("outputBinding", new LinkedHashMap<String, Object>() {{
                put("glob", "outputs");
            }});
        }});
        root.put("outputs", outputs);

        // Write YAML
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        try {
            yaml.writeValue(out.toFile(), root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createDiagram(Path tmpPath, RoCrate.RoCrateBuilder builder) {
        Path diagramPath = tmpPath.resolve("diagram.png");
        RenderPipelineRun renderPipelineRun = new RenderPipelineRun(project, diagramPath, new RenderPipelineRunSettings());
        renderPipelineRun.setProgressInfo(getProgressInfo().resolveAndLog("Creating diagram").detachProgress());
        try {
            renderPipelineRun.run();
            builder.addDataEntity(new FileEntity.FileEntityBuilder()
                    .setId("diagram.png")
                    .setLocation(diagramPath)
                    .addType("File")
                    .addType("ImageObject")
                    .addProperty("encodingFormat", "image/png")
                    .addProperty("about", "./")
                    .build());
        } catch (Exception e) {
            getProgressInfo().log(e);
            getProgressInfo().log("Unable to create diagram!");
        }
    }

    private RoCrate.RoCrateBuilder createROCrateBuilder() {
        RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder(getProject().getMetadata().getName(),
                getProject().getMetadata().getSummary().toPlainText(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                getProject().getMetadata().getLicense());
        builder.addContextualEntity(new JsonDescriptor.Builder().addConformsTo("https://w3id.org/workflowhub/workflow-ro-crate/1.0").build());

        // CWL ComputerLanguage contextual entity
        ContextualEntity cwlLanguage = new ContextualEntity.ContextualEntityBuilder()
                .setId("https://w3id.org/workflowhub/workflow-ro-crate#cwl")
                .addType("ComputerLanguage")
                .addProperty("name", "Common Workflow Language")
                .addProperty("alternateName", "CWL")
                .addIdProperty("identifier", "https://w3id.org/cwl/v1.2/")
                .addIdProperty("url", "https://www.commonwl.org/")
                .addProperty("version", "1.2")
                .build();
        builder.addContextualEntity(cwlLanguage);

        // Bioschemas ComputationalWorkflow profile guide
        ContextualEntity bioschemasGuide = new ContextualEntity.ContextualEntityBuilder()
                .setId("https://bioschemas.org/profiles/ComputationalWorkflow/1.0-RELEASE")
                .addType("Guide")
                .addProperty("name", "ComputationalWorkflow Profile")
                .addProperty("version", "1.0-RELEASE")
                .addProperty("description", "Bioschemas specification for describing a Computational Workflow")
                .build();
        builder.addContextualEntity(bioschemasGuide);

        return builder;
    }

    private void addROCrateAuthors(RoCrate.RoCrateBuilder builder) {
        for (JIPipeAuthorMetadata author : project.getMetadata().getAuthors()) {
            PersonEntity.PersonEntityBuilder entityBuilder = new PersonEntity.PersonEntityBuilder();
            entityBuilder.setId(author.getOrcidUrl());
            if (!StringUtils.isNullOrEmpty(author.getFirstName())) {
                entityBuilder.setGivenName(author.getFirstName());
            }
            if (!StringUtils.isNullOrEmpty(author.getLastName())) {
                entityBuilder.setFamilyName(author.getLastName());
            }
            if (!StringUtils.isNullOrEmpty(author.getEmail())) {
                entityBuilder.setEmail(author.getEmail());
            }
            if (!StringUtils.isNullOrEmpty(author.getContact())) {
                entityBuilder.setEmail(author.getContact());
            }
            for (JIPipeOrganizationMetadata affiliation : author.getAffiliations()) {
                entityBuilder.addIdProperty("affiliation", affiliation.getUniqueId());
            }
            builder.addContextualEntity(entityBuilder.build());
        }
    }

    private void addROCrateOrganizations(RoCrate.RoCrateBuilder builder) {
        for (JIPipeAuthorMetadata author : project.getMetadata().getAuthors()) {
            for (JIPipeOrganizationMetadata affiliation : author.getAffiliations()) {
                OrganizationEntity.OrganizationEntityBuilder organizationEntityBuilder = new OrganizationEntity.OrganizationEntityBuilder();
                organizationEntityBuilder.setId(affiliation.getUniqueId());
                organizationEntityBuilder.addProperty("name", affiliation.getName());
                builder.addContextualEntity(organizationEntityBuilder.build());
            }
        }
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
