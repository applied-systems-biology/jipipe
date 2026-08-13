package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Extension of {@link InstrumentationOperation} for long-running operations.
 * The {@link InstrumentationServer} detects async operations and returns a
 * job ID immediately, then broadcasts job progress and completion events.
 */
public interface AsyncInstrumentationOperation extends InstrumentationOperation {
    /**
     * Starts the async operation. Returns immediately with a job handle.
     * Progress and completion are reported via the job's event callbacks.
     */
    InstrumentationJob executeAsync(InstrumentationContext ctx, JsonNode params) throws Exception;
}
