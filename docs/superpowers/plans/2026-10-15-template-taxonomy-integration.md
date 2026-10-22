# Template-Taxonomy Integration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fold node templates into the taxonomy-driven add-nodes panel (replacing the separate Templates dock panel), give templates a taxonomy function, generalize workflow relations into Similar/Previous/Next mined from family steps and template graphs, and ship the 185 corpus-mined patterns as System templates.

**Architecture:** Templates become first-class node-database entries carrying a `JIPipeTaxonomyFunctionRef` (a dedicated reference class serialized as a plain id string, mirroring `JIPipeDataInfoRef`) plus a `TemplateScope`. They appear in the function tree under their function AND under a synthetic `Templates` branch (System/Global/Project). A new `NodeRelationIndex` computes Similar/Previous/Next from pluggable sources (family steps + all template graphs), replacing the fragmented `followUps`/`related` API. The 185 pattern JSONs ship as bundled System resources.

**Tech Stack:** Java 21, Jackson (parameter/template serialization), Swing (browser/finder/rail/modal), JUnit 5 (jipipe-core unit tests; jipipe-launcher full-registry integration tests).

**Spec:** `docs/superpowers/specs/2026-10-15-template-taxonomy-integration-design.md` — this plan implements it. The spec's §1–§13 map to the tasks below; re-read the spec section a task cites before implementing it.

## Global Constraints

- **Stable-ID rule:** every registered id is a stable string literal, never code-derived. New ids introduced here: `jipipe:taxonomy-function` (param type), `jipipe:function:templates` (catch-all leaf). Both are literals.
- **Value type rule:** `taxonomyFunction` is `JIPipeTaxonomyFunctionRef`, NOT a `String` — a bare string would present the default string editor. (Spec §6.)
- **Scope is storage location, not a field:** System = bundled resources registered at init; Global = user dir; Project = project metadata. (Spec §5.)
- **No explicit template ordering** anywhere — name-sorted in every placement. (Spec, user decision.)
- **Badges computed, not stored:** `Pattern` = template graph ≥ 2 nodes; `Custom` = 1 node. (Spec §5.)
- **Mining scope for relations:** family steps (all families) + **all** template scopes (System+Global+Project). (Spec §10.2, user: "mine the full extent from the templates".)
- **Virtual relation family:** `TemplateRelationFamily` is internal; it must NOT appear in public `getFamilies()` views or trip `TaxonomyFullValidationTest`. (Spec §10.2.)
- **Read-only references:** `/data/src/jipipe-2` and the corpus are never written; the 185 pattern JSONs are *copied* from `utils/taxonomy-research/patterns/templates/` into bundled resources (the research dir stays a study artifact).
- **Offline build:** `mvn -o`. Core suite baseline is 384 tests / 1 skipped (must stay green, with the expected additions). Launcher tests need the ImageJ patcher protection already in place — do not add listeners/`--add-opens`.
- **6.0.0 unreleased:** removing `JIPipeNodeDatabase.followUps(entry)`/`related(entry, max)` and `JIPipeNodeTemplate.menuPath` is allowed; update all in-tree consumers, none are deleted.
- **Commits:** continue the `(N)` sequence. The plan doc is **(49)**; tasks are **(50)–(58)** below. (If execution opens a fresh branch/worktree, the sequence restarts from that branch's base — keep the same subjects.)

## Review Focus

Five input classes/failure modes the spec implies but a naive implementation breaks — each is pinned by a test in the owning task:

1. **Stale function id** — a template whose `taxonomyFunction` id no longer resolves (a function was removed by an upgrade). Expect: no throw, `isResolved()==false`, `getFunction()==null`, and tree placement in the `jipipe:function:templates` catch-all. → Task 1 (ref) + Task 4 (entry `getFunctionIds()`).
2. **Stale template edge** — a template graph containing a node id no longer in the registry. Expect: `TemplateRelationSource` logs a warning and skips that edge; startup and `relations()` never fail. → Task 5.
3. **Single-node, zero-edge template** — a `Custom` template with no edges. Expect: `Custom` badge, empty previous/next, still listed under its function AND under `Templates/<scope>`. → Task 4 (entry).
4. **Compact (1-pane) mode context menu** — the tree takes over the node-list role below 380 px but its leaves currently have no menu. Expect: the tree-leaf menu and the list menu produce **identical** items for the same entry (the named parity bug). → Task 7.
5. **De-duplication in the node list** — a template visible under both its function subtree and its scope group in the same query. Expect: it appears **once** in the list (deduped by entry id). → Task 6.

---

### Task 1: `JIPipeTaxonomyFunctionRef` + parameter type `jipipe:taxonomy-function`

**Spec:** §4, §6.

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/taxonomy/JIPipeTaxonomyFunctionRef.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/taxonomy/JIPipeDesktopTaxonomyFunctionRefParameterEditorUI.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/StandardParametersPlugin.java` (add a `registerTaxonomyFunctionRef()` call next to `registerJIPipeTypes()` at ~line 633)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/parameters/library/taxonomy/JIPipeTaxonomyFunctionRefTest.java`

**Interfaces:**
- Produces: `JIPipeTaxonomyFunctionRef` — `JIPipeTaxonomyFunctionRef()` (unset), `JIPipeTaxonomyFunctionRef(String id)`, `JIPipeTaxonomyFunctionRef(JIPipeTaxonomyFunctionRef other)`; `String getId()` (null when unset); `JIPipeNodeTaxonomyFunction getFunction()` (null when unset or unresolved); `boolean isResolved()`; custom `@JsonSerialize`/`@JsonDeserialize` writing/reading the id string or `null`; `equals`/`hashCode` on the id. Registered as param type id `jipipe:taxonomy-function`.
- Consumes: `JIPipe.getNodes().getFunctions()` → `FunctionRegistry` (`byId(id)`, `all()`).

- [ ] **Step 1: Write the failing test** (serialization + resolve + equals, in `JIPipeTaxonomyFunctionRefTest`):

```java
class JIPipeTaxonomyFunctionRefTest {
    @Test void serializesToIdString() throws Exception {
        String json = JsonUtils.toPrettyJsonString(new JIPipeTaxonomyFunctionRef("jipipe:function:images"));
        assertThat(json, containsString("jipipe:function:images"));
    }
    @Test void deserializesFromIdString() throws Exception {
        JIPipeTaxonomyFunctionRef ref =
            JsonUtils.readFromString("\"jipipe:function:tables\"", JIPipeTaxonomyFunctionRef.class);
        assertThat(ref.getId()).isEqualTo("jipipe:function:tables");
    }
    @Test void nullRoundTripsToUnset() {
        JIPipeTaxonomyFunctionRef ref = JsonUtils.readFromString("null", JIPipeTaxonomyFunctionRef.class);
        assertThat(ref.getId()).isNull();
        assertThat(ref.isResolved()).isFalse();
    }
    @Test void unresolvedIdDoesNotThrow() {
        JIPipeTaxonomyFunctionRef ref = new JIPipeTaxonomyFunctionRef("jipipe:function:does-not-exist");
        assertThat(ref.isResolved()).isFalse();   // registry not booted in a unit test → unresolved
        assertThat(ref.getFunction()).isNull();
    }
    @Test void equalsAndHashCodeOnId() {
        assertThat(new JIPipeTaxonomyFunctionRef("x")).isEqualTo(new JIPipeTaxonomyFunctionRef("x"));
        assertThat(new JIPipeTaxonomyFunctionRef("x")).isNotEqualTo(new JIPipeTaxonomyFunctionRef("y"));
    }
}
```

Note: `unresolvedIdDoesNotThrow` relies on the ref **not** calling `JIPipe.getInstance()` when the JIPipe singleton is absent — guard with `JIPipe.getInstance() != null` so unit tests (no boot) resolve to `isResolved()==false`. Confirm the guard in the test that `new JIPipeTaxonomyFunctionRef("…")` never throws.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -pl jipipe-core test -Dtest='JIPipeTaxonomyFunctionRefTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compile failure (class does not exist).

- [ ] **Step 3: Implement `JIPipeTaxonomyFunctionRef`** in `…/library/taxonomy/JIPipeTaxonomyFunctionRef.java`. Mirror `JIPipeDataInfoRef` exactly: `private String id;` + the three constructors; `getId()` returns `id`; `isResolved()` = `id != null && JIPipe.getInstance() != null && JIPipe.getNodes().getFunctions().byId(id) != null`; `getFunction()` returns `byId(id)` when resolved else `null`. Inner `Serializer` writes `writeString(id)` (null → `writeNull`); inner `Deserializer` reads `p.getValueAsString()` → `new JIPipeTaxonomyFunctionRef(str)`, or `new JIPipeTaxonomyFunctionRef()` on `p.getNullValue()`/empty. `equals`/`hashCode` on `id`. Implement `JIPipeValidatable` with `reportValidity` that, when set-but-unresolved, reports an (informational) issue rather than throwing — consistent with `JIPipeDataInfoRef`.

- [ ] **Step 4: Implement the editor UI** `JIPipeDesktopTaxonomyFunctionRefParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeTaxonomyFunctionRef>` modeled on `JIPipeDesktopDataInfoRefParameterEditorUI`: a `currentlyDisplayed` button showing the resolved function name (or "Not set") + a "Browse…" button opening a picker dialog. The picker dialog builds a `JTree` from `JIPipe.getNodes().getFunctions().all()` (parent→children via `getParent()`), rendered with `FunctionTreeCellRenderer`, with a `JTextField` filter (case-insensitive name match, keeps a leaf if it or any ancestor matches). **Both branches and leaves selectable.** Selecting a node sets the ref's id; an "Unset" menu item clears it. (Reuse the tree-building helpers from `NodeBrowserTreeController` where practical.)

- [ ] **Step 5: Register the parameter type** in `StandardParametersPlugin`: add a private `registerTaxonomyFunctionRef()` invoked from the existing registration flow (next to `registerJIPipeTypes()`), using the 7-arg overload:

```java
registerParameterType("jipipe:taxonomy-function",
    JIPipeTaxonomyFunctionRef.class,
    JIPipeParameterArchetype.Value, JIPipeTaxonomyFunctionRef::new,
    r -> new JIPipeTaxonomyFunctionRef((JIPipeTaxonomyFunctionRef) r),
    "Taxonomy function", "The taxonomy function this template is filed under",
    JIPipeDesktopTaxonomyFunctionRefParameterEditorUI.class);
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn -o -pl jipipe-core test -Dtest='JIPipeTaxonomyFunctionRefTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (5 tests).

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/library/taxonomy \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/taxonomy \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/StandardParametersPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/plugins/parameters/library/taxonomy
git commit -m "feat: JIPipeTaxonomyFunctionRef parameter type (taxonomy-function picker) (6.0.0) (50)"
```

---

### Task 2: `TemplatesFunction` catch-all leaf

**Spec:** §4 (naming), §7.2 (catch-all leaf).

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions/TemplatesFunction.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/taxonomy/TaxonomyPlugin.java` (register the leaf in the `leaves` array)
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TemplatesFunctionTest.java`

**Interfaces:**
- Produces: `TemplatesFunction implements JIPipeNodeTaxonomyFunction` with `getId()` = `"jipipe:function:templates"`, `getName()` = `"Templates"`, `getParent()` = `new MiscellaneousFunction()`. Registered so it appears in the registry dumper's `functions[]`.
- Consumes: `MiscellaneousFunction` (parent), `FunctionRegistry.register`.

- [ ] **Step 1: Write the failing integration test** (boots the full registry):

```java
class TemplatesFunctionTest {
    @BeforeAll static void boot() { JIPipe.ensureInstance(); }
    @Test void isRegisteredAsLeafUnderMisc() {
        FunctionRegistry f = JIPipe.getNodes().getFunctions();
        JIPipeNodeTaxonomyFunction fn = f.byId("jipipe:function:templates");
        assertThat(fn).isNotNull();
        assertThat(fn.getParent().getId()).isEqualTo("jipipe:function:misc");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -pl jipipe-launcher -am test -Dtest='TemplatesFunctionTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (`byId` returns null — not yet registered).

- [ ] **Step 3: Implement `TemplatesFunction`** mirroring `MiscDataFunction`/`FileSystemFunction` (a leaf): `getId()` literal `jipipe:function:templates`, `getName()` `Templates`, `getParent()` `new MiscellaneousFunction()`, `getColorHue()` a fixed hue, `getDescription()`, `getIcon()` (a templates-ish icon from `JIPipe.RESOURCES`), `getTags()` empty, `isVisibleInPipeline()/isVisibleInCompartments()` `true`.

- [ ] **Step 4: Register it** in `TaxonomyPlugin` — add `new TemplatesFunction()` to the `leaves` array (anywhere in the misc group).

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -o -pl jipipe-launcher -am test -Dtest='TemplatesFunctionTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS. Then confirm `TaxonomyFullValidationTest` still green (the new leaf has no required node assignments — it is a catch-all, like `misc`).

Run: `mvn -o -pl jipipe-launcher -am test -Dtest='TaxonomyFullValidationTest,TemplatesFunctionTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: both PASS.

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions/TemplatesFunction.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/taxonomy/TaxonomyPlugin.java \
        jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TemplatesFunctionTest.java
git commit -m "feat: register jipipe:function:templates catch-all leaf under Miscellaneous (6.0.0) (51)"
```

---

### Task 3: `JIPipeNodeTemplate` model change + migration converter

**Spec:** §5 (data model), §9 (migration).

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/JIPipeNodeTemplate.java` (add `taxonomyFunction` ref + `migrateLegacyMenuPath()`; `menuPath` becomes a transient legacy capture — keep `@JsonSetter`, drop `@JsonGetter` + `@JIPipeParameter`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeNodeTemplatesServiceComponent.java` (call `migrateLegacyMenuPath()` in `reloadGlobalTemplates()` after each read)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/JIPipeNodeTemplateMigration.java` (the converter: `migrate(String)` + `appendPreviouslyFooter`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/JIPipeNodeTemplateMigrationTest.java`

**Interfaces:**
- Produces: `JIPipeNodeTemplate.getTaxonomyFunction()` / `setTaxonomyFunction(JIPipeTaxonomyFunctionRef)` (field `taxonomyFunction`, `@JIPipeParameter("taxonomy-function", uiOrder = -70)`, `@JsonGetter("taxonomy-function")`/`@JsonSetter("taxonomy-function")`). `JIPipeNodeTemplate.migrateLegacyMenuPath()` (public, **idempotent**, order-safe — see mechanism below). `JIPipeNodeTemplateMigration.migrate(String firstMenuSegment) -> JIPipeTaxonomyFunctionRef` (pure-ish: needs the booted `FunctionRegistry` for the name→id match; returns the ref or an unset ref) + `appendPreviouslyFooter(HTMLText desc, String oldPath) -> HTMLText`.
- **The legacy `menu-path` mechanism (R7 — supersedes the plan's earlier "ignore on read" wording):** `JIPipeNodeTemplate` is a Jackson *bean* (explicit `@JsonGetter`/`@JsonSetter` pairs; no class-level deserializer) loaded via `JsonUtils.readFromFile(..., JIPipeNodeTemplate.class)`. Simply dropping `menu-path` would make the lenient mapper silently ignore the key — LOSING the value the migration must map + footer. Instead: keep the `menuPath` field as a **transient legacy capture** — keep `@JsonSetter("menu-path")` (captured on read) but **remove `@JsonGetter("menu-path")`** (never written → "gone after one save cycle" for free). `migrateLegacyMenuPath()`: if `menuPath` non-empty AND `taxonomyFunction.getId()==null` → `taxonomyFunction = migrate(firstSegment)`; `description = appendPreviouslyFooter(description, oldPath)`; then empty `menuPath`. The `getId()==null` guard makes it **order-safe** (a file with BOTH keys: Jackson sets `taxonomy-function` too, so by the post-deserialization call the guard is false → no-op, existing function preserved) and **idempotent** (second call: `menuPath` empty → no-op).
- **Call sites for `migrateLegacyMenuPath()`** (post-deserialization, both load paths): (a) `JIPipeNodeTemplatesServiceComponent.reloadGlobalTemplates()` after each `readFromFile` (global); (b) `JIPipeNodeDatabaseBuilderRun` before building each template entry (Task 4 — covers the project-metadata path uniformly; idempotent so double-calling is harmless). This is the faithful reading of the spec's "at deserialization time" (no arbitrary logic can run during Jackson bean construction without a custom deserializer, and spec §9.5 requires the key to vanish after one save, which the no-getter transient capture gives).
- Consumes: `JIPipeTaxonomyFunctionRef` (Task 1), `FunctionRegistry.all()` (for the name→id match), `jipipe:function:templates` (Task 2) as the catch-all id.

- [ ] **Step 1: Write the failing test** (pure converter logic + round-trip):

```java
class JIPipeNodeTemplateMigrationTest {
    @BeforeAll static void boot() { JIPipe.ensureInstance(); }

    // --- the name→id matcher (R7b: a name tie resolves to the TOP-LEVEL branch before a leaf) ---
    @Test void imageSegmentMapsToTopLevelBranchNotExportLeaf() {
        // the registry has a childless leaf "Images" (export-images) AND the 18-child
        // top-level "Images" branch — R7b must pick the branch.
        JIPipeTaxonomyFunctionRef ref = JIPipeNodeTemplateMigration.migrate("Image");
        assertThat(ref.getId()).isEqualTo("jipipe:function:images");
    }
    @Test void tableSegmentMapsToTopLevelBranchNotExportLeaf() {
        JIPipeTaxonomyFunctionRef ref = JIPipeNodeTemplateMigration.migrate("Table");
        assertThat(ref.getId()).isEqualTo("jipipe:function:tables");
    }
    @Test void leafOnlyNameFallsThroughToLeaf() {
        // "Thresholding" is a leaf with no same-named top-level branch → leaf pass applies
        JIPipeTaxonomyFunctionRef ref = JIPipeNodeTemplateMigration.migrate("Thresholding");
        assertThat(ref.getId()).isEqualTo("jipipe:function:thresholding");
    }
    @Test void unknownSegmentYieldsUnset() {
        assertThat(JIPipeNodeTemplateMigration.migrate("TotallyUnknownXyz").isResolved()).isFalse();
        assertThat(JIPipeNodeTemplateMigration.migrate("TotallyUnknownXyz").getId()).isNull();
    }
    @Test void emptySegmentYieldsUnset() {
        assertThat(JIPipeNodeTemplateMigration.migrate("").isResolved()).isFalse();
    }

    // --- the model migration (transient capture + order-safe + footer + drop-on-save) ---
    @Test void legacyTemplateMigratesFunctionAndFooters() throws Exception {
        String json = "{\"name\":\"legacy\",\"menu-path\":[\"Image\",\"Threshold\"]}";
        JIPipeNodeTemplate t = JsonUtils.readFromString(json, JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        assertThat(t.getTaxonomyFunction().isResolved()).isTrue();   // "Image"→a real function
        assertThat(t.getDescription().getHtml(), containsString("Previously"));  // auditable footer
    }
    @Test void existingFunctionWinsOverMenuPath() throws Exception {  // order-safety
        String json = "{\"name\":\"x\",\"menu-path\":[\"Image\"],\"taxonomy-function\":\"jipipe:function:tables\"}";
        JIPipeNodeTemplate t = JsonUtils.readFromString(json, JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        assertThat(t.getTaxonomyFunction().getId()).isEqualTo("jipipe:function:tables");  // preserved
        assertThat(t.getDescription().getHtml(), not(containsString("Previously")));      // no footer
    }
    @Test void migrationIsIdempotent() throws Exception {
        JIPipeNodeTemplate t = JsonUtils.readFromString(
            "{\"name\":\"x\",\"menu-path\":[\"Image\"]}", JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        String afterFirst = t.getDescription().getHtml();
        t.migrateLegacyMenuPath();
        assertThat(t.getDescription().getHtml()).isEqualTo(afterFirst);  // no double footer
    }
    @Test void saveDropsLegacyKey() throws Exception {
        JIPipeNodeTemplate t = JsonUtils.readFromString(
            "{\"name\":\"x\",\"menu-path\":[\"Image\",\"Threshold\"]}", JIPipeNodeTemplate.class);
        t.migrateLegacyMenuPath();
        assertThat(JsonUtils.toPrettyJsonString(t), not(containsString("menu-path")));
    }
}
```

(If `JIPipeNodeTemplate` requires a non-null `data` field to construct, give each JSON a minimal valid `data` graph string — the point is the migration behavior, not the graph.)

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -pl jipipe-core test -Dtest='JIPipeNodeTemplateMigrationTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compile failure (no `migrate` / no `taxonomyFunction`).

- [ ] **Step 3: Implement the model change** in `JIPipeNodeTemplate`:
  - Add `private JIPipeTaxonomyFunctionRef taxonomyFunction = new JIPipeTaxonomyFunctionRef();` with `@JIPipeParameter(value = "taxonomy-function", uiOrder = -70)` + `@JsonGetter("taxonomy-function")` getter / `@JsonSetter("taxonomy-function")` setter.
  - Rework `menuPath` into the **transient legacy capture** (R7): keep the field + `@JsonSetter("menu-path")` (so it is captured on read), but **remove `@JsonGetter("menu-path")`** and the `@JIPipeParameter("menu-path")` annotation (it is no longer a user-facing parameter and must never be written). Keep a private accessor for the migration.
  - Add `public void migrateLegacyMenuPath()` per the R7 interface (the `getId()==null` guard + footer + clear `menuPath`).
  - Update the copy constructor + `getLocationInfo()` to use `taxonomyFunction` (the old `locationInfo` menu-path rendering becomes the scoped string — Task 4 finalizes the entry's `locationInfo`, so here just make it compile and not reference the removed getter).

- [ ] **Step 4: Implement `JIPipeNodeTemplateMigration`**:
  - `migrate(String firstSegment)`: if blank → unset ref. Else normalize via the abbreviation table (Image→Images, Table→Tables, Track→Tracking, Roi→ROIs, Plot→Plotting, Annotation→Annotations, Export→Export, Data→"Data Sources"), then match case-insensitively against function display names from `JIPipe.getNodes().getFunctions().all()` — **a name tie resolves to the TOP-LEVEL branch (parent==null) before a leaf** (R7b), then to a leaf. On match `new JIPipeTaxonomyFunctionRef(matchedId)`, else unset. **R7b rationale:** the registry has childless export sub-leaves literally named "Images"/"Tables" (`jipipe:function:export-images`/`export-tables`) that would shadow the 18-child top-level `Images` branch / `Tables` branch under a naive leaf-first pass — but legacy menuPath first-segments are top-level menu categories, so "Image" must map to `jipipe:function:images` and "Table" to `jipipe:function:tables`. (Unresolved = unset; the catch-all is applied by the entry in Task 4, never here.)
  - `appendPreviouslyFooter(HTMLText desc, String oldPath) -> HTMLText`: appends `<i><br>Previously: Image → Threshold</i>` (oldPath = the menu segments joined with " → ").
  - **Wire the call sites** (R7): `JIPipeNodeTemplatesServiceComponent.reloadGlobalTemplates()` → `template.migrateLegacyMenuPath()` after each `readFromFile`. (The `JIPipeNodeDatabaseBuilderRun` call site is added in Task 4; idempotency makes the double-call harmless.)

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -o -pl jipipe-core test -Dtest='JIPipeNodeTemplateMigrationTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (9 tests).

- [ ] **Step 6: Run the full core suite** to catch any other `menuPath` consumer:

Run: `grep -rn "getMenuPath\|menuPath\|setMenuPath" --include=*.java jipipe-core/src/main plugins/*/src/main` then fix/compile.
Run: `mvn -o -pl jipipe-core test`
Expected: BUILD SUCCESS, all green (baseline 389 from Tasks 1–2 + the 7 new migration tests). (Update any in-tree `getMenuPath()` reference the grep surfaces — e.g. `CreateNewNodesByTemplateDatabaseEntry`'s `locationInfo` — to a minimal compile fix; that file is fully reworked in Task 4.)

- [ ] **Step 7: Commit**

```bash
git add -A jipipe-core/src
git commit -m "feat: JIPipeNodeTemplate taxonomyFunction (ref) + menu-path migration converter (6.0.0) (52)"
```

---

### Task 4: `TemplateScope` + database entry extension

**Spec:** §7.1 (entry), §3 (badge rule).

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/entries/TemplateScope.java` (enum)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/entries/CreateNewNodesByTemplateDatabaseEntry.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeNodeDatabaseBuilderRun.java` (pass scope when building template entries — where `getAllTemplates`/`isPluginTemplate` are used)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/entries/CreateNewNodesByTemplateDatabaseEntryTest.java`

**Interfaces:**
- Produces: `TemplateScope { System, Global, Project }`. `CreateNewNodesByTemplateDatabaseEntry(JIPipeNodeTemplate, TemplateScope)`; `getScope()`; `getBadge()` → `"Pattern"` if `template.getGraph().getGraphNodes().size() >= 2` else `"Custom"`; `getFunctionIds()` → `{ref.getId()}` when resolved else `{jipipe:function:templates}`; `getLocationInfos()` → `["Templates / " + scope.name()]`.
- Consumes: `JIPipeNodeTemplate` (Task 3), `JIPipeTaxonomyFunctionRef` (Task 1), `JIPipe.getNodes().getFunctions()` (resolve).

- [ ] **Step 1: Write the failing test**:

```java
class CreateNewNodesByTemplateDatabaseEntryTest {
    @BeforeAll static void boot() { JIPipe.ensureInstance(); }
    // build a 1-node and a 2-node template from registry node ids
    private JIPipeNodeTemplate tmpl(int nodes, String fnId) { /* construct JIPipeGraph with n registry nodes + edges */ }
    @Test void badgeIsCustomForSingleNode() {
        var e = new CreateNewNodesByTemplateDatabaseEntry(tmpl(1, "jipipe:function:images"), TemplateScope.Project);
        assertThat(e.getBadge()).isEqualTo("Custom");
    }
    @Test void badgeIsPatternForMultiNode() {
        var e = new CreateNewNodesByTemplateDatabaseEntry(tmpl(2, "jipipe:function:images"), TemplateScope.Global);
        assertThat(e.getBadge()).isEqualTo("Pattern");
    }
    @Test void functionIdsFromRef() {
        var e = new CreateNewNodesByTemplateDatabaseEntry(tmpl(2, "jipipe:function:tables"), TemplateScope.System);
        assertThat(e.getFunctionIds()).containsExactly("jipipe:function:tables");
    }
    @Test void unresolvedRefFallsBackToCatchAll() {
        JIPipeNodeTemplate t = tmpl(2, null); t.setTaxonomyFunction(new JIPipeTaxonomyFunctionRef("jipipe:function:gone"));
        var e = new CreateNewNodesByTemplateDatabaseEntry(t, TemplateScope.Project);
        assertThat(e.getFunctionIds()).containsExactly("jipipe:function:templates");
    }
    @Test void locationInfoIsScoped() {
        var e = new CreateNewNodesByTemplateDatabaseEntry(tmpl(1, "jipipe:function:images"), TemplateScope.Global);
        assertThat(e.getLocationInfos()).containsExactly("Templates / Global");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -pl jipipe-core test -Dtest='CreateNewNodesByTemplateDatabaseEntryTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compile failure (no `TemplateScope` constructor arg / `getBadge`).

- [ ] **Step 3: Implement** `TemplateScope` enum + the entry changes (badge, functionIds with catch-all fallback, scoped locationInfo, `getScope()`). Add the overload constructor `(JIPipeNodeTemplate, TemplateScope)`; keep the old `(JIPipeNodeTemplate)` delegating to `Global` for source compat. Update `initializeTokens()` to add the scope word + badge to search tokens.

- [ ] **Step 4: Wire the scope in the builder** — in `JIPipeNodeDatabaseBuilderRun` where template entries are created (the `getAllTemplates` loop at ~line 99), determine scope: `isPluginTemplate(t)` → System, else (in `getGlobalTemplates()`) → Global, else (project metadata) → Project, and pass it to the constructor.

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -o -pl jipipe-core test -Dtest='CreateNewNodesByTemplateDatabaseEntryTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (5 tests).

- [ ] **Step 6: Run the full core suite**

Run: `mvn -o -pl jipipe-core test`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add -A jipipe-core/src
git commit -m "feat: TemplateScope + template database entry (function ids, badge, scoped location) (6.0.0) (53)"
```

---

### Task 5: Relations — Similar / Previous / Next

**Spec:** §10 (the relation model + sources + UI).

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/relations/NodeRelations.java` (record)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/relations/NodeRelationSource.java` (interface)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/relations/FamilyRelationSource.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/relations/TemplateRelationSource.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/relations/NodeRelationIndex.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeNodeDatabase.java` (add `relations(entry, maxPerList)`; remove `followUps(entry)` + `related(entry, max)`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/nodefinder/NodeFinderDetailRail.java` (3 sections)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/addnodepanel/JIPipeDesktopNodeBrowser.java` (follow-up strip → 3 groups)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/database/relations/NodeRelationIndexTest.java` (pure logic)
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/database/relations/TemplateRelationSourceTest.java` (integration: real template graph mining)

**Interfaces:**
- Produces: `record NodeRelations(List<JIPipeNodeDatabaseEntry> similar, List<JIPipeNodeDatabaseEntry> previous, List<JIPipeNodeDatabaseEntry> next)`. `JIPipeNodeDatabase.relations(JIPipeNodeDatabaseEntry, int maxPerList) -> NodeRelations`. `NodeRelationIndex` (pure, non-JIPipe) with `addEdge(String from, String to)`, `addEdgesIgnoringUnresolved(Set<String> knownIds, List<Edge> edges)` (drops + the caller-warns any edge touching an id not in `knownIds`), `List<String> next(String)`, `List<String> previous(String)` (support-desc then name). `NodeRelationSource` = `void contribute(Consumer<Edge> sink)`; the **production** sources resolve against the registry (skip + `logger.warn` unresolved) before calling `sink.accept(edge)`; `Edge(String from, String to)`.
- `FamilyRelationSource`: emits each family's resolved after-edges (reuse `FamilyAssembler`'s step graphs — successors AND predecessors). `TemplateRelationSource`: for every loaded template graph (all scopes), emit an `Edge` per in-graph connection (by node info id); skip+warn edges whose node id no longer resolves.
- Consumes: `FamilyAssembler` (existing), `JIPipe.getNodeTemplates().getAllTemplates(project)` (all scopes), `JIPipeNodeDatabaseEntry.getOriginNodeId()`.

- [ ] **Step 1: Write the failing pure-logic test** (index, no JIPipe boot):

```java
class NodeRelationIndexTest {
    @Test void edgeProducesBothDirections() {
        var idx = new NodeRelationIndex();
        idx.addEdge("a", "b");            // b follows a
        assertThat(idx.next("a")).containsExactly("b");
        assertThat(idx.previous("b")).containsExactly("a");
    }
    @Test void supportRanksHigherFirst() {
        var idx = new NodeRelationIndex();
        idx.addEdge("a", "x"); idx.addEdge("a", "y");   // x and y both follow a
        idx.addEdge("a", "y");                            // y seen again → higher support
        assertThat(idx.next("a")).isEqualTo(List.of("y", "x"));  // support desc, then name
    }
    @Test void sourceNodesGetPrevious() {
        // a template with source node S (no incoming edge) → previous(S) empty, next(S) = its out-neighbor
        var idx = new NodeRelationIndex();
        idx.addEdge("s", "p");
        assertThat(idx.previous("s")).isEmpty();
        assertThat(idx.next("s")).containsExactly("p");
    }
    @Test void staleEdgeIsSkippedNotThrown() {
        // a template edge whose node id no longer resolves is dropped, not fatal
        var idx = new NodeRelationIndex();
        idx.addEdgesIgnoringUnresolved(Set.of("known-node", "other-known"),   // "ghost-node" is NOT known
            List.of(new Edge("known-node", "ghost-node"), new Edge("known-node", "other-known")));
        assertThat(idx.next("known-node")).isEqualTo(List.of("other-known"));  // ghost edge dropped
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -pl jipipe-core test -Dtest='NodeRelationIndexTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compile failure (no `NodeRelationIndex`).

- [ ] **Step 3: Implement the index + sources** (`NodeRelationIndex`, `NodeRelationSource`, `Edge`, `FamilyRelationSource`, `TemplateRelationSource`). `TemplateRelationSource`: for each template in `getAllTemplates(project)`, for each connection in `template.getGraph()`, resolve source/target node info ids; if either unresolved → `logger.warn("Skipping stale template edge …")` and continue; else `idx.addEdge(fromId, toId)`. Family edges contribute the same way (successor = next, and predecessor = next in reverse for `previous`).

- [ ] **Step 4: Implement the integration test** `TemplateRelationSourceTest` (boots registry): build a real 2-node template graph from two registry node ids that have a data-type-compatible edge, add it via `JIPipe.getNodeTemplates().addToGlobal(...)`, then assert `relations(entryFor(nodeA), 6).next()` contains `nodeB` and `relations(entryFor(nodeB), 6).previous()` contains `nodeA`. Add one more assertion for **similar excludes workflow neighbors** (Review Focus #3): pick two nodes in the *same* function that are connected by a template edge; assert the target appears in `next()` but NOT in `similar()` of the source (the three lists stay disjoint).

- [ ] **Step 5: Wire `relations()` into `JIPipeNodeDatabase`** — build the index on the same triggers as `relatedCache` (registry/template refresh), expose `relations(entry, maxPerList)` returning a `NodeRelations` (map node ids → entries via `entryByOriginNodeId`, cap per list at `maxPerList`). **Remove** `followUps(entry)` and `related(entry, max)`; update the in-tree consumers (the rail + browser strip — Task 5's UI step).

- [ ] **Step 6: Update the rail + browser strip to three sections** — `NodeFinderDetailRail`: replace the Related/Follow-ups lists with **Similar / Previous steps / Next steps** (three `JList`s, distinct icons, same click-callback). `JIPipeDesktopNodeBrowser` follow-up strip: render the three groups (next first, as today, then previous, then similar) — keep the "workflow successors first" ordering.

- [ ] **Step 7: Run tests to verify they pass**

Run: `mvn -o -pl jipipe-core test -Dtest='NodeRelationIndexTest' -Dsurefire.failIfNoSpecifiedTests=false` → PASS
Run: `mvn -o -pl jipipe-launcher -am test -Dtest='TemplateRelationSourceTest' -Dsurefire.failIfNoSpecifiedTests=false` → PASS
Run: `mvn -o -pl jipipe-core test` → BUILD SUCCESS (the removed methods' old tests updated to call `relations()`).

- [ ] **Step 8: Commit**

```bash
git add -A jipipe-core/src jipipe-launcher/src
git commit -m "feat: relations generalization (Similar/Previous/Next) mined from family steps + template graphs (6.0.0) (54)"
```

---

### Task 6: Tree + list integration (synthetic Templates branch, dual placement, badges)

**Spec:** §7.2, §7.3 (tree placement + badges).

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/nodebrowser/NodeBrowserTreeController.java` (add the synthetic `Templates` top-level branch with System/Global/Project children)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/addnodepanel/NodeBrowserModel.java` (scope placement special case + de-dup in the list query)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/nodebrowser/FunctionTreeCellRenderer.java` (badge chip)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/nodetemplate/JIPipeNodeTemplateListCellRenderer.java` (badge chip in the list)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/grapheditor/addnodepanel/NodeBrowserModelTemplateTest.java` (non-Swing: model-level placement + de-dup)

**Interfaces:**
- Produces: `NodeBrowserModel` exposes (non-Swing, testable) `List<JIPipeNodeDatabaseEntry> entriesFor(TreePathOrKey key)` and a `boolean isTemplateScopeNode(Object treeNodeUserObject)`; the synthetic branch is a `TreeModel` user-object marker class `TemplateScopeTreeNode(scope)`. De-dup: the list query returns each entry once even when it matches both a function subtree and a scope group.
- Consumes: `CreateNewNodesByTemplateDatabaseEntry.getScope()` / `getFunctionIds()` / `getBadge()` (Task 4).

- [ ] **Step 1: Write the failing model test** (non-Swing):

```java
class NodeBrowserModelTemplateTest {
    @BeforeAll static void boot() { JIPipe.ensureInstance(); }
    @Test void templateAppearsUnderItsFunctionAndItsScope() {
        var model = buildModelWithTemplate(/* 1 global template fn=images */);
        assertThat(model.entriesFor("jipipe:function:images")).hasSizeIncludingTemplate();
        assertThat(model.entriesFor(TemplateScopeTreeNode.GLOBAL)).containsTheTemplate();
    }
    @Test void listDeduplicatesWhenBothVisible() {
        // a single query that would match both placements → entry appears once
        assertThat(model.allVisibleEntries()).containsExactlyOnce(theTemplate);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -pl jipipe-core test -Dtest='NodeBrowserModelTemplateTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (no synthetic branch / no scope placement).

- [ ] **Step 3: Implement the model + tree changes** — add `TemplateScopeTreeNode`; in `NodeBrowserModel`, when building the visible set, include a template entry if its `functionIds()` intersect the selected function subtree OR its scope group is selected; the list query **dedupes by entry id** (a `Set<String>` of seen ids). `NodeBrowserTreeController`: append the synthetic `Templates` branch (System/Global/Project children, each a `TemplateScopeTreeNode`) at the end of the top-levels.

- [ ] **Step 4: Add badge rendering** — `FunctionTreeCellRenderer` and `JIPipeNodeTemplateListCellRenderer` draw a small chip (`Pattern` / `Custom`) when the entry is a `CreateNewNodesByTemplateDatabaseEntry` (read `getBadge()`). Keep the template's own icon/hue.

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -o -pl jipipe-core test -Dtest='NodeBrowserModelTemplateTest' -Dsurefire.failIfNoSpecifiedTests=false` → PASS
Run: `mvn -o -pl jipipe-core test` → BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add -A jipipe-core/src
git commit -m "feat: templates in the function tree (synthetic Templates branch, dual placement, badges, list de-dup) (6.0.0) (55)"
```

---

### Task 7: Context-menu parity (1-pane bug fix)

**Spec:** §7.3 (the shared menu-builder; the compact-mode parity bug).

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/NodeEntryContextMenu.java` (shared, non-Swing spec builder + Swing assembly)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/addnodepanel/JIPipeDesktopNodeBrowser.java` (list menu + compact-mode tree leaves both delegate to it)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/NodeEntryContextMenuTest.java`

**Interfaces:**
- Produces: `NodeEntryContextMenu.specs(JIPipeNodeDatabaseEntry, Set<String> pinnedIds) -> List<MenuItemSpec>` (non-Swing, testable: returns the ordered item ids/labels), and `NodeEntryContextMenu.buildJPopupMenu(specs, actions) -> JPopupMenu`. `MenuItemSpec(String id, String label, boolean enabled)`. Item ids: `insert-at-cursor`, `pin`/`unpin`, `show-doc`, and for template entries `edit`, `copy-to-global`, `copy-to-project`, `delete`, `configure-templates` (Global/Project scope) — System-scope template entries get the read-only subset (no delete/edit).
- Consumes: `CreateNewNodesByTemplateDatabaseEntry.getScope()` (Task 4), pinned set.

- [ ] **Step 1: Write the failing test**:

```java
class NodeEntryContextMenuTest {
    @BeforeAll static void boot() { JIPipe.ensureInstance(); }
    @Test void listAndTreeProduceIdenticalSpecs() {
        // the SAME specs() method serves both surfaces → assert the same list for a node entry
        var nodeEntry = anyNodeEntry();
        assertThat(NodeEntryContextMenu.specs(nodeEntry, Set.of())).extracting(MenuItemSpec::id)
            .containsExactly("insert-at-cursor", "pin", "show-doc");
    }
    @Test void globalTemplateHasEditDeleteConfigure() {
        var t = globalTemplateEntry();
        assertThat(NodeEntryContextMenu.specs(t, Set.of())).extracting(MenuItemSpec::id)
            .contains("edit", "delete", "configure-templates");
    }
    @Test void systemTemplateIsReadOnly() {
        var t = systemTemplateEntry();
        assertThat(NodeEntryContextMenu.specs(t, Set.of())).extracting(MenuItemSpec::id)
            .doesNotContain("edit", "delete");
    }
    @Test void pinnedNodeShowsUnpinNotPin() {
        var e = anyNodeEntry();
        assertThat(NodeEntryContextMenu.specs(e, Set.of(e.getId()))).extracting(MenuItemSpec::id)
            .contains("unpin").doesNotContain("pin");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -pl jipipe-core test -Dtest='NodeEntryContextMenuTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compile failure (no `NodeEntryContextMenu`).

- [ ] **Step 3: Implement `NodeEntryContextMenu`** (the `specs()` logic is the single source of truth; `buildJPopupMenu` is a thin Swing wrapper). Refactor the **list** menu in `JIPipeDesktopNodeBrowser` to call it. Then attach the **same** `buildJPopupMenu` to the **tree leaves** in compact mode (and non-compact, for parity) — this is the bug fix: the tree leaves now get the list's menu.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -o -pl jipipe-core test -Dtest='NodeEntryContextMenuTest' -Dsurefire.failIfNoSpecifiedTests=false` → PASS
Run: `mvn -o -pl jipipe-core test` → BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A jipipe-core/src
git commit -m "fix: node tree context menu parity (shared menu builder; compact-mode tree leaves get the list menu) (6.0.0) (56)"
```

---

### Task 8: Ship the 185 patterns as System templates

**Spec:** §11.

**Files:**
- Create: `jipipe-core/src/main/resources/org/hkijena/jipipe/plugins/nodetemplate/patterns/` (copy the 185 `templates/*.json` from `utils/taxonomy-research/patterns/templates/`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/nodetemplate/NodeTemplateBoxToolPlugin.java` (or a new `SystemTemplatePlugin`) — register the 185 at init
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/plugins/nodetemplate/SystemPatternTemplatesTest.java`

**Interfaces:**
- Produces: at startup, 185 System-scope templates are registered (`JIPipe.getNodeTemplates().addFromPlugin(...)` so `isPluginTemplate` → true → scope System). Each has `taxonomyFunction` = the pattern's primary function branch (deterministic rule below), `name`/`description` from the pattern, `source = "System (corpus-mined)"`, hue derived from the function.
- Consumes: the 185 `templates/<id>.json` files; the 185 `patterns/<id>.md` frontmatter (for `taxonomyFunction` — the `tags` primary branch) — but the source of truth for the function is a **committed mapping**, not parsing markdown at runtime.

- [ ] **Step 1: Determine each pattern's primary function branch (offline, commit the result)**

Run a throwaway Python against `patterns.json` + `registry.json` to compute, for each of the 185 emitted patterns, the most common primary top-level branch among its nodes' `functions[0]` (ties → lexicographic). Emit a committed resource `patterns/functions.json` = `{ "<pattern-id>": "<function-id>", … }`. This keeps runtime free of markdown parsing and makes the mapping reviewable.

- [ ] **Step 2: Write the failing integration test**

```java
class SystemPatternTemplatesTest {
    @BeforeAll static void boot() { JIPipe.ensureInstance(); }
    @Test void all185SystemPatternsRegisteredAndDeserializable() {
        long system = JIPipe.getNodeTemplates().getAllTemplates(null).stream()
            .filter(JIPipe.getNodeTemplates()::isPluginTemplate).count();
        assertThat(system).isGreaterThanOrEqualTo(185);
        // every one deserializes (generalizes TemplateDataDeserializationTest from 1→all)
        JIPipe.getNodeTemplates().getAllTemplates(null).stream()
            .filter(JIPipe.getNodeTemplates()::isPluginTemplate)
            .forEach(t -> assertThat(t.getGraph().getGraphNodes()).isNotEmpty());
    }
    @Test void flagshipPatternMinesRelations() {
        // folder-list-files → path-to-annotation-simple is a mined next-step
        var e = entryFor("path-to-annotation-simple");
        assertThat(JIPipeNodeDatabase.getInstance().relations(e, 6).next())
            .anySatisfy(en -> assertThat(en.getName()).contains("Import image"));
    }
}
```

- [ ] **Step 3: Copy resources + implement registration** — copy the 185 JSONs into `…/nodetemplate/patterns/` (the research dir is untouched). In the plugin `register()`, for each bundled JSON: deserialize to `JIPipeNodeTemplate`, set `taxonomyFunction` from `functions.json` (via `JIPipeTaxonomyFunctionRef`), set `name`/`description`/`source`/hue, then `addFromPlugin(t)` + fire `NodeTemplatesRefreshedEvent`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -o -pl jipipe-launcher -am test -Dtest='SystemPatternTemplatesTest' -Dsurefire.failIfNoSpecifiedTests=false` → PASS
Run: `mvn -o -pl jipipe-launcher -am test -Dtest='TemplateDataDeserializationTest,TaxonomyFullValidationTest' -Dsurefire.failIfNoSpecifiedTests=false` → both still PASS (the shipped patterns' template edges feed the virtual `TemplateRelationFamily`, which is excluded from validation).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/resources/org/hkijena/jipipe/plugins/nodetemplate/patterns \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/nodetemplate \
        jipipe-launcher/src/test
git commit -m "feat: ship the 185 corpus-mined patterns as System templates (6.0.0) (57)"
```

---

### Task 9: UI consolidation — remove `NodeTemplateBox`, add "Configure templates" modal

**Spec:** §8 (UI consolidation, the modal, the gear button).

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/pipeline/JIPipeDesktopPipelineGraphEditorUI.java` (unmount `NodeTemplateBox` from the dock)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/templatemanage/TemplateManagementDialog.java` (the modal: System/Global/Project/Download tabs)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/templatemanage/TemplateManagementController.java` (non-Swing actions: add/edit/copy/delete/export/import per scope)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/nodefinder/JIPipeDesktopNodeFinderDialogUI.java` (gear button next to AI search)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/addnodepanel/JIPipeDesktopNodeBrowser.java` (control-bar gear + "Configure templates" context item)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopProjectSettingsUI.java` (remove the `node-templates` settings hook — replaced by the modal's Project tab)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/grapheditor/templatemanage/TemplateManagementControllerTest.java`

**Interfaces:**
- Produces: `TemplateManagementController` (non-Swing, testable) with `addFromSelection(scope, templates)`, `edit(scope, template, name, desc, function, icon, hue)`, `copyTo(scopeFrom, scopeTo, template)`, `delete(scope, template)`, `export(scope, templates, Path)`, `importTo(scope, Path)` — each operating on the scope's store (`JIPipeNodeTemplatesServiceComponent` for Global, project metadata for Project, read-only for System) and firing `NodeTemplatesRefreshedEvent`. `TemplateManagementDialog` is a thin Swing wrapper over it (tabs = scopes; Download tab embeds the existing `NodeTemplateDownloaderDialog`).
- Consumes: `TemplateScope` (Task 4), the service component, `NodeEntryContextMenu` (Task 7) for the "Configure templates" item.

- [ ] **Step 1: Write the failing controller test**:

```java
class TemplateManagementControllerTest {
    @BeforeAll static void boot() { JIPipe.ensureInstance(); }
    @Test void copyGlobalToProject() { /* add a global template, copyTo(Global,Project), assert in project metadata + fires event */ }
    @Test void deleteProjectTemplate() { /* add a project template, delete, assert gone + event */ }
    @Test void systemScopeIsReadOnly() { /* delete(System,…) is a no-op / throws; edit(System,…) is a no-op */ }
    @Test void exportAndImportRoundTrip() throws Exception { /* export global→file, importTo into a fresh scope, assert equal template */ }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -pl jipipe-core test -Dtest='TemplateManagementControllerTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compile failure (no controller).

- [ ] **Step 3: Implement the controller + dialog** — the controller delegates to the service component's existing methods (`addToGlobal`/`addToProject`/`removeFromGlobal`/`removeFromProject`/`editTemplate`) and, for export/import, the existing **JSON-array** format (`JsonUtils.toPrettyJsonString(List<JIPipeNodeTemplate>)` to a file; read a JSON array of `JIPipeNodeTemplate` back — mirroring `NodeTemplateBox.exportTemplates`/`importTemplates`, which the lenient `menu-path` deserializer from Task 3 keeps working). System-scope actions are no-ops (read-only). Tabs per scope; the Download tab embeds the existing `NodeTemplateDownloaderDialog`; the gear button opens the dialog; the "Configure templates" context item opens it on the entry's scope tab. Unmount `NodeTemplateBox` from the pipeline dock; remove its `openNewToolBoxWindow` mount point; remove the `node-templates` hook from project settings (the modal's Project tab replaces it).

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -o -pl jipipe-core test -Dtest='TemplateManagementControllerTest' -Dsurefire.failIfNoSpecifiedTests=false` → PASS
Run: `mvn -o -pl jipipe-core test` → BUILD SUCCESS (the removed `NodeTemplateBox`'s in-tree references compile away; its feature set is fully relocated to the controller/modal/context menu).

- [ ] **Step 5: Compile the whole desktop tree** (catches any dangling `NodeTemplateBox`/`menuPath`/`followUps` references the earlier tasks left):

Run: `mvn -o clean compile`
Expected: BUILD SUCCESS. (Fix any remaining reference here; if a dangling consumer surfaces in another module, fix it in place — it is an in-tree caller of an API this feature removed.)

- [ ] **Step 6: Commit**

```bash
git add -A jipipe-core/src
git commit -m "feat: templates management modal (System/Global/Project/Download) + remove NodeTemplateBox dock (6.0.0) (58)"
```

---

## Post-plan verification (controller runs before handoff, not a task)

1. `mvn -o -pl jipipe-core test` — full core suite green.
2. `mvn -o -pl jipipe-launcher -am test` — full launcher suite green (incl. `TaxonomyFullValidationTest`, `TemplateDataDeserializationTest`, `SystemPatternTemplatesTest`, `TemplatesFunctionTest`, `RegistryDumperTest`).
3. Confirm `registry.json` (via `RegistryDumperTest`) shows `jipipe:function:templates` in `functions[]` and does **not** show the virtual `TemplateRelationFamily` in `families[]`.
4. Whole-branch review (subagent-driven) over the feature commits.
