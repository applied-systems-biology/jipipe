# Improved RO-Crate Generation (#1308)

## Overview

This spec addresses GitLab work item #1308: improving JIPipe's RO-Crate generation to comply with the [Workflow RO-Crate 1.0 profile](https://about.workflowhub.eu/Workflow-RO-Crate/), make Docker settings configurable, automate Docker image builds via CI, verify RO-Crate execution with standard tools, and add instrumentation support for remote RO-Crate creation.

## Scope

Four parts:
1. **Docker settings** — Application-level defaults with per-export override
2. **CI Docker builds** — Automated Docker image builds pushed to Docker Hub with version-based tags
3. **RO-Crate standards compliance** — Fix metadata to satisfy Workflow-RO-Crate 1.0 profile
4. **Instrumentation + verification** — Synchronous `create_ro_crate` operation + CI verification with `cwltool`

## Part 1: Docker Settings

### Application Defaults

**New class**: `ROCrateApplicationSettings` extends `JIPipeDefaultApplicationsSettingsSheet`, registered via `PublishPlugin`.

Stores the default Docker configuration used when creating RO-Crates:

| Parameter | Type | Default |
|-----------|------|---------|
| `dockerImage` | String | `"appsysbiohkijena/jipipe"` |
| `dockerTag` | String | `VersionUtils.getJIPipeVersion()` (evaluated dynamically at access time) |
| `dockerEnvVars` | `StringAndStringPairParameterList` | Entries: `JAVA_TOOL_OPTIONS=-Duser.home=/tmp -Djava.util.prefs.userRoot=/tmp/.java`, `XDG_CACHE_HOME=/tmp/.cache`, `XDG_CONFIG_HOME=/tmp/.config`, `XDG_DATA_HOME=/tmp/.local/share` |

> **Note**: `dockerEnvVars` is stored as `StringAndStringPairParameterList` (a `JIPipeListParameter<StringAndStringPairParameter>`) because JIPipe's parameter system does not support `Map<String, String>` directly. It is converted to `Map<String, String>` when consumed by `CreateROCrateRun.createWorkflowCwl()`.

### Per-Export Override

**`ROCratePublisherAssistant.Settings`** gains an "Advanced" section with the same three parameters (`dockerImage`, `dockerTag`, `dockerEnvVars`), initialized from `ROCrateApplicationSettings` defaults. Users can override per-export through the publisher assistant UI.

### ROCrateDockerSettings

**New class**: `ROCrateDockerSettings` — a simple `AbstractJIPipeParameterCollection` holding `dockerImage` (String), `dockerTag` (String), `dockerEnvVars` (`StringAndStringPairParameterList`). Constructed from either application defaults or per-export overrides. Passed to `CreateROCrateRun`. Provides a `getEnvVarsAsMap()` convenience method that converts the pair list to `Map<String, String>` for CWL generation.

### CreateROCrateRun Refactoring

`CreateROCrateRun` is refactored:
- Constructor accepts `ROCrateDockerSettings` parameter
- `createWorkflowCwl()` uses `settings.getDockerImage()` + `settings.getDockerTag()` for the `DockerRequirement` section instead of hardcoded values
- `createWorkflowCwl()` uses `settings.getEnvVarsAsMap()` for the `EnvVarRequirement` section instead of hardcoded env vars (converts `StringAndStringPairParameterList` to `Map<String, String>`)
- Backward-compatible: if no settings provided, uses `ROCrateApplicationSettings` defaults

### Files Changed

- **New**: `jipipe-core/.../plugins/publish/rocrate/ROCrateApplicationSettings.java`
- **New**: `jipipe-core/.../plugins/publish/rocrate/ROCrateDockerSettings.java`
- **Modified**: `jipipe-core/.../plugins/publish/rocrate/CreateROCrateRun.java` — accept settings, use them in CWL generation
- **Modified**: `jipipe-core/.../plugins/publish/rocrate/ROCratePublisherAssistant.java` — add advanced settings section
- **Modified**: `jipipe-core/.../plugins/publish/PublishPlugin.java` — register `ROCrateApplicationSettings`

## Part 2: CI Docker Build

### Dockerfile

**Location**: `dist/docker/Dockerfile`

**Base image**: `ubuntu:24.04`

**Installed packages**:
- `xvfb` — for headless AWT/ImageJ ROI support
- Native X libraries: `libxrender1`, `libxtst6`, `libxi6`, `libxext6`, `libx11-6`, `libgl1`, `libglib2.0-0`, `libfontconfig1`, `libfreetype6`, `libnspr4`, `libnss3`, `libdbus-1-3`, `libatk1.0-0`, `libatk-bridge2.0-0`, `libcups2`, `libdrm2`, `libxcb1`, `libxcomposite1`, `libxdamage1`, `libxfixes3`, `libxrandr2`, `libgbm1`, `libpango-1.0-0`, `libcairo2`, `libasound2t64`
- `libopencv-dev` — for image processing

**Build**: The Dockerfile accepts the `JIPipe-*-Prepackaged-Linux64.tar.gz` as a build arg. The tar.gz is extracted into `/opt/jipipe/`.

**Entrypoint**: `xvfb-run -a /opt/jipipe/jipipe-linux-x64 "$@"` — `xvfb-run -a` automatically picks a free display number, starts Xvfb, sets `DISPLAY`, runs the command, and cleans up on exit. This handles ImageJ's AWT/ROI requirements without a custom entrypoint script.

### Build Script

**Location**: `dist/docker/build.sh`

**Usage**: `./build.sh <path-to-linux64-tar.gz> <version>`

Builds and tags:
- `appsysbiohkijena/jipipe:<version>` (e.g., `6.0.0`)
- `appsysbiohkijena/jipipe:latest`

### CI Job

**Job name**: `docker_build_push`
**Stage**: `publish` (after `upload_to_package_registry`)
**Trigger**: master branch only

**Steps**:
1. Download the `JIPipe-<version>-Prepackaged-Linux64.tar.gz` from the GitLab package registry (URL from `urls-*.txt` artifact)
2. Extract version from the tar.gz filename
3. Run `dist/docker/build.sh <tar.gz-path> <version>` to build the Docker image
4. `docker login -u $DOCKER_HUB_USERNAME -p $DOCKER_HUB_TOKEN` (CI/CD variables)
5. `docker push appsysbiohkijena/jipipe:<version>` and `docker push appsysbiohkijena/jipipe:latest`

**Tag strategy**: The version tag (e.g., `6.0.0`) is derived from the Maven version, stripped of `-SNAPSHOT`. On master commits, this tag points to the latest snapshot. On release, the same tag is overwritten with the release build. `latest` always tracks the newest.

### Files

- **New**: `dist/docker/Dockerfile`
- **New**: `dist/docker/build.sh`
- **Modified**: `.gitlab-ci.yml` — add `docker_build_push` job

## Part 3: RO-Crate Standards Compliance Fixes

The following fixes bring the generated RO-Crate metadata into compliance with the [Workflow RO-Crate 1.0 profile](https://about.workflowhub.eu/Workflow-RO-Crate/) and [RO-Crate 1.1 specification](https://w3id.org/ro/crate/1.1).

### 3.1 CWL ComputerLanguage Contextual Entity (Missing)

Currently, the workflow entity references `https://w3id.org/workflowhub/workflow-ro-crate#cwl` as its `programmingLanguage`, but the corresponding contextual entity is not added to the crate.

**Fix**: Add a contextual entity:
```json
{
  "@id": "https://w3id.org/workflowhub/workflow-ro-crate#cwl",
  "@type": "ComputerLanguage",
  "name": "Common Workflow Language",
  "alternateName": "CWL",
  "identifier": {"@id": "https://w3id.org/cwl/v1.2/"},
  "url": {"@id": "https://www.commonwl.org/"},
  "version": "1.2"
}
```

### 3.2 Bioschemas Conformance (Missing)

The main workflow entity should declare conformance with the Bioschemas ComputationalWorkflow profile.

**Fix**: Add to the workflow entity:
```json
"conformsTo": {"@id": "https://bioschemas.org/profiles/ComputationalWorkflow/1.0-RELEASE"}
```

Add corresponding `Guide` contextual entity:
```json
{
  "@id": "https://bioschemas.org/profiles/ComputationalWorkflow/1.0-RELEASE",
  "@type": "Guide",
  "name": "ComputationalWorkflow Profile",
  "version": "1.0-RELEASE",
  "description": "Bioschemas specification for describing a Computational Workflow"
}
```

### 3.3 Encoding Format on File Entities (Missing)

Add `encodingFormat` to file entities:
- `workflow.cwl` → `"application/x-yaml"`
- `project.jip` → `"application/json"`
- `project-user-paths.json` → `"application/json"`
- `diagram.png` → `"image/png"`
- `README.md` → already has `"text/markdown"` (no change needed)

### 3.4 Keywords (Missing)

Add `keywords` to the root data entity. Source from project metadata (if available) or default to `"JIPipe"`.

### 3.5 DatePublished Format

Currently uses `ISO_LOCAL_DATE` (date only). Change to `ISO_LOCAL_DATE_TIME` to include time, matching the spec's datetime format.

### 3.6 License Format

Currently passes the raw license string (e.g., `"MIT"`). The Workflow-RO-Crate spec shows `license` as an object with `@id`. However, WorkflowHub also accepts raw SPDX license strings.

**Fix**: Wrap in `{"@id": "https://spdx.org/licenses/<license>"}` when the license is a known SPDX identifier. Keep as raw string if not recognized.

### Files Changed

- **Modified**: `jipipe-core/.../plugins/publish/rocrate/CreateROCrateRun.java` — all compliance fixes in the builder methods

## Part 4: Instrumentation Operation

### New Operation: `create_ro_crate`

**Type**: Synchronous `InstrumentationOperation`
**Registration**: `InstrumentationPlugin.register()` via `registerInstrumentationOperation("create_ro_crate", new ROCrateOperations.CreateROCrate())`
**File**: `jipipe-core/.../api/instrumentation/operations/ROCrateOperations.java`

### Request Parameters

```json
{
  "type": "create_ro_crate",
  "requestId": "optional-id",
  "outputPath": "/path/to/output.crate.zip",
  "dockerImage": "appsysbiohkijena/jipipe",
  "dockerTag": "6.0.0",
  "dockerEnvVars": [{"key": "JAVA_TOOL_OPTIONS", "value": "-Duser.home=/tmp"}],
  "userPathOverrides": {
    "input_data": "input",
    "output_data": "output"
  }
}
```

- `outputPath` (required): Path where the `.crate.zip` will be saved
- `dockerImage` (optional): Override Docker image name. Defaults from `ROCrateApplicationSettings`.
- `dockerTag` (optional): Override Docker tag. Defaults from `ROCrateApplicationSettings`.
- `dockerEnvVars` (optional): Override environment variables as a JSON array of `{"key": "...", "value": "..."}` objects (maps to `StringAndStringPairParameterList` internally). Defaults from `ROCrateApplicationSettings`.
- `userPathOverrides` (optional): Map of user path key → role string. Valid roles: `"Unspecified"`, `"Input"`, `"Output"`, `"Ignored"` (matching `JIPipeProjectUserPaths.Role` enum). Defaults to `"Unspecified"` (auto-detect).

### Response

```json
{
  "requestId": "optional-id",
  "data": {
    "path": "/path/to/output.crate.zip",
    "sizeBytes": 1234567
  }
}
```

### Implementation

Delegates to `InstrumentationAPI.createROCrate(ctx, outputPath, dockerSettings, userPathOverrides)`:

1. Gets the selected project from `ctx.getProject()`
2. Gets the project save path from the desktop project window
3. Creates `ROCrateDockerSettings` from params or `ROCrateApplicationSettings` defaults
4. Constructs `CreateROCrateRun` with the settings
5. Runs the creation (synchronous)
6. Returns output path and file size

### Files

- **New**: `jipipe-core/.../api/instrumentation/operations/ROCrateOperations.java` — contains `CreateROCrate` operation class
- **Modified**: `jipipe-core/.../api/instrumentation/InstrumentationAPI.java` — add `createROCrate()` method
- **Modified**: `jipipe-core/.../plugins/instrumentation/InstrumentationPlugin.java` — register the operation

## Part 5: CI Verification with cwltool

### Test Data

Test projects stored in-repo at `dist/docker/test-data/`:
- `kidney.jip` — basic kidney pipeline
- `kidney_with_user_dirs.jip` — kidney pipeline with user directories
- `raw/` — input image data (git-lfs)
- `results/` — expected output reference (git-lfs)

Large files (images) tracked with git-lfs.

### CI Job: `verify_ro_crate`

**Stage**: `publish` (after `docker_build_push`)
**Trigger**: master branch only

**Steps**:
1. Start the JIPipe Docker image in the background (the entrypoint already wraps with `xvfb-run -a`):
   ```bash
   docker run -d --name jipipe-verify \
     -v $(pwd)/dist/docker/test-data:/test-data:ro \
     -v /tmp/output:/output \
     appsysbiohkijena/jipipe:latest \
     --instrumentation 8780
   ```
2. Wait for the instrumentation server to be ready (poll WebSocket connection)
3. Run a Python script (`dist/docker/verify_ro_crate.py`) that:
   - Connects to `ws://<container-ip>:8780`
   - Sends `open_project` with `/test-data/kidney_with_user_dirs.jip`
   - Sends `select_project`
   - Sends `create_ro_crate` with `outputPath: /output/kidney.crate.zip`
   - Waits for `operation_result`
4. Extract the crate: `unzip /output/kidney.crate.zip -d /tmp/crate`
5. Install cwltool: `pip install cwltool`
6. Validate: `cwltool --validate /tmp/crate/workflow.cwl`
7. Execute: `cwltool /tmp/crate/workflow.cwl --project kidney_with_user_dirs.jip --inputs_dir inputs/ --output_dir output/ --user_directories_config project-user-paths.json`
8. Verify output files exist
9. Stop and remove the container
10. Repeat with `kidney.jip` (simpler pipeline without user dirs)

### Verification Script

**Location**: `dist/docker/verify_ro_crate.py`

A lightweight WebSocket client (similar to `agent_test_client.py`) that connects to the instrumentation server, opens a project, creates an RO-Crate, and verifies the result.

### Files

- **New**: `dist/docker/test-data/kidney.jip` (from `JSC_2025_06_Kidney_Exports/`)
- **New**: `dist/docker/test-data/kidney_with_user_dirs.jip` (from `JSC_2025_06_Kidney_Exports/`)
- **New**: `dist/docker/test-data/raw/` (git-lfs)
- **New**: `dist/docker/test-data/results/` (git-lfs)
- **New**: `dist/docker/verify_ro_crate.py`
- **Modified**: `.gitlab-ci.yml` — add `verify_ro_crate` job
- **New**: `.gitattributes` — configure git-lfs for test data

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Application Settings                  │
│            ROCrateApplicationSettings                    │
│  (dockerImage, dockerTag, dockerEnvVars defaults)        │
└──────────────────────┬──────────────────────────────────┘
                       │
           ┌───────────┴───────────┐
           ▼                       ▼
┌─────────────────────┐  ┌─────────────────────────────┐
│  GUI Publisher      │  │  Instrumentation Operation  │
│  Assistant          │  │  create_ro_crate            │
│  (per-export        │  │  (WebSocket API)            │
│   override)         │  │                             │
└─────────┬───────────┘  └──────────┬──────────────────┘
          │                         │
          ▼                         ▼
┌─────────────────────────────────────────────────────────┐
│              CreateROCrateRun                            │
│  Accepts ROCrateDockerSettings                          │
│  Generates CWL + RO-Crate metadata                      │
│  Compliant with Workflow-RO-Crate 1.0                   │
└─────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────┐
│              Output: *.crate.zip                         │
│  Contains: workflow.cwl, project.jip, inputs/,          │
│  README.md, diagram.png, ro-crate-metadata.json         │
└─────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────┐
│              CI Verification                             │
│  cwltool --validate + cwltool execution                  │
│  Uses Docker image with xvfb-run                         │
└─────────────────────────────────────────────────────────┘
```

## Success Criteria

1. Users can customize Docker image, tag, and env vars per-export through the publisher assistant UI
2. Application settings provide sensible defaults for Docker configuration
3. CI builds and pushes Docker images to Docker Hub on master commits with version-based tags
4. Generated RO-Crates pass `cwltool --validate` without errors
5. Generated RO-Crates execute successfully with `cwltool`
6. The `create_ro_crate` instrumentation operation creates RO-Crates via WebSocket
7. RO-Crate metadata includes all required Workflow-RO-Crate 1.0 fields
8. Both `kidney.jip` and `kidney_with_user_dirs.jip` produce valid, executable RO-Crates

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| cwltool execution fails due to Docker-in-Docker requirements | Run cwltool inside a Docker-enabled CI runner with socket mounting |
| xvfb-run hangs in container | `xvfb-run -a` auto-selects display, has built-in timeout logic |
| Large test data bloats repo | Use git-lfs for binary image files |
| Docker Hub rate limits | Use CI/CD variables for Docker Hub credentials with appropriate token |
| RO-Crate library limitations | The existing `contrib/jipipe-ro-crate-java-2.1.0` library supports all needed metadata operations |
