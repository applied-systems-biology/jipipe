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

package org.hkijena.jipipe.api.automation.permissions;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the resolution order of {@link AutomationPermissionService}:
 * grant, then CLI override, then user state, then category default.
 */
public class AutomationPermissionServiceTest {

    private static AutomationOperation stubOperation(String id, boolean readOnly) {
        return new AutomationOperation() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getDescription() {
                return "stub";
            }

            @Override
            public JsonNode getInputSchema() {
                return AutomationSchemaUtils.parseJson("{}");
            }

            @Override
            public JsonNode execute(AutomationContext ctx, JsonNode params) {
                throw new UnsupportedOperationException("never executed by the permission service");
            }

            @Override
            public boolean isReadOnly() {
                return readOnly;
            }
        };
    }

    @Test
    void readOnlyOperationResolvesAllowByDefault() {
        AutomationPermissionService service = new AutomationPermissionService();
        assertEquals(AutomationPermissionState.Allow,
                service.resolve(stubOperation("query-graph", true), "p1"));
    }

    @Test
    void nonReadOnlyOperationResolvesAskByDefault() {
        AutomationPermissionService service = new AutomationPermissionService();
        assertEquals(AutomationPermissionState.Ask,
                service.resolve(stubOperation("run-pipeline", false), "p1"));
    }

    @Test
    void userDenyOverridesCategoryDefault() {
        AutomationPermissionService service = new AutomationPermissionService();
        service.setUserState("run-pipeline", AutomationPermissionState.Deny);
        assertEquals(AutomationPermissionState.Deny,
                service.resolve(stubOperation("run-pipeline", false), "p1"));
    }

    @Test
    void cliOverrideAllowWinsOverUserDeny() {
        AutomationPermissionService service = new AutomationPermissionService();
        service.setUserState("run-pipeline", AutomationPermissionState.Deny);
        service.setCliOverride(AutomationPermissionState.Allow, List.of("run-pipeline"));
        assertEquals(AutomationPermissionState.Allow,
                service.resolve(stubOperation("run-pipeline", false), "p1"));
    }

    @Test
    void grantAlwaysAppliesOnlyToGrantedProject() {
        AutomationPermissionService service = new AutomationPermissionService();
        service.grantAlways("run-pipeline", "p1");
        assertEquals(AutomationPermissionState.Allow,
                service.resolve(stubOperation("run-pipeline", false), "p1"));
        assertEquals(AutomationPermissionState.Ask,
                service.resolve(stubOperation("run-pipeline", false), "p2"));
    }

    @Test
    void revokeAllGrantsRestoresCategoryDefault() {
        AutomationPermissionService service = new AutomationPermissionService();
        service.grantAlways("run-pipeline", "p1");
        service.revokeAllGrants();
        assertEquals(AutomationPermissionState.Ask,
                service.resolve(stubOperation("run-pipeline", false), "p1"));
        assertEquals(AutomationPermissionState.Ask,
                service.resolve(stubOperation("run-pipeline", false), "p2"));
    }

    @Test
    void userHideResolvesHide() {
        AutomationPermissionService service = new AutomationPermissionService();
        service.setUserState("query-graph", AutomationPermissionState.Hide);
        assertEquals(AutomationPermissionState.Hide,
                service.resolve(stubOperation("query-graph", true), "p1"));
    }
}
