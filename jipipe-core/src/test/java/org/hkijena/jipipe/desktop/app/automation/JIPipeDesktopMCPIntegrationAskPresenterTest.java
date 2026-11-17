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

package org.hkijena.jipipe.desktop.app.automation;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the fail-closed contract of the MCP ask flow across the presenter seam: an
 * uninstalled integration denies every presented ask through the installed
 * {@link MCPAskPresenter}, so the waiting endpoint thread never blocks on a dead layer.
 * Also pins the ask origin default ("mcp" for asks that arrive through the two-argument
 * handler).
 */
class JIPipeDesktopMCPIntegrationAskPresenterTest {

    @Test
    void uninstallDeniesAllPendingAsksThroughThePresenter() throws Exception {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getOrCreate();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // The ask blocks (up to the ask bound) until the uninstall denies it.
            Future<Boolean> answered = executor.submit(() -> integration.ask("test_operation", null));

            long deadline = System.currentTimeMillis() + 10_000;
            while (integration.getPendingAsks().isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            assertEquals(1, integration.getPendingAsks().size(), "the ask must be presented");
            JIPipeDesktopMCPIntegration.PendingAsk pending = integration.getPendingAsks().get(0);
            assertEquals("test_operation", pending.getOperationId());
            assertNull(pending.getProjectKey());
            assertEquals("mcp", pending.getOrigin(), "the two-argument handler reports the default MCP origin");

            JIPipeDesktopMCPIntegration.uninstall();

            assertFalse(answered.get(10, TimeUnit.SECONDS),
                    "an uninstalled integration must deny its pending asks");
            assertTrue(integration.getPendingAsks().isEmpty(), "the denied ask must leave the presentation");
        } finally {
            executor.shutdownNow();
        }
    }
}
