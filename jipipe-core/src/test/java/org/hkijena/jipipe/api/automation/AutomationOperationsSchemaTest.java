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

package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the input schemas of the REAL registered automation operations (spec section 11
 * intent): a typo'd schema text block must surface here, not only at first dispatch.
 * <p>
 * Plugin-aware per the test policy: presence-based assertions on a curated set of core
 * operation ids, no pinned registry counts (plugins may register more operations).
 */
public class AutomationOperationsSchemaTest {

    /**
     * The curated core operation-id set: every operation registered by a core plugin
     * (single-source ship set: only {@code AutomationPlugin} registers operations). The schema
     * gates iterate exactly this set, never the live registry (plugins may register further
     * operations whose schema drift must not fail the core build). Entries are grouped like the
     * registration blocks in {@code AutomationPlugin} for easy one-to-one auditing.
     * <p>
     * Presence gate for the tests below: they would pass vacuously against an empty registry
     * otherwise.
     */
    static final Set<String> EXPECTED_CORE_OPERATION_IDS = Set.of(
            // Project operations
            "list_projects",
            "list_operations",
            "create_project",
            "open_project",
            "get_server_info",
            "save_project",
            "get_project_metadata",
            // Query operations
            "query_compartments",
            "query_graph",
            "query_notes",
            "query_node",
            "get_parameter_diff",
            "query_data",
            "find_nodes",
            // Execution operations
            "run_node",
            "run_compartment",
            "run_pipeline",
            "update_cache",
            "get_job_status",
            "cancel_job",
            "arrange_nodes",
            // Modification operations
            "add_node",
            "remove_node",
            "add_connection",
            "remove_connection",
            "move_node",
            "create_note",
            "set_parameter",
            "set_project_metadata",
            "add_compartment",
            "rename_compartment",
            // Slot operations
            "add_slot",
            "remove_slot",
            // User path operations
            "list_user_paths",
            "set_user_path",
            "remove_user_path",
            // Pipeline map operations
            "get_pipeline_map",
            "get_segment_detail",
            "describe_project",
            "search_nodes",
            // Info operations
            "get_project_info",
            "get_project_report",
            "check_fair",
            "list_algorithms",
            "get_node_docs",
            // Knowledge base operations
            "search_knowledge",
            "get_knowledge_entry",
            "list_knowledge_domains",
            // Data operations
            "describe_data",
            "export_data",
            "get_thumbnail",
            "open_data_viewer",
            // Capability operations (#1376)
            "get_capability_status",
            "list_capabilities",
            "install_capability",
            // UI state operations
            "get_ui_state",
            "navigate_to_node",
            "fit_to_nodes",
            "clear_selection",
            // UI interaction operations
            "fire_action",
            "list_ui_actions",
            "set_dock_visible",
            "list_docks",
            "ui_probe",
            "ui_click",
            "ui_drag",
            "ui_scroll",
            "ui_type_text",
            // Data viewer operations
            "get_viewer_state",
            "goto_viewer_row",
            "close_viewer",
            // Diagnostics operations
            "get_errors",
            "get_log",
            // Screenshot operations
            "screenshot_canvas",
            "screenshot_minimap",
            "screenshot_ui",
            "screenshot_window",
            "export_graph_image",
            // RO-Crate operations
            "create_ro_crate");

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static AutomationOperationRegistry registry() {
        return JIPipe.getInstance().getAutomation().getOperationRegistry();
    }

    @Test
    void coreOperationsAreRegistered() {
        Map<String, AutomationOperation> operations = registry().getAll();
        assertFalse(operations.isEmpty(), "the automation registry must not be empty");
        for (String id : EXPECTED_CORE_OPERATION_IDS) {
            assertTrue(operations.containsKey(id),
                    "core operation '" + id + "' must be registered (found: " + operations.keySet() + ")");
        }
    }

    /**
     * The completeness half of the curated-set pair (the presence gate
     * {@link #coreOperationsAreRegistered} pins the opposite direction): every id the live
     * registry holds must be curated, so a core operation registered but not added to
     * {@code EXPECTED_CORE_OPERATION_IDS} cannot silently escape the schema and bijection gates.
     */
    @Test
    void everyRegisteredOperationIsCurated() {
        List<String> uncurated = registry().getAll().keySet().stream()
                .filter(id -> !EXPECTED_CORE_OPERATION_IDS.contains(id))
                .sorted()
                .toList();
        assertTrue(uncurated.isEmpty(), "operation(s) registered but missing from the curated set: "
                + uncurated + " - add it to EXPECTED_CORE_OPERATION_IDS");
    }

    @Test
    void everyOperationDeclaresAnObjectSchema() {
        for (Map.Entry<String, AutomationOperation> entry : registry().getAll().entrySet()) {
            JsonNode schema = entry.getValue().getInputSchema();
            assertNotNull(schema, "operation '" + entry.getKey() + "' must declare an input schema");
            assertTrue(schema.isObject(), "the input schema of '" + entry.getKey()
                    + "' must be a JSON object, got: " + schema.getNodeType());
            JsonNode type = schema.get("type");
            assertNotNull(type, "the input schema of '" + entry.getKey() + "' must declare a type");
            assertEquals("object", type.asText(), "the input schema type of '" + entry.getKey() + "'");
        }
    }

    @Test
    void emptyArgsValidateAccordingToRequired() {
        JsonNode emptyArgs = AutomationSchemaUtils.parseJson("{}");
        for (Map.Entry<String, AutomationOperation> entry : registry().getAll().entrySet()) {
            JsonNode schema = entry.getValue().getInputSchema();
            JsonNode required = schema.get("required");
            boolean hasRequired = required != null && required.isArray() && !required.isEmpty();
            List<String> errors = AutomationSchemaUtils.validate(schema, emptyArgs);
            if (hasRequired) {
                assertFalse(errors.isEmpty(), "empty args against the schema of '" + entry.getKey()
                        + "' must be rejected (required: " + required + ")");
                String firstRequired = required.get(0).asText();
                assertTrue(errors.get(0).contains(firstRequired), "the first error for '" + entry.getKey()
                        + "' must name the first required property '" + firstRequired + "': " + errors);
            } else {
                assertEquals(List.of(), errors, "empty args against the schema of '"
                        + entry.getKey() + "' (no required properties) must validate cleanly");
            }
        }
    }

    /**
     * Registry contract: every property of every registered operation's input schema carries a
     * non-empty description, so agents pass correct arguments without guesswork. The validator
     * ignores the "description" keyword, but the broker copies property objects verbatim into
     * the MCP tool schemas, so the descriptions reach clients as-is. Applies to every future
     * registration too: the automation layer is internal-plugin only, and all plugins register
     * into the same registry that this test iterates.
     */
    @Test
    void everySchemaPropertyHasADescription() {
        for (Map.Entry<String, AutomationOperation> entry : registry().getAll().entrySet()) {
            JsonNode properties = entry.getValue().getInputSchema().get("properties");
            if (properties == null || !properties.isObject()) {
                continue;
            }
            properties.fields().forEachRemaining(property -> {
                JsonNode description = property.getValue().get("description");
                assertNotNull(description, "the schema property '" + property.getKey() + "' of operation '"
                        + entry.getKey() + "' must declare a description");
                assertFalse(description.asText().isBlank(), "the description of the schema property '"
                        + property.getKey() + "' of operation '" + entry.getKey() + "' must not be empty");
            });
        }
    }
}
