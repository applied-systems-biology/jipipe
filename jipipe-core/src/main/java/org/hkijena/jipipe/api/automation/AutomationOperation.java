package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for operations that can be executed via the automation layer.
 * Operations are registered in the {@link AutomationOperationRegistry}
 * and dispatched by the {@link org.hkijena.jipipe.api.instrumentation.InstrumentationServer}.
 */
public interface AutomationOperation {
    String getId();

    String getDescription();

    JsonNode execute(AutomationContext ctx, JsonNode params) throws Exception;
}
