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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.AutomationToolScope;
import org.hkijena.jipipe.api.knowledge.KdbDomain;
import org.hkijena.jipipe.api.knowledge.KdbEntry;
import org.hkijena.jipipe.api.knowledge.KdbEntryKind;
import org.hkijena.jipipe.api.knowledge.KdbEntryLoader;
import org.hkijena.jipipe.api.knowledge.KnowledgeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the three read-only knowledge base operations over the live registry
 * (declarations) and over an injected fixture corpus (behavior): the INSTANCE_GLOBAL
 * scope and the Query category, the search response with the rendered agent facing
 * text, the clamped limit, the missing key error envelope, the payload fields
 * (structured plus rendered), and the schemas that mirror the parse.
 * <p>
 * The tests never execute against the packaged corpus (it is a content concern that
 * changes independently): the singleton is replaced with a fixture corpus for the
 * behavior tests and reset afterwards.
 */
public class KnowledgeOperationsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final List<String> OPERATION_IDS = List.of(
            "search_knowledge", "get_knowledge_entry", "list_knowledge_domains");

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @AfterEach
    void resetSingleton() {
        KnowledgeService.setInstanceForTests(null);
    }

    /**
     * A context that the operations must never touch: the knowledge operations
     * are INSTANCE_GLOBAL and work without a project.
     */
    private static AutomationContext unusedContext() {
        return new AutomationContext(null, null, null, null, null);
    }

    private static Path fixtureRoot() {
        try {
            return Paths.get(KnowledgeOperationsTest.class.getResource("/kdb-fixtures/lanes/service").toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The fixture corpus (the same fixtures the service tests use) plus one
     * hand-built entry that carries sinceVersion and sourceRefs, so the
     * "included when present" wire behavior of get_knowledge_entry is
     * observable.
     */
    private static void installFixtureCorpus() {
        List<KdbEntry> entries = new ArrayList<>(
                new KdbEntryLoader().loadAll(fixtureRoot()).getEntries());
        entries.add(new KdbEntry(
                "tables/patterns/version-pinned",
                UUID.randomUUID(),
                KdbEntryKind.PATTERN,
                KdbDomain.TABLES,
                "Version pinned entry",
                Map.of(),
                "An entry with provenance.",
                List.of(),
                "6.0.0",
                null,
                List.of("workflows/report.md#pinned"),
                Map.of()));
        KnowledgeService.setInstanceForTests(new KnowledgeService(entries));
    }

    private static AutomationOperation operation(String id) {
        AutomationOperation operation = JIPipe.getAutomation().getOperationRegistry().get(id);
        assertNotNull(operation, "operation '" + id + "' must be registered");
        return operation;
    }

    private static JsonNode findResult(JsonNode result, String entryKey) {
        for (JsonNode item : result.get("results")) {
            if (entryKey.equals(item.get("entryKey").asText())) {
                return item;
            }
        }
        return null;
    }

    @Test
    void registersThreeReadOnlyGlobalOperations() {
        for (String id : OPERATION_IDS) {
            AutomationOperation operation = operation(id);
            assertTrue(operation.isReadOnly(), "'" + id + "' must be read-only");
            assertEquals(AutomationToolScope.INSTANCE_GLOBAL, operation.getToolScope(),
                    "'" + id + "' must be INSTANCE_GLOBAL");
            assertEquals(AutomationOperationCategory.Query, operation.getCategory(),
                    "'" + id + "' must be a Query operation");
        }
    }

    @Test
    void searchReturnsRenderedEntries() {
        installFixtureCorpus();
        ObjectNode params = mapper.createObjectNode().put("query", "cellpose");

        JsonNode result = new KnowledgeOperations.SearchKnowledge().execute(unusedContext(), params);

        assertFalse(result.has("error"), () -> "a valid search must not error: " + result);
        JsonNode results = result.get("results");
        assertNotNull(results, "the search response must carry a results array");
        assertFalse(results.isEmpty(), "the fixture corpus must answer the pilot term");
        JsonNode hit = findResult(result, "bioimage-analysis/patterns/current-cellpose");
        assertNotNull(hit, "the pilot term must surface the fixture entry: " + results);
        assertEquals("Current Cellpose pipeline", hit.get("title").asText());
        assertEquals("pattern", hit.get("kind").asText());
        assertEquals("bioimage-analysis", hit.get("domain").asText());
        assertTrue(hit.get("score").isNumber(), "the result must carry the fused score");
        assertTrue(hit.get("lanes").isArray() && hit.get("lanes").size() > 0,
                "the result must name the contributing lanes");
        boolean lexical = false;
        for (JsonNode lane : hit.get("lanes")) {
            lexical |= KnowledgeService.LANE_LEXICAL.equals(lane.asText());
        }
        assertTrue(lexical, "the fixture hit must be attributed to the lexical lane");
        assertTrue(hit.get("rendered").asText().contains("Current Cellpose pipeline"),
                "the result must carry the rendered agent facing text");
        assertTrue(hit.get("rendered").asText().contains("Kind: pattern"),
                "the rendered text must carry the kind line");
    }

    @Test
    void searchLimitIsClampedNotRejected() {
        installFixtureCorpus();
        ObjectNode zeroLimit = mapper.createObjectNode().put("query", "cellpose").put("limit", 0);
        JsonNode clamped = new KnowledgeOperations.SearchKnowledge().execute(unusedContext(), zeroLimit);
        assertFalse(clamped.has("error"), "an out of range limit must be clamped, not rejected");
        assertFalse(clamped.get("results").isEmpty(),
                "a limit below 1 must clamp up to 1 instead of yielding nothing");

        ObjectNode hugeLimit = mapper.createObjectNode().put("query", "cellpose").put("limit", 1000);
        JsonNode capped = new KnowledgeOperations.SearchKnowledge().execute(unusedContext(), hugeLimit);
        assertFalse(capped.has("error"), "a limit above the maximum must be clamped, not rejected");
        assertFalse(capped.get("results").isEmpty());
    }

    @Test
    void missingEntryKeyReturnsAnErrorEnvelope() {
        installFixtureCorpus();
        ObjectNode params = mapper.createObjectNode()
                .put("entry-key", "no-such-domain/patterns/no-such-entry");

        JsonNode result = new KnowledgeOperations.GetKnowledgeEntry().execute(unusedContext(), params);

        assertTrue(result.has("error"), "an unknown key must return an error field, never a throw");
        String message = result.get("error").asText();
        assertTrue(message.contains("search_knowledge"),
                "the error must point to search_knowledge for valid keys: " + message);
    }

    @Test
    void getKnowledgeEntryReturnsThePayloadFields() {
        installFixtureCorpus();

        JsonNode result = new KnowledgeOperations.GetKnowledgeEntry().execute(unusedContext(),
                mapper.createObjectNode().put("entry-key", "bioimage-analysis/patterns/current-cellpose"));

        assertFalse(result.has("error"), () -> "a known key must not error: " + result);
        assertEquals("bioimage-analysis/patterns/current-cellpose", result.get("entryKey").asText());
        assertEquals("Current Cellpose pipeline", result.get("title").asText());
        assertEquals("pattern", result.get("kind").asText());
        assertEquals("bioimage-analysis", result.get("domain").asText());
        assertTrue(result.get("rendered").asText().contains("Current Cellpose pipeline"),
                "the response must carry the rendered agent facing text");
        assertTrue(result.get("aliases").isArray(), "the response must carry the aliases");
        assertEquals("current", result.get("aliases").get(0).asText());
        assertEquals("6.0.0", result.get("untilVersion").asText(), "an untilVersion is included when present");
        assertFalse(result.has("sinceVersion"), "a missing sinceVersion is omitted");
        assertEquals("The pipeline of today", result.get("slots").get("problem").get(0).asText(),
                "the response must carry the slot facts");

        JsonNode pinned = new KnowledgeOperations.GetKnowledgeEntry().execute(unusedContext(),
                mapper.createObjectNode().put("entry-key", "tables/patterns/version-pinned"));
        assertFalse(pinned.has("error"));
        assertEquals("6.0.0", pinned.get("sinceVersion").asText(), "a sinceVersion is included when present");
        assertFalse(pinned.has("untilVersion"), "a missing untilVersion is omitted");
        assertEquals("workflows/report.md#pinned", pinned.get("sourceRefs").get(0).asText(),
                "the response must carry the source references");
    }

    @Test
    void listKnowledgeDomainsReturnsDomainInfos() {
        installFixtureCorpus();

        JsonNode result = new KnowledgeOperations.ListKnowledgeDomains().execute(unusedContext(),
                mapper.createObjectNode());

        assertFalse(result.has("error"), () -> "the domain list must not error: " + result);
        JsonNode domains = result.get("domains");
        assertNotNull(domains, "the response must carry a domains array");
        boolean foundTables = false;
        for (JsonNode domain : domains) {
            assertNotNull(domain.get("id").asText(), "every domain carries its id");
            assertFalse(domain.get("label").asText().isBlank(), "every domain carries a human readable label");
            assertTrue(domain.get("entryCount").isNumber(), "every domain carries its entry count");
            if ("tables".equals(domain.get("id").asText())) {
                foundTables = true;
                assertEquals(2, domain.get("entryCount").asInt(),
                        "the entry count covers every loaded entry, including the hand-built one");
            }
        }
        assertTrue(foundTables, "the fixture corpus must list the tables domain");
    }

    @Test
    void schemasMirrorTheParse() {
        JsonNode searchSchema = operation("search_knowledge").getInputSchema();
        assertEquals(List.of(), AutomationSchemaUtils.validate(searchSchema,
                AutomationSchemaUtils.parseJson("{\"query\": \"cellpose\", \"domain\": \"tables\", "
                        + "\"kind\": \"mistake\", \"limit\": 8}")),
                "all search_knowledge parameters validate");
        assertFalse(AutomationSchemaUtils.validate(searchSchema, AutomationSchemaUtils.parseJson("{}")).isEmpty(),
                "search_knowledge requires the query");
        assertFalse(AutomationSchemaUtils.validate(searchSchema,
                        AutomationSchemaUtils.parseJson("{\"query\": \"x\", \"extra\": 1}")).isEmpty(),
                "search_knowledge rejects unknown properties");

        JsonNode entrySchema = operation("get_knowledge_entry").getInputSchema();
        assertEquals(List.of(), AutomationSchemaUtils.validate(entrySchema,
                AutomationSchemaUtils.parseJson("{\"entry-key\": \"tables/mistakes/string-sorting\"}")),
                "get_knowledge_entry validates its entry key");
        assertFalse(AutomationSchemaUtils.validate(entrySchema, AutomationSchemaUtils.parseJson("{}")).isEmpty(),
                "get_knowledge_entry requires the entry key");
        assertFalse(AutomationSchemaUtils.validate(entrySchema,
                        AutomationSchemaUtils.parseJson("{\"entry-key\": \"x\", \"query\": \"y\"}")).isEmpty(),
                "get_knowledge_entry rejects unknown properties");

        JsonNode domainsSchema = operation("list_knowledge_domains").getInputSchema();
        assertEquals(List.of(), AutomationSchemaUtils.validate(domainsSchema,
                AutomationSchemaUtils.parseJson("{}")),
                "list_knowledge_domains takes no parameters");
        assertFalse(AutomationSchemaUtils.validate(domainsSchema,
                        AutomationSchemaUtils.parseJson("{\"query\": \"x\"}")).isEmpty(),
                "list_knowledge_domains rejects unknown properties");
    }
}
