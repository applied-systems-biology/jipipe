# Mirror Snapshots to GitHub Prereleases — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish every master-branch JIPipe snapshot distribution as a GitHub prerelease on `applied-systems-biology/jipipe`.

**Architecture:** Add a dedicated `upload_to_github_prerelease` job to the `publish` stage of `.gitlab-ci.yml`. It consumes `distribution_package` artifacts, infers the snapshot version exactly like the existing `upload_to_package_registry` job, and uses the GitHub CLI `gh` to delete any prior release for that tag and recreate it as a prerelease with all 5 assets.

**Tech Stack:** GitLab CI YAML, GitHub CLI (`gh`), bash, Alpine Linux.

## Global Constraints

- GitHub repo is `applied-systems-biology/jipipe` (NOT `appsysbio/jipipe`).
- Do NOT alter the existing `distribution_package`, `upload_to_package_registry`, `docker_build_push`, or `verify_ro_crate` jobs.
- Do not touch feature-branch (`build_feature_branch`, `feature_branch_package`, `feature_branch_verify_ro_crate`) jobs.
- The new job runs only on `master` (`if: $CI_COMMIT_BRANCH == "master"`).
- Version inference logic must match `upload_to_package_registry` exactly (so GitLab and GitHub publish the same version/files).
- Release must be tagged with the snapshot version, flagged `--prerelease`, and carry all 5 assets.
- No Java/JUnit changes.

---

### Task 1: Add `upload_to_github_prerelease` job

**Files:**
- Modify: `.gitlab-ci.yml` (insert after the `upload_to_package_registry` job, before `docker_build_push`)

**Interfaces:**
- Consumes: artifacts from the `distribution_package` job (`jipipe-distribution-files/*-snapshot${CI_PIPELINE_IID}/`), CI variable `GH_TOKEN`.
- Produces: a GitHub prerelease on `applied-systems-biology/jipipe` and a job exit code (pass/fail); writes `urls-*.txt` (already produced by `upload_to_package_registry`, not re-generated here).

- [ ] **Step 1: Confirm the current `upload_to_package_registry` version block**

Read `.gitlab-ci.yml` lines ~63–140 (the `upload_to_package_registry` job) and note the exact `ls`/`basename`/`grep` logic used to derive `OUTDIR`, `ONE`, `BASE`, `VERSION_BASE`, `VERSION`, and `SNAPSHOT_SUFFIX`. The new job must replicate this verbatim. (Current logic: derive `ONE` from the first non-Prepackaged `JIPipe-*<snapshot>.zip`, strip `JIPipe-` and `-snapshot<CI_PIPELINE_IID>.zip` to get `VERSION_BASE`, set `VERSION="${VERSION_BASE}-snapshot${CI_PIPELINE_IID}"`, and `SNAPSHOT_SUFFIX="${VERSION#${VERSION_BASE}-}"`.)

- [ ] **Step 2: Add the new job to `.gitlab-ci.yml`**

Insert the following job block immediately AFTER the closing of the `upload_to_package_registry` job (after its `artifacts` block ending with `paths:\n      - urls-*.txt`) and BEFORE `docker_build_push`:

```yaml
upload_to_github_prerelease:
  stage: publish
  needs:
    - job: distribution_package
      artifacts: true
  image: alpine:3.19
  before_script:
    - apk add --no-cache github-cli bash curl jq
  script:
    - set -euo pipefail
    - |
      OUTDIR="$(ls -d jipipe-distribution-files/*-snapshot${CI_PIPELINE_IID} | head -n 1)"
      test -n "$OUTDIR" || {
        echo "Snapshot outdir not found"
        ls -lah jipipe-distribution-files || true
        exit 1
      }
    - |
      ONE="$(
        ls "$OUTDIR"/JIPipe-*-snapshot${CI_PIPELINE_IID}.zip 2>/dev/null \
          | grep -v Prepackaged \
          | head -n 1 \
          || true
      )"
      if [ -z "$ONE" ]; then
        echo "Could not find the plugins zip JIPipe-*-snapshot${CI_PIPELINE_IID}.zip to infer version"
        ls -lah "$OUTDIR"
        exit 1
      fi
    - |
      BASE="$(basename "$ONE")"
      BASE="${BASE#JIPipe-}"
      VERSION_BASE="${BASE%-snapshot${CI_PIPELINE_IID}.zip}"
      VERSION="${VERSION_BASE}-snapshot${CI_PIPELINE_IID}"
      SNAPSHOT_SUFFIX="${VERSION#${VERSION_BASE}-}"
      echo "Publishing GitHub prerelease for $VERSION"
    - |
      test -n "${GH_TOKEN:-}" || {
        echo "GH_TOKEN is not set. Create a masked CI/CD variable containing a GitHub fine-grained PAT with 'Contents: Read and write' on applied-systems-biology/jipipe."
        exit 1
      }
    - |
      FILES="$(
        find "$OUTDIR" -maxdepth 1 -type f -size +0c \( \
          -name "JIPipe-*-${SNAPSHOT_SUFFIX}-Installer-Win64.exe" -o \
          -name "JIPipe-*-${SNAPSHOT_SUFFIX}-Prepackaged-Win64.zip" -o \
          -name "JIPipe-*-${SNAPSHOT_SUFFIX}-Prepackaged-macOS.zip" -o \
          -name "JIPipe-*-${SNAPSHOT_SUFFIX}-Prepackaged-Linux64.tar.gz" -o \
          -name "JIPipe-${VERSION}.zip" \
        \) | sort
      )"
      if [ -z "$FILES" ]; then
        echo "No matching package files found"
        echo "Expected files for version: $VERSION"
        ls -lah "$OUTDIR"
        exit 1
      fi
      echo "Files selected for GitHub upload:"
      echo "$FILES" | sed 's/^/  - /'
    - |
      # Delete-and-recreate for idempotent re-runs
      if gh release view "$VERSION" --repo applied-systems-biology/jipipe >/dev/null 2>&1; then
        echo "Existing release $VERSION found; deleting before recreate"
        gh release delete "$VERSION" --repo applied-systems-biology/jipipe --yes
      fi
    - |
      # Build the GitLab package-registry mirror URLs directly (deterministic
      # generic-package format) rather than reading the urls-*.txt artifact,
      # because this job runs in parallel with upload_to_package_registry and
      # that file may not exist yet.
      GENERIC_BASE="$CI_API_V4_URL/projects/$CI_PROJECT_ID/packages/generic/jipipe/$VERSION"
      NOTES="JIPipe $VERSION snapshot build.

GitLab package registry mirror (may be throttled to ~2 MB/s):
"
      while IFS= read -r f; do
        [ -n "$f" ] || continue
        name="$(basename "$f")"
        NOTES="$NOTES
- $GENERIC_BASE/$name"
      done < <(echo "$FILES")
      NOTES="$NOTES
"
    - |
      gh release create "$VERSION" \
        --repo applied-systems-biology/jipipe \
        --prerelease \
        --title "JIPipe $VERSION" \
        --notes "$NOTES" \
        $FILES
    - |
      echo "Verifying uploaded GitHub release assets ..."
      expected="$FILES"
      count=0
      while IFS= read -r line; do
        [ -n "$line" ] || continue
        count=$((count+1))
      done < <(echo "$expected")
      found="$(
        gh release view "$VERSION" --repo applied-systems-biology/jipipe \
          --json assets --jq '.assets[].name' | sort
      )"
      ok=1
      while IFS= read -r f; do
        [ -n "$f" ] || continue
        name="$(basename "$f")"
        if ! echo "$found" | grep -qx "$name"; then
          echo "Missing GitHub release asset: $name"
          ok=0
        fi
      done < <(echo "$expected")
      if [ "$ok" != "1" ]; then
        echo "Asset verification failed."
        echo "Assets on GitHub:"
        echo "$found"
        exit 1
      fi
      echo "All $count expected assets verified on the GitHub prerelease."
    - echo "Done. GitHub prerelease created: https://github.com/applied-systems-biology/jipipe/releases/tag/$VERSION"
  rules:
    - if: $CI_COMMIT_BRANCH == "master"
```

- [ ] **Step 3: Validate the YAML parses**

Run: `python3 -c "import yaml,sys; yaml.safe_load(open('.gitlab-ci.yml')); print('YAML OK')"`
- Requires PyYAML (`pip install pyyaml` if missing).
- Expected: `YAML OK`. If PyYAML is unavailable, fall back to a manual scan for balanced indentation/yaml block markers (`- |` blocks must all be present and colon-aligned). If it fails, fix the YAML and re-run.

- [ ] **Step 4: Double-check job placement & rules are unchanged elsewhere**

Verify the diff only ADDS the `upload_to_github_prerelease` job and does not modify any other job block. Run:
`git diff .gitlab-ci.yml | grep -E '^[-+]' | grep -viE 'hardens of|^-   -|^- ' ` — confirm the only additions are under the new job and that no existing job lines are removed.

- [ ] **Step 5: Commit**

```bash
git add .gitlab-ci.yml
git commit -m "feat(ci): publish snapshot builds as GitHub prereleases"
```