package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Extension of {@link AutomationOperation} for long-running operations.
 * The {@link org.hkijena.jipipe.api.instrumentation.InstrumentationServer} detects async operations and returns a
 * job ID immediately, then broadcasts job progress and completion events.
 */
public interface AsyncAutomationOperation extends AutomationOperation {
    /**
     * Starts the async operation. Returns immediately with a job handle.
     * Progress and completion are reported via the job's event callbacks.
     */
    InstrumentationJob executeAsync(AutomationContext ctx, JsonNode params) throws Exception;
}
