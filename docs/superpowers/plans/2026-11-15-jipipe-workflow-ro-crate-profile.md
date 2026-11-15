# JIPipe Workflow RO-Crate profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish the "JIPipe Workflow RO-Crate profile 1.0" spec (spec page + Profile Crate + redirects) and make generated JIPX metadata declare it, per work item #1360.

**Architecture:** Three independent deliverables sharing fixed URI string literals: (A) jipipe-core stamps the profile URI into the JIPX crate metadata and a new launcher test pins the full conformance table; (B) the CMS website serves the spec page, the language page, and the Profile Crate JSON; (C) the GitHub gh-pages branch adds meta-refresh redirect pages so the jipipe.org permalinks resolve. No build coupling between the repos.

**Tech Stack:** Java 21 + Maven (jipipe-core, jipipe-launcher tests, vendored `contrib/jipipe-ro-crate-java-2.1.0` library), Spring Boot + Thymeleaf (jipipe-cms-website), static HTML (GitHub Pages).

**Spec:** `docs/superpowers/specs/2026-11-15-jipipe-workflow-ro-crate-profile-design.md`

## Global Constraints

- Canonical URIs, verbatim: profile permalink `https://jipipe.org/ro-crate/jipipe-workflow/1.0`; language entity `https://jipipe.org/jipipe`; language page `https://jipipe.hki-jena.de/jipipe`; Profile Crate `https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0/ro-crate-metadata.json`; base profiles `https://w3id.org/ro/crate/1.1` and `https://w3id.org/workflowhub/workflow-ro-crate/1.0`; Bioschemas `https://bioschemas.org/profiles/ComputationalWorkflow/1.0-RELEASE`; HKI ROR `https://ror.org/055s37c97`; author ORCID `https://orcid.org/0000-0002-0723-6038`.
- All URIs are stable string literals; never code-derived.
- No em dashes or en dashes anywhere (code, Javadoc, comments, HTML, JSON strings).
- `contrib/jipipe-ro-crate-java-2.1.0` is vendored: read it, do not restructure it.
- jipipe launcher tests call `JIPipe.ensureInstance()` in `@BeforeAll` (the module's `LauncherSessionListener` handles the ImageJ patcher; do not add `--add-opens` or preinit calls).
- Conformance assertions on `conformsTo` arrays must be order-independent (the library stores extra values in a `HashSet`).
- Commit style: `type(scope): message, #1360` (e.g. `feat(jipx): declare JIPipe Workflow RO-Crate profile, #1360`).
- Pushing any branch (including gh-pages) requires explicit user confirmation.
- Website repo: leave the untracked files (`.mvn/`, `.serena/`, `mvnw`, `mvnw.cmd`) untouched.

## Review Focus

- Save-time performance: `buildCrate` must NOT re-serialize the project; the part list comes from the already-materialized workspace manifest. Pinned by the Task 2 signature (manifest-derived `Set<String>`), no re-slice call.
- Re-saving an older JIPX container (written before this change): unknown entries preserved, new inventory lists only what the manifest lists. Exercised by the existing unknown-part-preservation writer tests plus Task 2's manifest-driven assertions.
- Descriptor `conformsTo` array order is nondeterministic (HashSet) - every assertion order-independent.
- A project with empty `additional-metadata`: its part file is absent from the container; the inventory test reads the manifest rather than assuming all 9 parts.
- Profile Crate and spec page must agree with the generated metadata on every literal (permalinks, version 1.0.0); the Task 3/4 tests pin the page contents, the manual `rocrate` validator run (Task 2 verification) pins machine validity.

---

### Task 1: Profile declaration in the crate builder (jipipe repo)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectCrate.java`
- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectCrateConformanceTest.java`

**Interfaces:**
- Consumes: existing `JIPipeProjectCrate.buildCrate(JIPipeProject, List<String>)`, `JsonDescriptor.Builder.addConformsTo(String)` (accumulates; `LATEST_STABLE` = RO-Crate 1.1 is always first), `RoCrate.getRootDataEntity()`, `RootDataEntity.addIdListProperties(String, Collection<String>)`, `RoCrate.getJsonDescriptor()` (returns `ContextualEntity`; `getProperties()` gives the JSON).
- Produces: `JIPipeProjectCrate.WORKFLOW_RO_CRATE_PROFILE_URI`, `JIPipeProjectCrate.JIPIPE_WORKFLOW_RO_CRATE_PROFILE_URI` (public static final Strings). Task 2 and the conformance test use them.

Work on branch `1360-jipipe-workflow-ro-crate-profile` (already exists; the spec commit `ab9913b819` is its first commit).

- [ ] **Step 1: Write the failing test**

Create `JIPipeProjectCrateConformanceTest.java` modeled on `JIPipeXProjectWriterTest` (same fixture `/projects/5.3.0-taxonomy-fixtures/basic-images.jipipe`, `@BeforeAll JIPipe.ensureInstance()`, `@TempDir Path tmp`). Duplicate the small private helpers needed: `loadFixture()`, `readZipEntryAsJson(Path, String)`, `readZipEntryAsJson` over the zip, `entityWithId(JsonNode, String)`, `conformsToContains(JsonNode, String)`, `typesContain(JsonNode, String)`, and a new `idArrayContains(JsonNode hasPart, String id)` (true when the array contains an object with `"@id"` equal to `id`). Add a private helper `Path writeContainer(String fileName)` that loads the fixture, writes `tmp/<fileName>` via `new JIPipeXProjectWriter().writeWithViolations(project, target, JIPipeProgressInfo.SILENT, violations)`, asserts `violations` is empty, and returns `target`; and `JsonNode writeAndReadMetadata(String fileName)` = `readZipEntryAsJson(writeContainer(fileName), "ro-crate-metadata.json")`.

Test methods (class javadoc: "Conformance of generated JIPX crate metadata against Workflow RO-Crate 1.0 and the JIPipe Workflow RO-Crate profile 1.0 (work item #1360)"):

```java
@Test
void descriptorDeclaresAllThreeProfiles() {
    JsonNode metadata = writeAndReadMetadata("conformance-descriptor.jipx");
    JsonNode descriptor = entityWithId(metadata.get("@graph"), "ro-crate-metadata.json");
    assertNotNull(descriptor);
    JsonNode conformsTo = descriptor.get("conformsTo");
    assertTrue(conformsToContains(conformsTo, "https://w3id.org/ro/crate/1.1"),
            "descriptor must declare RO-Crate 1.1: " + conformsTo);
    assertTrue(conformsToContains(conformsTo, JIPipeProjectCrate.WORKFLOW_RO_CRATE_PROFILE_URI),
            "descriptor must declare the Workflow RO-Crate profile: " + conformsTo);
    assertTrue(conformsToContains(conformsTo, JIPipeProjectCrate.JIPIPE_WORKFLOW_RO_CRATE_PROFILE_URI),
            "descriptor must declare the JIPipe Workflow RO-Crate profile: " + conformsTo);
}

@Test
void rootDeclaresWorkflowProfiles() {
    JsonNode metadata = writeAndReadMetadata("conformance-root.jipx");
    JsonNode root = entityWithId(metadata.get("@graph"), "./");
    assertNotNull(root);
    JsonNode conformsTo = root.get("conformsTo");
    assertTrue(conformsToContains(conformsTo, JIPipeProjectCrate.WORKFLOW_RO_CRATE_PROFILE_URI),
            "root must declare the Workflow RO-Crate profile: " + conformsTo);
    assertTrue(conformsToContains(conformsTo, JIPipeProjectCrate.JIPIPE_WORKFLOW_RO_CRATE_PROFILE_URI),
            "root must declare the JIPipe Workflow RO-Crate profile: " + conformsTo);
}

@Test
void languageEntityCarriesNameVersionAndUrl() {
    JsonNode metadata = writeAndReadMetadata("conformance-language.jipx");
    JsonNode graph = metadata.get("@graph");
    JsonNode language = entityWithId(graph, "https://jipipe.org/jipipe");
    assertNotNull(language, "the JIPipe ComputerLanguage entity must be present");
    assertEquals("JIPipe", language.get("name").asText());
    assertEquals(JIPipe.getJIPipeVersion(), language.get("version").asText());
    assertEquals("https://jipipe.hki-jena.de/jipipe", language.path("url").path("@id").asText(),
            "the language entity must carry the hosted language page as url");
}

@Test
void mainWorkflowHasFullWorkflowTypeSet() {
    JsonNode metadata = writeAndReadMetadata("conformance-main.jipx");
    JsonNode graph = metadata.get("@graph");
    JsonNode mainEntity = entityWithId(graph, "project/graph.json");
    assertNotNull(mainEntity);
    assertTrue(typesContain(mainEntity.get("@type"), "File"));
    assertTrue(typesContain(mainEntity.get("@type"), "SoftwareSourceCode"));
    assertTrue(typesContain(mainEntity.get("@type"), "ComputationalWorkflow"));
    assertEquals("project/graph.json",
            entityWithId(graph, "./").get("mainEntity").path("@id").asText());
    assertNotNull(entityWithId(graph, "./").get("license"), "the crate MUST specify a license");
    assertNotNull(mainEntity.get("programmingLanguage"), "the main workflow MUST have programmingLanguage");
}

@Test
void containerCarriesMandatoryRootEntries() throws IOException {
    Path jipx = writeContainer("conformance-entries.jipx");
    List<String> entries = zipEntryNames(jipx);
    assertEquals("ro-crate-metadata.json", entries.get(0), "metadata file must be the first zip entry");
    assertTrue(entries.contains("jipx.json"), "the manifest entry must exist");
    assertTrue(entries.contains("README.md"), "the README entry must exist");
    JsonNode metadata = readZipEntryAsJson(jipx, "ro-crate-metadata.json");
    assertNotNull(entityWithId(metadata.get("@graph"), "README.md"), "the README data entity must be present");

    // Manifest fields (profile 1.0 requirement 5)
    JsonNode manifest = readZipEntryAsJson(jipx, "jipx.json");
    assertEquals(1, manifest.get("format-version").asInt(), "manifest format-version must be 1");
    assertEquals(JIPipe.getJIPipeVersion(), manifest.get("jipipe-version").asText(),
            "the manifest must record the writing JIPipe version");
    assertEquals("project/graph.json", manifest.get("main-graph").asText(),
            "the manifest main-graph must be the main entity path");
    assertEquals("graph", manifest.get("parts").get("project/graph.json").asText(),
            "the parts table must map the main graph path to its part key");
}
```

Add `zipEntryNames(Path)` (copy from `JIPipeXProjectWriterTest`).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-launcher -Dtest=JIPipeProjectCrateConformanceTest`
Expected: COMPILATION ERROR (`JIPIPE_WORKFLOW_RO_CRATE_PROFILE_URI` and the new assertions do not exist).

- [ ] **Step 3: Implement in `JIPipeProjectCrate.java`**

1. Add the two constants (with javadoc: permalink of the published profile spec, stable string literal):

```java
public static final String WORKFLOW_RO_CRATE_PROFILE_URI = "https://w3id.org/workflowhub/workflow-ro-crate/1.0";
public static final String JIPIPE_WORKFLOW_RO_CRATE_PROFILE_URI = "https://jipipe.org/ro-crate/jipipe-workflow/1.0";
```

2. In `buildCrate`, replace the inline descriptor block:

```java
crate.setJsonDescriptor(new JsonDescriptor.Builder()
        .addConformsTo(WORKFLOW_RO_CRATE_PROFILE_URI)
        .addConformsTo(JIPIPE_WORKFLOW_RO_CRATE_PROFILE_URI)
        .build());
```

3. Immediately after the `mainEntity` line in `buildCrate`, add the root declaration:

```java
crate.getRootDataEntity().addIdListProperties("conformsTo",
        List.of(WORKFLOW_RO_CRATE_PROFILE_URI, JIPIPE_WORKFLOW_RO_CRATE_PROFILE_URI));
```

4. In `newBuilder`, extend the language entity builder chain with `.addIdProperty("url", "https://jipipe.hki-jena.de/jipipe")`.

5. In `checkCompliance`, add (using the same `crate.getJsonDescriptor().getProperties().path("conformsTo")` node): if it does not contain `JIPIPE_WORKFLOW_RO_CRATE_PROFILE_URI`, add the violation `"the JIPipe Workflow RO-Crate profile URI is missing from the metadata descriptor's conformsTo"`. Reuse a local static variant of the `conformsToContains` logic (the test helper cannot be reused from main sources; implement a private `descriptorConformsToContains(RoCrate, String)`).

6. Update the class javadoc: `LANGUAGE_ENTITY_ID` now points at the hosted language page (no longer a placeholder); the compliance contract mentions the profile declaration.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-launcher -Dtest=JIPipeProjectCrateConformanceTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Run the existing writer suite to verify no regression**

Run: `mvn test -pl jipipe-launcher -Dtest=JIPipeXProjectWriterTest`
Expected: PASS (its `conformsToContains` and language-id assertions are unaffected).

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectCrate.java jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectCrateConformanceTest.java
git commit -m "feat(jipx): declare the JIPipe Workflow RO-Crate profile in crate metadata, #1360"
```

---

### Task 2: Container inventory in root hasPart (jipipe repo)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectCrate.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeXProjectWriter.java` (one call site, line ~337)
- Modify: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectCrateConformanceTest.java`

**Interfaces:**
- Consumes: `JIPipeProjectWorkspace.getManifest()` (interface method, `throws IOException`), `JIPipeXManifest.getParts()` (`Map<String,String>`: zip entry path -> part key, exactly what gets zipped), `JIPipeXManifest.MANIFEST_FILE_NAME` = `"jipx.json"`, `JIPipeProjectCrate.MAIN_ENTITY_ID` = `"project/graph.json"`.
- Produces: `buildCrate(JIPipeProject project, Set<String> partEntryPaths, List<String> violationsOut)` - the single new signature; no other caller exists (verified: only `JIPipeXProjectWriter` line 337).

- [ ] **Step 1: Write the failing test**

Add to `JIPipeProjectCrateConformanceTest`:

```java
@Test
void hasPartInventoriesManifestAndAllParts() throws IOException {
    Path jipx = tmp.resolve("conformance-inventory.jipx");
    JIPipeProject project = loadFixture();
    List<String> violations = new ArrayList<>();
    new JIPipeXProjectWriter().writeWithViolations(project, jipx, JIPipeProgressInfo.SILENT, violations);
    assertTrue(violations.isEmpty(), violations::toString);

    JsonNode manifest = readZipEntryAsJson(jipx, "jipx.json");
    JsonNode metadata = readZipEntryAsJson(jipx, "ro-crate-metadata.json");
    JsonNode root = entityWithId(metadata.get("@graph"), "./");
    JsonNode hasPart = root.get("hasPart");
    assertNotNull(hasPart, "the root entity must carry hasPart");

    assertTrue(idArrayContains(hasPart, "jipx.json"), "the manifest must be listed in hasPart");
    // parts is a JSON object (entry path -> part key); iterate its FIELD NAMES
    manifest.get("parts").fieldNames().forEachRemaining(partPath ->
            assertTrue(idArrayContains(hasPart, partPath),
                    "part " + partPath + " must be listed in hasPart"));
    assertTrue(idArrayContains(hasPart, "README.md"));

    JsonNode manifestEntity = entityWithId(metadata.get("@graph"), "jipx.json");
    assertNotNull(manifestEntity, "the manifest data entity must be present");
    assertEquals("application/json", manifestEntity.get("encodingFormat").asText());
    assertEquals("JIPipe project manifest", manifestEntity.get("name").asText());
}
```

Note for the implementer: `parts` is a JSON object (entry path -> part key); the test above iterates its field names via `fieldNames()`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-launcher -Dtest=JIPipeProjectCrateConformanceTest`
Expected: FAIL - `hasPart` lists only `project/graph.json` and `README.md`; the manifest entity is absent.

- [ ] **Step 3: Implement**

1. `JIPipeProjectCrate`: change the signature to `buildCrate(JIPipeProject project, Set<String> partEntryPaths, List<String> violationsOut)` and thread `partEntryPaths` into `newBuilder(project, partEntryPaths)`. Update the class javadoc accordingly (inventory requirement: manifest + part files listed; unknown/payload entries intentionally not listed in profile 1.0).
2. In `newBuilder`, after the README entity, add:

```java
// The manifest entity (profile 1.0 container-inventory requirement).
builder.addDataEntity(new FileEntity.FileEntityBuilder()
        .setId(JIPipeXManifest.MANIFEST_FILE_NAME)
        .addType("File")
        .addProperty("encodingFormat", "application/json")
        .addProperty("name", "JIPipe project manifest")
        .build());

// One File entity per part except the main entity, which is the
// WorkflowEntity added above (same @id - adding it twice would
// overwrite the workflow entity).
for (String partEntryPath : partEntryPaths) {
    if (MAIN_ENTITY_ID.equals(partEntryPath)) {
        continue;
    }
    builder.addDataEntity(new FileEntity.FileEntityBuilder()
            .setId(partEntryPath)
            .addType("File")
            .addProperty("encodingFormat", "application/json")
            .build());
}
```

(`addDataEntity` auto-wires root `hasPart`.)

3. `JIPipeXProjectWriter.writeWithViolations`, line ~337: replace the call with

```java
RoCrate crate = JIPipeProjectCrate.buildCrate(project, workspace.getManifest().getParts().keySet(), violations);
```

(the manifest is fresh: `materializeFromModel` ran first in the same synchronized block). Update the `buildCrate` javadoc `@param`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-launcher -Dtest=JIPipeProjectCrateConformanceTest`
Expected: PASS (6 tests).

- [ ] **Step 5: Run the full jipx test package and the round-trip suite**

Run: `mvn test -pl jipipe-launcher -Dtest='JIPipeX*Test,JIPipeProject*Test,JipxRoundTripFixpointTest,CorpusRoundTripFixpointTest'`
Expected: PASS (crate metadata changes must not affect project-JSON round-trips).

- [ ] **Step 6: External sanity check (manual, one-off)**

Write one container (any test or the fixture) and run a local RO-Crate validator on the extracted folder, e.g. `pipx run rocrate validate <dir>` (or `python -m rocrate validate`). Expected: no errors. If the tool reports issues, stop and report before "fixing" anything ad hoc.

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectCrate.java jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeXProjectWriter.java jipipe-launcher/src/test/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectCrateConformanceTest.java
git commit -m "feat(jipx): list manifest and parts in the crate root hasPart, #1360"
```

---

### Task 3: Spec page, language page, and routes (website repo)

**Files:**
- Create: `src/main/java/org/hkijena/jipipecmswebsite/controllers/RoCrateSpecController.java`
- Create: `src/main/resources/templates/ro_crate_spec.html`
- Create: `src/main/resources/templates/jipipe_language.html`
- Test: `src/test/java/org/hkijena/jipipecmswebsite/controllers/RoCrateSpecControllerTest.java`

Repo: `/fastdata/src/jipipe-cms-website`, branch `1360-ro-crate-spec-page` off `master`.

**Interfaces:**
- Consumes: the `layout :: base(~{::section})` decorator; model attributes `pageTitle` (String) and `activeNav` (String, may be omitted); the `@WebMvcTest` controller-test pattern of `PagesControllerTest` (no service mocks needed - this controller has no dependencies).
- Produces: routes `GET /ro-crate/jipipe-workflow/1.0` (+ `/`), `GET /ro-crate/jipipe-workflow` (+ `/`) -> redirect, `GET /jipipe` (+ `/`); view names `ro_crate_spec` and `jipipe_language`. Task 4's static JSON path complements the versioned route.

- [ ] **Step 1: Write the failing test**

```java
package org.hkijena.jipipecmswebsite.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoCrateSpecController.class)
class RoCrateSpecControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void versionedSpecPageRenders() throws Exception {
        mockMvc.perform(get("/ro-crate/jipipe-workflow/1.0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("JIPipe Workflow RO-Crate profile 1.0")))
                .andExpect(content().string(containsString("https://jipipe.org/ro-crate/jipipe-workflow/1.0")))
                .andExpect(content().string(containsString("https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0/ro-crate-metadata.json")))
                .andExpect(content().string(containsString("https://ror.org/055s37c97")));
        mockMvc.perform(get("/ro-crate/jipipe-workflow/1.0/")).andExpect(status().isOk());
    }

    @Test
    void latestPermalinkRedirectsToCurrentVersion() throws Exception {
        mockMvc.perform(get("/ro-crate/jipipe-workflow"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ro-crate/jipipe-workflow/1.0"));
        mockMvc.perform(get("/ro-crate/jipipe-workflow/"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void languagePageRenders() throws Exception {
        mockMvc.perform(get("/jipipe"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("https://jipipe.hki-jena.de/")));
        mockMvc.perform(get("/jipipe/")).andExpect(status().isOk());
    }

    @Test
    void unknownVersionIsNotFound() throws Exception {
        mockMvc.perform(get("/ro-crate/jipipe-workflow/1.1")).andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RoCrateSpecControllerTest`
Expected: COMPILATION ERROR (`RoCrateSpecController` does not exist).

- [ ] **Step 3: Implement controller and templates**

`RoCrateSpecController.java` (style of `PagesController`, no constructor dependencies):

```java
@Controller
public class RoCrateSpecController {

    @GetMapping({"/ro-crate/jipipe-workflow/1.0", "/ro-crate/jipipe-workflow/1.0/"})
    public ModelAndView workflowRoCrateSpec() {
        ModelAndView model = new ModelAndView("ro_crate_spec");
        model.addObject("pageTitle", "JIPipe Workflow RO-Crate profile 1.0");
        return model;
    }

    @GetMapping({"/ro-crate/jipipe-workflow", "/ro-crate/jipipe-workflow/"})
    public ModelAndView workflowRoCrateSpecLatest() {
        // The "latest" permalink always points at the current version.
        return new ModelAndView("redirect:/ro-crate/jipipe-workflow/1.0");
    }

    @GetMapping({"/jipipe", "/jipipe/"})
    public ModelAndView jipipeLanguage() {
        ModelAndView model = new ModelAndView("jipipe_language");
        model.addObject("pageTitle", "JIPipe");
        return model;
    }
}
```

`ro_crate_spec.html` (`th:replace="~{layout :: base(~{::section})}"`, content prose taken from the spec document sections 3, 4, 9 - the spec is the copy source):

Header block, exactly these literals: title "JIPipe Workflow RO-Crate profile 1.0"; permalink (this version) `https://jipipe.org/ro-crate/jipipe-workflow/1.0`; permalink (latest) `https://jipipe.org/ro-crate/jipipe-workflow/`; version "1.0.0"; publisher "Leibniz-Institut für Naturstoff-Forschung und Infektionsbiologie e. V. - Hans-Knöll-Institut (HKI)" linking `https://ror.org/055s37c97` and `https://www.leibniz-hki.de/`; license "CC-BY-4.0" linking `https://creativecommons.org/licenses/by/4.0/`.

Body sections: abstract (JIPX containers are Workflow RO-Crates; this profile says what a third party needs to read, write, and execute them); the eight normative requirements as subsections (copy MUST/SHOULD/MAY statements from spec section 4, with the example `ro-crate-metadata.json` from spec section 4 in a `<pre>` block); a "Profile Crate" paragraph linking `https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0/ro-crate-metadata.json`; a "Versioning" section (spec section 9: semver, permalink carries major.minor, old versions stay published); a "Reserved extension points" section (`data/`, `graphs/`, `history/`, unknown parts, unknown manifest entries). HTML-escape nothing manually - Thymeleaf escapes `th:` text; the JSON example goes in a plain `<pre><code>` with `<` written as `&lt;` where needed (the example has none). No em/en dashes in the prose.

`jipipe_language.html` (same decorator): h1 "JIPipe", one paragraph: "JIPipe is a visual batch processing language for image analysis, developed by the Leibniz-Institut für Naturstoff-Forschung und Infektionsbiologie e. V. - Hans-Knöll-Institut (HKI). JIPipe workflows are packaged and exchanged as Workflow RO-Crates; see the JIPipe Workflow RO-Crate profile." linking `https://jipipe.org/ro-crate/jipipe-workflow/1.0` and the site home `https://jipipe.hki-jena.de/`. No `activeNav` on either page (no nav entry in 1.0).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=RoCrateSpecControllerTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Run the full website suite**

Run: `mvn test`
Expected: PASS (existing `PagesControllerTest`, `RichTextToHtmlRendererTest`, `JipipeCmsWebsiteApplicationTests` unaffected).

- [ ] **Step 6: Verify by eye**

Run: `mvn spring-boot:run` and open `http://localhost:8080/ro-crate/jipipe-workflow/1.0`, `http://localhost:8080/jipipe`, `http://localhost:8080/ro-crate/jipipe-workflow` (redirect). Confirm layout, nav, and prose render correctly. Stop the server.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/org/hkijena/jipipecmswebsite/controllers/RoCrateSpecController.java src/main/resources/templates/ro_crate_spec.html src/main/resources/templates/jipipe_language.html src/test/java/org/hkijena/jipipecmswebsite/controllers/RoCrateSpecControllerTest.java
git commit -m "feat: JIPipe Workflow RO-Crate profile 1.0 spec page and language page, #1360"
```

---

### Task 4: Profile Crate static JSON (website repo)

**Files:**
- Create: `src/main/resources/static/ro-crate/jipipe-workflow/1.0/ro-crate-metadata.json`
- Modify: `src/test/java/org/hkijena/jipipecmswebsite/controllers/RoCrateSpecControllerTest.java`

**Interfaces:**
- Consumes: Task 3's controller and test class.
- Produces: the machine-readable Profile Crate at `https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0/ro-crate-metadata.json` (static resource, served by the default resource handler).

- [ ] **Step 1: Write the failing test**

Add to `RoCrateSpecControllerTest`:

```java
@Test
void profileCrateIsServed() throws Exception {
    mockMvc.perform(get("/ro-crate/jipipe-workflow/1.0/ro-crate-metadata.json"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(content().string(containsString("\"version\": \"1.0.0\"")))
            .andExpect(content().string(containsString("https://jipipe.org/ro-crate/jipipe-workflow/1.0")))
            .andExpect(content().string(containsString("https://ror.org/055s37c97")));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RoCrateSpecControllerTest`
Expected: FAIL - 404, the static resource does not exist.

- [ ] **Step 3: Create the Profile Crate JSON**

Create `src/main/resources/static/ro-crate/jipipe-workflow/1.0/ro-crate-metadata.json` with EXACTLY this content (mirrors the Workflow RO-Crate 1.0 Profile Crate structure; adjust nothing):

```json
{
  "@context": "https://w3id.org/ro/crate/1.1/context",
  "@graph": [
    {
      "@id": "ro-crate-metadata.json",
      "@type": "CreativeWork",
      "about": { "@id": "./" },
      "conformsTo": [
        { "@id": "https://w3id.org/ro/crate/1.1" },
        { "@id": "https://www.researchobject.org/ro-crate/1.2/profiles" }
      ],
      "license": { "@id": "https://spdx.org/licenses/CC-BY-4.0" }
    },
    {
      "@id": "./",
      "@type": "Dataset",
      "name": "JIPipe Workflow RO-Crate profile",
      "description": "This is the Profile Crate for the JIPipe Workflow RO-Crate profile. A JIPipe Workflow RO-Crate is a Workflow RO-Crate that packages a JIPipe project (a JIPX container): the main entity is the JIPX main graph part (project/graph.json) written in the JIPipe workflow language, and the jipx.json manifest describes the container structure.",
      "datePublished": "2026-11-15T00:00:00Z",
      "version": "1.0.0",
      "identifier": "https://jipipe.org/ro-crate/jipipe-workflow/1.0",
      "sameAs": { "@id": "https://jipipe.org/ro-crate/jipipe-workflow/1.0" },
      "license": { "@id": "https://spdx.org/licenses/CC-BY-4.0" },
      "publisher": { "@id": "https://ror.org/055s37c97" },
      "author": { "@id": "https://orcid.org/0000-0002-0723-6038" },
      "hasPart": [
        { "@id": "https://jipipe.org/ro-crate/jipipe-workflow/1.0" },
        { "@id": "#base-profile" },
        { "@id": "#declare-descriptor" },
        { "@id": "#declare-root" },
        { "@id": "#main-workflow" },
        { "@id": "#workflow-language" },
        { "@id": "#manifest" },
        { "@id": "#readme" },
        { "@id": "#container-inventory" },
        { "@id": "#reserved-slots" }
      ],
      "mentions": [
        { "@id": "https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0" }
      ]
    },
    {
      "@id": "https://jipipe.org/ro-crate/jipipe-workflow/1.0",
      "@type": "Guide",
      "name": "JIPipe Workflow RO-Crate profile recommendations",
      "hasPart": [
        { "@id": "#base-profile" },
        { "@id": "#declare-descriptor" },
        { "@id": "#declare-root" },
        { "@id": "#main-workflow" },
        { "@id": "#workflow-language" },
        { "@id": "#manifest" },
        { "@id": "#readme" },
        { "@id": "#container-inventory" },
        { "@id": "#reserved-slots" }
      ]
    },
    {
      "@id": "#base-profile",
      "@type": "Recommendation",
      "name": "Be a Workflow RO-Crate",
      "category": "MUST",
      "itemReviewed": { "@id": "https://w3id.org/workflowhub/workflow-ro-crate/1.0" },
      "reviewBody": "The container MUST be a valid Workflow RO-Crate 1.0 (hence a valid RO-Crate 1.1), zipped, with the ro-crate-metadata.json file at the root of the zip archive."
    },
    {
      "@id": "#declare-descriptor",
      "@type": "Recommendation",
      "name": "Declare profile conformance in the metadata descriptor",
      "category": "MUST",
      "itemReviewed": { "@id": "https://jipipe.org/ro-crate/jipipe-workflow/1.0" },
      "reviewBody": "The Metadata File Descriptor conformsTo MUST be an array that contains https://w3id.org/ro/crate/1.1, https://w3id.org/workflowhub/workflow-ro-crate/1.0, and https://jipipe.org/ro-crate/jipipe-workflow/1.0."
    },
    {
      "@id": "#declare-root",
      "@type": "Recommendation",
      "name": "Declare profile conformance on the root data entity",
      "category": "SHOULD",
      "itemReviewed": { "@id": "https://jipipe.org/ro-crate/jipipe-workflow/1.0" },
      "reviewBody": "The Root Data Entity conformsTo SHOULD additionally contain https://w3id.org/workflowhub/workflow-ro-crate/1.0 and https://jipipe.org/ro-crate/jipipe-workflow/1.0."
    },
    {
      "@id": "#main-workflow",
      "@type": "Recommendation",
      "name": "Include the JIPX main graph as Main Workflow",
      "category": "MUST",
      "itemReviewed": { "@id": "https://bioschemas.org/ComputationalWorkflow" },
      "reviewBody": "The root mainEntity MUST reference project/graph.json, a data entity of type [\"File\", \"SoftwareSourceCode\", \"ComputationalWorkflow\"] with encodingFormat application/json. This file is the executable workflow a third party reads or writes."
    },
    {
      "@id": "#workflow-language",
      "@type": "Recommendation",
      "name": "Declare the JIPipe workflow language",
      "category": "MUST",
      "itemReviewed": { "@id": "https://jipipe.org/jipipe" },
      "reviewBody": "The Main Workflow's programmingLanguage MUST reference a ComputerLanguage contextual entity with @id https://jipipe.org/jipipe, name JIPipe, and version set to the JIPipe version that wrote the container. The entity SHOULD carry url https://jipipe.hki-jena.de/jipipe."
    },
    {
      "@id": "#manifest",
      "@type": "Recommendation",
      "name": "Include the JIPX manifest",
      "category": "MUST",
      "itemReviewed": { "@id": "https://jipipe.org/ro-crate/jipipe-workflow/1.0" },
      "reviewBody": "The container MUST contain jipx.json with format-version (integer, 1 in this version), jipipe-version (string), main-graph (zip entry path of the Main Workflow), and a parts table mapping zip entry paths to part keys. Writers MUST preserve unknown manifest entries and unknown container files verbatim."
    },
    {
      "@id": "#readme",
      "@type": "Recommendation",
      "name": "Include a README",
      "category": "MUST",
      "itemReviewed": { "@id": "https://jipipe.org/ro-crate/jipipe-workflow/1.0" },
      "reviewBody": "The container MUST contain a file README.md at the root. It SHOULD be about ./ and SHOULD have encodingFormat text/markdown."
    },
    {
      "@id": "#container-inventory",
      "@type": "Recommendation",
      "name": "List the manifest and parts in hasPart",
      "category": "SHOULD",
      "itemReviewed": { "@id": "https://jipipe.org/ro-crate/jipipe-workflow/1.0" },
      "reviewBody": "The manifest and every part file SHOULD be listed as data entities referenced from the root hasPart."
    },
    {
      "@id": "#reserved-slots",
      "@type": "Recommendation",
      "name": "Reserved payload slots",
      "category": "MAY",
      "itemReviewed": { "@id": "https://jipipe.org/ro-crate/jipipe-workflow/1.0" },
      "reviewBody": "The directories data/, graphs/, and history/ MAY appear as payload (Dataset entities in hasPart when present). They are reserved extension points for later profile versions and carry no defined meaning in 1.0."
    },
    {
      "@id": "https://w3id.org/ro/crate/1.1",
      "@type": "Guide",
      "name": "RO-Crate specification",
      "version": "1.1"
    },
    {
      "@id": "https://w3id.org/workflowhub/workflow-ro-crate/1.0",
      "@type": "Guide",
      "name": "Workflow RO-Crate profile 1.0",
      "version": "1.0"
    },
    {
      "@id": "https://bioschemas.org/ComputationalWorkflow",
      "@type": "DefinedTerm",
      "name": "ComputationalWorkflow",
      "termCode": "ComputationalWorkflow",
      "subjectOf": { "@id": "https://bioschemas.org/profiles/ComputationalWorkflow/1.0-RELEASE" }
    },
    {
      "@id": "https://bioschemas.org/profiles/ComputationalWorkflow/1.0-RELEASE",
      "@type": "Guide",
      "name": "ComputationalWorkflow Profile",
      "version": "1.0-RELEASE",
      "description": "Bioschemas specification for describing a Computational Workflow"
    },
    {
      "@id": "https://jipipe.org/jipipe",
      "@type": "ComputerLanguage",
      "name": "JIPipe",
      "url": { "@id": "https://jipipe.hki-jena.de/jipipe" }
    },
    {
      "@id": "https://ror.org/055s37c97",
      "@type": "Organization",
      "name": "Leibniz-Institut für Naturstoff-Forschung und Infektionsbiologie e. V. - Hans-Knöll-Institut (HKI)",
      "url": { "@id": "https://www.leibniz-hki.de/" }
    },
    {
      "@id": "https://orcid.org/0000-0002-0723-6038",
      "@type": "Person",
      "name": "Ruman Gerst"
    },
    {
      "@id": "https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0",
      "@type": "WebPage",
      "name": "JIPipe Workflow RO-Crate profile 1.0 specification",
      "encodingFormat": "text/html"
    }
  ]
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=RoCrateSpecControllerTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Validate the Profile Crate with the external RO-Crate tool (manual, one-off)**

Extract the JSON and run `pipx run rocrate validate <dir>` (rocrate reads `ro-crate-metadata.json` from a directory). Expected: no errors. Report any complaint instead of hand-tuning the structure silently.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/ro-crate/jipipe-workflow/1.0/ro-crate-metadata.json src/test/java/org/hkijena/jipipecmswebsite/controllers/RoCrateSpecControllerTest.java
git commit -m "feat: JIPipe Workflow RO-Crate Profile Crate, #1360"
```

---

### Task 5: jipipe.org redirects (applied-systems-biology/jipipe, gh-pages)

**Files:**
- Create: `ro-crate/jipipe-workflow/1.0/index.html`
- Create: `ro-crate/jipipe-workflow/index.html`
- Create: `jipipe/index.html`

Repo: `/fastdata/src/jipipe-github`. Branch: `git checkout -b 1360-ro-crate-redirects origin/gh-pages`.

**Interfaces:**
- Consumes: the existing gh-pages redirect pattern (`download/index.html`: title "JIPipe for ImageJ", meta-refresh, "Please follow ... this link" body). CNAME is `www.jipipe.org`; apex `jipipe.org` 301s to it, so serving under `www.jipipe.org` covers both.

- [ ] **Step 1: Create the three redirect pages**

Each file has exactly this shape (fill in the target):

```html
<!DOCTYPE html>
<html>
  <head>
    <title>JIPipe for ImageJ</title>
    <meta charset="utf-8">
    <meta http-equiv="refresh" content="0; url='TARGET'" />
  </head>
  <body>
    <p>Please follow <a href="TARGET">this link</a>.</p>
  </body>
</html>
```

Targets:
- `ro-crate/jipipe-workflow/1.0/index.html` -> `https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0`
- `ro-crate/jipipe-workflow/index.html` -> `https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0` (the "latest" permalink points at the current version; update this file when a new profile version is published)
- `jipipe/index.html` -> `https://jipipe.hki-jena.de/jipipe`

- [ ] **Step 2: Verify the files match the existing pattern**

Run: `grep -c "meta http-equiv" ro-crate/jipipe-workflow/1.0/index.html ro-crate/jipipe-workflow/index.html jipipe/index.html`
Expected: `1` for each file. Then compare visually against `git show origin/gh-pages:download/index.html` (same head block, same body wording, only the target differs; no em/en dashes).

- [ ] **Step 3: Commit**

```bash
git add ro-crate/jipipe-workflow/1.0/index.html ro-crate/jipipe-workflow/index.html jipipe/index.html
git commit -m "add jipipe.org redirects for the JIPipe Workflow RO-Crate profile (#1360)"
```

- [ ] **Step 4: Push (requires explicit user confirmation)**

Do NOT push until the user confirms. After confirmation: `git push origin 1360-ro-crate-redirects:gh-pages` (or push the branch and fast-forward, per the user's preference). Then verify from a cold cache:

Run: `curl -s https://jipipe.org/ro-crate/jipipe-workflow/1.0 | head -8`
Expected: the meta-refresh HTML with target `https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0`.

---

## Merge order and follow-ups

1. Tasks 1-2 (jipipe MR) and Tasks 3-4 (website MR) are independent; Task 5 lands any time.
2. Website merge should precede or accompany the jipipe MR merge so the profile URI resolves once consumers see it.
3. Tag the website repo `jipipe-workflow-ro-crate-1.0.0` when its MR merges.
4. Follow-ups (not in this plan): news entry/resources link, FormParameter descriptions, HTTP-level redirects.
