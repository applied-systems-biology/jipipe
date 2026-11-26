# Taxonomy Graph-Branch Split (1403) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix work item 1403 (comment nodes cannot be deleted) by splitting the Graph taxonomy branch: user-facing debug/meta-run nodes go to `DebuggingFunction`/`DataUtilitiesFunction`, internally used graph structural nodes get a new hidden leaf `GraphStructuralFunction`, and `GraphDebugFunction` ceases (deprecated, unregistered).

**Architecture:** Node behavior flags (visibility in add-nodes/find panels, extractability, hence deletability) are read from a node's primary taxonomy function via `TaxonomyView.behaviorFlags(info)`. The fix is a pure re-assignment: 11 `@ConfigureJIPipeNode(function = ...)` annotation swaps, one new leaf class, one deprecated leaf class, registration swap, and lockstep test updates. No node registration IDs change.

**Tech Stack:** Java 21, Maven multi-module, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-26-taxonomy-graph-structure-split-design.md` (user rulings recorded there; the disposition table is normative).

## Global Constraints

- No em dashes or en dashes anywhere (AGENTS.md): hyphens/colons only.
- Stable-ID rule: the `jipipe:function:graph-debug` literal stays verbatim inside the deprecated `GraphDebugFunction` class; the new leaf pins `jipipe:function:graph-structural`. No other `ID =` line changes.
- `TaxonomyConversionReportTest` is system-property-gated and has no committed report files: nothing to regenerate.
- Test policy (AGENTS.md): no tests for getters/trivial structure; the disposition test is a registry/ship-set contract pin (the 1403 regression), which is allowed.
- Branch: `1403-comment-node-deletion` (already created).

## Review Focus

- A user of the Compartments tab: structural nodes (`JIPipeProjectCompartmentOutput`, wrapper plugs) must stay invisible in the compartments add-nodes panel (filtered by `isVisibleInCompartments`), and the compartment output must still be creatable programmatically (`JIPipeProject.createNode`, not the panel). Pinned by GraphBranchDispositionTest + grep gate in Task 3.
- A user who saved a 5.x/6.0.0-dev project containing a comment node: project must load and the node must now be deletable. Pinned by the disposition test (annotation-derived flags) + TaxonomyFullValidationTest.
- A user browsing Misc > Debugging: Wait/LogHammer must be joined by Comment, slot-folder helpers, Crasher, RunJIPipeProject; none of them may inherit hidden flags. Pinned by GraphBranchDispositionTest.
- A developer referencing `GraphDebugFunction` from third-party code: the class must still compile with its frozen ID (deprecated, unregistered). Pinned by FunctionLeavesTest removal + compile.
- The search index: tokens come from names/descriptions; `TokenTaxonomyTest` uses only self-registered fixtures, so no production token pins move. Verified by running it in Task 3.

---

### Task 1: New `GraphStructuralFunction` leaf, deprecate `GraphDebugFunction`, registration swap

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions/graph/GraphStructuralFunction.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions/graph/GraphDebugFunction.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/taxonomy/TaxonomyPlugin.java` (graph section, ~line 323-325)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/functions/FunctionLeavesTest.java` (~line 197-200)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/TaxonomyPluginTest.java` (~line 95)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/InternalFunctionFlagsTest.java` (~lines 16-18, 48-52)

**Interfaces:**
- Produces: `GraphStructuralFunction` with `public static final String ID = "jipipe:function:graph-structural"`, `getName()` = "Structure", `getParent()` = `new GraphFunction()`, `getDescription()` = "Internally used graph structural nodes", flags `isVisibleInPipeline()`/`isVisibleInCompartments()`/`canExtract()` all `false`. Later tasks assign nodes to it by class reference.
- Produces: `GraphDebugFunction` becomes `@Deprecated`, unregistered, ID literal unchanged.

- [ ] **Step 1: Update the three test files to the target state** (write them as if the change existed)
  - `FunctionLeavesTest`: in the `// graph` section of `leafLiterals()`, remove `literals.put(GraphDebugFunction.class, "jipipe:function:graph-debug");` and add `literals.put(GraphStructuralFunction.class, "jipipe:function:graph-structural");` (same position, keep the `// graph` comment); swap the import.
  - `TaxonomyPluginTest`: in `EXPECTED_FUNCTION_IDS`, replace `"jipipe:function:graph-debug"` with `"jipipe:function:graph-structural"` (keep list order: after `graph-compartment-outputs`).
  - `InternalFunctionFlagsTest`: in the internal-leaves array, replace `GraphDebugFunction.class` with `GraphStructuralFunction.class` (import swap); adjust the class javadoc sentence naming the trio members (now: GraphStructural, GraphWrappers, GraphCompartmentOutputs) and drop the "Task 21b regression background" wording that names GraphDebugFunction as a member if it confuses; keep the test methods as-is.

- [ ] **Step 2: Run the three tests, verify failure**

Run: `mvn test -pl jipipe-core -Dtest='FunctionLeavesTest,TaxonomyPluginTest,InternalFunctionFlagsTest' -DfailIfNoTests=false`
Expected: FAIL (compilation error: `GraphStructuralFunction` does not exist; `TaxonomyPluginTest` fails on the missing registered id).

- [ ] **Step 3: Implement the leaf and deprecation**
  - Create `GraphStructuralFunction.java` modeled line-for-line on `GraphDebugFunction.java` (same license header, same structure) with the constants from the Interfaces block; javadoc: "Seed leaf of the Graph / Project branch: internally used graph structural nodes (loop scaffold, wrapper plugs, compartment outputs). Hidden in the node browsers and not user-extractable, mirroring the legacy InternalNodeTypeCategory flags." Include the canExtract() javadoc explaining the internal trio, referencing GraphStructuralFunction's members.
  - `GraphDebugFunction.java`: add `@Deprecated` on the class, javadoc: "Deprecated since the 1403 split: this leaf mixed user-facing debug nodes with internally used graph structural nodes. User-facing members moved to DebuggingFunction, structural members to GraphStructuralFunction. No longer registered by TaxonomyPlugin; the ID literal is frozen because it is persisted in node database entries and UI state." Do not touch the ID, flags, or any other member.
  - `TaxonomyPlugin.java`: replace `new GraphDebugFunction(),` with `new GraphStructuralFunction(),` in the `// graph` section (keep list position) and swap the import.

- [ ] **Step 4: Run the three tests, verify pass**

Run: `mvn test -pl jipipe-core -Dtest='FunctionLeavesTest,TaxonomyPluginTest,InternalFunctionFlagsTest' -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A jipipe-core docs/superpowers/specs/2026-11-26-taxonomy-graph-structure-split-design.md
git commit -m "feat: add Graph>Structure taxonomy leaf, deprecate Graph>Debugging (1403, 6.0.0)"
```

---

### Task 2: Reassign the 11 nodes, add the disposition contract test

**Files:**
- Modify (annotation `function = ...` + import swap only, nothing else):
  - `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/core/nodes/JIPipeCommentNode.java:34` -> `DebuggingFunction.class`
  - `plugins/jipipe-plugin-utils/src/main/java/org/hkijena/jipipe/plugins/utils/algorithms/meta/GetJIPipeSlotFolderAlgorithm.java:60` -> `DebuggingFunction.class`
  - `plugins/jipipe-plugin-utils/src/main/java/org/hkijena/jipipe/plugins/utils/algorithms/ImportJIPipeSlotFolderAlgorithm.java:50` -> `DebuggingFunction.class`
  - `plugins/jipipe-plugin-utils/src/main/java/org/hkijena/jipipe/plugins/utils/algorithms/iterationsteps/CrasherAlgorithm.java:34` -> `DebuggingFunction.class`
  - `plugins/jipipe-plugin-utils/src/main/java/org/hkijena/jipipe/plugins/utils/algorithms/meta/RunJIPipeProjectAlgorithm.java:56` -> `DebuggingFunction.class`
  - `jipipe-core/src/main/java/org/hkijena/jipipe/api/looping/LoopStartNode.java:47` -> `GraphStructuralFunction.class`
  - `jipipe-core/src/main/java/org/hkijena/jipipe/api/looping/LoopEndNode.java:33` -> `GraphStructuralFunction.class`
  - `jipipe-core/src/main/java/org/hkijena/jipipe/api/grouping/GraphWrapperAlgorithmInput.java` -> `GraphStructuralFunction.class`
  - `jipipe-core/src/main/java/org/hkijena/jipipe/api/grouping/GraphWrapperAlgorithmOutput.java` -> `GraphStructuralFunction.class`
  - `jipipe-core/src/main/java/org/hkijena/jipipe/api/compartments/algorithms/JIPipeProjectCompartmentOutput.java:31` -> `GraphStructuralFunction.class`
  - `jipipe-core/src/main/java/org/hkijena/jipipe/api/compartments/algorithms/IOInterfaceAlgorithm.java:33` -> `DataUtilitiesFunction.class`
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/nodes/taxonomy/GraphBranchDispositionTest.java`
- Create: `plugins/jipipe-plugin-utils/src/test/java/org/hkijena/jipipe/plugins/utils/GraphBranchDispositionUtilsPluginTest.java`
- Modify (fixture import/class swap only): `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ParameterIntrospectionOperationsTest.java` (~lines 583, 630), `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperationsTest.java` (~line 515)

**Interfaces:**
- Consumes: `GraphStructuralFunction`, `DebuggingFunction`, `DataUtilitiesFunction` from Task 1 (existing classes).
- Produces: `GraphBranchDispositionTest` (core) pinning, via `Class.getAnnotation(ConfigureJIPipeNode.class).function()[0]`: JIPipeCommentNode, LoopStartNode, LoopEndNode, GraphWrapperAlgorithmInput, GraphWrapperAlgorithmOutput, JIPipeProjectCompartmentOutput, IOInterfaceAlgorithm -> their new functions; plus one regression assertion: the comment node's assigned function instance has `canExtract() == true` (the 1403 contract). The plugin-utils test pins the same for GetJIPipeSlotFolderAlgorithm, ImportJIPipeSlotFolderAlgorithm, CrasherAlgorithm, RunJIPipeProjectAlgorithm. Pure reflection; no JIPipe context, following the `ConfigureJIPipeNodeAnnotationTest` precedent.

- [ ] **Step 1: Write both disposition tests (failing)**

Core test: one `@Test` per destination leaf grouping the node classes, each asserting `getAnnotation(ConfigureJIPipeNode.class).function()[0]` equals the expected function class (exact expected pairs are the spec's disposition table), plus the `canExtract()` regression assertion on the comment node's function instance. Plugin-utils test: same pattern for its four node classes (package `org.hkijena.jipipe.plugins.utils`).

- [ ] **Step 2: Run them, verify failure**

Run: `mvn test -pl jipipe-core -Dtest=GraphBranchDispositionTest -DfailIfNoTests=false`
Expected: FAIL (comment node still on GraphDebugFunction).

- [ ] **Step 3: Apply the 11 annotation swaps and the 2 fixture swaps**

Only the `@ConfigureJIPipeNode(function = ...)` line and the corresponding import change per file. Do not reorder members, do not reformat.

- [ ] **Step 4: Run the new tests and the touched fixture tests, verify pass**

Run: `mvn test -pl jipipe-core -Dtest='GraphBranchDispositionTest,ParameterIntrospectionOperationsTest,PipelineMapOperationsTest' -DfailIfNoTests=false`
Run: `mvn test -pl plugins/jipipe-plugin-utils -am -Dtest=GraphBranchDispositionUtilsPluginTest -DfailIfNoTests=false`
Expected: PASS (fixtures on `GraphStructuralFunction` keep identical flags, so their assertions hold unchanged).

- [ ] **Step 5: Commit**

```bash
git add -A jipipe-core plugins/jipipe-plugin-utils
git commit -m "feat: reassign graph branch nodes to Structure/Debugging/Data utilities leaves (1403, 6.0.0)"
```

---

### Task 3: Description, docs, full verification

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/taxonomy/functions/DebuggingFunction.java:50` (description only)
- Modify: `utils/taxonomy-conversion/conversion-map.md` (rule 10 line, table rows, distribution counts, Flag (e) paragraphs)

**Interfaces:**
- Produces: `DebuggingFunction.getDescription()` = "Debugging and meta-run helper nodes" (exact value; feeds no token test).

- [ ] **Step 1: Update `DebuggingFunction` description** to the exact value above (name, ID, flags untouched).
- [ ] **Step 2: Amend `conversion-map.md`**: add a short "2026-11-26 amendment (work item 1403)" section after the mapping rules recording the split (GraphDebugFunction deprecated; new GraphStructuralFunction; the 11 node moves), and update the rows/counts that name GraphDebugFunction members (lines ~28, 160-168, 1121-1124, 1239-1251) so the record matches reality. Historical wording elsewhere stays.
- [ ] **Step 3: Grep gates**

Run: `grep -rn "GraphDebugFunction" --include=*.java . | grep -v /target/ | grep -v "GraphDebugFunction.java"`
Expected: no output (only the deprecated class file itself references the name).
Run: `grep -rn "jipipe:function:graph-debug" --include=*.java . | grep -v /target/`
Expected: only `GraphDebugFunction.java`.

- [ ] **Step 4: Module builds + full taxonomy test sweep**

Run: `mvn compile -pl jipipe-core,plugins/jipipe-plugin-utils -am`
Run: `mvn test -pl jipipe-core -Dtest='FunctionLeavesTest,TaxonomyPluginTest,InternalFunctionFlagsTest,GraphBranchDispositionTest,TaxonomyViewTest,TaxonomyHueTest,TopLevelBranchesTest,TaxonomyAutoAssignmentTest,FamilyAssemblerTest,FunctionRegistryTest,TokenTaxonomyTest,NodeDatabaseTaxonomyTest,JIPipeJavaNodeInfoTaxonomyTest' -DfailIfNoTests=false`
Run: `mvn test -pl jipipe-launcher -am -Dtest=TaxonomyFullValidationTest -DfailIfNoTests=false`
Expected: PASS everywhere (TaxonomyFullValidationTest runs the production validation over the entire registry: every node still has a primary function).

- [ ] **Step 5: Commit and push, open MR**

```bash
git add -A
git commit -m "docs: record 1403 graph branch split in conversion map, widen Debugging description (6.0.0)"
git push -u origin 1403-comment-node-deletion
```

Open MR via GitLab API (project 131, PRIVATE-TOKEN from ~/.git-credentials; glab is broken on this machine). Title: "Resolve 1403: comment nodes cannot be deleted (taxonomy graph branch split)". Description: root cause paragraph, disposition table, leaf changes, test list; mention `Closes #1403` linking via work item URL.

---

## Deferred minors (carry, do not fix here)

- `GraphCompartmentOutputsFunction` stays registered but empty (browser trees derive from node membership, so it renders nowhere; recorded in the amendment). If a later sweep wants, it can follow the GraphDebugFunction deprecation pattern.
