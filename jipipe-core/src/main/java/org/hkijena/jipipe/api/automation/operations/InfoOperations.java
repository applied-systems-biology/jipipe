package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaSpec;
import org.hkijena.jipipe.api.automation.AutomationToolScope;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.jipx.JIPipeProjectFairCheck;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Built-in info operations: project summary, FAIR check, project report, algorithm catalog,
 * and node documentation.
 */
public class InfoOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Maximum number of catalog entries returned by list_algorithms before truncation.
     */
    private static final int LIST_ALGORITHMS_LIMIT = 200;

    /**
     * Fixed heading level of get_project_report: the report mirrors the Project Report UI,
     * whose top level is heading level 3.
     */
    private static final int REPORT_HEADING_LEVEL = 3;

    /**
     * Inline cap of get_project_report: responses without save-to refuse reports above this
     * length instead of truncating them. Package-visible for the boundary test.
     */
    static final int INLINE_CAP_CHARS = 100_000;

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    private static String textOrNull(JsonNode params, String field) {
        return params.has(field) && !params.get(field).isNull() ? params.get(field).asText() : null;
    }

    /**
     * Renders the project report text and applies the get_project_report response contract.
     * Package-visible seam so the inline cap boundary is testable without a context. The
     * rendered text mirrors the Project Report UI (compartments, nodes, connections, and
     * non-default parameters) at the fixed heading level {@link #REPORT_HEADING_LEVEL}. A
     * non-blank save-to that resolves inside the allowed roots
     * ({@link AutomationOperationUtils#resolveWriteTarget}, the same machinery as the
     * screenshot save-to) is written as UTF-8 and only the path and size are returned, never
     * the inline text (size contract); a refused target returns the guard's error naming the
     * roots. Without save-to, reports of at most {@link #INLINE_CAP_CHARS} characters are
     * returned inline with truncated=false; longer reports are refused with an error naming
     * the actual size and advising save-to. The cap is a hard refusal, never a truncation:
     * exactly the cap still inlines.
     */
    static JsonNode renderReport(JIPipeProject project, String saveTo) {
        StringBuilder stringBuilder = new StringBuilder();
        project.getTextDescription(stringBuilder, REPORT_HEADING_LEVEL);
        String report = stringBuilder.toString();
        if (saveTo != null && !saveTo.isBlank()) {
            try {
                AutomationOperationUtils.TargetPath resolved = AutomationOperationUtils.resolveWriteTarget(
                        project, "save-to", saveTo.trim());
                if (resolved.path() == null) {
                    return error(resolved.errorMessage());
                }
                Path target = resolved.path();
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                Files.writeString(target, report, StandardCharsets.UTF_8);
                ObjectNode data = mapper.createObjectNode();
                data.put("savedTo", target.toString());
                data.put("sizeBytes", Files.size(target));
                return data;
            } catch (Exception e) {
                return error("Failed to save project report: " + e.getMessage());
            }
        }
        if (report.length() > INLINE_CAP_CHARS) {
            return error("The rendered project report is " + report.length() + " characters, above the inline cap of "
                    + INLINE_CAP_CHARS + " characters. Use the save-to parameter to write the full report to a file "
                    + "inside an allowed export root (the jipipe-mcp-export directory in the system temp directory, "
                    + "or the project work directory).");
        }
        ObjectNode data = mapper.createObjectNode();
        data.put("report", report);
        data.put("truncated", false);
        return data;
    }

    /**
     * Renders the taxonomy path(s) of a node as human-readable function chains,
     * e.g. "Images > Filtering". The first entry is the primary function.
     */
    private static List<String> taxonomyPaths(JIPipeNodeInfo info) {
        Set<JIPipeNodeTaxonomyFunction> functions = new LinkedHashSet<>();
        if (info.getPrimaryFunction() != null) {
            functions.add(info.getPrimaryFunction());
        }
        functions.addAll(info.getSecondaryFunctions());
        List<String> paths = new ArrayList<>();
        for (JIPipeNodeTaxonomyFunction function : functions) {
            Deque<String> names = new ArrayDeque<>();
            for (JIPipeNodeTaxonomyFunction current = function; current != null; current = current.getParent()) {
                names.push(current.getName());
            }
            paths.add(String.join(" > ", names));
        }
        return paths;
    }

    /**
     * Adds the taxonomy function paths of a node info to an array field.
     */
    private static void putFunctions(ObjectNode node, String field, JIPipeNodeInfo info) {
        ArrayNode functions = node.putArray(field);
        for (String path : taxonomyPaths(info)) {
            functions.add(path);
        }
    }

    /**
     * Converts a plain-text parameter description to text without HTML markup.
     * Parameter descriptions are plain strings that may still contain HTML tags or entities.
     */
    private static String parameterDescriptionToText(String description) {
        if (description == null || description.isEmpty()) {
            return "";
        }
        return new HTMLText(description).toPlainText();
    }

    public static class GetProjectInfo implements AutomationOperation {
        @Override
        public String getId() {
            return "get_project_info";
        }

        @Override
        public String getDescription() {
            return "Get a project summary: name, file path, compartments with node counts, metadata, and cache size";
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
                return mapper.createObjectNode().put("error", "No project loaded");
            }
            ObjectNode data = mapper.createObjectNode();
            String name = project.getMetadata().getName();
            if (name == null || name.isBlank()) {
                name = project.getProjectFile() != null
                        ? project.getProjectFile().getFileName().toString() : "Untitled";
            }
            data.put("name", name);
            data.put("path", project.getProjectFile() != null ? project.getProjectFile().toString() : null);
            ArrayNode compartments = data.putArray("compartments");
            int totalNodes = 0;
            for (var entry : project.getCompartments().entrySet()) {
                int nodeCount = project.getGraph().getNodesWithinCompartment(entry.getKey()).size();
                totalNodes += nodeCount;
                ObjectNode comp = compartments.addObject();
                comp.put("id", entry.getKey().toString());
                comp.put("name", entry.getValue().getName());
                comp.put("nodeCount", nodeCount);
            }
            data.put("compartmentCount", compartments.size());
            data.put("nodeCount", totalNodes);
            data.set("metadata", JIPipeAutomationAPI.projectMetadata(project));
            ObjectNode cache = data.putObject("cache");
            cache.put("entries", project.getCache().size());
            return data;
        }
    }

    /**
     * FAIR-readiness query (feature #1383): the metadata-level pre-save check for
     * sharing a project. Delegates to {@link JIPipeProjectFairCheck}, whose item
     * strings are passed through verbatim so their stable machine-readable
     * prefixes ("License -", "Summary -", "Authors -") stay matchable.
     */
    public static class CheckFair implements AutomationOperation {
        @Override
        public String getId() {
            return "check_fair";
        }

        @Override
        public String getDescription() {
            return "Check the FAIR-readiness of the current project metadata (pre-save check): returns fair and the list of missing items. "
                    + "Fix items with set_project_metadata: license fixes a \"License -\" item, summary fixes a \"Summary -\" item, "
                    + "authors fixes an \"Authors -\" item. The missing strings are passed through verbatim from the checker and start with "
                    + "stable, machine-readable prefixes (\"License -\", \"Summary -\", \"Authors -\") for exact matching";
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
                return mapper.createObjectNode().put("error", "No project loaded");
            }
            List<String> missing = JIPipeProjectFairCheck.missing(project);
            ObjectNode data = mapper.createObjectNode();
            data.put("fair", missing.isEmpty());
            ArrayNode missingItems = data.putArray("missing");
            for (String item : missing) {
                missingItems.add(item);
            }
            return data;
        }
    }

    public static class ListAlgorithms implements AutomationOperation {
        @Override
        public String getId() {
            return "list_algorithms";
        }

        @Override
        public String getDescription() {
            return "List the algorithm catalog (registered node types) with id, name, description, and taxonomy functions";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("query", "Optional filter matched case-insensitively against node type name and id; omit to list the full catalog")
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
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
        public AutomationToolScope getToolScope() {
            return AutomationToolScope.INSTANCE_GLOBAL;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String query = params.has("query") ? params.get("query").asText() : null;
            String lowerQuery = query != null && !query.isBlank() ? query.toLowerCase() : null;
            List<JIPipeNodeInfo> matches = new ArrayList<>();
            for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
                String name = info.getName() != null ? info.getName() : info.getId();
                if (lowerQuery == null
                        || name.toLowerCase().contains(lowerQuery)
                        || info.getId().toLowerCase().contains(lowerQuery)) {
                    matches.add(info);
                }
            }
            matches.sort(Comparator.comparing(JIPipeNodeInfo::getId));
            ObjectNode data = mapper.createObjectNode();
            ArrayNode algorithms = data.putArray("algorithms");
            int count = Math.min(matches.size(), LIST_ALGORITHMS_LIMIT);
            for (int i = 0; i < count; i++) {
                JIPipeNodeInfo info = matches.get(i);
                ObjectNode entry = algorithms.addObject();
                entry.put("id", info.getId());
                entry.put("name", info.getName() != null ? info.getName() : info.getId());
                entry.put("description", info.getDescription() != null ? info.getDescription().toPlainText() : "");
                putFunctions(entry, "functions", info);
            }
            data.put("totalMatches", matches.size());
            data.put("truncated", matches.size() > LIST_ALGORITHMS_LIMIT);
            return data;
        }
    }

    public static class GetNodeDocs implements AutomationOperation {
        @Override
        public String getId() {
            return "get_node_docs";
        }

        @Override
        public String getDescription() {
            return "Get documentation for a node type: description, taxonomy functions, and parameter docs";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("node-id", "Registered node type id, as returned by list_algorithms").required()
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
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
        public AutomationToolScope getToolScope() {
            return AutomationToolScope.INSTANCE_GLOBAL;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String nodeType = params.get("node-id").asText();
            if (!JIPipe.getNodes().hasNodeInfoWithId(nodeType)) {
                return mapper.createObjectNode().put("error", "Unknown node type: " + nodeType);
            }
            JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(nodeType);
            ObjectNode data = mapper.createObjectNode();
            data.put("id", info.getId());
            data.put("name", info.getName() != null ? info.getName() : info.getId());
            data.put("description", info.getDescription() != null ? info.getDescription().toPlainText() : "");
            putFunctions(data, "functions", info);
            ArrayNode parameters = data.putArray("parameters");
            try {
                JIPipeGraphNode instance = info.newInstance();
                for (JIPipeParameterAccess access : JIPipeParameterTree.getParameters(instance).values()) {
                    String key = access.getKey();
                    if (key == null || key.startsWith("jipipe:") || key.startsWith("jipipe-ui:")) continue;
                    ObjectNode p = parameters.addObject();
                    p.put("key", key);
                    p.put("name", access.getName() != null ? access.getName() : key);
                    p.put("description", parameterDescriptionToText(access.getDescription()));
                    p.put("type", access.getFieldClass() != null ? access.getFieldClass().getSimpleName() : "?");
                }
            } catch (Exception e) {
                // Node could not be instantiated in this environment: return the docs without parameters
            }
            return data;
        }
    }

    /**
     * Project report query: the Project Report UI's text, with an inline cap and a rooted
     * save-to alternative for large projects.
     */
    public static class GetProjectReport implements AutomationOperation {
        @Override
        public String getId() {
            return "get_project_report";
        }

        @Override
        public String getDescription() {
            return "Get the project report: the text of the Project Report UI (compartments with their nodes, "
                    + "connections, and non-default parameters), rendered at fixed heading level 3. Reports of at "
                    + "most 100000 characters are returned inline in the report field; truncated is always false "
                    + "because the cap refuses oversized reports instead of truncating them. For a larger report, "
                    + "or to keep the text, pass save-to: the full report is written as a UTF-8 text file and only "
                    + "savedTo and sizeBytes are returned. The optional save-to target must point inside an allowed "
                    + "export root: the jipipe-mcp-export directory in the system temp directory, or the project "
                    + "work directory. Targets outside these roots are refused (error naming the roots); a blank "
                    + "save-to falls back to the inline response";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("save-to", "Optional file path to save the full report to as a UTF-8 text file; must be inside an allowed export root (the jipipe-mcp-export directory in the system temp directory, or the project work directory). Targets outside these roots are refused (error naming the roots). Blank or omitted: the report is returned inline, subject to the inline cap stated in the operation description")
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
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
            return renderReport(project, textOrNull(params, "save-to"));
        }
    }
}
