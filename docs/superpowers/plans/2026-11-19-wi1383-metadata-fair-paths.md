# #1383 Sub-project F - Project Metadata, FAIR Check, User Paths Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose the full project metadata read/write, the FAIR check, and project user-path CRUD to MCP agents, so the agent loop check_fair -> set_project_metadata -> re-check works and user paths are manageable without touching the GUI.

**Architecture:** All work is in the existing operation layer: `JIPipeAutomationAPI`/`JIPipeProjectMetadata`/`JIPipeProjectUserPaths` provide the domain access, the operation classes parse JSON and delegate, `AutomationPlugin` registers the new tools. No broker, permission-model, or UI changes.

**Tech Stack:** Java 21, jackson databind, JUnit 5 in jipipe-core, Maven.

**Spec:** `docs/superpowers/specs/2026-11-19-wi1383-metadata-fair-paths-design.md`

## Global Constraints

- Commit format `type(#1383): subject`; explicit `git add` of exactly the touched files; no push.
- No em dashes or en dashes anywhere (code, Javadoc, comments, strings).
- kebab-case operation param names; `additionalProperties: false` on every schema.
- Read-only ops: `getCategory() = Query`, `isReadOnly() = true`, no `requiresEdt()` override. Mutating ops: `Modify` category, `isReadOnly() = false` (Ask by category default).
- Test command after each task: `mvn -q test -pl jipipe-core -Dtest=ProjectMetadataOperationsTest,UserPathOperationsTest,InfoOperationsTest,ModificationOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` (skip classes that do not exist yet). Builds are sequential - one slot.
- Empty-string convention: every response string field is emitted as `""` when unset, never an omitted key.
- House test policy: no tests that restate the implementation; no Swing interaction tests.
- Controller ruling (branch strategy): sub-project F lands on the SAME branch `1383-mcp-enhancements` (MR !204 grows; children track the slices). Do not create a new branch.

## Review Focus

Uncovered-by-spec failure modes and the task that pins each with a test:

1. **`setUserPath(key, value)` only updates the path** - it does not touch name/description/role and does not create an entry with the full fields in one call. The implementation must NOT delegate to it for updates; it must update the matched entry's fields in place. Pinned in Task 3 (update-then-list shows name/description/role preserved and changed as requested, no duplicate key).
2. **A provided authors list with a valid first entry and a second entry missing `lastName`** - the operation refuses the whole call with a named error (index + field), and the existing author list is untouched (no partial apply). Pinned in Task 1.
3. **The FAIR prefix stability** ("License -", "Summary -", "Authors -") is load-bearing for agents that match items - pin the verbatim prefixes, not just the count. Pinned in Task 2.
4. **`remove_user_path` on the last entry** - the list ends up empty and `list_user_paths` shows an empty array (no dangling entry). Pinned in Task 3.
5. **Blank-string license/summary are stored as-is** (clearing intent) and `check_fair` then reports them missing - no accidental trim-to-default. Pinned in Task 2 (the spec's explicit "stored as-is" sentence).

---

### Task 1: full metadata read/write

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (extend the setProjectMetadata API; add the get-side enrichment if it is cleaner API-side, else operation-side - keep one place per concern)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java` (`GetProjectInfo` metadata enrichment)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ModificationOperations.java` (`SetProjectMetadata` extension)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ProjectMetadataOperationsTest.java` (new file)

**Interfaces:**
- Consumes: `JIPipeProjectMetadata` extends `JIPipeStandardMetadata` (api/JIPipeStandardMetadata.java): `getLicense/setLicense`, `getSummary/setSummary(HTMLText)`, `getWebsite/setWebsite`, `getCitation/setCitation`, `getAuthors/setAuthors(JIPipeAuthorMetadata.List)`, `getAcknowledgements/setAcknowledgements`. `JIPipeAuthorMetadata.List extends JIPipeListParameter<JIPipeAuthorMetadata>` (`.add(...)`, `.size()`, indexable); `JIPipeAuthorMetadata` setters: `setFirstName/setLastName/setOrcid/setEmail/setWebsite/setFirstAuthor/setCorrespondingAuthor/setAffiliations(JIPipeOrganizationMetadata.List)`; `JIPipeOrganizationMetadata` setters `setName/setRor/setWebsite`.
- Produces:
  - `get_project_info` `metadata` object gains: `license`, `summary` (toPlainText), `website`, `citation` (strings, `""` when unset) and `authors`/`acknowledgements` arrays of `{firstName, lastName, orcid, email, website, firstAuthor, correspondingAuthor, affiliations: [{name, ror, website}]}` - all string fields `""` when unset at every level, booleans always present.
  - `set_project_metadata` gains optional params `license`, `summary`, `website`, `citation`, `authors`, `acknowledgements`. Semantics: omitted keeps current; provided array REPLACES the whole list; blank strings stored as-is. Author entry: `firstName` and `lastName` required (non-blank after trim), all else optional; a bad entry refuses the whole call with an error naming the index and the missing field, and leaves the existing list untouched (validate the full array before applying anything).
  - The operation description gains: `The FAIR check needs license, summary, and at least one author - see check_fair.`

- [ ] **Step 1: Write the failing tests**

New `ProjectMetadataOperationsTest` (pattern: `InstrumentationAPITest`'s `createContext` + `JIPipe.ensureInstance()` in `@BeforeAll`):

- `getProjectInfo_metadataEmitsAllFieldsWithEmptyDefaults`: fresh project -> `metadata` has `license`/`summary`/`website`/`citation` as `""`, `authors`/`acknowledgements` as empty arrays.
- `getProjectInfo_authorShape`: set one author (firstName "R", lastName "G", orcid "0000-0001", email, one affiliation name+ror) -> the entry emits the exact pinned shape with every key present, `firstAuthor`/`correspondingAuthor` false, missing affiliation fields `""`.
- `setProjectMetadata_scalarsRoundTrip`: set license/summary/website/citation -> read back via get_project_info.
- `setProjectMetadata_clearWithBlankString`: set a license, then set `license: ""` -> get_project_info shows `""` (stored as-is, no trim-to-default).
- `setProjectMetadata_authorsReplaceNotMerge`: authors [A] then authors [B, C] -> exactly [B, C]; acknowledgements replaced independently.
- `setProjectMetadata_omittedParamsKeepCurrent`: set license only -> authors/summary/etc. unchanged.
- `setProjectMetadata_invalidAuthorEntryRefusesWholeCall` (pin Review Focus 2): authors = [valid, {firstName only}] -> error message is `Invalid authors entry at index 1: lastName is required` (field names in the message are the camelCase response field names, as the schema and the get-side shape use them); the pre-existing author list is byte-identical after the failed call.
- `setProjectMetadata_emptyArrayReplacesWithEmpty`: authors [A] then `authors: []` -> the list is empty (presence of the key - even an empty array - replaces; only the ABSENCE of the key keeps the current list).
- `setProjectMetadata_nameAndDescriptionUnchanged`: name/description still work exactly as before (regression pin).

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=ProjectMetadataOperationsTest`
Expected: FAIL (missing fields / params refused by `additionalProperties: false`).

- [ ] **Step 3: Implement**

- `InfoOperations.GetProjectInfo`: emit the new metadata fields per the interface contract.
- `ModificationOperations.SetProjectMetadata`: parse the new params (validate the whole authors/acknowledgements arrays first - iterate, require non-blank `firstName` AND `lastName`, error `Invalid authors entry at index <i>: <field> is required` with the camelCase field name - `firstName`/`lastName` - before applying anything); build `JIPipeAuthorMetadata`/`JIPipeOrganizationMetadata` via the typed setters; replace the lists when provided (an empty array is still a replacement); set scalars via `HTMLText` for summary (the existing description path already uses `new HTMLText(...)` - mirror it).
- Keep the schema `additionalProperties: false`; document the replace semantics and the FAIR cross-reference in the param descriptions.

- [ ] **Step 4: Run tests green**

Run: `mvn -q test -pl jipipe-core -Dtest=ProjectMetadataOperationsTest,InfoOperationsTest,ModificationOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ModificationOperations.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ProjectMetadataOperationsTest.java
git commit -m "feat(#1383): expose full project metadata read/write to the automation layer"
```
(If JIPipeAutomationAPI is untouched, drop it from the `git add` list - only add files actually changed.)

### Task 2: `check_fair` + RO-Crate documentation glue

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java` (new `CheckFair` operation)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ROCrateOperations.java` (description sentence only)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register `check_fair` in the info block)
- Test: `ProjectMetadataOperationsTest.java`

**Interfaces:**
- Consumes: `JIPipeProjectFairCheck.missing(JIPipeProject)` / `isFair(JIPipeProject)` (`api/project/jipx/`, documented thread-safe, no Swing).
- Produces: `check_fair` (Query, read-only, no EDT requirement): response `{ "fair": bool, "missing": [<verbatim checker strings>] }`. Description cross-references `set_project_metadata` (which fields fix which item) and states that the missing strings are stable, machine-readable prefixes ("License -", "Summary -", "Authors -").
- `create_ro_crate` description gains exactly one sentence: metadata completeness is the pre-save check (`check_fair`); the crate's own compliance list remains the post-save check.

- [ ] **Step 1: Failing tests (add to `ProjectMetadataOperationsTest`)**

- `checkFair_freshProjectMissingAllThree`: `fair` false, `missing` size 3; pin the three verbatim prefixes "License -", "Summary -", "Authors -" (Review Focus 3) plus the exact known strings for the license item.
- `checkFair_completeProjectIsFair`: set license + summary + one author via the extended set_project_metadata -> `fair` true, `missing` empty.
- `checkFair_eachFieldFlipsItsItem`: from the complete state, clear license (blank string) -> exactly one missing item starting "License -"; restore, clear summary -> "Summary -" only; remove the author (authors: []) -> "Authors -" only.
- `checkFair_blankStringsCountAsMissing` (pin Review Focus 5): blank license/summary stored as-is are reported missing (the checker trims; the operation must not pre-populate defaults).

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=ProjectMetadataOperationsTest`
Expected: FAIL (no `check_fair` operation).

- [ ] **Step 3: Implement**

- `CheckFair` in InfoOperations delegating to `JIPipeProjectFairCheck`; the RO-Crate description sentence; registration in AutomationPlugin's info block.

- [ ] **Step 4: Run tests green**

Run: `mvn -q test -pl jipipe-core -Dtest=ProjectMetadataOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ROCrateOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ProjectMetadataOperationsTest.java
git commit -m "feat(#1383): add check_fair operation and the ro-crate documentation tie-in"
```

### Task 3: project user paths CRUD

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/UserPathOperations.java` (List/Set/Remove, the operation class pattern of the sibling files)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register the three ops)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/UserPathOperationsTest.java` (new file)

**Interfaces:**
- Consumes: `project.getMetadata().getUserPaths().getPaths()` -> `JIPipeParameterCollectionList` (extends `JIPipeListParameter<JIPipeDynamicParameterCollection>`; `addNewInstance()` applies the `UserPathEntry` template; iterable). Entries are `JIPipeDynamicParameterCollection` accessed via `collection.get("key").get(String.class)` / `collection.setParameter("path", value)` - the same access `JIPipeProjectUserPaths.setUserPath` uses. IMPORTANT (Review Focus 1): `setUserPath(key, value)` updates ONLY the path - do not delegate to it for updates; set each field on the matched entry directly.
- Produces:
  - `list_user_paths` (Query, read-only, no EDT requirement): `{ "userPaths": [ { key, name, description, path, role } ] }` - `path` as stored (absolute string), strings `""` when unset, `role` always one of the four enum names.
  - `set_user_path` (Modify, Ask): params `key` (required, non-blank), `path` (required, non-blank; `Path.of(...).toAbsolutePath().normalize()`), `name` (optional), `description` (optional), `role` (optional enum `Input|Output|Unspecified|Ignored`, default `Unspecified`; an invalid role is refused by the schema enum). Upsert: existing key updates its entry in place (name/description/role set only when provided - preserving unset fields the entry already has, path always overwritten); new key appends `paths.addNewInstance()` with all fields. Response `{ "key": ..., "created": bool }`. Description carries the Output-role warning: a role of `Output` makes the path a pipeline write target.
  - `remove_user_path` (Modify, Ask): param `key` (required). No matching key -> error `Unknown user path key 'X'. Known keys: a, b` (house refusal style; an empty list names none). Response `{ "removed": true }`.
- The `JIPipeParameterCollectionList` items carry a `"role"` field of type `JIPipeProjectUserPaths.Role` (verify the exact dynamic key names `key`, `name`, `description`, `path`, `role` against the entry class before coding - they are the `@JIPipeParameter` values).

- [ ] **Step 1: Failing tests (new `UserPathOperationsTest`)**

Build a fresh project; drive everything through the operations (execute with parsed JSON params).

- `listUserPaths_emptyOnFreshProject`: `userPaths` is an empty array.
- `setUserPath_createAndList`: create with key `data`, path, name, description, role `Input` -> list shows the exact entry (empty-string convention for unset fields).
- `setUserPath_updateInPlacePreservesOtherFields` (pin Review Focus 1): create with name+description+role, then update the same key with a new path and new role only -> exactly one entry (no duplicate key), new path/role applied, name/description preserved; then update with name only -> description still preserved.
- `setUserPath_roleDefaultsUnspecified`: create without role -> role `Unspecified` in the list.
- `setUserPath_blankKeyRefused`: `key: ""` -> error naming the offending field.
- `removeUserPath_removesMatched`: two entries, remove one -> the other remains, removed key gone (pin Review Focus 4: remove the second too -> empty array).
- `removeUserPath_unknownKeyErrorsListingKnown`: remove `nope` -> error contains `nope` and both known keys.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -pl jipipe-core -Dtest=UserPathOperationsTest`
Expected: FAIL (no operations/API).

- [ ] **Step 3: Implement**

- `UserPathOperations.java` with the three operations per the contract; the upsert walks `getPaths()` once (match by `key`, else append), setting fields on the `JIPipeDynamicParameterCollection` directly (never `setUserPath`).
- Register in `AutomationPlugin` (a new user-paths block or the modification block - follow the file's grouping style).

- [ ] **Step 4: Run tests green (including the registry gate)**

Run: `mvn -q test -pl jipipe-core -Dtest=UserPathOperationsTest,ProjectMetadataOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/UserPathOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/UserPathOperationsTest.java
git commit -m "feat(#1383): add project user path operations to the automation layer"
```
