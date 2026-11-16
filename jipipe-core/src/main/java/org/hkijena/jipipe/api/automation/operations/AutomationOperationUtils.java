package org.hkijena.jipipe.api.automation.operations;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.project.JIPipeProject;

/**
 * Shared helpers for the built-in automation operations.
 */
final class AutomationOperationUtils {

    private AutomationOperationUtils() {
    }

    /**
     * Resolves a compartment by UUID, alias, or name.
     * {@link JIPipeProject#findCompartment(String)} matches UUIDs and aliases: this helper
     * additionally falls back to the display name.
     */
    static JIPipeProjectCompartment findCompartment(JIPipeProject project, String compartmentId) {
        var resolved = project.findCompartment(compartmentId);
        if (resolved != null) {
            return resolved;
        }
        for (var entry : project.getCompartments().entrySet()) {
            if (entry.getValue().getName() != null && entry.getValue().getName().equals(compartmentId)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
