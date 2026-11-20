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

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins the tool scope declarations of the non-default operations over the LIVE registry
 * (spec section 8, review 2026-11-20 I-2: the promised pin was missing). The broker routes
 * and publishes tools from these declarations, so a deleted or accidentally reverted
 * override must fail here instead of silently changing the MCP surface.
 * <p>
 * Plugin-aware per the test policy: exact (id, scope) pairs for the curated core set, no
 * pinned registry counts (plugins may register more operations).
 */
public class AutomationToolScopeDeclarationTest {

    /**
     * Every operation whose declared scope is NOT the {@link AutomationToolScope#PROJECT_BOUND}
     * default, with the scope it must declare.
     */
    private static final Map<String, AutomationToolScope> EXPECTED_SCOPES = new LinkedHashMap<>();

    static {
        EXPECTED_SCOPES.put("list_projects", AutomationToolScope.INSTANCE_GLOBAL_AGGREGATED);
        EXPECTED_SCOPES.put("list_operations", AutomationToolScope.NOT_A_TOOL);
        EXPECTED_SCOPES.put("create_project", AutomationToolScope.INSTANCE_GLOBAL);
        EXPECTED_SCOPES.put("open_project", AutomationToolScope.INSTANCE_GLOBAL);
        EXPECTED_SCOPES.put("get_server_info", AutomationToolScope.INSTANCE_GLOBAL);
        EXPECTED_SCOPES.put("get_node_docs", AutomationToolScope.INSTANCE_GLOBAL);
        EXPECTED_SCOPES.put("list_algorithms", AutomationToolScope.INSTANCE_GLOBAL);
        EXPECTED_SCOPES.put("search_nodes", AutomationToolScope.INSTANCE_GLOBAL);
        EXPECTED_SCOPES.put("get_job_status", AutomationToolScope.INSTANCE_GLOBAL);
        EXPECTED_SCOPES.put("cancel_job", AutomationToolScope.INSTANCE_GLOBAL);
    }

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @Test
    void nonDefaultScopesAreDeclaredByTheLiveRegistry() {
        for (Map.Entry<String, AutomationToolScope> expected : EXPECTED_SCOPES.entrySet()) {
            AutomationOperation operation = JIPipe.getAutomation().getOperationRegistry().get(expected.getKey());
            assertNotNull(operation, "operation '" + expected.getKey()
                    + "' must be registered (its scope declaration is pinned by this test)");
            assertEquals(expected.getValue(), operation.getToolScope(), "the tool scope of '"
                    + expected.getKey() + "'");
        }
    }
}
