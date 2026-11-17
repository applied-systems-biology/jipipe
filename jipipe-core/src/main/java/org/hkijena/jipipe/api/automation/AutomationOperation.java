package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for operations that can be executed via the automation layer.
 * Operations are registered in the {@link AutomationOperationRegistry}
 * and dispatched by the {@link org.hkijena.jipipe.api.automation.endpoint.AutomationInstanceEndpoint}.
 */
public interface AutomationOperation {
    String getId();

    String getDescription();

    JsonNode execute(AutomationContext ctx, JsonNode params) throws Exception;

    /**
     * Returns a simplified JSON schema describing the parameters accepted by this operation.
     * Validated by {@link AutomationSchemaUtils} before dispatch.
     */
    JsonNode getInputSchema();

    /**
     * Returns the functional category shown in tool listings.
     * Defaults to {@link AutomationOperationCategory#Meta} for operations that do not declare one.
     */
    default AutomationOperationCategory getCategory() {
        return AutomationOperationCategory.Meta;
    }

    /**
     * Returns true if the operation does not modify the project.
     * Defaults to true for operations that do not declare themselves.
     */
    default boolean isReadOnly() {
        return true;
    }

    /**
     * Returns true if the operation must be dispatched on the Swing event dispatch thread:
     * operations that touch Swing (graph modifications whose change events drive the UI,
     * runs, screenshots, open-window iteration) return true. The MCP integration consults
     * this instead of a hardcoded operation id set.
     */
    default boolean requiresEdt() {
        return false;
    }

    /**
     * Returns documentation targeted at automation agents.
     * Defaults to {@link #getDescription()}.
     */
    default String getAgentDocumentation() {
        return getDescription();
    }
}
