# Template-taxonomy integration — design spec

Work item 1324 (6.0.0). Status: design approved in conversation 2026-10-15.
Follows the taxonomy redesign (`2026-10-08-node-taxonomy-redesign-design.md`) and the
corpus research (`utils/taxonomy-research/report.md`).

## 1. Problem

Node templates live in a separate world from the node taxonomy:

- They carry a `menuPath` (a holdover from the removed menu system) with no taxonomy meaning.
- The Templates dock panel (`NodeTemplateBox`) is a parallel UI surface: its own list, its
  own edit/organize actions, its own context menu — while the add-nodes panel / node finder (tree + list + detail rail)
  is the taxonomy-driven surface.
- Templates appear in the node database only as "create nodes" entries with **no function
  ids**, so they live in no tree branch and are invisible to function-based discovery.
- The workflow-relation machinery is fragmented: `FamilyAssembler.followUps` (successors
  only), `siblingNodeIds` (internal), and `JIPipeNodeDatabase.related` (a 4-tier similarity
  blend that conflates similarity with "next steps"). There is no "what comes before" query.
- The 161-project corpus research produced 185 curated pattern templates
  (`utils/taxonomy-research/patterns/templates/*.json`), deserialization-verified against
  `JIPipeNodeTemplate` — shipped nowhere.

## 2. Goals

1. Templates fold into the add-nodes panel / node finder; the Templates dock panel is removed.
2. Templates get a **taxonomy function** (replacing `menuPath`), picked through a
   user-friendly function-tree UI (never raw ids).
3. Templates appear in the taxonomy tree **and** the node list, clearly badged as
   `Pattern` (multi-node) or `Custom` (single-node), grouped by scope (System/Global/Project).
4. The node tree gains context-menu support; in compact (1-pane) mode the tree-leaf context
   menu is identical to the list's (the existing parity bug).
5. Global/Project templates gain the edit/organize features previously in the Templates
   panel; detailed management moves to a modal ("Configure templates") behind a new gear
   button next to the AI search button + a context-menu item.
6. Relations generalize into one model — **Similar / Previous steps / Next steps** — mined
   from family steps and from **all** template graphs (both edge directions).
7. The corpus-mined patterns ship as **System** templates (curated: see §11 —
   the 64 with derivable names at this revision).
8. No explicit ordering for templates (they follow the add-nodes list's ranked order).
9. The design must not preclude #1333 (custom-node repository: sharing/downloading
   templates) — seams only, no implementation.

## 3. Non-goals (explicit)

- **#1333** repository features (upload, remote browsing, auth UI) — the `source` field,
  the Download tab, and `NodeRelationSource` are the seams it builds on.
- The **starter-patterns feature** (suggesting common starts from template source nodes) —
  the `NodeRelationSource` seam + the source-node concept slot it in later.
- Editing a template's **graph** from the management UI (graph editing stays on the canvas;
  the manager edits metadata: name, description, taxonomy function, icon, hue, scope).
- Reordering templates (no sort index; they follow the list's ranked order).
- Mining relations from AI/LLM sources.

## 4. Naming

| Concept                                            | Name                                                                                                                                                                                                 |
|----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Template field (was `menuPath`)                    | `taxonomyFunction` (serialization key `taxonomy-function`); **value type: `JIPipeTaxonomyFunctionRef`** — a dedicated reference class (see §6), serialized as the plain function-id string or `null` |
| Display name                                       | **Taxonomy function**                                                                                                                                                                                |
| Parameter type id                                  | `jipipe:taxonomy-function` (registered for `JIPipeTaxonomyFunctionRef`, NOT a String)                                                                                                                |
| Catch-all function leaf for unclassified templates | name "Templates", id `jipipe:function:templates`, **leaf under the `Miscellaneous` top-level branch** (a real registered function, stable literal id)                                                |
| Synthetic tree grouping                            | **Templates** branch (UI-only; children System/Global/Project; *not* a registered function)                                                                                                          |
| Entry badges                                       | `Pattern` (graph ≥ 2 nodes) / `Custom` (graph = 1 node)                                                                                                                                              |
| Relation sources (pluggable, §10.2)                | `FamilyRelationSource` + `TemplateRelationSource` (both `NodeRelationSource`s feeding one `NodeRelationIndex` directly; **no virtual family**, so `getFamilies()` and validation are untouched)      |

Naming rationale: "Taxonomy function" (not "function") avoids the collision with
`JIPipeFunctionParameter` (a different, expression-related concept).

## 5. Data model — `JIPipeNodeTemplate`

- **Remove** the `menuPath` field from the model. The Jackson deserializer keeps a
  lenient path: a present `menuPath` key is consumed by the migration converter (§9) and
  dropped. Through the 6.0.0 pre-release the JSON schema still **accepts** `menuPath`
  (extensions may ship it); after one load+save cycle it is gone.
- **Add** `taxonomyFunction` of type `JIPipeTaxonomyFunctionRef` (§6 — a dedicated
  reference class serialized as the plain id string); the ref is nullable/unset =
  unclassified → the `jipipe:function:templates` catch-all governs placement.
- **Scope is storage location, not a field** (unchanged from today):
    - **System** — bundled resources, registered at plugin init (new; §10).
    - **Global** — user dir `node-templates/` (`JIPipeNodeTemplatesServiceComponent`).
    - **Project** — project metadata key `node-templates`.
- `source` (free-string provenance, e.g. "User"/"Extension") is **retained unchanged**;
  System patterns use `source = "System (corpus-mined)"`. #1333 remote downloads slot into
  Global with their own source string — no model change needed.
- Badges are **computed** (node count), never stored.

## 6. Parameter type `jipipe:taxonomy-function`

**The value is a dedicated reference class, not a String** — mirroring the existing
data-type-selection pattern (`JIPipeDataInfoRef`, registered as the `data-type` parameter
type: a class wrapping a String id with custom `@JsonSerialize`/`@JsonDeserialize` that
writes/reads the plain id string, plus a picker editor). A bare `String` field would fall
through to the default string editor and present raw ids to the user.

- **New class `JIPipeTaxonomyFunctionRef`** (in
  `org.hkijena.jipipe.plugins.parameters.library.taxonomy`, next to the other reference
  params):
    - Wraps a `String` function id (nullable = unset).
    - `@JsonSerialize`/`@JsonDeserialize` custom: writes the id string or `null` — the
      on-disk format is a plain string, so template JSON stays human-readable and the
      migration (§9) reads/writes the same key shape.
    - `getId()` / `getFunction()` → resolves via `JIPipe.getNodes().getFunctions().byId(id)`;
      unresolved id → the ref reports `isResolved() == false` and `getFunction() == null`
      (never throws — stale-id resilience lives here, not at call sites).
    - `equals`/`hashCode` on the id string; a no-arg constructor (unset).
- **The template field** `JIPipeNodeTemplate.taxonomyFunction` is of this type
  (`@JIPipeParameter("taxonomy-function", uiOrder = -70)`, same position the old
  `menu-path` held). `JIPipeNodeTemplate` extends `AbstractJIPipeParameterCollection`, so
  the parameter machinery — and therefore the custom serializer — applies to the field
  automatically (exactly how `JIPipeDataInfoRef` fields serialize inside
  `JFreeChartPlotEditor`).
- **Registration:** `registerParameterType("jipipe:taxonomy-function",
  JIPipeTaxonomyFunctionRef.class, JIPipeParameterArchetype.Value,
  JIPipeTaxonomyFunctionRef::new, r -> new JIPipeTaxonomyFunctionRef((JIPipeTaxonomyFunctionRef) r),
  "Taxonomy function", "The taxonomy function this template is filed under",
  JIPipeDesktopTaxonomyFunctionRefParameterEditorUI.class)` in
  `StandardParametersPlugin` (next to the `data-type` registration), so the param type is
  available before template editing is usable.
- **Editor UI** (`JIPipeDesktopTaxonomyFunctionRefParameterEditorUI`, modeled on
  `JIPipeDesktopDataInfoRefParameterEditorUI`): a "currently displayed" button showing the
  resolved function's name (or "Not set") + a picker button. The picker dialog is a
  `JTree` built from the `FunctionRegistry`, rendered with the node-browser tree's cell
  renderer/helpers, with a name filter field. **Both branches and leaves are selectable**
  (user decision) — a branch placement shows the template under that branch node. An
  "Unset" action clears the ref.
- The parameter type is a generic citizen: any future feature needing "pick a taxonomy
  function" (e.g. #1333's repository categorization) reuses it.

## 7. Database entries + tree/list integration

### 7.1 Entry extension

`CreateNewNodesByTemplateDatabaseEntry` (extended in place):

- New `TemplateScope` (System/Global/Project) set by the builder from the list a template
  came from; `locationInfo` = `Templates / <Scope>` (search tokens include the scope word).
- `getFunctionIds()` returns `{ref.getId()}` when the `taxonomyFunction` ref is **resolved**, else
  `{jipipe:function:templates}` (the catch-all leaf) — an unset/stale
  ref still yields a function, so templates are always in the tree (today: empty — the
  reason they are invisible).
- **Badge:** `Pattern` if the template graph has ≥ 2 nodes, else `Custom`.
- Everything else (insertion via `addToGraph`, drag, "Insert at cursor") is unchanged.

### 7.2 The tree

The browser/finder function tree gains one **synthetic top-level branch `Templates`**
(distinct icon; built in the tree model, *not* a registry function — the taxonomy
registry, validation, and dumper are untouched by it):

```
…registry branches…
Images
  Thresholding
    …nodes…
    ◇ My threshold helper [Custom]      ← under its taxonomy function (leaf)
  ◈ My image pattern [Pattern]          ← branch-level function placement
Templates ◈                              ← synthetic
  System
    ◈ File → Annotate with path → Import image [Pattern]
  Global
  Project
    ◇ My 1-node thing [Custom]
```

- **Dual placement, two mechanisms:**
    - *Taxonomy-function placement* — driven by `getFunctionIds()` (§7.1) through the
      existing tree path: a template with function *f* is a leaf under the *f* node (the branch node if *f* is a branch,
      the leaf if *f* is a leaf; `null`/unresolved →
      the `jipipe:function:templates` catch-all leaf).
    - *Scope placement* — a special case in the tree **model** (not in `functionIds`): every
      template entry is additionally listed under the synthetic `Templates / <scope>` branch
      by its `TemplateScope`. This keeps the synthetic branch decoupled from the function
      registry — it is pure UI grouping keyed on the entry type + scope.
    - Selecting either the *f* subtree or the `Templates / <scope>` group lists the template
      in the node list alongside regular entries (de-duplicated by entry id if both are
      visible in the same query).
- All placements follow the node list's **relevance-ranked order** (the same order the
  add-nodes list already uses — the synthetic `Templates / <scope>` branch mirrors it);
  there is no separate ordering control for templates (user decision).
- Badges render as small chips in the tree-leaf renderer and list cell renderer (reusing existing template cell-renderer
  assets).
- *Naming note:* the synthetic top-level `Templates` branch (System/Global/Project
  children) is distinct from the `jipipe:function:templates` catch-all **function leaf**
  (under Miscellaneous). They coexist without ambiguity — the synthetic branch is a
  scope group, the leaf is a real function a user can pick in the picker.

### 7.3 The 1-pane context-menu bug fix

Compact mode (< 380 px, `NodeBrowserModel.TIER_COMPACT`) makes the tree take over the
node-list role, but tree leaves currently have **no context menu** — Insert at cursor /
Pin / Unpin / Show documentation are unreachable. Fix: **one shared menu-builder** used by
the list and the tree leaves (parity is structural, not copy-paste). For
Global/Project template entries the menu additionally carries the §8 quick actions (Edit / Copy / Delete / Configure
templates); System entries carry the read-only set.

## 8. UI consolidation

### 8.1 Removed

- The `NodeTemplateBox` dock in the pipeline editor (and its standalone tool-window mode)
  and its `NodeTemplatePopupMenu` mount points. Feature relocation below; nothing lost.

### 8.2 Kept in the shared surfaces (docked browser + finder dialog)

- **Insert** — list/tree context menu + drag (unchanged mechanism).
- **Edit template** — the existing template editor dialog (rename, description, icon, hue,
  graph preview) **gains the taxonomy-function picker** (§6). Quick actions in the context
  menu on Global/Project entries: Edit / Copy to Global / Copy to Project / Delete /
  Configure templates.

### 8.3 The "Configure templates" modal

New gear icon **next to the AI search button** in the finder dialog toolbar (and the
docked browser's control bar) + context-menu item "Configure templates" (on template
entries; opens the tab matching the entry's scope). A modal dialog with tabs:

- **System** — read-only list; "Copy to Global" (the escape hatch); no add/delete.
- **Global** — edit, rename, delete, copy to project,
  export, import. (In-modal "add" was ruled out: the canvas "Add template" action
  covers "from selection"; "new" is covered by import.)
- **Project** — same, scoped to the current project's metadata (project templates travel
  with saved projects).
- **Download** — the existing `NodeTemplateDownloaderDialog` embedded (repository URL +
  auth). This is the #1333 seam: the future repository swaps the tab's source.

Project-settings' `node-templates` management (the settings-UI hook at
`JIPipeDesktopProjectSettingsUI`) is replaced by this modal's Project tab.

## 9. Migration — `menuPath` → `taxonomyFunction`

Converter at deserialization time; one-shot, idempotent:

1. No `menuPath`, no `taxonomyFunction` (fresh) → unset `JIPipeTaxonomyFunctionRef` →
   catch-all placement.
2. `menuPath` present → match the **first segment** against registered function display
   names (case-insensitive + a small abbreviation table: Image→Images, Table→Tables,
   Track→Tracking, …; matching against **top-level branches before same-named
   leaves**). Match → a
   `JIPipeTaxonomyFunctionRef` holding that function id. No match → unset → catch-all.
3. `taxonomyFunction` present (a string id on disk → the ref) → no-op.
4. On conversion, the old menu path is appended to the template description as an italic
   footer line ("*Previously: Image → Thresholding*") so the auto-mapping is auditable;
   re-picking the function in the editor clears the line. (Flagged exception to the
   no-authored-content rule: it is user data preserved, not opinion.)
5. After one save cycle the `menuPath` key is gone from user files. Extension-shipped
   lists that still contain `menuPath` load via the converter until extensions update.

Tests: fixture of old-format JSONs (branch match, leaf match, no match, empty path,
already-has-function, unknown top segment) asserting the resulting function + placement;
a load→save→load round-trip proving the key disappears.

## 10. Relations — Similar / Previous / Next

### 10.1 The model

One index, three named directions, per node entry:

```java
record NodeRelations(List<Entry> similar, List<Entry> previous, List<Entry> next) {}
JIPipeNodeDatabase.relations(entry, maxPerList)   // the new public API
```

- **`similar`** — refactored from today's `related()`: same-function → same-framework →
  shared-tags tiers, **minus** workflow neighbors (previous/next entries are excluded so
  the three lists stay distinct).
- **`previous`** — nodes that run before this one (family predecessors ∪ template edges
  pointing into it).
- **`next`** — nodes that run after this one (family successors ∪ template edges pointing
  out of it).
- Ranking inside previous/next: edge **support** (number of distinct templates containing
  the edge), then name. Capped per list (`maxPerList`, default ~6).

### 10.2 Sources (pluggable)

`NodeRelationSource` interface, two sources now:

1. **`FamilyRelationSource`** — the family assembler's step graphs (adds the missing *predecessor* direction alongside
   the existing successor query).
2. **`TemplateRelationSource`** — mines **every** loaded template graph (System, Global, *and* Project — the user
   directed "mine the full extent from the templates";
   relations are per-entry database data, and the database already holds all scopes).
   Each in-template edge `A→B` contributes `next(A) += B` and `previous(B) += A`.
   Template edges feed the same relation index directly via
   `TemplateRelationSource` (a `NodeRelationSource` implementation, alongside
   `FamilyRelationSource`); no virtual family is introduced, so `FamilyAssembler`,
   the public `getFamilies()` views, and `TaxonomyFullValidationTest` are untouched.

Future (out of scope): a starter-pattern source, an LLM source, and #1333's repository
templates as source material — all attach without touching the model.

### 10.3 UI

- The detail rail's two sections (Related / Follow-ups) become three: **Similar**, **Previous steps**, **Next steps**
  (same click-to-navigate behavior; distinct icons).
- The browser's follow-up strip becomes the same three-group strip (workflow successors
  first, as today).
- `JIPipeNodeDatabase.followUps(entry)` and `related(entry, max)` are **removed**
  (all consumers are in-tree: rail, browser strip; 6.0.0 unreleased, break is contained).

## 11. Shipping the curated patterns as System templates

- The `utils/taxonomy-research/patterns/templates/*.json` files move to bundled
  resources `jipipe-core/src/main/resources/org/hkijena/jipipe/plugins/nodetemplate/patterns/`.
- A system-template registration step (taxonomy plugin `register()` loop using
  `registerNodeTemplate`) deserializes each, setting:
    - `taxonomyFunction` = the pattern's **primary function branch** (deterministic rule:
      the most common primary top-level branch among the pattern's nodes; ties →
      lexicographic). The pattern docs' `tags` block is the source data.
    - `name` = pattern name; `description` = corpus block (role sequence, wiring);
      `source = "System (corpus-mined)"`.
    - `colorHue`/icon derived from the function branch (visual clustering).
- **Curation (added after the 1324 manual review):** a pattern ships only if
  it has a proper **derivable name of what it does**. From the 185 research
  artifacts, the statistical fan shapes ("X fans in from N source ids") and
  every pattern containing a `jipipe:compartment-output` sink (workflow-
  specific plumbing) are excluded, and the survivors' shape-label duplicates
  are deduped → the shipped set (64 at this revision; a DAG-aware mining
  round is scheduled — `docs/superpowers/plans/2026-10-16-dag-pattern-mining.md`).
  The generator asserts the shipped set's function distribution and fails on
  any name that still carries a raw role token.
- At startup the `TemplateRelationSource` mines their edges → the round-9 ask ("every
  import-role node gains 'Annotate with path' as a follow-up") is realized from shipped
  data; the research's 6a table (416 rows) is what the index produces at runtime.

## 12. Testing & verification

- **Unit (jipipe-core):**
    - Migration converter fixtures + round-trip (§9).
    - `NodeRelationIndex`/`relations`: family + template merge, both directions, support
      ranking, previous/next exclusion from similar, pluggability via a fake source,
      stale-node-id skip (template edge whose node id no longer resolves → warn, skip,
      never fail).
    - `CreateNewNodesByTemplateDatabaseEntry` extension: function ids, scope, badge rule.
    - `jipipe:taxonomy-function` parameter type / `JIPipeTaxonomyFunctionRef`: custom
      serialization (id string ↔ ref, `null` ↔ unset), `isResolved()`/`getFunction()`
      behavior for known/unknown ids, equals/hashCode on id, no-arg constructor.
    - The `jipipe:function:templates` function passes the taxonomy gates (`TaxonomyFullValidationTest`, per-module
      conversion report) — it is a normal
      registered function leaf.
- **Integration (jipipe-launcher, mirrors `RegistryDumperTest`):**
    - All shipped patterns deserialize via `JIPipeNodeTemplate.getGraph()`
      (generalizes the existing `TemplateDataDeserializationTest` from 1 to the
      full shipped set), and the curation rules hold (no compartment sink, no
      statistical fan name).
    - Spot-check mined relations: `path-to-annotation-simple`'s next includes
      `import-imagej-imgplus-from-file`; `folder-list-files`' previous is empty, next
      includes `path-to-annotation-simple`.
    - Registry dumper: `jipipe:function:templates` appears in `functions[]`; the
      registry dumper's `families[]` output is unchanged by this feature (no new
      family is introduced).
- **UI:** context-menu parity — compact-mode tree leaf and list produce identical items
  for the same entry (the named bug); badge rendering in both renderers.
- **Regression guards:** `TaxonomyFullValidationTest` green; jipipe-core suite green (the removed `followUps`/`related`
  methods have their tests updated, not deleted).

## 13. Out-of-scope seams for the future

- **#1333 custom-node repository:** `source` strings, the Download tab, the Global-store
  import path, and `NodeRelationSource` are the four seams. A repository download is a
  Global template with a repository source string; nothing in this design forces a
  different mechanism later.
- **Starter patterns:** template *source* nodes (no incoming in-template edge) are
  computable from the same template graph — a future relation kind ("starts like
  this") or a new `NodeRelationSource` can surface them as "start candidates" with **no index/model change** (the index
  already stores only next/previous support
  maps; `previous(id)` is already empty for source nodes).
- **Template graph editing** in the management UI (canvas-based editing only).
