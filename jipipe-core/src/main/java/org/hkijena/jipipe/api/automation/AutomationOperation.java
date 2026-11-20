package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

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
     * Returns the typed schema spec: the single override point and the source of truth from
     * which both the JSON schema ({@link #getInputSchema()}) and the parameter metadata
     * ({@link #getParameters()}) derive. Defaults to an empty spec (an operation that accepts
     * no parameters).
     * <p>
     * Reuse contract for consumers (the phase-4 agent harness): derive tool definitions from
     * {@link #getParameters()} or the AST; {@link #getInputSchema()} is always the derivation
     * of this method.
     */
    default AutomationSchemaSpec getSchemaSpec() {
        return AutomationSchemaSpec.object().build();
    }

    /**
     * Returns a simplified JSON schema describing the parameters accepted by this operation.
     * Validated by {@link AutomationSchemaUtils} before dispatch.
     * <p>
     * Defaults to the derivation of {@link #getSchemaSpec()}. Existing overrides still win
     * over the default, which keeps the migration to the spec layer incremental.
     */
    default JsonNode getInputSchema() {
        return getSchemaSpec().toJsonSchema();
    }

    /**
     * Returns the typed metadata of the operation's top-level parameters.
     * Defaults to the derivation of {@link #getSchemaSpec()}. Migrated operations return the
     * same immutable list instance on every call (the instance contract: consumers may cache it).
     */
    default List<AutomationParameterSpec> getParameters() {
        return getSchemaSpec().getParameters();
    }

    /**
     * Returns true if the registry fills absent optional parameters from the schema's
     * top-level {@code default} keywords before dispatch. Defaults to true (the JSON Schema
     * semantic).
     * <p>
     * Override to false when the operation's execute() distinguishes a parameter's ABSENCE
     * from its default value (for example a fail-fast refusal of a text-box-only parameter
     * on another kind). For such parameters the schema must not declare a default at all
     * (declaring one is a spec violation); until the schema is corrected, the override keeps
     * the absence semantics intact.
     * <p>
     * Plugin-author warning: default application is registry-wide, so it also reaches
     * operations registered by plugins. Before declaring a schema {@code default}, verify
     * that the operation's execute() treats the parameter's absence and its default value
     * identically; if it does not, do not declare the default.
     */
    default boolean appliesSchemaDefaults() {
        return true;
    }

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
     * operations that touch Swing without bridging it themselves (graph modifications whose
     * change events drive the UI, runs, open-window iteration) return true. The screenshot
     * window/ui operations are the exception: their Swing phases are bridged internally
     * ({@code callOnEdt}) and their Robot-based captures run off the EDT, because the
     * Wayland screencast portal can block the native capture indefinitely, which would
     * freeze the whole application. The MCP integration consults
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

    /**
     * Returns the {@link AutomationToolScope} that declares how this operation is exposed
     * as an MCP tool and how calls to it are routed. Defaults to
     * {@link AutomationToolScope#PROJECT_BOUND}.
     * <p>
     * Per the registry convention, the operation carries all information about itself:
     * brokers and descriptors read the scope from here instead of hardcoding operation
     * id sets.
     */
    default AutomationToolScope getToolScope() {
        return AutomationToolScope.PROJECT_BOUND;
    }
}
