# Investigation 1376: Implement ACP

Status: investigation (2026-11-21). Work item: https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1376
(OPEN, labels AI / Doing / Feature, milestone "JIPipe AI").

## What the work item asks

JIPipe so far aimed at an in-house agent for small local LLMs (#1290). The agent
landscape has moved: users want to connect their own agents. #1369 (MCP server) covers
tool access, but a project-bound AI chat panel is still preferable for many users. The
work item proposes the Agent Client Protocol (ACP) as the unifying layer, with these
parts:

1. **ACP implementation + UI** - JIPipe becomes an ACP client; agents are subprocesses
   the user configures. The AI panel UI from the #1290 branch is extracted and adapted
   to ACP. The in-house agent moves to a new plugin and later connects through the same
   interfaces ("the in-house agent as one ACP client", also #1379).
2. **Unified AI settings page** - the current AI setup dialog (download/configure local
   models) was never designed for ACP and needs a full redesign. The new application
   settings system (#1150) enables custom UI. One page as control center for all
   AI-related settings, including the "Disable AI" switch.
3. **AI registry and capability redesign** - a new API abstracting an "AI capability
   package" that can be enabled/installed; packages encapsulate capabilities (embedding,
   agent, ...). MCP itself becomes a capability with a dependency on embedding. Motivated
   by the growing AI surfaces (embedding for MCP knowledge search, GAN upscaler,
   decision model / jev, ...).
4. **Available agents** - offer out-of-the-box ACP-capable agents like JetBrains does;
   investigate an existing repository of agents vs handrolled integrations. Users can
   add custom agents (Environments as a likely base).
5. **Ready-to-use prompts** - copyable instructions that let an agent connect itself to
   the JIPipe MCP, and that make an unsupported ACP agent emit a JSON the custom-ACP
   setup dialog can validate and ingest.
6. **Existing settings transfer** - existing AI settings move into the new system.
7. **AI panel** - ACP-capable, full standard. Requires investigation: project-bound vs
   global sessions; storing the session in JIPX (project-bound only).

Related: #1290 (AI LLM integration branch, source of the panel UI and the in-house
agent), #1369 (MCP server, merged), #1379 (agentic MCP interactions; permission asks
must redirect from the statusbar balloon into the active agent's chat once #1376 lands),
#1150 (settings system), #753 (.jipx format), #1286 (AI umbrella), #1294 (KDB).

## ACP: what the protocol actually is

Agent Client Protocol (https://agentclientprotocol.com), initiated by Zed, now governed
with JetBrains on board (Junie, AI Assistant speak ACP). It is to coding agents what
LSP is to language servers: JSON-RPC 2.0 between a **Client** (here: JIPipe) and an
**Agent** (e.g. Gemini CLI, Claude Agent, Codex, OpenCode). The client boots the agent
as a subprocess and talks over stdio (stable); remote transports (Streamable HTTP /
WebSocket) exist as an RFD and in the SDKs, but remote scenarios are declared
work-in-progress in the docs. One connection can serve multiple concurrent sessions.
Content blocks and several payloads are re-used from MCP; Markdown is the default for
user-readable text.

### Protocol versions

- **v1** is the stable baseline (`protocolVersion: 1`). Every agent on the market today
  speaks it.
- **v2** is in Preview (RFD-driven: background work, subagents, replay/retry rules,
  message updates). Not yet a target for third-party clients.
- Recommendation: implement the **v1 baseline plus the stabilized v1-era RFD
  extensions** (session/list, session/resume, session/close, session/delete, elicitation,
  terminal auth, session config options, session notices, compaction, subagents,
  usage_update, tool call names, message IDs, client system prompt). Track v2, do not
  target it. The official Java SDK (below) exposes exactly this split
  (`@UnstableAcpApi` marks the unstable surface).

### Lifecycle and methods (v1)

1. **initialize** - client sends its `protocolVersion`, `clientCapabilities`,
   `clientInfo`. Agent answers with negotiated version, `agentCapabilities`,
   `agentInfo`, `authMethods`. Omitted capabilities mean unsupported (both directions).
2. **Authentication** (optional) - `authMethods` entries are type `agent`
   (client calls `authenticate(methodId)`) or `terminal` (client launches the configured
   agent invocation interactively with the method's `args`/`env`, waits for exit 0, then
   reconnects; requires `clientCapabilities.auth.terminal`). `session/new` fails with
   `auth_required` until authenticated. `logout` requires `agentCapabilities.auth.logout`.
3. **Session setup** - `session/new(cwd, mcpServers, ...)` returns a `sessionId`.
   `session/load(sessionId)` (capability `loadSession`) replays the full conversation as
   `session/update` notifications. `session/resume(sessionId)` (capability
   `sessionCapabilities.resume`) reconnects without replay. `session/close`,
   `session/delete`, `session/list` are separate capabilities. Optional on lifecycle
   requests: `additionalDirectories` (extra workspace roots) and `systemPrompt`
   (append or override mode).
4. **Prompt turn** - client sends `session/prompt(prompt: ContentBlock[])`; agent
   streams `session/update` notifications and finally responds with a `stopReason`
   (`end_turn`, `max_tokens`, `max_turn_requests`, `refusal`, `cancelled`). Client can
   interrupt with the `session/cancel` notification; pending permission requests must
   then be answered `cancelled`.

`session/update` variants (each behind its own client capability where noted):
`user_message_chunk`, `agent_message_chunk`, `agent_thought_chunk` (message chunks may
carry `messageId`), `tool_call`, `tool_call_update`, `plan` (+ `plan_update`,
`plan_removed` behind the `plan` capability), `available_commands_update` (slash
commands), `current_mode_update`, `config_option_update`, `session_info_update` (title,
updatedAt), `usage_update` (context tokens used/size, cumulative cost - mapped 1:1 to
JetBrains' session cost display), `notice` (live, not history; behind `session.notices`),
`compaction_update` + `compaction_summary_chunk` (behind `session.compaction`),
`subagent_update` + `session_message` + `session_message_chunk` (behind `subagents`).

### Client-side methods the agent may call

- `session/request_permission` - the central UX hook. Options carry
  `kind: allow_once | allow_always | reject_once | reject_always`; the **agent owns**
  remembered decisions (scope and duration are the agent's business; `kind` is a UI
  hint). This is exactly the seam #1379 needs: when an ACP session owns the chat,
  MCP operation asks (AutomationPermissionService Ask state) should route into this
  dialog rendered in the AI panel instead of the statusbar balloon.
- `fs/read_text_file`, `fs/write_text_file` - gated by `fs` capabilities. For JIPipe
  these should map to the session's root set (project directory / cwd).
- `terminal/create|output|release|wait_for_exit|kill` - gated by the `terminal`
  capability. Purely opt-in; JIPipe can refuse to advertise it initially.
- `elicitation/create` - structured input from the user, MCP-derived. Two explicit
  modes: `form` (restricted flat JSON schema; must not carry secrets) and `url`
  (out-of-band OAuth-style flow with `elicitationId` + `elicitation/complete`). ACP is
  stricter than MCP: each advertised mode must be explicit (`{}` advertises nothing).

### MCP integration (this is where #1369 and #1376 meet)

- `session/new` / `session/load` / `session/resume` take `mcpServers[]`. All agents
  **must** support the stdio transport (`name`, `command`, `args`, `env`); HTTP and SSE
  are optional agent capabilities (`mcpCapabilities.http/sse`). The client passes the
  configs; the **agent** connects to the MCP servers directly.
- A client that wants to give the agent its own tools exposes them as an MCP server and
  lists it. For JIPipe that server already exists (#1369: loopback endpoint with
  per-endpoint secret, optional shared bearer token, broker on port 8781). Consequence
  for "copy instructions for agent": the ready-to-use prompt must include the URL and
  token and works out of the box only for agents with `mcpCapabilities.http`; for
  stdio-only agents a small stdio-to-HTTP proxy (tunneled back into JIPipe) is the
  pattern ACP documents. The MCP-over-ACP RFD proposes carrying MCP requests over the
  ACP connection itself (no separate endpoint) - not stabilized, worth tracking.
- Security rules the settings UI must respect: never send stdio MCP configs derived
  from workspace files/agent output without user approval; keep `env`/header values out
  of logs and session state; persist credentials in OS secret storage.

### Transports

stdio (subprocess) is the stable, universal transport. The Streamable HTTP + WebSocket
RFD is implemented by the SDKs (loopback-bound listeners by default, origin checks) and
the docs still classify full remote support as work in progress. For #1376 the
subprocess/stdio path is the v1 scope; the HTTP path matters only for the in-house
agent and possible remote agents later.

## Official Java SDK (fit check)

https://github.com/agentclientprotocol/java-sdk - artifact
`com.agentclientprotocol:acp-core:0.81.0` on Maven Central, Apache-2.0, Java 17+
(JIPipe is Java 21, fine).

- Modules: `acp-core` (models, JSON-RPC plumbing, client + agent sides, sync/async
  APIs), JSON impl **not included** since 0.18.0: pick `acp-json-jackson2`
  (requires Jackson 2.18.1+) or `acp-json-jackson3` (requires Jackson 3.0+; identical
  wire bytes). Transports: `StdioAcpClientTransport`, `WebSocketAcpClientTransport`,
  `StreamableHttpAcpClientTransport` (+ Jetty/servlet/WebFlux agent-side hosts).
- Client shape: `AcpClient.sync(transport).sessionUpdateHandler(...).build()`, typed
  handlers for fs/terminal/elicitation/permission, `NegotiatedCapabilities` with
  `supports*()` checks and `require*()` guards, structured errors
  (`AcpErrorCodes.AUTHENTICATION_REQUIRED` ...), `acp-test` module with
  `InMemoryTransportPair` + `MockAcpAgent`/`MockAcpClient` for non-trivial tests.
- Versioning: the SDK follows the protocol (no strict semver); pin the exact version
  per the project's dependency policy (dist-info.json for external jars).
- JIPipe dependency fit: master pins Jackson 2.18.0 (root pom dependencyManagement;
  annotations bumped to 2.21 for the Jackson 3 stack of the MCP SDK
  `io.modelcontextprotocol.sdk:mcp:2.2.0` in jipipe-core). Either bump Jackson 2 to
  2.18.1+ for `acp-json-jackson2`, or use `acp-json-jackson3` which rides the Jackson 3
  stack already present for MCP. Decision for the design phase; jackson3 avoids touching
  the JIPipe-own Jackson 2 stack, jackson2 keeps one mapper for our own code paths.
- SDK logging caveat (security): the stdio client logs agent stderr at INFO on logger
  `com.agentclientprotocol.sdk.client.transport.agent-stderr` and full payloads at
  DEBUG/TRACE - route or silence in JIPipe's logging setup.

## Agent ecosystem and the registry (answers "repository or handroll")

- **Registry exists and is stabilized**
  (https://github.com/agentclientprotocol/registry): curated, auth-supporting agents,
  machine-readable at
  `https://cdn.agentclientprotocol.com/registry/v1/latest/registry.json` with per-agent
  metadata **including distribution information for automatic installation** and an
  `agent.json` schema. This is the Jetbrains-"store" data source the work item's UX
  sketch wants; no handrolled per-agent integration code is needed for listing.
- **Agents**: 50+ listed, including Gemini CLI, Claude Agent (adapter), Codex (adapter),
  OpenCode, GitHub Copilot (preview), Cursor, Cline, Goose, Qwen Code, Kimi Code, Junie,
  Kiro CLI, Factory Droid, Devin CLI, Mistral Vibe, and adapters for local models.
- **Clients for reference**: Zed, JetBrains (AI Assistant/Air), VS Code extension,
  plus the Neovim/emacs ecosystem.
- Practical consequence: JIPipe's "available agents" store = registry fetch (cached,
  like dist-info style curated sets) + manual custom entries. Custom agents map well
  onto the **Environments** concept (executable + args + env + working directory),
  which is also where the in-house agent plugin plugs in.

## UX reference (JetBrains Air)

The blog post the work item links shows the established pattern: a **Manage Agents**
store (detect already-installed compatible agents, install from registry, connect
custom ones), agent **sessions as editor tabs** with per-session status, attach-context
input, diffs and changed files reachable from the session, session-level cost estimates,
and per-task agent choice. Maps onto JIPipe as: the AI panel as a dockable in the
project window (project-bound sessions) plus global sessions in a standalone window,
session list from `session/list` + `session_info_update`, cost from `usage_update`,
diffs from `tool_call` content type `diff`, review/navigate via `toolCallLocation` and
the #1379 focus-awareness tools.

## Current state on master

### The AI toggle and the AI status-bar item

- `plugins/ai/AIApplicationSettings.java` is the global switch: sheet
  `"org.hkijena.jipipe:ai"`, parameter `enable-ai` (default `true`), plus the
  `embedding-model` override resolved through `getEmbeddingModelEnvironment()`
  (defaults to artifact query `cn.ac.baai.bge_small_en:*` via `AIPlugin.DEFAULT_EMBEDDING_ARTIFACT`).
  Static `getInstance()` accessor; 16 read sites across launcher, node database/search,
  browser/finder, embedding tools, and the MCP gate in the GUI launcher
  (`GuiCommand.java:203-211,498`).
- `desktop/commons/components/ai/statusbar/JIPipeDesktopAIManager.java` is the unified
  AI status-bar item (AI button + phase/MCP status graph + popup with embedding
  load/unload, monitor deep link, and `openSettingsSection` deep links). It is created
  in `JIPipeDesktopProjectWorkbench` and added/removed live by
  `updateAiStatusBarItem()` on settings change - the add/remove-on-toggle pattern any
  ACP chat surface should reuse.
- The MCP ask bubble is shown from the AI manager (`refreshAskBubble`/`showAskBubble`,
  BalloonTip registered on the project window); allow/grant-always answers go through
  `JIPipeDesktopMCPIntegration.answerAsk`.

### The MCP layer and the reserved ask-presenter seam

- Operation layer `api/automation` (AutomationOperation/registry/context/events,
  ~60 operations in `operations/*`), endpoint `api/automation/endpoint`
  (`AutomationInstanceEndpoint` on 127.0.0.1 with `X-JIPipe-Endpoint-Secret`,
  `MCPBrokerClient` with discovery file `<userDir>/mcp/discovery.json`), broker
  `servers/mcp` (`MCPBrokerServer`, streamable HTTP MCP on `/mcp` + control routes,
  optional bearer token), desktop integration `desktop/app/automation/JIPipeDesktopMCPIntegration`
  (lifecycle hooks installed by the GUI launcher, ask queue with a 300 s bound,
  `grantAlways` writeback into `AutomationPermissionService`).
- **The seam for #1376 already exists**: `MCPAskPresenter.java` (interface) - its
  Javadoc says verbatim that "the ACP integration (#1376) will register a presenter
  that redirects asks into the active agent's chat". `JIPipeDesktopMCPIntegration`
  has `setAskPresenter`; resolution (PendingAsk latch, timeout, grant writeback) is
  fixed, only presentation is replaceable. `PendingAsk` carries a free-form `origin`
  field (endpoint constant `ASK_ORIGIN_MCP = "mcp"`), so ACP-originated asks are
  distinguishable. Today's balloon behavior is preserved by the private
  `DefaultAskPresenter`.
- AI monitor pages (`JIPipeDesktopAIMonitorWindow`: Overview/Log/MCP/MCP operations/
  broker log/embedding log) are where MCP controls and logs live today; the MCP
  settings sheet (`"org.hkijena.jipipe:mcp"`) is a plain parameter sheet with a custom
  permission-table editor.

### Settings system (#1150) - what exists on master

- Framework `api/settings/`: `JIPipeSettingsSheet` with
  `JIPipeApplicationSettingsSheet` / `JIPipeProjectSettingsSheet` scopes
  (`SettingsScope`: APPLICATION/PROJECT/RUNTIME), registered by plugins via
  `registerApplicationSettingsSheet(...)` into `JIPipeApplicationSettingsServiceComponent`.
- Persistence is JSON documents (not literally a database): `<userDir>/settings.json`
  (application), `<userDir>/runtime-settings.json` (runtime), and the `"settings"`
  member inside saved projects; atomic writes with `.bad-` backups
  (`SettingsFileStore`), 5.x to 6.0 migration, two-way sheet-document sync
  (`SettingsSheetBridge`).
- Rendering: `desktop/app/settings/JIPipeDesktopSettingsTab` - no Save button (writes
  through immediately), default rendering is an auto-generated parameter form from the
  sheet's parameter tree, and **custom pages are supported** via the merged-page
  mechanism (`registerMergedPage(sheetId, page)` / `registerAbsorbedSheet`):
  `JIPipeDesktopKeyboardSection` (fully custom keyboard page),
  `JIPipeDesktopAppearanceSection`, `JIPipeDesktopRawSettingsSection` are the working
  examples, registered from `StandardSettingsPlugin`. Deep links:
  `JIPipeDesktopProjectWorkbench.openSettingsSection(scope, sheetId)`.
- Per-parameter custom editors are a second seam
  (`registerParameterType(..., editorClass)`), used e.g. for `MCPPermissionStates`
  and for environment editors.
- Work item #1150 itself is still OPEN with no linked MR - the infrastructure the
  #1376 description leans on exists on master; confirm with the owner what remains
  open in #1150 before building on it.
- Conclusion for #1376: the unified AI settings page can follow the
  keyboard-section pattern (custom merged page anchored on the existing
  `"org.hkijena.jipipe:ai"` sheet, absorbing the MCP sheet), with deep links from the
  AI manager/panel.

### Environments (base for custom agents)

- `api/environments/`: `JIPipeEnvironment` base, `JIPipeProcessArtifactEnvironment`
  (artifact-or-path + process launching - the closest model for "external ACP agent"),
  configurators with sources, setup tools, archetypes (Base/Optional/List).
- Registration: `JIPipeDefaultJavaPlugin.registerEnvironment(id, artifactQuery,
  archetype, environmentClass, optionalClass, listClass, ...)` registers parameter
  types `env:<id>`, `env:<id>:optional`, `env:<id>:list` and wires the shared editor
  `JIPipeDesktopExternalEnvironmentParameterEditorUI`. Existing examples: Python
  (`PythonEnvironment`: executable + arguments expression + env-var pairs, System/
  Conda/VirtualEnv, with setup tools), R, and the embedding model environment
  (`EmbeddingModelEnvironment` registered by `AIPlugin`).
- An "external ACP agent" (command + args + env + cwd) is the same shape; the
  environment system also gives the per-project override machinery for free
  (project settings sheets grow one entry per registered environment).

### AI capability surfaces today

- **Embedding** is the only real model surface on master: `AIPlugin` (registers
  `ai-embedding-model` environment), `JIPipeAIServiceComponent` (owns the
  `EmbeddingModelService` microservice + executors), model runners
  (`JIPipeEmbeddingAIModelRunner`, ONNX and API variants), the spawned
  OpenAI-compatible embedding server (`plugins/embeddingserver/`,
  `EmbeddingServerInstance`), consumers (tiered global/local embedding DB in
  `api/nodes/database/`, `KnowledgeService`, node browser/finder AI search), and the
  existing setup dialog `desktop/commons/components/ai/JIPipeDesktopAISetupDialog`
  (the one the work item retires/redesigns).
- `api/ai/llm/activity/LLMActivityTracker` tracks LLM phases (E/T/R/C) for the status
  graph; producers are the embedding service and MCP dispatch.
- **Not on master at all**: decision models / "jev" (zero matches), GAN upscalers
  (zero matches), any chat UI. These are design-time concerns in #1376, not migration
  concerns.

### .jipx and session storage

- `api/project/jipx/`: `.jipx` is a ZIP container (`JIPipeXProjectWriter`: ro-crate
  metadata, `jipx.json` manifest, README, then `project/<part>.json` in manifest
  order, then unknown/payload entries; atomic replace, self-verify). The manifest
  (`JIPipeXManifest`) preserves non-part entries verbatim in an `unknown` map with
  **reserved slots `data/`, `graphs/`, and `history/`**; workspace materialization
  (`JIPipeLocalProjectWorkspace`) keeps unknown/payload entries across round-trips.
- Two viable attachment points for a stored chat session (project-bound):
  1. a new manifest-listed part (extend `JIPipeProjectParts.PART_KEYS`, slice/assemble
     contract applies), or
  2. a reserved/payload slot like the already-reserved `history/` - no reader/writer
     schema change, entries survive round-trips verbatim.
  Option 2 matches "bring their chat" (opaque, agent-transcript-shaped data); option 1
  only if the session must appear in the project part model. `JIPipeProjectSaveTarget`
  already carries a forward-hook note about "the first jipx-exclusive feature".

### Where an AI panel docks

- Self-built dock: `utils/ui/JIPipeDesktopDockPanel` (`addDockPanel(id, title, icon,
  component, PanelLocation, ...)`, per-side toolbars, JSON state save/restore,
  shortcut scopes). Existing hosts: graph editors (pipeline flavor docks:
  add-nodes, node context help/parameters, slot/input manager, results; commons:
  log, history, errors, calculator, context, find, relations), compartments flavor,
  project overview, data viewer, plot editor, welcome panel.
- Alternative: full-area singleton document tabs (`JIPipeDesktopProjectWorkbench`
  TAB_* registry) or ordinary document tabs. Project-bound sessions fit the dock
  pattern; global sessions fit a standalone window (like the AI monitor window).

### Command bar / shortcuts

- No AI-related `JIPipeShortcutAction`s exist (`JIPipeShortcutActions` catalog has
  none; the AI manager's menu items are plain popup entries). Any AI commands
  (open AI panel, new ACP session, ...) must be registered as shortcut actions with
  stable string-literal ids to appear in the command bar.

### Plugin layout for the new AI/ACP plugin

- Two shapes exist: core-internal plugin packages in
  `jipipe-core/.../plugins/` (SciJava `@Plugin` discovery, `JIPipePrepackagedDefaultJavaPlugin`,
  no own maven module) - `ai`, `automation`, `embeddingserver`, ... - and separate
  maven modules `plugins/jipipe-plugin-*` (26 of them; root pom module entry +
  `dist/dist-info.json` "jipipe-modules" entry via `dist/generate-dist-scripts.py`).
- `AIPlugin` (`"org.hkijena.jipipe:ai"`) and `EmbeddingServerPlugin`
  (`"org.hkijena.jipipe:embedding-server"`) already exist as core-internal packages.
  The ACP work extends these or adds a sibling (per the work item: the in-house agent
  moves to a *new plugin*; a separate `jipipe-plugin-ai` module is the cleaner target,
  keeping the ACP client core reusable by both the desktop UI and the agent plugin).
- Cross-cutting: the launcher owns the MCP lifecycle (`GuiCommand`, `MCP_EXECUTOR`,
  `--mcp [port]`, `--mcp-permissions`) and gates every MCP start on `enable-ai`;
  the three pre-built extension points reserved for #1376 are the ask-presenter seam,
  `PendingAsk.origin`, and the merged-settings-page mechanism.

## The #1290 branch inventory (`origin/1290-ai-llm-integration`)

### Branch state

Tip `c200e2f105` "current state for later development" (2026-11-05). Merge base with
master is 2026-08-13; the branch carries 777 commits and master moved 1088 commits
independently since - the two sides diverged hard, not a fast-forward situation. Diff
vs merge base: 1275 files, +1,443,816 / -2,187 (994 added). All AI code lives inside
`jipipe-core` - there is no module boundary to extract along, so "move to a new
plugin" is a genuine re-packaging. Sizes: `api/ai/` 192 files / 29.4k lines,
`plugins/ai/` 156 files / 30.2k lines, `desktop/app/ai/` + `desktop/app/chat/` 87
files / 10.2k lines of Swing UI, `servers/llmproxy/` 22 files / 3.5k lines. ~109
AI test classes, mostly pure logic (they travel with an extraction). No mention of
ACP anywhere on the branch. A stray 9,106-line `session-ses_04cf.md` agent transcript
sits at the repo root (do not ship). Useful branch design docs:
`AI-SERVICE-ARCHITECTURE.md`, `docs/agentic-loop-architecture.md`,
`KNOWLEDGE_DATABASE_DESIGN.md`, `JIPPIPE_UI_ARCHITECTURE.md`,
`docs/ai-model-calibration-design.md`, `docs/plan-external-server-processes-final.md`.

### The AI panel UI (branch-only packages)

- `desktop/app/ai/assistant/JIPipeDesktopAIAssistantPanel` (205 lines): workbench
  panel with CardLayout over three cards (setup-required / session list / chat view);
  shows setup until embedding model and LLM are configured and ready.
- `ChatViewPanel` (1,035 lines): toolbar (back, session title, context usage,
  "compact context"), `JIPipeDesktopChatUI`, plan panel, streaming deltas, tool-call
  and sub-agent entries, stage approval (StageReviewProvider/StageWindow), subscribes
  to ~15 `ProjectAIChatState` events.
- Supporting: `SessionListPanel` (284, JSON import/export of sessions),
  `AIAssistantLandingPanel` (getting started + sessions + shared chat input + mode
  picker + permissions), `PermissionPanel`, `PlanPanel`, `stages/` (10 classes),
  `prompts/actions/` (12 premade prompt actions behind `AIChatPromptAction`).
- Chat rendering: `desktop/app/chat/ui/JIPipeDesktopChatUI` + log + content
  renderers; Markdown via `MarkdownText.getRenderedHTML()` into a JTextPane; an
  `Interactive` content type with renderer registry (`ChoiceInputRenderer`,
  `NodePickInputRenderer`, `PathPickInputRenderer`) for agent-to-user questions;
  entry taxonomy `JIPipeDesktopChatLogEntryType` (User, ToolCall, Response, Reasoning,
  SubAgent*, Stage, Iteration, Condensation, Status, Error, Complete, ...).
- **Docking**: registered as dock panel id `AI_ASSISTANT`, `TopRight`, gated on
  `enable-ai`, in three hosts (pipeline graph editor, compartments graph editor,
  project overview) - all three host files exist on master. Session routing in
  `JIPipeDesktopProjectWorkbench.startAIChatSession(...)`/`sendToAI(...)` (branch
  additions): picks the editor tab, activates the dock, sends the message.
- This is the UI the work item means by "extracted and adapted to ACP": its
  event-driven surface (stream deltas, tool calls, plans, permissions, stages, usage)
  maps almost 1:1 onto ACP `session/update` variants; what changes underneath is the
  engine (AgentSession over an OpenAI-compatible proxy) replaced by ACP agent sessions.

### llama.cpp shared server infrastructure (branch-only, feeds the plugin extraction)

- `servers/llmproxy/`: standalone HTTP proxy in its own JVM
  (`LLMProxyProcess`, a `SharedServerProcess`; discovery file + PID liveness -
  the same pattern as the MCP broker), endpoints `/v1/chat/completions`,
  `/v1/models`, `/control/*`, `/health`. `LlamaServerManager` builds/spawns/warms
  `llama-server`; `ensureLlamaServerRunning` merges all registered instances'
  criteria/budgets, ranks candidates via `SpeedTestDatabase`, budgets RAM/VRAM
  (`ResourceMonitor`), and forwards to remote backends when present
  (`RemoteForwarder`, `BackendRouter`, `FallbackHandler`).
- Client side: `LLMProxyClient` (spawn-on-demand + `POST /control/register` with
  llama.cpp dir, model package, `ModelSelectionCriteria`, `LLMResourceBudget`,
  remote backends), acquired through the generic lease system
  (`JIPipeServerInstanceManager`). The generic shared-process framework in
  `api/servers/` is already reconciled between master and branch
  (`SharedServerProcess` is byte-identical).
- Models: `models.json` (`ModelsFile`, `CompiledModelEntry` with capabilities,
  sampling matrix, llama-server args, calibration results) loaded by
  `CompiledModelRegistry.selectModel` merging local + remote backends.
- Embedding (not llama.cpp): in-process ONNX Runtime + DJL tokenizer
  (`JIPipeOnnxEmbeddingAIModelRunner`) or OpenAI-compatible HTTP
  (`JIPipeAPIEmbeddingAIModelRunner`); optional separate-JVM embedding server
  (`servers/embedding/`, deliberately free of JIPipe runtime).

### In-house agent abstractions (branch-only)

- `api/ai/agent/AgentSession` (3,765 lines): the engine. Function table, attachments,
  stages + `StageApprovalGate` (CompletableFuture-based), history, ledger, skills,
  state tracker, ~15 event emitters (stream delta, tool call, LLM response, sub-agent
  start/finish, context usage, user-input request, plan updated, ...). Explicitly
  framework-free (Javadoc) - good extraction material.
- `AgentSessionConfig` (model id, system prompt, max iterations, sampling,
  approval granularity, context window 32000, condensation threshold 0.9, ...).
- `ChatSession` (230): Jackson-serializable session POJO (id, title, mode,
  `PermissionSet`, message history, stage summaries).
- `ProjectAIChatState` (1,174): per-project singleton, sessions + active id,
  runs the agent loop on a daemon thread, **in-memory only** (persistence is manual
  JSON export/import; ACP session persistence would be new work - relevant to the
  "store session in JIPX" investigation).
- Modes: `AgentMode` interface (id/name/template `modes/<id>.jinja`/function
  providers/permissions/sampling) with 8 modes (Orchestrator, PipelineBuilder,
  ImageAnalyst, Debugger, Documenter, ExpressionWriter, CodeWriter, Qa); prompts via
  Jinjava (`PromptTemplateEngine`); system prompt assembled by
  `JIPipeAIAgentServiceComponent.buildSystemPrompt`.
- Tools: annotation-driven `@LLMFunction`/`@LLMFunctionParam` with schema generator
  and invoker; providers in `plugins/ai/agent/*Functions.java` (knowledge, api, plan,
  pipeline map, run, user input with `ask_user_choice`/`ask_user_pick_nodes`/
  `ask_user_pick_path`, expression writer, parameter writing, ...).
- Protocol engine: `api/ai/agent/protocol/` (state machine, input classifier,
  completion judge, verifiers, todo tracker, context builder) +
  `api/ai/agent/protocols/` SPI with KDB-guided TODO solutions
  (`KdbSolutionRegistry`, hybrid keyword + embedding scoring).

### AI setup UI on the branch (what gets retired/redesigned)

- `desktop/app/ai/setup/JIPipeDesktopAISetupDialog` (330): modal dialog with terms
  checkbox and three configuration components (embedding model, llama.cpp, LLM model
  package) writing override environments into the branch's `AIApplicationSettings`;
  downloads missing models as artifacts (defaults in `AIPlugin`:
  `cn.ac.baai.bge_small_en:*`, `ai.ggml.llamacpp:*`, `org.hkijena.jipipe-llm-models:*`),
  optional `SpeedCalibrationRun`; statics `checkFirstTimeSetup(workbench)` and
  `disableAI(parent)`.
- Around it: `AIModelCalibrationWindow` (795 lines, model-package editing +
  calibration), `RemoteBackendsPanel`/`AddRemoteBackendDialog` (external OpenAI-style
  backends), and a branch `JIPipeDesktopAIMonitorWindow` with different pages than
  master's (Components/Embedding tester/LLM tester/External models/Performance/Log).

### Extraction frictions (what will fight an ACP abstraction)

Recorded for completeness: per D1/D2, the agent stack and most of these frictions are
not carried into #1376 - only the chat/assistant UI packages are taken and adapted to
ACP. Items that survive: the chat UI's coupling points (4) and the package
relocation requirement (5).

1. **No module boundary**: UI, agent core, proxy, settings all in jipipe-core; the
   new plugin is a re-packaging, not a move.
2. **Dead control layer**: the branch exposes AI operations via the old
   `api/instrumentation` package (`AIOperations`: `ai_create_session`,
   `ai_send_message`, ...), which master removed in favor of `api/automation` + MCP.
   Extraction must re-base these onto the automation operation registry.
3. **Transport seam is good but leaks**: `ModularLLMClient`/`OpenAICompatibleLLMClient`
   is a plausible ACP insertion point, but proxy/model lifecycle leaks upward -
   `ProjectAIChatState.runAgentLoop` polls `ProxyStatus` strings ("Starting"/
   "Swapping"/"Ready") every 500 ms and `AgentSession`/`JIPipeAIServiceComponent`
   know about model swaps and calibration-derived sampling params.
4. **UI-agent coupling mostly event-based (good), with exceptions**: `ChatViewPanel`
   imports `AgentState` enums and `StageReviewProvider`;
   `JIPipeDesktopProjectWorkbench.startAIChatSession` calls the static
   `SpeedTestUtils.isAllModelsFailedForCurrentSettings()`; `isAIConfigured()`
   hard-requires both embedding and LLM. Permission/stage/user-input flows
   (`PermissionSet`, `StageApprovalGate`, `UserInputRequest`) map naturally onto ACP
   session-request semantics and are the cleanest parts to keep.
5. **Simple-name collisions with master's newer AI UI**: `JIPipeDesktopAISetupDialog`,
   `JIPipeDesktopAIMonitorWindow`, `JIPipeDesktopAIMonitorPage`,
   `JIPipeDesktopAIMonitorOverviewPage`, `JIPipeDesktopAIMonitorLogPage`,
   `JIPipeDesktopAIManager`, `ModelConfigurationComponent` exist on both sides with
   different implementations. Package relocation is mandatory; master's versions win
   for monitor/statusbar (they carry the MCP integration), the branch's chat/assistant
   packages are the ones worth extracting.
6. **`org.hkijena.jipipe.api.knowledge` is double-occupied**: master has a newer
   `Kdb*`/`KnowledgeService` implementation (19 classes, embedding lanes); the branch
   has its own `KnowledgeDatabase`/`KnowledgeSearchService`/API-reference stack with
   bundled embedding DBs in the launcher (built by `BuildEmbeddingsMain`). The
   extraction cannot copy `api/knowledge` over; it must merge onto master's stack.
7. **Embedding runner drift**: both sides have embedding runners with ~90 lines of
   real drift (branch added retry logic); reconcile deliberately.
8. **Sessions are not persisted** on the branch (in-memory + manual JSON); the ACP
   design owns persistence (and possibly JIPX storage, see above).
9. **Branch registration APIs** (`registerAgentMode`, `registerAgentProtocol`,
   `registerTodoHandler`, `registerAIChatPromptAction`,
   `registerAgentSkillsFromResources`, `registerInstrumentationOperation`) do not
   exist on master - the plugin re-packaging must re-create them on the new plugin
   base (with `registerInstrumentationOperation` mapped to the automation registry).
10. **Hard-coded assumptions**: artifact query defaults, the llama.cpp directory
    layout heuristic (`resolveLlamaCppDirectory`: cpu|vulkan|cudaNNN),
    `EmbeddingModelType.LocalOnnx` (ONNX + tokenizer pair), calibration-driven model
    selection. Fine inside the agent plugin; must not leak into the ACP core.


## Decisions (2026-11-21, owner)

- **D1 - In-house agent is out of scope here.** It moves to another team member who
  will start from scratch given recent developments (months off). No functionality
  from the #1290 branch is transferred: #1376 delivers the ACP client layer, UI,
  settings, capability packages, and prompts; the future agent plugs into the same
  ACP interfaces. The branch's agent stack below stays documented for that future
  work, not for extraction.
- **D2 - Master wins.** The branch is a source of parts only (primarily the
  chat/assistant UI packages), never merged. Name and package collisions resolve in
  favor of master.
- **D3 - Permissions are bridged to JIPipe's system.** ACP permission requests map
  onto the existing Ask / allow-always / deny pipeline
  (`AutomationPermissionService` + the `MCPAskPresenter` seam, origin "acp");
  additional JIPipe permissions may be added as needed.
- **D4 - Settings organization.** If the unified "store" page gets too crowded, AI
  settings move into their own settings category "AI" with pages "Setup", "MCP",
  "Agents" (or similar organization).
- **D5 - JIPX session storage is a spec feature.** Chat sessions are added as proper
  jipx spec entries (not an opaque reserved slot); the format version is bumped
  accordingly and jipipe-cms-website is updated.
- **D6 - MCP transport: prefer HTTP, fall back to stdio.** Ready-to-use prompts
  carry the HTTP endpoint + token by default; a stdio fallback (the proxy pattern)
  covers agents without `mcpCapabilities.http`.
- **D7 - #1150 is finished** (left open accidentally); the settings system is
  complete and is the foundation for the AI settings work.

## Synthesis: how the work item maps onto what exists

| #1376 requirement | Existing material | Gap |
|---|---|---|
| ACP client + UI | Official Java SDK (`acp-core`, stdio transport, typed handlers, in-memory tests); branch chat UI with an event-driven surface that mirrors ACP updates; dock panel + workbench routing pattern | ACP connection layer (subprocess mgmt, capability negotiation, transports), mapping `ProjectAIChatState`/`ChatViewPanel` events onto `session/update`, permission options UI |
| Unified AI settings page | Settings system on master, finished (#1150 closed as done per D7): sheets, scopes, JSON documents, merged custom pages, deep links, `enable-ai` toggle | Custom page(s) via the keyboard-section pattern; if crowded, own category "AI" with pages "Setup", "MCP", "Agents" (D4); agent store; the branch setup dialog's artifact download flows are not transferred (D1) |
| AI capability packages | `AIPlugin`/`EmbeddingServerPlugin` core-internal plugins; `registerEnvironment` registry; embedding service microservice; MCP layer | The abstraction itself (registry of capability packages, dependency declarations like "MCP depends on embedding", enable/install lifecycle). Design work; no legacy migration burden (decision models / GAN upscalers have zero code yet) |
| Available agents | ACP Registry (stabilized, machine-readable JSON with install metadata); Environments as the config base for custom agents | Registry fetch/cache UI, agent detection, per-agent launch configs (Environments), validation of custom agent entries |
| Ready-to-use prompts | MCP endpoint/broker/token settings on master | Prompt generator, HTTP-first (endpoint + token, D6); stdio fallback via the proxy pattern for agents without `mcpCapabilities.http`; the JSON-emission flow for unsupported agents with dialog-side validation |
| Existing settings transfer | `AIApplicationSettings` (branch overrides), `MCPApplicationSettings`, permission map, embedding model override | Carry-over into the new page's model; keep stable sheet ids or migrate deliberately |
| AI panel (project-bound vs global, JIPX) | Dock system (3 hosts already register the branch panel), document tabs, standalone windows; .jipx manifest reserved/payload slots (`history/` pattern) | Session lifecycle design (ACP session/load vs local persistence); chat sessions become proper jipx spec entries with a format-version bump and a jipipe-cms-website update (D5); design the entry schema (what is stored, replay interaction) |
| Permission redirect (#1379) | `MCPAskPresenter` seam + `PendingAsk.origin` (reserved for exactly this) | Presenter implementation rendering ACP permission options in the chat; bridge decisions into JIPipe's permission system (D3), adding JIPipe permissions as needed |
| In-house agent | Moves to another team member who starts from scratch (D1); the branch stack (`AgentSession`, modes, tools, protocol engine, llmproxy, calibration, models.json) is not transferred and stays documented above for that future work | The ACP interfaces it will plug into, delivered by #1376 |

## Risks and open questions (after the 2026-11-21 decisions)

1. **v1 vs v2 protocol timing**: v2 is in preview with breaking changes
   (prompt lifecycle, permission requests). Build on the stable v1 baseline +
   stabilized RFD extensions, keep the SDK version pinned, and design the client
   layer so v2 migration touches the transport/spec layer only.
2. **JIPX spec bump ripple (D5)**: adding chat sessions as proper spec entries
   touches `JIPipeXManifest`/`JIPipeProjectParts`/reader and writer, the round-trip
   tests, the forward-hook note in `JIPipeProjectSaveTarget` ("the first
   jipx-exclusive feature" - this may be it), and the public docs on
   jipipe-cms-website. Design phase must define the entry schema (raw transcript
   vs replayable session log) and how it interacts with ACP `session/load` replay.
3. **Chat UI adaptation risk**: `ChatViewPanel`'s ~15 `ProjectAIChatState` event
   subscriptions become the adaptation contract; the coupling points to remove or
   re-point are the `AgentState` enum imports, `StageReviewProvider`, the
   `SpeedTestUtils.isAllModelsFailedForCurrentSettings()` static call, the
   `ProxyStatus` polling, and the `isAIConfigured()` embedding+LLM hard requirement
   (capabilities decide the setup card, not model presence).
4. **Secrets handling**: MCP bearer token and any agent credentials must go into OS
   secret storage per ACP security guidance; SDK logs agent stderr at INFO -
   silence/route it in JIPipe's logging setup.
5. **Small stuff**: the stray `session-ses_04cf.md` at the branch root should not be
   carried over; dist-info.json must gain the new dependency (acp-core + JSON module)
   and the module id if a separate maven module is chosen.

## Sources

- Work items: #1376, #1290, #1369, #1379, #1150, #1286, #1294, #753 (GitLab).
- ACP docs: get-started (introduction, architecture, agents, registry, related
  protocols), protocol v1 (overview, initialization, authentication, session setup,
  session list, prompt turn, content, tool calls, agent plan, elicitation, session
  modes, file system, terminals, transports, errors, extensibility, security), RFD
  index, announcements (v2 preview, registry stabilized, elicitation stabilized, ...).
- ACP Java SDK README (github.com/agentclientprotocol/java-sdk, 0.81.0).
- JetBrains blog: "Getting Started with Air in JetBrains IDEs - Part 1" (Oct 2026).
- deja-vu session ses_f493ee54cff (2026-09-19): embedding server shared-instance
  infrastructure notes (api/servers PortManager/ProcessSupervisor/leases,
  EmbeddingServerPlugin, OpenAI-compatible /v1/embeddings endpoint).
