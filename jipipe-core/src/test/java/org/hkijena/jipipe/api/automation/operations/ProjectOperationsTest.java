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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the get_server_info health probe: the pinned response shape (JIPipe version,
 * open project count, registered operation count, server time) executed directly over
 * a null-project (headless) context, because the probe is callable without a project
 * and ignores the context entirely.
 */
public class ProjectOperationsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    /**
     * A null-project context: the health probe never touches it.
     */
    private static AutomationContext createContext() {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(null, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    @Test
    void serverInfoReportsVersionCountsAndTime() {
        long now = System.currentTimeMillis();

        JsonNode info = new ProjectOperations.GetServerInfo().execute(createContext(), mapper.createObjectNode());

        // The pinned response shape: exactly these four keys
        Set<String> keys = new HashSet<>();
        info.fieldNames().forEachRemaining(keys::add);
        assertEquals(Set.of("jipipeVersion", "projects", "operations", "serverTime"), keys);

        assertFalse(info.get("jipipeVersion").asText().isBlank(), "the JIPipe version must be non-blank");
        assertTrue(info.get("operations").asInt() >= 1, "at least the registered operations must be counted");
        assertTrue(info.get("projects").asInt() >= 0, "the open project count must never be negative (headless: 0)");
        assertTrue(Math.abs(info.get("serverTime").asLong() - now) <= 60_000,
                "the server time must be within 60 seconds of the current time");
    }
}
