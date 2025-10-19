package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;

/**
 * An interface for objects that support project upgrades
 */
public interface JIPipeProjectUpgradable {
    /**
     * Applies a project upgrade from the given JIPipe version to the current one
     *
     * @param fromVersion the previous version
     * @param context the validation report context
     * @param report the report for issues
     */
    void applyProjectUpgrade(String fromVersion, JIPipeValidationReportContext context, JIPipeValidationReport report);
}
