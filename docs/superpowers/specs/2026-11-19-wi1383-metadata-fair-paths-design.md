# Spec: #1383 sub-project F - project metadata, FAIR check, user paths

Date: 2026-11-19
Status: DRAFT for user review
Work item: #1386 (child of #1383, MCP enhancements). Depends on: sub-project A (MR !204; independent file surface, mergeable before or after this sub-project).
Source: user additions to #1383 (2026-11-19): "edit project metadata like license, description, short description, verify project is FAIR (and letting agent handle it). Also adding/configuring project paths, do ro-crate export".

## 1. Intent

An agent preparing a project for sharing must be able to read and write the full project metadata, verify FAIR-completeness, and configure the project's user paths. Today only name and description are reachable (`get_project_info` reads name/description/globalParameters; `set_project_metadata` writes name/description), the FAIR checker exists but is not exposed, and user paths have no operations at all. RO-Crate export already exists as `create_ro_crate` and only gains a documentation-level tie-in.

Verified ground: the FAIR check requires exactly license + summary + at least one author (`JIPipeProjectFairCheck.missing`, `api/project/jipx/JIPipeProjectFairCheck.java:56-76`, documented thread-safe, no Swing). The metadata fields live on `JIPipeStandardMetadata` (`api/JIPipeStandardMetadata.java`: name, description, summary, authors, acknowledgements, website, license, citation, dependencyCitations). Authors are structured: `JIPipeAuthorMetadata` carries title, firstName, lastName, affiliations (`JIPipeOrganizationMetadata.List`, entries `{name, ror, website}`), website, contact, email, firstAuthor, correspondingAuthor, orcid, customText; the legacy string-list affiliations remain readable but the organization list is canonical. User paths live on `JIPipeProjectUserPaths` (`api/project/JIPipeProjectUserPaths.java`): entries `{key, name, description, path, role Input|Output|Unspecified|Ignored}`, with `setUserPath(key, value)` already present.

## 2. Confirmed decisions

1. **Design approved in chat** (user, 2026-11-19): the four sections below as presented.
2. **Sub-project order** (charter, user-approved 2026-11-19): F second of six, between A and E.
3. **List replacement semantics** (presented and approved): providing `authors`/`acknowledgements` replaces the whole list; omitted params keep current values (house convention).

## 3. F1: full metadata read/write

### 3.1 `get_project_info` (additive, back-compatible)

The existing `metadata` object (`InfoOperations.GetProjectInfo`) gains:

- `license` (string), `summary` (plain text via `toPlainText()`), `website`, `citation` (strings, empty when unset).
- `authors` and `acknowledgements`: arrays of `{firstName, lastName, orcid, email, website, firstAuthor, correspondingAuthor, affiliations: [{name, ror, website}]}`. The empty-string convention applies at every level (unset string fields emit `""`, never omitted keys; unset `ror`/`website` in affiliations likewise), so the shape is stable. `title`, `contact`, `customText`, and the legacy affiliation list stay unexposed (YAGNI; the FAIR loop needs none of them).
- Existing fields (`description`, `globalParameters`) and everything outside `metadata` are unchanged.

### 3.2 `set_project_metadata` (extends in place)

New optional params: `license`, `summary`, `website`, `citation` (strings), `authors` and `acknowledgements` (arrays of the author object above; `firstName`/`lastName` required per entry, everything else optional). Semantics: omitted param keeps the current value; a provided array REPLACES the whole list (documented in the schema description - there is no merge). Author entries map to `JIPipeAuthorMetadata` with affiliations mapped to `JIPipeOrganizationMetadata` (`name` required, `ror`/`website` optional). `name`/`description` behavior is byte-identical to today. A blank `summary`/`license` string is stored as-is (clearing a field is a legitimate agent intent; the FAIR check will report it as missing).

The operation description gains: "The FAIR check needs license, summary, and at least one author - see check_fair."

## 4. F2: `check_fair`

New read-only Query operation in `InfoOperations`, registered as `check_fair`. No EDT requirement. Response: `{ "fair": bool, "missing": ["License - no license is set on the project metadata.", ...] }` - the `missing` strings are `JIPipeProjectFairCheck.missing` verbatim (human-readable, prefix-stable: "License -", "Summary -", "Authors -"), so an agent can fix exactly what is named and re-check. The operation description cross-references `set_project_metadata`.

## 5. F3: project user paths

New operations-group file `UserPathOperations.java` (`api/automation/operations/`), three operations registered in `AutomationPlugin`:

- **`list_user_paths`** (Query, read-only, no EDT requirement): `{ "userPaths": [ { key, name, description, path, role } ] }` - one entry per `UserPathEntry`, strings empty when unset, `path` absolute as stored.
- **`set_user_path`** (Modify, Ask default): upsert by `key`. Params: `key` (required), `path` (required, stored absolute-normalized), `name` (optional), `description` (optional), `role` (optional enum `Input|Output|Unspecified|Ignored`, default `Unspecified`). An existing key updates its entry in place; a new key appends. The description explicitly warns: a role of `Output` makes the path a pipeline write target. Response: `{ "key": ..., "created": bool }`.
- **`remove_user_path`** (Modify, Ask default): param `key` (required). Unknown key is an error listing the known keys (house error style, mirroring the template-id refusal). Response: `{ "removed": true }`.

**Path-argument ruling (mirrors the open_project precedent, user ruling 2026-11-17):** user-path targets are not restricted to the allowed export roots - the Ask prompt is the consent gate for every add/update/remove. The write-target rooting of export_data/save-to style parameters is unaffected (those keep their roots); user paths are project data the user consents to through the operation prompt.

## 6. F4: RO-Crate glue (documentation only)

`create_ro_crate`'s description gains one sentence: metadata completeness is the pre-save check (`check_fair`); the crate's own compliance list remains the post-save check (the distinction `JIPipeProjectFairCheck`'s Javadoc already draws). No schema or behavior change.

## 7. Permissions

`check_fair` and `list_user_paths`: Query, read-only, Allow by category default. `set_project_metadata` (existing), `set_user_path`, `remove_user_path`: Modify, non-read-only, Ask by category default - every mutation prompts. No broker changes (all project-bound; nothing joins `GLOBAL_TOOL_IDS`).

## 8. Testing policy

Headless-safe focused tests in jipipe-core over programmatically built projects (same pattern as sub-project A's `QueryOperationsTest`):

- `get_project_info` metadata enrichment: all new fields present with `""` defaults on a fresh project; set values round-trip; author arrays emit the pinned shape (including empty affiliations arrays).
- `set_project_metadata`: scalar set/clear round-trips; author list replacement (two entries in, one different entry out - replace, not merge); omitted params keep current values; name/description unchanged behavior; entry validation (missing firstName/lastName refused with a named error).
- `check_fair`: fresh project -> `fair:false` with all three items; after setting license+summary+one author -> `fair:true`, `missing` empty; each single field restored individually flips its item (prefix stability pinned: "License -", "Summary -", "Authors -").
- User paths: empty list on fresh project; upsert create-then-update (same key, no duplicate); role default; remove known/unknown (error naming known keys); list shape stability.
- No Swing interaction tests (standing policy). No FAIR-check behavior tests against the GUI badge - the checker itself is core logic and already tested there; these tests pin the operation layer.

## 9. Expected files

- `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java` - get_project_info enrichment, new CheckFair operation.
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ModificationOperations.java` - SetProjectMetadata extension.
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/UserPathOperations.java` - new file (List/Set/Remove).
- `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` - register `check_fair`, `list_user_paths`, `set_user_path`, `remove_user_path`.
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ROCrateOperations.java` - description sentence only.
- Test class(es) under `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/` (new `UserPathOperationsTest` + a `ProjectMetadataOperationsTest` or extensions of the existing info/modification test files, implementer's choice following house naming).

## 10. Out of scope

- Project report / parameter diff / set_parameter read-back / slot editing (sub-project E).
- Anything in the RO-Crate build itself or its compliance list.
- `dependencyCitations`, plugin `categories`, `updateSiteDependencies`, `globalParameters` writes (not requested; `globalParameters` is already readable).
- MCP broker or permission-model changes.
