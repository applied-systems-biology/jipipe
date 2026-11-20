package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaSpec;
import org.hkijena.jipipe.api.automation.AutomationToolScope;
import org.hkijena.jipipe.api.knowledge.DomainInfo;
import org.hkijena.jipipe.api.knowledge.KdbEntry;
import org.hkijena.jipipe.api.knowledge.KnowledgeEntryPayload;
import org.hkijena.jipipe.api.knowledge.KnowledgeResult;
import org.hkijena.jipipe.api.knowledge.KnowledgeService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Built-in knowledge base operations: read-only access to the packaged KDB
 * corpus (design spec 5.7) through the process-wide {@link KnowledgeService}
 * singleton. The KDB is global canon, not project data, so the operations
 * are INSTANCE_GLOBAL and never touch the automation context: any live
 * window answers, and no project argument is part of the schemas.
 */
public class KnowledgeOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * The default of the search_knowledge limit parameter.
     */
    static final int SEARCH_DEFAULT_LIMIT = 8;

    /**
     * The upper clamp of the search_knowledge limit parameter. Out of range
     * values are clamped, never rejected.
     */
    static final int SEARCH_MAX_LIMIT = 25;

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    /**
     * Search the knowledge base: the query is ranked over the lexical, the
     * optional vector, and the relation lane, fused and re-ranked
     * deterministically. Every result carries the rendered agent facing text,
     * so one call gives the agent the full entry content.
     */
    public static class SearchKnowledge implements AutomationOperation {
        @Override
        public String getId() {
            return "search_knowledge";
        }

        @Override
        public String getDescription() {
            return "Search the JIPipe knowledge base: curated corpus entries (concepts, workflow patterns, "
                    + "mistakes, decision trees, node notes) ranked over a lexical, an optional vector, and a "
                    + "relation lane. Each result carries entryKey, title, kind, domain, score, lanes, and the "
                    + "rendered entry text. Resolve an entryKey with get_knowledge_entry for the full entry and "
                    + "list the valid domains with list_knowledge_domains. Questions about the open project's "
                    + "data (nodes, parameters, slots, cache) are answered by the project operations such as "
                    + "query_graph, query_node, and query_data, not by the knowledge base";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("query", "The search text: node ids, parameter names, error strings, or free prose; a blank query matches nothing").required()
                .string("domain", "Exact domain id to filter by, as listed by list_knowledge_domains; omit for all domains")
                .string("kind", "Exact entry kind id to filter by (one of concept, pattern, mistake, decision-tree, node-note); omit for all kinds")
                .integer("limit", "Maximum number of results, clamped into 1..25 (default 8); out of range values are clamped, never rejected")
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
            KnowledgeService service = KnowledgeService.getInstance();
            String query = params.has("query") ? params.get("query").asText() : null;
            String domain = params.has("domain") ? params.get("domain").asText() : null;
            String kind = params.has("kind") ? params.get("kind").asText() : null;
            int requestedLimit = params.path("limit").asInt(SEARCH_DEFAULT_LIMIT);
            int limit = Math.max(1, Math.min(SEARCH_MAX_LIMIT, requestedLimit));
            ObjectNode data = mapper.createObjectNode();
            ArrayNode results = data.putArray("results");
            for (KnowledgeResult result : service.searchKnowledge(query, domain, kind, limit)) {
                ObjectNode item = results.addObject();
                item.put("entryKey", result.entryKey());
                item.put("score", result.score());
                ArrayNode lanes = item.putArray("lanes");
                for (String lane : result.lanes()) {
                    lanes.add(lane);
                }
                // The rendered text comes from the same service, so a result
                // and its full entry never disagree.
                Optional<KnowledgeEntryPayload> payload = service.getKnowledgeEntry(result.entryKey());
                if (payload.isPresent()) {
                    KdbEntry entry = payload.get().entry();
                    item.put("title", entry.getTitle());
                    item.put("kind", entry.getKind().getId());
                    item.put("domain", entry.getDomain().getId());
                    item.put("rendered", payload.get().rendered());
                }
            }
            return data;
        }
    }

    /**
     * One knowledge base entry by key: the structured corpus entry plus the
     * rendered agent facing text (the payload of the service). Unknown keys
     * return an error envelope naming the key convention, never a throw.
     */
    public static class GetKnowledgeEntry implements AutomationOperation {
        @Override
        public String getId() {
            return "get_knowledge_entry";
        }

        @Override
        public String getDescription() {
            return "Get one knowledge base entry by its entry key: the structured fields (title, kind, domain, "
                    + "aliases, validity versions, source references, slot facts) plus the rendered agent facing "
                    + "text with the registry resolved node facts and next steps. Entry keys have the form "
                    + "<domain>/<kind-directory>/<name> and come from search_knowledge; list_knowledge_domains "
                    + "lists the domains. An unknown key returns an error naming the convention. Node parameter "
                    + "details live in the node registry (get_node_docs), not in the corpus";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("entry-key", "The entry key of the entry, of the form <domain>/<kind-directory>/<name>, as returned by search_knowledge").required()
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
            KnowledgeService service = KnowledgeService.getInstance();
            String entryKey = params.get("entry-key").asText();
            Optional<KnowledgeEntryPayload> payload = service.getKnowledgeEntry(entryKey);
            if (payload.isEmpty()) {
                return error("Unknown entry key: '" + entryKey + "'. Entry keys have the form "
                        + "<domain>/<kind-directory>/<name> (list_knowledge_domains lists the domains); "
                        + "use search_knowledge to find valid keys.");
            }
            KdbEntry entry = payload.get().entry();
            ObjectNode data = mapper.createObjectNode();
            data.put("entryKey", entry.getEntryKey());
            data.put("title", entry.getTitle());
            data.put("kind", entry.getKind().getId());
            data.put("domain", entry.getDomain().getId());
            ArrayNode aliases = data.putArray("aliases");
            for (String alias : entry.getAliases()) {
                aliases.add(alias);
            }
            if (entry.getSinceVersion() != null) {
                data.put("sinceVersion", entry.getSinceVersion());
            }
            if (entry.getUntilVersion() != null) {
                data.put("untilVersion", entry.getUntilVersion());
            }
            ArrayNode sourceRefs = data.putArray("sourceRefs");
            for (String sourceRef : entry.getSourceRefs()) {
                sourceRefs.add(sourceRef);
            }
            ObjectNode slots = data.putObject("slots");
            for (Map.Entry<String, List<Object>> slot : entry.getSlotValues().entrySet()) {
                ArrayNode values = slots.putArray(slot.getKey());
                for (Object value : slot.getValue()) {
                    values.add(mapper.valueToTree(value));
                }
            }
            data.put("rendered", payload.get().rendered());
            return data;
        }
    }

    /**
     * The knowledge base domains with their entry counts: the valid values of
     * the domain filter of search_knowledge.
     */
    public static class ListKnowledgeDomains implements AutomationOperation {
        @Override
        public String getId() {
            return "list_knowledge_domains";
        }

        @Override
        public String getDescription() {
            return "List the knowledge base domains with their entry counts: the valid values of the domain "
                    + "filter of search_knowledge. Entries are searched with search_knowledge and resolved by "
                    + "key with get_knowledge_entry";
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
            ObjectNode data = mapper.createObjectNode();
            ArrayNode domains = data.putArray("domains");
            for (DomainInfo domain : KnowledgeService.getInstance().listKnowledgeDomains()) {
                ObjectNode item = domains.addObject();
                item.put("id", domain.id());
                item.put("label", domain.label());
                item.put("entryCount", domain.entryCount());
            }
            return data;
        }
    }
}
