# KDB Revival Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Revive the JIPipe knowledge database as a structured, layer-verified corpus with registry-sourced node facts, a three-lane retrieval service, and read-only MCP operations - starting with a node-registry successor API that also fixes the user-facing "what do I use instead?" complaint.

**Architecture:** Phase 1 is a registry-only change (`@JIPipeNodeSuccessor` annotation read into `JIPipeNodeInfo`, consumed by node documentation and the KDB renderer). Phase 2 adds the structured corpus (YAML entries, kinds, slot schemas as code, `KnowledgeService` with lexical/vector/relation lanes fused by RRF). Phase 3 distills more domains. Phase 4 is harness handoff (out of scope for this plan; the plan covers phases 1-2, with phase 2 structured so phase 3 is content-only).

**Tech Stack:** Java 21, Maven, Jackson (`jackson-dataformat-yaml`), the existing JEMB embedding database and `JIPipeAIServiceComponent.embedNow`, the MCP automation operation layer, JUnit 5 in `jipipe-core` and `jipipe-launcher`.

**Spec:** `docs/superpowers/specs/2026-11-20-kdb-revival-design.md`

## Global Constraints

- No em dashes (U+2014) or en dashes (U+2013) in code, Javadoc, comments, or user-facing strings.
- Registered ids are stable string literals. Never derive an id from a class name. The new annotation's attribute is a node id literal.
- Schema parameters are kebab-case where they are node parameters; automation-operation ids are stable snake_case literals.
- No Swing interaction tests. Pure-logic helpers and registry contracts get tests; UI rendering is verified by eye by the user.
- Never assume text sizes: any UI text that must enclose or align with content is measured from the component or `FontMetrics`.
- Registry-aware tests assert presence of expected ids, never exact registry counts.
- Commits: `type(#1383): subject`, explicit `git add`, no push unless asked.
- ImageJ legacy patcher listener stays intact; do not call `LegacyInjector.preinit()` yourself.
- The corpus is one owner (this repo) so a single-source test may pin counts; coverage is a warning, never a gate.
- Golden relevance test is golden on lexical and relation lanes only; the vector lane is pinned by a stubbed-embedding wiring test.

## Review Focus

The failure modes most likely to bite a real user, each pinned by a test in the task that owns the code:

1. **A node is marked deprecated without a successor, or a successor id does not resolve.** A user opens the docs and gets "Deprecated" with no guidance, or a stale pointer to a node that no longer exists. Task 1's `NodeSuccessorRegistryTest` fails on both.
2. **A corpus entry references a node id that was removed from the registry.** The KDB serves advice about a node that does not exist. Task 6's `KdbCorpusIntegrityTest` fails with the registry's own message.
3. **A search for an exact node id or error string returns nothing** (the oikb failure): lexical lane must match node ids and aliases verbatim. Task 5's lexical lane test pins verbatim id matching.
4. **An entry key drifts from its folder path** (a rename leaves a dangling ENTRY_REF). Task 6's integrity test pins entryKey == path-derived key.
5. **The renderer emits a deprecated node without its successor line** (the drift-kill silently regresses). Task 7's render test pins the "use X instead" line for a deprecated node with a successor.
6. **An unreadable or malformed YAML entry crashes the service at first search** instead of failing the build. Task 3's loader test pins a clean finding, not an exception.

---

### Task 1: Registry-side successor API

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/JIPipeNodeSuccessor.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/JIPipeNodeInfo.java` (add two defaults after `isDeprecated()`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/infos/JIPipeJavaNodeInfo.java` (read annotation in constructor near the `@Deprecated` read)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/infos/JIPipeMutableNodeInfo.java` (fields + setters near `setDeprecated`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/looping/LoopStartNode.java`, `LoopEndNode.java` (annotate explicitly as successor-less)
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/NodeSuccessorRegistryTest.java`

**Interfaces:**
- Produces: `@JIPipeNodeSuccessor(value = {"<node-id>", ...}, reason = "...")`; `JIPipeNodeInfo.getSuccessorNodeIds() : List<String>` (default empty, immutable); `JIPipeNodeInfo.getSuccessorReason() : String` (default null); `JIPipeMutableNodeInfo.setSuccessorNodeIds(List<String>)`, `setSuccessorReason(String)`.

- [ ] **Step 1: Write the failing test**

```java
package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NodeSuccessorRegistryTest {

    @BeforeAll
    static void init() {
        JIPipe.ensureInstance();
    }

    @Test
    void everyDeclaredSuccessorResolves() {
        // Presence, not exact counts: plugins may add nodes.
        Map<String, JIPipeNodeInfo> infos = JIPipe.getNodes().getRegisteredNodeInfos();
        for (JIPipeNodeInfo info : infos.values()) {
            for (String successorId : info.getSuccessorNodeIds()) {
                assertTrue(infos.containsKey(successorId),
                        "Node '" + info.getId() + "' declares successor '" + successorId
                                + "' which is not registered");
            }
        }
    }

    @Test
    void successorImpliesDeprecatedAndNeverSelfReferences() {
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            if (!info.getSuccessorNodeIds().isEmpty()) {
                assertTrue(info.isDeprecated(),
                        "Node '" + info.getId() + "' declares a successor but is not deprecated");
                assertFalse(info.getSuccessorNodeIds().contains(info.getId()),
                        "Node '" + info.getId() + "' lists itself as successor");
            }
        }
    }

    @Test
    void loopHelpersAreAnnotatedExplicitly() {
        // The known deprecated node classes carry the annotation with an empty
        // successor list, so the "deprecated but no successor" state is explicit
        // rather than defaulted.
        JIPipeNodeInfo loopStart = JIPipe.getNodes().getRegisteredNodeInfos().get("jipipe:loop-start");
        if (loopStart != null) {
            assertNotNull(loopStart.getSuccessorReason(),
                    "LoopStartNode must carry an explicit successor note");
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-launcher -Dtest=NodeSuccessorRegistryTest`
Expected: compile FAIL (`getSuccessorNodeIds` does not exist).

- [ ] **Step 3: Create the annotation**

`JIPipeNodeSuccessor.java`, `@Retention(RUNTIME) @Target(TYPE)` - Javadoc states: node ids (stable string literals) of preferred replacements, ordered; empty when the node has no replacement (state the reason in `reason`); the importing class must also be `@Deprecated`; unknown ids fail the launcher gate.

```java
public @interface JIPipeNodeSuccessor {
    String[] value() default {};
    String reason() default "";
}
```

- [ ] **Step 4: Add the `JIPipeNodeInfo` defaults**

After `isDeprecated()`:

```java
default List<String> getSuccessorNodeIds() {
    return List.of();
}

default String getSuccessorReason() {
    return null;
}
```

- [ ] **Step 5: Read the annotation in `JIPipeJavaNodeInfo`**

Add a static helper mirroring `getFrameworkOf` style and call it after the `@Deprecated` read:

```java
private static List<String> getSuccessorIdsOf(Class<?> nodeClass) {
    JIPipeNodeSuccessor successor = nodeClass.getAnnotation(JIPipeNodeSuccessor.class);
    return successor == null ? List.of() : List.of(successor.value());
}

private static String getSuccessorReasonOf(Class<?> nodeClass) {
    JIPipeNodeSuccessor successor = nodeClass.getAnnotation(JIPipeNodeSuccessor.class);
    String reason = successor == null ? null : successor.reason();
    return (reason == null || reason.isBlank()) ? null : reason;
}
```

and in the constructor: `setSuccessorNodeIds(getSuccessorIdsOf(nodeClass)); setSuccessorReason(getSuccessorReasonOf(nodeClass));`

- [ ] **Step 6: Add the mutable fields and setters**

In `JIPipeMutableNodeInfo`, next to `deprecated`/`unstable`:

```java
private List<String> successorNodeIds = new ArrayList<>();
private String successorReason = null;

@Override
public List<String> getSuccessorNodeIds() {
    return List.copyOf(successorNodeIds);
}

public void setSuccessorNodeIds(List<String> ids) {
    this.successorNodeIds = new ArrayList<>(ids == null ? List.of() : ids);
}

@Override
public String getSuccessorReason() {
    return successorReason;
}

public void setSuccessorReason(String reason) {
    this.successorReason = (reason == null || reason.isBlank()) ? null : reason;
}
```

- [ ] **Step 7: Annotate the existing deprecated node classes**

On `LoopStartNode` and `LoopEndNode` (both already `@Deprecated`):

```java
@JIPipeNodeSuccessor(reason = "Internal loop helper, created automatically by the loop wrapper; use the loop nodes instead")
```

- [ ] **Step 8: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-launcher -Dtest=NodeSuccessorRegistryTest`
Expected: PASS (3 tests). Then `mvn -q test -pl jipipe-core -Dtest='*NodeInfo*'` for regressions.

- [ ] **Step 9: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/JIPipeNodeSuccessor.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/JIPipeNodeInfo.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/infos/JIPipeJavaNodeInfo.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/infos/JIPipeMutableNodeInfo.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/looping/LoopStartNode.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/looping/LoopEndNode.java \
  jipipe-launcher/src/test/java/org/hkijena/jipipe/api/nodes/NodeSuccessorRegistryTest.java
git commit -m "feat(#1383): add the node-registry successor API"
```

---

### Task 2: "Use that instead" in node documentation

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/documentation/JIPipeDesktopAlgorithmCompendiumUI.java` (inside `buildAlgorithmDocumentation(JIPipeNodeInfo)`, right after the `# <name>` line)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/documentation/NodeDocumentationSuccessorTest.java`

**Interfaces:**
- Consumes: `JIPipeNodeInfo.getSuccessorNodeIds()`, `getSuccessorReason()`, `isDeprecated()` (Task 1); `JIPipe.getNodes().getRegisteredNodeInfos()`.
- Produces: `JIPipeDesktopAlgorithmCompendiumUI.buildAlgorithmDocumentation(JIPipeNodeInfo)` now emits, for a deprecated node with successors, a paragraph `**Deprecated.** Use <successor display names with ids> instead. <reason>`; for a deprecated node without successors and with a reason, `**Deprecated.** <reason>`; nothing extra otherwise. (Signature unchanged.) The line is emitted as markdown through the file's existing `MarkdownText` import (`org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText`).

- [ ] **Step 1: Write the failing test**

Use `JIPipeJavaNodeInfo(id, SomeRegisteredNodeClass)` to get a concrete info, then set the successor fields; the second test borrows a real registered id as the successor target.

```java
package org.hkijena.jipipe.desktop.app.documentation;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NodeDocumentationSuccessorTest {

    @BeforeAll
    static void init() {
        JIPipe.ensureInstance();
    }

    @Test
    void deprecatedNodeWithSuccessorRendersUseThatInstead() {
        List<String> ids = List.copyOf(JIPipe.getNodes().getRegisteredNodeInfos().keySet());
        String targetId = ids.get(0);
        String targetName = JIPipe.getNodes().getRegisteredNodeInfos().get(targetId).getName();

        JIPipeJavaNodeInfo source = new JIPipeJavaNodeInfo("kdb-test:deprecated-with-successor",
                org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode.class);
        source.setDeprecated(true);
        source.setSuccessorNodeIds(List.of(targetId));
        source.setSuccessorReason("Test reason");

        String text = JIPipeDesktopAlgorithmCompendiumUI.buildAlgorithmDocumentation(source).getBody();
        assertTrue(text.contains("**Deprecated.**"), "missing the deprecated marker: " + text);
        assertTrue(text.contains(targetName), "missing the successor display name: " + text);
        assertTrue(text.contains("Test reason"), "missing the successor reason: " + text);
    }

    @Test
    void deprecatedNodeWithoutSuccessorRendersOnlyTheReason() {
        JIPipeJavaNodeInfo source = new JIPipeJavaNodeInfo("kdb-test:deprecated-no-successor",
                org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode.class);
        source.setDeprecated(true);
        source.setSuccessorReason("Internal helper");

        String text = JIPipeDesktopAlgorithmCompendiumUI.buildAlgorithmDocumentation(source).getBody();
        assertTrue(text.contains("**Deprecated.**"), "missing the deprecated marker: " + text);
        assertTrue(text.contains("Internal helper"), "missing the reason: " + text);
        assertFalse(text.contains("instead"), "must not claim a successor exists: " + text);
    }
}
```

Confirm the node class used (`JIPipeCommentNode` or any registered simple node) and `JIPipeJavaNodeInfo`'s second constructor argument while writing this test; use the real class, do not invent one.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=NodeDocumentationSuccessorTest`
Expected: FAIL (no `**Deprecated.**` text).

- [ ] **Step 3: Implement the documentation line**

In `buildAlgorithmDocumentation`, after the title is appended, build the paragraph from `info.isDeprecated()`, `info.getSuccessorNodeIds()`, `info.getSuccessorReason()`, resolving each successor id through `JIPipe.getNodes().getRegisteredNodeInfos()` for its display name (fall back to the raw id if it does not resolve; the launcher gate covers the real registry, this is a display guard). Plain markdown with hyphens only.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=NodeDocumentationSuccessorTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/documentation/JIPipeDesktopAlgorithmCompendiumUI.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/documentation/NodeDocumentationSuccessorTest.java
git commit -m "feat(#1383): show 'use that instead' in node documentation"
```

---

### Task 3: KDB entry model and YAML loader

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KdbEntryKind.java` (closed enum with slot schemas)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KdbSlotDef.java`, `KdbValueType.java`, `KdbCardinality.java`, `KdbDomain.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KdbEntry.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KdbFinding.java` (+ `KdbFindingCode` closed enum)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KdbEntryLoader.java`
- Modify: `jipipe-core/pom.xml` (explicit `jackson-dataformat-yaml` dependency at the version already resolved)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/knowledge/KdbEntryLoaderTest.java`, fixtures under `jipipe-core/src/test/resources/kdb-fixtures/`

**Interfaces:**
- Produces: `KdbDomain` enum (11 ids per spec 5.1); `KdbEntryKind` enum (`concept`, `pattern`, `mistake`, `decision-tree`, `node-note`) with `getSlots() : List<KdbSlotDef>` and `getDirectoryName() : String` (`concepts`, `patterns`, `mistakes`, `decision-trees`, `node-notes`); record `KdbSlotDef(String name, KdbValueType type, KdbCardinality cardinality)`; `KdbEntry` with `getEntryKey()`, `getKind()`, `getDomain()`, `getTitle()`, `getSlotValues() : Map<String,String[]>`-style accessor set, `getBody()`, `getAliases()`, `getSourceRefs()`, `getSinceVersion()`, `getUntilVersion()`, `getProperties()`; `KdbEntryLoader.loadAll(Path root) : KdbLoadResult` where `KdbLoadResult(List<KdbEntry> entries, List<KdbFinding> findings)`; `KdbFinding(KdbFindingCode code, String entryKey, String message)`.

- [ ] **Step 1: Write the failing test**

```java
class KdbEntryLoaderTest {

    @Test
    void loadsAWellFormedEntryWithDerivedDomainAndKey() { /* load kdb-fixtures/good/, assert one entry,
        entryKey == "bioimage-analysis/patterns/pilot", domain == BioimageAnalysis */ }

    @Test
    void rejectsAnUnknownSlotAsAStructuralFindingNotAnException() { /* kdb-fixtures/unknown-slot/,
        assert no exception, findings contains KdbFindingCode.UNKNOWN_SLOT with the slot name */ }

    @Test
    void rejectsAKindDirectoryMismatch() { /* a pattern file under concepts/ asserts KIND_PATH_MISMATCH */ }

    @Test
    void reportsACardinalityViolation() { /* two values for a SINGLE slot -> CARDINALITY_VIOLATION */ }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=KdbEntryLoaderTest`
Expected: compile FAIL.

- [ ] **Step 3: Implement the model**

`KdbDomain` enum with the 11 stable ids from the spec. `KdbEntryKind` with the exact slot lists from spec 5.1 (`domain` is NOT a slot; it is derived from the path). `KdbFindingCode` closed enum: `UNKNOWN_KIND`, `UNKNOWN_SLOT`, `TYPE_MISMATCH`, `CARDINALITY_VIOLATION`, `KIND_PATH_MISMATCH`, `MISSING_TITLE`, `DUPLICATE_ENTRY_KEY`, `UNRESOLVED_NODE_REF`, `UNRESOLVED_ENTRY_REF`, `PROPERTY_SHADOWS_SLOT`, `EMPTY_CORPUS`. Severity map: structural = ERROR (the first eight), the rest WARNING. `KdbEntry` immutable value type.

- [ ] **Step 4: Implement the loader**

Walk `root` for `*.yaml`, derive domain from the first path segment and kind from the second (rejecting unknown), parse with `new ObjectMapper(new YAMLFactory())` into a tree, then reconstruct through the kind's schema: each value is narrowed to its declared type; a value that cannot be narrowed produces a finding, never an exception. Entry key is `<domain>/<kind-directory>/<file-name-without-.yaml>`.

- [ ] **Step 5: Add the explicit pom dependency**

```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
```

(version managed by the parent, as it already resolves transitively).

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=KdbEntryLoaderTest`
Expected: PASS (4 tests).

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/ \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/knowledge/KdbEntryLoaderTest.java \
  jipipe-core/src/test/resources/kdb-fixtures/ jipipe-core/pom.xml
git commit -m "feat(#1383): add the KDB entry model and YAML loader"
```

---

### Task 4: Corpus integrity gate and finding-code coverage

**Files:**
- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/knowledge/KdbCorpusIntegrityTest.java`
- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/knowledge/KdbFindingCodesTest.java`
- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/knowledge/KdbCoverageTest.java`
- Create: `docs/superpowers/kdb-finding-codes.md` (the severity table the coverage test reads)
- Test: the three files above.

**Interfaces:**
- Consumes: `KdbEntryLoader.loadAll` (Task 3), `JIPipe.getNodes().getRegisteredNodeInfos()` (Task 1 patterns), `KdbFindingCode.values()`.

- [ ] **Step 1: Write the failing coverage test**

```java
class KdbFindingCodesTest {
    @Test
    void everyFindingCodeIsNamedInTheDocsTable() throws Exception {
        String doc = Files.readString(Path.of("..", "docs", "superpowers", "kdb-finding-codes.md"));
        for (KdbFindingCode code : KdbFindingCode.values()) {
            assertTrue(doc.contains("`" + code.name() + "`"),
                    "Finding code " + code + " is not documented in docs/superpowers/kdb-finding-codes.md");
        }
    }
}
```

- [ ] **Step 2: Write the failing integrity test**

```java
class KdbCorpusIntegrityTest {
    @Test
    void productionCorpusHasNoStructuralFindings() { /* load the packaged corpus root; assert no ERROR findings */ }
    @Test
    void everyNodeRefResolvesOrTheEntryIsFlagged() { /* explicit, with the registry's message included */ }
    @Test
    void everyEntryRefResolves() { /* ... */ }
    @Test
    void entryKeysMatchTheirPaths() { /* ... */ }
}
```

- [ ] **Step 2b: Write the coverage test (a WARN gate, never a fail)**

```java
class KdbCoverageTest {
    @Test
    void reportsUncoveredTaxonomyLeaves() {
        // Enumerate the 48 taxonomy leaves from the live registry and the corpus's
        // referenced node ids; print the uncovered leaves. This test only asserts the
        // report is computable (no exception) and prints its size; coverage is NOT a
        // gate. It makes the warning visible in CI output without failing the build.
        assertDoesNotThrow(() -> { /* build the report, System.out.println it */ });
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-launcher -Dtest='Kdb*Test'`
Expected: FAIL (docs file missing; corpus root empty).

- [ ] **Step 4: Write the findings doc and the corpus root**

Create `docs/superpowers/kdb-finding-codes.md` - one row per `KdbFindingCode` with severity and meaning. Ensure `jipipe-core/src/main/resources/org/hkijena/jipipe/ai/kdb/entries/` exists with at least a `.gitkeep`; `EMPTY_CORPUS` is a WARNING (the gate stays green on an empty corpus until Task 9 fills it).

- [ ] **Step 4b: Pin the two Review Focus items that belong to this gate**

Add to `KdbEntryLoaderTest` (Task 3 file, extended here) two cases named for the failure they prevent: `malformedYamlProducesAFindingNotAnException` (a syntactically broken `.yaml` fixture loads as a `TYPE_MISMATCH` or `UNKNOWN_KIND` finding with a file path in the message, never a thrown exception) and `entryKeyInTheFileIsIgnoredInFavourOfThePath` (an entry whose body claims a different key still takes its path-derived key; a duplicate path-derived key is `DUPLICATE_ENTRY_KEY`).

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-launcher -Dtest='Kdb*Test'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add jipipe-launcher/src/test/java/org/hkijena/jipipe/api/knowledge/ \
  docs/superpowers/kdb-finding-codes.md \
  jipipe-core/src/main/resources/org/hkijena/jipipe/ai/kdb/entries/
git commit -m "test(#1383): add the KDB corpus integrity and finding-code gates"
```

---

### Task 5: Lexical and relation lanes

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KdbLexicalIndex.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KdbEntryGraph.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KnowledgeService.java` (search skeleton returning lexical+relation only)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/knowledge/KdbLexicalIndexTest.java`, `KdbEntryGraphTest.java`

**Interfaces:**
- Produces: `KdbLexicalIndex.build(List<KdbEntry>)`, `search(String query, int topK) : List<ScoredEntry>`; `KdbEntryGraph(List<KdbEntry>)`, `neighbors(String entryKey) : List<RelatedEntry>`; `KnowledgeService.searchKnowledge(String query, String domainFilter, String kindFilter, int limit) : List<KnowledgeResult>`; `KnowledgeResult(String entryKey, double score, Set<String> lanes)`.

- [ ] **Step 1: Write the failing lexical test**

```java
@Test
void matchesAnExactNodeIdVerbatim() {
    // fixture entry has nodeChain ["ij3-cellpose-import"]
    assertTrue(index.search("ij3-cellpose-import", 5).stream()
            .anyMatch(s -> s.entryKey().equals("bioimage-analysis/patterns/cellpose-segmentation")));
}

@Test
void matchesAnAliasAndIsCaseInsensitive() { /* alias "Cellpose" matches "cellpose" */ }
@Test
void doesNotMatchUnrelatedQueries() { /* empty or below-threshold result */ }
```

- [ ] **Step 2: Write the failing relation test**

```java
@Test
void oneHopTraversalIsCycleGuardedAndLabeled() { /* A elaborates B, B relatedConcepts A; neighbors(A) contains B once, with label */ }
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='KdbLexicalIndexTest,KdbEntryGraphTest'`
Expected: compile FAIL.

- [ ] **Step 4: Implement the lexical lane**

Inverted index over entryKey, aliases, title, TEXT slot values, NODE_REF ids (kept as whole tokens, not split on hyphens), and body; BM25 with k1=1.2, b=0.75; lowercase; tokenize node ids and error strings as units (split on whitespace and punctuation except `-`, `:`, `_`).

- [ ] **Step 5: Implement the relation lane**

`KdbEntryGraph.neighbors` returns one-hop ENTRY_REF neighbors of an entry, each with the relation label and the direction (declared here vs. declared on the target), cycle-guarded by a visited set.

- [ ] **Step 6: Implement the `KnowledgeService` search skeleton**

Runs lexical top-K and relation expansion over its hits, fuses with RRF (k=60), applies domain/kind filters and the version drop (pinned constant `CORPUS_VERSION = "6.0.0"` as the comparison target), sorts by score then entryKey.

- [ ] **Step 7: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest='KdbLexicalIndexTest,KdbEntryGraphTest'`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KdbLexicalIndex.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KdbEntryGraph.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KnowledgeService.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/knowledge/KdbLexicalIndexTest.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/knowledge/KdbEntryGraphTest.java
git commit -m "feat(#1383): add the KDB lexical and relation lanes"
```

---

### Task 6: Vector lane wiring

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KdbVectorLane.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KnowledgeService.java` (optional vector lane in fusion)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/knowledge/KdbVectorLaneTest.java`

**Interfaces:**
- Consumes: `JIPipeAIServiceComponent.embedNow(String) : float[]`; `JIPipeEmbeddingDatabase.loadFromResource(String, String, JIPipeProgressInfo)`.
- Produces: `KdbVectorLane` with an injectable `Function<String, float[]> embedder` constructor parameter (production passes the AI component; tests pass a stub), `search(String query, int topK)`.

- [ ] **Step 1: Write the failing wiring test (stubbed embedder, no model)**

```java
@Test
void vectorLaneParticipatesInFusion() { /* stub embedder returns deterministic vectors; assert the lane is present
    in the KnowledgeResult.lanes of an entry only the vector lane could reach, and recompute the RRF score */ }
@Test
void missingEmbedderDegradesGracefully() { /* embedder throws -> search returns lexical+relation results, vector lane absent, no exception */ }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=KdbVectorLaneTest`
Expected: compile FAIL.

- [ ] **Step 3: Implement `KdbVectorLane`**

Constructor `KdbVectorLane(Function<String, float[]> embedder, Path cacheDir)`. `search(String query, int topK)` returns `List<ScoredEntry>` from cosine similarity over per-entry vectors. Entry vectors are keyed by `KdbEntry`'s content hash and cached in `cacheDir` under a sanitized model id (the node-search precedent in `JIPipeEmbeddingDatabase`); a cache miss computes via the embedder. Until Task 7 exists, embed `title + " " + slot values + " " + body`; Task 7 rewires it to the rendered text.

- [ ] **Step 4: Wire into `KnowledgeService`**

`KnowledgeService` gains an optional `KdbVectorLane` (null in production until an embedding model is configured). Its hits join the RRF fusion at the spec's per-lane weight; the resulting `KnowledgeResult.lanes` includes `"vector"` only for entries it reached.

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=KdbVectorLaneTest`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(#1383): add the KDB vector lane with an injectable embedder"
```

---

### Task 7: Renderer with registry-sourced node facts

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KdbRenderer.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/knowledge/KnowledgeService.java` (getKnowledgeEntry returns rendered + structured)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/knowledge/KdbRendererTest.java`

**Interfaces:**
- Consumes: `JIPipeNodeInfo` display name, `isDeprecated()`, `getSuccessorNodeIds()` (Task 1); `KdbEntryGraph.neighbors` (Task 5).
- Produces: `KdbRenderer.render(KdbEntry, int maxTokens) : String`.

- [ ] **Step 1: Write the failing render test**

```java
@Test
void deprecatedNodeRendersWithSuccessorLine() {
    // entry references a node id whose registry info is deprecated with a successor
    String text = renderer.render(entry, 1200);
    assertTrue(text.contains("deprecated"));
    assertTrue(text.contains("instead"));
}
@Test
void renderIsTokenBounded() { /* a huge entry is truncated under the bound */ }
@Test
void nextStepsListRelatedEntriesByKey() { /* relate entry renders "get_knowledge_entry" pointer */ }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=KdbRendererTest`
Expected: compile FAIL.

- [ ] **Step 3: Implement the renderer**

Per spec 5.6: title, kind, domain, resolved node lines (display name + id + deprecation and successor annotations), slot facts, body, and a Next steps block (related entry keys with relation labels and a `get_knowledge_entry` pointer). Node facts are resolved per render from `JIPipe.getNodes().getRegisteredNodeInfos()`, never cached in the entry. Token bound: check for a token estimator on master and use it; otherwise a whitespace heuristic with the constant and its rationale in the class Javadoc.

- [ ] **Step 4: Switch the vector lane to embed the rendered text**

`KdbVectorLane` embeds `KdbRenderer.render(entry, DEFAULT_BOUND)` instead of the raw fields, and the cache key becomes the rendered-text hash.

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest='KdbRendererTest,KdbVectorLaneTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(#1383): render KDB entries with registry-sourced node facts"
```

---

### Task 8: MCP operations

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/KnowledgeOperations.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register three operations)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/KnowledgeOperationsTest.java`

**Interfaces:**
- Consumes: `KnowledgeService` (Tasks 5-7); `AutomationOperation` (id, description, input schema, category, `isReadOnly`, `getToolScope`).
- Produces: operations `search_knowledge` (query: string, optional domain: string, optional limit: integer), `get_knowledge_entry` (entry-key: string, kebab-case), `list_knowledge_domains` (no args). All `isReadOnly() = true`, `getToolScope() = AutomationToolScope.INSTANCE_GLOBAL`, category `Query`, schemas with `additionalProperties: false` and a description on every property.

- [ ] **Step 1: Write the failing test**

```java
@Test
void registersThreeReadOnlyGlobalOperations() {
    // from the production registry: search_knowledge, get_knowledge_entry, list_knowledge_domains
    // all isReadOnly() == true and getToolScope() == AutomationToolScope.INSTANCE_GLOBAL
}

@Test
void searchReturnsRenderedEntries() { /* execute search_knowledge with a pilot term, assert
    an entry key appears and the rendered text is present */ }

@Test
void missingEntryKeyReturnsAnErrorEnvelope() { /* get_knowledge_entry with unknown key -> ok:false or
    an error field per the operations' wire convention, never a thrown exception */ }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=KnowledgeOperationsTest`
Expected: compile FAIL (operations do not exist).

- [ ] **Step 3: Implement and register**

Three `AutomationOperation` inner classes in `KnowledgeOperations`, registering in `AutomationPlugin` next to the existing query-operation block. Ids are stable snake_case literals; the description names the three related operations and points data questions elsewhere per the entry docs.

- [ ] **Step 4: Run test to verify it passes, then the registry gates**

Run: `mvn -q test -pl jipipe-core -Dtest='KnowledgeOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest'`
Expected: PASS on all.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(#1383): expose the KDB through read-only MCP operations"
```

---

### Task 9: Pilot corpus and golden relevance test

**Files:**
- Create: `jipipe-core/src/main/resources/org/hkijena/jipipe/ai/kdb/entries/bioimage-analysis/patterns/*.yaml` (from the 12 dormant bioimage pattern/decision-tree docs)
- Create: `docs/knowledge/sources/bioimage-analysis/**` (the rescued markdown, as provenance)
- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/knowledge/KdbRelevanceGoldenTest.java`
- Modify: the integrity gate expectations if the pilot exposes a new finding class.

**Interfaces:**
- Consumes: everything above.

- [ ] **Step 1: Rescue the markdown as provenance**

Extract the 12 bioimage patterns and decision-tree documents from the dying branch into `docs/knowledge/sources/bioimage-analysis/` with `git show origin/1294-image-analysis-kdb:<path> > <dest>`. Verify all 12 are present (`git ls-tree -r --name-only origin/1294-image-analysis-kdb | grep 'bioimage-analysis/\(patterns\|decision-trees\)/'` lists 12; the destination must hold the same 12). These files are provenance only: they are not packaged into jars, and `sourceRefs` point at them.

- [ ] **Step 2: Write the failing golden test**

`KdbRelevanceGoldenTest` with a table of 15 pinned queries (`query`, `expectedEntryKeys`, `expectedLanes`), covering: exact node-id lookups (2), aliases (2), plain concept phrases (4), error-shaped strings (2), cross-domain queries (2), related-item queries that only the relation lane reaches (2), and one query that must return nothing above threshold (1). Lane attribution is asserted for the lexical and relation lanes only.

- [ ] **Step 3: Run it to verify it fails**

Run: `mvn -q test -pl jipipe-launcher -Dtest=KdbRelevanceGoldenTest`
Expected: FAIL (expected entries missing; the corpus root is empty).

- [ ] **Step 4: Distill the pilot entries**

One YAML entry per pattern and decision tree, minimum 10 entries covering the 8 pattern documents that name a node chain plus the 2 decision trees. Every `nodeChain` id must be a registered id (check against the registry while authoring). `parameterEvidence` cites `/data/JIPipe/Example workflows/workflow-pattern-analysis.md` with the section the value came from; where the report has no value for a pattern, omit the slot rather than invent one. `aliases` carry the user-facing vocabulary from the markdown (cellpose, nuclei, segmentation, and so on).

- [ ] **Step 5: Run the golden test until it passes**

Run: `mvn -q test -pl jipipe-launcher -Dtest=KdbRelevanceGoldenTest`
Expected: PASS (15 queries). Adjust entries, never the pinned expectations, unless a pin is factually wrong against the corpus (say so in the commit message).

- [ ] **Step 6: Run the corpus gates and record the coverage report**

Run: `mvn -q test -pl jipipe-launcher -Dtest='KdbCorpusIntegrityTest,KdbCoverageTest'`
Expected: integrity PASS; coverage test PASS with its uncovered-leaf report printed. Paste the printed leaf count into the progress ledger.

- [ ] **Step 7: Commit**

```bash
git add docs/knowledge/sources/ jipipe-core/src/main/resources/org/hkijena/jipipe/ai/kdb/entries/ \
  jipipe-launcher/src/test/java/org/hkijena/jipipe/api/knowledge/KdbRelevanceGoldenTest.java
git commit -m "feat(#1383): add the KDB bioimage pilot corpus and golden relevance test"
```

---

### Task 10: Whole-branch verification

- [ ] **Step 1:** `mvn -q clean test -pl jipipe-core` - record counts.
- [ ] **Step 2:** `mvn -q clean test -pl jipipe-launcher` - record counts; confirm `KdbCoverageTest` report text is visible and sane.
- [ ] **Step 3:** Manual by-eye run: open a node's documentation in the compendium for a deprecated node and inspect the "Use that instead" line; hit the three MCP tools from an external client and inspect one search's rendered result. Report what you saw.
- [ ] **Step 4:** Whole-branch review, MR, work-item #1394 sync.