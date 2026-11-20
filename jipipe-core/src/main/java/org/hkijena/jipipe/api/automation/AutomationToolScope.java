package org.hkijena.jipipe.api.automation;

/**
 * Declares how an automation operation is exposed as an MCP tool and how calls to the
 * tool are routed. Every operation declares its scope, so the broker routes tools from
 * the machine-readable declaration instead of hardcoding operation id sets.
 */
public enum AutomationToolScope {
    /**
     * The tool operates on one project: its schema receives the injected project
     * argument, and calls route to the instance that carries the addressed project.
     */
    PROJECT_BOUND,
    /**
     * The tool does not address a single project: project-less calls route to the
     * most-recent live instance carrying the tool (the E2E ruling 2026-11-16
     * semantics, now declared by the tool itself).
     */
    INSTANCE_GLOBAL,
    /**
     * The tool is answered by the broker, which aggregates the answers of every live
     * instance carrying the tool and rewrites the ids to routable form.
     */
    INSTANCE_GLOBAL_AGGREGATED,
    /**
     * The operation is registered but excluded from the {@code tools/list} catalog.
     */
    NOT_A_TOOL
}
