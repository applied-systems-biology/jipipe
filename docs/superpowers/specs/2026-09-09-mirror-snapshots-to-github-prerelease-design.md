# Mirror Snapshot Builds to GitHub Prereleases — Design

Date: 2026-09-09

## Problem

JIPipe snapshot packages are published only to the GitLab generic package
registry (`asb-git.hki-jena.de`). IT restrictions throttle download speed
from that GitLab instance to ~2 MB/s, which is too slow for large package
archives (Windows installers, prepackaged zips/tar.gz, plugins zip). We want
to also publish each master-branch snapshot as a **GitHub prerelease** on
`applied-systems-biology/jipipe` so users can download fast.

## Goal

For every `master` pipeline that builds a snapshot distribution, create one
GitHub **prerelease** on `applied-systems-biology/jipipe`, tagged with the snapshot version,
uploading **all** package assets, so release downloads bypass the GitLab
throttle.

## Non-goals

- Do not alter the existing GitLab generic package registry publish.
- Do not change Docker Hub publish, RO-Crate verification, or feature-branch
  jobs.
- No user-facing or test-code changes; the deliverable is an addition to
  `.gitlab-ci.yml`.

## Current system (context)

The existing master pipeline flow:

```
master commit
  → distribution_package          (builds *-snapshot$CI_PIPELINE_IID/ artifacts)
  → upload_to_package_registry    (GitLab generic package registry, uploads 5 files)
  → docker_build_push             (Docker Hub)
  → verify_ro_crate
```

`upload_to_package_registry` infers a snapshot version from a built ZIP, e.g.
`6.0.0-snapshot<CI_PIPELINE_IID>`, and uploads:

1. `JIPipe-*-<snapshotSuffix>-Installer-Win64.exe`
2. `JIPipe-*-<snapshotSuffix>-Prepackaged-Win64.zip`
3. `JIPipe-*-<snapshotSuffix>-Prepackaged-macOS.zip`
4. `JIPipe-*-<snapshotSuffix>-Prepackaged-Linux64.tar.gz`
5. `JIPipe-<VERSION>.zip` (plugins zip)

## Approach (chosen)

**Option A — a new dedicated `upload_to_github_prerelease` job** in the
`publish` stage, using the official GitHub CLI `gh`.

Rationale:

- Clean separation: GitHub tooling/auth is isolated so a GitHub failure cannot
  affect GitLab package publication.
- `gh` handles the error-prone GitHub HTTP work (auth, multipart asset upload,
  `upload_url`, retries) instead of hand-rolling curl.
- The existing GitLab package job stays untouched.

## Design

### 1. Architecture & release identity

Add job `upload_to_github_prerelease` to the `publish` stage.

- `needs: distribution_package` (artifacts) — it does **not** wait on
  `upload_to_package_registry`; the two publish jobs run in parallel.
- `rules: if: $CI_COMMIT_BRANCH == "master"` (same as siblings).
- Runs only when the build artifacts exist; fails with a clear message
  otherwise.

Release identity:

- **Tag:** the snapshot version, e.g. `6.0.0-snapshot<CI_PIPELINE_IID>`,
  derived with the exact same inference logic as `upload_to_package_registry`.
- **Prerelease flag:** `--prerelease`, so it shows under GitHub Pre-releases
  and never as a stable release.
- **Title:** `JIPipe <VERSION>` (human-readable).
- **Body:** auto-generated summary plus the GitLab package-registry download
  URLs (from `urls-*.txt`) as a fallback for users with restricted GitLab
  access.
- **Repo:** `applied-systems-biology/jipipe`.

### 2. Job implementation

Base image: `alpine:3.19` with `apk add github-cli bash curl jq` (mirroring
the existing `apk add curl jq bash` pattern elsewhere in the file).

Script steps:

1. Locate the snapshot output dir:
   ```
   OUTDIR="$(ls -d jipipe-distribution-files/*-snapshot${CI_PIPELINE_IID} | head -n 1)"
   ```
2. Infer `VERSION`, `VERSION_BASE`, `SNAPSHOT_SUFFIX` identically to
   `upload_to_package_registry`.
3. Gather the 5 asset files using the same `find ... \( -name ... \)`
   predicate; fail if empty.
4. Auth: `gh` reads the `GH_TOKEN` env var automatically.
5. Delete-and-recreate (see §4):
   - `gh release view <VERSION>`; if present, `gh release delete --yes`.
6. Create + upload:
   ```
   gh release create "<VERSION>" \
     --repo applied-systems-biology/jipipe \
     --prerelease \
     --title "JIPipe <VERSION>" \
     --notes "<body>" \
     <asset-1> ... <asset-5>
   ```
7. Verify (see §5).

### 3. Data flow & CI variables

```
master commit → distribution_package
               (artifacts: *-snapshot$IID/{5 files}, urls-*.txt)
            → upload_to_package_registry (GitLab, unchanged)
            → upload_to_github_prerelease (NEW, reads GH_TOKEN)
            → docker_build_push (unchanged)
```

New CI/CD variable (GitLab → Settings → CI/CD → Variables, masked):

| Name | Masked | Value |
|------|--------|-------|
| `GH_TOKEN` | yes | GitHub **fine-grained** PAT with `Contents: Read and write` on `applied-systems-biology/jipipe` only |

Token guidance:

- Prefer a **fine-grained** PAT scoped to only the `applied-systems-biology/jipipe`
  repository with only `Contents: Read and write` — enough for
  `GET /repos/.../releases`, `POST /repos/.../releases`, and asset upload.
- Do not use a broad classic PAT.
- Prefer a bot/service-account token so it survives engineer offboarding.
- Set an expiration date; keep the value masked.

Existing variable `PACKAGE_UPLOAD_TOKEN` (GitLab generic package upload) is
unchanged and not shared with GitHub.

### 4. Error handling & idempotency

- **Release/tag already exists** (pipeline re-run or re-trigger):
  delete-and-recreate. Before creating, `gh release view <VERSION>`; if it
  exists, `gh release delete --yes`, then recreate. This makes re-runs
  idempotent and recovers from a partial asset upload.
- **Missing asset file:** the `find` predicate fails if any expected file is
  empty/missing (same guards as `upload_to_package_registry`).
- **Auth failure / unset `GH_TOKEN`:** fail with a clear message naming the
  variable (mirrors the `PACKAGE_UPLOAD_TOKEN` guard). GitLab package upload
  still succeeds independently.
- **Transient network failure:** rely on `gh`'s built-in asset-upload retries
  plus the job's `retry: 1`.

### 5. Testing & verification

Verification is intrinsic to the CI job (no JUnit applies to CI YAML):

- After create, run:
  ```
  gh release view <VERSION> --json assets --jq '.assets[].name'
  ```
  and assert all 5 expected filenames are present; fail if any are missing.
  (Delete-and-recreate means a re-run recovers.)
- Confirm the release is queryable after creation.
- **Manual smoke test** on first master merge: inspect
  `https://github.com/applied-systems-biology/jipipe/releases` — confirm the "Pre-release"
  badge, all 5 assets present and downloadable, and that download speed from
  GitHub exceeds the ~2 MB/s GitLab limit.

## Out of scope

- Altering the GitLab package registry upload.
- GitHub release for tagged/stable versions (not requested).
- Any Java/JUnit test changes.

## Files changed

- `.gitlab-ci.yml` — add `upload_to_github_prerelease` job.