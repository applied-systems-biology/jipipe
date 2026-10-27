# Modernized Application Settings System — Design

**Work item:** [#1345](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1345)
**Related:** #1346 (keyboard shortcuts — plugs in as a settings section), #1102 (runtime & cache server — advances the `runtime` scope), #701 (live collaboration — same sync mechanism)
**Status:** approved design, 2026-10-27 (rev. 2 — three live scopes: application, project, runtime)
**Breaking-change window:** 6.0.0 is in development; 5.3.0 is the last stable. The UI and the on-disk format may change without back-compat to 5.x *profiles* (old profiles are still readable/migratable; see §8).

## 1. Problem

Today, application settings are handled by three independent systems:

1. **Sheets** — 35 `JIPipeApplicationSettingsSheet` parameter collections, saved into `settings.json` (one block per registered sheet ID).
2. **Registry** — ~15 ad-hoc settings (dock layouts, pinned nodes, color palettes, node-browser kinds) in `settings-db-<key>.json` files via `JIPipePathMetadataStore`, accessed through `putIntoRegistry`/`getFromRegistry`/`getListFromRegistry`.
3. **Recent projects** — `JIPipeRecentProjectsRegistry`, a separate service component with its own `recent-projects.txt`.

Confirmed defects in the current code:

- **Fragile across versions.** The profile directory is `<base>/<version>/` (`PathUtils.getJIPipeUserDir`). Profile carry-over *does* exist: `JIPipeServiceInitializer.applyProfileUpgrades()` copies the newest older profile dir into the empty current one on first launch (upgrades only, skipped once the current dir is non-empty). But: (a) dev builds and releases share a version string (`VersionUtils.getJIPipeVersion()` strips `-SNAPSHOT`), so they write to the **same** profile dir — when the user then runs the release (a downgrade relative to the dev build), `settings.json` is rewritten from the *registered* sheets only on save and blocks written by the dev build are **silently cleared**; (b) the copy is a whole-directory move that conflates settings with caches/templates; (c) there is no reverse migration, so settings changed while running an older version are lost when the newer version is run again (the newer profile dir is non-empty, so no copy happens).
- **Three systems, no shared semantics.** Different file formats, different save triggers, different (or missing) change events. UI reads registry settings once at component init and never learns about later changes: e.g. `JIPipeDesktopGraphEditorUI` reads `any/dock/show-labels` once (`:225`) and writes it back on change (`:228`), so a second open graph editor is desynced.
- **In-memory-til-Save UX.** Sheet edits live in the sheet object until the settings dialog's *Save* is pressed; *Reset/Cancel* re-read the file. A crash or window close loses the session's settings work.
- **No multi-instance story.** Multiple JIPipe processes share the same file; last writer wins, no notification, no safe write (direct `FileWriter`, not atomic).
- **Not ready for runtime isolation** (#1102): settings are an inseparable tangle of sheets, registry, and ad-hoc files with no scoped, transferable document — there is no clean boundary between user-interface, project, and execution configuration, so a future runtime process would have to be handed the whole settings blob (including UI state and recent projects).
- **No reset-to-defaults** for any setting or section (only "re-read saved file").
- **Settings are hard to find** (modal dialog, tree of 35 sheets, plus a second "Customize JIPipe" modal for theme/accessibility, plus project settings in a third place).

## 2. Goals & non-goals

**Goals**

- One unified, scoped settings **document** with one API, one event bus, one storage format — replacing all three systems.
- **Stable across versions:** unknown keys/IDs are passed through byte-for-byte; upgrading or downgrading never silently clears settings.
- **Multi-instance safe:** two or more JIPipe processes on one machine see each other's changes live, without corruption.
- **Runtime-ready:** application, project, and runtime configuration are three live scopes today; isolating the runtime into its own process (#1102 step 1) and later onto a remote pool (step 2) moves *where* the runtime document lives, not how settings are defined, scoped, observed, or gated.
- **Live bindings:** any feature (graph editor, dock panel, node browser) observes a setting and updates when it changes *anywhere* — same process, other process, future remote runtime.
- **Modern UI:** non-modal settings tab, global search, merged "Customize JIPipe" + application settings + project settings, per-key/per-section reset, and a raw `about:config`-style editor.

**Non-goals (this work item)**

- Remote/cloud sync across *machines* (the multi-instance requirement is per-machine, multiple processes). VS-Code-style account sync can reuse this document later.
- The keyboard-shortcut feature itself (#1346) — this work only guarantees the settings system can host it (a registered section + keys).
- The runtime *process split* itself (#1102) — this work makes `runtime` a live scope and shapes it for the split; hosting it in another process and permission-gated remote pools come with #1102.
- Project *file* format changes beyond the settings block (the `.jip`/`.jipx` structure stays).

## 3. Data model & file format

### 3.1 Scopes

A **scope** is a self-contained settings document:

| Scope | Location | Notes |
|---|---|---|
| `application` | profile dir: `<profile>/settings.json` | user-interface / user-preference state; per user; shared by all processes of that profile |
| `project` | inside the project file (`"settings"` member, position unchanged) | per project; embedded in `.jip`/`.jipx` |
| `runtime` | profile dir: `<profile>/runtime-settings.json` (own file) | execution state: threads, environments, temp/cache dirs, acceleration; per machine and per JIPipe install, not part of the project |

All three are **live from day one** — `runtime` is not a placeholder for a future server. Today it is a separate document in the same profile, loaded and saved by the same engine; the future split (#1102) changes only *where the document is materialized* and *who is allowed to change it*, never its shape or its API:

- **Step 1 (planned):** the runtime is hosted in its own process; its scope document is transferred over the local connection at startup and observed/diffed with the same change-event stream (§4.4). Client-side UI for runtime settings becomes a *view* onto the runtime document; the runtime process owns the writer for it.
- **Step 2 (further future):** a remote runtime pool. This is where **permission gating** comes in: runtime-relevant settings are declared with an access policy on the key (`user-editable`, `runtime-admin`, `read-only`), so the UI can show an administrator-managed runtime's settings as read-only (or hide them) and a remote pool can grant or deny writes per key. The key registry carries that policy from the start, so no second redesign is needed when the pool arrives.

The three scopes separate cleanly by **who owns the value**:

| | `application` | `project` | `runtime` |
|---|---|---|---|
| travels with a project? | no | yes | no |
| meaningful on a remote machine? | only via the user's profile | yes (shipped with the project) | yes (it *is* the machine's execution config) |
| typical content | theme, UI behaviour, file chooser, node browser pins, recent projects | project IO format, project authors, per-project environments | thread pool, temp/cache dirs, environment installations, hardware acceleration, artifact dirs |
| owner | user's JIPipe instance | the project | the runtime hosting execution |

Today's split is murky: `JIPipeRuntimeApplicationSettings` (threads, temp dir, cache) lives in `application` sheets although it is machine/execution config, and `JIPipeDefaultEnvironmentsApplicationSettings` mixes *installed* environments (machine) with *default* environments (project creation). The redesign assigns each existing setting to exactly one scope — the mapping is enumerated during implementation, key by key.

Scope is a first-class argument in the API, not a special case.

### 3.2 Document shape

```json
{
  "$schema": 2,
  "$meta": { "written-by-version": "6.0.0", "written-at": "2026-10-27T20:00:00Z", "writer-id": "a41f..." },
  "sheet:org.hkijena.jipipe:general-ui": { "theme": "Modern", "reduced-motion": false },
  "registry:ui-graph-editor": { "pipeline/dock-state": { }, "any/dock/show-labels": true },
  "registry:node-db": { "pinned-node-ids": [] },
  "state:recent-projects": ["/home/user/projects/a.jip"]
}
```

- Top-level keys are **`"<kind>:<id>"`**, `kind ∈ { sheet, registry, state }`. The reserved fields `$schema` and `$meta` are the only other permitted top-level keys.
  - `sheet:<sheet-id>` — the existing 35 sheet blocks, **internal layout unchanged** (parameter key/values). This makes sheet deserialization work as-is.
  - `registry:<database-key>` — the existing flat `key("a","b")` → value layout; `JIPipePathMetadataStore`'s serializer is reused.
  - `state:<id>` — volatile/simple state that was neither sheet nor registry (recent projects first; future: window positions, etc.).
- **Unknown-key pass-through.** On load, any top-level key whose `kind` is unknown, or whose ID is not registered, is kept as a raw `JsonNode` in a pass-through map and re-serialized **verbatim** on save. Same rule inside project files (unknown project-settings sheets). This directly fixes "downgrade clears settings" and "parallel versions trash each other's unknown sheets".
- **Defaults are not stored.** A key that equals its default (or was never set) is absent from the document. The document contains *user changes*, not a snapshot of all settings.
- `$meta` carries conflict-detection info only; it is never a gate for loading (a document written by a newer version still loads).
- **Atomic writes:** serialize to `<file>.tmp.<writer-id>`, then `Files.move(..., ATOMIC_MOVE)` (fallback: non-atomic move + size/mtime verify for filesystems without atomic rename). Orphan tmp files are cleaned at startup. A corrupt target is backed up as `<file>.bad-<timestamp>` and defaults take over (with a notification), never a hard failure. The same rules apply to the runtime document.

### 3.3 Profile carry-over across versions

Profile dirs remain per-version (the directory layout is out of scope — other state like caches and artifacts already assume it). The existing whole-directory carry-over (`JIPipeServiceInitializer.applyProfileUpgrades()`) is **kept as-is** for caches, templates, and the rest of the profile. On top of it, the new system adds a targeted **settings migration** that runs when the copy produces a file in the old layout, or when a profile dir already exists but its `settings.json` was written by a *different* JIPipe version:

1. Old-layout detection (`$schema` missing) → the §6 migration transforms it in place (sheet prefixes, registry blocks, recent projects, `$meta`).
2. **Reverse/recovery merge.** When the running version's profile has a `settings.json` but a *newer* version's profile contains blocks this version does not know, those **unknown** blocks are merged in from the newest other profile as pass-through — recovering data without letting a dev build's experimental *registered* values flow into the release. Registered (known) blocks always keep the local version's value: no cross-version override of real settings. This is the reverse-migration case the current code cannot handle, and it is conservative by construction: pass-through blocks have zero runtime effect in this version and are fully visible/deletable in the raw editor.
3. A key can opt out of merge-in via `migrate: false` in its registration for values that must reset per version.

The whole-directory copy remains the fast path (first launch on a new version); the settings-level merge is the safety net for the shared-version-string and downgrade cases, and it is explicit, testable, and idempotent.

## 4. Java API

### 4.1 Service component

One component replaces the current `JIPipeApplicationSettingsServiceComponent` + registry methods + `JIPipeRecentProjectsRegistry` persistence:

- **`JIPipeSettingsServiceComponent`** (new name, same registration slot in `JIPipeService`) owns: the registered sheets, the key registry, the per-scope `SettingsDocument` instances, the file writer, and the file watcher.
- `JIPipeRecentProjectsRegistry` stays as a thin component delegating to `state:recent-projects` (its `List<Path>` API, change events, and all callers are untouched).
- `JIPipe.getSettings()` now returns the facade (Section 4.3); existing `JIPipe.getSettings().getById(...)` / `getByType(...)` / `save()` / `reload()` signatures keep working as compatibility delegates.

### 4.2 Keys — the declarative primitive

```java
JIPipeSettingKey<Boolean> SHOW_DOCK_LABELS =
    JIPipeSettingKey.bool("registry", "ui-graph-editor", "any/dock/show-labels")
        .defaultValue(true)
        .scope(SettingsScope.APPLICATION)
        .access(AccessPolicy.USER_EDITABLE)
        .description("Show labels in graph editor dock toolbars");
```

A key declares: kind + id + path, value type, default, scope, **access policy** (`USER_EDITABLE`, `RUNTIME_ADMIN`, `READ_ONLY` — the hook for permission-gated remote runtimes), doc metadata (name/description), and optional `migrate: false`. Keys are **stable string literals** (project rule for registered IDs). The existing registry constants (`NodeEntryKind.SETTINGS_DATABASE`, `key(...)`) become key objects; the old `putIntoRegistry` etc. are deprecated delegates that route through the same document.

### 4.3 Bindings — the live adapter

```java
SettingsBinding<Boolean> labels = JIPipe.getSettings().bind(SHOW_DOCK_LABELS);
labels.get();
labels.set(true);                                   // write-through, debounced disk save
labels.subscribe(value -> dockPanel.setShowToolbarLabels(value));
labels.reset();                                     // back to key default
labels.isModified();
```

A `SettingsBinding` is the settings analogue of `JIPipeParameterAccess`: a typed, observable view over one key in one scope. It fires on changes made by **any** writer:

- another component in this process (graph editor B learns editor A's toggle),
- the settings tab or raw editor,
- another JIPipe process (via the file watcher, §5),
- a future remote-runtime push (the runtime process or pool is the writer; the client observes).

Subscribers get a re-entrancy guard: a write that round-trips through the binding (value unchanged) does not re-fire.

**Parameter-sheet adapter.** Sheet classes keep their exact shape (`AbstractJIPipeParameterCollection`, `@JIPipeParameter` fields, `getInstance()` statics, plugin `registerApplicationSettingsSheet`). What changes is the *direction of data flow*: the sheet block is a **projection** of the document. A bidirectional bridge connects the document's per-path change events with the sheet's existing `JIPipeParameterChangedEventEmitter` in both directions:

- document → sheet: external change (raw editor, other instance) sets the parameter and re-fires the sheet's parameter-changed events, so open UI updates;
- sheet → document: a parameter change writes the value into the sheet block and schedules the debounced save.

This makes the existing 35-sheet migration mechanical: no sheet class needs its fields or IDs touched; the dialog plumbing (§7) is the UI work, not the model work.

### 4.4 Event bus

One emitter on the component: **per-path change events** `SettingsChangedEvent(scope, keyPath, oldValue, newValue, origin)` where `origin ∈ {local-edit, local-programmatic, external-file, external-process, carry-over, reset, raw-edit}`. The old coarse `ChangedEvent` ("settings changed") is kept as an aggregate (any path) for compatibility with current subscribers. This single stream is also what the future runtime split and the instrumentation API will expose.

### 4.5 Raw access

The document exposes `raw(path)` / `setRaw(path, JsonNode)` for the raw editor, instrumentation operations, and programmatic migration. Raw edits go through the same change-event + debounced-save pipeline as typed edits.

## 5. Multi-instance sync

**Model: watched file + atomic replace + per-block merge.** (Research: VS Code uses file watching + atomic writes; KDE's KConfigWatcher uses D-Bus broadcast on Linux with a file-watch fallback; GSettings/dconf uses a BLOB service. The portable, headless-safe foundation for us is the file itself; no IPC dependency.) The sync engine is scope-agnostic and runs per scope **document** that is file-backed: `application` (`settings.json`) and `runtime` (`runtime-settings.json`) each get their own document, writer, fingerprint, and watcher registration. `project` scope syncs through the project file's existing save/merge path plus the same change-event stream.

Write side (per process, per document):

1. All edits mutate the in-memory document only (single in-process authority; EDT for UI-driven edits).
2. Debounced save job (off-EDT): serialize → `<file>.tmp.<writer-id>` → atomic `Files.move`.
3. After a successful write, the process records the resulting (size, last-modified-time) fingerprint; its own watcher events matching that fingerprint are ignored.

Read side (per process, per document):

1. A watcher (NIO `WatchService` on the profile dir + a ~2 s poll fallback comparing fingerprints; the dual pattern already exists in the script-editor UI) fires on any settings-file change.
2. On a foreign fingerprint: parse the new file. Per `kind:<id>` **block** compare with ours:
   - block changed only externally → adopt the foreign block, fire per-path events;
   - block changed only locally (unsaved or already saved) → keep ours; if ours was already saved and the file still shows the old block, re-save (we are newer);
   - block changed both (true collision) → **foreign wins** (it is the most recent on-disk truth); local unsaved edits to that block are discarded and a notification is shown: "Settings were updated by another JIPipe instance".
3. No event loops: after adopting/re-saving, the fingerprint is updated, so our own subsequent write is self-ignored.

**Accepted limit:** two processes editing the *same* block simultaneously lose the older block's local unsaved edits — the same trade-off VS Code accepts. The settings tab shows a "changed elsewhere" banner while open to make this visible.

**Headless/CLI:** the writer works identically; the watcher is optional and disabled in headless mode.

## 6. Storage-format migration (5.x → 6.0.0)

One-time, on first load of a profile that has the old layout:

1. Old `settings.json` (top-level = sheet IDs, no prefix) → prefix each registered sheet ID with `sheet:`; unregistered sheet IDs → keep as `sheet:<id>` in the pass-through map (not dropped — they stay loadable by newer versions and visible in the raw editor).
2. Each `settings-db-<key>.json` → its content becomes the `registry:<key>` block in `settings.json`.
3. `recent-projects.txt` → `state:recent-projects` array.
4. **Scope split:** each resulting block is routed to `application` or `runtime` according to the key/scope registry (§3.1). Blocks whose scope is not yet decided default to `application` (the safe choice — nothing about a user's UI is lost), and the runtime document (`runtime-settings.json`) is created for the blocks that move there. Blocks not covered by any key declaration (e.g. sheet parameters of an unregistered extension) stay in `application` as pass-through.
5. Write the unified files (atomic), then **move** the old files to `settings-migration-backup/` inside the profile dir (not delete; visible to the user, safe to remove).
6. Add `$meta` and bump to `$schema: 2`. Old-format detection: no `$schema` field.

Migration runs before any sheet is loaded (replacing the current `migrateFromLegacy` recent-projects hook), is idempotent, and is covered by tests over fixture profiles.

## 7. Settings UI

Shell: a **workbench-level tab** (beside the Welcome tab) — non-modal, multiple instances allowed, live-updating. The old modal `JIPipeDesktopApplicationSettingsUI` and `JIPipeDesktopCustomizerDialog` are removed; `openApplicationSettings(category)` call sites (~15) become `openSettingsTab(section)`.

Layout:

- **Top:** search bar (filters tree + content; matches names, descriptions, raw key paths) and a **scope switcher** `Application | Project | Runtime`.
- **Left:** category tree (existing categories, reorganized):
  - *General → Appearance* (merged: theme, theme style, UI scale, reduced motion, file chooser type + fallback, **with the live preview panel from Customize JIPipe**) — application scope
  - *General → Accessibility* (reduced motion, UI scale, future a11y settings) — application scope
  - *General → Keyboard* (placeholder section — empty state "shortcut profiles coming"; this is where #1346 registers) — application scope
  - *Runtime* category (threads, temp/cache directories, environment installations, hardware acceleration, artifact dirs) — runtime scope, presented alongside the rest
  - remaining sheets map to sections under their categories; small sheets are grouped into their category page.
- **Right:** content pane reusing `JIPipeDesktopParameterFormPanel` (all existing parameter editors work unchanged), with per-section header: description, **modified** badge, **Reset to defaults** button.
- **Scope behavior:** the tree always shows all sections; each section belongs to exactly one scope, and sections of a different scope than the switcher are shown dimmed and read-only with a "belongs to &lt;scope&gt;" hint, so users can *find* a setting without knowing its scope. The switcher makes that scope's sections editable. Project-only sections are unavailable when no project is open; runtime sections are `RUNTIME_ADMIN`/`READ_ONLY`-gated once the runtime is remote.
- **Advanced → Raw settings** (`about:config`): tree of every key in the active-scope document — documented keys with metadata, pass-through keys marked "managed by another version", typed value editing dialog, per-key reset, warning banner. Edits the same API as the typed UI.
- **Advanced → About:** version, profile dir, "open profile folder".

Behavior:

- No Save/Cancel buttons: edits are immediate, write-through, debounced.
- A **changes strip** (top-right) lists keys changed since the tab opened, each with one-click reset — replacing the old Save/Cancel workflow.
- Live updates: when a foreign write lands while the tab is open, a banner appears ("Settings were updated by another JIPipe instance") and affected values refresh via the binding events.
- Search + scope switcher + live values together address "hard to find settings" and "desynced UIs".

## 8. Compatibility & removals

- **Sheet classes: unchanged** (IDs, fields, `getInstance()` statics).
- **Plugin API: unchanged** (`registerApplicationSettingsSheet`, `JIPipeDefaultJavaPlugin` helpers).
- **Deprecated delegates:** `putIntoRegistry`/`getFromRegistry`/`getListFromRegistry`, `JIPipeApplicationSettingsServiceComponent` (renamed, old type kept as deprecated subclass/delegate), `JIPipeRecentProjectsRegistry` (delegates to the document). Removed in a post-6.0 cleanup, not in this change.
- **Old profile dirs (5.x):** still *readable* for one-time migration (§6); the theme read from old profile (`ThemeUtils`) now goes through migration instead.
- **Tests:** the taxonomy-gate and registry-test conventions of AGENTS.md apply: no exact registry-size pins, presence-based assertions, serialization round-trip tests over fixture profiles (5.x layout, unknown-sheet pass-through, corrupt file, concurrent tmp files).

## 9. Extension points for future work items

- **#1346 (shortcuts):** register a `Keyboard` section + keys (`state:keyboard-shortcuts` or `sheet:...`); the UI placeholder already exists; key-capture dialogs are a separate #1341 concern.
- **#1102 (runtime & cache server):** the `runtime` scope already exists and is live; #1102 moves its document into a separate process (step 1) and later onto a remote, permission-gated runtime pool (step 2). Nothing in the settings shape changes — the runtime process becomes the writer for the runtime document, the client observes it through the same change-event stream, and remote pools enforce the key `access` policies.
- **#701 (live collaboration):** the per-path change stream with `origin` is the wire format for collaboration broadcasts.
- **Instrumentation:** raw access + change events map directly to new instrumentation operations (`get_setting`, `set_setting`, `list_settings`).

## 10. Risks & mitigations

| Risk | Mitigation |
|---|---|
| 35 sheets + ~15 registry users migrated simultaneously → huge blast radius | Phased implementation: model+storage first (shims keep everything green), then bindings, then UI. Each phase is independently shippable. |
| Watcher flakiness (Windows, network drives, CI) | Dual watch+poll with fingerprint compare; poll is the correctness floor, watch is the latency optimization; headless disables watching. |
| Parameter-sheet ↔ document bridge re-entrancy (change storms, double-firing) | Re-entrancy guards on the binding; single event bus; round-trip suppression when value unchanged; integration test with a double-subscribed sheet. |
| Raw editor edits corrupting settings | Typed validation on documented keys; pass-through keys edit raw JSON only; corrupt-file recovery path (§3.2) always available. |
| Per-version profile dir still splits other state (caches, artifacts) | Out of scope — settings are the user-visible state; other state is intentionally per-version and disposable. |

## 11. Success criteria

1. A 5.x profile is opened by 6.0.0: all settings present, old files moved to backup, nothing lost. A 6.x-written profile is opened by an *older* 6.x: unknown keys survive a save cycle (round-trip test).
2. Two JIPipe processes: change a setting in one → the other reflects it within ~2 s, open settings tabs update, no corruption under simultaneous saves (concurrency test).
3. Toggle "show toolbar labels" in one open graph editor → every other open graph editor updates without reopening (binding test).
4. All 35 sheets and the 4 Customize-JIPipe controls are reachable in the new tab; search finds any setting by name or key path; raw editor can list and edit every stored key including pass-through ones.
5. No feature depends on "save-on-shutdown" for correctness: killing the process mid-session loses ≤ the last debounced window of edits.
6. All three scopes (`application`, `project`, `runtime`) exist, are loaded and saved by the same engine, and every current setting has been assigned to exactly one of them; a `runtime` document can be created, written, observed, and access-checked using only the public API (shape test, no process split).
7. The runtime document can be exported and re-imported into a fresh service instance without loss (transferability test — the prerequisite for the #1102 process split).
