# General Instrumentation Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a WebSocket-based instrumentation layer in jipipe-core that exposes JIPipe project operations (queries, execution, modifications, search) for external automation tools, with a typed Java API that the AI agent will later delegate to.

**Architecture:** Hybrid approach — a typed `InstrumentationAPI` Java facade provides built-in operations; an `InstrumentationOperation` interface enables plugin-registered WebSocket commands. An `InstrumentationEventBus` distributes project change events; an `InstrumentationJobManager` tracks async pipeline runs. A `InstrumentationServer` (WebSocket, localhost) dispatches commands to the operation registry and broadcasts events to clients.

**Tech Stack:** Java 21, Maven, Jackson (JSON), java-websocket 1.5.7, JUnit 5, Mockito

## Global Constraints

- Java 21 required (`maven.compiler.source=21`)
- All code in package `org.hkijena.jipipe.api.instrumentation` (unless modifying existing files)
- WebSocket server binds to `127.0.0.1` only
- No authentication (localhost-only)
- Uses existing JDK `com.sun.net.httpserver` patterns and `org.java_websocket` library
- Follow existing JIPipe plugin registration patterns (`JIPipeDefaultJavaPlugin.register()` convenience methods)
- No new external dependencies beyond `java-websocket` (already used in jipipe-2) and `mockito-core` (test only)
- Tests use JUnit 5 + Mockito
- All POJOs use Jackson annotations for JSON serialization

---

## File Structure

### New files (jipipe-core)

```
src/main/java/org/hkijena/jipipe/api/instrumentation/
├── InstrumentationAPI.java
├── InstrumentationOperation.java
├── AsyncInstrumentationOperation.java
├── InstrumentationOperationRegistry.java
├── InstrumentationContext.java
├── InstrumentationEventBus.java
├── InstrumentationEvent.java
├── InstrumentationJob.java
├── InstrumentationJobStatus.java
├── InstrumentationJobManager.java
├── InstrumentationRunEngine.java
├── RunMode.java
├── InstrumentationServer.java
├── InstrumentationProtocol.java
├── InstrumentationApplicationSettings.java
├── InstrumentationPlugin.java
├── operations/
│   ├── ProjectOperations.java
│   ├── QueryOperations.java
│   ├── ExecutionOperations.java
│   ├── ModificationOperations.java
│   └── PipelineMapOperations.java
├── events/
│   ├── ProjectListChangedEvent.java
│   ├── ProjectChangedEvent.java
│   ├── NodeAddedEvent.java
│   ├── NodeRemovedEvent.java
│   ├── ConnectionChangedEvent.java
│   ├── ParameterChangedEvent.java
│   ├── CompartmentChangedEvent.java
│   ├── JobStartedEvent.java
│   ├── JobProgressEvent.java
│   └── JobCompletedEvent.java
└── pipeline_map/
    ├── PipelineMap.java
    ├── SegmentNode.java
    ├── SegmentEdge.java
    └── SegmentStatus.java

src/main/java/org/hkijena/jipipe/api/service/components/
└── JIPipeInstrumentationServiceComponent.java

src/test/java/org/hkijena/jipipe/api/instrumentation/
├── InstrumentationEventBusTest.java
├── InstrumentationJobManagerTest.java
├── InstrumentationOperationRegistryTest.java
├── InstrumentationAPITest.java
└── InstrumentationServerTest.java
```

### Modified files

- `jipipe-core/pom.xml` — add java-websocket, mockito-core
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java` — add instrumentation service component
- `jipipe-core/src/main/java/org/hkijena/jipipe/JIPipe.java` — add `getInstrumentation()`
- `jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeDefaultJavaPlugin.java` — add `registerInstrumentationOperation()`
- `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/GuiCommand.java` — add `--instrumentation` flag

---

### Task 1: Add Dependencies and Service Infrastructure

**Files:**
- Modify: `jipipe-core/pom.xml` (add java-websocket and mockito dependencies)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeInstrumentationServiceComponent.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/JIPipe.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeDefaultJavaPlugin.java`

**Interfaces:**
- Produces: `JIPipeInstrumentationServiceComponent` with `getOperationRegistry()` method, `JIPipe.getInstrumentation()` static accessor, `JIPipeDefaultJavaPlugin.registerInstrumentationOperation(String, InstrumentationOperation)` convenience method. (Note: `InstrumentationOperation` and `InstrumentationOperationRegistry` are created in Task 4 — this task creates a minimal placeholder interface so the service component compiles.)

- [ ] **Step 1: Add dependencies to jipipe-core/pom.xml**

Add before the closing `</dependencies>` tag (after the `ai.djl.huggingface` tokenizers dependency):

```xml
        <dependency>
            <groupId>org.java-websocket</groupId>
            <artifactId>Java-WebSocket</artifactId>
            <version>1.5.7</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Create minimal InstrumentationOperation interface (placeholder for Task 4)**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationOperation.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for operations that can be executed via the instrumentation layer.
 * Operations are registered in the {@link InstrumentationOperationRegistry}
 * and dispatched by the {@link InstrumentationServer}.
 */
public interface InstrumentationOperation {
    String getId();
    String getDescription();
    JsonNode execute(InstrumentationContext ctx, JsonNode params) throws Exception;
}
```

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationOperationRegistry.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of all instrumentation operations (built-in and plugin-registered).
 */
public class InstrumentationOperationRegistry {
    private final Map<String, InstrumentationOperation> operations = new ConcurrentHashMap<>();

    public void register(String id, InstrumentationOperation operation) {
        operations.put(id, operation);
    }

    public InstrumentationOperation get(String id) {
        return operations.get(id);
    }

    public boolean has(String id) {
        return operations.containsKey(id);
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(operations.keySet());
    }

    public Map<String, InstrumentationOperation> getAll() {
        return Collections.unmodifiableMap(operations);
    }
}
```

Create a minimal `InstrumentationContext.java` (will be expanded in Task 3):

```java
package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;

/**
 * Context passed to instrumentation operations.
 */
public class InstrumentationContext {
    private final JIPipeProject project;
    private final JIPipeProgressInfo progressInfo;

    public InstrumentationContext(JIPipeProject project, JIPipeProgressInfo progressInfo) {
        this.project = project;
        this.progressInfo = progressInfo != null ? progressInfo : new JIPipeProgressInfo();
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }
}
```

- [ ] **Step 3: Create JIPipeInstrumentationServiceComponent**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeInstrumentationServiceComponent.java`:

```java
package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.instrumentation.InstrumentationOperationRegistry;
import org.hkijena.jipipe.api.service.JIPipeService;

/**
 * Service component that holds the instrumentation operation registry.
 */
public class JIPipeInstrumentationServiceComponent extends JIPipeServiceComponent {
    private final InstrumentationOperationRegistry operationRegistry = new InstrumentationOperationRegistry();

    public JIPipeInstrumentationServiceComponent(JIPipeService service) {
        super(service);
    }

    public InstrumentationOperationRegistry getOperationRegistry() {
        return operationRegistry;
    }
}
```

- [ ] **Step 4: Register component in JIPipeService**

In `JIPipeService.java`, add the field declaration (after the `statistics` field):

```java
    private final JIPipeInstrumentationServiceComponent instrumentation;
```

In the constructor, after `statistics = new JIPipeStatisticsServiceComponent(this);`:

```java
        instrumentation = new JIPipeInstrumentationServiceComponent(this);
```

In the `components` array, add `instrumentation` at the end:

```java
        this.components = new JIPipeServiceComponent[] {
                recentProjects, nodes, dataTypes, imageJDataAdapters, customMenuItems, parameterTypes, applicationSettings, projectSettings, expressionFunctions, utilityClasses,
                environments, plugins, projectTemplates, artifacts, nodeTemplates, acceleration, cleanup, projectBackup, aiService, serverService, statistics, instrumentation
        };
```

Add accessor method (after `getStatistics()` or at the end of the accessors):

```java
    public JIPipeInstrumentationServiceComponent getInstrumentation() {
        ensureInitialized();
        return instrumentation;
    }
```

- [ ] **Step 5: Add static accessor to JIPipe**

In `JIPipe.java`, add after the `getStatistics()` method (or similar accessor):

```java
    public static JIPipeInstrumentationServiceComponent getInstrumentation() {
        return instance.getInstrumentation();
    }
```

Add the import: `import org.hkijena.jipipe.api.service.components.JIPipeInstrumentationServiceComponent;`

- [ ] **Step 6: Add registerInstrumentationOperation to JIPipeDefaultJavaPlugin**

In `JIPipeDefaultJavaPlugin.java`, add this method (after the other `register*` methods, before the closing brace):

```java
    /**
     * Registers an instrumentation operation.
     *
     * @param id        the operation ID (e.g. "query_compartments")
     * @param operation the operation
     */
    public void registerInstrumentationOperation(String id, InstrumentationOperation operation) {
        getService().getInstrumentation().getOperationRegistry().register(id, operation);
    }
```

Add the import: `import org.hkijena.jipipe.api.instrumentation.InstrumentationOperation;`

- [ ] **Step 7: Verify compilation**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add jipipe-core/pom.xml jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/ jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeInstrumentationServiceComponent.java jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java jipipe-core/src/main/java/org/hkijena/jipipe/JIPipe.java jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeDefaultJavaPlugin.java
git commit -m "Add instrumentation service infrastructure (#1305)"
```

---

### Task 2: Core Event System

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationEvent.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationEventBus.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/events/` (10 event classes)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationEventBusTest.java`

**Interfaces:**
- Produces: `InstrumentationEvent` base class with `getProjectId()`, `InstrumentationEventBus` with `subscribe(Class<T>, Consumer<T>)` and `publish(InstrumentationEvent)`, and 10 event subclasses.

- [ ] **Step 1: Write the failing test**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationEventBusTest.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.instrumentation.events.NodeAddedEvent;
import org.hkijena.jipipe.api.instrumentation.events.ParameterChangedEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentationEventBusTest {

    @Test
    void subscribeAndPublish_deliversToMatchingListener() {
        InstrumentationEventBus bus = new InstrumentationEventBus();
        List<NodeAddedEvent> received = new ArrayList<>();

        bus.subscribe(NodeAddedEvent.class, received::add);

        NodeAddedEvent event = new NodeAddedEvent("project-1", "compartment-1", "node-1", "import-image", "Import image");
        bus.publish(event);

        assertEquals(1, received.size());
        assertEquals("node-1", received.get(0).getNodeId());
    }

    @Test
    void subscribeAndPublish_doesNotDeliverToOtherType() {
        InstrumentationEventBus bus = new InstrumentationEventBus();
        List<NodeAddedEvent> nodeEvents = new ArrayList<>();
        List<ParameterChangedEvent> paramEvents = new ArrayList<>();

        bus.subscribe(NodeAddedEvent.class, nodeEvents::add);
        bus.subscribe(ParameterChangedEvent.class, paramEvents::add);

        bus.publish(new NodeAddedEvent("p1", "c1", "n1", "t1", "Name"));

        assertEquals(1, nodeEvents.size());
        assertTrue(paramEvents.isEmpty());
    }

    @Test
    void publish_nullEvent_doesNothing() {
        InstrumentationEventBus bus = new InstrumentationEventBus();
        assertDoesNotThrow(() -> bus.publish(null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=InstrumentationEventBusTest -q`
Expected: FAIL with compilation errors (classes not found)

- [ ] **Step 3: Create InstrumentationEvent base class**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationEvent.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

/**
 * Base class for all instrumentation events.
 * Each event carries the project ID it belongs to.
 */
public abstract class InstrumentationEvent {
    private final String projectId;

    protected InstrumentationEvent(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectId() {
        return projectId;
    }
}
```

- [ ] **Step 4: Create InstrumentationEventBus**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationEventBus.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple publish-subscribe event bus for instrumentation events.
 * Listeners subscribe by event type and are notified when events of that type are published.
 */
public class InstrumentationEventBus {
    private final Map<Class<? extends InstrumentationEvent>, List<Consumer<? extends InstrumentationEvent>>> listeners = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends InstrumentationEvent> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public void publish(InstrumentationEvent event) {
        if (event == null) return;
        List<Consumer<? extends InstrumentationEvent>> listenersForType = listeners.get(event.getClass());
        if (listenersForType != null) {
            for (Consumer<? extends InstrumentationEvent> listener : listenersForType) {
                ((Consumer<InstrumentationEvent>) listener).accept(event);
            }
        }
    }

    public void clear() {
        listeners.clear();
    }
}
```

- [ ] **Step 5: Create event subclasses**

Create each file in `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/events/`:

`ProjectListChangedEvent.java`:
```java
package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class ProjectListChangedEvent extends InstrumentationEvent {
    public ProjectListChangedEvent() {
        super(null);
    }
}
```

`ProjectChangedEvent.java`:
```java
package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class ProjectChangedEvent extends InstrumentationEvent {
    private final String projectName;

    public ProjectChangedEvent(String projectId, String projectName) {
        super(projectId);
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }
}
```

`NodeAddedEvent.java`:
```java
package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class NodeAddedEvent extends InstrumentationEvent {
    private final String compartmentId;
    private final String nodeId;
    private final String nodeTypeId;
    private final String nodeName;

    public NodeAddedEvent(String projectId, String compartmentId, String nodeId, String nodeTypeId, String nodeName) {
        super(projectId);
        this.compartmentId = compartmentId;
        this.nodeId = nodeId;
        this.nodeTypeId = nodeTypeId;
        this.nodeName = nodeName;
    }

    public String getCompartmentId() { return compartmentId; }
    public String getNodeId() { return nodeId; }
    public String getNodeTypeId() { return nodeTypeId; }
    public String getNodeName() { return nodeName; }
}
```

`NodeRemovedEvent.java`:
```java
package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class NodeRemovedEvent extends InstrumentationEvent {
    private final String nodeId;

    public NodeRemovedEvent(String projectId, String nodeId) {
        super(projectId);
        this.nodeId = nodeId;
    }

    public String getNodeId() { return nodeId; }
}
```

`ConnectionChangedEvent.java`:
```java
package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class ConnectionChangedEvent extends InstrumentationEvent {
    private final String source;
    private final String sourceSlot;
    private final String target;
    private final String targetSlot;
    private final String action;

    public ConnectionChangedEvent(String projectId, String source, String sourceSlot, String target, String targetSlot, String action) {
        super(projectId);
        this.source = source;
        this.sourceSlot = sourceSlot;
        this.target = target;
        this.targetSlot = targetSlot;
        this.action = action;
    }

    public String getSource() { return source; }
    public String getSourceSlot() { return sourceSlot; }
    public String getTarget() { return target; }
    public String getTargetSlot() { return targetSlot; }
    public String getAction() { return action; }
}
```

`ParameterChangedEvent.java`:
```java
package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class ParameterChangedEvent extends InstrumentationEvent {
    private final String nodeId;
    private final String key;
    private final String value;

    public ParameterChangedEvent(String projectId, String nodeId, String key, String value) {
        super(projectId);
        this.nodeId = nodeId;
        this.key = key;
        this.value = value;
    }

    public String getNodeId() { return nodeId; }
    public String getKey() { return key; }
    public String getValue() { return value; }
}
```

`CompartmentChangedEvent.java`:
```java
package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class CompartmentChangedEvent extends InstrumentationEvent {
    private final String compartmentId;
    private final String name;
    private final String action;

    public CompartmentChangedEvent(String projectId, String compartmentId, String name, String action) {
        super(projectId);
        this.compartmentId = compartmentId;
        this.name = name;
        this.action = action;
    }

    public String getCompartmentId() { return compartmentId; }
    public String getName() { return name; }
    public String getAction() { return action; }
}
```

`JobStartedEvent.java`:
```java
package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class JobStartedEvent extends InstrumentationEvent {
    private final String jobId;
    private final String operation;
    private final String description;

    public JobStartedEvent(String projectId, String jobId, String operation, String description) {
        super(projectId);
        this.jobId = jobId;
        this.operation = operation;
        this.description = description;
    }

    public String getJobId() { return jobId; }
    public String getOperation() { return operation; }
    public String getDescription() { return description; }
}
```

`JobProgressEvent.java`:
```java
package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class JobProgressEvent extends InstrumentationEvent {
    private final String jobId;
    private final double progress;
    private final String message;

    public JobProgressEvent(String projectId, String jobId, double progress, String message) {
        super(projectId);
        this.jobId = jobId;
        this.progress = progress;
        this.message = message;
    }

    public String getJobId() { return jobId; }
    public double getProgress() { return progress; }
    public String getMessage() { return message; }
}
```

`JobCompletedEvent.java`:
```java
package org.hkijena.jipipe.api.instrumentation.events;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;
import org.hkijena.jipipe.api.instrumentation.InstrumentationJobStatus;

public class JobCompletedEvent extends InstrumentationEvent {
    private final String jobId;
    private final InstrumentationJobStatus status;
    private final JsonNode result;
    private final String error;

    public JobCompletedEvent(String projectId, String jobId, InstrumentationJobStatus status, JsonNode result, String error) {
        super(projectId);
        this.jobId = jobId;
        this.status = status;
        this.result = result;
        this.error = error;
    }

    public String getJobId() { return jobId; }
    public InstrumentationJobStatus getStatus() { return status; }
    public JsonNode getResult() { return result; }
    public String getError() { return error; }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=InstrumentationEventBusTest -q`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/ jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationEventBusTest.java
git commit -m "Add instrumentation event system (#1305)"
```

---

### Task 3: Job Manager and Context

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJobStatus.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJob.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJobManager.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationContext.java` (expand from Task 1 placeholder)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJobManagerTest.java`

**Interfaces:**
- Consumes: `InstrumentationEventBus` from Task 2, `InstrumentationJobStatus` (this task)
- Produces: `InstrumentationJob` (id, status, progress, result, complete/fail/cancel methods), `InstrumentationJobManager` (createJob, getJob, cancelJob), expanded `InstrumentationContext` (with eventBus, jobManager, graphOverride)

- [ ] **Step 1: Write the failing test**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJobManagerTest.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.instrumentation.events.JobCompletedEvent;
import org.hkijena.jipipe.api.instrumentation.events.JobStartedEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentationJobManagerTest {

    @Test
    void createJob_returnsJobWithPendingStatus() {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager manager = new InstrumentationJobManager(eventBus, "project-1");

        InstrumentationJob job = manager.createJob("run_node", "Update cache: Node A");

        assertEquals("run_node", job.getOperation());
        assertEquals("Update cache: Node A", job.getDescription());
        assertEquals(InstrumentationJobStatus.PENDING, job.getStatus());
        assertNotNull(job.getId());
    }

    @Test
    void createJob_publishesJobStartedEvent() {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        List<JobStartedEvent> events = new ArrayList<>();
        eventBus.subscribe(JobStartedEvent.class, events::add);
        InstrumentationJobManager manager = new InstrumentationJobManager(eventBus, "project-1");

        InstrumentationJob job = manager.createJob("run_node", "Test");

        assertEquals(1, events.size());
        assertEquals(job.getId(), events.get(0).getJobId());
    }

    @Test
    void completeJob_setsStatusAndPublishesEvent() {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        List<JobCompletedEvent> events = new ArrayList<>();
        eventBus.subscribe(JobCompletedEvent.class, events::add);
        InstrumentationJobManager manager = new InstrumentationJobManager(eventBus, "project-1");

        InstrumentationJob job = manager.createJob("run_node", "Test");
        job.complete(null);

        assertEquals(InstrumentationJobStatus.COMPLETED, job.getStatus());
        assertEquals(1, events.size());
        assertEquals(job.getId(), events.get(0).getJobId());
        assertEquals(InstrumentationJobStatus.COMPLETED, events.get(0).getStatus());
    }

    @Test
    void failJob_setsFailedStatus() {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager manager = new InstrumentationJobManager(eventBus, "project-1");

        InstrumentationJob job = manager.createJob("run_node", "Test");
        job.fail("Something went wrong");

        assertEquals(InstrumentationJobStatus.FAILED, job.getStatus());
        assertEquals("Something went wrong", job.getError());
    }

    @Test
    void getJob_returnsCreatedJob() {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager manager = new InstrumentationJobManager(eventBus, "project-1");

        InstrumentationJob job = manager.createJob("run_node", "Test");

        assertSame(job, manager.getJob(job.getId()));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=InstrumentationJobManagerTest -q`
Expected: FAIL (InstrumentationJobStatus, InstrumentationJob, InstrumentationJobManager not found)

- [ ] **Step 3: Create InstrumentationJobStatus enum**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJobStatus.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

public enum InstrumentationJobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

- [ ] **Step 4: Create InstrumentationJob class**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJob.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.instrumentation.events.JobCompletedEvent;
import org.hkijena.jipipe.api.instrumentation.events.JobProgressEvent;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks an async instrumentation job (e.g., a pipeline run).
 * Progress and completion are reported via the {@link InstrumentationEventBus}.
 */
public class InstrumentationJob {
    private final String id;
    private final String operation;
    private final String description;
    private final Instant createdAt;
    private final AtomicReference<InstrumentationJobStatus> status = new AtomicReference<>(InstrumentationJobStatus.PENDING);
    private volatile double progress;
    private volatile String message;
    private volatile JsonNode result;
    private volatile String error;
    private volatile Instant completedAt;
    private final InstrumentationEventBus eventBus;
    private final String projectId;
    private final Thread thread;

    InstrumentationJob(String operation, String description, InstrumentationEventBus eventBus, String projectId) {
        this.id = "job-" + UUID.randomUUID().toString().substring(0, 8);
        this.operation = operation;
        this.description = description;
        this.createdAt = Instant.now();
        this.eventBus = eventBus;
        this.projectId = projectId;
        this.thread = Thread.currentThread();
    }

    public String getId() { return id; }
    public String getOperation() { return operation; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public InstrumentationJobStatus getStatus() { return status.get(); }
    public double getProgress() { return progress; }
    public String getMessage() { return message; }
    public JsonNode getResult() { return result; }
    public String getError() { return error; }

    void setStatus(InstrumentationJobStatus newStatus) {
        status.set(newStatus);
    }

    public void updateProgress(double progress, String message) {
        this.progress = progress;
        this.message = message;
        if (status.get() == InstrumentationJobStatus.PENDING) {
            status.set(InstrumentationJobStatus.RUNNING);
        }
        eventBus.publish(new JobProgressEvent(projectId, id, progress, message));
    }

    public void complete(JsonNode result) {
        this.result = result;
        this.completedAt = Instant.now();
        status.set(InstrumentationJobStatus.COMPLETED);
        eventBus.publish(new JobCompletedEvent(projectId, id, InstrumentationJobStatus.COMPLETED, result, null));
    }

    public void fail(String error) {
        this.error = error;
        this.completedAt = Instant.now();
        status.set(InstrumentationJobStatus.FAILED);
        eventBus.publish(new JobCompletedEvent(projectId, id, InstrumentationJobStatus.FAILED, null, error));
    }

    public void cancel() {
        this.completedAt = Instant.now();
        status.set(InstrumentationJobStatus.CANCELLED);
        eventBus.publish(new JobCompletedEvent(projectId, id, InstrumentationJobStatus.CANCELLED, null, "Cancelled by user"));
        thread.interrupt();
    }
}
```

- [ ] **Step 5: Create InstrumentationJobManager**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJobManager.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.instrumentation.events.JobStartedEvent;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and tracks async instrumentation jobs.
 */
public class InstrumentationJobManager {
    private final Map<String, InstrumentationJob> jobs = new ConcurrentHashMap<>();
    private final InstrumentationEventBus eventBus;
    private final String projectId;

    public InstrumentationJobManager(InstrumentationEventBus eventBus, String projectId) {
        this.eventBus = eventBus;
        this.projectId = projectId;
    }

    public InstrumentationJob createJob(String operation, String description) {
        InstrumentationJob job = new InstrumentationJob(operation, description, eventBus, projectId);
        jobs.put(job.getId(), job);
        eventBus.publish(new JobStartedEvent(projectId, job.getId(), operation, description));
        return job;
    }

    public InstrumentationJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    public Collection<InstrumentationJob> getAllJobs() {
        return jobs.values();
    }

    public boolean cancelJob(String jobId) {
        InstrumentationJob job = jobs.get(jobId);
        if (job != null && (job.getStatus() == InstrumentationJobStatus.PENDING || job.getStatus() == InstrumentationJobStatus.RUNNING)) {
            job.cancel();
            return true;
        }
        return false;
    }
}
```

- [ ] **Step 6: Expand InstrumentationContext**

Replace `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationContext.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.project.JIPipeProject;

/**
 * Context passed to instrumentation operations.
 * <p>
 * The {@code graphOverride} allows callers (e.g., the AI agent's stage system)
 * to pass a working graph instead of the project graph. External WebSocket tools
 * always use the project graph (graphOverride is null).
 */
public class InstrumentationContext {
    private final JIPipeProject project;
    private final JIPipeGraph graphOverride;
    private final JIPipeProgressInfo progressInfo;
    private final InstrumentationEventBus eventBus;
    private final InstrumentationJobManager jobManager;
    private final String projectId;

    public InstrumentationContext(JIPipeProject project, String projectId,
                                   JIPipeProgressInfo progressInfo,
                                   InstrumentationEventBus eventBus,
                                   InstrumentationJobManager jobManager) {
        this(project, projectId, null, progressInfo, eventBus, jobManager);
    }

    public InstrumentationContext(JIPipeProject project, String projectId,
                                   JIPipeGraph graphOverride,
                                   JIPipeProgressInfo progressInfo,
                                   InstrumentationEventBus eventBus,
                                   InstrumentationJobManager jobManager) {
        this.project = project;
        this.projectId = projectId;
        this.graphOverride = graphOverride;
        this.progressInfo = progressInfo != null ? progressInfo : new JIPipeProgressInfo();
        this.eventBus = eventBus;
        this.jobManager = jobManager;
    }

    public JIPipeProject getProject() { return project; }
    public String getProjectId() { return projectId; }
    public JIPipeProgressInfo getProgressInfo() { return progressInfo; }
    public InstrumentationEventBus getEventBus() { return eventBus; }
    public InstrumentationJobManager getJobManager() { return jobManager; }

    /**
     * Returns the graph to operate on. If a graphOverride was set (e.g., a stage's
     * working graph), returns that; otherwise returns the project's graph.
     */
    public JIPipeGraph getGraph() {
        if (graphOverride != null) return graphOverride;
        if (project != null) return project.getGraph();
        return null;
    }
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=InstrumentationJobManagerTest -q`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJobStatus.java jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJob.java jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJobManager.java jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationContext.java jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationJobManagerTest.java
git commit -m "Add instrumentation job manager and context (#1305)"
```

---

### Task 4: Complete Operation Framework

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationOperation.java` (already created in Task 1, no changes needed)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/AsyncInstrumentationOperation.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationOperationRegistryTest.java`

**Interfaces:**
- Produces: `AsyncInstrumentationOperation` interface (extends InstrumentationOperation, adds `executeAsync`), tested registry.

- [ ] **Step 1: Write the failing test**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationOperationRegistryTest.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentationOperationRegistryTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void registerAndGet_returnsOperation() {
        InstrumentationOperationRegistry registry = new InstrumentationOperationRegistry();
        InstrumentationOperation op = createOp("test_op", "Test operation");

        registry.register("test_op", op);

        assertSame(op, registry.get("test_op"));
        assertTrue(registry.has("test_op"));
    }

    @Test
    void get_unknownId_returnsNull() {
        InstrumentationOperationRegistry registry = new InstrumentationOperationRegistry();
        assertNull(registry.get("nonexistent"));
        assertFalse(registry.has("nonexistent"));
    }

    @Test
    void getIds_returnsAllRegisteredIds() {
        InstrumentationOperationRegistry registry = new InstrumentationOperationRegistry();
        registry.register("op1", createOp("op1", "One"));
        registry.register("op2", createOp("op2", "Two"));

        assertEquals(2, registry.getIds().size());
        assertTrue(registry.getIds().contains("op1"));
        assertTrue(registry.getIds().contains("op2"));
    }

    private InstrumentationOperation createOp(String id, String desc) {
        return new InstrumentationOperation() {
            @Override public String getId() { return id; }
            @Override public String getDescription() { return desc; }
            @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
                return mapper.createObjectNode();
            }
        };
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=InstrumentationOperationRegistryTest -q`
Expected: PASS (registry was already created in Task 1). If it passes, proceed.

- [ ] **Step 3: Create AsyncInstrumentationOperation interface**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/AsyncInstrumentationOperation.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Extension of {@link InstrumentationOperation} for long-running operations.
 * The {@link InstrumentationServer} detects async operations and returns a
 * job ID immediately, then broadcasts job progress and completion events.
 */
public interface AsyncInstrumentationOperation extends InstrumentationOperation {
    /**
     * Starts the async operation. Returns immediately with a job handle.
     * Progress and completion are reported via the job's event callbacks.
     */
    InstrumentationJob executeAsync(InstrumentationContext ctx, JsonNode params) throws Exception;
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl jipipe-core -Dtest=InstrumentationOperationRegistryTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/AsyncInstrumentationOperation.java jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationOperationRegistryTest.java
git commit -m "Add async operation interface and registry tests (#1305)"
```

---

### Task 5: Pipeline Map POJOs

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/pipeline_map/PipelineMap.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/pipeline_map/SegmentNode.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/pipeline_map/SegmentEdge.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/pipeline_map/SegmentStatus.java`

**Interfaces:**
- Produces: `PipelineMap` (POJO with root `SegmentNode`), `SegmentNode` (hierarchical segment with id, label, summary, children, nodeIds), `SegmentEdge` (connection between segments), `SegmentStatus` (enum). These are pure data classes with Jackson annotations.

- [ ] **Step 1: Create SegmentStatus enum**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/pipeline_map/SegmentStatus.java`:

```java
package org.hkijena.jipipe.api.instrumentation.pipeline_map;

public enum SegmentStatus {
    OK,
    WARNING,
    ERROR,
    UNKNOWN
}
```

- [ ] **Step 2: Create SegmentNode POJO**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/pipeline_map/SegmentNode.java`:

```java
package org.hkijena.jipipe.api.instrumentation.pipeline_map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SegmentNode {
    private String id;
    private String label;
    private String summary;
    private SegmentStatus status = SegmentStatus.UNKNOWN;
    private String compartmentId;
    private List<String> nodeIds = new ArrayList<>();
    private List<SegmentNode> children;

    public SegmentNode() {
    }

    @JsonCreator
    public SegmentNode(
            @JsonProperty("id") String id,
            @JsonProperty("label") String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public SegmentStatus getStatus() { return status; }
    public void setStatus(SegmentStatus status) { this.status = status; }

    public String getCompartmentId() { return compartmentId; }
    public void setCompartmentId(String compartmentId) { this.compartmentId = compartmentId; }

    public List<String> getNodeIds() { return nodeIds; }
    public void setNodeIds(List<String> nodeIds) { this.nodeIds = nodeIds; }

    public List<SegmentNode> getChildren() { return children; }
    public void setChildren(List<SegmentNode> children) { this.children = children; }
}
```

- [ ] **Step 3: Create SegmentEdge POJO**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/pipeline_map/SegmentEdge.java`:

```java
package org.hkijena.jipipe.api.instrumentation.pipeline_map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SegmentEdge {
    private final String source;
    private final String target;
    private final String label;

    @JsonCreator
    public SegmentEdge(
            @JsonProperty("source") String source,
            @JsonProperty("target") String target,
            @JsonProperty("label") String label) {
        this.source = source;
        this.target = target;
        this.label = label;
    }

    public String getSource() { return source; }
    public String getTarget() { return target; }
    public String getLabel() { return label; }
}
```

- [ ] **Step 4: Create PipelineMap POJO**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/pipeline_map/PipelineMap.java`:

```java
package org.hkijena.jipipe.api.instrumentation.pipeline_map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PipelineMap {
    private long version;
    private SegmentNode root;
    private List<SegmentEdge> edges = new ArrayList<>();

    public PipelineMap(SegmentNode root) {
        this(0, root);
    }

    @JsonCreator
    public PipelineMap(
            @JsonProperty("version") long version,
            @JsonProperty("root") SegmentNode root) {
        this.version = version;
        this.root = root;
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public SegmentNode getRoot() { return root; }
    public void setRoot(SegmentNode root) { this.root = root; }

    public List<SegmentEdge> getEdges() { return edges; }
    public void setEdges(List<SegmentEdge> edges) { this.edges = edges; }

    public SegmentNode findSegment(String id) {
        if (root == null) return null;
        return findSegmentRecursive(root, id);
    }

    private static SegmentNode findSegmentRecursive(SegmentNode node, String id) {
        if (node.getId() != null && node.getId().equals(id)) return node;
        if (node.getChildren() != null) {
            for (SegmentNode child : node.getChildren()) {
                SegmentNode found = findSegmentRecursive(child, id);
                if (found != null) return found;
            }
        }
        return null;
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/pipeline_map/
git commit -m "Add pipeline map POJOs (#1305)"
```

---

### Task 6: RunMode and InstrumentationRunEngine

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/RunMode.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationRunEngine.java`

**Interfaces:**
- Consumes: `JIPipeProject`, `JIPipeGraphNode`, `JIPipeGraphRun`, `JIPipeGraphRunConfiguration` (existing core API classes), `InstrumentationJob` from Task 3
- Produces: `RunMode` enum, `InstrumentationRunEngine` with `runNode(project, node, mode, job)` and `runPipeline(project, mode, job)` methods. Extracts core logic from `JIPipeDesktopQuickRun`.

- [ ] **Step 1: Create RunMode enum**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/RunMode.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

/**
 * Defines how a pipeline or node should be run.
 */
public enum RunMode {
    /** Run to target node, cache only target output in memory. */
    UPDATE_CACHE,
    /** Run to target node, cache all intermediate outputs in memory. */
    CACHE_INTERMEDIATE,
    /** Run to predecessors of target (skip target itself), cache in memory. */
    UPDATE_PREDECESSOR_CACHE,
    /** Run entire pipeline, cache in memory only. */
    PIPELINE_CACHE,
    /** Run entire pipeline, write to output folder. */
    PIPELINE_FILESYSTEM,
    /** Run entire pipeline, discard results. */
    PIPELINE_DISCARD
}
```

- [ ] **Step 2: Create InstrumentationRunEngine**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationRunEngine.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeGraphRunConfiguration;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralDataApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;

import java.util.*;

/**
 * Core run engine that extracts the pipeline execution logic from
 * {@link org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun}.
 * This class is headless-compatible (no Swing dependencies).
 */
public class InstrumentationRunEngine {

    private final JIPipeProject project;
    private final JIPipeProgressInfo progressInfo;

    public InstrumentationRunEngine(JIPipeProject project, JIPipeProgressInfo progressInfo) {
        this.project = project;
        this.progressInfo = progressInfo != null ? progressInfo : new JIPipeProgressInfo();
    }

    /**
     * Runs the pipeline up to one or more target nodes with the given mode.
     */
    public void runNodes(List<JIPipeGraphNode> targetNodes, RunMode mode, InstrumentationJob job) {
        boolean storeToCache = mode != RunMode.PIPELINE_DISCARD;
        boolean storeToDisk = mode == RunMode.PIPELINE_FILESYSTEM;
        boolean storeIntermediate = mode == RunMode.CACHE_INTERMEDIATE;
        boolean excludeSelected = mode == RunMode.UPDATE_PREDECESSOR_CACHE;

        JIPipeGraphRunConfiguration config = new JIPipeGraphRunConfiguration();
        config.setOutputPath(project.newTemporaryDirectory());
        config.setLoadFromCache(true);
        config.setStoreToCache(storeToCache);
        config.setNumThreads(JIPipeRuntimeApplicationSettings_getDefaultQuickRunThreads());
        config.setStoreToDisk(storeToDisk);
        config.setSilent(true);
        config.setIgnoreDeactivatedInputs(true);

        JIPipeGraphRun run = new JIPipeGraphRun(project, config);
        run.setProgressInfo(progressInfo);

        List<JIPipeGraphNode> targetNodeCopies = new ArrayList<>();
        for (JIPipeGraphNode targetNode : targetNodes) {
            JIPipeGraphNode equivalentNode = run.getGraph().getEquivalentNode(targetNode);
            targetNodeCopies.add(equivalentNode);
            if (equivalentNode instanceof JIPipeAlgorithm) {
                ((JIPipeAlgorithm) equivalentNode).setEnabled(true);
            }
        }

        if (!storeIntermediate) {
            HashSet<UUID> disabled = new HashSet<>(run.getGraph().getGraphNodeUUIDs());
            for (JIPipeGraphNode targetNodeCopy : targetNodeCopies) {
                disabled.remove(targetNodeCopy.getUUIDInParentGraph());
            }
            if (excludeSelected) {
                for (JIPipeGraphNode targetNodeCopy : targetNodeCopies) {
                    for (JIPipeDataSlot inputSlot : targetNodeCopy.getInputSlots()) {
                        for (JIPipeDataSlot sourceSlot : run.getGraph().getInputIncomingSourceSlots(inputSlot)) {
                            JIPipeGraphNode node = sourceSlot.getNode();
                            disabled.remove(node.getUUIDInParentGraph());
                        }
                    }
                }
            }
            config.setDisableStoreToDiskNodes(disabled);
            config.setDisableStoreToCacheNodes(disabled);
        }

        // Find predecessors without cache
        Set<JIPipeGraphNode> predecessorAlgorithms = findPredecessorsWithoutCache(run, targetNodeCopies);
        if (!excludeSelected) {
            predecessorAlgorithms.addAll(targetNodeCopies);
        }
        for (JIPipeGraphNode node : run.getGraph().getGraphNodes()) {
            if (!predecessorAlgorithms.contains(node)) {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setSkipped(true);
                }
            }
        }
        if (excludeSelected && !storeIntermediate) {
            for (JIPipeGraphNode targetNodeCopy : targetNodeCopies) {
                for (JIPipeDataSlot inputSlot : targetNodeCopy.getInputSlots()) {
                    for (JIPipeDataSlot sourceSlot : run.getGraph().getInputIncomingSourceSlots(inputSlot)) {
                        JIPipeGraphNode node = sourceSlot.getNode();
                        if (node instanceof JIPipeAlgorithm) {
                            ((JIPipeAlgorithm) node).setSkipped(false);
                        }
                    }
                }
            }
        }

        // Remove target from cache
        if (config.isLoadFromCache()) {
            for (JIPipeGraphNode targetNode : targetNodes) {
                project.getCache().softClear(targetNode.getUUIDInParentGraph(), progressInfo);
            }
        }

        // Remove outdated cache
        if (JIPipeGeneralDataApplicationSettings.getInstance().isAutoRemoveOutdatedCachedData()) {
            project.getCache().clearOutdated(progressInfo.resolveAndLog("Remove outdated cache"));
        }

        job.setStatus(InstrumentationJobStatus.RUNNING);
        run.run();

        // Clear data
        for (JIPipeGraphNode node : run.getGraph().getGraphNodes()) {
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                inputSlot.clearData(false, progressInfo);
            }
            for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                outputSlot.clearData(false, progressInfo);
            }
        }
    }

    /**
     * Runs the entire pipeline with the given mode.
     */
    public void runPipeline(RunMode mode, InstrumentationJob job) {
        boolean storeToCache = mode != RunMode.PIPELINE_DISCARD;
        boolean storeToDisk = mode == RunMode.PIPELINE_FILESYSTEM;

        JIPipeGraphRunConfiguration config = new JIPipeGraphRunConfiguration();
        config.setOutputPath(project.newTemporaryDirectory());
        config.setLoadFromCache(true);
        config.setStoreToCache(storeToCache);
        config.setNumThreads(JIPipeRuntimeApplicationSettings_getDefaultQuickRunThreads());
        config.setStoreToDisk(storeToDisk);
        config.setSilent(true);

        JIPipeGraphRun run = new JIPipeGraphRun(project, config);
        run.setProgressInfo(progressInfo);

        job.setStatus(InstrumentationJobStatus.RUNNING);
        run.run();
    }

    private Set<JIPipeGraphNode> findPredecessorsWithoutCache(JIPipeGraphRun run, List<JIPipeGraphNode> targetNodeCopies) {
        Set<JIPipeGraphNode> predecessors = new HashSet<>();
        Set<JIPipeGraphNode> handledNodes = new HashSet<>();
        Stack<JIPipeGraphNode> stack = new Stack<>();
        for (JIPipeGraphNode targetNodeCopy : targetNodeCopies) {
            stack.push(targetNodeCopy);
        }
        JIPipeProgressInfo pi = new JIPipeProgressInfo();
        while (!stack.isEmpty()) {
            JIPipeGraphNode node = stack.pop();
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                Set<JIPipeDataSlot> inputIncomingSourceSlots = run.getGraph().getInputIncomingSourceSlotsNoTunnel(inputSlot);
                for (JIPipeDataSlot sourceSlot : inputIncomingSourceSlots) {
                    JIPipeGraphNode predecessorNode = sourceSlot.getNode();
                    if (handledNodes.contains(predecessorNode)) {
                        continue;
                    }
                    handledNodes.add(predecessorNode);
                    if (!predecessorNode.getInfo().isRunnable()) {
                        continue;
                    }
                    if (predecessorNode instanceof JIPipeAlgorithm) {
                        JIPipeRuntimePartition runtimePartition = run.getRuntimePartition(((JIPipeAlgorithm) predecessorNode).getRuntimePartition());
                        if (runtimePartition.getIterationMode() != JIPipeGraphWrapperAlgorithm.IterationMode.PassThrough) {
                            predecessors.add(predecessorNode);
                            stack.push(predecessorNode);
                            continue;
                        }
                    }
                    JIPipeGraphNode projectPredecessorNode = project.getGraph().getEquivalentNode(predecessorNode);
                    Map<String, JIPipeDataTable> slotMap = project.getCache().query(projectPredecessorNode, projectPredecessorNode.getUUIDInParentGraph(), pi);
                    if (slotMap.isEmpty()) {
                        predecessors.add(predecessorNode);
                        stack.push(predecessorNode);
                    } else {
                        for (Map.Entry<String, JIPipeDataTable> cacheEntry : slotMap.entrySet()) {
                            JIPipeDataSlot outputSlot = predecessorNode.getOutputSlot(cacheEntry.getKey());
                            outputSlot.addDataFromTable(cacheEntry.getValue(), pi);
                        }
                    }
                }
            }
        }
        for (JIPipeGraphNode targetNodeCopy : targetNodeCopies) {
            predecessors.remove(targetNodeCopy);
        }
        return predecessors;
    }

    private static int JIPipeRuntimeApplicationSettings_getDefaultQuickRunThreads() {
        return org.hkijena.jipipe.plugins.settings.application.JIPipeRuntimeApplicationSettings.getInstance().getDefaultQuickRunThreads();
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/RunMode.java jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationRunEngine.java
git commit -m "Add run mode enum and run engine (#1305)"
```

---

### Task 7: InstrumentationAPI

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationAPI.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationAPITest.java`

**Interfaces:**
- Consumes: `InstrumentationContext` (Task 3), `InstrumentationRunEngine` (Task 6), `InstrumentationJob` (Task 3), `RunMode` (Task 6), `PipelineMap` (Task 5), existing JIPipe core classes (`JIPipeProject`, `JIPipeGraph`, `JIPipeGraphNode`, `JIPipeDataSlot`, `JIPipeParameterAccess`, `JIPipeParameterTree`, `JIPipeNodeInfo`)
- Produces: `InstrumentationAPI` — stateless service with typed methods for all built-in operations. Each method takes an `InstrumentationContext`.

- [ ] **Step 1: Write the failing test**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationAPITest.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentationAPITest {

    @TempDir
    Path tempDir;

    @Test
    void queryCompartments_returnsEmptyArrayForNewProject() throws Exception {
        JIPipeProject project = new JIPipeProject();
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        InstrumentationContext ctx = new InstrumentationContext(project, "test", new org.hkijena.jipipe.api.JIPipeProgressInfo(), eventBus, jobManager);

        JsonNode result = InstrumentationAPI.queryCompartments(ctx);

        assertTrue(result.has("compartments"));
        assertEquals(1, result.get("compartments").size()); // default compartment
    }

    @Test
    void queryGraph_returnsNodesAndConnectionsArrays() throws Exception {
        JIPipeProject project = new JIPipeProject();
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        InstrumentationContext ctx = new InstrumentationContext(project, "test", new org.hkijena.jipipe.api.JIPipeProgressInfo(), eventBus, jobManager);

        JsonNode result = InstrumentationAPI.queryGraph(ctx, null);

        assertTrue(result.has("nodes"));
        assertTrue(result.has("connections"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=InstrumentationAPITest -q`
Expected: FAIL (InstrumentationAPI not found)

- [ ] **Step 3: Create InstrumentationAPI**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationAPI.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.instrumentation.events.*;
import org.hkijena.jipipe.api.instrumentation.pipeline_map.PipelineMap;
import org.hkijena.jipipe.api.instrumentation.pipeline_map.SegmentEdge;
import org.hkijena.jipipe.api.instrumentation.pipeline_map.SegmentNode;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stateless service providing typed Java methods for all built-in instrumentation operations.
 * Both the AI agent and the WebSocket operation wrappers delegate to this class.
 */
public final class InstrumentationAPI {

    private static final ObjectMapper mapper = new ObjectMapper();

    private InstrumentationAPI() {
    }

    // ── Queries ──

    public static JsonNode queryCompartments(InstrumentationContext ctx) {
        ObjectNode data = mapper.createObjectNode();
        ArrayNode compartments = data.putArray("compartments");
        JIPipeProject project = ctx.getProject();
        if (project == null) return data;
        for (var entry : project.getCompartments().entrySet()) {
            ObjectNode comp = compartments.addObject();
            comp.put("id", entry.getKey().toString());
            comp.put("name", entry.getValue().getName());
            comp.put("nodeCount", project.getGraph().getNodesWithinCompartment(entry.getKey()).size());
        }
        return data;
    }

    public static JsonNode queryGraph(InstrumentationContext ctx, String compartmentIdStr) {
        ObjectNode data = mapper.createObjectNode();
        JIPipeProject project = ctx.getProject();
        if (project == null) {
            data.putArray("nodes");
            data.putArray("connections");
            return data;
        }
        JIPipeGraph graph = project.getGraph();
        UUID compartmentFilter = null;
        if (compartmentIdStr != null && !compartmentIdStr.isBlank()) {
            try {
                compartmentFilter = UUID.fromString(compartmentIdStr);
            } catch (IllegalArgumentException e) {
                var comp = project.findCompartment(compartmentIdStr);
                if (comp != null) {
                    compartmentFilter = comp.getProjectCompartmentUUID();
                }
            }
        }
        if (compartmentFilter != null) {
            data.put("compartmentId", compartmentFilter.toString());
        }
        ArrayNode nodes = data.putArray("nodes");
        ArrayNode connections = data.putArray("connections");
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            UUID nodeCompartment = graph.getCompartmentUUIDOf(node);
            if (compartmentFilter != null && !compartmentFilter.equals(nodeCompartment)) continue;
            UUID uuid = graph.getUUIDOf(node);
            String alias = graph.getAliasIdOf(node);
            Point loc = node.getNodeUILocationWithin(nodeCompartment != null ? nodeCompartment.toString() : "");
            ObjectNode n = nodes.addObject();
            n.put("id", alias != null ? alias : (uuid != null ? uuid.toString() : "?"));
            n.put("uuid", uuid != null ? uuid.toString() : null);
            n.put("name", node.getName());
            n.put("typeId", node.getInfo() != null ? node.getInfo().getId() : "?");
            n.put("x", loc != null ? loc.x : 0);
            n.put("y", loc != null ? loc.y : 0);
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : graph.getSlotEdges()) {
            JIPipeDataSlot source = edge.getKey();
            JIPipeDataSlot target = edge.getValue();
            UUID sourceComp = graph.getCompartmentUUIDOf(source.getNode());
            UUID targetComp = graph.getCompartmentUUIDOf(target.getNode());
            if (compartmentFilter != null && !compartmentFilter.equals(sourceComp) && !compartmentFilter.equals(targetComp))
                continue;
            ObjectNode c = connections.addObject();
            c.put("source", graph.getAliasIdOf(source.getNode()));
            c.put("sourceSlot", source.getName());
            c.put("target", graph.getAliasIdOf(target.getNode()));
            c.put("targetSlot", target.getName());
        }
        return data;
    }

    public static JsonNode queryNode(InstrumentationContext ctx, String nodeIdStr) {
        JIPipeProject project = ctx.getProject();
        if (project == null) return mapper.createObjectNode().put("error", "No project loaded");
        JIPipeGraph graph = project.getGraph();
        JIPipeGraphNode node = graph.findNode(nodeIdStr);
        if (node == null) return mapper.createObjectNode().put("error", "Node not found: " + nodeIdStr);
        ObjectNode data = mapper.createObjectNode();
        data.put("id", graph.getAliasIdOf(node));
        data.put("uuid", graph.getUUIDOf(node) != null ? graph.getUUIDOf(node).toString() : null);
        data.put("name", node.getName());
        data.put("typeId", node.getInfo() != null ? node.getInfo().getId() : "?");
        UUID compartment = graph.getCompartmentUUIDOf(node);
        data.put("compartmentId", compartment != null ? compartment.toString() : null);
        ObjectNode params = data.putObject("parameters");
        for (JIPipeParameterAccess access : JIPipeParameterTree.getParameters(node).values()) {
            String key = access.getKey();
            if (key == null || key.startsWith("jipipe:") || key.startsWith("jipipe-ui:")) continue;
            Object value = access.get(Object.class);
            params.put(key, value != null ? value.toString() : "null");
        }
        ArrayNode inputSlots = data.putArray("inputSlots");
        for (JIPipeDataSlot slot : node.getInputSlots()) {
            ObjectNode s = inputSlots.addObject();
            s.put("name", slot.getName());
            s.put("dataType", JIPipeData.getNameOf(slot.getAcceptedDataType()));
        }
        ArrayNode outputSlots = data.putArray("outputSlots");
        for (JIPipeDataSlot slot : node.getOutputSlots()) {
            ObjectNode s = outputSlots.addObject();
            s.put("name", slot.getName());
            s.put("dataType", JIPipeData.getNameOf(slot.getAcceptedDataType()));
        }
        return data;
    }

    public static JsonNode queryData(InstrumentationContext ctx, String nodeIdStr, String slotName, int limit) {
        JIPipeProject project = ctx.getProject();
        if (project == null) return mapper.createObjectNode().put("error", "No project loaded");
        JIPipeGraph graph = project.getGraph();
        JIPipeGraphNode node = graph.findNode(nodeIdStr);
        if (node == null) return mapper.createObjectNode().put("error", "Node not found: " + nodeIdStr);
        JIPipeDataSlot slot = node.getOutputSlot(slotName);
        if (slot == null) slot = node.getInputSlot(slotName);
        if (slot == null) return mapper.createObjectNode().put("error", "Slot not found: " + slotName);
        ObjectNode data = mapper.createObjectNode();
        data.put("nodeId", graph.getAliasIdOf(node));
        data.put("slotName", slot.getName());
        data.put("rowCount", slot.getRowCount());
        ArrayNode columns = data.putArray("columns");
        columns.add("Name");
        for (String col : slot.getTextAnnotationColumnNames()) columns.add(col);
        int maxRows = Math.min(slot.getRowCount(), limit);
        ArrayNode rows = data.putArray("rows");
        for (int row = 0; row < maxRows; row++) {
            ObjectNode r = rows.addObject();
            try {
                String strRep = slot.getDataItemStore(row).getStringRepresentation();
                r.put("Name", strRep != null ? strRep : "row-" + row);
            } catch (Exception e) {
                r.put("Name", "row-" + row);
            }
            for (String col : slot.getTextAnnotationColumnNames()) {
                var ann = slot.getTextAnnotationMap(row).get(col);
                r.put(col, ann != null ? ann.getValue() : "");
            }
        }
        return data;
    }

    // ── Execution (async) ──

    public static InstrumentationJob runNode(InstrumentationContext ctx, String nodeId, RunMode mode) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        JIPipeGraphNode node = project.getGraph().findNode(nodeId);
        if (node == null) throw new IllegalArgumentException("No node found for ID '" + nodeId + "'");
        InstrumentationJob job = ctx.getJobManager().createJob("run_node", "Run: " + node.getName());
        Thread runThread = new Thread(() -> {
            try {
                InstrumentationRunEngine engine = new InstrumentationRunEngine(project, ctx.getProgressInfo());
                engine.runNodes(List.of(node), mode, job);
                job.complete(null);
            } catch (Exception e) {
                job.fail(e.getMessage());
            }
        }, "instrumentation-run-" + job.getId());
        runThread.setDaemon(true);
        runThread.start();
        return job;
    }

    public static InstrumentationJob runCompartment(InstrumentationContext ctx, String compartmentId) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        var compartment = project.findCompartment(compartmentId);
        if (compartment == null) throw new IllegalArgumentException("Compartment not found: " + compartmentId);
        List<JIPipeGraphNode> outputNodes = project.getGraph().getNodesWithinCompartment(compartment.getProjectCompartmentUUID())
                .stream().filter(n -> n.getInfo() != null && n.getInfo().isRunnable()).toList();
        InstrumentationJob job = ctx.getJobManager().createJob("run_compartment", "Run compartment: " + compartment.getName());
        Thread runThread = new Thread(() -> {
            try {
                InstrumentationRunEngine engine = new InstrumentationRunEngine(project, ctx.getProgressInfo());
                engine.runNodes(outputNodes, RunMode.PIPELINE_FILESYSTEM, job);
                job.complete(null);
            } catch (Exception e) {
                job.fail(e.getMessage());
            }
        }, "instrumentation-run-" + job.getId());
        runThread.setDaemon(true);
        runThread.start();
        return job;
    }

    public static InstrumentationJob runPipeline(InstrumentationContext ctx, RunMode mode) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        InstrumentationJob job = ctx.getJobManager().createJob("run_pipeline", "Run pipeline");
        Thread runThread = new Thread(() -> {
            try {
                InstrumentationRunEngine engine = new InstrumentationRunEngine(project, ctx.getProgressInfo());
                engine.runPipeline(mode, job);
                job.complete(null);
            } catch (Exception e) {
                job.fail(e.getMessage());
            }
        }, "instrumentation-run-" + job.getId());
        runThread.setDaemon(true);
        runThread.start();
        return job;
    }

    // ── Modifications ──

    public static String addNode(InstrumentationContext ctx, String compartmentId, String nodeTypeId, int x, int y) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(nodeTypeId);
        if (info == null) throw new IllegalArgumentException("Unknown node type: " + nodeTypeId);
        JIPipeGraphNode node = info.newInstance();
        UUID compartmentUuid = resolveCompartmentUUID(ctx, compartmentId);
        graph.insertNode(node, compartmentUuid, new Point(x, y));
        String alias = graph.getAliasIdOf(node);
        ctx.getEventBus().publish(new NodeAddedEvent(ctx.getProjectId(), compartmentUuid != null ? compartmentUuid.toString() : null, alias, nodeTypeId, node.getName()));
        return alias;
    }

    public static void removeNode(InstrumentationContext ctx, String nodeId) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode node = graph.findNode(nodeId);
        if (node == null) throw new IllegalArgumentException("Node not found: " + nodeId);
        graph.removeNode(node);
        ctx.getEventBus().publish(new NodeRemovedEvent(ctx.getProjectId(), nodeId));
    }

    public static void addConnection(InstrumentationContext ctx, String source, String sourceSlot, String target, String targetSlot) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode sourceNode = graph.findNode(source);
        JIPipeGraphNode targetNode = graph.findNode(target);
        if (sourceNode == null) throw new IllegalArgumentException("Source node not found: " + source);
        if (targetNode == null) throw new IllegalArgumentException("Target node not found: " + target);
        graph.connectTo(sourceNode.getOutputSlot(sourceSlot), targetNode.getInputSlot(targetSlot));
        ctx.getEventBus().publish(new ConnectionChangedEvent(ctx.getProjectId(), source, sourceSlot, target, targetSlot, "added"));
    }

    public static void removeConnection(InstrumentationContext ctx, String source, String sourceSlot, String target, String targetSlot) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode sourceNode = graph.findNode(source);
        JIPipeGraphNode targetNode = graph.findNode(target);
        if (sourceNode == null) throw new IllegalArgumentException("Source node not found: " + source);
        if (targetNode == null) throw new IllegalArgumentException("Target node not found: " + target);
        graph.disconnect(sourceNode.getOutputSlot(sourceSlot), targetNode.getInputSlot(targetSlot));
        ctx.getEventBus().publish(new ConnectionChangedEvent(ctx.getProjectId(), source, sourceSlot, target, targetSlot, "removed"));
    }

    public static void setParameter(InstrumentationContext ctx, String nodeId, String key, String value) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode node = graph.findNode(nodeId);
        if (node == null) throw new IllegalArgumentException("Node not found: " + nodeId);
        JIPipeParameterAccess access = JIPipeParameterTree.getParameters(node).get(key);
        if (access == null) throw new IllegalArgumentException("Parameter not found: " + key);
        access.set(value);
        ctx.getEventBus().publish(new ParameterChangedEvent(ctx.getProjectId(), nodeId, key, value));
    }

    public static String addCompartment(InstrumentationContext ctx, String name) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        var compartment = project.addCompartment(name);
        ctx.getEventBus().publish(new CompartmentChangedEvent(ctx.getProjectId(), compartment.getProjectCompartmentUUID().toString(), name, "added"));
        return compartment.getProjectCompartmentUUID().toString();
    }

    public static void setProjectMetadata(InstrumentationContext ctx, String name, String description) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        if (name != null) project.setName(name);
        if (description != null) project.setDescription(description);
    }

    // ── Pipeline Map ──

    public static JsonNode getPipelineMap(InstrumentationContext ctx) {
        JIPipeProject project = ctx.getProject();
        if (project == null) return mapper.createObjectNode();
        // Build a simple flat map: one segment per compartment
        SegmentNode root = new SegmentNode("root", project.getName() != null ? project.getName() : "Project");
        for (var entry : project.getCompartments().entrySet()) {
            String compId = entry.getKey().toString();
            String compName = entry.getValue().getName();
            SegmentNode segment = new SegmentNode(compId, compName);
            segment.setCompartmentId(compId);
            List<JIPipeGraphNode> nodes = project.getGraph().getNodesWithinCompartment(entry.getKey());
            for (JIPipeGraphNode node : nodes) {
                String alias = project.getGraph().getAliasIdOf(node);
                segment.getNodeIds().add(alias != null ? alias : node.getName());
            }
            segment.setSummary(nodes.size() + " nodes");
            if (root.getChildren() == null) root.setChildren(new java.util.ArrayList<>());
            root.getChildren().add(segment);
        }
        PipelineMap map = new PipelineMap(root);
        return mapper.valueToTree(map);
    }

    public static JsonNode getSegmentDetail(InstrumentationContext ctx, String segmentId) {
        JIPipeProject project = ctx.getProject();
        if (project == null) return mapper.createObjectNode().put("error", "No project loaded");
        // Segment ID = compartment UUID
        try {
            UUID compUuid = UUID.fromString(segmentId);
            var compartment = project.getCompartments().get(compUuid);
            if (compartment == null) return mapper.createObjectNode().put("error", "Segment not found: " + segmentId);
            ObjectNode data = mapper.createObjectNode();
            data.put("segmentId", segmentId);
            data.put("name", compartment.getName());
            ArrayNode nodes = data.putArray("nodes");
            for (JIPipeGraphNode node : project.getGraph().getNodesWithinCompartment(compUuid)) {
                ObjectNode n = nodes.addObject();
                n.put("id", project.getGraph().getAliasIdOf(node));
                n.put("name", node.getName());
                n.put("typeId", node.getInfo() != null ? node.getInfo().getId() : "?");
            }
            return data;
        } catch (IllegalArgumentException e) {
            return mapper.createObjectNode().put("error", "Invalid segment ID: " + segmentId);
        }
    }

    // ── Node Search ──

    public static JsonNode searchNodes(InstrumentationContext ctx, String query, int limit) {
        ObjectNode data = mapper.createObjectNode();
        ArrayNode results = data.putArray("results");
        var nodeInfos = JIPipe.getNodes().getInfos();
        String lowerQuery = query.toLowerCase();
        int count = 0;
        for (JIPipeNodeInfo info : nodeInfos) {
            if (count >= limit) break;
            String name = info.getName() != null ? info.getName() : info.getId();
            String desc = info.getDescription() != null ? info.getDescription().toPlainText() : "";
            if (name.toLowerCase().contains(lowerQuery) || desc.toLowerCase().contains(lowerQuery) || info.getId().toLowerCase().contains(lowerQuery)) {
                ObjectNode r = results.addObject();
                r.put("id", info.getId());
                r.put("name", name);
                r.put("description", desc);
                count++;
            }
        }
        return data;
    }

    // ── Helpers ──

    private static UUID resolveCompartmentUUID(InstrumentationContext ctx, String compartmentId) {
        if (compartmentId == null || compartmentId.isBlank()) return null;
        try {
            return UUID.fromString(compartmentId);
        } catch (IllegalArgumentException e) {
            var comp = ctx.getProject().findCompartment(compartmentId);
            return comp != null ? comp.getProjectCompartmentUUID() : null;
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=InstrumentationAPITest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationAPI.java jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationAPITest.java
git commit -m "Add instrumentation API facade (#1305)"
```

---

### Task 8: Built-in Operations

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/QueryOperations.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/ExecutionOperations.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/ModificationOperations.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/PipelineMapOperations.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/ProjectOperations.java`

**Interfaces:**
- Consumes: `InstrumentationAPI` (Task 7), `InstrumentationOperation` (Task 1), `AsyncInstrumentationOperation` (Task 4)
- Produces: 5 operation classes that wrap API methods as `InstrumentationOperation` instances. Each provides `getId()`, `getDescription()`, and `execute()` (or `executeAsync()`).

- [ ] **Step 1: Create QueryOperations**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/QueryOperations.java`:

```java
package org.hkijena.jipipe.api.instrumentation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.api.instrumentation.InstrumentationAPI;
import org.hkijena.jipipe.api.instrumentation.InstrumentationContext;
import org.hkijena.jipipe.api.instrumentation.InstrumentationOperation;

import java.util.Map;

/**
 * Built-in query operations for the instrumentation layer.
 * Each operation wraps a {@link InstrumentationAPI} method.
 */
public class QueryOperations {

    public static class QueryCompartments implements InstrumentationOperation {
        @Override public String getId() { return "query_compartments"; }
        @Override public String getDescription() { return "List all compartments with node counts"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            return InstrumentationAPI.queryCompartments(ctx);
        }
    }

    public static class QueryGraph implements InstrumentationOperation {
        @Override public String getId() { return "query_graph"; }
        @Override public String getDescription() { return "Get nodes and connections for a compartment (or all if not specified)"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String compartmentId = params.has("compartmentId") ? params.get("compartmentId").asText() : null;
            return InstrumentationAPI.queryGraph(ctx, compartmentId);
        }
    }

    public static class QueryNode implements InstrumentationOperation {
        @Override public String getId() { return "query_node"; }
        @Override public String getDescription() { return "Get detailed info for a single node including parameters and slots"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            return InstrumentationAPI.queryNode(ctx, params.get("nodeId").asText());
        }
    }

    public static class QueryData implements InstrumentationOperation {
        @Override public String getId() { return "query_data"; }
        @Override public String getDescription() { return "Get data table for a node's slot"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String nodeId = params.get("nodeId").asText();
            String slotName = params.get("slotName").asText();
            int limit = params.has("limit") ? params.get("limit").asInt() : 50;
            return InstrumentationAPI.queryData(ctx, nodeId, slotName, limit);
        }
    }
}
```

- [ ] **Step 2: Create ExecutionOperations**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/ExecutionOperations.java`:

```java
package org.hkijena.jipipe.api.instrumentation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.instrumentation.*;

/**
 * Built-in async execution operations.
 */
public class ExecutionOperations {

    public static class RunNode implements AsyncInstrumentationOperation {
        @Override public String getId() { return "run_node"; }
        @Override public String getDescription() { return "Run pipeline up to a node with specified mode"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) throws Exception {
            InstrumentationJob job = executeAsync(ctx, params);
            return new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("jobId", job.getId());
        }
        @Override public InstrumentationJob executeAsync(InstrumentationContext ctx, JsonNode params) {
            String nodeId = params.get("nodeId").asText();
            RunMode mode = RunMode.valueOf(params.has("mode") ? params.get("mode").asText() : "UPDATE_CACHE");
            return InstrumentationAPI.runNode(ctx, nodeId, mode);
        }
    }

    public static class RunCompartment implements AsyncInstrumentationOperation {
        @Override public String getId() { return "run_compartment"; }
        @Override public String getDescription() { return "Run all output nodes of a compartment"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) throws Exception {
            InstrumentationJob job = executeAsync(ctx, params);
            return new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("jobId", job.getId());
        }
        @Override public InstrumentationJob executeAsync(InstrumentationContext ctx, JsonNode params) {
            return InstrumentationAPI.runCompartment(ctx, params.get("compartmentId").asText());
        }
    }

    public static class RunPipeline implements AsyncInstrumentationOperation {
        @Override public String getId() { return "run_pipeline"; }
        @Override public String getDescription() { return "Run the entire pipeline"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) throws Exception {
            InstrumentationJob job = executeAsync(ctx, params);
            return new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("jobId", job.getId());
        }
        @Override public InstrumentationJob executeAsync(InstrumentationContext ctx, JsonNode params) {
            RunMode mode = RunMode.valueOf(params.has("mode") ? params.get("mode").asText() : "PIPELINE_CACHE");
            return InstrumentationAPI.runPipeline(ctx, mode);
        }
    }
}
```

- [ ] **Step 3: Create ModificationOperations**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/ModificationOperations.java`:

```java
package org.hkijena.jipipe.api.instrumentation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.instrumentation.InstrumentationAPI;
import org.hkijena.jipipe.api.instrumentation.InstrumentationContext;
import org.hkijena.jipipe.api.instrumentation.InstrumentationOperation;

/**
 * Built-in modification operations.
 */
public class ModificationOperations {

    public static class AddNode implements InstrumentationOperation {
        @Override public String getId() { return "add_node"; }
        @Override public String getDescription() { return "Add a node to a compartment"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String compartmentId = params.has("compartmentId") ? params.get("compartmentId").asText() : null;
            String nodeTypeId = params.get("nodeTypeId").asText();
            int x = params.has("x") ? params.get("x").asInt() : 0;
            int y = params.has("y") ? params.get("y").asInt() : 0;
            String alias = InstrumentationAPI.addNode(ctx, compartmentId, nodeTypeId, x, y);
            ObjectNode result = new ObjectMapper().createObjectNode();
            result.put("nodeId", alias);
            return result;
        }
    }

    public static class RemoveNode implements InstrumentationOperation {
        @Override public String getId() { return "remove_node"; }
        @Override public String getDescription() { return "Remove a node from the graph"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            InstrumentationAPI.removeNode(ctx, params.get("nodeId").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class AddConnection implements InstrumentationOperation {
        @Override public String getId() { return "add_connection"; }
        @Override public String getDescription() { return "Connect two slots"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            InstrumentationAPI.addConnection(ctx,
                    params.get("source").asText(), params.get("sourceSlot").asText(),
                    params.get("target").asText(), params.get("targetSlot").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class RemoveConnection implements InstrumentationOperation {
        @Override public String getId() { return "remove_connection"; }
        @Override public String getDescription() { return "Disconnect two slots"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            InstrumentationAPI.removeConnection(ctx,
                    params.get("source").asText(), params.get("sourceSlot").asText(),
                    params.get("target").asText(), params.get("targetSlot").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class SetParameter implements InstrumentationOperation {
        @Override public String getId() { return "set_parameter"; }
        @Override public String getDescription() { return "Set a parameter on a node"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            InstrumentationAPI.setParameter(ctx,
                    params.get("nodeId").asText(), params.get("key").asText(), params.get("value").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class SetProjectMetadata implements InstrumentationOperation {
        @Override public String getId() { return "set_project_metadata"; }
        @Override public String getDescription() { return "Set project name and/or description"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String name = params.has("name") ? params.get("name").asText() : null;
            String description = params.has("description") ? params.get("description").asText() : null;
            InstrumentationAPI.setProjectMetadata(ctx, name, description);
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class AddCompartment implements InstrumentationOperation {
        @Override public String getId() { return "add_compartment"; }
        @Override public String getDescription() { return "Create a new compartment"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String id = InstrumentationAPI.addCompartment(ctx, params.get("name").asText());
            return new ObjectMapper().createObjectNode().put("compartmentId", id);
        }
    }

    public static class RenameCompartment implements InstrumentationOperation {
        @Override public String getId() { return "rename_compartment"; }
        @Override public String getDescription() { return "Rename a compartment"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            // Delegates to project API
            var project = ctx.getProject();
            if (project == null) throw new IllegalStateException("No project available");
            String compartmentId = params.get("compartmentId").asText();
            String name = params.get("name").asText();
            var comp = project.findCompartment(compartmentId);
            if (comp == null) throw new IllegalArgumentException("Compartment not found: " + compartmentId);
            comp.setName(name);
            ctx.getEventBus().publish(new org.hkijena.jipipe.api.instrumentation.events.CompartmentChangedEvent(
                    ctx.getProjectId(), comp.getProjectCompartmentUUID().toString(), name, "renamed"));
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }
}
```

- [ ] **Step 4: Create PipelineMapOperations**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/PipelineMapOperations.java`:

```java
package org.hkijena.jipipe.api.instrumentation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.instrumentation.InstrumentationAPI;
import org.hkijena.jipipe.api.instrumentation.InstrumentationContext;
import org.hkijena.jipipe.api.instrumentation.InstrumentationOperation;

/**
 * Built-in pipeline map and search operations.
 */
public class PipelineMapOperations {

    public static class GetPipelineMap implements InstrumentationOperation {
        @Override public String getId() { return "get_pipeline_map"; }
        @Override public String getDescription() { return "Get a hierarchical map of the pipeline structure"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            return InstrumentationAPI.getPipelineMap(ctx);
        }
    }

    public static class GetSegmentDetail implements InstrumentationOperation {
        @Override public String getId() { return "get_segment_detail"; }
        @Override public String getDescription() { return "Get detail for a specific pipeline segment"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            return InstrumentationAPI.getSegmentDetail(ctx, params.get("segmentId").asText());
        }
    }

    public static class SearchNodes implements InstrumentationOperation {
        @Override public String getId() { return "search_nodes"; }
        @Override public String getDescription() { return "Search available JIPipe node types"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String query = params.get("query").asText();
            int limit = params.has("limit") ? params.get("limit").asInt() : 20;
            return InstrumentationAPI.searchNodes(ctx, query, limit);
        }
    }
}
```

- [ ] **Step 5: Create ProjectOperations**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/ProjectOperations.java`:

```java
package org.hkijena.jipipe.api.instrumentation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.instrumentation.InstrumentationContext;
import org.hkijena.jipipe.api.instrumentation.InstrumentationOperation;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;

/**
 * Built-in project management operations.
 * These are handled by the server directly (not via InstrumentationAPI)
 * because they require access to the desktop window system.
 */
public class ProjectOperations {

    public static class ListProjects implements InstrumentationOperation {
        @Override public String getId() { return "list_projects"; }
        @Override public String getDescription() { return "List all open projects"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode data = mapper.createObjectNode();
            ArrayNode projects = data.putArray("projects");
            for (var window : JIPipeDesktopProjectWindow.getOpenWindows()) {
                ObjectNode p = projects.addObject();
                p.put("id", window.getWindowId());
                p.put("name", window.getProject() != null ? window.getProject().getName() : "Untitled");
                p.put("path", window.getProject() != null && window.getProject().getProjectDirectory() != null
                        ? window.getProject().getProjectDirectory().toString() : null);
                p.put("isOpen", true);
            }
            return data;
        }
    }

    public static class ListOperations implements InstrumentationOperation {
        @Override public String getId() { return "list_operations"; }
        @Override public String getDescription() { return "List all available operations"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode data = mapper.createObjectNode();
            ArrayNode ops = data.putArray("operations");
            var registry = org.hkijena.jipipe.JIPipe.getInstance().getInstrumentation().getOperationRegistry();
            for (var entry : registry.getAll().entrySet()) {
                ObjectNode op = ops.addObject();
                op.put("id", entry.getKey());
                op.put("description", entry.getValue().getDescription());
                op.put("async", entry.getValue() instanceof org.hkijena.jipipe.api.instrumentation.AsyncInstrumentationOperation);
            }
            return data;
        }
    }
}
```

- [ ] **Step 6: Verify compilation**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/operations/
git commit -m "Add built-in instrumentation operations (#1305)"
```

---

### Task 9: InstrumentationPlugin

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/instrumentation/InstrumentationPlugin.java`

**Interfaces:**
- Consumes: All operation classes from Task 8, `JIPipePrepackagedDefaultJavaPlugin` (existing), `registerInstrumentationOperation()` from Task 1
- Produces: `InstrumentationPlugin` — a SciJava `@Plugin` annotated class that registers all built-in operations during `register()`.

- [ ] **Step 1: Create InstrumentationPlugin**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/instrumentation/InstrumentationPlugin.java`:

```java
package org.hkijena.jipipe.plugins.instrumentation;

import org.hkijena.jipipe.JIPipeDefaultJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.instrumentation.operations.*;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.utils.HTMLText;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.hkijena.jipipe.JIPipeJavaPlugin;

import java.util.Collections;
import java.util.Set;

/**
 * Registers the built-in instrumentation operations.
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class InstrumentationPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    @Override
    public String getName() {
        return "Instrumentation";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides the instrumentation WebSocket API for external automation and diagnostics.");
    }

    @Override
    public Set<org.hkijena.jipipe.JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public void register(org.hkijena.jipipe.api.service.JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        // Project operations
        registerInstrumentationOperation("list_projects", new ProjectOperations.ListProjects());
        registerInstrumentationOperation("list_operations", new ProjectOperations.ListOperations());

        // Query operations
        registerInstrumentationOperation("query_compartments", new QueryOperations.QueryCompartments());
        registerInstrumentationOperation("query_graph", new QueryOperations.QueryGraph());
        registerInstrumentationOperation("query_node", new QueryOperations.QueryNode());
        registerInstrumentationOperation("query_data", new QueryOperations.QueryData());

        // Execution operations
        registerInstrumentationOperation("run_node", new ExecutionOperations.RunNode());
        registerInstrumentationOperation("run_compartment", new ExecutionOperations.RunCompartment());
        registerInstrumentationOperation("run_pipeline", new ExecutionOperations.RunPipeline());

        // Modification operations
        registerInstrumentationOperation("add_node", new ModificationOperations.AddNode());
        registerInstrumentationOperation("remove_node", new ModificationOperations.RemoveNode());
        registerInstrumentationOperation("add_connection", new ModificationOperations.AddConnection());
        registerInstrumentationOperation("remove_connection", new ModificationOperations.RemoveConnection());
        registerInstrumentationOperation("set_parameter", new ModificationOperations.SetParameter());
        registerInstrumentationOperation("set_project_metadata", new ModificationOperations.SetProjectMetadata());
        registerInstrumentationOperation("add_compartment", new ModificationOperations.AddCompartment());
        registerInstrumentationOperation("rename_compartment", new ModificationOperations.RenameCompartment());

        // Pipeline map operations
        registerInstrumentationOperation("get_pipeline_map", new PipelineMapOperations.GetPipelineMap());
        registerInstrumentationOperation("get_segment_detail", new PipelineMapOperations.GetSegmentDetail());
        registerInstrumentationOperation("search_nodes", new PipelineMapOperations.SearchNodes());
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/instrumentation/InstrumentationPlugin.java
git commit -m "Add instrumentation plugin (#1305)"
```

---

### Task 10: WebSocket Protocol and Server

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationProtocol.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationServer.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationServerTest.java`

**Interfaces:**
- Consumes: `InstrumentationOperationRegistry` (Task 1/4), `InstrumentationEventBus` (Task 2), `InstrumentationJobManager` (Task 3), `InstrumentationContext` (Task 3), `AsyncInstrumentationOperation` (Task 4), `JIPipeDesktopProjectWindow` (existing)
- Produces: `InstrumentationProtocol` (message type constants), `InstrumentationServer` (WebSocket server that dispatches commands to the registry and broadcasts events)

- [ ] **Step 1: Create InstrumentationProtocol**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationProtocol.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

/**
 * Message type constants for the instrumentation WebSocket protocol.
 * All messages are JSON objects with a "type" string field.
 */
public final class InstrumentationProtocol {

    private InstrumentationProtocol() {
    }

    // ── Event types (server -> client) ──
    public static final String EVENT_PROJECT_LIST = "project_list";
    public static final String EVENT_PROJECT_CHANGED = "project_changed";
    public static final String EVENT_OPERATION_RESULT = "operation_result";
    public static final String EVENT_OPERATION_LIST = "operation_list";
    public static final String EVENT_NODE_ADDED = "node_added";
    public static final String EVENT_NODE_REMOVED = "node_removed";
    public static final String EVENT_CONNECTION_CHANGED = "connection_changed";
    public static final String EVENT_PARAMETER_CHANGED = "parameter_changed";
    public static final String EVENT_COMPARTMENT_CHANGED = "compartment_changed";
    public static final String EVENT_JOB_STARTED = "job_started";
    public static final String EVENT_JOB_PROGRESS = "job_progress";
    public static final String EVENT_JOB_COMPLETED = "job_completed";
    public static final String EVENT_ERROR = "error";

    // ── Command prefixes ──
    // Commands are dispatched to the operation registry by their "type" field.
    // Special commands handled directly by the server:
    public static final String CMD_SELECT_PROJECT = "select_project";
    public static final String CMD_GET_JOB_STATUS = "get_job_status";
    public static final String CMD_CANCEL_JOB = "cancel_job";
}
```

- [ ] **Step 2: Write the failing test**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationServerTest.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class InstrumentationServerTest {

    @Test
    void serverStartsAndAcceptsConnection() throws Exception {
        InstrumentationServer server = new InstrumentationServer(0);
        server.start();
        Thread.sleep(200);

        int port = server.getAddress().getPort();
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        WebSocketClient client = new WebSocketClient(new URI("ws://127.0.0.1:" + port)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                connected.countDown();
            }

            @Override
            public void onMessage(String message) {
                receivedMessage.set(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
            }

            @Override
            public void onError(Exception ex) {
            }
        };
        client.connect();
        assertTrue(connected.await(5, TimeUnit.SECONDS));

        client.close();
        server.stop(0);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=InstrumentationServerTest -q`
Expected: FAIL (InstrumentationServer not found)

- [ ] **Step 4: Create InstrumentationServer**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationServer.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.instrumentation.events.*;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket server that exposes instrumentation operations to external tools.
 * Binds to 127.0.0.1 only. Dispatches commands to the operation registry
 * and broadcasts events to all connected clients.
 */
public class InstrumentationServer extends WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentationServer.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile JIPipeProject selectedProject;
    private volatile String selectedProjectId;
    private volatile InstrumentationEventBus eventBus;
    private volatile InstrumentationJobManager jobManager;
    private final List<Runnable> activeSubscriptions = new ArrayList<>();

    public InstrumentationServer(int port) {
        super(new InetSocketAddress(
                System.getProperty("jipipe.instrumentation.bind", "127.0.0.1"), port));
        setDaemon(true);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("Instrumentation client connected: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("Instrumentation client disconnected: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonNode msg = mapper.readTree(message);
            String type = msg.has("type") ? msg.get("type").asText() : "";
            String requestId = msg.has("requestId") ? msg.get("requestId").asText() : null;

            if (InstrumentationProtocol.CMD_SELECT_PROJECT.equals(type)) {
                handleSelectProject(conn, msg, requestId);
            } else if (InstrumentationProtocol.CMD_GET_JOB_STATUS.equals(type)) {
                handleGetJobStatus(conn, msg, requestId);
            } else if (InstrumentationProtocol.CMD_CANCEL_JOB.equals(type)) {
                handleCancelJob(conn, msg, requestId);
            } else {
                handleOperation(conn, type, msg, requestId);
            }
        } catch (Exception e) {
            logger.error("Error processing message", e);
            sendError(conn, null, e.getMessage());
        }
    }

    private void handleSelectProject(WebSocket conn, JsonNode msg, String requestId) {
        String projectId = msg.has("projectId") ? msg.get("projectId").asText() : null;
        unsubscribeFromProject();

        JIPipeDesktopProjectWindow window = findWindow(projectId);
        if (window == null) {
            sendError(conn, requestId, "Project not found: " + projectId);
            return;
        }
        selectedProject = window.getProject();
        selectedProjectId = projectId;
        eventBus = new InstrumentationEventBus();
        jobManager = new InstrumentationJobManager(eventBus, projectId);
        subscribeToProject();

        ObjectNode response = mapper.createObjectNode();
        response.put("type", InstrumentationProtocol.EVENT_PROJECT_CHANGED);
        response.put("projectId", projectId);
        response.put("name", selectedProject.getName());
        if (requestId != null) response.put("requestId", requestId);
        broadcast(response.toString());
    }

    private void handleGetJobStatus(WebSocket conn, JsonNode msg, String requestId) {
        String jobId = msg.get("jobId").asText();
        ObjectNode response = mapper.createObjectNode();
        response.put("type", InstrumentationProtocol.EVENT_OPERATION_RESULT);
        if (requestId != null) response.put("requestId", requestId);
        if (jobManager != null) {
            InstrumentationJob job = jobManager.getJob(jobId);
            if (job != null) {
                ObjectNode data = response.putObject("data");
                data.put("jobId", job.getId());
                data.put("status", job.getStatus().name());
                data.put("progress", job.getProgress());
                data.put("operation", job.getOperation());
                if (job.getError() != null) data.put("error", job.getError());
            } else {
                response.put("error", "Job not found: " + jobId);
            }
        } else {
            response.put("error", "No project selected");
        }
        conn.send(response.toString());
    }

    private void handleCancelJob(WebSocket conn, JsonNode msg, String requestId) {
        String jobId = msg.get("jobId").asText();
        ObjectNode response = mapper.createObjectNode();
        response.put("type", InstrumentationProtocol.EVENT_OPERATION_RESULT);
        if (requestId != null) response.put("requestId", requestId);
        if (jobManager != null) {
            boolean cancelled = jobManager.cancelJob(jobId);
            response.put("data", mapper.createObjectNode().put("success", cancelled));
        } else {
            response.put("error", "No project selected");
        }
        conn.send(response.toString());
    }

    private void handleOperation(WebSocket conn, String type, JsonNode msg, String requestId) {
        var registry = org.hkijena.jipipe.JIPipe.getInstance().getInstrumentation().getOperationRegistry();
        InstrumentationOperation op = registry.get(type);
        if (op == null) {
            sendError(conn, requestId, "Unknown command: " + type);
            return;
        }
        if (selectedProject == null && !isProjectManagementOp(type)) {
            sendError(conn, requestId, "No project selected. Call list_projects and select_project first.");
            return;
        }
        try {
            InstrumentationContext ctx = new InstrumentationContext(
                    selectedProject, selectedProjectId,
                    new JIPipeProgressInfo(),
                    eventBus != null ? eventBus : new InstrumentationEventBus(),
                    jobManager != null ? jobManager : new InstrumentationJobManager(new InstrumentationEventBus(), selectedProjectId));

            if (op instanceof AsyncInstrumentationOperation asyncOp) {
                InstrumentationJob job = asyncOp.executeAsync(ctx, msg);
                ObjectNode response = mapper.createObjectNode();
                response.put("type", InstrumentationProtocol.EVENT_JOB_STARTED);
                response.put("jobId", job.getId());
                response.put("operation", type);
                response.put("description", job.getDescription());
                if (requestId != null) response.put("requestId", requestId);
                conn.send(response.toString());
            } else {
                JsonNode result = op.execute(ctx, msg);
                ObjectNode response = mapper.createObjectNode();
                response.put("type", InstrumentationProtocol.EVENT_OPERATION_RESULT);
                if (requestId != null) response.put("requestId", requestId);
                response.set("data", result != null ? result : mapper.createObjectNode());
                conn.send(response.toString());
            }
        } catch (Exception e) {
            logger.error("Error executing operation: {}", type, e);
            sendError(conn, requestId, e.getMessage());
        }
    }

    private boolean isProjectManagementOp(String type) {
        return "list_projects".equals(type) || "list_operations".equals(type);
    }

    private void sendError(WebSocket conn, String requestId, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", InstrumentationProtocol.EVENT_ERROR);
        if (requestId != null) response.put("requestId", requestId);
        response.put("message", message);
        conn.send(response.toString());
    }

    private void subscribeToProject() {
        if (eventBus == null) return;
        // Subscribe to all event types and broadcast as JSON
        subscribe(NodeAddedEvent.class, e -> broadcastEvent("node_added", serializeEvent(e)));
        subscribe(NodeRemovedEvent.class, e -> broadcastEvent("node_removed", serializeEvent(e)));
        subscribe(ConnectionChangedEvent.class, e -> broadcastEvent("connection_changed", serializeEvent(e)));
        subscribe(ParameterChangedEvent.class, e -> broadcastEvent("parameter_changed", serializeEvent(e)));
        subscribe(CompartmentChangedEvent.class, e -> broadcastEvent("compartment_changed", serializeEvent(e)));
        subscribe(JobStartedEvent.class, e -> broadcastEvent("job_started", serializeEvent(e)));
        subscribe(JobProgressEvent.class, e -> broadcastEvent("job_progress", serializeEvent(e)));
        subscribe(JobCompletedEvent.class, e -> broadcastEvent("job_completed", serializeEvent(e)));
    }

    private <T extends InstrumentationEvent> void subscribe(Class<T> type, Consumer<T> listener) {
        eventBus.subscribe(type, listener);
        activeSubscriptions.add(() -> {
            // EventBus doesn't support unsubscribe; we clear all on project change
        });
    }

    private void unsubscribeFromProject() {
        if (eventBus != null) {
            eventBus.clear();
        }
        activeSubscriptions.clear();
    }

    private JsonNode serializeEvent(InstrumentationEvent event) {
        return mapper.valueToTree(event);
    }

    private void broadcastEvent(String eventType, JsonNode event) {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("type", eventType);
        msg.set("data", event);
        broadcast(msg.toString());
    }

    private JIPipeDesktopProjectWindow findWindow(String windowId) {
        for (var window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            if (window.getWindowId().equals(windowId)) return window;
        }
        return null;
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("Instrumentation server error", ex);
    }

    @Override
    public void onStart() {
        logger.info("Instrumentation server started on port {}", getPort());
    }

    /**
     * Called when project windows change. Broadcasts the updated project list.
     */
    public void onProjectWindowsChanged() {
        broadcastProjectList();
    }

    private void broadcastProjectList() {
        try {
            var registry = org.hkijena.jipipe.JIPipe.getInstance().getInstrumentation().getOperationRegistry();
            InstrumentationOperation op = registry.get("list_projects");
            if (op != null) {
                JsonNode result = op.execute(null, mapper.createObjectNode());
                ObjectNode msg = mapper.createObjectNode();
                msg.put("type", InstrumentationProtocol.EVENT_PROJECT_LIST);
                msg.set("projects", result.get("projects"));
                broadcast(msg.toString());
            }
        } catch (Exception e) {
            logger.error("Error broadcasting project list", e);
        }
    }
}
```

Note: The `subscribe` method uses `java.util.function.Consumer`. Add the import:
```java
import java.util.function.Consumer;
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=InstrumentationServerTest -q`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationProtocol.java jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationServer.java jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationServerTest.java
git commit -m "Add WebSocket instrumentation server and protocol (#1305)"
```

---

### Task 11: Settings and CLI Integration

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationApplicationSettings.java`
- Modify: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/GuiCommand.java`

**Interfaces:**
- Consumes: `InstrumentationServer` (Task 10), `JIPipeDesktopProjectWindow` (existing), application settings infrastructure (existing)
- Produces: `InstrumentationApplicationSettings` (enable, port, auto-start), `--instrumentation` CLI flag in `GuiCommand`, server lifecycle in `WindowWatcher`.

- [ ] **Step 1: Create InstrumentationApplicationSettings**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationApplicationSettings.java`:

```java
package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;

/**
 * Application settings for the instrumentation server.
 */
public class InstrumentationApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    @SetJIPipeDocumentation(name = "Enable instrumentation", description = "Enable the instrumentation WebSocket server for external automation and diagnostics tools.")
    @JIPipeParameter("enable-instrumentation")
    private boolean enableInstrumentation = false;

    @SetJIPipeDocumentation(name = "Port", description = "WebSocket port for the instrumentation server (default: 8780)")
    @JIPipeParameter("port")
    private int port = 8780;

    @SetJIPipeDocumentation(name = "Auto-start", description = "Start the instrumentation server when JIPipe launches")
    @JIPipeParameter("auto-start")
    private boolean autoStart = false;

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.General;
    }

    public boolean isEnableInstrumentation() { return enableInstrumentation; }
    public void setEnableInstrumentation(boolean enableInstrumentation) { this.enableInstrumentation = enableInstrumentation; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }
}
```

Also, register the settings in `InstrumentationPlugin.register()` by adding this line at the top of the `register()` method (Task 9):

```java
        registerApplicationSettingsSheet(new InstrumentationApplicationSettings());
```

- [ ] **Step 2: Modify GuiCommand to add --instrumentation flag**

Modify `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/GuiCommand.java`. Add the import at the top:

```java
import org.hkijena.jipipe.api.instrumentation.InstrumentationServer;
```

Add static fields (after the class declaration):

```java
    private static InstrumentationServer instrumentationServer;
    private static int pendingInstrumentationPort = -1;

    public static InstrumentationServer getInstrumentationServer() {
        return instrumentationServer;
    }
```

In `startGui()`, add instrumentation flag parsing (inside the for loop, after the `--ai-agent-monitor` check if it exists, or after the existing flags):

```java
            if ("--instrumentation".equals(argsList.get(i))) {
                if (i + 1 < argsList.size()) {
                    try {
                        pendingInstrumentationPort = Integer.parseInt(argsList.get(i + 1));
                        i++;
                    } catch (NumberFormatException e) {
                        pendingInstrumentationPort = 8780;
                    }
                } else {
                    pendingInstrumentationPort = 8780;
                }
            }
```

In `WindowWatcher.onWindowOpened()`, add after the existing window-opened logic:

```java
            // Start the instrumentation server after the first window opens
            if (instrumentationServer == null && pendingInstrumentationPort > 0) {
                instrumentationServer = new InstrumentationServer(pendingInstrumentationPort);
                try {
                    instrumentationServer.start();
                    System.err.println("Instrumentation server started on port " + pendingInstrumentationPort);
                } catch (Exception e) {
                    System.err.println("Failed to start instrumentation server: " + e.getMessage());
                    instrumentationServer = null;
                }
                pendingInstrumentationPort = -1;
            }
            if (instrumentationServer != null) {
                SwingUtilities.invokeLater(() -> instrumentationServer.onProjectWindowsChanged());
            }
```

In `WindowWatcher.onWindowClosed()`, add before `JIPipe.exitLater(0)`:

```java
                if (instrumentationServer != null) {
                    try {
                        instrumentationServer.stop(0);
                    } catch (Exception ignored) { }
                }
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl jipipe-core,jipipe-launcher -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/InstrumentationApplicationSettings.java jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/GuiCommand.java
git commit -m "Add instrumentation settings and CLI integration (#1305)"
```

---

### Task 12: Final Verification

- [ ] **Step 1: Run full compilation**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all instrumentation tests**

Run: `mvn test -pl jipipe-core -Dtest="org.hkijena.jipipe.api.instrumentation.*" -q`
Expected: All tests PASS

- [ ] **Step 3: Verify the server starts with --instrumentation flag**

Run: `mvn package -pl jipipe-core,jipipe-launcher -q -DskipTests` (if needed for a runnable jar, or test manually)

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "Complete instrumentation layer (#1305)"
```
