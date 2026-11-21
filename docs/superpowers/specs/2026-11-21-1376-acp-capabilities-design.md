# Design: ACP client, AI capability system, and AI panel (#1376)

Date: 2026-11-21. Work item: https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1376
Investigation report: `docs/investigations/1376-acp/investigation.md`.
Decisions D1-D7 below were made by the owner on 2026-11-21 and are constraints, not
proposals.

## Intent and why

Users should connect their own AI coding agents to JIPipe (Agent Client Protocol),
chat with them in a project-bound AI panel, and manage every AI-related piece from
one settings "store". The previous plan (an in-house llama.cpp agent, #1290 branch) is
descoped: a team member will rebuild the in-house agent from scratch later, plugging
into the same interfaces designed here (D1).

The work is also the moment to fix a structural problem: AI features have hidden
dependencies (MCP knowledge search needs the embedding model; the AI panel needs MCP
to be useful) and nothing tells the user before things fail. The capability system in
this design is the general fix, built domain-agnostic so the upcoming Python/R
overhaul and later a possible ImageJ-plugin domain (#1363) reuse it.

## Goals

- G1: JIPipe is an ACP client (v1 baseline): users configure agent launch configs,
  chat in a project-bound docked AI panel, permission asks render in the chat.
- G2: A capability registry answers, for humans, for JIPipe preflight, and for agents
  over MCP: "is capability X available; if not, what is missing and how big is it?"
- G3: The AI settings store shows capabilities as cards (grid) and agents as a list,
  with install/setup flows driven by the registry.
- G4: Project-bound chat sessions persist as proper .jipx spec entries (format bump)
  and survive project close/reopen, including continuity when the agent itself lost
  the session.
- G5: Ready-to-use prompts let an agent self-connect to the JIPipe MCP (HTTP-first,
  stdio fallback).
- G6: The Python/R overhaul can express "interpreter + packages" as capabilities with
  no AI-specific code in the core registry.

## Non-goals

- N1: The in-house llama.cpp agent (D1). No branch agent code (AgentSession, llmproxy,
  calibration, models.json) is transferred.
- N2: Global (project-independent) chat sessions: machinery is designed so they can
  be added, but v1 ships project-bound only (owner accepted the lean on 2026-11-21).
- N3: Remote/HTTP ACP agents (protocol remote transport is work-in-progress upstream);
  v1 is subprocess/stdio.
- N4: ACP v2. v1 baseline plus stabilized v1-era RFD extensions only.
- N5: Migrating the #1290 setup dialog's llama.cpp/model-package downloads.

## Anti-goals (failure even if it "works")

- A fake dependency chain the user cannot see: gating must name what is missing and
  what it costs, never just disable buttons.
- Losing a user's chat on project reopen when the agent cannot restore: continuity
  must work via transcript re-injection (owner ruling, see Restore ladder).
- AI-specific knobs leaking into the core registry: the registry must stay usable for
  Python/R and future domains.

## Constraints (decisions and codebase facts)

- D2: master wins; the #1290 branch donates parts only (chat/assistant UI packages).
- D3: ACP permission requests bridge into `AutomationPermissionService` via the
  `MCPAskPresenter` seam (`desktop/app/automation/MCPAskPresenter.java`, origin
  field on `PendingAsk`); additional JIPipe permissions may be added as needed.
- D4: if the store page crowds up, split into settings category "AI" with pages
  "Setup", "MCP", "Agents".
- D5: chat sessions are proper jipx spec entries; format version bumps;
  jipipe-cms-website gets updated.
- D6: MCP transport: HTTP endpoint + token first; stdio proxy fallback.
- D7: settings system (#1150) is finished; custom merged pages, categories, deep
  links, write-through (no save button).
- The AI toggle `AIApplicationSettings` (`org.hkijena.jipipe:ai`, `enable-ai`) gates
  all AI surfaces today; the AI status-bar item (`JIPipeDesktopAIManager`) is
  added/removed on toggle and hosts the MCP ask balloon today.
- The MCP layer (#1369) is live: `api/automation` (operations, permission service,
  127.0.0.1 endpoint, broker on 8781, optional bearer token), desktop integration
  with ask queue and 300 s bound.
- Official ACP Java SDK: `com.agentclientprotocol:acp-core:0.81.0` (Apache-2.0,
  Java 17+, Maven Central), JSON module separate. jipipe-core pins Jackson 2.18.0
  and already carries Jackson 3 via the MCP SDK (`io.modelcontextprotocol.sdk:mcp:2.2.0`).
- The ACP Registry (stabilized) is the agent catalog source:
  `https://cdn.agentclientprotocol.com/registry/v1/latest/registry.json`.
- Project workbench docks: `JIPipeDesktopDockPanel`; the branch registered the panel
  as dock `AI_ASSISTANT` in pipeline editor, compartments editor, project overview.
- .jipx: ZIP container, manifest with parts table and `unknown` map; 9 monolith part
  keys in `JIPipeProjectParts`; `format-version: 1`; reader/writer in
  `api/project/jipx/`; `JIPipeProjectSaveTarget` carries a forward-hook note about
  "the first jipx-exclusive feature".

## Approaches considered

### Capability system

- **A (chosen): capability registry with provides/requires and derived IDs.** One
  registry; capabilities and providers are registered; state is computed from the
  existing artifact/environment/service sources; requirements expand transitively;
  install flows offer ranked equivalents. Refined per owner feedback: domain-agnostic
  naming, `provides` edges, domain info on stable IDs, artifacts map in directly and
  are queryable over MCP.
- **B: capabilities as artifact-backed packages** with manifests and
  dependency-ordered installs reusing `JIPipeArtifactRepositoryApplyInstallUninstallRun`.
  Rejected as the *identity* model because it conflates downloadable artifacts with
  capabilities that have no artifact (MCP is a server toggle; an ACP agent is an
  executable). Artifacts remain the install mechanism under A.
- **C: status-only checklist** without a dependency graph. Rejected: loses the
  explicit chain ("install embedding before MCP") that the owner explicitly wants.

### Session restore (owner ruling)

- Native-only (`session/load` or nothing) was rejected by the owner: users care that
  continuity works, not how. Chosen: a restore ladder (below) whose last rung
  re-injects the stored transcript into a fresh ACP session.

### Store layout (owner ruling)

- Grid of cards for the few important capabilities; list rows for long lists
  (agents). Mockups approved 2026-11-21 (`.superpowers/brainstorm/`, session
  1150236-1795270756, `ai-store.html`).

## The design

### 1. Capability registry (core, domain-agnostic)

New package `org.hkijena.jipipe.api.capabilities` in jipipe-core. No new dependencies.

**ID model.** IDs are stable string literals with a provider kind prefix:
`artifact:<id>`, `system:<id>`, `jipipe:<id>`, `custom:<id>`. Concrete provider IDs
may carry variant/qualifier segments (for example
`artifact:com.github.mouseland.cellpose4.cu124`, `system:python3.v312`). Each
provider **derives** the less specific capability IDs it satisfies by stripping
qualifier segments (`system:python3.v312` -> `system:python3`), dropping the kind
prefix into the plain capability space (`python3`), and following declared family
aliases (`python3` -> `python`). Derivation rules live per kind; alias edges are
declared by the domain that owns the family. Requirements name capability-space IDs
optionally with version constraints (`python.package.scikit-image>=0.19`).

**Model types.**

- `JIPipeCapability`: stable id, display name, description, icon, domain tag,
  `requires` (list of requirement ids), and metadata used by stores (category, sort).
- `JIPipeCapabilityProvider`: registered by plugins; declares `provides` (the
  capability ids it can satisfy), a **presence probe** (is this provider currently
  present? artifact installed / environment configured / service enabled / executable
  found), and an **install/setup descriptor** (how to get it: artifact query + size
  hint, `auto_on_demand`, manual instructions, or nothing).
- `JIPipeCapabilityDomain`: id, name, icon, description; groups capabilities in UI
  and lets non-AI domains (python, r, later imagej) own their corner of the registry.

**State computation.** The registry computes per capability id:
`available` (at least one present provider satisfies it and all `requires` resolve
to available), `needs_setup` (a provider candidate exists that is installable or
configurable), `blocked` (requirements exist whose chain cannot be satisfied -
report the chain), or `missing` (no provider registered at all). State is **never
persisted**; it is derived on demand from the artifact repository, environment
configurators, and service state. Cycles in `requires`/alias edges are rejected at
registration.

**Query result** (this exact shape is the MCP tool answer):

```
{
  "id": "cellpose4",
  "state": "needs_setup",
  "blockedBy": [],
  "install": { "method": "auto_on_demand", "size": "4GB" },
  "setup": null,
  "candidates": [ { "id": "artifact:com.github.mouseland.cellpose4.cu124", ... } ]
}
```

**Install flow (owner ruling).** When the user activates a capability whose
requirements are not met, the prompt reads: "This will require the following
additional capabilities. Install them?" One row per missing capability, each with a
**combobox of ranked equivalents** (preselected default if one exists, empty
selection allowed if none), size totals, and the install order. Confirm installs in
dependency order through the existing artifact runs / environment setup tools.

**MCP exposure.** New automation operations `get_capability_status`,
`list_capabilities` (read-only), and `install_capability` (Ask permission), so an
agent can answer "is cellpose4 installed?" and tell the user "you need the 4 GB
model first" before anything fails.

**First tenants.** Domain `ai`: `embedding` (provider: artifact
`cn.ac.baai.bge_small_en:*` + the embedding service probe), `mcp`
(requires `embedding`; provider: the MCP settings toggle), `acp-client` (requires
`mcp`; provider: any ACP launch config). Domain `acp`: `agent` (providers: installed
agents; many providers satisfy one capability). Domain `python`/`r` as the Python/R
overhaul lands: interpreters and packages, with adapters over
`PythonPackageLibraryEnvironment`/`REnvironment` (bundled package sets become
provided capabilities; `provided-by-environment` maps to "all packages of the
environment are provided").

**Owner-refinement notes.** The system name is generic (capability system, not "AI
capability system"); domains carry the domain info; nothing in the AI work is wasted
for Python/R (owner: "nothing is wasted").

### 2. AI settings store (application settings)

- New custom merged page anchored on the existing sheet
  `org.hkijena.jipipe:ai` (keyboard-section pattern: `registerMergedPage` +
  `registerAbsorbedSheet`; the MCP sheet `org.hkijena.jipipe:mcp` is absorbed so
  there is one AI page). The `enable-ai` switch sits in the page header.
- Layout per approved mockup: capability **cards in a grid** (embedding, MCP, ACP
  client; later upscaler/decision model), each card showing state chip, description,
  `provides`/`requires` line, and the action button (Install ~size / Enable / Set up
  / Configure). Agents section is a **list**: name, source, `provides
  acp:agent` line, state chip (Ready / Sign-in required / Not installed), actions
  (Chat / Sign in / Install), plus "+ Add custom agent".
- If the page crowds up later, split per D4 into category "AI" with pages "Setup",
  "MCP", "Agents" (the settings tab supports categories).
- Agent catalog: fetched from the ACP Registry JSON (cached, refresh button),
  merged with user-defined custom agents. Registry availability failures degrade to
  the custom list with a notice.

### 3. ACP client layer

- Dependency: `com.agentclientprotocol:acp-core:0.81.0` pinned + `acp-json-jackson3`
  (rides the Jackson 3 stack already present for the MCP SDK; no change to the
  Jackson 2 pin). dist-info.json gains the jars.
- Home: jipipe-core, package `org.hkijena.jipipe.api.acp` (engine, session records,
  launch configs) plus desktop glue in `desktop/app/ai/`. This mirrors the MCP SDK
  precedent (jipipe-core already carries the MCP dependency); a dedicated module is
  not justified for v1 and can be extracted when the in-house agent plugin lands.
- `AcpAgentLaunchConfig`: stable string-literal id, display name, executable, args,
  env, working-directory rule; serialized with the settings. Registry-catalog entries
  prefill known launch args (e.g. `gemini --experimental-acp`).
- `AcpConnectionManager`: one ACP connection per launch config (subprocess, stdio),
  lazily spawned, capability negotiation recorded (`NegotiatedCapabilities`), shared
  across projects; multiple sessions multiplexed per connection.
- `AcpSessionRecord` (JIPipe-owned, stable id): agent launch config id, title, mode,
  created/updated, transcript (below), agent-side `sessionId` if known, cwd.
- Lifecycle: `initialize` -> (auth: `agent` method via `authenticate`; `terminal`
  auth shows the interactive login flow in a terminal panel) -> `session/new(cwd,
  mcpServers)` -> prompt turns. Prompts carry the MCP server config pointing at the
  JIPipe endpoint when MCP is available (per capability state, D6: HTTP config with
  token; stdio proxy fallback, see section 6).
- Client capabilities advertised: `fs.readTextFile/writeTextFile` (scoped to the
  session root set), elicitation `form` (JIPipe renders it from parameter-schema
  machinery); `terminal` and `url` elicitation are **not** advertised in v1.
- Update handling: `session/update` variants map onto panel renderers; `usage_update`
  feeds the session header; `session_info_update` renames sessions; notices render
  inline; compaction/subagent updates are rendered if advertised, ignored gracefully
  otherwise.

### 4. Restore ladder (owner ruling: it must work)

1. **Agent-native**: agent advertised `loadSession`/`sessionCapabilities.resume` and
   the recorded agent-side session id still resolves -> `session/load` (or `resume`);
   badge "Agent-native restore".
2. **Transcript re-injection**: otherwise, export the stored transcript to a Markdown
   file under the session working context (inside the agent root set), then start a
   fresh session and prime it with the previous conversation: attach the transcript
   as an embedded `resource` content block on the first `session/prompt` when the
   agent advertises `promptCapabilities.embeddedContext`, otherwise reference the
   file path in the first prompt text ("read this file; it is the transcript of our
   previous conversation; treat it as history"). Badge "Context restored from
   transcript". Oversized transcripts are head+tail truncated with a note.
3. The identical export-and-inject path implements **agent handoff**: switching a
   session's agent reuses rung 2.

The stored transcript is the normalized render model (section 5), so rung 2 is
always possible and the UI history never depends on agent cooperation.

### 5. AI panel and JIPX session storage

- **Extraction (D2)**: take from `origin/1290-ai-llm-integration` the packages
  `desktop/app/ai/assistant/` and `desktop/app/chat/` (panel, ChatViewPanel,
  SessionListPanel, PermissionPanel, PlanPanel, chat renderers, entry types) plus
  the workbench routing (`startAIChatSession`/`sendToAI`). Master's versions of the
  colliding simple names (`JIPipeDesktopAISetupDialog`, `JIPipeDesktopAIMonitor*`,
  `JIPipeDesktopAIManager`) win; the branch setup dialog is not migrated (N5).
- **Adaptation**: replace the `ProjectAIChatState`/`AgentSession` engine with the ACP
  client (section 3): the ~15 chat-state events are re-emitted from ACP
  `session/update` handling; `AgentState`/`StageReviewProvider`/`SpeedTestUtils`/
  `ProxyStatus` couplings are removed. Permission and plan UI reuse the existing
  panels. The "setup required" card keys off the capability registry, not
  model presence.
- **Docking**: dock id `AI_ASSISTANT` (gated on `enable-ai`) in the pipeline graph
  editor, compartments editor, and project overview, as on the branch. Register
  command-bar/shortcut actions with stable ids (`org.hkijena.jipipe:ai.open-panel`,
  `org.hkijena.jipipe:ai.new-session`) per the shortcut registry rules.
- **JIPX (D5)**: new manifest-listed part key `ai-sessions` holding the project's
  `AcpSessionRecord` list (normalized transcript: message chunks grouped by
  `messageId`, tool calls with status/content, plans, collapsed thoughts, usage).
  `JIPipeProjectParts.PART_KEYS` gains the key; `format-version` bumps 1 -> 2; the
  reader accepts both versions (v1 projects simply have no sessions part); the
  writer emits v2; round-trip and slice/assemble tests extend to the new part;
  jipipe-cms-website documents the entry. This is expected to be the
  "first jipx-exclusive feature" the save-target note anticipates.

### 6. Ready-to-use prompts and MCP bridging

- "Copy instructions for agent" on the MCP card (and in the panel) generates a
  ready-to-paste prompt containing: the MCP endpoint URL, the bearer token (from OS
  secret storage, never logged), the agent-side config snippet for the two supported
  transports, and a request that the agent emit its ACP launch JSON for the custom
  agent dialog when it is not in the catalog (the dialog validates that JSON).
- D6: HTTP transport config is the default; agents without `mcpCapabilities.http`
  get a stdio fallback: a small bundled proxy process (stdio MCP in, HTTP out to the
  local endpoint) shipped as a launcher class; the prompt auto-selects per agent
  capability info from the catalog.

### 7. Permission bridging (D3)

- ACP `session/request_permission` (options `allow_once`, `allow_always`,
  `reject_once`, `reject_always`; the agent owns remembered decisions) renders in
  the chat panel through a new `AcpPermissionPresenter` (resolution 3), separate
  from `MCPAskPresenter`. The reserved `MCPAskPresenter` seam is not modified.
- Mapping: MCP operation Ask -> chat permission dialog with allow/reject options;
  `allow_always` writes back through the existing `grantAlways` path when the tool
  call corresponds to a JIPipe-MCP operation. Ask bubbles (statusbar) remain the
  fallback when no ACP session is active.
- JIPipe-side additions per D3 as needed (for example an explicit permission for
  `install_capability`).

### 8. Status integration

- ACP session activity feeds `LLMActivityTracker` phases so the AI status-bar graph
  stays truthful; the AI manager gains an "ACP sessions" entry pointing at the panel
  (its menu items may become registered shortcut actions so the command bar sees
  them).

## Builder-check resolutions (2026-11-21)

Answers the builder needed before planning. Items 1-3 were open questions; the rest
are calls made here and marked as such.

1. **SDK resolvability and JSON module (verified).**
   `com.agentclientprotocol:acp-core:0.81.0`, `acp-json-jackson3:0.81.0`, and
   `acp-test:0.81.0` are all on Maven Central (metadata latest = 0.81.0). `acp-test`
   ships `InMemoryTransportPair`, `MockAcpAgent`, `MockAcpClient`. Use
   `acp-json-jackson3` (declares `acp-core` + Jackson 3 databind); no extra Maven
   repository is needed. Pin all three at 0.81.0. Add them to `dist/dist-info.json`.
   Risk to watch at build time: transitive Jackson 3 versions from the ACP SDK vs the
   MCP SDK 2.2.0 stack - if they differ, align via the existing dependencyManagement
   (the MCP SDK already dictates the Jackson 3 line).
2. **API surface.** The builder may download the SDK jars, sources, or Javadoc into
   `/tmp/opencode` (it is the approved scratch area) and read the upstream repo at
   github.com/agentclientprotocol/java-sdk for planning. The golden path in the README
   is representative: `AcpClient.sync(new StdioAcpClientTransport(AgentParameters...
   .build())).sessionUpdateHandler(n -> ...).build()`, then `initialize()`,
   `newSession(new NewSessionRequest(cwd, mcpServers))`, `prompt(new PromptRequest(id,
   blocks))`. `NegotiatedCapabilities` exposes `supports*()` and `require*()`.
3. **Permission bridge direction (call).** `MCPAskPresenter` is presentation-only for
   MCP-originated asks; it is NOT reused for ACP. Add a parallel
   `AcpPermissionPresenter` interface in the ACP desktop glue (post / answer / cancel /
   drop-all), implemented by the chat panel, registered by the
   `JIPipeDesktopMCPIntegration`-owned ACP session manager when a session is active.
   ACP permission options are independent of `AutomationPermissionState`; only the
   `allow_always` / `reject_always` outcomes mirror into the JIPipe
   `AutomationPermissionService` (via the existing `grantAlways`-equivalent path) for
   the matching operation when the tool call is a JIPipe-MCP operation. The reserved
   `MCPAskPresenter` seam is NOT modified; a separate presenter is added.
4. **V1 client capabilities (calls).**
   - `fs`: root set = the session `cwd` (project directory) plus any
     `additionalDirectories` the session was created with; handlers reject paths
     outside that set with `-32602` and never follow symlinks out of it.
   - elicitation `form`: advertise `form` only. Accept flat JSON-schema objects with
     primitive/enum properties (string, number, integer, boolean, enum), render with
     the existing parameter-form machinery, pre-populate defaults, validate on submit.
     Do not advertise `url` mode.
   - `terminal`, `url` elicitation, `subagents`, `session.compaction`: not advertised.
5. **Agent registry and install (verified + call).**
   The live registry (`.../registry/v1/latest/registry.json`) is a JSON object with
   `version`, `agents[]`, `extensions`. Each agent has `id`, `name`, `version`,
   `description`, `repository`, `website`, `authors`, `license`, `icon`, and
   `distribution` which is one of:
   - `npx: { package, args }` (23 agents),
   - `uvx: { package, args }` (2 agents),
   - `binary: { <platform-key>: { archive, cmd, sha256 } }` (24 agents; platform keys
     like `darwin-aarch64`, `linux-x86_64`, `windows-x86_64`).
   Install model (call): `binary` distributions download the platform archive,
   verify sha256, unpack under `<userDir>/ai/acp-agents/<agent-id>/<version>/`;
   `npx`/`uvx` distributions are launched via the respective CLI if present on PATH
   and otherwise show "requires Node"/"requires uv" with a manual instruction - JIPipe
   does not install Node or uv. A custom agent is always a binary-style launch config
   (executable + args + env). State mapping: `binary` installed against sha256 ->
   Ready; `npx`/`uvx` with CLI present -> Ready; otherwise -> Not installed /
   Requires Node/uv. The builder may fetch the live registry JSON into `/tmp/opencode`
   for field-accurate planning.
6. **Session portability across machines (call).** `AcpSessionRecord.agentSideSessionId`
   and `cwd` are machine-specific hints, never contract. On reopen, rung 1
   (agent-native) is attempted only when the recorded `cwd` exists locally AND the
   agent still reports that session (or `session/load` succeeds); otherwise fall
   straight to rung 2 (transcript re-injection). Record carries the cwd it was created
   with; a mismatch is not an error, it just skips rung 1. Tests include a
   "reopened at a different cwd" fixture asserting rung-2 behaviour.
7. **`install_capability` execution model (call).** `install_capability` is an
   `AsyncAutomationOperation` returning an `InstrumentationJob`; it streams progress
   through the existing progress store and publishes a capability-changed event on
   the automation event bus. It uses Ask permission. The blocking single-capability
   getter remains `get_capability_status` (implemented with a fail-closed guard for
   installs running concurrently on the same capability, returning the active job id).
8. **Format bump scope (confirmed: jipx only).** D5's `format-version` 1 -> 2 is the
   jipx manifest version in `api/project/jipx/`. `JIPipeProject.CURRENT_PROJECT_FORMAT_VERSION`
   (mono-file projects) is NOT bumped. The reader keeps accepting v1 jipx (without an
   `ai-sessions` part). The jipipe-cms-website update documents the jipx entry only.

## Testing (per project test policy)

- Registry: derivation chains, alias edges, transitive requirements, cycle
  rejection, ranked candidates, state computation - non-trivial logic, unit tested.
- Restore ladder: transcript export/injection prompt construction - unit tested with
  the SDK's `acp-test` in-memory transport pair.
- JIPX: round-trip with `ai-sessions` across format versions; slice/assemble
  contract for the new part.
- No Swing interaction tests; the owner verifies UI by eye.

## What is left to the builder (explicit non-decisions)

- Exact Java package/class names inside `api/acp`, event plumbing between the
  connection manager and panel renderers, and the transcript render-model class
  shape.
- The stdio proxy launcher design and its packaging.
- Transcript truncation threshold and head/tail policy.
- Whether MCP settings fields move onto the store page or stay behind "Configure"
  (visual call during implementation; sheet ids stay stable either way).
- Registry catalog caching details (refresh cadence, offline behavior beyond the
  stated degrade-to-custom-list).
