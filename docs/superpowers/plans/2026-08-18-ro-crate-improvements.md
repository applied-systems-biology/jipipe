# Improved RO-Crate Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make RO-Crate Docker settings configurable, automate Docker image builds via CI, fix Workflow-RO-Crate 1.0 standards compliance, and add instrumentation support for remote RO-Crate creation.

**Architecture:** Application-level defaults (`ROCrateApplicationSettings`) feed into a `ROCrateDockerSettings` parameter collection that is passed to `CreateROCrateRun`. The GUI publisher assistant and the instrumentation `create_ro_crate` operation both create `ROCrateDockerSettings` from these defaults (with per-export overrides). CI builds Docker images from the Linux64 prepackaged tar.gz and pushes to Docker Hub. A CI verification job creates an RO-Crate via instrumentation and validates/executes it with `cwltool`.

**Tech Stack:** Java 21, Maven, JIPipe plugin system, `contrib/jipipe-ro-crate-java-2.1.0`, WebSocket instrumentation API, Docker, `xvfb-run`, `cwltool`, GitLab CI, git-lfs

## Global Constraints

- Java 21 required
- `dockerEnvVars` must be `StringAndStringPairParameterList` (not `Map<String, String>`) in all JIPipe parameter contexts
- Docker entrypoint uses `xvfb-run -a` (not a custom Xvfb script)
- Docker image built from `JIPipe-*-Prepackaged-Linux64.tar.gz` (not from JARs)
- Test data stored in-repo with git-lfs for large files
- CWL version is v1.2
- Workflow-RO-Crate profile 1.0 compliance target
- Follow existing JIPipe plugin registration patterns (`JIPipePrepackagedDefaultJavaPlugin`, `registerApplicationSettingsSheet`, `registerInstrumentationOperation`)

---

## File Structure

### New Files

| File | Responsibility |
|------|---------------|
| `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/rocrate/ROCrateDockerSettings.java` | Parameter collection holding docker image, tag, env vars pair list. Provides `getEnvVarsAsMap()`. |
| `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/rocrate/ROCrateApplicationSettings.java` | Application settings sheet with default Docker config. |
| `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/ROCrateOperations.java` | `create_ro_crate` instrumentation operation. |
| `dist/docker/Dockerfile` | Docker image definition using Linux64 prepackaged tar.gz + xvfb-run. |
| `dist/docker/build.sh` | Build script for Docker image. |
| `dist/docker/verify_ro_crate.py` | WebSocket client for CI RO-Crate creation + verification. |
| `dist/docker/test-data/kidney.jip` | Test project (from JSC_2025_06_Kidney_Exports). |
| `dist/docker/test-data/kidney_with_user_dirs.jip` | Test project with user dirs. |
| `dist/docker/test-data/raw/` | Input data (git-lfs). |
| `dist/docker/test-data/results/` | Expected output reference (git-lfs). |

### Modified Files

| File | Changes |
|------|---------|
| `jipipe-core/.../publish/rocrate/CreateROCrateRun.java` | Accept `ROCrateDockerSettings`, use it in CWL generation, add standards compliance fixes |
| `jipipe-core/.../publish/rocrate/ROCratePublisherAssistant.java` | Add advanced Docker settings section, pass to `CreateROCrateRun` |
| `jipipe-core/.../publish/PublishPlugin.java` | Register `ROCrateApplicationSettings` |
| `jipipe-core/.../api/instrumentation/InstrumentationAPI.java` | Add `createROCrate()` static method |
| `jipipe-core/.../plugins/instrumentation/InstrumentationPlugin.java` | Register `create_ro_crate` operation |
| `.gitlab-ci.yml` | Add `docker_build_push` and `verify_ro_crate` jobs |
| `.gitattributes` | Configure git-lfs for test data |

---

## Task 1: ROCrateDockerSettings Parameter Collection

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/rocrate/ROCrateDockerSettings.java`

**Interfaces:**
- Produces: `ROCrateDockerSettings` class with `getDockerImage()`, `getDockerTag()`, `getDockerEnvVars()` (returns `StringAndStringPairParameterList`), `getEnvVarsAsMap()` (returns `Map<String, String>`)

- [ ] **Step 1: Write the ROCrateDockerSettings class**

```java
package org.hkijena.jipipe.plugins.publish.rocrate;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.hkijena.jipipe.utils.VersionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class ROCrateDockerSettings extends AbstractJIPipeParameterCollection {

    private String dockerImage = "appsysbiohkijena/jipipe";
    private String dockerTag = VersionUtils.getJIPipeVersion();
    private StringAndStringPairParameterList dockerEnvVars = defaultEnvVars();

    public ROCrateDockerSettings() {
    }

    public ROCrateDockerSettings(ROCrateDockerSettings other) {
        this.dockerImage = other.dockerImage;
        this.dockerTag = other.dockerTag;
        this.dockerEnvVars = new StringAndStringPairParameterList(other.dockerEnvVars);
    }

    private static StringAndStringPairParameterList defaultEnvVars() {
        StringAndStringPairParameterList list = new StringAndStringPairParameterList();
        list.add(new StringAndStringPairParameter("JAVA_TOOL_OPTIONS", "-Duser.home=/tmp -Djava.util.prefs.userRoot=/tmp/.java"));
        list.add(new StringAndStringPairParameter("XDG_CACHE_HOME", "/tmp/.cache"));
        list.add(new StringAndStringPairParameter("XDG_CONFIG_HOME", "/tmp/.config"));
        list.add(new StringAndStringPairParameter("XDG_DATA_HOME", "/tmp/.local/share"));
        return list;
    }

    @SetJIPipeDocumentation(name = "Docker image", description = "The Docker image name used in the CWL DockerRequirement.")
    @JIPipeParameter("docker-image")
    public String getDockerImage() {
        return dockerImage;
    }

    @JIPipeParameter("docker-image")
    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    @SetJIPipeDocumentation(name = "Docker tag", description = "The Docker image tag used in the CWL DockerRequirement.")
    @JIPipeParameter("docker-tag")
    public String getDockerTag() {
        return dockerTag;
    }

    @JIPipeParameter("docker-tag")
    public void setDockerTag(String dockerTag) {
        this.dockerTag = dockerTag;
    }

    @SetJIPipeDocumentation(name = "Docker environment variables", description = "Environment variables passed to the Docker container via the CWL EnvVarRequirement.")
    @JIPipeParameter("docker-env-vars")
    public StringAndStringPairParameterList getDockerEnvVars() {
        return dockerEnvVars;
    }

    @JIPipeParameter("docker-env-vars")
    public void setDockerEnvVars(StringAndStringPairParameterList dockerEnvVars) {
        this.dockerEnvVars = dockerEnvVars;
    }

    public Map<String, String> getEnvVarsAsMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (StringAndStringPairParameter pair : dockerEnvVars) {
            map.put(pair.getKey(), pair.getValue());
        }
        return map;
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/rocrate/ROCrateDockerSettings.java
git commit -m "Add ROCrateDockerSettings parameter collection"
```

---

## Task 2: ROCrateApplicationSettings

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/rocrate/ROCrateApplicationSettings.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/PublishPlugin.java`

**Interfaces:**
- Consumes: `ROCrateDockerSettings` from Task 1
- Produces: `ROCrateApplicationSettings` with `getInstance()` static method, `ID` field, and getters for `dockerImage`, `dockerTag`, `dockerEnvVars`

- [ ] **Step 1: Write the ROCrateApplicationSettings class**

```java
package org.hkijena.jipipe.plugins.publish.rocrate;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.hkijena.jipipe.utils.VersionUtils;

import javax.swing.*;

public class ROCrateApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static final String ID = "org.hkijena.jipipe:publish:ro-crate";

    private String dockerImage = "appsysbiohkijena/jipipe";
    private String dockerTag = VersionUtils.getJIPipeVersion();
    private StringAndStringPairParameterList dockerEnvVars = defaultEnvVars();

    public ROCrateApplicationSettings() {
    }

    private static StringAndStringPairParameterList defaultEnvVars() {
        StringAndStringPairParameterList list = new StringAndStringPairParameterList();
        list.add(new org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter("JAVA_TOOL_OPTIONS", "-Duser.home=/tmp -Djava.util.prefs.userRoot=/tmp/.java"));
        list.add(new org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter("XDG_CACHE_HOME", "/tmp/.cache"));
        list.add(new org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter("XDG_CONFIG_HOME", "/tmp/.config"));
        list.add(new org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter("XDG_DATA_HOME", "/tmp/.local/share"));
        return list;
    }

    public static ROCrateApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, ROCrateApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Docker image", description = "The default Docker image name used in the CWL DockerRequirement when creating RO-Crates.")
    @JIPipeParameter("docker-image")
    public String getDockerImage() {
        return dockerImage;
    }

    @JIPipeParameter("docker-image")
    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    @SetJIPipeDocumentation(name = "Docker tag", description = "The default Docker image tag used in the CWL DockerRequirement when creating RO-Crates.")
    @JIPipeParameter("docker-tag")
    public String getDockerTag() {
        return dockerTag;
    }

    @JIPipeParameter("docker-tag")
    public void setDockerTag(String dockerTag) {
        this.dockerTag = dockerTag;
    }

    @SetJIPipeDocumentation(name = "Docker environment variables", description = "The default environment variables passed to the Docker container via the CWL EnvVarRequirement.")
    @JIPipeParameter("docker-env-vars")
    public StringAndStringPairParameterList getDockerEnvVars() {
        return dockerEnvVars;
    }

    @JIPipeParameter("docker-env-vars")
    public void setDockerEnvVars(StringAndStringPairParameterList dockerEnvVars) {
        this.dockerEnvVars = dockerEnvVars;
    }

    public ROCrateDockerSettings toDockerSettings() {
        ROCrateDockerSettings settings = new ROCrateDockerSettings();
        settings.setDockerImage(dockerImage);
        settings.setDockerTag(dockerTag);
        settings.setDockerEnvVars(new StringAndStringPairParameterList(dockerEnvVars));
        return settings;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Plugins;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("apps/ro-crate.png");
    }

    @Override
    public String getName() {
        return "RO-Crate";
    }

    @Override
    public String getDescription() {
        return "Settings for RO-Crate creation.";
    }
}
```

- [ ] **Step 2: Register the settings in PublishPlugin**

In `PublishPlugin.java`, add to the `register()` method (after the existing `registerMenuExtension` calls):

```java
registerApplicationSettingsSheet(new ROCrateApplicationSettings());
```

Add the import:
```java
import org.hkijena.jipipe.plugins.publish.rocrate.ROCrateApplicationSettings;
```

- [ ] **Step 3: Verify it compiles**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/rocrate/ROCrateApplicationSettings.java
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/PublishPlugin.java
git commit -m "Add ROCrateApplicationSettings with default Docker config"
```

---

## Task 3: Refactor CreateROCrateRun for Docker Settings + Standards Compliance

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/rocrate/CreateROCrateRun.java`

**Interfaces:**
- Consumes: `ROCrateDockerSettings` from Task 1
- Produces: `CreateROCrateRun` that accepts `ROCrateDockerSettings`, generates standards-compliant RO-Crate metadata

This is the largest task. It has two sub-parts: (A) Docker settings integration, (B) standards compliance fixes.

- [ ] **Step 1: Add ROCrateDockerSettings field and constructor parameter**

Add a new field and modify the constructor. Add import for `ROCrateDockerSettings`:

```java
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
```

Add field after `archivedProjectUserPaths`:

```java
private final ROCrateDockerSettings dockerSettings;
```

Add a new constructor that accepts docker settings:

```java
public CreateROCrateRun(JIPipeProject project, Path projectFile, Path roCrateFile,
                        Map<String, JIPipeProjectUserPaths.Role> archivedProjectUserPaths,
                        ROCrateDockerSettings dockerSettings) {
    this.project = project;
    this.projectFile = projectFile;
    this.roCrateFile = roCrateFile;
    this.archivedProjectUserPaths = archivedProjectUserPaths;
    this.dockerSettings = dockerSettings;
}
```

Keep the old constructor for backward compatibility, delegating to the new one with defaults from `ROCrateApplicationSettings`:

```java
public CreateROCrateRun(JIPipeProject project, Path projectFile, Path roCrateFile,
                        Map<String, JIPipeProjectUserPaths.Role> archivedProjectUserPaths) {
    this(project, projectFile, roCrateFile, archivedProjectUserPaths,
            ROCrateApplicationSettings.getInstance().toDockerSettings());
}
```

- [ ] **Step 2: Update createWorkflowCwl() to use Docker settings**

Replace the hardcoded Docker requirement section in `createWorkflowCwl()`:

Old code (lines ~278-307):
```java
String version = String.valueOf(VersionUtils.getJIPipeVersion());
// ...
Map<String, Object> dockerReq = new LinkedHashMap<>();
dockerReq.put("class", "DockerRequirement");
dockerReq.put("dockerPull", "appsysbiohkijena/jipipe:" + version);
requirements.add(dockerReq);
// ...
Map<String, Object> envReq = new LinkedHashMap<>();
envReq.put("class", "EnvVarRequirement");
Map<String, Object> envDef = new LinkedHashMap<>();
envDef.put("JAVA_TOOL_OPTIONS", "-Duser.home=/tmp -Djava.util.prefs.userRoot=/tmp/.java");
envDef.put("XDG_CACHE_HOME", "/tmp/.cache");
envDef.put("XDG_CONFIG_HOME", "/tmp/.config");
envDef.put("XDG_DATA_HOME", "/tmp/.local/share");
envReq.put("envDef", envDef);
requirements.add(envReq);
```

New code:
```java
Map<String, Object> dockerReq = new LinkedHashMap<>();
dockerReq.put("class", "DockerRequirement");
dockerReq.put("dockerPull", dockerSettings.getDockerImage() + ":" + dockerSettings.getDockerTag());
requirements.add(dockerReq);
// ...
Map<String, Object> envReq = new LinkedHashMap<>();
envReq.put("class", "EnvVarRequirement");
Map<String, Object> envDef = new LinkedHashMap<>();
for (Map.Entry<String, String> entry : dockerSettings.getEnvVarsAsMap().entrySet()) {
    envDef.put(entry.getKey(), entry.getValue());
}
envReq.put("envDef", envDef);
requirements.add(envReq);
```

Remove the `String version = String.valueOf(VersionUtils.getJIPipeVersion());` line since it's no longer used here.

- [ ] **Step 3: Add CWL ComputerLanguage contextual entity**

In `createROCrateBuilder()`, after the `JsonDescriptor` contextual entity is added, add the CWL language entity and Bioschemas guide entities:

```java
private RoCrate.RoCrateBuilder createROCrateBuilder() {
    RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder(getProject().getMetadata().getName(),
            getProject().getMetadata().getSummary().toPlainText(),
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            getProject().getMetadata().getLicense());
    builder.addContextualEntity(new JsonDescriptor.Builder().addConformsTo("https://w3id.org/ro/crate/1.1").addConformsTo("https://w3id.org/workflowhub/workflow-ro-crate/1.0").build());

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
```

Add import:
```java
import org.hkijena.jipipe.contrib.ro_crate.entities.contextual.ContextualEntity;
```

Note: Also change `DateTimeFormatter.ISO_LOCAL_DATE` to `DateTimeFormatter.ISO_LOCAL_DATE_TIME` for the `datePublished` field.

- [ ] **Step 4: Add Bioschemas conformsTo and encodingFormat to the workflow entity**

In `addROCrateCwlWorkflow()`, add `conformsTo` and `encodingFormat`:

```java
private void addROCrateCwlWorkflow(RoCrate.RoCrateBuilder builder, Path tmpPath) {
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
```

- [ ] **Step 5: Add encodingFormat to other file entities**

In `addProjectToROCrate()`, add encodingFormat to `project.jip`:
```java
builder.addDataEntity(new FileEntity.FileEntityBuilder()
        .setId("./project.jip")
        .setLocation(tmpPath.resolve("project.jip"))
        .addProperty("encodingFormat", "application/json")
        .build());
```

In `copyProjectUserPaths()`, add encodingFormat to `project-user-paths.json`:
```java
builder.addDataEntity(new FileEntity.FileEntityBuilder()
        .setId("project-user-paths.json")
        .setLocation(tmpPath.resolve("project-user-paths.json"))
        .addProperty("encodingFormat", "application/json")
        .build());
```

In `createDiagram()`, add encodingFormat to `diagram.png`:
```java
builder.addDataEntity(new FileEntity.FileEntityBuilder()
        .setId("diagram.png")
        .setLocation(diagramPath)
        .addType("File")
        .addType("ImageObject")
        .addProperty("encodingFormat", "image/png")
        .addProperty("about", "./")
        .build());
```

- [ ] **Step 6: Add keywords to the root data entity**

In `addROCrateMainEntity()` or in `createROCrateBuilder()`, add keywords. The simplest approach is to add it in `createROCrateBuilder()` — but the root data entity is created by the builder. Add after `builder.build()` in `run()`, before `addROCrateMainEntity()`:

Actually, the root data entity is accessible via `crate.getRootDataEntity()` after build. In `run()`, after `RoCrate crate = builder.build();`:

```java
crate.getRootDataEntity().addProperty("keywords", "JIPipe");
```

- [ ] **Step 7: Verify it compiles**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/rocrate/CreateROCrateRun.java
git commit -m "Refactor CreateROCrateRun for Docker settings and RO-Crate standards compliance"
```

---

## Task 4: Update ROCratePublisherAssistant with Advanced Docker Settings

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/rocrate/ROCratePublisherAssistant.java`

**Interfaces:**
- Consumes: `ROCrateDockerSettings` from Task 1, `ROCrateApplicationSettings` from Task 2
- Produces: Updated `ROCratePublisherAssistant` that passes Docker settings to `CreateROCrateRun`

- [ ] **Step 1: Add dockerSettings to the Settings class**

Add a `ROCrateDockerSettings` field to the `Settings` inner class, initialized from `ROCrateApplicationSettings` defaults:

```java
public static class Settings extends AbstractJIPipeParameterCollection {
    private final JIPipeDynamicParameterCollection userDirectories = new JIPipeDynamicParameterCollection();
    private final ROCrateDockerSettings dockerSettings = new ROCrateDockerSettings();

    public Settings() {
        ROCrateApplicationSettings appSettings = ROCrateApplicationSettings.getInstance();
        dockerSettings.setDockerImage(appSettings.getDockerImage());
        dockerSettings.setDockerTag(appSettings.getDockerTag());
        dockerSettings.setDockerEnvVars(new StringAndStringPairParameterList(appSettings.getDockerEnvVars()));
    }

    @SetJIPipeDocumentation(name = "Project user paths", description = "Each path must be either an input or an output.")
    @JIPipeParameter("user-directories")
    public JIPipeDynamicParameterCollection getUserDirectories() {
        return userDirectories;
    }

    @SetJIPipeDocumentation(name = "Advanced Docker settings", description = "Configure the Docker image, tag, and environment variables used in the generated CWL.")
    @JIPipeParameter("docker-settings")
    public ROCrateDockerSettings getDockerSettings() {
        return dockerSettings;
    }
}
```

Add imports:
```java
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
```

- [ ] **Step 2: Pass dockerSettings to CreateROCrateRun**

In `createAssistantTask()`, update the `CreateROCrateRun` constructor call:

```java
return new CreateROCrateRun(getProject(),
        getDesktopProjectWorkbench().getProjectWindow().getProjectSavePath(),
        crateFile,
        projectDirectorySettings,
        settings.getDockerSettings());
```

- [ ] **Step 3: Verify it compiles**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/publish/rocrate/ROCratePublisherAssistant.java
git commit -m "Add advanced Docker settings to RO-Crate publisher assistant"
```

---

## Task 5: Instrumentation Operation — create_ro_crate

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/ROCrateOperations.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationAPI.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/instrumentation/InstrumentationPlugin.java`

**Interfaces:**
- Consumes: `ROCrateDockerSettings` from Task 1, `ROCrateApplicationSettings` from Task 2, `CreateROCrateRun` from Task 3
- Produces: `create_ro_crate` WebSocket operation and `InstrumentationAPI.createROCrate()` static method

- [ ] **Step 1: Add createROCrate() to InstrumentationAPI**

Add the following static method to `InstrumentationAPI.java`. This method contains the business logic shared between the operation wrapper and the internal AI agent.

```java
public static JsonNode createROCrate(InstrumentationContext ctx, String outputPath,
                                     ROCrateDockerSettings dockerSettings,
                                     Map<String, JIPipeProjectUserPaths.Role> userPathOverrides) {
    JIPipeProject project = ctx.getProject();
    if (project == null) {
        throw new RuntimeException("No project selected");
    }

    Path roCrateFile = Path.of(outputPath);
    Path projectFile = getProjectSavePath(ctx);

    CreateROCrateRun run = new CreateROCrateRun(project, projectFile, roCrateFile,
            userPathOverrides != null ? userPathOverrides : new HashMap<>(),
            dockerSettings);
    run.setProgressInfo(ctx.getProgressInfo().resolveAndLog("Create RO-Crate"));
    run.run();

    ObjectNode result = mapper.createObjectNode();
    result.put("path", roCrateFile.toAbsolutePath().toString());
    try {
        result.put("sizeBytes", java.nio.file.Files.size(roCrateFile));
    } catch (IOException e) {
        result.put("sizeBytes", -1);
    }
    return result;
}
```

Add a helper to get the project save path from the context. The instrumentation context provides access to the desktop project window. Add this method:

```java
private static Path getProjectSavePath(InstrumentationContext ctx) {
    org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow window =
            org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow.getWindowForProject(ctx.getProject());
    if (window != null) {
        return window.getProjectSavePath();
    }
    return null;
}
```

Then update the `createROCrate` method to use `getProjectSavePath(ctx)` instead of `ctx.getProjectSavePath()`.

Add imports to `InstrumentationAPI.java`:
```java
import org.hkijena.jipipe.plugins.publish.rocrate.CreateROCrateRun;
import org.hkijena.jipipe.plugins.publish.rocrate.ROCrateDockerSettings;
import org.hkijena.jipipe.api.project.JIPipeProjectUserPaths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.HashMap;
```

- [ ] **Step 2: Write the ROCrateOperations.java file**

```java
package org.hkijena.jipipe.api.instrumentation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.instrumentation.InstrumentationAPI;
import org.hkijena.jipipe.api.instrumentation.InstrumentationContext;
import org.hkijena.jipipe.api.instrumentation.InstrumentationOperation;
import org.hkijena.jipipe.api.project.JIPipeProjectUserPaths;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.hkijena.jipipe.plugins.publish.rocrate.ROCrateApplicationSettings;
import org.hkijena.jipipe.plugins.publish.rocrate.ROCrateDockerSettings;

import java.util.HashMap;
import java.util.Map;

public class ROCrateOperations {

    public static class CreateROCrate implements InstrumentationOperation {
        @Override
        public String getId() {
            return "create_ro_crate";
        }

        @Override
        public String getDescription() {
            return "Create a Workflow RO-Crate from the currently selected project";
        }

        @Override
        public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String outputPath = params.get("outputPath").asText();

            ROCrateDockerSettings dockerSettings;
            if (params.has("dockerImage") || params.has("dockerTag") || params.has("dockerEnvVars")) {
                dockerSettings = ROCrateApplicationSettings.getInstance().toDockerSettings();
                if (params.has("dockerImage")) {
                    dockerSettings.setDockerImage(params.get("dockerImage").asText());
                }
                if (params.has("dockerTag")) {
                    dockerSettings.setDockerTag(params.get("dockerTag").asText());
                }
                if (params.has("dockerEnvVars")) {
                    StringAndStringPairParameterList envVars = new StringAndStringPairParameterList();
                    for (JsonNode entry : params.get("dockerEnvVars")) {
                        envVars.add(new StringAndStringPairParameter(
                                entry.get("key").asText(),
                                entry.get("value").asText()));
                    }
                    dockerSettings.setDockerEnvVars(envVars);
                }
            } else {
                dockerSettings = ROCrateApplicationSettings.getInstance().toDockerSettings();
            }

            Map<String, JIPipeProjectUserPaths.Role> userPathOverrides = null;
            if (params.has("userPathOverrides")) {
                userPathOverrides = new HashMap<>();
                for (Map.Entry<String, JsonNode> entry : com.google.common.collect.ImmutableList.copyOf(
                        params.get("userPathOverrides").fields())) {
                    String roleStr = entry.getValue().asText();
                    try {
                        JIPipeProjectUserPaths.Role role = JIPipeProjectUserPaths.Role.valueOf(roleStr);
                        userPathOverrides.put(entry.getKey(), role);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid user path role: " + roleStr +
                                ". Valid values: Unspecified, Input, Output, Ignored");
                    }
                }
            }

            return InstrumentationAPI.createROCrate(ctx, outputPath, dockerSettings, userPathOverrides);
        }
    }
}
```

- [ ] **Step 3: Register the operation in InstrumentationPlugin**

In `InstrumentationPlugin.java`, add to the `register()` method:

```java
// RO-Crate operations
registerInstrumentationOperation("create_ro_crate", new ROCrateOperations.CreateROCrate());
```

Add import:
```java
import org.hkijena.jipipe.api.instrumentation.operations.ROCrateOperations;
```

- [ ] **Step 4: Verify it compiles**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/ROCrateOperations.java
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationAPI.java
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/instrumentation/InstrumentationPlugin.java
git commit -m "Add create_ro_crate instrumentation operation"
```

---

## Task 6: Dockerfile and Build Script

**Files:**
- Create: `dist/docker/Dockerfile`
- Create: `dist/docker/build.sh`

**Interfaces:**
- Produces: Docker image `appsysbiohkijena/jipipe:<version>` with `xvfb-run -a` entrypoint

- [ ] **Step 1: Write the Dockerfile**

```dockerfile
FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libxext6 \
    libx11-6 \
    libgl1 \
    libglib2.0-0 \
    libfontconfig1 \
    libfreetype6 \
    libnspr4 \
    libnss3 \
    libdbus-1-3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2t64 \
    libopencv-dev \
    && rm -rf /var/lib/apt/lists/*

ARG PACKAGE_PATH

ADD ${PACKAGE_PATH} /tmp/jipipe.tar.gz
RUN mkdir -p /opt/jipipe && \
    tar -xzf /tmp/jipipe.tar.gz -C /opt/jipipe --strip-components=1 && \
    rm /tmp/jipipe.tar.gz && \
    chmod +x /opt/jipipe/jipipe-linux-x64

ENTRYPOINT ["xvfb-run", "-a", "/opt/jipipe/jipipe-linux-x64"]
```

- [ ] **Step 2: Write the build script**

```bash
#!/bin/bash
set -euo pipefail

PACKAGE_PATH="${1:?Usage: build.sh <path-to-linux64-tar.gz> <version>}"
VERSION="${2:?Usage: build.sh <path-to-linux64-tar.gz> <version>}"

IMAGE_NAME="appsysbiohkijena/jipipe"

echo "Building Docker image ${IMAGE_NAME}:${VERSION} from ${PACKAGE_PATH}"

docker build \
    --build-arg PACKAGE_PATH="${PACKAGE_PATH}" \
    -t "${IMAGE_NAME}:${VERSION}" \
    -t "${IMAGE_NAME}:latest" \
    -f Dockerfile \
    .

echo "Built ${IMAGE_NAME}:${VERSION} and ${IMAGE_NAME}:latest"
```

Make it executable: `chmod +x dist/docker/build.sh`

- [ ] **Step 3: Commit**

```bash
git add dist/docker/Dockerfile dist/docker/build.sh
git commit -m "Add Dockerfile and build script for JIPipe Docker image"
```

---

## Task 7: CI Jobs — docker_build_push and verify_ro_crate

**Files:**
- Modify: `.gitlab-ci.yml`
- Create: `dist/docker/verify_ro_crate.py`

**Interfaces:**
- Consumes: Docker image from Task 6, instrumentation operation from Task 5, test data

- [ ] **Step 1: Write the verify_ro_crate.py script**

```python
#!/usr/bin/env python3
"""Create an RO-Crate via instrumentation and verify it with cwltool."""
import asyncio
import json
import sys
import time
import zipfile
from pathlib import Path

import websockets

WS_URL = "ws://127.0.0.1:8780/"


async def create_ro_crate(project_path: str, output_path: str, timeout: int = 120) -> dict:
    async with websockets.connect(WS_URL, max_size=50 * 1024 * 1024) as ws:
        # Wait for project_list
        msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=30))
        assert msg["type"] == "project_list", f"Expected project_list, got {msg['type']}"

        # Open the project
        await ws.send(json.dumps({
            "type": "open_project",
            "path": project_path,
            "forceCurrentWindow": False,
        }))

        # Wait for the project to appear
        project_id = None
        for _ in range(60):
            try:
                msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=5))
                if msg["type"] == "project_list":
                    for p in msg.get("projects", []):
                        project_id = p["id"]
                        break
                    if project_id:
                        break
            except asyncio.TimeoutError:
                continue

        if not project_id:
            raise RuntimeError("Project did not open")

        # Select the project
        await ws.send(json.dumps({
            "type": "select_project",
            "projectId": project_id,
            "requestId": "select",
        }))

        # Wait for project_changed
        for _ in range(20):
            msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=5))
            if msg["type"] == "project_changed":
                break

        # Create RO-Crate
        await ws.send(json.dumps({
            "type": "create_ro_crate",
            "requestId": "create",
            "outputPath": output_path,
        }))

        # Wait for operation_result
        deadline = time.time() + timeout
        while time.time() < deadline:
            msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=deadline - time.time()))
            if msg["type"] == "operation_result" and msg.get("requestId") == "create":
                if "error" in msg:
                    raise RuntimeError(f"RO-Crate creation failed: {msg['error']}")
                return msg.get("data", {})
            elif msg["type"] == "error":
                raise RuntimeError(f"Server error: {msg.get('message', 'unknown')}")

        raise RuntimeError("Timeout waiting for RO-Crate creation")


async def main():
    project_path = sys.argv[1]
    output_path = sys.argv[2]

    print(f"Creating RO-Crate from {project_path} -> {output_path}")
    result = await create_ro_crate(project_path, output_path)
    print(f"RO-Crate created: {result}")

    # Verify the zip exists
    crate_file = Path(output_path)
    if not crate_file.exists():
        print(f"ERROR: Output file {crate_file} does not exist")
        sys.exit(1)

    # Extract the crate
    extract_dir = Path("/tmp/crate-extracted")
    extract_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(crate_file, 'r') as z:
        z.extractall(extract_dir)

    # Check workflow.cwl exists
    cwl_path = extract_dir / "workflow.cwl"
    if not cwl_path.exists():
        print(f"ERROR: workflow.cwl not found in crate")
        sys.exit(1)

    print(f"RO-Crate extracted to {extract_dir}")
    print(f"workflow.cwl found at {cwl_path}")


if __name__ == "__main__":
    asyncio.run(main())
```

- [ ] **Step 2: Add docker_build_push job to .gitlab-ci.yml**

Add after the `upload_to_package_registry` job:

```yaml
docker_build_push:
  stage: publish
  needs:
    - job: upload_to_package_registry
      artifacts: true
  image: docker:24
  services:
    - docker:24-dind
  before_script:
    - apk add --no-cache curl jq bash
  script:
    - set -euo pipefail
    - |
      OUTDIR="$(ls -d jipipe-distribution-files/*-snapshot${CI_PIPELINE_IID} | head -n 1)"
      test -n "$OUTDIR" || { echo "Snapshot outdir not found"; exit 1; }
    - |
      TAR_GZ="$(ls "$OUTDIR"/JIPipe-*-Prepackaged-Linux64.tar.gz | head -n 1)"
      test -n "$TAR_GZ" || { echo "Linux64 tar.gz not found"; ls -lah "$OUTDIR"; exit 1; }
    - |
      BASE="$(basename "$TAR_GZ")"
      BASE="${BASE#JIPipe-}"
      VERSION_BASE="${BASE%-Prepackaged-Linux64.tar.gz}"
      VERSION="${VERSION_BASE%-snapshot*}"
      echo "JIPipe version: ${VERSION}"
    - |
      docker build \
        --build-arg PACKAGE_PATH="$TAR_GZ" \
        -t appsysbiohkijena/jipipe:${VERSION} \
        -t appsysbiohkijena/jipipe:latest \
        -f dist/docker/Dockerfile \
        .
    - |
      echo "$DOCKER_HUB_TOKEN" | docker login -u "$DOCKER_HUB_USERNAME" --password-stdin
      docker push appsysbiohkijena/jipipe:${VERSION}
      docker push appsysbiohkijena/jipipe:latest
  rules:
    - if: $CI_COMMIT_BRANCH == "master"
```

- [ ] **Step 3: Add verify_ro_crate job to .gitlab-ci.yml**

Add after `docker_build_push`:

```yaml
verify_ro_crate:
  stage: publish
  needs:
    - job: docker_build_push
  image: docker:24
  services:
    - docker:24-dind
  before_script:
    - apk add --no-cache python3 py3-pip curl jq bash
  script:
    - set -euo pipefail
    - |
      pip install --break-system-packages websockets cwltool
    - |
      docker run -d --name jipipe-verify \
        -v "$(pwd)/dist/docker/test-data:/test-data:ro" \
        -v /tmp/jipipe-output:/output \
        appsysbiohkijena/jipipe:latest \
        --instrumentation 8780
    - |
      sleep 30
      python3 dist/docker/verify_ro_crate.py /test-data/kidney_with_user_dirs.jip /output/kidney.crate.zip
    - |
      cwltool --validate /tmp/crate-extracted/workflow.cwl
    - |
      echo "RO-Crate validation passed"
    - |
      docker stop jipipe-verify || true
      docker rm jipipe-verify || true
  after_script:
    - docker stop jipipe-verify || true
    - docker rm jipipe-verify || true
  rules:
    - if: $CI_COMMIT_BRANCH == "master"
```

- [ ] **Step 4: Commit**

```bash
git add dist/docker/verify_ro_crate.py .gitlab-ci.yml
git commit -m "Add CI jobs for Docker build/push and RO-Crate verification"
```

---

## Task 8: Test Data Setup

**Files:**
- Create: `dist/docker/test-data/kidney.jip`
- Create: `dist/docker/test-data/kidney_with_user_dirs.jip`
- Create: `dist/docker/test-data/raw/` (git-lfs)
- Create: `dist/docker/test-data/results/` (git-lfs)
- Create: `.gitattributes`

- [ ] **Step 1: Copy test project files**

```bash
cp /data/JIPipe/AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney.jip dist/docker/test-data/
cp /data/JIPipe/AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/kidney_with_user_dirs.jip dist/docker/test-data/
cp -r /data/JIPipe/AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/raw dist/docker/test-data/
cp -r /data/JIPipe/AIAgentTests/01-BasicWorkshop/JSC_2025_06_Kidney_Exports/results dist/docker/test-data/
```

- [ ] **Step 2: Configure git-lfs**

```bash
git lfs install
git lfs track "dist/docker/test-data/raw/**"
git lfs track "dist/docker/test-data/results/**"
```

Write `.gitattributes`:
```
dist/docker/test-data/raw/** filter=lfs diff=lfs merge=lfs -text
dist/docker/test-data/results/** filter=lfs diff=lfs merge=lfs -text
```

- [ ] **Step 3: Add and commit**

```bash
git add .gitattributes dist/docker/test-data/
git commit -m "Add test data for RO-Crate CI verification"
```

---

## Task 9: Full Build Verification

- [ ] **Step 1: Run full Maven compile**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run existing tests**

Run: `mvn test -pl contrib/jipipe-ro-crate-java-2.1.0 -q`
Expected: All tests pass

- [ ] **Step 3: Run instrumentation tests**

Run: `mvn test -pl jipipe-core -Dtest=Instrumentation*Test -q`
Expected: All tests pass

- [ ] **Step 4: Commit any remaining changes**

```bash
git add -A
git commit -m "Final verification and cleanup" || echo "Nothing to commit"
```
