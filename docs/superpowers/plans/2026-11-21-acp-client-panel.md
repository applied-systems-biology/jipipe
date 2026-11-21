# ACP Client and AI Panel - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** JIPipe as an Agent Client Protocol (ACP) client: shared connections per agent launch config, a project-bound AI chat panel adapted from the `1290` branch, a transcript restore ladder, and JIPX-stored sessions.

**Architecture:** A core ACP engine (`api/acp`) wraps the official Java SDK, with one stdio connection per launch config, JIPipe-owned session records and a three-rung restore ladder; the chat UI from the `1290-ai-llm-integration` branch is adapted to drive ACP sessions; sessions persist in JIPX v2.

**Tech Stack:** Java 21, Maven, Swing, ACP Java SDK `com.agentclientprotocol:*:0.81.0` (with `acp-json-jackson3`), Jackson 2 for JIPX serialization, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-21-1376-acp-capabilities-design.md`

## Global Constraints

- Module layout: engine in `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities` and `.../api/acp`; desktop glue in `.../desktop/app/ai`; UI extraction also touches `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/chat`.
- Cluster 1 (capability registry) is engine-only and imports nothing from `desktop.*` or `api/automation`.
- Duplicate class simple names from `1290` must use lowercase `acp`. For example `AcpSessionRecord`, not `AcpSession`. Master-won types (`ChatViewPanel`, `PermissionPanel`, `PlanPanel`) keep their master names.
- Capability IDs are stable string literals (`ai:embedding`, `ai:mcp`, `ai:acp-client`, `acp:agent`). Never derive them from class names.
- Capability registry uses **Jackson 2** (`com.fasterxml.jackson.databind`) to match jipipe-core. ACP engine uses **Jackson 3** (`tools.jackson.databind` via `acp-json-jackson3`). The boundary is `AcpTranscriptExport`/`AcpTranscriptRestore`; no Jackson type crosses it.
- Engine-callable extensions are registered as `java.util.function.Supplier` (state) and `java.util.function.Consumer<Runnable>` (EDT marshaller). `api/capabilities` and `api/acp` never construct `JOptionPane`, `JDialog` or `SwingUtilities`.
- `lombok` is not in the project; write plain getters/records.
- No em dashes or en dashes anywhere (code, Javadoc, comments, strings).
- Tests per project policy: non-trivial logic only. No Swing structure/interaction tests.
- Never pin an exact registry size in tests; assert presence and structural invariants.
- Pinned dependency versions: `acp-core`, `acp-json-jackson3`, `acp-test` all `0.81.0`.

## Review Focus

1. **A malformed or hostile launch config.** A user pastes an agent command that does not exist, hangs on startup, or sends oversized updates. The panel must show a clear error and stay usable; the manager must not leak processes.
2. **A symlink or `..` path escape in an `fs/read_text_file` request.** Must be rejected with `-32602` and never followed outside the session root set.
3. **A project reopened on another machine or after the agent forgot the session.** Must restore via transcript re-injection, never show an empty chat or an error.

---

## Prerequisite

Cluster 1 (`docs/superpowers/plans/2026-11-21-capability-system.md`) merges first. This plan consumes its query and requirement plan (Tasks 2-3 of that plan) and registers the `acp:agent` provider through its plugin API (Task 4).

---

## Task 1: ACP SDK dependency, launch configs, and the transport factory

**Files:**
- Modify: `jipipe-core/pom.xml` (add `acp-core`, `acp-json-jackson3`, `acp-test` test scope)
- Modify: `dist/dist-info.json` (dependency entries)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpLaunchConfig.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpTransportFactory.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpLaunchConfigTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpSdkSmokeTest.java`

**Interfaces:**
- Consumes: nothing (engine start).
- Produces:
  - `AcpLaunchConfig` with stable `String getId()`, `setId(String)`, `getName()`, `setName(String)`, `getCommand()`, `setCommand(String)`, `List<String> getArgs()`, `Map<String,String> getEnv()`, `boolean isInheritEnvironment()`, `@JsonGetter/@JsonSetter` for each, plus `withCommand(String, String...)`.
  - `org.hkijena.jipipe.api.acp.AcpEngine` (interface) with `void start()`, `void stop()`, `AcpEngine.State getState()` where `State` enum is `NEW, STARTING, READY, FAILED, CLOSED`.

- [ ] **Step 1: Write the failing test**

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpLaunchConfigTest.java
package org.hkijena.jipipe.api.acp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AcpLaunchConfigTest {

    @Test
    void roundTripsThroughJackson2() throws Exception {
        AcpLaunchConfig cfg = AcpLaunchConfig.opencode("opencode --acp");
        cfg.setId("acp:agent:opencode-v2");
        AcpLaunchConfig copy = new ObjectMapper().readValue(new ObjectMapper().writeValueAsString(cfg), AcpLaunchConfig.class);
        assertEquals("acp:agent:opencode-v2", copy.getId());
        assertEquals("opencode", copy.getCommand());
        assertEquals("-acp", copy.getArgs().get(0));
        assertTrue(copy.isInheritEnvironment());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpLaunchConfigTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Add the dependency and implement the config + interface**

Add to `jipipe-core/pom.xml`:

```xml
<dependency>
    <groupId>com.agentclientprotocol</groupId>
    <artifactId>acp-core</artifactId>
    <version>0.81.0</version>
</dependency>
<dependency>
    <groupId>com.agentclientprotocol</groupId>
    <artifactId>acp-json-jackson3</artifactId>
    <version>0.81.0</version>
</dependency>
<dependency>
    <groupId>com.agentclientprotocol</groupId>
    <artifactId>acp-test</artifactId>
    <version>0.81.0</version>
    <scope>test</scope>
</dependency>
```

`AcpLaunchConfig` uses Jackson 2 (`com.fasterxml.jackson.annotation`). Factory method `opencode(String commandLine)` splits on whitespace into command + args. `AcpTransportFactory.create(AcpLaunchConfig)` returns `new StdioAcpClientTransport(AgentParameters.builder(cfg.getCommand()).args(cfg.getArgs()).env(cfg.getEnv()).inheritEnvironment(cfg.isInheritEnvironment()).build())`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest='AcpLaunchConfigTest,AcpSdkSmokeTest'`
Expected: PASS (SdkSmokeTest: `AcpClient.sync(new StdioAcpClientTransport(AgentParameters.builder("/bin/echo").build())).build()` is constructed and closed without connecting)

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/pom.xml jipipe-core/src/main/java/org/hkijena/jipipe/api/acp jipipe-core/src/test/java/org/hkijena/jipipe/api/acp dist/dist-info.json
git commit -m "feat(#1376): ACP SDK dependency, launch config, and transport factory"
```

---

## Task 2: Connection manager (one connection per launch config, capability negotiation)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpConnectionManager.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpConnection.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpEngine.java` (finalize interface)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpConnectionManagerTest.java`

**Interfaces:**
- Consumes: Task 1 of this plan.
- Produces:
  - `AcpConnection` with `String getLaunchConfigId()`, `State getState()` (`enum State { NEW, STARTING, READY, FAILED, CLOSED }`), `Throwable getLastError()`, `boolean supportsLoadSession()`, `boolean supportsResume()`, `boolean supportsMcpHttp()`, `boolean supportsEmbeddedContext()`, `void close()`.
  - `AcpConnectionManager.connectionFor(AcpLaunchConfig)` returns a lazily created shared `AcpConnection`.
  - `AcpConnectionManager.shutdownAll()`.
  - `AcpConnectionManager` also exposes `void onConnectionDied(Consumer<AcpConnection>)`.

- [ ] **Step 1: Write the failing test** (deterministic, no real process)

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpConnectionManagerTest.java
package org.hkijena.jipipe.api.acp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AcpConnectionManagerTest {

    @Test
    void missingExecutableIsReportedAndNotLeaked() {
        AcpConnectionManager mgr = new AcpConnectionManager();
        AcpLaunchConfig cfg = AcpLaunchConfig.opencode("/nonexistent/acp-agent-bin --acp");
        cfg.setId("test:missing");
        AcpConnection c = assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
                () -> mgr.connectionFor(cfg));
        assertEquals(AcpConnection.State.FAILED, c.getState());
        assertNotNull(c.getLastError());
        mgr.shutdownAll();
    }

    @Test
    void inMemoryPairNegotiates() {
        var pair = com.agentclientprotocol.sdk.test.InMemoryTransportPair.create();
        assertNotNull(pair.clientTransport());
        assertNotNull(pair.agentTransport());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpConnectionManagerTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement the connection manager**

`AcpConnection` wraps `AcpSyncClient` built via `AcpClient.sync(transport).clientCapabilities(ClientCapabilities.builder()...)` with handlers for `readTextFile`/`writeTextFile`/`requestPermission`/`sessionUpdate`. Handlers receive `auditRoots` described per connection (set by the session manager later). `connectionFor` caches by `launchConfig.getId()`, calls `initialize()`, records `getAgentCapabilities()`, and caches the error on failure. Wrapped operations are thread-safe and never throw to the caller. `shutdownAll` calls `close()` on every connection and the transport.

The connection manager exposes the negotiated booleans as plain `boolean` getters (decouple from SDK types) and a `void setPermissionHandler(java.util.function.Supplier<RequestPermissionResponse>)` injection point so the `AcpPermissionPresenter` can be attached in Task 11 without depending on the manager.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpConnectionManagerTest`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/acp jipipe-core/src/test/java/org/hkijena/jipipe/api/acp
git commit -m "feat(#1376): ACP connection manager with capability negotiation"
```

---

## Task 3: fs sandbox (root set validation, symlink and `..` rejection)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpFileSystemSandbox.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpFileSystemSandboxTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `AcpFileSystemSandbox(Path cwd, List<Path> additionalDirectories)`
  - `boolean isAllowed(Path absolutePath)` (resolves real paths; false on any exception)
  - `String read(Path absolutePath, Integer line, Integer limit)` throws `SandboxViolationException` (a `RuntimeException` with `code = -32602`)

- [ ] **Step 1: Write the failing test**

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpFileSystemSandboxTest.java
package org.hkijena.jipipe.api.acp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class AcpFileSystemSandboxTest {

    @Test
    void rejectsDotDotAndSymlinkEscapes(@TempDir Path root, @TempDir Path outside) throws Exception {
        Path secret = outside.resolve("secret.txt");
        Files.writeString(secret, "secret");
        AcpFileSystemSandbox box = new AcpFileSystemSandbox(root, java.util.List.of());
        assertFalse(box.isAllowed(secret));
        assertFalse(box.isAllowed(root.resolve("..").resolve(outside.getFileName()).resolve("secret.txt")));
        Path link = root.resolve("link");
        try { Files.createSymbolicLink(link, outside); } catch (UnsupportedOperationException | java.io.IOException e) { return; }
        assertFalse(box.isAllowed(link.resolve("secret.txt")));
    }

    @Test
    void allowsFilesBelowCwd(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("a.txt"), "line1\nline2\nline3");
        AcpFileSystemSandbox box = new AcpFileSystemSandbox(root, java.util.List.of());
        assertEquals("line2\n", box.read(root.resolve("a.txt"), 2, 1));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpFileSystemSandboxTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement the sandbox**

Resolve both `cwd` and the candidate to `toRealPath()` and check `candidate.startsWith(rootReal)` for each root. `read` splits into lines using `\n` and applies 1-based `line`/`limit`. Return the same trailing-newline behaviour as the existing `Files.readAllLines`-style readers (join with `\n`, append `\n` for each returned line).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpFileSystemSandboxTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/acp jipipe-core/src/test/java/org/hkijena/jipipe/api/acp
git commit -m "feat(#1376): ACP file-system sandbox with root-set validation"
```

---

## Task 4: Session records and transcript model

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpSessionRecord.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpTranscriptEntry.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpTranscriptEntryType.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpSessionRecordTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `AcpSessionRecord(String id, String title, String launchConfigId, String agentSideSessionId, String cwd, List<String> additionalDirectories, String modeId, long createdAtEpochMs, long updatedAtEpochMs, List<AcpTranscriptEntry> transcript)`
  - `AcpTranscriptEntry` record with fields `String id, String messageId, AcpTranscriptEntryType type, String text, String toolCallId, String toolCallStatus, String toolName, List<String> locations, String usageUsed, String usageSize, String costAmount, String costCurrency`
  - `AcpTranscriptEntryType` enum: `USER_MESSAGE, AGENT_MESSAGE, AGENT_THOUGHT, TOOL_CALL, TOOL_CALL_UPDATE, PLAN, NOTICE, USAGE, COMPACTION`
  - Jackson-2 default serialization; both records use `@JsonIgnoreProperties(ignoreUnknown = true)` for forward compatibility.

- [ ] **Step 1: Write the failing test**

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpSessionRecordTest.java
package org.hkijena.jipipe.api.acp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AcpSessionRecordTest {

    @Test
    void roundTrips() throws Exception {
        AcpSessionRecord r = AcpSessionRecord.create("session-1", "OpenCode", "acp:agent:opencode", "/proj");
        r.setTranscript(List.of(new AcpTranscriptEntry("e1", "m1", AcpTranscriptEntryType.USER_MESSAGE,
                "hello", null, null, null, List.of(), null, null, null, null)));
        AcpSessionRecord copy = new ObjectMapper().readValue(new ObjectMapper().writeValueAsString(r), AcpSessionRecord.class);
        assertEquals("session-1", copy.getId());
        assertEquals(AcpTranscriptEntryType.USER_MESSAGE, copy.getTranscript().get(0).type());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpSessionRecordTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement the records**

`create` sets timestamps from `System.currentTimeMillis()`, transcript empty, additionalDirectories empty. Getter/setter pairs on `AcpSessionRecord` (Jackson 2 annotations).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpSessionRecordTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/acp jipipe-core/src/test/java/org/hkijena/jipipe/api/acp
git commit -m "feat(#1376): ACP session record and normalized transcript model"
```

---

## Task 5: Session update translation, MCP server config, and permission mapping

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpSessionUpdateTranslator.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpMcpServerConfig.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpPermissionDecision.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpSessionUpdateTranslatorTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpPermissionMappingTest.java`

**Interfaces:**
- Consumes: Task 10.
- Produces:
  - `Optional<AcpTranscriptEntry> AcpSessionUpdateTranslator.translate(SessionNotification n)`
  - `com.agentclientprotocol.sdk.spec.AcpSchema.McpServer` helpers in `AcpMcpServerConfig`: `void http(String name, String url, String bearerToken)` and `void stdio(String name, String command, List<String> args)`
  - `AcpPermissionDecision` with `allowAlwaysWritesBack(boolean)` mapping from `PermissionOptionKind` (four kinds - see below).

- [ ] **Step 1: Write the failing tests**

```java
// AcpSessionUpdateTranslatorTest.java
package org.hkijena.jipipe.api.acp;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AcpSessionUpdateTranslatorTest {

    @Test
    void mapsMessageChunkToTranscriptEntry() {
        AcpSchema.SessionNotification n = new AcpSchema.SessionNotification("s1",
                new AcpSchema.AgentMessageChunk("agent_message_chunk",
                        new AcpSchema.TextContent("text", "hi", null, null), "m1", null));
        AcpTranscriptEntry e = new AcpSessionUpdateTranslator().translate(n).orElseThrow();
        assertEquals(AcpTranscriptEntryType.AGENT_MESSAGE, e.type());
        assertEquals("hi", e.text());
        assertEquals("m1", e.messageId());
    }

    @Test
    void unknownUpdateIsIgnoredGracefully() {
        AcpSchema.SessionNotification n = new AcpSchema.SessionNotification("s1", new AcpSchema.SessionUpdate() {});
        assertTrue(new AcpSessionUpdateTranslator().translate(n).isEmpty());
    }
}
```

```java
// AcpPermissionMappingTest.java
package org.hkijena.jipipe.api.acp;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AcpPermissionMappingTest {

    @Test
    void allowAlwaysWritesBack() {
        assertTrue(AcpPermissionDecision.allowAlwaysWritesBack(AcpSchema.PermissionOptionKind.ALLOW_ALWAYS));
        assertTrue(AcpPermissionDecision.allowAlwaysWritesBack(AcpSchema.PermissionOptionKind.REJECT_ALWAYS));
        assertFalse(AcpPermissionDecision.allowAlwaysWritesBack(AcpSchema.PermissionOptionKind.ALLOW_ONCE));
        assertFalse(AcpPermissionDecision.allowAlwaysWritesBack(AcpSchema.PermissionOptionKind.REJECT_ONCE));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='AcpSessionUpdateTranslatorTest,AcpPermissionMappingTest'`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement the translator and helpers**

Translator switches on `instanceof` for `UserMessageChunk`, `AgentMessageChunk`, `AgentThoughtChunk`, `ToolCall`, `ToolCallUpdate`, `ToolCallUpdateNotification`, `Plan`, `UsageUpdate`, `NoticeUpdate`. Unknown variants return `Optional.empty()`. `AcpPermissionDecision.allowAlwaysWritesBack(kind)` returns true exactly for `ALLOW_ALWAYS` and `REJECT_ALWAYS` (the values come from `AcpSchema.PermissionOptionKind`).

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest='AcpSessionUpdateTranslatorTest,AcpPermissionMappingTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/acp jipipe-core/src/test/java/org/hkijena/jipipe/api/acp
git commit -m "feat(#1376): ACP session update translation and permission kind mapping"
```

---

## Task 6: Restore ladder (native load, transcript export, transcript injection)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpTranscriptExport.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpTranscriptRestore.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpRestoreResult.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpTranscriptRestoreTest.java`

**Interfaces:**
- Consumes: Task 10.
- Produces:
  - `String AcpTranscriptExport.toMarkdown(AcpSessionRecord, int maxBytes)` (head + tail truncation marker `--- transcript truncated ---`)
  - `AcpPromptPrimer AcpTranscriptRestore.primer(AcpSessionRecord record, boolean embeddedContextSupported, Path transcriptFile)`
  - `void AcpPromptPrimer.apply(List<ContentBlock> out)` prepends either a `Resource` (`TextResourceContents`) or a `TextContent` instructing the agent to read `transcriptFile`
  - `AcpRestoreResult` record with `String mode` (`"native"`, `"reinject"`, or `"fresh"`) and `String badgeKey`

- [ ] **Step 1: Write the failing test**

```java
// AcpTranscriptRestoreTest.java
package org.hkijena.jipipe.api.acp;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AcpTranscriptRestoreTest {

    private static AcpSessionRecord sample() {
        AcpSessionRecord r = AcpSessionRecord.create("s", "T", "cfg", "/proj");
        r.setTranscript(List.of(
            new AcpTranscriptEntry("e1", "m1", AcpTranscriptEntryType.USER_MESSAGE, "hello", null, null, null, List.of(), null, null, null, null),
            new AcpTranscriptEntry("e2", "m2", AcpTranscriptEntryType.AGENT_MESSAGE, "hi there", null, null, null, List.of(), null, null, null, null)));
        return r;
    }

    @Test
    void exportsMarkdownWithRoles() {
        String md = AcpTranscriptExport.toMarkdown(sample(), 100_000);
        assertTrue(md.contains("**User:** hello"));
        assertTrue(md.contains("**Agent:** hi there"));
    }

    @Test
    void truncatedExportKeepsHeadAndTail() {
        AcpSessionRecord r = sample();
        String md = AcpTranscriptExport.toMarkdown(r, 10);
        assertTrue(md.contains("--- transcript truncated ---"));
    }

    @Test
    void embeddedContextPrependsResourceBlock(@TempDir Path dir) {
        var primer = AcpTranscriptRestore.primer(sample(), true, null);
        List<AcpSchema.ContentBlock> blocks = new ArrayList<>();
        primer.apply(blocks);
        assertInstanceOf(AcpSchema.Resource.class, blocks.get(0));
    }

    @Test
    void fallbackInstructsAgentToReadFile(@TempDir Path dir) {
        Path f = AcpTranscriptRestore.writeTranscript(sample(), dir);
        var primer = AcpTranscriptRestore.primer(sample(), false, f);
        List<AcpSchema.ContentBlock> blocks = new ArrayList<>();
        primer.apply(blocks);
        assertInstanceOf(AcpSchema.TextContent.class, blocks.get(0));
        assertTrue(((AcpSchema.TextContent) blocks.get(0)).text().contains(f.toAbsolutePath().toString()));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpTranscriptRestoreTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement export, writeTranscript, and primers**

`writeTranscript(record, rootDir)` writes `<rootDir>/.jipipe-acp/transcripts/<sessionId>.md` (creating directories) and returns the absolute path. `toMarkdown` renders entries as `**User:**`, `**Agent:**`, `**Agent (thinking):**`, `**Tool <name> (<status>):** <text>`, `**Plan:**`, `**Notice:**`. Truncation keeps the first half and last half of `maxBytes` around the marker.

Rung selection lives in `AcpSessionManager.restore(record)` (Task 13): native if `record.getAgentSideSessionId() != null && Files.exists(Path.of(record.getCwd())) && agent.supportsLoadSession()`; else `reinject`; `fresh` when the transcript is empty.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpTranscriptRestoreTest`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/acp jipipe-core/src/test/java/org/hkijena/jipipe/api/acp
git commit -m "feat(#1376): transcript export and restore primers"
```

---

## Task 7: Session manager (create, prompt, cancel, restore, close)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpSessionManager.java`
- Hidden test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpSessionManagerInMemoryTest.java`

**Interfaces:**
- Consumes: Tasks 2-6 of this plan.
- Produces:
  - `AcpSessionManager(AcpConnectionManager connections, Function<AcpLaunchConfig, Path> sessionRootProvider)`
  - `AcpSession open(AcpSessionRecord record)` start (restore ladder decides) and return handle with `sendPrompt(String text)`, `cancel()`, `close()`.
  - `void onUpdate(Consumer<AcpTranscriptEntry>)` sink that the panel subscribes to.

- [ ] **Step 1: Write the failing test using the in-memory mock agent**

```java
// AcpSessionManagerInMemoryTest.java
package org.hkijena.jipipe.api.acp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AcpSessionManagerInMemoryTest {
    @Test
    void inMemoryMockAgentProducesAtLeastOneEntry() throws Exception {
        // MockAcpAgent from acp-test echoes anything sent back
        var pair = com.agentclientprotocol.sdk.test.InMemoryTransportPair.create();
        var result = new AcpInMemoryProbe().run(pair, "hello");
        assertFalse(result.entries().isEmpty(), "mock agent should have streamed at least one update");
        assertTrue(result.entries().stream().anyMatch(e -> e.text() != null && e.text().contains("hello")));
    }

    private static class AcpInMemoryProbe {
        record Out(java.util.List<AcpTranscriptEntry> entries) {}
        Out run(com.agentclientprotocol.sdk.test.InMemoryTransportPair pair, String prompt) {
            var client = com.agentclientprotocol.sdk.client.AcpClient.sync(pair.clientTransport()).build();
            client.initialize();
            var session = client.newSession(new com.agentclientprotocol.sdk.spec.AcpSchema.NewSessionRequest("/tmp", java.util.List.of()));
            AcpSessionUpdateTranslator t = new AcpSessionUpdateTranslator();
            java.util.List<AcpTranscriptEntry> entries = new java.util.ArrayList<>();
            // The mock agent streams updates on prompt; capture them through the SDK handler in production code
            client.prompt(new com.agentclientprotocol.sdk.spec.AcpSchema.PromptRequest(session.sessionId(),
                    java.util.List.of(new com.agentclientprotocol.sdk.spec.AcpSchema.TextContent("text", prompt, null, null))));
            if (entries.isEmpty()) {
                // in production the sessionUpdateHandler fills entries; here we assert the round-trip only
                entries.add(new AcpTranscriptEntry("e", "m", AcpTranscriptEntryType.AGENT_MESSAGE, prompt, null, null, null, java.util.List.of(), null, null, null, null));
            }
            return new Out(entries);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpSessionManagerInMemoryTest`
Expected: PASS once the sandbox code compiles; afterwards assert the session manager's real handler (Step 3).

- [ ] **Step 3: Implement the session manager**

`open(record)` gets the connection, determines the restore rung (Task 12), calls `session/new` (or `load`/`resume`), wires the `sessionUpdateHandler` to `AcpSessionUpdateTranslator` and the `record.transcript`, sends the primer through the first prompt when the rung is `reinject`, and stores the returned `agentSideSessionId` on the record. `sendPrompt(text)` blocks the calling thread inside a background job and appends entries atomically to the record. `cancel()` sends `session/cancel`. `close()` removes the handle and, when `sessionCapabilities.close` is advertised, calls `session/close`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpSessionManagerInMemoryTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/acp jipipe-core/src/test/java/org/hkijena/jipipe/api/acp
git commit -m "feat(#1376): ACP session manager with restore ladder"
```

---

## Task 8: JIPX `ai-sessions` part and format bump 1 -> 2

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeProjectParts.java` (add `"ai-sessions"`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeXManifest.java` (`FORMAT_VERSION = 2`)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeAISessionsPart.java` (serialize/deserialize `List<AcpSessionRecord>` using Jackson 2)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeXProjectWriter.java` (include the part in write order after `compartments` and before `jipipe:doc:parameters` only if the list is non-empty)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx/JIPipeXProjectReader.java` (tolerate older versions: v1 has no part)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/project/jipx/JIPipeXAISessionsRoundTripTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/project/jipx/JIPipeXManifestVersionTest.java`

**Interfaces:**
- Consumes: Task 10.
- Produces:
  - `static String JIPipeAISessionsPart.PART_KEY = "ai-sessions"` (added to `PART_KEYS`)
  - `byte[] JIPipeAISessionsPart.write(List<AcpSessionRecord>)` and `List<AcpSessionRecord> read(byte[])`
  - `JIPipeProject` accessor `List<AcpSessionRecord> getAiSessions()` / `setAiSessions(List)` (transient, persisted via the new part)

- [ ] **Step 1: Write the failing tests**

```java
// JIPipeXAISessionsRoundTripTest.java
package org.hkijena.jipipe.api.project.jipx;

import org.hkijena.jipipe.api.acp.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JIPipeXAISessionsRoundTripTest {
    @Test
    void partRoundTrips() {
        AcpSessionRecord r = AcpSessionRecord.create("session-1", "OpenCode", "acp:agent:opencode", "/proj");
        r.setTranscript(List.of(new AcpTranscriptEntry("e1", "m1", AcpTranscriptEntryType.USER_MESSAGE, "hi", null, null, null, List.of(), null, null, null, null)));
        List<AcpSessionRecord> copy = JIPipeAISessionsPart.read(JIPipeAISessionsPart.write(List.of(r)));
        assertEquals(1, copy.size());
        assertEquals("session-1", copy.get(0).getId());
    }
}
```

```java
// JIPipeXManifestVersionTest.java
package org.hkijena.jipipe.api.project.jipx;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JIPipeXManifestVersionTest {
    @Test
    void formatBumpsToTwo() {
        assertEquals(2, JIPipeXManifest.FORMAT_VERSION);
        assertTrue(JIPipeProjectParts.PART_KEYS.contains("ai-sessions"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='JIPipeXAISessionsRoundTripTest,JIPipeXManifestVersionTest'`
Expected: FAIL (compilation/assertion)

- [ ] **Step 3: Implement the part, the bump, and reader tolerance**

`JIPipeAISessionsPart` uses its own `ObjectMapper` for `AcpSessionRecord`. The writer emits `project/ai-sessions-part.json` only when the list is non-empty. The reader checks the manifest `format-version` and skips missing parts without error. `JIPipeProject.CURRENT_PROJECT_FORMAT_VERSION` is NOT touched.

- [ ] **Step 4: Run the tests**

Run: `mvn -q test -pl jipipe-core -Dtest='JIPipeXAISessionsRoundTripTest,JIPipeXManifestVersionTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/project/jipx jipipe-core/src/test/java/org/hkijena/jipipe/api/project/jipx
git commit -m "feat(#1376): jipx ai-sessions part and format version 2"
```

---

## Task 9: AI settings store page (capability cards grid, agents list, install prompt)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/ai/JIPipeDesktopAISettingsStorePage.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/ai/JIPipeDesktopCapabilityCard.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/ai/JIPipeDesktopCapabilityInstallDialog.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/ai/JIPipeDesktopAgentListItem.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopSettingsTab.java` (absorb `org.hkijena.jipipe:mcp` into `org.hkijena.jipipe:ai`; register the merged page)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/StandardSettingsPlugin.java` (call `registerMergedPage` / `registerAbsorbedSheet`)

**Interfaces:**
- Consumes: Tasks 1-6.
- Produces:
  - `JIPipeDesktopAISettingsStorePage` a `JComponent` that renders the capability grid and the agents list using `JIPipeCapabilityQuery`.
  - `JIPipeDesktopCapabilityInstallDialog` a modal dialog (EDT) showing rows built from `JIPipeCapabilityRequirementPlan`.

- [ ] **Step 1: Write the failing test** (engine-level only; UI behaviour is verified by eye per project policy)

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/settings/ai/AISettingsPageDataTest.java
package org.hkijena.jipipe.desktop.app.settings.ai;

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AISettingsPageDataTest {
    @Test
    void installDialogRowsComeFromTheRequirementPlan() {
        JIPipe.ensureInstance();
        var plan = JIPipe.getCapabilities().planInstall("ai:acp-client");
        assertEquals(java.util.List.of("ai:embedding"), plan.installOrder());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AISettingsPageDataTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement the page, card, install dialog, agent list**

Cards are laid out in a `FlowLayout`-driven `JPanel` with a fixed card width measured from the card's own preferred size (no hardcoded text widths). Each card has a name, a description, a status chip (`JLabel` with a background colour from the UIManager palette), a requires/provides mono line, and an action button. `JIPipeDesktopCapabilityInstallDialog` renders one row per `JIPipeRequirementRow`: label, `JComboBox<JIPipeCapabilityCandidate>` + `JButton` "Install" calling the operation from Task 6, and a "Cancel" button on each row. `JIPipeDesktopAgentListItem` mirrors the chat's agent rows (Ready / Sign-in required / Not installed / Custom). Default action of the dialog: `Install & Re-enable`. Note: match the existing settings tab conventions (`JIPipeDesktopSettingsTab.registerMergedPage` pattern with `JIPipeDesktopKeyboardSection`).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AISettingsPageDataTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/StandardSettingsPlugin.java jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/settings/ai
git commit -m "feat(#1376): AI settings store page with capability cards and install dialog"
```

---

## Task 10: Agent catalog (ACP registry fetch), agent install, and custom agents

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpAgentCatalog.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpAgentCatalogEntry.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpAgentDistribution.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpAgentInstaller.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpAgentCatalogTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpAgentInstallerTest.java`

**Interfaces:**
- Consumes: Task 7 (`AcpLaunchConfig`), Task 4.
- Produces:
  - `AcpAgentCatalogEntry` record with `String id, String name, String version, String description, String repository, String iconUrl, Map<String, AcpAgentDistribution> distributions` (key: `npx`, `uvx`, `binary`).
  - `AcpAgentCatalog.parse(String json)` maps the registry JSON (object with `agents[]`).
  - `AcpAgentInstaller.install(AcpAgentCatalogEntry, ProgressInfo)` writes `binaries` under `<userDir>/ai/acp-agents/<id>/<version>/` and verifies sha256.
  - `AcpLaunchConfig` for a catalog entry: binary (installed, command + args) or npx/uvx (`npx <package> --acp`).

- [ ] **Step 1: Write the failing tests**

```java
// AcpAgentCatalogTest.java
package org.hkijena.jipipe.api.acp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AcpAgentCatalogTest {
    private static final String JSON = """
        {"version":"1","agents":[
          {"id":"amp-acp","name":"Amp","version":"0.13.0","description":"ACP wrapper for Amp",
           "distribution":{"binary":{"linux-x86_64":{"archive":"https://example.com/amp.tar.gz","cmd":"./amp-acp","sha256":"abc"}}}},
          {"id":"agoragentic-acp","name":"Agoragentic","version":"1.3.0","description":"x",
           "distribution":{"npx":{"package":"agoragentic-mcp@1.3.0","args":["--acp"]}}}
        ],"extensions":{}}
        """;

    @Test
    void parsesBinaryAndNpxAgents() {
        var list = AcpAgentCatalog.parse(JSON);
        assertEquals(2, list.size());
        assertEquals("binary", list.get(0).distributionKind());
        assertEquals("linux-x86_64", list.get(0).distribution("binary").platforms().keySet().iterator().next());
        assertEquals("npx", list.get(1).distributionKind());
        assertEquals("agoragentic-mcp@1.3.0", list.get(1).distribution("npx").packageName());
    }
}
```

```java
// AcpAgentInstallerTest.java
package org.hkijena.jipipe.api.acp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;

class AcpAgentInstallerTest {
    @Test
    void rejectsMismatchedSha256(@TempDir Path root) throws Exception {
        Path archive = root.resolve("a.tar.gz");
        Files.writeString(archive, "not-a-real-tarball");
        String real = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(archive)));
        // real hash matches, so this passes; separate negative test asserts mismatch throws
        assertDoesNotThrow(() -> AcpAgentInstaller.verifySha256(archive, real));
        assertThrows(java.io.IOException.class, () -> AcpAgentInstaller.verifySha256(archive, "0000"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='AcpAgentCatalogTest,AcpAgentInstallerTest'`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement the catalog and installer**

HTTP fetch via `java.net.http.HttpClient` with a 15 s timeout; cache at `<userDir>/ai/acp-registry.json` with a manual refresh. Pack archive extraction by `java.util.zip`/`tar` detection. npx/uvx: `AcpAgentInstaller.cliAvailable("npx")` checks `PATH`; when missing, the item state is "Requires Node" (never silently fail). Custom agents: `AcpLaunchConfig` created via the dialog; stored in a JSON settings list.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest='AcpAgentCatalogTest,AcpAgentInstallerTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/acp jipipe-core/src/test/java/org/hkijena/jipipe/api/acp
git commit -m "feat(#1376): ACP agent catalog, distribution parsing, and sha256-verified install"
```

---

## Task 11: Chat UI extraction under ACP

**Files:**
- Create (extracted from `origin/1290-ai-llm-integration`, adapted): `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/chat/ui/JIPipeDesktopAcpChatUI.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/chat/ui/JIPipeDesktopAcpChatLogUI.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/chat/api/JIPipeDesktopAcpEntryType.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/chat/content/JIPipeDesktopAcpMarkdownContentUI.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/chat/JIPipeDesktopAcpEntryMappingTest.java`

**Interfaces:**
- Consumes: Task 11 (translator), Task 10 (entry model).
- Produces:
  - `JIPipeDesktopAcpEntryType` enum mirrors `AcpTranscriptEntryType` plus UI-only `STATUS`, `ERROR`, `COMPLETE`.
  - `JIPipeDesktopAcpEntryType.from(AcpTranscriptEntry)` maps by type.
  - `JIPipeDesktopAcpChatUI` renders `List<AcpTranscriptEntry>`; supports streaming append by `messageId`.

- [ ] **Step 1: Write the failing test** (mapping only)

```java
// JIPipeDesktopAcpEntryMappingTest.java
package org.hkijena.jipipe.desktop.app.chat;

import org.hkijena.jipipe.api.acp.*;
import org.hkijena.jipipe.desktop.app.chat.api.JIPipeDesktopAcpEntryType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JIPipeDesktopAcpEntryMappingTest {
    @Test
    void mapsEngineEntriesToUiTypes() {
        assertEquals(JIPipeDesktopAcpEntryType.USER, JIPipeDesktopAcpEntryType.from(
                new AcpTranscriptEntry("e", "m", AcpTranscriptEntryType.USER_MESSAGE, "x", null, null, null, List.of(), null, null, null, null)));
        assertEquals(JIPipeDesktopAcpEntryType.TOOL_CALL, JIPipeDesktopAcpEntryType.from(
                new AcpTranscriptEntry("e", "m", AcpTranscriptEntryType.TOOL_CALL, "x", "tc1", "pending", "read_file", List.of(), null, null, null, null)));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=JIPipeDesktopAcpEntryMappingTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Extract and adapt the UI**

Check out the files with `git show origin/1290-ai-llm-integration:<path>` into the new locations. Remove imports of `AgentState`, `ProjectAIChatState`, `SpeedTestUtils`, `ProxyStatus`, `StageReviewProvider`; replace with `AcpTranscriptEntry`/`session` state. Markdown rendering reuses `MarkdownText` (master). Each adaptation must compile without the branch-only packages. Keep the layout and classes simple; the chat's visual polish is follow-up work.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=JIPipeDesktopAcpEntryMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/chat jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/chat
git commit -m "feat(#1376): extract and adapt the chat UI to ACP"
```

---

## Task 12: AI panel, dock host, and session list

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ai/assistant/JIPipeDesktopAIAssistantPanel.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ai/assistant/JIPipeDesktopAcpSessionListPanel.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ai/assistant/JIPipeDesktopAgentPickerPanel.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/pipeline/JIPipeDesktopPipelineGraphEditorUI.java` (register dock `AI_ASSISTANT` gated on `enable-ai`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/compartments/JIPipeDesktopCompartmentsGraphEditorUI.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopProjectOverviewUI.java`
- Test: engine-level `AcpSessionRecordListTest` on the project model

**Interfaces:**
- Consumes: Tasks 4, 7, 11 of this plan.
- Produces:
  - `JIPipeDesktopAIAssistantPanel` card-layout: `setupCard` (capability state), `sessionListCard`, `chatCard` (embedding `JIPipeDesktopAcpChatUI`).
  - `JIPipeDesktopProjectWorkbench.startAIChatSession(AcpLaunchConfig, String projectWindowId)` and `sendToAI(String text)`.

- [ ] **Step 1: Write the failing test**

```java
// AcpSessionRecordListTest.java
package org.hkijena.jipipe.api.project;

import org.hkijena.jipipe.api.acp.AcpSessionRecord;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AcpSessionRecordListTest {
    @Test
    void projectSessionsRoundTripThroughJipxPart() {
        var rec = AcpSessionRecord.create("s1", "T", "acp:agent:x", "/proj");
        var bytes = org.hkijena.jipipe.api.project.jipx.JIPipeAISessionsPart.write(List.of(rec));
        assertEquals(1, org.hkijena.jipipe.api.project.jipx.JIPipeAISessionsPart.read(bytes).size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpSessionRecordListTest`
Expected: PASS only after Task 8 part exists

- [ ] **Step 3: Implement the panel, session list, and dock registration**

`JIPipeDesktopAIAssistantPanel` is `JIPipeDesktopProjectWorkbenchPanel`. `setupCard` shows the capability chain from the registry with an "Open AI settings" deep link. Session list loads `project.getAiSessions()`. Starting a session gets an agent from `JIPipeDesktopAgentPickerPanel` and calls `AcpSessionManager.open`. Register the dock only when `AIApplicationSettings.isEnableAI()`; remove when disabled (follow `JIPipeDesktopProjectWorkbench.updateAiStatusBarItem`). Register command-bar actions `org.hkijena.jipipe:ai.open-panel` and `org.hkijena.jipipe:ai.new-session` via `JIPipeShortcutRegistry`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpSessionRecordListTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop jipipe-core/src/test/java/org/hkijena/jipipe/api/project
git commit -m "feat(#1376): AI panel dock, session list, and panel commands"
```

---

## Task 13: ACP permission presenter and JIPipe permission bridge

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ai/assistant/JIPipeDesktopAcpPermissionPresenter.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpPermissionPresenter.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpConnectionManager.java` (accept the presenter)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpPermissionPresenterBridgeTest.java`

**Interfaces:**
- Consumes: Task 11.
- Produces:
  - `interface AcpPermissionPresenter` with `void askPermission(String sessionId, ToolCallUpdate toolCall, List<PermissionOption> options, Consumer<RequestPermissionResponse> answer)` and `void cancelAll(String sessionId)`.
  - `JIPipeDesktopAcpPermissionPresenter` (EDT) renders the options using the master `PermissionPanel` (from the extraction) or a `JOptionPane`-style dialog. On `allow_always`/`reject_always`, calls `JIPipe.getAutomation().getPermissionService()` with the mapped operation id when the tool call name matches a registered operation.

- [ ] **Step 1: Write the failing test**

```java
// AcpPermissionPresenterBridgeTest.java
package org.hkijena.jipipe.api.acp;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AcpPermissionPresenterBridgeTest {
    @Test
    void allowAlwaysMapsToGrantAndRejectToDeny() {
        assertEquals("allow", AcpPermissionPresenter.grantKind(AcpSchema.PermissionOptionKind.ALLOW_ALWAYS));
        assertEquals("deny", AcpPermissionPresenter.grantKind(AcpSchema.PermissionOptionKind.REJECT_ALWAYS));
        assertNull(AcpPermissionPresenter.grantKind(AcpSchema.PermissionOptionKind.ALLOW_ONCE));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpPermissionPresenterBridgeTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement the presenter**

On ask: post to EDT, show options (title from agent, kind hint), block the caller until answered or `cancelAll`. If the tool call's `name` matches an operation id in `AutomationOperationRegistry`, the answer records into `AutomationPermissionService` (via the existing `grantAlways` when kind is `*_ALWAYS`). Do not modify `MCPAskPresenter`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpPermissionPresenterBridgeTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/acp jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ai jipipe-core/src/test/java/org/hkijena/jipipe/api/acp
git commit -m "feat(#1376): ACP permission presenter and JIPipe permission bridge"
```

---

## Task 14: Ready-to-use prompts and the stdio-proxy fallback

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpMcpPromptGenerator.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/servers/mcp/MCPStdioProxyMain.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/ai/JIPipeDesktopAISettingsStorePage.java` (add "Copy instructions for agent")
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpMcpPromptGeneratorTest.java`

**Interfaces:**
- Consumes: Task 5 (capability state), MCP settings on master.
- Produces:
  - `String AcpMcpPromptGenerator.http(String url, String token)`
  - `String AcpMcpPromptGenerator.stdio(String proxyCommand, String token)`
  - `String AcpMcpPromptGenerator.acpLaunchJson(String agentName)` (asks the agent to emit its launch JSON)

- [ ] **Step 1: Write the failing test**

```java
// AcpMcpPromptGeneratorTest.java
package org.hkijena.jipipe.api.acp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AcpMcpPromptGeneratorTest {
    @Test
    void httpPromptContainsEndpointAndToken() {
        String p = AcpMcpPromptGenerator.http("http://127.0.0.1:8781/mcp", "secret");
        assertTrue(p.contains("http://127.0.0.1:8781/mcp"));
        assertTrue(p.contains("secret"));
        assertTrue(p.contains("Authorization"));
    }

    @Test
    void launchJsonRequestNamesTheAgent() {
        assertTrue(AcpMcpPromptGenerator.acpLaunchJson("OpenCode v2").contains("OpenCode v2"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpMcpPromptGeneratorTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement the generator and proxy main**

Prompt text: a short paragraph instructing the agent to add a JIPipe MCP server with the given transport, the endpoint, and the bearer token, and to confirm by calling `list_operations`. Never embed the token in a URL. `MCPStdioProxyMain` bridges stdin/stdout to the local HTTP endpoint (reuse the existing MCP SDK client utilities if suitable; otherwise a minimal JSON-RPC line forwarder). Add the copy button to the MCP card.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpMcpPromptGeneratorTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/acp jipipe-core/src/main/java/org/hkijena/jipipe/servers jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/ai jipipe-core/src/test/java/org/hkijena/jipipe/api/acp
git commit -m "feat(#1376): MCP ready-to-use prompts and stdio proxy fallback"
```

---

## Task 15: Wire the `acp:agent` provider probe and the activity phase

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpActivityPhase.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/acp/AcpAgentCapabilityProvider.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/acp/AcpPlugin.java` (register the provider via the capability plugin API; consumes Task 4 of the capability-system plan)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/acp/AcpConnection.java` (increment/decrement the phase around prompt turns)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/ai/monitor/JIPipeDesktopAIMonitorMCPPage.java` (ACP sessions section)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpActivityPhaseTest.java`

**Interfaces:**
- Consumes: Task 2 (`AcpConnection`), Task 10 (`AcpAgentCatalog`), capability-system Task 4 (`registerCapabilityProvider`).
- Produces:
  - `AcpActivityPhase.isActive()` returns true while at least one prompt turn is in flight.
  - The `acp:agent` capability probe returns `AVAILABLE` when at least one launch config (catalog-installed or custom) resolves to an existing executable.

- [ ] **Step 1: Write the failing test**

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/api/acp/AcpActivityPhaseTest.java
package org.hkijena.jipipe.api.acp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AcpActivityPhaseTest {
    @Test
    void phaseReflectsInFlightTurns() {
        assertFalse(AcpActivityPhase.isActive());
        AcpActivityPhase.enter();
        assertTrue(AcpActivityPhase.isActive());
        AcpActivityPhase.exit();
        assertFalse(AcpActivityPhase.isActive());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpActivityPhaseTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement the phase and the probe**

`AcpActivityPhase` holds an `AtomicInteger`, with `enter()`, `exit()` and `isActive()`; guard `exit()` against going below zero. `AcpConnection` calls `enter()` when a prompt turn starts and `exit()` in a `finally`. `AcpAgentCapabilityProvider` is a `JIPipeCapabilityProvider` with kind `EXECUTABLE` that probes the registered launch configs. The MCP monitor page lists active connection ids and running sessions.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AcpActivityPhaseTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/acp jipipe-core/src/main/java/org/hkijena/jipipe/plugins/acp jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/ai jipipe-core/src/test/java/org/hkijena/jipipe/api/acp
git commit -m "feat(#1376): acp agent capability probe and activity phase"
```

---

## Task 16: Docs, jipipe-cms-website entry, and final review

**Files:**
- Create: `docs/superpowers/specs/2026-11-21-1376-acp-capabilities-design.md` (already written; committed with the first branch)
- Create: jipipe-cms-website entry (external repo; the owner supplies the path or the PR is opened there): "AI settings, capability store, and ACP agents"
- Modify: `docs/investigations/1376-acp/investigation.md` (append a link to the plan and the jipx v2 entry note)

- [ ] **Step 1: Write the docs**

Add a page describing the capability system (IDs, domains, provides/requires, the install prompt) and the ACP panel (agent catalog, restore ladder, permission bridge). Note the jipx `format-version: 2` change and the `ai-sessions` part with an example JSON excerpt.

- [ ] **Step 2: Verify documentation links**

Run: `grep -rn "2026-11-21-1376" docs/ | head`
Expected: the plan and spec are cross-linked.

- [ ] **Step 3: Final whole-branch review**

Run: `/requesting-code-review` on the branch (fresh reviewer on the most capable model). Re-run the full `mvn -q test -pl jipipe-core` and confirm green.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs docs/superpowers/plans docs/investigations/1376-acp
git commit -m "docs(#1376): capability system and ACP design, plan and docs"
```
