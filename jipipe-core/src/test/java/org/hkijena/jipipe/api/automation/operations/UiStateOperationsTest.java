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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests the headless guard paths of the UI state operations: the no-project refusal of
 * fit_to_nodes and clear_selection, and the unknown-compartment refusal of fit_to_nodes.
 * The window-resolution and the view-state application paths need a live desktop and are
 * deliberately untested (the user verifies them in the app).
 */
public class UiStateOperationsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static AutomationContext createContext(JIPipeProject project) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(project, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    private static JIPipeProject createProject() {
        JIPipeProject project = new JIPipeProject();
        project.addCompartment("Screenshot Compartment");
        return project;
    }

    @Test
    void fitToNodesWithoutProjectReturnsError() {
        AutomationContext ctx = createContext(null);
        ObjectNode params = mapper.createObjectNode();
        var result = new UiStateOperations.FitToNodes().execute(ctx, params);
        assertEquals("No project loaded", result.get("error").asText());
        assertFalse(result.has("zoom"));
        assertFalse(result.has("nodes"));
    }

    @Test
    void clearSelectionWithoutProjectReturnsError() {
        AutomationContext ctx = createContext(null);
        ObjectNode params = mapper.createObjectNode();
        var result = new UiStateOperations.ClearSelection().execute(ctx, params);
        assertEquals("No project loaded", result.get("error").asText());
        assertFalse(result.has("cleared"));
    }

    @Test
    void fitToNodesWithUnknownCompartmentReturnsError() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        ObjectNode params = mapper.createObjectNode().put("compartment", "missing");
        var result = new UiStateOperations.FitToNodes().execute(ctx, params);
        assertEquals("Compartment not found: missing", result.get("error").asText());
        assertFalse(result.has("tab_opened"));
    }
}
