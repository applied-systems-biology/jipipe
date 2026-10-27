# Modernized Application Settings System — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace JIPipe's three settings systems (parameter sheets in `settings.json`, registry files `settings-db-*.json`, and `recent-projects.txt`) with one scoped, version-stable, multi-instance-safe settings document (`application` / `project` / `runtime`), expose it through a key + binding API, route all existing sheet/registry consumers onto it, and deliver the new non-modal settings tab with search, scope switcher, merged Appearance section, and a raw `about:config`-style editor.

**Architecture:** A new `SettingsDocument` (JSON, `sheet:`/`registry:`/`state:` prefixed blocks, `$schema` + `$meta`, unknown-key pass-through, atomic writes) is owned by one `JIPipeSettingsServiceComponent` per `JIPipeService`. Typed `JIPipeSettingKey<T>` declarations plus `SettingsBinding<T>` views provide the live change stream, and a parameter-sheet bridge projects existing `JIPipeApplicationSettingsSheet` collections onto document blocks in both directions. A file watcher + per-block merge gives multi-instance sync. Existing registry/recent-project APIs become deprecated delegates so the ~50 call sites keep compiling while the model is swapped underneath.

**Tech Stack:** Java 21, Maven, Jackson (`JsonUtils.getObjectMapper()`), JUnit 5 in `jipipe-core` (with `junit-platform-launcher` + `ImageJLegacyPatcherLauncherSessionListener`), Java NIO `WatchService`, Swing (JIPipe desktop parameter panels).

**Spec:** `docs/superpowers/specs/2026-10-27-modern-settings-system-design.md` — read it first; this plan implements it.

**Why the shims stay until later:** `putIntoRegistry`/`getFromRegistry`/`getListFromRegistry` have ~15 call sites, `JIPipeApplicationSettingsServiceComponent` is referenced from ~20 places, and all 35 sheets are registered by plugins. Removing any of them up-front leaves the tree non-compiling for many tasks. The new engine lands underneath, the shims delegate, and the rip-out is a later work item (spec §8: "removed in a post-6.0 cleanup").

## Global Constraints

- Branch: `1345-modern-settings-system` (GitLab convention `<issue-id>-<name-in-kebab-case>`); work in a git worktree off `master`.
- **Registered IDs are stable string literals.** Block keys (`"sheet:<id>"`, `"registry:<key>"`, `"state:<id>"`) and setting key IDs are explicit literals; never derive them from class names (`AGENTS.md`).
- **Do not pin registry sizes in tests** (`AGENTS.md` "Plugin-Aware Registry Tests"): assert presence of expected IDs and structural invariants, never `assertEquals(N, registry.size())`. Exact counts are only allowed for single-source ship sets (fixture files this plan generates).
- **Do not test trivial things** (`AGENTS.md`): no Swing component-structure tests, no getter/setter tests. Test serialization round-trips, ID stability, pass-through, merge semantics, watcher self-ignore, migration.
- TDD: every task starts with a failing test. Run gate suite with `mvn test -pl jipipe-core` (the settings engine has no launcher-module dependency; launcher tests run in the final task).
- Never call `LegacyInjector.preinit()` from tests, never add `--add-opens` workarounds (`AGENTS.md`; the launcher session listener handles this).
- Jackson: always `JsonUtils.getObjectMapper()`; never construct a bare `ObjectMapper`.
- All new runtime-touching code must run headless (`RuntimeSettings` writes in the CLI too); the watcher is disabled when headless.
- Commit after every task. The tree must compile at every commit.

## Review Focus

Failure modes the spec implies but whose tests are easy to forget — each line is pinned into its owning task:

1. **A settings file containing blocks this version does not know is saved back byte-identical** (the "downgrade clears settings" bug) → unknown `sheet:`/`registry:`/`state:` blocks and unknown *paths inside known blocks* survive a load→save cycle verbatim. *Pinned in Task 3.*
2. **A corrupt / truncated settings file** (crash mid-write, disk full, a `settings.json` that is not JSON) → the file is backed up as `<name>.bad-<timestamp>`, defaults load, the process starts, and the next save writes a valid file — never a startup failure or an infinite backup loop. *Pinned in Task 4.*
3. **Self-ignoring the watcher** — a process that saves its own document must not treat its own write as a foreign change (otherwise every save triggers a reload storm). *Pinned in Task 8.*
4. **Two instances editing different blocks simultaneously** → neither block is lost (per-block merge), and the older *unsaved* block loses only on a true same-block collision with a user-visible notification. *Pinned in Task 9.*
5. **The parameter-sheet bridge must not loop** — a sheet parameter change writes to the document, whose change event must not re-fire the same parameter change (infinite recursion / change storm). *Pinned in Task 6.*
6. **`recent-projects.txt` from a 5.x profile is not stranded** — its paths end up in `state:recent-projects` and every consumer (`WelcomePanel`, recent-projects menu, statistics item) sees them. *Pinned in Task 11.*
7. **A `state:`/`registry:` block whose scope is ambiguous defaults to `application`** so a user's UI state is never lost to a wrong scope guess. *Pinned in Task 10.*

---

# Phase 1 — The settings document (storage core)

No UI, no consumers yet; the engine is built and tested behind the existing API surface (which is untouched in this phase).

### Task 1: Scopes, kinds, and the key path model

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsScope.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsKind.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsBlockPath.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsBlockPathTest.java`

**Interfaces:**
- Produces:
  - `enum SettingsScope { APPLICATION, PROJECT, RUNTIME }`
  - `enum SettingsKind { SHEET, REGISTRY, STATE }` with `String prefix()` returning `"sheet"`, `"registry"`, `"state"`, and `static SettingsKind fromPrefix(String)` returning `null` for unknown prefixes.
  - `final class SettingsBlockPath` — `SettingsBlockPath(SettingsKind kind, String id)` and `SettingsBlockPath(String rawId)` parsing `"<prefix>:<id>"`; `SettingsKind kind()`, `String id()`, `String path()` (the canonical `"<prefix>:<id>"`), `String subPath()`, `SettingsBlockPath withSubPath(String sub)`; `equals`/`hashCode` on `path()+subPath()`; unknown prefixes parse with `kind() == null` (pass-through marker, Review Focus #1).
  - `SettingsBlockPath` is immutable; `withSubPath` returns a new instance.

- [ ] **Step 1: Write the failing test**

```java
class SettingsBlockPathTest {
    @Test void parsesPrefixedBlockId() {
        SettingsBlockPath p = new SettingsBlockPath("sheet:org.hkijena.jipipe:general-ui");
        assertEquals(SettingsKind.SHEET, p.kind());
        assertEquals("org.hkijena.jipipe:general-ui", p.id());
        assertEquals("sheet:org.hkijena.jipipe:general-ui", p.path());
    }

    @Test void unknownPrefixIsPassThrough() {
        SettingsBlockPath p = new SettingsBlockPath("future-thing:abc");
        assertNull(p.kind());
        assertEquals("future-thing:abc", p.path());
    }

    @Test void subPathAppendsWithSlash() {
        SettingsBlockPath p = new SettingsBlockPath("registry:ui-graph-editor").withSubPath("any/dock/show-labels");
        assertEquals("registry:ui-graph-editor/any/dock/show-labels", p.pathWithSubPath());
    }

    @Test void equalityUsesFullPath() {
        assertEquals(new SettingsBlockPath("state:recent-projects"), new SettingsBlockPath("state:recent-projects"));
        assertNotEquals(new SettingsBlockPath("state:a"), new SettingsBlockPath("state:b"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsBlockPathTest -q`
Expected: FAIL — compile error, `SettingsBlockPath` does not exist.

- [ ] **Step 3: Implement the three types**

`SettingsScope` and `SettingsKind` are plain enums as specified. `SettingsBlockPath.parse` splits on the **first** `:` only (IDs contain `:` themselves, e.g. `sheet:org.hkijena.jipipe:general-ui`); a raw id with no `:` yields `kind() == null` and `id() == rawId`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsBlockPathTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsScope.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsKind.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsBlockPath.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsBlockPathTest.java
git commit -m "feat(settings): scopes, kinds, and block path model (#1345)"
```

### Task 2: The settings document (in-memory, pass-through)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsDocument.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsDocumentTest.java`

**Interfaces:**
- Consumes: `SettingsBlockPath`, `SettingsKind`, `SettingsScope` (Task 1).
- Produces: `final class SettingsDocument`
  - `SettingsDocument(SettingsScope scope)`; `SettingsScope scope()`
  - `ObjectNode root()` — the live mutable root (callers must go through `set*` for events)
  - `JsonNode getBlock(String blockPath)` — block JSON or `MissingNode`
  - `void setBlock(String blockPath, JsonNode value)`
  - `JsonNode get(String blockPath, String subPath)` / `set(String blockPath, String subPath, JsonNode value)` — sub-path is slash-separated, `JIPipePathMetadataStore.key(...)`-compatible
  - `boolean hasBlock(String blockPath)`; `boolean has(String blockPath, String subPath)`
  - `Set<String> blockPaths()`; `Set<String> subPaths(String blockPath)`
  - `boolean isPassThrough(String blockPath)` — true when the block's kind is unknown or its id is not registered; a pass-through block is stored and written back verbatim, and `get`/`set` on its sub-paths is allowed but only raw
  - `void remove(String blockPath, String subPath)`; `void removeBlock(String blockPath)`
  - `int schemaVersion()` / `void setSchemaVersion(int)`; `ObjectNode meta()`
  - `int writerId`-independent: `String writerId()` fixed per instance (random hex, generated in constructor)
  - Defaults are never materialized: `get` on an absent sub-path returns `MissingNode`, not the value's type default.

- [ ] **Step 1: Write the failing test**

```java
class SettingsDocumentTest {
    @Test void absentSubPathIsMissingNotDefault() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        assertTrue(d.get("registry:ui-graph-editor", "any/dock/show-labels").isMissingNode());
    }

    @Test void subPathRoundTrip() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("registry:node-db", "pinned-node-ids", JsonUtils.toJsonNode(List.of("a", "b")));
        assertEquals(2, d.get("registry:node-db", "pinned-node-ids").size());
    }

    @Test void nestedSubPathCreatesIntermediateObjects() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("registry:ui-graph-editor", "any/dock/show-labels", BooleanNode.TRUE);
        assertTrue(d.get("registry:ui-graph-editor", "any/dock").isObject());
        assertTrue(d.get("registry:ui-graph-editor", "any/dock/show-labels").asBoolean());
    }

    @Test void unregisteredBlockCanBeMarkedPassThrough() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.setBlock("future-thing:abc", JsonUtils.toJsonNode(Map.of("x", 1)));
        assertTrue(d.isPassThrough("future-thing:abc"));
        assertFalse(d.isPassThrough("registry:ui-graph-editor"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsDocumentTest -q`
Expected: FAIL — `SettingsDocument` does not exist.

- [ ] **Step 3: Implement `SettingsDocument`**

Back it with a Jackson `ObjectNode` root; `$schema` and `$meta` are reserved fields (`$schema` int, `$meta` object with `written-by-version`, `written-at`, `writer-id`). `set` walks/creates the slash-split sub-path, creating intermediate object nodes. `isPassThrough` is decided by an injected `Predicate<String> registeredBlock` (constructor overload; default `p -> new SettingsBlockPath(p).kind() != null`), so registration-aware callers can mark `sheet:` blocks with unknown sheet ids as pass-through. Interpolation of `$meta` values is done by the caller (Task 4) so the document stays a pure data structure.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsDocumentTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsDocument.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsDocumentTest.java
git commit -m "feat(settings): in-memory settings document with pass-through blocks (#1345)"
```

### Task 3: Serialization round-trip and verbatim pass-through

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsDocumentIO.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsDocumentIOTest.java`

**Interfaces:**
- Consumes: `SettingsDocument` (Task 2).
- Produces: `final class SettingsDocumentIO` (static)
  - `ObjectNode toJson(SettingsDocument doc)`
  - `SettingsDocument fromJson(SettingsScope scope, JsonNode node, Predicate<String> registeredBlock)`
  - `String toPrettyString(SettingsDocument doc)` (pretty printer, matching `JsonUtils`)
  - Round-trip guarantee: `fromJson(toJson(d))` preserves every block and sub-path, **including unknown blocks and unknown sub-paths inside known blocks** (Review Focus #1).

- [ ] **Step 1: Write the failing test**

```java
class SettingsDocumentIOTest {
    private static final String FIXTURE = """
            {
              "$schema": 2,
              "$meta": { "written-by-version": "9.9.9", "written-at": "2026-01-01T00:00:00Z", "writer-id": "deadbeef" },
              "sheet:org.hkijena.jipipe:general-ui": { "theme": "Modern", "a-newer-knob": 42 },
              "registry:ui-graph-editor": { "any/dock/show-labels": true },
              "future-thing:abc": { "x": [1, 2, 3] },
              "state:recent-projects": ["/a/b.jip"]
            }
            """;

    @Test void preservesUnknownBlocksVerbatim() throws Exception {
        JsonNode in = JsonUtils.getObjectMapper().readTree(FIXTURE);
        SettingsDocument d = SettingsDocumentIO.fromJson(SettingsScope.APPLICATION, in, p -> true);
        assertTrue(d.isPassThrough("future-thing:abc"));
        JsonNode out = SettingsDocumentIO.toJson(d);
        assertEquals(in.get("future-thing:abc"), out.get("future-thing:abc"));
    }

    @Test void preservesUnknownSubPathsInsideKnownBlocks() throws Exception {
        JsonNode in = JsonUtils.getObjectMapper().readTree(FIXTURE);
        SettingsDocument d = SettingsDocumentIO.fromJson(SettingsScope.APPLICATION, in, p -> true);
        JsonNode out = SettingsDocumentIO.toJson(d);
        assertEquals(42, out.get("sheet:org.hkijena.jipipe:general-ui").get("a-newer-knob").asInt());
    }

    @Test void fullRoundTripIsStable() throws Exception {
        JsonNode in = JsonUtils.getObjectMapper().readTree(FIXTURE);
        JsonNode once = SettingsDocumentIO.toJson(SettingsDocumentIO.fromJson(SettingsScope.APPLICATION, in, p -> true));
        JsonNode twice = SettingsDocumentIO.toJson(SettingsDocumentIO.fromJson(SettingsScope.APPLICATION, once, p -> true));
        assertEquals(once, twice);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsDocumentIOTest -q`
Expected: FAIL — `SettingsDocumentIO` does not exist.

- [ ] **Step 3: Implement `SettingsDocumentIO`**

`toJson` returns a deep copy of `doc.root()` (so callers cannot mutate the live document by accident). `fromJson` deep-copies the input into a fresh document, sets the schema version (defaulting to `2` when absent), and stores `$meta` as-is. No filtering happens here — dropping unknown content is precisely the bug this task prevents.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsDocumentIOTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsDocumentIO.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsDocumentIOTest.java
git commit -m "feat(settings): document serialization with verbatim pass-through (#1345)"
```

### Task 4: Atomic file store, corrupt-file recovery, tmp cleanup

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsFileStore.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsFileStoreTest.java`

**Interfaces:**
- Consumes: `SettingsDocument`, `SettingsDocumentIO` (Tasks 2–3).
- Produces: `final class SettingsFileStore`
  - `SettingsFileStore(Path file, SettingsScope scope, Predicate<String> registeredBlock)`
  - `SettingsDocument load()` — returns a fresh document; missing file → empty document; unreadable/corrupt file → **move to `<name>.bad-<ISO timestamp>` and return an empty document** (Review Focus #2); the backup move happens at most once per corrupt file (a `.bad-*` file is never itself backed up)
  - `void save(SettingsDocument doc)` — write `<name>.tmp.<writerId>` in the same directory, then `Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, REPLACE_EXISTING)`; if `AtomicMoveNotSupportedException`, fall back to `REPLACE_EXISTING` move; always delete the tmp on failure
  - `FileFingerprint fingerprint()` — record of `(size, lastModifiedTimeMillis)` or `null` if the file does not exist
  - `static void cleanUpOrphanTempFiles(Path directory)` — deletes `*.tmp.*` files older than 24 h in `directory` (called at startup)
  - `record FileFingerprint(long size, long lastModifiedMillis)` with `static FileFingerprint of(Path)`
  - `Path file()`

- [ ] **Step 1: Write the failing test**

```java
@TempDir Path tempDir;

@Test void corruptFileIsBackedUpAndDefaultsLoad() throws Exception {
    Path f = tempDir.resolve("settings.json");
    Files.writeString(f, "{ this is not json");
    SettingsFileStore store = new SettingsFileStore(f, SettingsScope.APPLICATION, p -> true);
    SettingsDocument doc = store.load();
    assertTrue(doc.blockPaths().isEmpty(), "corrupt file must load as empty document");
    try (var s = Files.list(tempDir)) {
        assertTrue(s.anyMatch(p -> p.getFileName().toString().startsWith("settings.json.bad-")),
                "corrupt file must be preserved as .bad-<timestamp>");
    }
}

@Test void missingFileLoadsEmptyAndFirstSaveCreatesIt() {
    Path f = tempDir.resolve("sub/settings.json");
    SettingsFileStore store = new SettingsFileStore(f, SettingsScope.APPLICATION, p -> true);
    SettingsDocument doc = store.load();
    doc.set("registry:x", "a", BooleanNode.TRUE);
    store.save(doc);
    assertTrue(Files.isRegularFile(f));
}

@Test void saveIsAtomicAndLeavesNoTempFile() throws Exception {
    Path f = tempDir.resolve("settings.json");
    SettingsFileStore store = new SettingsFileStore(f, SettingsScope.RUNTIME, p -> true);
    SettingsDocument doc = store.load();
    doc.set("registry:x", "a", BooleanNode.TRUE);
    store.save(doc);
    try (var s = Files.list(tempDir)) {
        assertFalse(s.anyMatch(p -> p.getFileName().toString().contains(".tmp.")), "no tmp file may remain");
    }
}

@Test void fingerprintChangesAfterSave() throws Exception {
    Path f = tempDir.resolve("settings.json");
    SettingsFileStore store = new SettingsFileStore(f, SettingsScope.APPLICATION, p -> true);
    SettingsDocument doc = store.load();
    assertNull(store.fingerprint());
    doc.set("registry:x", "a", BooleanNode.TRUE);
    store.save(doc);
    assertNotNull(store.fingerprint());
    assertTrue(store.fingerprint().size() > 0);
}

@Test void orphanTempFilesAreCleanedButFreshOnesSurvive() throws Exception {
    Path fresh = Files.writeString(tempDir.resolve("settings.json.tmp.abc"), "x");
    Path old = Files.writeString(tempDir.resolve("settings.json.tmp.def"), "x");
    Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(48, ChronoUnit.HOURS)));
    SettingsFileStore.cleanUpOrphanTempFiles(tempDir);
    assertTrue(Files.exists(fresh));
    assertFalse(Files.exists(old));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsFileStoreTest -q`
Expected: FAIL — `SettingsFileStore` does not exist.

- [ ] **Step 3: Implement `SettingsFileStore`**

Create parent directories before writing. `load()` catches `IOException` **and** Jackson parse errors and performs the backup move; if the move fails (locked file), log and continue with an empty document — never throw out of `load()`. `fingerprint()` returns `null` when absent (so a first save is detectable as a change). `cleanUpOrphanTempFiles` also removes `*.tmp.*` in the given directory regardless of the settings file name, so a stale `runtime-settings.json.tmp.*` is covered too.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsFileStoreTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsFileStore.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsFileStoreTest.java
git commit -m "feat(settings): atomic file store with corrupt-file recovery (#1345)"
```

### Task 5: Setting keys and typed bindings

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/JIPipeSettingKey.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsAccessPolicy.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsBinding.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsBindingTest.java`

**Interfaces:**
- Consumes: `SettingsDocument` (Task 2), `SettingsScope`, `SettingsKind`.
- Produces:
  - `enum SettingsAccessPolicy { USER_EDITABLE, RUNTIME_ADMIN, READ_ONLY }`
  - `final class JIPipeSettingKey<T>` — static factories `bool(...)`, `string(...)`, `integer(...)`, `doub(...)`, `object(...)` each taking `(String kindOrBlockPath, String subPath, Class<T> type)`; builder methods `defaultValue(T)`, `scope(SettingsScope)`, `access(SettingsAccessPolicy)`, `name(String)`, `description(String)`, `migrate(boolean)`; accessors `blockPath()`, `subPath()`, `type()`, `defaultValue()`, `scope()`, `access()`, `name()`, `description()`, `migrates()`
  - `final class SettingsBinding<T>` — `T get()`, `void set(T)`, `void reset()`, `boolean isModified()`, `void subscribe(Consumer<T>)`, `void unsubscribe(Consumer<T>)`, `JIPipeSettingKey<T> key()`
  - A binding reads/writes one sub-path of one block of one document and fires subscribers on **any** change to that path (including a document-level external reload), with a re-entrancy guard: `set(x)` when the value already equals `x` fires nothing, and an event whose new value equals the last value delivered is not re-delivered.
  - All bindings are created by the settings service (Task 7), never directly by features.

- [ ] **Step 1: Write the failing test**

```java
class SettingsBindingTest {
    private JIPipeSettingKey<Boolean> key() {
        return JIPipeSettingKey.bool("registry:ui-graph-editor", "any/dock/show-labels", Boolean.class)
                .defaultValue(true).scope(SettingsScope.APPLICATION).access(SettingsAccessPolicy.USER_EDITABLE);
    }

    @Test void absentValueReadsAsDefault() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        assertTrue(b.get());
        assertFalse(b.isModified());
    }

    @Test void setWritesThroughAndMarksModified() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        b.set(false);
        assertFalse(b.get());
        assertTrue(b.isModified());
        assertTrue(d.has("registry:ui-graph-editor", "any/dock/show-labels"));
    }

    @Test void resetRemovesValueAndRestoresDefault() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        b.set(false);
        b.reset();
        assertTrue(b.get());
        assertFalse(b.isModified());
    }

    @Test void subscribersFireOnForeignChange() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> a = new SettingsBinding<>(d, key());
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        List<Boolean> seen = new ArrayList<>();
        b.subscribe(seen::add);
        a.set(false);
        assertEquals(List.of(false), seen);
    }

    @Test void noEventWhenValueUnchanged() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsBinding<Boolean> b = new SettingsBinding<>(d, key());
        List<Boolean> seen = new ArrayList<>();
        b.subscribe(seen::add);
        b.set(true);   // already default true -> no modification
        assertTrue(d.get("registry:ui-graph-editor", "any/dock/show-labels").isMissingNode(),
                "setting the default value must not materialize the key");
        assertTrue(seen.isEmpty(), "no event for an unchanged value");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsBindingTest -q`
Expected: FAIL — `JIPipeSettingKey`/`SettingsBinding` do not exist.

- [ ] **Step 3: Implement the key and the binding**

`SettingsBinding` is constructed with a `SettingsDocument` and a key. Its change notifications come from a small listener interface on `SettingsDocument`: add `SettingsDocument.addPathListener(String pathWithSubPath, Consumer<JsonNode> listener)` / `removePathListener(...)` in this task (a `Map<String, List<Consumer<JsonNode>>>` on the document, fired by `set`/`remove`/`setBlock`/`replaceRoot`). `set(default)` removes the sub-path instead of writing it (defaults are never materialized). Value conversion goes through `JsonUtils.getObjectMapper().treeToValue(node, key.type())` on read and `valueToTree(value)` on write; a `JsonNode`-typed key (`object(...)`) round-trips nodes unchanged. `get()` catches conversion errors, logs, and returns the default (a settings file must never crash a caller).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsBindingTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/JIPipeSettingKey.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsAccessPolicy.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsBinding.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsDocument.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsBindingTest.java
git commit -m "feat(settings): typed setting keys and live bindings (#1345)"
```

### Task 6: The parameter-sheet bridge (sheet ⇄ document, loop-safe)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsSheetBridge.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsSheetBridgeTest.java`

**Interfaces:**
- Consumes: `SettingsDocument` (Task 2), `JIPipeApplicationSettingsSheet`, `JIPipeParameterCollection` serialization (existing).
- Produces: `final class SettingsSheetBridge`
  - `SettingsSheetBridge(SettingsDocument doc, String sheetId, JIPipeApplicationSettingsSheet sheet)`
  - `void attach()` — serialize the sheet into `sheet:<sheetId>` if the block is absent (so a fresh profile behaves like the old defaults), then subscribe to both directions
  - `void detach()`
  - `void flushToDocument()` — serialize sheet → block (used by save and by explicit writes)
  - `void applyFromDocument()` — block → sheet, firing the parameter change events the UI listens to
  - Loop safety (Review Focus #5): a flag plus value comparison ensures a change that originates from the document does not write back to the document, and vice versa. `applyFromDocument` sets a single parameter at a time only when the serialized value differs.
  - Pass-through preservation: `applyFromDocument` deserializes **only** the parameters the sheet knows; unknown sub-paths remain in the block and on `flushToDocument` the serialized sheet output is **merged into** the existing block rather than replacing it (unknown sub-paths survive).

- [ ] **Step 1: Write the failing test**

```java
class SettingsSheetBridgeTest {
    static class ProbeSheet extends AbstractJIPipeParameterCollection implements JIPipeApplicationSettingsSheet {
        @JIPipeParameter("theme")
        private String theme = "Modern";
        // getId/getName/getIcon/getCategory/getCategoryDescription and the getter/setter are elided here;
        // the implementer keeps them as in any JIPipeApplicationSettingsSheet.
    }

    @Test void documentChangeUpdatesSheetWithoutWritingBack() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        ProbeSheet sheet = new ProbeSheet();
        SettingsSheetBridge bridge = new SettingsSheetBridge(d, "test-sheet", sheet);
        bridge.attach();

        d.set("sheet:test-sheet", "theme", TextNode.valueOf("Dark"));
        bridge.applyFromDocument();
        assertEquals("Dark", sheet.getTheme());
        assertEquals("Dark", d.get("sheet:test-sheet", "theme").asText());
    }

    @Test void sheetChangeWritesToDocumentOnce() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        ProbeSheet sheet = new ProbeSheet();
        SettingsSheetBridge bridge = new SettingsSheetBridge(d, "test-sheet", sheet);
        bridge.attach();
        List<String> writes = new ArrayList<>();
        d.addPathListener("sheet:test-sheet/theme", n -> writes.add(n.asText()));

        sheet.setTheme("Dark");
        assertEquals(List.of("Dark"), writes, "exactly one write, no change storm");
    }

    @Test void unknownSubPathsInBlockSurviveASheetRoundTrip() {
        SettingsDocument d = new SettingsDocument(SettingsScope.APPLICATION);
        d.set("sheet:test-sheet", "a-newer-knob", IntNode.valueOf(42));
        ProbeSheet sheet = new ProbeSheet();
        SettingsSheetBridge bridge = new SettingsSheetBridge(d, "test-sheet", sheet);
        bridge.attach();
        sheet.setTheme("Dark");
        bridge.flushToDocument();
        assertEquals(42, d.get("sheet:test-sheet", "a-newer-knob").asInt(),
                "a knob unknown to this version must not be cleared by a sheet write");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsSheetBridgeTest -q`
Expected: FAIL — `SettingsSheetBridge` does not exist.

- [ ] **Step 3: Implement `SettingsSheetBridge`**

Use `sheet.serializeToJsonString()` → `JsonNode`, and `sheet.deserializeFromJsonNode(node)` for the sheet side; use the document's `addPathListener` for the document side. `flushToDocument` merges: read the existing block, set the sheet's keys onto it (Jackson `ObjectNode.setAll`), write the merged block back. `applyFromDocument` compares each serialized sheet key against the block value and calls `deserializeFromJsonNode` on a **filtered copy** (only known keys) so pass-through sub-paths cannot fail validation. Check sheet parameter change events via `sheet.getParameterChangedEventEmitter().subscribeLambda(...)` and derive the changed key from `event.getKey()`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsSheetBridgeTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsSheetBridge.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsSheetBridgeTest.java
git commit -m "feat(settings): loop-safe parameter sheet bridge (#1345)"
```

### Task 7: The settings service component

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeSettingsServiceComponent.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java` (add the component, keep `applicationSettings`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/service/JIPipeSettingsServiceComponentTest.java`

**Interfaces:**
- Consumes: Tasks 1–6.
- Produces: `final class JIPipeSettingsServiceComponent extends JIPipeServiceComponent`
  - `SettingsDocument document(SettingsScope scope)` — one document per scope; `APPLICATION` backed by `<userDir>/settings.json`, `RUNTIME` by `<userDir>/runtime-settings.json`, `PROJECT` by an in-memory document that the project pushes/pulls (Task 12)
  - `Path fileFor(SettingsScope scope)` — `null` for `PROJECT`
  - `<T> SettingsBinding<T> bind(JIPipeSettingKey<T> key)` — registers the key in the key registry and returns a live binding on the key's scope
  - `void registerKey(JIPipeSettingKey<?> key)`
  - `JIPipeSettingKey<?> keyByBlockAndSubPath(String blockPath, String subPath)`
  - `Set<JIPipeSettingKey<?>> registeredKeys()`
  - `void attachSheet(JIPipeApplicationSettingsSheet sheet)` — registers the bridge for `sheet:<sheet.getId()>` (replaces the sheet registration path)
  - `void reload()` — re-read every file-backed document from disk (used at startup and by the old `reload()` callers)
  - `void save()` — flush all sheet bridges, then save every file-backed document
  - `void saveLater()` — debounced (250 ms) `save()`, same contract as today's `saveLater`
  - `SettingsChangedEventEmitter getChangedEventEmitter()` with `SettingsChangedEvent(SettingsScope scope, String path, JsonNode oldValue, JsonNode newValue, ChangeOrigin origin)` and `enum ChangeOrigin { LOCAL_EDIT, LOCAL_PROGRAMMATIC, EXTERNAL_FILE, EXTERNAL_PROCESS, CARRY_OVER, RESET, RAW_EDIT }`
  - A legacy aggregate: `ChangedEventEmitter getLegacyChangedEventEmitter()` emitting on any change (wired in Task 13)
  - Startup order: `document()` lazily loads and attaches bridges, so behavior matches today's `reload()`-after-registration sequence.

- [ ] **Step 1: Write the failing test**

```java
class JIPipeSettingsServiceComponentTest {
    @Test void documentsAreScopeSeparated() {
        // Construct the component against a temp user dir override is not possible in a pure unit test;
        // this test uses the injectable file resolver constructor instead.
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        assertNotSame(c.document(SettingsScope.APPLICATION), c.document(SettingsScope.RUNTIME));
        assertEquals(SettingsScope.APPLICATION, c.document(SettingsScope.APPLICATION).scope());
    }

    @Test void registeredKeyIsResolvable() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        JIPipeSettingKey<Boolean> k = JIPipeSettingKey.bool("registry:x", "a", Boolean.class).defaultValue(true);
        c.registerKey(k);
        assertSame(k, c.keyByBlockAndSubPath("registry:x", "a"));
    }

    @Test void bindingRoundTripsThroughSaveAndReload() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        SettingsBinding<Boolean> b = c.bind(JIPipeSettingKey.bool("registry:x", "a", Boolean.class).defaultValue(true));
        b.set(false);
        c.save();
        c.reload();
        assertFalse(c.bind(JIPipeSettingKey.bool("registry:x", "a", Boolean.class).defaultValue(true)).get());
    }

    @Test void availableScopesAlwaysContainAllThree() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        assertEquals(Set.of(SettingsScope.APPLICATION, SettingsScope.PROJECT, SettingsScope.RUNTIME), c.availableScopes());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeSettingsServiceComponentTest -q`
Expected: FAIL — the component does not exist.

- [ ] **Step 3: Implement the component**

Add a package-visible `static JIPipeSettingsServiceComponent forTesting(Path userDir)` factory (used only by tests) that skips `JIPipeService` wiring but uses the same code path via an injected `Path userDir` field; the production constructor `JIPipeSettingsServiceComponent(JIPipeService service)` defaults that field to `JIPipe.getJIPipeUserDir(false)`. Register the component in `JIPipeService`'s constructor and in the `components` array (needed for `dispose()`), and add `getSettings()` accessor; **do not remove** the existing `applicationSettings` component yet (Task 13 makes it delegate). The debounce timer must be a `javax.swing.Timer` **only when a Swing EDT exists** — use a `ScheduledExecutorService` in headless mode so CLI runs still coerce saves without initializing Swing.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeSettingsServiceComponentTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeSettingsServiceComponent.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/service/JIPipeSettingsServiceComponentTest.java
git commit -m "feat(settings): settings service component with scoped documents (#1345)"
```

### Task 8: File watcher with self-write suppression

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsFileWatcher.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsFileWatcherTest.java`

**Interfaces:**
- Consumes: `SettingsFileStore.FileFingerprint` (Task 4).
- Produces: `final class SettingsFileWatcher implements AutoCloseable`
  - `SettingsFileWatcher(Path file, Consumer<FileFingerprint> onForeignChange)`
  - `void start()` — `WatchService` on the parent dir (filtered to the file name) plus a poll thread every 2 s comparing `FileFingerprint`; the poll is the correctness floor
  - `void setOwnFingerprint(FileFingerprint)` — called by the store owner after each save; a detected change matching this fingerprint fires nothing (Review Focus #3)
  - `void close()` — stop threads, close the watch service
  - `boolean isHeadlessSafe()` — true when `GraphicsEnvironment.isHeadless()`; `start()` is a no-op returning immediately in that case
  - Duplicate events for one write (WatchService may fire several) are debounced to a single callback per distinct fingerprint.

- [ ] **Step 1: Write the failing test**

```java
@Test void ownWriteDoesNotFire() throws Exception {
    Path f = Files.writeString(tempDir.resolve("settings.json"), "{\"a\":1}");
    List<FileFingerprint> seen = new CopyOnWriteArrayList<>();
    SettingsFileWatcher w = new SettingsFileWatcher(f, seen::add);
    w.start();
    FileFingerprint fp = FileFingerprint.of(f);
    w.setOwnFingerprint(fp);
    Files.writeString(f, "{\"a\":2}");
    w.setOwnFingerprint(FileFingerprint.of(f));   // this process wrote it
    Thread.sleep(2600);
    assertTrue(seen.isEmpty(), "a write this process performed must not fire a foreign change");
    w.close();
}

@Test void foreignWriteFiresOnce() throws Exception {
    Path f = Files.writeString(tempDir.resolve("settings.json"), "{\"a\":1}");
    List<FileFingerprint> seen = new CopyOnWriteArrayList<>();
    SettingsFileWatcher w = new SettingsFileWatcher(f, seen::add);
    w.start();
    w.setOwnFingerprint(FileFingerprint.of(f));
    // simulate another process: change size+mtime without touching our fingerprint record
    Files.writeString(f, "{\"a\":1,\"b\":2}");
    Files.setLastModifiedTime(f, FileTime.from(Instant.now().plusSeconds(5)));
    Thread.sleep(2600);
    w.close();
    assertEquals(1, seen.size(), "exactly one callback per distinct fingerprint");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsFileWatcherTest -q`
Expected: FAIL — `SettingsFileWatcher` does not exist.

- [ ] **Step 3: Implement the watcher**

Use a daemon thread for the poll loop and a daemon thread for `WatchService.take()`; both exit on `close()` via an `AtomicBoolean`. Compare fingerprints by `(size, lastModifiedMillis)` and suppress equality. Debounce identical fingerprints. Register `ENTRY_CREATE`, `ENTRY_MODIFY` (`ENTRY_DELETE` fires a change too, since deletion means the file was replaced). Guard all callbacks with a try/catch so a throwing consumer cannot kill the poll thread.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsFileWatcherTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsFileWatcher.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsFileWatcherTest.java
git commit -m "feat(settings): file watcher with self-write suppression (#1345)"
```

### Task 9: Per-block merge and foreign-change application

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsDocumentMerger.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeSettingsServiceComponent.java` (wire watcher → merger → document)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsDocumentMergerTest.java`

**Interfaces:**
- Consumes: `SettingsDocument` (Task 2), `SettingsFileWatcher` (Task 8).
- Produces: `final class SettingsDocumentMerger`
  - `static MergeResult merge(SettingsDocument local, SettingsDocument foreign, Set<String> locallyChangedBlocks)`
  - `record MergeResult(Set<String> adoptedBlocks, Set<String> keptBlocks, Set<String> conflictedBlocks)`
  - Rules (Review Focus #4): a block changed only externally is adopted; a block in `locallyChangedBlocks` that also differs in the foreign document is a **conflict** and the **foreign block wins** (it is the most recent on-disk truth); a block changed only locally is kept and the caller re-saves. Blocks only present locally are kept (another instance may simply not have written them yet). Pass-through blocks are merged by the same rules.
  - The component fires `SettingsChangedEvent` with `ChangeOrigin.EXTERNAL_FILE` / `EXTERNAL_PROCESS` for adopted/conflicted blocks and exposes `boolean hasForeignConflict()` for the UI banner.

- [ ] **Step 1: Write the failing test**

```java
class SettingsDocumentMergerTest {
    @Test void adoptsBlockChangedOnlyExternally() {
        SettingsDocument local = docWith("registry:a", "x", 1);
        SettingsDocument foreign = docWith("registry:a", "x", 2);
        MergeResult r = SettingsDocumentMerger.merge(local, foreign, Set.of());
        assertEquals(Set.of("registry:a"), r.adoptedBlocks());
        assertEquals(2, local.get("registry:a", "x").asInt());
    }

    @Test void keepsBlockChangedOnlyLocally() {
        SettingsDocument local = docWith("registry:a", "x", 9);
        SettingsDocument foreign = docWith("registry:a", "x", 1);
        MergeResult r = SettingsDocumentMerger.merge(local, foreign, Set.of("registry:a"));
        assertEquals(Set.of("registry:a"), r.keptBlocks());
        assertEquals(9, local.get("registry:a", "x").asInt());
    }

    @Test void foreignWinsOnCollisionAndIsReported() {
        SettingsDocument local = docWith("registry:a", "x", 9);
        SettingsDocument foreign = docWith("registry:a", "x", 2);
        MergeResult r = SettingsDocumentMerger.merge(local, foreign, Set.of("registry:a"));
        assertEquals(Set.of("registry:a"), r.conflictedBlocks());
        assertEquals(2, local.get("registry:a", "x").asInt());
    }

    @Test void blockPresentOnlyLocallyIsKept() {
        SettingsDocument local = docWith("registry:a", "x", 9);
        SettingsDocument foreign = new SettingsDocument(SettingsScope.APPLICATION);
        MergeResult r = SettingsDocumentMerger.merge(local, foreign, Set.of());
        assertEquals(9, local.get("registry:a", "x").asInt());
    }

    @Test void unknownPassThroughBlocksAreMergedToo() {
        SettingsDocument local = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsDocument foreign = new SettingsDocument(SettingsScope.APPLICATION);
        foreign.setBlock("future:z", IntNode.valueOf(1));
        SettingsDocumentMerger.merge(local, foreign, Set.of());
        assertEquals(1, local.get("future:z", "").asInt());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsDocumentMergerTest -q`
Expected: FAIL — `SettingsDocumentMerger` does not exist.

- [ ] **Step 3: Implement the merger and wire it**

The merger compares block JSON for equality (`JsonNode.equals`, i.e. structural). `locallyChangedBlocks` is maintained by the component: every `set`/`remove` through a binding or a bridge adds the block path; after a successful save and after a merge the set is cleared. Wiring: on a watcher callback, load the file into a temporary document and call `merge`; on any adopted/conflicted block fire per-path events; if `keptBlocks` is non-empty and no conflict, schedule a re-save (we are newer). A conflicted merge also schedules a save so the file converges.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsDocumentMergerTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsDocumentMerger.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeSettingsServiceComponent.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsDocumentMergerTest.java
git commit -m "feat(settings): per-block merge for multi-instance sync (#1345)"
```

### Task 10: 5.x → 6.0 storage migration with scope routing

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsMigration.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsMigrationTest.java`

**Interfaces:**
- Consumes: `SettingsDocument`, `SettingsDocumentIO`, `SettingsFileStore` (Tasks 2–4).
- Produces: `final class SettingsMigration`
  - `static void migrateLegacyProfile(Path profileDir, Predicate<String> registeredBlock, Map<String, SettingsScope> scopeByBlock, JIPipeProgressInfo progress)`
  - Reads `<profileDir>/settings.json` in the old shape (top-level = sheet ids, no `$schema`), prefixes registered and unregistered sheet ids with `sheet:`, folds each `settings-db-<key>.json` into `registry:<key>`, converts `recent-projects.txt` into `state:recent-projects`, routes each block to `APPLICATION` or `RUNTIME` per `scopeByBlock`, **defaulting to `APPLICATION`** when a block is absent from the map (Review Focus #7), writes `settings.json` + `runtime-settings.json` atomically, then moves the old files into `<profileDir>/settings-migration-backup/`.
  - `static boolean needsMigration(Path profileDir)` — true when `settings.json` exists without a `$schema` field, or when `settings-db-*.json` / `recent-projects.txt` still exist alongside a `$schema`-less or migrated file.
  - Idempotent: a second call with no legacy input is a no-op and does not touch the backup directory.

- [ ] **Step 1: Write the failing test**

```java
@Test void migratesSheetRegistryAndRecentProjects() throws Exception {
    Path p = tempDir;
    Files.writeString(p.resolve("settings.json"),
            "{\"org.hkijena.jipipe:general-ui\":{\"theme\":\"Dark\"}, \"unknown-sheet:1\":{\"a\":1}}");
    Files.writeString(p.resolve("settings-db-node-db.json"), "{\"pinned-node-ids\":[\"n1\"]}");
    Files.writeString(p.resolve("recent-projects.txt"), "/a/b.jip\n/c/d.jip\n");

    SettingsMigration.migrateLegacyProfile(p, block -> true,
            Map.of("registry:node-db", SettingsScope.APPLICATION), JIPipeProgressInfo.SILENT);

    JsonNode global = JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("settings.json")));
    assertEquals(2, global.get("$schema").asInt());
    assertEquals("Dark", global.get("sheet:org.hkijena.jipipe:general-ui").get("theme").asText());
    assertEquals(1, global.get("sheet:unknown-sheet:1").get("a").asInt(), "unknown sheet ids survive");
    assertEquals("n1", global.get("registry:node-db").get("pinned-node-ids").get(0).asText());
    assertEquals(2, global.get("state:recent-projects").size());
    assertTrue(Files.isDirectory(p.resolve("settings-migration-backup")));
    assertFalse(Files.exists(p.resolve("recent-projects.txt")), "legacy file must be moved, not left behind");
}

@Test void unambiguousBlocksDefaultToApplication() throws Exception {
    Path p = tempDir;
    Files.writeString(p.resolve("settings.json"), "{\"x\":{\"a\":1}}");
    SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);
    JsonNode global = JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("settings.json")));
    assertEquals(1, global.get("sheet:x").get("a").asInt());
}

@Test void secondRunIsANoOp() throws Exception {
    Path p = tempDir;
    Files.writeString(p.resolve("settings.json"), "{\"x\":{\"a\":1}}");
    SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);
    JsonNode first = JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("settings.json")));
    SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);
    assertEquals(first, JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("settings.json"))));
    assertFalse(SettingsMigration.needsMigration(p));
}

@Test void runtimeBlocksGoToRuntimeFile() throws Exception {
    Path p = tempDir;
    Files.writeString(p.resolve("settings.json"), "{\"org.hkijena.jipipe:runtime\":{\"num-threads\":4}}");
    SettingsMigration.migrateLegacyProfile(p, b -> true,
            Map.of("sheet:org.hkijena.jipipe:runtime", SettingsScope.RUNTIME), JIPipeProgressInfo.SILENT);
    JsonNode global = JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("settings.json")));
    JsonNode runtime = JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("runtime-settings.json")));
    assertFalse(global.has("sheet:org.hkijena.jipipe:runtime"), "runtime block must leave settings.json");
    assertEquals(4, runtime.get("sheet:org.hkijena.jipipe:runtime").get("num-threads").asInt());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsMigrationTest -q`
Expected: FAIL — `SettingsMigration` does not exist.

- [ ] **Step 3: Implement `SettingsMigration`**

`JIPipeProgressInfo.SILENT` already exists — verify before use; otherwise add a `SILENT` constant. The backup directory name is fixed (`settings-migration-backup`); existing files there are never overwritten — a name clash gets a `-1`, `-2`, … suffix. A `settings.json` that fails to parse is backed up as `.bad-<timestamp>` and treated as absent (no data destruction). `needsMigration` must be cheap (existence checks + one small read), since it runs at every startup.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsMigrationTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsMigration.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsMigrationTest.java
git commit -m "feat(settings): 5.x profile migration with scope routing (#1345)"
```

### Task 11: Cross-version carry-over / recovery merge

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeSettingsServiceComponent.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsCarryOver.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsCarryOverTest.java`

**Interfaces:**
- Consumes: `SettingsDocumentIO` (Task 3), `SettingsMigration` (Task 10), `SettingsDocumentMerger` (Task 9).
- Produces: `final class SettingsCarryOver`
  - `static Set<String> recoverUnknownBlocks(Path currentSettingsFile, Path profileBaseDir, String currentVersion, Predicate<String> registeredBlock)`
  - Scans sibling profile directories (excluding the current version), newest first, and merges in only blocks that are **unknown to this version** (pass-through) — never overriding a registered block (spec §3.3 step 2). Returns the set of recovered block paths.
  - Uses `StringUtils.compareVersions` for ordering and `VersionUtils.getJIPipeVersion()` for the current version.

- [ ] **Step 1: Write the failing test**

```java
@Test void recoversUnknownBlocksFromNewerProfile() throws Exception {
    Path base = tempDir.resolve("profiles");
    Path current = Files.createDirectories(base.resolve("6.0.0"));
    Path newer = Files.createDirectories(base.resolve("6.1.0"));
    Files.writeString(current.resolve("settings.json"),
            "{\"$schema\":2,\"sheet:org.hkijena.jipipe:general-ui\":{\"theme\":\"Dark\"}}");
    Files.writeString(newer.resolve("settings.json"),
            "{\"$schema\":2,\"sheet:org.hkijena.jipipe:general-ui\":{\"theme\":\"Light\"}," +
            "\"sheet:new-feature-x\":{\"on\":true}}");

    Set<String> recovered = SettingsCarryOver.recoverUnknownBlocks(
            current.resolve("settings.json"), base, "6.0.0", p -> !p.startsWith("sheet:new-feature-x"));

    assertEquals(Set.of("sheet:new-feature-x"), recovered);
    JsonNode global = JsonUtils.getObjectMapper().readTree(Files.readString(current.resolve("settings.json")));
    assertEquals("Dark", global.get("sheet:org.hkijena.jipipe:general-ui").get("theme").asText(),
            "a registered block must never be overridden by another version");
    assertTrue(global.get("sheet:new-feature-x").get("on").asBoolean());
}

@Test void noSiblingProfilesIsANoOp() {
    Path base = Files.createDirectories(tempDir.resolve("profiles/6.0.0"));
    Path f = base.resolve("settings.json");
    assertEquals(Set.of(), SettingsCarryOver.recoverUnknownBlocks(f, tempDir.resolve("profiles"), "6.0.0", p -> true));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsCarryOverTest -q`
Expected: FAIL — `SettingsCarryOver` does not exist.

- [ ] **Step 3: Implement `SettingsCarryOver` and wire it into startup**

Call it from `JIPipeSettingsServiceComponent` right after `SettingsMigration.migrateLegacyProfile` and before the first document load. Record recovered block paths so the UI can report "settings recovered from version X" once per session. A missing/unreadable sibling file is skipped silently (log only).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsCarryOverTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/settings/SettingsCarryOver.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeSettingsServiceComponent.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/settings/SettingsCarryOverTest.java
git commit -m "feat(settings): cross-version recovery merge for unknown blocks (#1345)"
```

### Task 12: Project scope document

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProjectSettingsDocument.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProject.java` (read/write the `"settings"` member through the new document)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/project/JIPipeProjectSettingsDocumentTest.java`

**Interfaces:**
- Consumes: `SettingsDocument`, `SettingsDocumentIO`, `SettingsDocumentMerger`.
- Produces: `final class JIPipeProjectSettingsDocument`
  - Wraps a `SettingsDocument` scoped `PROJECT` with the same `$schema`/pass-through rules.
  - `static JIPipeProjectSettingsDocument fromJson(JsonNode legacySettingsObject, Predicate<String> registeredBlock)` — reads both a legacy project `"settings"` object (top-level = sheet ids) and a new-shape object; the discriminator is `$schema`.
  - `ObjectNode toJson()`
  - Pass-through: unknown project sheet ids survive load→save.
  - `JIPipeProject` writes `toJson()` under the `"settings"` key (unchanged position) and drops its ad-hoc `unloadedSettingsSheets` round-tripping in favour of the document's pass-through.

- [ ] **Step 1: Write the failing test**

```java
@Test void legacyProjectSettingsBlockIsUpgradedWithPassThrough() throws Exception {
    JsonNode legacy = JsonUtils.getObjectMapper().readTree(
            "{\"org.hkijena.jipipe:project-io\":{\"format\":\"jipx\"},\"unknown-sheet\":{\"z\":1}}");
    JIPipeProjectSettingsDocument d = JIPipeProjectSettingsDocument.fromJson(legacy, p -> true);
    JsonNode out = d.toJson();
    assertEquals("jipx", out.get("sheet:org.hkijena.jipipe:project-io").get("format").asText());
    assertEquals(1, out.get("sheet:unknown-sheet").get("z").asInt());
    assertEquals(2, out.get("$schema").asInt());
}

@Test void newShapeIsReadAsIs() throws Exception {
    JsonNode in = JsonUtils.getObjectMapper().readTree("{\"$schema\":2,\"sheet:a\":{\"b\":1}}");
    assertEquals(1, JIPipeProjectSettingsDocument.fromJson(in, p -> true).toJson().get("sheet:a").get("b").asInt());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeProjectSettingsDocumentTest -q`
Expected: FAIL — the class does not exist.

- [ ] **Step 3: Implement the project document and switch `JIPipeProject`**

Keep `JIPipeProject`'s public `getSettingsSheets()`/`getSettingsSheet(...)` behavior (they still return the registered project sheet instances); only the JSON read/write path changes. Verify the existing project round-trip tests still pass after the switch.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeProjectSettingsDocumentTest -q` and `mvn test -pl jipipe-core -Dtest='*ProjectIo*' -q`
Expected: PASS for both.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProjectSettingsDocument.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/project/JIPipeProject.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/project/JIPipeProjectSettingsDocumentTest.java
git commit -m "feat(settings): project scope settings document with pass-through (#1345)"
```

### Task 13: Wire the service, replace the registry API with delegates

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeApplicationSettingsServiceComponent.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeRecentProjectsRegistry.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeSettingsServiceComponent.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/init/JIPipeServiceDefaultInitializer.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/JIPipe.java` (add the `getSettingsService()` delegate accessor)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/service/SettingsDelegateTest.java`

**Interfaces:**
- Consumes: Task 7–12.
- Produces: existing APIs keep their signatures, all backed by the new engine:
  - `JIPipeApplicationSettingsServiceComponent.register(sheet)` → also `settingsService.attachSheet(sheet)`
  - `getById`/`getByType`/`getRegisteredSheets` unchanged
  - `save()`/`saveLater()`/`reload()` → delegate to `JIPipeSettingsServiceComponent`
  - `putIntoRegistry(db, key, value)` → `document(APPLICATION|RUNTIME).set("registry:" + db, keyAsSlashPath, valueToTree(value))`
  - `getFromRegistry(db, key, type, default, destructive)` → binding-style read on `registry:<db>`; the `destructive` flag maps to "write the default when absent" (true) vs "leave absent" (false), preserving today's semantics exactly
  - `getListFromRegistry(db, key, type, destructive)` → analogous, with an empty list default
  - `JIPipeRecentProjectsRegistry` → `state:recent-projects` in the **application** document; `List<Path>` API, event emitter, `add`/`removeAll`/`clear`/`cleanup`/`migrateFromLegacy` signatures unchanged; `migrateFromLegacy` becomes a no-op when the migration already ran (Task 10 moved the data)
  - `getChangedEventEmitter()` on the old component re-emits the new aggregate event

- [ ] **Step 1: Write the failing test**

```java
// jipipe-launcher module: this test boots the real service (JIPipe.ensureInstance).
class SettingsDelegateTest {
    @Test void registryRoundTripThroughDocument() {
        JIPipe.ensureInstance();
        JIPipe.getSettings().putIntoRegistry("uitest", JIPipePathMetadataStore.key("a", "b"), Boolean.TRUE);
        assertTrue(JIPipe.getSettings().getFromRegistry("uitest", JIPipePathMetadataStore.key("a", "b"),
                Boolean.class, false, false));
        assertTrue(JIPipe.getSettingsService().document(SettingsScope.APPLICATION)
                .has("registry:uitest", "a/b"), "registry value must live in the new document");
    }

    @Test void recentProjectsUseTheDocument() {
        JIPipe.ensureInstance();
        Path p = Paths.get("/tmp/recent-test.jip");
        JIPipe.getRecentProjects().add(p);
        assertTrue(JIPipe.getSettingsService().document(SettingsScope.APPLICATION)
                .get("state:recent-projects", "0").asText().endsWith("recent-test.jip"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-launcher -Dtest=SettingsDelegateTest -q`
Expected: FAIL — `JIPipe.getSettingsService()` does not exist.

- [ ] **Step 3: Implement the delegates and startup wiring**

Add `JIPipe.getSettingsService()` returning the new component. In `JIPipeServiceDefaultInitializer`, replace `getApplicationSettings().reload()` with: run migration + carry-over (via the new component), then `getSettingsService().reload()`, then `getApplicationSettings().reload()` becomes a delegate call (keep the call site working). Keep the existing ordering so `createDefaultImporterSettings()` etc. still run before the load. `JIPipe.getSettings().getRegisteredSheets()` must still return the registered sheets (used at `JIPipe.java:484` for the runtime temp dir) — sheet registration stays in the old component and is forwarded to the bridge.

Also wire the sheet bridge for the existing sheets so `XxxApplicationSettings.getInstance()` keeps working unchanged: `JIPipeApplicationSettingsServiceComponent.register(sheet)` must call `settingsService.attachSheet(sheet)`, and `registerApplicationSettingsSheet(...)` in `JIPipeDefaultJavaPlugin` already routes through `register(...)`, so all 35 sheets across every plugin attach without touching a single sheet class. `reload()` must re-attach bridges (the bridge holds a document reference; after a document reload the bridge must re-read).

- [ ] **Step 3b: Write the sheet-instance regression test (part of the same test class)**

```java
@Test void registeredSheetInstanceStaysLiveAfterReload() {
    JIPipe.ensureInstance();
    JIPipeGeneralUIApplicationSettings sheet = JIPipe.getSettings()
            .getById(JIPipeGeneralUIApplicationSettings.ID, JIPipeGeneralUIApplicationSettings.class);
    assertNotNull(sheet, "sheet registration and getInstance() must keep working");
    sheet.setTheme(JIPipeDesktopUITheme.Modern);   // or the theme enum type the class actually uses
    JIPipe.getSettingsService().save();
    JIPipe.getSettingsService().reload();
    assertEquals(JIPipeDesktopUITheme.Modern, sheet.getTheme(),
            "a registered sheet must read back the value written through the document");
}

@Test void everyRegisteredSheetHasADocumentBlock() {
    JIPipe.ensureInstance();
    SettingsDocument doc = JIPipe.getSettingsService().document(SettingsScope.APPLICATION);
    for (String sheetId : JIPipe.getSettings().getRegisteredSheets().keySet()) {
        assertTrue(doc.hasBlock("sheet:" + sheetId), "missing bridge for sheet " + sheetId);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-launcher -Dtest=SettingsDelegateTest -q`
Expected: PASS. Then run the core gate: `mvn test -pl jipipe-core -q` — expected PASS (no settings regression in existing tests).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeApplicationSettingsServiceComponent.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeRecentProjectsRegistry.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/service/init/JIPipeServiceDefaultInitializer.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/JIPipe.java \
        jipipe-launcher/src/test/java/org/hkijena/jipipe/api/service/SettingsDelegateTest.java
git commit -m "feat(settings): delegate registry and recent projects to the new engine (#1345)"
```

---

# Phase 2 — Consumers on the live stream

### Task 14: Scope assignment for all existing settings

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/SettingsScopes.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/init/JIPipeServiceDefaultInitializer.java` (pass the map to migration)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/settings/SettingsScopesTest.java`

**Interfaces:**
- Produces: `final class SettingsScopes` — `static Map<String, SettingsScope> scopeByBlock()` returning an immutable map of every **runtime-scoped** block (block path → `RUNTIME`); anything absent is `APPLICATION`. The map contains at minimum the runtime execution blocks: `sheet:org.hkijena.jipipe:runtime`, `sheet:org.hkijena.jipipe:hardware-acceleration`, `registry:ui-run`, and the artifact/acceleration registry keys.
- Registering a key with `scope(RUNTIME)` also registers its block in this map (the component consults `registeredKeys()` before the static map, so keys are the primary source of truth and the static map is the fallback for not-yet-keyified blocks).

- [ ] **Step 1: Write the failing test**

```java
class SettingsScopesTest {
    @Test void runtimeExecutionBlocksAreRuntimeScoped() {
        Map<String, SettingsScope> m = SettingsScopes.scopeByBlock();
        assertEquals(SettingsScope.RUNTIME, m.get("sheet:org.hkijena.jipipe:runtime"));
        assertEquals(SettingsScope.RUNTIME, m.get("sheet:org.hkijena.jipipe:hardware-acceleration"));
    }

    @Test void uiBlocksAreNotRuntimeScoped() {
        assertFalse(SettingsScopes.scopeByBlock().containsKey("sheet:org.hkijena.jipipe:general-ui"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsScopesTest -q`
Expected: FAIL — `SettingsScopes` does not exist.

- [ ] **Step 3: Implement `SettingsScopes` and the access-policy check**

Derive the initial content from the spec's ownership table: runtime = thread pool, temp/cache directories, environment installations, hardware acceleration, artifact directories; application = everything UI and user preference (theme, file chooser, node browser pins, recent projects, notifications, backups, downloads, statistics, instrumentation server, AI, extensions); project = the existing project sheets. Anything the implementer cannot confidently classify stays `APPLICATION` (Review Focus #7) and gets a `TODO(#1345)` comment listing it for a follow-up sweep.

Also implement the **access-policy enforcement seam** on `JIPipeSettingsServiceComponent`, which is the hook the future runtime pool needs (spec §3.1 step 2) and which must exist now so keys are declared correctly from the start:

- `boolean canWrite(JIPipeSettingKey<?> key)` — `RUNTIME_ADMIN` and `READ_ONLY` keys are writable while the runtime document is local (there is no remote admin yet), so this returns `true` today; the method exists as the single choke point that the #1102 runtime host will override.
- `void setWriteGuard(Predicate<JIPipeSettingKey<?>> guard)` — replaces the policy check; a rejected write throws `SettingsAccessDeniedException` (a new `RuntimeException` subclass) and leaves the document untouched.
- `SettingsBinding.set` and the sheet bridge both route through `canWrite`, so no consumer can bypass the guard.

- [ ] **Step 3b: Write the access-policy test (part of the same test class)**

```java
@Test void writeGuardBlocksRejectedKey() {
    JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
    JIPipeSettingKey<Boolean> k = JIPipeSettingKey.bool("registry:x", "a", Boolean.class)
            .defaultValue(true).access(SettingsAccessPolicy.RUNTIME_ADMIN);
    c.registerKey(k);
    c.setWriteGuard(key -> false);   // simulate a remote runtime that denies writes
    assertThrows(SettingsAccessDeniedException.class, () -> c.bind(k).set(false));
    assertTrue(c.bind(k).get(), "a denied write must not change the value");
}

@Test void allKeysAreWritableWhenNoGuardIsInstalled() {
    JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
    JIPipeSettingKey<Boolean> k = JIPipeSettingKey.bool("registry:x", "a", Boolean.class)
            .defaultValue(true).access(SettingsAccessPolicy.READ_ONLY);
    c.registerKey(k);
    c.bind(k).set(false);   // local runtime: READ_ONLY is not enforced yet
    assertFalse(c.bind(k).get());
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsScopesTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/SettingsScopes.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/plugins/settings/SettingsScopesTest.java
git commit -m "feat(settings): scope assignment for existing settings blocks (#1345)"
```

### Task 15: Keyify the registry consumers (desync fix)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/SettingsKeys.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorUI.java:225-230`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/pipeline/JIPipeDesktopPipelineGraphEditorUI.java:493-527`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/compartments/JIPipeDesktopCompartmentsGraphEditorUI.java:364-392`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/nodefinder/JIPipeDesktopNodeFinderDialogUI.java:264`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/addnodepanel/JIPipeDesktopNodeBrowser.java:1081-1100`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/entries/NodeEntryKind.java:178-217`
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/settings/SettingsBindingLiveUpdateTest.java`

**Interfaces:**
- Consumes: `JIPipeSettingKey`, `SettingsBinding`, `JIPipe.getSettingsService()` (Tasks 5, 13).
- Produces: `final class SettingsKeys` — public static final `JIPipeSettingKey` constants with stable literal IDs:
  - `SHOW_DOCK_TOOLBAR_LABELS` — `registry:ui-graph-editor` / `any/dock/show-labels`, `Boolean`, `true`, `APPLICATION`
  - `PIPELINE_DOCK_STATE` — `registry:ui-graph-editor` / `pipeline/dock-state`, `JIPipeDesktopDockPanel.State`, new default state
  - `COMPARTMENTS_DOCK_STATE` — `registry:ui-graph-editor` / `compartments/dock-state`, same type
  - `DOCK_LAYOUT_TEMPLATES` — `registry:ui-dock` / `layouts`, `StringAndStringPairParameterList`
  - `PINNED_NODE_IDS` — `registry:node-db` / `pinned-node-ids`, `List<String>` via `object(...)`
  - `NODE_BROWSER_KINDS` — `registry:node-db` / `node-browser-kinds`, `List<String>`
  - each call site replaces a one-shot `getFromRegistry` + `putIntoRegistry` pair with a single `SettingsBinding` created once, subscribed for live updates, and written through on user action.

- [ ] **Step 1: Write the failing test**

```java
// Boots the service so the real component is available.
class SettingsBindingLiveUpdateTest {
    @Test void togglingOneGraphEditorsSettingUpdatesAnotherView() {
        JIPipe.ensureInstance();
        SettingsBinding<Boolean> a = JIPipe.getSettingsService().bind(SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS);
        SettingsBinding<Boolean> b = JIPipe.getSettingsService().bind(SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS);
        List<Boolean> seen = new ArrayList<>();
        b.subscribe(seen::add);
        a.set(!a.get());
        assertEquals(List.of(b.get()), seen, "a second view of the same setting must be notified");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-launcher -Dtest=SettingsBindingLiveUpdateTest -q`
Expected: FAIL — `SettingsKeys` does not exist.

- [ ] **Step 3: Implement `SettingsKeys` and migrate the call sites**

Each migrated site keeps its current default and behavior; only the read/write path changes. `JIPipeDesktopGraphEditorUI` binds `SHOW_DOCK_TOOLBAR_LABELS`, applies it to `dockPanel.setShowToolbarLabels(...)`, subscribes for live updates, and writes on the parameter-changed event (guarding against echo with the binding's unchanged-value rule). `NodeEntryKind.loadKinds`/`saveKinds` and the node browser's pin list use the same pattern. Do **not** change the stored keys — the block/sub-path pairs must match today's `getFromRegistry` arguments exactly so existing profiles keep their values.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-launcher -Dtest=SettingsBindingLiveUpdateTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/SettingsKeys.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/ \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/entries/NodeEntryKind.java \
        jipipe-launcher/src/test/java/org/hkijena/jipipe/settings/SettingsBindingLiveUpdateTest.java
git commit -m "feat(settings): bind registry consumers to the live change stream (#1345)"
```

### Task 16: Keyify the remaining registry consumers (palettes, run queue logger)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/color/palette/JIPipeDesktopRegistryBackedColorPaletteUserColorStorage.java:44-79`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/running/queue/JIPipeDesktopRunQueueLoggerPanel.java:51-106`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/SettingsKeys.java`
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/settings/SettingsPaletteKeysTest.java`

**Interfaces:**
- Produces: additional `SettingsKeys` constants:
  - `USER_PALETTE_COLORS_FG` / `_FG_A` / `_FG_BG` / `_FG_BG_A` — `registry:ui-palette` / `user-palette-colors/<variant>`, `object(List.class)`
  - `RUN_QUEUE_SHOW_LOG` — `registry:ui-general` / `logger-panel/show-log`, `Boolean`, `true`

- [ ] **Step 1: Write the failing test**

```java
@Test void paletteStorageUsesTheDocument() {
    JIPipe.ensureInstance();
    SettingsBinding<JsonNode> b = JIPipe.getSettingsService().bind(SettingsKeys.USER_PALETTE_COLORS_FG);
    b.set(JsonUtils.toJsonNode(List.of("#ff0000")));
    assertEquals("#ff0000", JIPipe.getSettingsService().document(SettingsScope.APPLICATION)
            .get("registry:ui-palette", "user-palette-colors/fg").get(0).asText());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-launcher -Dtest=SettingsPaletteKeysTest -q`
Expected: FAIL — the constants do not exist.

- [ ] **Step 3: Implement the constants and migrate the two consumers**

Preserve the exact sub-path format (`user-palette-colors/fg`, etc. via `JIPipePathMetadataStore.key(...)`) so existing stored palettes load.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-launcher -Dtest=SettingsPaletteKeysTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/SettingsKeys.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/color/palette/JIPipeDesktopRegistryBackedColorPaletteUserColorStorage.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/running/queue/JIPipeDesktopRunQueueLoggerPanel.java \
        jipipe-launcher/src/test/java/org/hkijena/jipipe/settings/SettingsPaletteKeysTest.java
git commit -m "feat(settings): keyify palette and run-queue settings consumers (#1345)"
```

### Task 17: Golden-profile integration test and full gate

**Files:**
- Create: `jipipe-launcher/src/test/java/org/hkijena/jipipe/settings/SettingsMigrationIntegrationTest.java`
- Create: `jipipe-launcher/src/test/resources/settings-fixtures/legacy-5x-profile/` (fixture files)
- Create: `jipipe-launcher/src/test/resources/settings-fixtures/unknown-sheet-profile/`
- Test: as above.

**Interfaces:**
- Consumes: everything in Phase 1.
- Produces: fixtures and tests that prove the spec's success criteria 1 and 5:
  - a 5.x-shaped profile (old `settings.json` + two `settings-db-*.json` + `recent-projects.txt`) migrates with no loss;
  - a 6.x profile containing an unknown sheet block and an unknown sub-path inside a known block round-trips through a save with no loss;
  - a profile where the process is killed mid-write (simulated: a leftover `settings.json.tmp.*` plus the previous settings file) loads cleanly and cleans up.

- [ ] **Step 1: Write the failing test**

```java
// Uses a JIPIPE_OVERRIDE_USER_DIR_BASE-like injection: the test builds a temp profile base and points
// the service at it via JIPipeSettingsServiceComponent.forTesting-style injection or the initialization
// settings override, then asserts on the files on disk.
@Test void legacyProfileMigratesWithoutLoss() throws Exception { /* assertions per Interfaces */ }
@Test void unknownBlocksSurviveASaveCycle() throws Exception { /* assertions per Interfaces */ }
@Test void leftoverTempFileAndPreviousFileLoadCleanly() throws Exception { /* assertions per Interfaces */ }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-launcher -Dtest=SettingsMigrationIntegrationTest -q`
Expected: FAIL — fixtures and test do not exist.

- [ ] **Step 3: Add fixtures and implement the test**

Classify the fixtures as a single-source ship set (this plan owns every byte), so exact-count assertions on fixture content are allowed here (Global Constraints). The test must not depend on the developer's real profile — point the user-dir base at a temp directory via `JIPipeServiceInitializationSettings.setOverrideUserDirBase(...)` before `ensureInstance()`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-launcher -Dtest=SettingsMigrationIntegrationTest -q` then the full settings gate `mvn test -pl jipipe-core,contrib/jipipe-ro-crate-java-2.1.0 -q` then `mvn test -pl jipipe-launcher -q`
Expected: PASS for all.

- [ ] **Step 5: Commit**

```bash
git add jipipe-launcher/src/test/java/org/hkijena/jipipe/settings/SettingsMigrationIntegrationTest.java \
        jipipe-launcher/src/test/resources/settings-fixtures/
git commit -m "test(settings): golden-profile migration and pass-through integration tests (#1345)"
```

---

# Phase 3 — The settings UI

### Task 18: Application settings tab shell (tree, search, scope switcher)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopSettingsTab.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/SettingsTreeModel.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java:1107-1159` (replace `openApplicationSettings` with `openSettingsTab`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/settings/SettingsTreeModelTest.java`

**Interfaces:**
- Consumes: `JIPipeSettingsServiceComponent` (Task 7), `SettingsScopes` (Task 14), the 35 registered sheets.
- Produces:
  - `SettingsTreeModel` — pure (no Swing) view model: `SettingsTreeModel(Map<SettingsScope, Map<String, List<SheetNode>>>)`, `List<CategoryNode> categories(SettingsScope)`, `List<SheetNode> search(String query, SettingsScope)`; a `SheetNode` carries sheet id, display name, category, scope, and whether the scope is editable. Search matches name, description, and block path, case-insensitively.
  - `JIPipeDesktopSettingsTab extends JIPipeDesktopWorkbenchPanel` — builds the tab: top bar (search field + scope `JTabbedPane`-style switcher `Application | Project | Runtime`), left `JTree`, right content pane (`JIPipeDesktopParameterFormPanel` for a sheet section), and a live-update banner.
  - `openSettingsTab(SettingsScope scope, String sectionSearch)` on the workbench replaces `openApplicationSettings(String)`; the ~15 existing deep-link call sites keep working because the old method remains as a delegating overload.

- [ ] **Step 1: Write the failing test**

```java
class SettingsTreeModelTest {
    @Test void searchIsCaseInsensitiveAndMatchesBlockPath() {
        SettingsTreeModel m = model();
        assertEquals(List.of("org.hkijena.jipipe:general-ui"), ids(m.search("GENERAL-UI", SettingsScope.APPLICATION)));
        assertEquals(List.of("org.hkijena.jipipe:general-ui"), ids(m.search("theme", SettingsScope.APPLICATION)));
    }

    @Test void sectionsOfOtherScopesArePresentButNotEditable() {
        SettingsTreeModel m = model();
        SheetNode runtimeSheet = m.nodeById("org.hkijena.jipipe:runtime");
        assertFalse(m.isEditable(runtimeSheet, SettingsScope.PROJECT));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsTreeModelTest -q`
Expected: FAIL — `SettingsTreeModel` does not exist.

- [ ] **Step 3: Implement the model and the tab shell**

Keep all Swing construction in the tab class and all logic in `SettingsTreeModel` so the model is testable without the EDT. The tab is a workbench tab (registered like the Welcome tab), not a `JDialog`. Reuse `JIPipeDesktopParameterFormPanel` exactly as `JIPipeDesktopApplicationSettingsUI` does today. Sections belonging to a non-active scope render read-only with a hint label.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsTreeModelTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/ \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/settings/SettingsTreeModelTest.java
git commit -m "feat(settings-ui): settings tab shell with search and scope switcher (#1345)"
```

### Task 19: Merged Appearance / Accessibility sections, live preview, per-section reset

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopAppearanceSection.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopSettingsTab.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java` (remove the `JIPipeDesktopCustomizerDialog` call sites)
- Delete: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/customizer/JIPipeDesktopCustomizerDialog.java` (move `ThemePreviewPanel` to `desktop/app/settings/`; `JIPipeDesktopThemeManager` stays)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/settings/AppearanceSectionModelTest.java`

**Interfaces:**
- Consumes: `SettingsTreeModel` (Task 18), `SettingsSheetBridge` (Task 6).
- Produces:
  - `JIPipeDesktopAppearanceSection` — composes the merged Appearance content: theme, theme style, UI scale, reduced motion (from `JIPipeGeneralUIApplicationSettings`), file chooser type + fallback (from `JIPipeFileChooserApplicationSettings`), plus the moved `ThemePreviewPanel`; edits write through the sheet bridge (immediate apply, no Save button).
  - A `SectionHeader` component: sheet name, description, **modified** badge (bound to `hasAnyModifiedParameter()`-style check over the block), **Reset section** button → removes the block's registered sub-paths from the document (per-key reset, spec §7).
  - An `Accessibility` section holding reduced motion + UI scale.
  - A `Keyboard` placeholder section (empty state text "Shortcut profiles will be available here (#1346)").
  - An `AppearanceSectionModel` (pure) exposing `boolean isModified()`, `void resetAll()`, `List<String> sectionIds()` for unit testing without Swing.

- [ ] **Step 1: Write the failing test**

```java
class AppearanceSectionModelTest {
    @Test void resetRestoresDefaultsAndClearsModified() {
        // build a document with a modified theme, wrap in AppearanceSectionModel, resetAll, assert
        assertFalse(model.isModified());
        assertEquals("Modern", themeBinding.get());
    }

    @Test void mergedSectionsContainThemeAndFileChooserEntries() {
        assertTrue(model.sectionIds().containsAll(List.of("appearance", "accessibility", "keyboard")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=AppearanceSectionModelTest -q`
Expected: FAIL — `AppearanceSectionModel` does not exist.

- [ ] **Step 3: Implement the merged section and remove the old dialog**

Per-project settings UI (`JIPipeDesktopProjectSettingsUI`) stays as a consumer but is reached through the same tab with the Project scope (spec §7 point 4); if that merge is not trivially reachable in this task, keep the existing project-settings dialog working and add the Project scope page to the tab — the two must show the same underlying document. Delete only the Customize JIPipe dialog; keep `JIPipeDesktopThemeManager` (used elsewhere).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=AppearanceSectionModelTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/ \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java
git rm jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/customizer/JIPipeDesktopCustomizerDialog.java
git commit -m "feat(settings-ui): merged appearance/accessibility sections with live reset (#1345)"
```

### Task 20: Raw settings editor (`about:config`)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopRawSettingsSection.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/RawSettingsTableModel.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/settings/RawSettingsTableModelTest.java`

**Interfaces:**
- Consumes: `SettingsDocument` (Task 2), `JIPipeSettingsServiceComponent` (Task 7).
- Produces:
  - `RawSettingsTableModel` — pure model over a document: `List<RawRow> rows(String filter)` where `RawRow` has `path`, `valuePreview`, `typeLabel`, `isPassThrough`, `isDocumented`, `defaultText`; `void setValue(String path, String rawJson)` (validating); `void resetKey(String path)`.
  - `JIPipeDesktopRawSettingsSection` — a table with a filter box, a warning banner, double-click editing (typed dialog for documented keys, JSON text editor for pass-through/unknown), per-key reset, and a scope selector matching the tab's.

- [ ] **Step 1: Write the failing test**

```java
class RawSettingsTableModelTest {
    @Test void listsDocumentedAndPassThroughKeys() {
        RawSettingsTableModel m = modelWith(docWithKeyAndPassThrough());
        assertTrue(m.rows("").stream().anyMatch(RawRow::isPassThrough));
        assertTrue(m.rows("").stream().anyMatch(RawRow::isDocumented));
    }

    @Test void filterMatchesPathSubstring() {
        assertEquals(1, modelWith(docWithKeyAndPassThrough()).rows("show-labels").size());
    }

    @Test void invalidJsonIsRejected() {
        RawSettingsTableModel m = modelWith(docWithKeyAndPassThrough());
        assertThrows(IllegalArgumentException.class, () -> m.setValue("registry:x/a", "{not json"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=RawSettingsTableModelTest -q`
Expected: FAIL — `RawSettingsTableModel` does not exist.

- [ ] **Step 3: Implement the model and the section**

`setValue` parses with `JsonUtils.getObjectMapper().readTree` and rethrows as `IllegalArgumentException` with a useful message; on success it applies through the component with `ChangeOrigin.RAW_EDIT` (so the change is broadcast and saved like any other). Pass-through rows are editable but marked. Documented keys show their type, default, and description.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=RawSettingsTableModelTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopRawSettingsSection.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/RawSettingsTableModel.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/settings/RawSettingsTableModelTest.java
git commit -m "feat(settings-ui): raw about:config-style settings editor (#1345)"
```

### Task 21: Live-sync banner and changes strip

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/SettingsChangeLog.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopSettingsTab.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/settings/SettingsChangeLogTest.java`

**Interfaces:**
- Consumes: `SettingsChangedEvent` (Task 7), `SettingsDocumentMerger` (Task 9).
- Produces:
  - `SettingsChangeLog` — pure: `void record(SettingsChangedEvent e, String formattedValue)`, `List<ChangeEntry> entries()`, `void clear()`, `int size()`; entries carry scope, path, formatted old/new value, origin, and whether the origin was this process.
  - The tab shows a banner ("Settings were updated by another JIPipe instance") when `hasForeignConflict()`, and a collapsible changes strip listing since-open changes with per-entry reset.

- [ ] **Step 1: Write the failing test**

```java
class SettingsChangeLogTest {
    @Test void recordsOnlyForeignOriginsInTheForeignList() {
        SettingsChangeLog log = new SettingsChangeLog();
        log.record(new SettingsChangedEvent(scope, "registry:x/a", null, node(1), ChangeOrigin.LOCAL_EDIT), "1");
        log.record(new SettingsChangedEvent(scope, "registry:x/b", null, node(2), ChangeOrigin.EXTERNAL_PROCESS), "2");
        assertEquals(2, log.size());
        assertEquals(1, log.entries().stream().filter(ChangeEntry::isForeign).count());
    }

    @Test void clearEmptiesEntries() {
        SettingsChangeLog log = new SettingsChangeLog();
        log.record(new SettingsChangedEvent(scope, "a", null, node(1), ChangeOrigin.LOCAL_EDIT), "1");
        log.clear();
        assertTrue(log.entries().isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=SettingsChangeLogTest -q`
Expected: FAIL — `SettingsChangeLog` does not exist.

- [ ] **Step 3: Implement the change log and bind it into the tab**

`isForeign` is true for `EXTERNAL_FILE` and `EXTERNAL_PROCESS`. The strip caps at 200 entries (drop oldest) so a pathological stream cannot grow without bound.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=SettingsChangeLogTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/SettingsChangeLog.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopSettingsTab.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/settings/SettingsChangeLogTest.java
git commit -m "feat(settings-ui): live-sync banner and change strip (#1345)"
```

### Task 22: Remove the modal settings UI and finish the wiring

**Files:**
- Delete: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopApplicationSettingsUI.java`
- Modify: every remaining `openApplicationSettings(...)` caller (use `git grep -l "openApplicationSettings"`) to `openSettingsTab(...)`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java`
- Test: `jipipe-launcher/src/test/java/org/hkijena/jipipe/settings/SettingsNoModalUiTest.java`

**Interfaces:**
- Consumes: Task 18.
- Produces: `openSettingsTab(SettingsScope scope, String search)` as the only entry point; `openApplicationSettings(String category)` removed; no `JDialog`-based settings UI remains.

- [ ] **Step 1: Write the failing test**

```java
// A structural existence gate, not a Swing interaction test (AGENTS.md): the removed types must be gone
// and the new entry point must exist.
@Test void modalSettingsAndCustomizerUiAreRemoved() {
    assertThrows(ClassNotFoundException.class,
            () -> Class.forName("org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopApplicationSettingsUI"));
    assertThrows(ClassNotFoundException.class,
            () -> Class.forName("org.hkijena.jipipe.desktop.app.customizer.JIPipeDesktopCustomizerDialog"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-launcher -Dtest=SettingsNoModalUiTest -q`
Expected: FAIL — the classes still exist.

- [ ] **Step 3: Delete the modal UI and migrate the call sites**

Run `git grep -n "openApplicationSettings"` and `git grep -n "JIPipeDesktopApplicationSettingsUI"` first; every remaining reference must be updated. The deep links navigate the tab to a section rather than opening a dialog.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-launcher -Dtest=SettingsNoModalUiTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A jipipe-core/src/main/java/org/hkijena/jipipe/desktop/
git commit -m "refactor(settings-ui): remove modal settings and customizer dialogs (#1345)"
```

### Task 23: Whole-branch verification

**Files:**
- Create: `docs/superpowers/plans/2026-10-27-modern-settings-system-verification.md`
- Test: the full gate suite.

**Interfaces:**
- Consumes: all tasks.
- Produces: a verification record mapping each of the spec's §11 success criteria to a test or a manual check, with the exact command and observed result.

- [ ] **Step 1: Run the full build**

Run: `mvn clean install -q`
Expected: BUILD SUCCESS.

- [ ] **Step 2: Run the settings-focused tests**

Run: `mvn test -pl jipipe-core -Dtest='*Settings*,Settings*' -q` and `mvn test -pl jipipe-launcher -Dtest='*Settings*' -q`
Expected: all PASS.

- [ ] **Step 3: Record each success criterion**

Write `2026-10-27-modern-settings-system-verification.md` with, per spec §11 criterion 1–7: the test name (or the manual step), the command, and the observed result.

- [ ] **Step 4: Verify no legacy settings file is still written**

Run: `git grep -n "settings-db-" -- jipipe-core/src/main` and `git grep -n "recent-projects.txt" -- jipipe-core/src/main`
Expected: only the migration code (Task 10/13) references them; no runtime write path remains.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/plans/2026-10-27-modern-settings-system-verification.md
git commit -m "test(settings): whole-branch verification record (#1345)"
```