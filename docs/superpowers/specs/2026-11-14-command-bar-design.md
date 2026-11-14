# Command bar for JIPipe (design spec)

Status: draft for review
Origin: design round following work item #1346 (keyboard shortcut overhaul). The command bar is the chord-aid layer: chords are rare keystrokes, and users need discoverability for them.
References: VS Code command palette (trigger, list, per-row keybinding, Configure-Keybinding affordance); the screenshots from the #1346 design round.

## 1. Goals

- One command bar per JIPipe window that lists the commands applicable right now and fires them.
- Chord aid: pressing a chord prefix (e.g. `Ctrl+T`) opens the bar in a special mode listing every command bound to that prefix, with the second key visible; the command fires by click, `Up`/`Down` + `Enter`, or by pressing the associated second key.
- Every registry shortcut action is reachable through the bar, including unassigned ones (docks) and future commands (e.g. "Rerun last update cache").
- Per-row Configure-Keybinding affordance reusing the existing capture dialog.
- Window and tab switching as commands.

## 2. Non-goals

- WoW-style quick-assign overlay: work item #1364.
- Bars in non-JIPipe windows.
- Concrete future commands ("Rerun last update cache" etc.): this spec ships the mechanism only.
- User profile management UI (existing follow-up of the shortcut overhaul, spec 9.4).

## 3. Decisions (agreed in the design round)

1. **Unified registry.** Every bar entry for a real command is a `JIPipeShortcutAction`. Future commands register as actions with no default binding: they appear in the bar, in the Keyboard settings table (State: Unassigned), and are user-bindable. Dynamic entries (window/tab switching) are ephemeral bar entries, not registry actions.
2. **Trigger.** A new registry action `org.hkijena.jipipe.command-bar.open` ("Open command bar", category "Menus", project scope family, text-input safe) with two default variants: `Ctrl+P` and `Ctrl+Shift+P`.
3. **Keymap revision.** The specialty operations move onto the `Ctrl+T` chord layer (Section 5), freeing their direct keys; the trigger takes `Ctrl+P`/`Ctrl+Shift+P`.
4. **List filtering.** Default mode: show commands applicable to the current window kind, gray live-unavailable ones. The mode is a configurable application setting ("Grayed unavailable" (default) / "Available only" / "Show everything"); real-world testing calibrates the default later.
5. **Switching commands.** Window and tab switching ship in this effort as ephemeral commands.
6. **Integration.** Overlay bar per window (root pane layered pane) with dispatcher handoff for chord prefixes (Approach 1 of the design round).
7. **Search evolution.** Substring search first; fuzzy matching and MRU ranking are direct in-branch follow-ups (Section 8).

## 4. Command model

### 4.1 Registry commands

A bar entry for a registry command carries: the action (id, name, category), the effective binding rendered via `JIPipeShortcutRegistry.formatVariants`, live availability, and (nullable) nothing else - firing reuses the scope-chain resolution the dispatcher performs, re-evaluated at fire time.

Availability has two layers:

- **Window kind:** a graph action cannot fire in a data viewer and vice versa. Computed the same way the dispatcher computes it: order all registered scopes containing the focus owner innermost-first; a command is resolvable when some scope's resolver returns a runnable.
- **Live state:** a selection-dependent command (Copy, Delete, Run) resolves only when its `matches(selection)` holds. In "Grayed unavailable" mode such commands render grayed and are not fireable; in "Available only" mode they are hidden; in "Show everything" mode the window-kind filter is dropped entirely.

The text guard does not apply inside the bar (the dispatcher is suspended while the bar is open, Section 6).

### 4.2 Ephemeral commands

Generated at open time by a per-window provider (Section 7). They carry name, category, icon, and a runnable; they are never bindable, never rendered with a binding, and never appear in the Keyboard settings table.

### 4.3 Future commands

Registering a `JIPipeShortcutAction` with no default variants is the entire integration: the action shows up in the bar (State column: Unassigned in the Keyboard page) and is bindable like any other.

## 5. Revised default keymap

Chord layer second keys already in use: `M`, `C`, `R`, `V` (graph tools), `E`, `T`, `A`, `I` (canvas notes tools).

| Action | Old default | New default |
|---|---|---|
| Open command bar | (new action) | `Ctrl+P` or `Ctrl+Shift+P` |
| Disable pass-through | `Ctrl+Shift+P` | `Ctrl+T` then `Shift+P` |
| Enable pass-through | `Ctrl+Alt+P` | `Ctrl+T` then `P` |
| Lock node | `Ctrl+L` | `Ctrl+T` then `L` |
| Unlock node | `Ctrl+Shift+L` | `Ctrl+T` then `Shift+L` |
| Disable node | `Ctrl+Shift+E` | `Ctrl+T` then `D` |
| Enable node | `Ctrl+E` | `Ctrl+T` then `Shift+D` |
| Add parameter set here | `Ctrl+Shift+A` | `Ctrl+T` then `S` |
| Move node here | `Ctrl+G` | `Ctrl+T` then `H` |
| Add tunnel entrance here | unbound | `Ctrl+T` then `G` (new registration) |
| Add tunnel exit here | unbound | `Ctrl+T` then `X` (new registration) |
| Update cache | `Ctrl+Space` | `Ctrl+T` then `U` |
| Cache intermediate results | unbound | `Ctrl+T` then `Shift+U` (new registration) |
| Update predecessor cache | unbound | `Ctrl+T` then `B` (new registration) |
| Add new node here | `Shift+A` | `Shift+A` or `Ctrl+T` then `N` (chord alternative) |

Kept direct: Add node `Shift+A`, Duplicate `Ctrl+D`, Duplicate with inputs `Ctrl+Shift+D`, Run & show results `Ctrl+Enter`, undo/redo/zoom/move-selection, project menu actions.

Consequences:

- `Ctrl+G`, `Ctrl+E`, `Ctrl+Shift+E`, `Ctrl+L`, `Ctrl+Shift+L`, `Ctrl+Alt+P`, `Ctrl+Space`, `Ctrl+Shift+A` become unbound defaults (still user-assignable).
- The **LEGACY** preset grows by five overrides to keep restoring pre-6.0 bindings: enable `Ctrl+E`, disable `Ctrl+Shift+E`, parameter set `Ctrl+Shift+A`, move-here `Ctrl+G`, update cache `Ctrl+Space`. Lock/unlock and pass-through are already in the preset.
- LEGACY interplay with the trigger: under LEGACY, `Ctrl+P`/`Ctrl+Shift+P` hit the old pass-through bindings inside graph editors (innermost scope wins, unchanged semantics) and open the bar everywhere else. Under the new default there is no collision.
- Mixed-length variants are already supported (`Shift+A` single stroke plus a `Ctrl+T` chord on the same action).

## 6. Bar UI, interaction, and dispatcher integration

### 6.1 Component and placement

`JIPipeDesktopCommandBar`, one instance per JIPipe window (project windows and data viewer windows). It is an overlay on the window's root pane layered pane, top-center; its width derives from the window width (never from text measurement, per the project's size rule). It registers a window-level `ShortcutScope` (root = root pane, resolver answers only the trigger id) with the same add/removeNotify lifecycle as dock panels.

### 6.2 Anatomy and modes

Autofocused search field, result list below. Rows: name (left), rendered binding (right, grayed), Configure-Keybinding affordance on the hovered/selected row (registry commands only). Category group headers in registry order ("Windows" and "Tabs" sort last).

- **Normal mode** (opened via the trigger): type to filter (case-insensitive substring over name and category), `Up`/`Down` move the selection, `Enter`/click fires, `Escape` or focus loss closes. Pressing a trigger variant while the bar is open closes it (toggle); the bar handles this itself, since dispatch is suspended while it is open.
- **Chord mode** (dispatcher handoff, Section 6.3): pre-filtered to commands whose effective binding starts with the pressed prefix (the same prefix set the dispatcher's chord machine already computes); the second key is rendered prominently. Pressing a listed second key fires that command immediately; an unlisted key cancels the chord and closes the bar (matching the existing swallow semantics); the chord timeout does not apply while the bar holds the prefix. A chord prefix opens the bar regardless of text focus (today the prefix keystroke is consumed in text fields anyway; the bar is strictly more useful than the status hint).

### 6.3 Dispatcher handoff

On `PREFIX_STARTED`, the dispatcher checks a static bar-host hook (same pattern as the status sink) for the focused window:

- With a host: hand the prefix to the bar, cancel the dispatcher's own pending-chord state, and suspend dispatch via the existing capture-suspend switch (`setCaptureActive`). The bar restores dispatch when it closes. Without a host: the current status-hint behavior stays unchanged.
- The status-bar pending-chord hint is superseded by the bar in chord mode (one UI). The "Executed: ..." status text is unchanged.
- `command-bar.open` is text-input safe, so the trigger works from text fields. While the bar is open the dispatcher is suspended, so no double handling occurs.

### 6.4 Firing and Configure-Keybinding

Firing re-resolves the command at fire time (fresh availability, identical to the dispatcher's walk) and closes the bar. Configure-Keybinding opens `JIPipeDesktopKeyboardCaptureDialog.capture` and applies the identical commit path as the Keyboard page: conflict check via `findConflicts` (skipped when clearing), warning with "Assign anyway"/"Cancel", write through the keymap binding.

## 7. Window and tab switching

A small per-window provider consults at open time and generates ephemeral entries:

- "Switch to window: \<title\>" for every other open JIPipe window: bring to front and focus. Project windows come from `JIPipeDesktopProjectWindow.getOpenWindows()`; data viewer windows have no equivalent registry yet, so the implementation adds a matching static set there (mirroring the project-window pattern) or a minimal shared window registry - decided during planning.
- "Switch to tab: \<name\>" for the focused window's graph editor tabs (pipeline and compartment flavors) and data viewer tabs: select the tab.

Categories "Windows" and "Tabs" sort after the registry categories. Entries are not bindable and carry no binding text.

## 8. Search and ranking (evolution within this effort)

1. **Base (first deliverable):** case-insensitive substring over name and category; registry ordering.
2. **Fuzzy matching (direct in-branch follow-up):** reuse the existing matching ladder of `JIPipeEnhancedNodeDatabaseSearch` (exact > prefix > stem > substring > strong fuzzy); fuzzy scores replace the substring filter behind the same UI.
3. **MRU (direct in-branch follow-up):** a bounded recently-fired list (ids only, application-scoped, persisted as a small settings block; pass-through semantics tolerate evolution). Used to (a) show a "Recently used" section at the top on empty query and (b) order equal-score matches by recency.

## 9. Settings and catalog additions

- New application setting "Command bar list mode" with the three modes from Decision 4, hosted as a combo on the Keyboard settings page and applied live.
- Catalog additions in the core catalog (`JIPipeShortcutActions`), all stable string literals:
  - `org.hkijena.jipipe.command-bar.open`, category "Menus", project family, text-input safe, variants `Ctrl+P` and `Ctrl+Shift+P`.
  - `org.hkijena.jipipe.graph.node.cache-intermediate-results` ("Cache intermediate results", category "Running", chord `Ctrl+T` then `Shift+U`).
  - `org.hkijena.jipipe.graph.node.update-predecessor-cache` ("Update predecessor cache", category "Running", chord `Ctrl+T` then `B`).
  - `org.hkijena.jipipe.graph.node.add-tunnel-entrance` ("Add tunnel entrance here ...", category "Creation", chord `Ctrl+T` then `G`).
  - `org.hkijena.jipipe.graph.node.add-tunnel-exit` ("Add tunnel exit here ...", category "Creation", chord `Ctrl+T` then `X`).
  - The corresponding context action classes receive `SHORTCUT_ACTION_ID` constants like every other context action.
- The Keyboard settings page picks up the new rows automatically; no structural change.

## 10. Testing

Pure tests only (no Swing interaction tests, per project policy):

- Chord-mode filter: given a prefix and keymap, the bar's command list and ordering (pure helper).
- Availability evaluation: stub scopes/resolvers pin innermost-first resolution and the three list modes.
- Ephemeral provider: fake window/tab model produces the expected entries.
- Keymap pins in `JIPipeShortcutResolveTest`: new defaults, freed keys unbound, LEGACY preset growth.
- Bar UI behavior (layout, focus, dismissal): user-verified in-app.

## 11. Delivery

- Spec: this document. Implementation follows via the writing-plans skill after spec approval.
- Branch strategy (merge onto the shortcut-overhaul branch vs. stacking on master) is decided at the finish gate of #1346.
