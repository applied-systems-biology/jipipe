package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for operations that can be executed via the instrumentation layer.
 * Operations are registered in the {@link InstrumentationOperationRegistry}
 * and dispatched by the {@link InstrumentationServer}.
 */
public interface InstrumentationOperation {
    String getId();
    String getDescription();
    JsonNode execute(InstrumentationContext ctx, JsonNode params) throws Exception;
}
