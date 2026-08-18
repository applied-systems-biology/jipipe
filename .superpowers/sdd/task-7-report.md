# Task 7 Report: CI Jobs — docker_build_push and verify_ro_crate

## What I Implemented

1. **`dist/docker/verify_ro_crate.py`** — Python script that connects to the JIPipe instrumentation WebSocket, opens a project, selects it, sends `create_ro_crate`, then extracts and checks the resulting zip for `workflow.cwl`.

2. **`.gitlab-ci.yml`** — Added two new CI jobs after `upload_to_package_registry`:
   - **`docker_build_push`**: Downloads the Linux64 tar.gz from distribution artifacts, builds the Docker image using `dist/docker/Dockerfile`, tags with version + latest, pushes to Docker Hub.
   - **`verify_ro_crate`**: Starts the JIPipe Docker container with `--instrumentation 8780`, runs the Python verification script, validates the RO-Crate with `cwltool --validate`.

## Files Changed

- **Modified**: `.gitlab-ci.yml` (+73 lines)
- **Created**: `dist/docker/verify_ro_crate.py` (110 lines, executable)

## Commit

- `8fd31e1d00` — Add CI jobs for Docker build/push and RO-Crate verification

## Self-Review Findings

### YAML Validation
- YAML syntax validated with Python's `yaml.safe_load()` — passes.
- Both jobs are in the `publish` stage, with correct `needs` dependencies (`upload_to_package_registry` → `docker_build_push` → `verify_ro_crate`).
- Both jobs have `rules: - if: $CI_COMMIT_BRANCH == "master"` matching the existing pattern.
- `verify_ro_crate` has `after_script` cleanup for the Docker container.

### Script Review
- The Python script matches the task brief exactly.
- `websockets` library is installed via `pip install --break-system-packages websockets cwltool` in the CI job.

### Issues and Concerns

1. **`open_project` operation does not exist**: The Python script sends `{"type": "open_project", "path": ...}` but there is no `open_project` operation registered in `InstrumentationPlugin` or handled by `InstrumentationServer`. The server would return `"Unknown command: open_project"`. The `InstrumentationServer.onMessage()` only handles `select_project`, `get_job_status`, and `cancel_job` directly; everything else goes through the operation registry which has no `open_project` entry.

   **Possible fix**: Either (a) register an `open_project` operation that calls `JIPipeDesktopProjectWindow.openProject(Path)`, or (b) pass the project path as a command-line argument to the Docker container (the `GuiCommand.startGui()` method supports this via `tryAddOpenProjectOnLoad`), in which case the Python script would skip the `open_project` step and just wait for `project_list` to show the already-open project.

2. **Instrumentation port not exposed**: The `docker run` command doesn't include `-p 8780:8780`. The Python script connects to `ws://127.0.0.1:8780/` from the CI runner, but the instrumentation server is inside the container. Without port publishing, the connection will fail.

3. **Output path mismatch**: The Python script checks `Path("/output/kidney.crate.zip")` on the CI runner, but the file is created inside the JIPipe container at `/output/kidney.crate.zip`, which maps to `/tmp/jipipe-output/kidney.crate.zip` on the CI runner host. The script should check `/tmp/jipipe-output/kidney.crate.zip` instead.

4. **Test data not yet present**: Task 8 creates `dist/docker/test-data/` with the `kidney_with_user_dirs.jip` file. The `verify_ro_crate` job will fail until Task 8 is completed.

5. **Docker Hub credentials**: The `docker_build_push` job requires `$DOCKER_HUB_USERNAME` and `$DOCKER_HUB_TOKEN` CI/CD variables to be configured.

These issues are inherent to the task brief as specified. They would need to be resolved before the CI pipeline can successfully run end-to-end.

## Post-Review Fixes

The following issues identified in the self-review (and by Task 7 review) have been fixed:

### Fix 1: `open_project` is not an instrumentation operation
- **Problem**: The Python script sent `{"type": "open_project", ...}` to the instrumentation WebSocket, but no such operation exists. `open_project` is an AI agent monitor command (port 8770), not an instrumentation server command (port 8780).
- **Fix**: Removed the `open_project` message from `verify_ro_crate.py`. The project is now auto-opened by passing the project file path as a CLI argument to the Docker container (`/test-data/kidney_with_user_dirs.jip` after `--instrumentation 8780`). The JIPipe launcher's `GuiCommand` supports this via `tryAddOpenProjectOnLoad`.

### Fix 2: Docker port not exposed
- **Problem**: The `docker run` command was missing `-p 8780:8780` to publish the instrumentation port from the container to the CI runner.
- **Fix**: Added `-p 8780:8780` to the `docker run` command in `.gitlab-ci.yml`.

### Fix 3: verify_ro_crate.py flow updated
- **Problem**: The script used `open_project` and checked the output file at the container path (`/output/kidney.crate.zip`) instead of the CI runner host path.
- **Fix**: Rewrote the script flow to:
  1. Connect to `ws://127.0.0.1:8780/`
  2. Wait for `project_list` (project auto-opened via CLI arg)
  3. Find the project in the list, send `select_project`
  4. Wait for `project_changed`
  5. Send `create_ro_crate` with `outputPath` (container path)
  6. Wait for `operation_result`
  7. Check the file at the host path (`/tmp/jipipe-output/kidney.crate.zip`)
  8. Extract to `/tmp/crate-extracted` and verify `workflow.cwl` exists

  The script now takes two arguments: `<container_output_path>` (sent to server) and `<host_output_path>` (checked on CI runner).

### Fix 4: Output path references in CI job
- **Problem**: The extraction and `cwltool --validate` referenced paths that didn't account for the volume mount mapping (`/output` in container → `/tmp/jipipe-output` on host).
- **Fix**: The Python script (running on the CI runner) extracts from `/tmp/jipipe-output/kidney.crate.zip` to `/tmp/crate-extracted/`. The CI job then runs `cwltool --validate /tmp/crate-extracted/workflow.cwl` on the CI runner.

### Remaining open items (not fixed — depend on other tasks)
- **Test data**: Task 8 creates `dist/docker/test-data/kidney_with_user_dirs.jip`.
- **Docker Hub credentials**: `$DOCKER_HUB_USERNAME` and `$DOCKER_HUB_TOKEN` must be configured as CI/CD variables.
