package org.hkijena.jipipe.api.automation;

/**
 * Functional category of an automation operation, shown in tool listings.
 */
public enum AutomationOperationCategory {
    /**
     * Reads information from the project without modifying it.
     */
    Query,
    /**
     * Modifies the project (graph, parameters, metadata).
     */
    Modify,
    /**
     * Runs the pipeline or parts of it.
     */
    Execute,
    /**
     * Renders or exports visual artifacts.
     */
    Render,
    /**
     * Introspects the automation layer itself.
     */
    Meta
}
