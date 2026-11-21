# AI Capability System - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A domain-agnostic capability registry that gates AI features, drives the settings store, and answers agents over MCP about what is installed and what is missing.

**Architecture:** A core `api/capabilities` package registers capabilities, domains and providers and computes state on demand from artifacts, environments and services; the MCP layer exposes it as operations; the AI settings store page renders it.

**Tech Stack:** Java 21, Maven, Swing, Jackson 2 (jipipe-core), MCP SDK `io.modelcontextprotocol.sdk:mcp:2.2.0` (already present), JUnit 5.

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

1. **A capability provider that disappears between two queries** (artifact uninstalled, agent binary deleted, service stopped). State must flip to blocked/missing on the next query without restarting anything.
2. **Cycle or duplicate registration in `requires`.** Must be rejected at registration with a message naming the cycle, never hang the resolver.
3. **An install that fails halfway.** The store must show the failed candidate and leave the capability in `NEEDS_SETUP`, not claim success; a cancelled install must not leave a half-written artifact marked installed.

---

## Task 1: Capability registry data model, derivation rules, and the resolver

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeCapability.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityDomain.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityProvider.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeProviderKind.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityRegistry.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityStatus.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityRegistryTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `JIPipeCapability(String id, String domain, String name, String description, String iconPath, List<String> requires)`
  - `JIPipeCapabilityDomain(String id, String name, String description, String iconPath)`
  - `JIPipeProviderKind{ARTIFACT, ENVIRONMENT, SERVICE, EXECUTABLE, CUSTOM}` with `getPrefix()` (`artifact:`, `environment:`, `service:` ...)
  - `JIPipeCapabilityProvider` is a plain class built with `JIPipeCapabilityProvider.builder(Supplier<JIPipeCapabilityStatus.State> probe)` (builder methods `kind(JIPipeProviderKind)`, `provides(String...)`, `alias(String from, String... to)`, `build()`). The probe returns `AVAILABLE` or `UNAVAILABLE`; the builder validates that `provides` is non-empty.
  - `JIPipeCapabilityRegistry` methods: `registerDomain(JIPipeCapabilityDomain)`, `register(JIPipeCapability)`, `registerProvider(JIPipeCapabilityProvider)`, `List<JIPipeCapabilityDomain> getDomains()`, `Optional<JIPipeCapability> find(String capabilityId)`, `JIPipeCapabilityStatus getStatus(String capabilityId)`
  - `JIPipeCapabilityStatus` record with fields `String id, State state, List<String> blockedBy, List<String> providedBy`, enum `State { AVAILABLE, NEEDS_SETUP, BLOCKED, MISSING }`.
  - Derived level rules: strip one trailing qualifier segment at a time (`python3.v312` returns `python3`), then strip the provider kind prefix, then follow registered aliases.
- [ ] **Step 1: Write the failing test**

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityRegistryTest.java
package org.hkijena.jipipe.api.capabilities;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JIPipeCapabilityRegistryTest {

    private static JIPipeCapabilityProvider present(String... provides) {
        return JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.AVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides(provides).build();
    }
    private static JIPipeCapabilityProvider absent(String... provides) {
        return JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides(provides).build();
    }

    @Test
    void derivesLevelsByStrippingQualifiersAndKindPrefix() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.AVAILABLE)
                .kind(JIPipeProviderKind.EXECUTABLE).provides("python").build());
        reg.registerProvider(present("artifact:system:python3.v312"));
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.EXECUTABLE).provides("python3").build());
        assertEquals(JIPipeCapabilityStatus.State.AVAILABLE, reg.getStatus("python").state(),
                "python3.v312 satisfies python via the chain");
    }

    @Test
    void cyclicRequirementsAreRejectedAtRegistration() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.register(new JIPipeCapability("a", "d", "A", "", null, List.of("b")));
        assertThrows(IllegalArgumentException.class,
                () -> reg.register(new JIPipeCapability("b", "d", "B", "", null, List.of("a"))));
    }

    @Test
    void blockedReportsTheWholeChain() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.register(new JIPipeCapability("embedding", "d", "E", "", null, List.of()));
        reg.register(new JIPipeCapability("mcp", "d", "M", "", null, List.of("embedding")));
        reg.register(new JIPipeCapability("acp", "d", "A", "", null, List.of("mcp")));
        assertEquals(List.of("embedding"), reg.getStatus("acp").blockedBy());
    }

    @Test
    void providerVanishesBetweenQueries() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        boolean[] installed = {true};
        reg.registerProvider(JIPipeCapabilityProvider.builder(() ->
                installed[0] ? JIPipeCapabilityStatus.State.AVAILABLE : JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides("artifact:model").build());
        assertEquals(JIPipeCapabilityStatus.State.AVAILABLE, reg.getStatus("model").state());
        installed[0] = false;
        assertEquals(JIPipeCapabilityStatus.State.MISSING, reg.getStatus("model").state());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=JIPipeCapabilityRegistryTest`
Expected: FAIL (compilation: symbols not found)

- [ ] **Step 3: Implement the model**

Store capabilities and providers in `LinkedHashMap`s. `getStatus(id)` first computes the derived candidate set for `id` (lower the query to the same level scheme, expand it to every provider-provided identifier), then evaluates providers in registration order. Evaluate requirements recursively with a visited set. `MISSING` means no provider, present or absent, can satisfy the capability or any of its derived levels. `BLOCKED` means a requirement's `blockedBy` is non-empty; report the flattened set of the deepest missing requirements. `NEEDS_SETUP` means a provider is present but its `probe` says `UNAVAILABLE`. `AVAILABLE` means the probe says `AVAILABLE` and all requirements resolve to `AVAILABLE`. The cycle check happens at `register(...)` by walking `requires` before inserting.

JIPipeCapabilityProvider is a plain class (not a record): it needs a builder for a readable registration call, and Jackson never serializes providers. Provide a `builder(Supplier<JIPipeCapabilityStatus.State>)` static plus a `build()` that validates `provides` is non-empty.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=JIPipeCapabilityRegistryTest`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities
git commit -m "feat(#1376): capability registry model, derivation rules, and resolver"
```

---

## Task 2: Ranked candidates and the query result shape (the MCP answer)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityQuery.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityProvider.java` (add `getInstallHint()`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityRegistry.java` (add `query(String)`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityQueryTest.java`

**Interfaces:**
- Consumes: Task 1 types.
- Produces:
  - `JIPipeCapabilityQuery(String id, JIPipeCapabilityStatus status, JIPipeInstallHint install, List<JIPipeCapabilityCandidate> candidates)` (a record)
  - `JIPipeInstallHint(String method, String size, String instruction)` where `method` is one of `"artifact"`, `"auto_on_demand"`, `"manual"`, `"none"`.
  - `JIPipeCapabilityCandidate(String providerId, String displayName, JIPipeInstallHint hint, boolean present)`
  - `JIPipeCapabilityRegistry.query(String id)` returns `JIPipeCapabilityQuery`. `install` is the hint of the highest-ranked absent candidate, or `none`.

- [ ] **Step 1: Write the failing test**

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityQueryTest.java
package org.hkijena.jipipe.api.capabilities;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JIPipeCapabilityQueryTest {

    @Test
    void cellpose4ReportsAutoOnDemandInstallAndItsCandidates() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides("artifact:com.github.mouseland.cellpose4.cu124")
                .installHint(new JIPipeInstallHint("auto_on_demand", "4GB", null)).build());
        JIPipeCapabilityQuery q = reg.query("cellpose4");
        assertEquals(JIPipeCapabilityStatus.State.NEEDS_SETUP, q.status().state());
        assertEquals("auto_on_demand", q.install().method());
        assertEquals("4GB", q.install().size());
        assertEquals(1, q.candidates().size());
        assertFalse(q.candidates().get(0).present());
    }

    @Test
    void presentProviderAppearsInProvidedBy() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.AVAILABLE)
                .kind(JIPipeProviderKind.EXECUTABLE).provides("python3").build());
        JIPipeCapabilityQuery q = reg.query("python");
        assertEquals(JIPipeCapabilityStatus.State.AVAILABLE, q.status().state());
        assertEquals(List.of("executable:python3"), q.status().providedBy());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=JIPipeCapabilityQueryTest`
Expected: FAIL (compilation: symbols not found)

- [ ] **Step 3: Implement ranked candidates and query**

Add `installHint` (nullable) and `displayName` to the provider; `query` filters providers satisfying the derived level set, sorts present-first, then by registration order. `getStatus(...).providedBy()` lists the provider identifiers whose probe returned `AVAILABLE`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=JIPipeCapabilityQueryTest`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities
git commit -m "feat(#1376): capability query with ranked candidates and install hints"
```

---

## Task 3: Requirement expansion and the equivalent-selection plan

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityRequirementPlan.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityRegistry.java` (add `planInstall(String)`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityRequirementPlanTest.java`

**Interfaces:**
- Consumes: Tasks 1-2.
- Produces:
  - `JIPipeCapabilityRequirementPlan(String requestedId, List<JIPipeRequirementRow> rows, List<String> installOrder)` (record)
  - `JIPipeRequirementRow(String capabilityId, String reason, List<JIPipeCapabilityCandidate> options, String preselectedProviderId)` where `reason` is the id of the capability that required it and `preselectedProviderId` is `null` when there is no default.
  - `String JIPipeCapabilityRegistry.resolveInstallOrder(String id)` returns a topological order of the capabilities that must be installed, requirements first.

- [ ] **Step 1: Write the failing test**

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities/JIPipeCapabilityRequirementPlanTest.java
package org.hkijena.jipipe.api.capabilities;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JIPipeCapabilityRequirementPlanTest {

    @Test
    void expandingAcpFindsMcpThenEmbedding() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.register(new JIPipeCapability("embedding", "ai", "Embedding", "", null, List.of()));
        reg.register(new JIPipeCapability("mcp", "ai", "MCP", "", null, List.of("embedding")));
        reg.register(new JIPipeCapability("acp", "ai", "ACP", "", null, List.of("mcp", "embedding")));
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides("artifact:embedding").build());
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.SERVICE).provides("service:mcp").build());
        JIPipeCapabilityRequirementPlan plan = reg.planInstall("acp");
        assertEquals(List.of("embedding", "mcp"), plan.installOrder());
        assertEquals(List.of("acp", "mcp"), plan.rows().stream()
                .map(r -> r.reason()).distinct().sorted().toList());
    }

    @Test
    void missingProviderYieldsEmptyOptions() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.register(new JIPipeCapability("x", "d", "X", "", null, List.of()));
        JIPipeCapabilityRequirementPlan plan = reg.planInstall("x");
        assertEquals(1, plan.rows().size());
        assertTrue(plan.rows().get(0).options().isEmpty());
        assertNull(plan.rows().get(0).preselectedProviderId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=JIPipeCapabilityRequirementPlanTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement planInstall and resolveInstallOrder**

`planInstall` walks the transitive requirements of the requested id (excluding requirements already `AVAILABLE`), creates one `JIPipeRequirementRow` per missing capability with `reason` set to the requiring capability, fills `options` from `query(...).candidates()`, picks `preselectedProviderId` as the first candidate whose hint method is not `none` (otherwise `null`). `resolveInstallOrder` performs a DFS post-order over the requirement graph restricted to non-available nodes, rejecting cycles (defensive; registration already rejects them).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=JIPipeCapabilityRequirementPlanTest`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities
git commit -m "feat(#1376): requirement expansion and equivalent-selection plan"
```

---

## Task 4: Plugin registration API and a registry service component

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeDefaultJavaPlugin.java` (add three delegating methods)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeCapabilitiesServiceComponent.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java` (add `getCapabilities()`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities/CapabilityPluginRegistrationTest.java`

**Interfaces:**
- Consumes: Tasks 1-3.
- Produces:
  - `void JIPipeDefaultJavaPlugin.registerCapabilityDomain(JIPipeCapabilityDomain domain)`
  - `void JIPipeDefaultJavaPlugin.registerCapability(JIPipeCapability capability)`
  - `void JIPipeDefaultJavaPlugin.registerCapabilityProvider(JIPipeCapabilityProvider provider)`
  - `JIPipeCapabilityRegistry JIPipe.getCapabilities()` and `JIPipeService.getCapabilities()` (`JIPipeCapabilitiesServiceComponent`)

- [ ] **Step 1: Write the failing test**

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities/CapabilityPluginRegistrationTest.java
package org.hkijena.jipipe.api.capabilities;

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CapabilityPluginRegistrationTest {

    @Test
    void capabilityServiceIsWiredIntoJIPipe() {
        JIPipe.ensureInstance(); // per AGENTS.md: never touch ij.* before this listener path
        assertNotNull(JIPipe.getCapabilities());
    }

    @Test
    void coreCopyRefinementCapabilitiesAreRegistered() {
        JIPipe.ensureInstance();
        // presence, not count (plugin-aware registry rule)
        assertTrue(JIPipe.getCapabilities().find("ai:embedding").isPresent());
        assertTrue(JIPipe.getCapabilities().find("ai:mcp").isPresent());
        assertTrue(JIPipe.getCapabilities().find("ai:acp-client").isPresent());
        assertTrue(JIPipe.getCapabilities().find("acp:agent").isPresent());
        assertEquals(List.of("ai:embedding"), JIPipe.getCapabilities().find("ai:mcp").orElseThrow().getRequires());
        assertEquals(List.of("ai:mcp"), JIPipe.getCapabilities().find("ai:acp-client").orElseThrow().getRequires());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=CapabilityPluginRegistrationTest`
Expected: FAIL (compilation: `JIPipe.getCapabilities()` not found)

- [ ] **Step 3: Implement the service component and plugin methods**

The component holds a `JIPipeCapabilityRegistry` and delegates the three registration methods loudly (throws `JIPipe`-style `IllegalStateException` if a duplicate id is registered with a different definition). The plugin methods delegate to `JIPipe.getCapabilities()`. Register the domains `ai` (`JIPipeCapabilityDomain("ai", "AI", "AI capabilities and helper services", "actions/ai.png")`) and `acp` (`new JIPipeCapabilityDomain("acp", "Agents", "External AI agents connected through ACP", "actions/ai.png")`) plus the four core capabilities in a new `registerCapabilityDomains()` call from `StandardSettingsPlugin.register(...)`, or a dedicated small plugin if that fits the structure better. The four capabilities' providers are added in Tasks 5, 7, 8 and 12.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=CapabilityPluginRegistrationTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/service jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeDefaultJavaPlugin.java jipipe-core/src/test/java/org/hkijena/jipipe/api/capabilities
git commit -m "feat(#1376): capability registry service component and plugin registration API"
```

---

## Task 5: Artifact-backed providers and the first tenants (embedding, MCP)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities/JIPipeArtifactCapabilityProvider.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/ai/capabilities/AICapabilitiesPluginPart.java` (or static registration in `AIPlugin`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/ai/AIPlugin.java` (register the embedding provider and capability)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register the mcp provider and capability)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/ai/capabilities/AICapabilitiesTest.java`

**Interfaces:**
- Consumes: Tasks 1-4.
- Produces:
  - `JIPipeArtifactCapabilityProvider(String artifactQueryPrefix)` whose probe checks the local artifact repository for an installed artifact whose coordinates start with `artifactQueryPrefix`.
  - `JIPipeCapabilityRegistry` now answers `ai:embedding`, `ai:mcp`, `ai:acp-client` reflectively of installation state.

- [ ] **Step 1: Write the failing test**

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/plugins/ai/capabilities/AICapabilitiesTest.java
package org.hkijena.jipipe.plugins.ai.capabilities;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.capabilities.JIPipeCapabilityQuery;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AICapabilitiesTest {

    @Test
    void mcpIsBlockedWhenEmbeddingIsNotInstalled() {
        JIPipe.ensureInstance();
        JIPipeCapabilityQuery embedding = JIPipe.getCapabilities().query("ai:embedding");
        if (embedding.status().providedBy().isEmpty()) {
            // test machine has no bundled embedding model
            assertTrue(JIPipe.getCapabilities().query("ai:mcp").status().blockedBy().contains("ai:embedding"));
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AICapabilitiesTest`
Expected: FAIL (or pass-with-empty only after Step 3 registers the providers)

- [ ] **Step 3: Register the providers**

`AIPlugin.register`: `registerCapabilityProvider(new JIPipeArtifactCapabilityProvider("cn.ac.baai.bge_small_en").provides("artifact:ai:embedding").installHint(new JIPipeInstallHint("artifact", JIPipeInstallHint.UNKNOWN_SIZE, null)))`, plus the capability `ai:embedding` (`requires` empty). `AutomationPlugin.register`: `ai:mcp` with `requires ["ai:embedding"]`, provider kind `SERVICE` probing `MCPApplicationSettings.isEnable()`; hint `none`. Also register `ai:acp-client` (`requires ["ai:mcp"]`, provider `SERVICE`) here since the feature is a JIPipe-provided client. Task 12 wires the ACP-side probe.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AICapabilitiesTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/capabilities jipipe-core/src/main/java/org/hkijena/jipipe/plugins/ai jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation jipipe-core/src/test/java/org/hkijena/jipipe/plugins/ai/capabilities
git commit -m "feat(#1376): artifact-backed providers and the ai/mcp capability tenants"
```

---

## Task 6: MCP operations for capability status, listing, and install

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/CapabilityOperations.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (register the operations)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/CapabilityOperationsTest.java`

**Interfaces:**
- Consumes: Tasks 1-5.
- Produces automation operations:
  - `get_capability_status` (read-only) input `{"capability": "<id>"}` output `{"id","state","blockedBy","install","setup","candidates"}`
  - `list_capabilities` (read-only) input `{"domain": "<optional>"}` output `{"capabilities":[...]}`
  - `install_capability` (async, not read-only) input `{"capability","provider"}` output `{"jobId"}`

- [ ] **Step 1: Write the failing test**

```java
// jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/CapabilityOperationsTest.java
package org.hkijena.jipipe.api.automation.operations;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CapabilityOperationsTest {

    @Test
    void statusOperationSchemaRequiresCapability() {
        CapabilityOperations.GetStatus op = new CapabilityOperations.GetStatus();
        assertEquals("get_capability_status", op.getId());
        assertTrue(op.isReadOnly());
        assertEquals("get_capability_status", op.getId());
        assertTrue(op.getInputSchema().toString().contains("\"capability\""));
    }

    @Test
    void installOperationIsAsync() {
        CapabilityOperations.Install op = new CapabilityOperations.Install();
        assertInstanceOf(org.hkijena.jipipe.api.automation.AsyncAutomationOperation.class, op);
        assertFalse(op.isReadOnly());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=CapabilityOperationsTest`
Expected: FAIL (compilation)

- [ ] **Step 3: Implement the three operations**

`GetStatus.execute` returns the `JIPipeCapabilityQuery` serialized via Jackson 2 to a `JsonNode`. `ListCapabilities` returns one query per capability in a domain. `Install` extends `AsyncAutomationOperation`; it resolves the provider, runs the existing `JIPipeArtifactRepositoryInstallArtifactRun` (or the environment setup tool) on a background job, returns the `InstrumentationJob`, and publishes a capability-changed event on the context event bus. Register all three in `AutomationPlugin.register`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=CapabilityOperationsTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/CapabilityOperations.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations
git commit -m "feat(#1376): capability status, list, and async install MCP operations"
```

---
