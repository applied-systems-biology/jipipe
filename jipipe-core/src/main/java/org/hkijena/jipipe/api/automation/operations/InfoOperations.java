package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.jipx.JIPipeProjectFairCheck;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Built-in info operations: project summary, FAIR check, algorithm catalog, and node documentation.
 */
public class InfoOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Maximum number of catalog entries returned by list_algorithms before truncation.
     */
    private static final int LIST_ALGORITHMS_LIMIT = 200;

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

    /**
     * Renders the authors or acknowledgements list of the project metadata as
     * the response array. Every string field is emitted as an empty string when
     * unset (never an omitted key) and the booleans are always present, so the
     * shape is stable at every level.
     */
    private static ArrayNode authorsToArray(JIPipeAuthorMetadata.List authors) {
        ArrayNode array = mapper.createArrayNode();
        if (authors == null) {
            return array;
        }
        for (JIPipeAuthorMetadata author : authors) {
            ObjectNode entry = array.addObject();
            entry.put("firstName", StringUtils.nullToEmpty(author.getFirstName()));
            entry.put("lastName", StringUtils.nullToEmpty(author.getLastName()));
            entry.put("orcid", StringUtils.nullToEmpty(author.getOrcid()));
            entry.put("email", StringUtils.nullToEmpty(author.getEmail()));
            entry.put("website", StringUtils.nullToEmpty(author.getWebsite()));
            entry.put("firstAuthor", author.isFirstAuthor());
            entry.put("correspondingAuthor", author.isCorrespondingAuthor());
            ArrayNode affiliations = entry.putArray("affiliations");
            for (JIPipeOrganizationMetadata affiliation : author.getAffiliations()) {
                ObjectNode affiliationNode = affiliations.addObject();
                affiliationNode.put("name", StringUtils.nullToEmpty(affiliation.getName()));
                affiliationNode.put("ror", StringUtils.nullToEmpty(affiliation.getRor()));
                affiliationNode.put("website", StringUtils.nullToEmpty(affiliation.getWebsite()));
            }
        }
        return array;
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
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.emptyObjectSchema();
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
            ObjectNode metadata = data.putObject("metadata");
            metadata.put("description", project.getMetadata().getDescription() != null
                    ? project.getMetadata().getDescription().toPlainText() : "");
            metadata.put("license", StringUtils.nullToEmpty(project.getMetadata().getLicense()));
            metadata.put("summary", project.getMetadata().getSummary() != null
                    ? project.getMetadata().getSummary().toPlainText() : "");
            metadata.put("website", StringUtils.nullToEmpty(project.getMetadata().getWebsite()));
            metadata.put("citation", StringUtils.nullToEmpty(project.getMetadata().getCitation()));
            metadata.set("authors", authorsToArray(project.getMetadata().getAuthors()));
            metadata.set("acknowledgements", authorsToArray(project.getMetadata().getAcknowledgements()));
            ObjectNode globalParameters = metadata.putObject("globalParameters");
            for (JIPipeParameterAccess access : project.getMetadata().getGlobalParameters().getParameters().values()) {
                String key = access.getKey();
                if (key == null || key.startsWith("jipipe:") || key.startsWith("jipipe-ui:")) continue;
                Object value = access.get(Object.class);
                globalParameters.put(key, value != null ? value.toString() : "null");
            }
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
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.emptyObjectSchema();
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

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "query": {"type": "string", "description": "Optional filter matched case-insensitively against node type name and id; omit to list the full catalog"}
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

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-id"],
                      "properties": {
                        "node-id": {"type": "string", "description": "Registered node type id, as returned by list_algorithms"}
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
}
