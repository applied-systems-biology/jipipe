/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressNode;
import org.hkijena.jipipe.api.progress.JIPipeProgressRecord;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Built-in diagnostics operations: project validation errors and the progress log of the current run.
 */
public class DiagnosticsOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Default number of records returned by get_log.
     */
    private static final int DEFAULT_LOG_TAIL = 200;

    /**
     * Maximum number of records returned by get_log. Matches the default tail limit of the
     * progress store: reading more than the tail holds is pointless.
     */
    private static final int MAX_LOG_TAIL = 8192;

    /**
     * The get_log read window over-reads the requested tail by this factor so that the level and
     * node filters can still fill the tail from the most recent records.
     */
    private static final int LOG_WINDOW_OVERREAD = 8;

    /**
     * Minimum number of records the get_log read window reads beyond the requested tail.
     */
    private static final int LOG_WINDOW_MIN_EXTRA = 64;

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    /**
     * Returns the graph node referenced by a validation entry context chain, or null.
     */
    private static JIPipeGraphNode entryNode(JIPipeValidationReportEntry entry) {
        for (JIPipeValidationReportContext context : entry.getContext().traverse()) {
            if (context instanceof GraphNodeValidationReportContext graphNodeContext) {
                return graphNodeContext.getGraphNode();
            }
        }
        return null;
    }

    /**
     * Returns the progress node a record is attributed to (log and error records carry a node id),
     * or null.
     */
    private static JIPipeProgressNode attributedNode(JIPipeProgressLog store, JIPipeProgressRecord record) {
        int nodeId;
        if (record instanceof JIPipeProgressRecord.Log logRecord) {
            nodeId = logRecord.id();
        } else if (record instanceof JIPipeProgressRecord.Error errorRecord) {
            nodeId = errorRecord.id();
        } else {
            return null;
        }
        return store.getNode(nodeId);
    }

    /**
     * Returns the node:// location URI of a progress node, or null if it has none.
     */
    private static String nodeLocation(JIPipeProgressNode node) {
        String location = node != null ? node.getLocation() : null;
        return location != null && location.startsWith("node://") ? location : null;
    }

    /**
     * Adds the node UUID of a record's attributed progress node to the entry.
     * Returns true if a node reference was added.
     */
    private static boolean putNodeReference(ObjectNode entry, JIPipeProgressLog store, JIPipeProgressRecord record) {
        String location = nodeLocation(attributedNode(store, record));
        if (location == null) {
            return false;
        }
        entry.put("node", location.substring("node://".length()));
        return true;
    }

    /**
     * Adds the compartment (and its name) of the graph node with the given UUID to the entry.
     */
    private static void putCompartmentReference(ObjectNode entry, JIPipeProject project, JIPipeGraph graph, UUID nodeUuid) {
        JIPipeGraphNode node = graph.getNodeByUUID(nodeUuid);
        UUID compartment = node != null ? graph.getCompartmentUUIDOf(node) : null;
        if (compartment == null) {
            return;
        }
        entry.put("compartment", compartment.toString());
        var projectCompartment = project.getCompartments().get(compartment);
        if (projectCompartment != null && projectCompartment.getName() != null) {
            entry.put("compartmentName", projectCompartment.getName());
        }
    }

    /**
     * Renders a numeric log level as its name, or the raw number for unknown values.
     */
    private static String levelName(int value) {
        for (JIPipeLogLevel level : JIPipeLogLevel.values()) {
            if (level.value() == value) {
                return level.toString();
            }
        }
        return String.valueOf(value);
    }

    /**
     * Parses a UUID string, or returns null if it is not a valid UUID.
     */
    private static UUID parseUuid(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static class GetErrors implements AutomationOperation {
        @Override
        public String getId() {
            return "get_errors";
        }

        @Override
        public String getDescription() {
            return "Get diagnostics errors: the project validation report (as shown by the errors panel) and, "
                    + "when a run happened, the error records of the current run's progress log";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "compartment": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Query;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeProject project = ctx.getProject();
            if (project == null) {
                return error("No project loaded");
            }
            JIPipeGraph graph = ctx.getGraph();
            UUID compartmentFilter = null;
            if (params.has("compartment")) {
                String compartmentId = params.get("compartment").asText();
                var compartment = AutomationOperationUtils.findCompartment(project, compartmentId);
                if (compartment == null) {
                    return error("Compartment not found: " + compartmentId);
                }
                compartmentFilter = compartment.getProjectCompartmentUUID();
            }

            ObjectNode data = mapper.createObjectNode();
            ArrayNode errors = data.putArray("errors");

            // Validation report: the same call the desktop errors panel makes on revalidate
            try {
                JIPipeValidationReport report = new JIPipeValidationReport();
                project.reportValidity(new UnspecifiedValidationReportContext(),
                        JIPipeValidationReportSettings.DEFAULT, report, ctx.getProgressInfo());
                for (JIPipeValidationReportEntry reportEntry : report) {
                    addValidationEntry(errors, project, graph, reportEntry, compartmentFilter);
                }
            } catch (Exception e) {
                return error("Validation failed: " + e.getMessage());
            }

            // Error records of the current run's progress store (none if no run happened)
            addRunErrors(errors, ctx, project, graph, compartmentFilter);

            data.put("count", errors.size());
            return data;
        }

        /**
         * Adds one validation report entry. Entries without a node reference are skipped when a
         * compartment filter is set: they cannot be attributed to a compartment.
         */
        private void addValidationEntry(ArrayNode errors, JIPipeProject project, JIPipeGraph graph,
                                        JIPipeValidationReportEntry reportEntry, UUID compartmentFilter) {
            JIPipeGraphNode node = entryNode(reportEntry);
            UUID compartment = node != null && graph != null ? graph.getCompartmentUUIDOf(node) : null;
            if (compartmentFilter != null && !compartmentFilter.equals(compartment)) {
                return;
            }
            ObjectNode item = errors.addObject();
            item.put("source", "validation");
            item.put("level", reportEntry.getLevel().toString());
            item.put("message", StringUtils.orElse(reportEntry.getTitle(), ""));
            if (!StringUtils.isNullOrEmpty(reportEntry.getExplanation())) {
                item.put("explanation", reportEntry.getExplanation());
            }
            if (!StringUtils.isNullOrEmpty(reportEntry.getSolution())) {
                item.put("solution", reportEntry.getSolution());
            }
            if (node != null) {
                item.put("node", node.getUUIDInParentGraph().toString());
                item.put("nodeName", node.getDisplayName());
            }
            if (compartment != null) {
                item.put("compartment", compartment.toString());
                var projectCompartment = project.getCompartments().get(compartment);
                if (projectCompartment != null && projectCompartment.getName() != null) {
                    item.put("compartmentName", projectCompartment.getName());
                }
            }
        }

        /**
         * Adds the error records of the current run's progress store. Records without a node
         * reference are skipped when a compartment filter is set.
         */
        private void addRunErrors(ArrayNode errors, AutomationContext ctx, JIPipeProject project,
                                  JIPipeGraph graph, UUID compartmentFilter) {
            JIPipeProgressLog store = ctx.getProgressInfo() != null ? ctx.getProgressInfo().getStore() : null;
            if (store == null) {
                return;
            }
            for (JIPipeProgressRecord record : store.readRecent(MAX_LOG_TAIL)) {
                if (!(record instanceof JIPipeProgressRecord.Error errorRecord)) {
                    continue;
                }
                ObjectNode item = mapper.createObjectNode();
                item.put("source", "run");
                item.put("level", "ERROR");
                item.put("message", StringUtils.orElse(errorRecord.c(), "?") + ": "
                        + StringUtils.orElse(errorRecord.m(), ""));
                item.put("ts", errorRecord.ts());
                boolean hasNode = putNodeReference(item, store, record);
                if (compartmentFilter != null && !hasNode) {
                    continue;
                }
                if (hasNode && graph != null) {
                    UUID nodeUuid = parseUuid(item.get("node").asText());
                    if (nodeUuid != null) {
                        putCompartmentReference(item, project, graph, nodeUuid);
                    }
                }
                if (compartmentFilter != null
                        && !compartmentFilter.toString().equals(item.path("compartment").asText(null))) {
                    continue;
                }
                errors.add(item);
            }
        }
    }

    public static class GetLog implements AutomationOperation {
        @Override
        public String getId() {
            return "get_log";
        }

        @Override
        public String getDescription() {
            return "Get the progress log of the current run: text records (log, error, notification) with timestamp, "
                    + "level, and node reference, newest-last. level=INFO returns only INFO records, "
                    + "level=DEBUG also includes DEBUG records";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "tail": {"type": "integer"},
                        "level": {"type": "string", "enum": ["INFO", "DEBUG"]},
                        "node": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Query;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeProject project = ctx.getProject();
            if (project == null) {
                return error("No project loaded");
            }
            JIPipeProgressLog store = ctx.getProgressInfo() != null ? ctx.getProgressInfo().getStore() : null;
            if (store == null) {
                return error("No progress log available");
            }

            int tail = params.has("tail") ? params.get("tail").asInt(DEFAULT_LOG_TAIL) : DEFAULT_LOG_TAIL;
            tail = Math.min(MAX_LOG_TAIL, Math.max(1, tail));
            JIPipeLogLevel minLevel = params.has("level") && "DEBUG".equals(params.get("level").asText())
                    ? JIPipeLogLevel.DEBUG : JIPipeLogLevel.INFO;
            String targetLocation = null;
            if (params.has("node")) {
                String nodeFilter = params.get("node").asText();
                JIPipeGraph graph = ctx.getGraph();
                JIPipeGraphNode node = graph != null ? graph.findNode(nodeFilter) : null;
                if (node == null) {
                    return error("Node not found: " + nodeFilter);
                }
                targetLocation = "node://" + node.getUUIDInParentGraph();
            }

            // Bounded read: the window over-reads the tail so the filters can still fill it,
            // and is capped by the store's own tail limit
            int window = Math.min(MAX_LOG_TAIL, Math.max(tail + LOG_WINDOW_MIN_EXTRA, tail * LOG_WINDOW_OVERREAD));
            List<JIPipeProgressRecord> records = new ArrayList<>();
            for (JIPipeProgressRecord record : store.readRecent(window)) {
                if (!isTextRecord(record)) {
                    continue;
                }
                if (!passesLevelFilter(record, minLevel)) {
                    continue;
                }
                if (targetLocation != null) {
                    String location = nodeLocation(attributedNode(store, record));
                    if (!targetLocation.equals(location)) {
                        continue;
                    }
                }
                records.add(record);
            }

            ObjectNode data = mapper.createObjectNode();
            ArrayNode items = data.putArray("records");
            int from = Math.max(0, records.size() - tail);
            for (int i = from; i < records.size(); i++) {
                items.add(renderRecord(store, records.get(i)));
            }
            data.put("count", items.size());
            return data;
        }

        /**
         * Returns true for records that carry user-readable text (the records the log render shows):
         * log, error, and notification records. Progress and structural records are skipped.
         */
        private boolean isTextRecord(JIPipeProgressRecord record) {
            return record instanceof JIPipeProgressRecord.Log
                    || record instanceof JIPipeProgressRecord.Error
                    || record instanceof JIPipeProgressRecord.Notification;
        }

        /**
         * Applies the level filter: only log records carry a level; error and notification records
         * always pass (mirrors the store's own level filter).
         */
        private boolean passesLevelFilter(JIPipeProgressRecord record, JIPipeLogLevel minLevel) {
            return !(record instanceof JIPipeProgressRecord.Log logRecord)
                    || logRecord.lv() >= minLevel.value();
        }

        /**
         * Renders one record as a diagnostics entry.
         */
        private ObjectNode renderRecord(JIPipeProgressLog store, JIPipeProgressRecord record) {
            ObjectNode item = mapper.createObjectNode();
            if (record instanceof JIPipeProgressRecord.Log logRecord) {
                item.put("type", "log");
                item.put("ts", logRecord.ts());
                item.put("level", levelName(logRecord.lv()));
                item.put("message", StringUtils.orElse(logRecord.m(), ""));
                putNodeReference(item, store, record);
            } else if (record instanceof JIPipeProgressRecord.Error errorRecord) {
                item.put("type", "error");
                item.put("ts", errorRecord.ts());
                item.put("level", "ERROR");
                item.put("message", StringUtils.orElse(errorRecord.c(), "?") + ": "
                        + StringUtils.orElse(errorRecord.m(), ""));
                putNodeReference(item, store, record);
            } else if (record instanceof JIPipeProgressRecord.Notification notificationRecord) {
                item.put("type", "notification");
                item.put("ts", notificationRecord.ts());
                item.put("message", StringUtils.orElse(notificationRecord.h(), "") + ": "
                        + StringUtils.orElse(notificationRecord.d(), ""));
            }
            return item;
        }
    }
}
