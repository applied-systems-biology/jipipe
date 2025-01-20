/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.python.algorithms.python;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.python.CorePythonPlugin;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonPlugin;
import org.hkijena.jipipe.plugins.python.adapter.JIPipePythonAdapterLibraryEnvironment;

/**
 * Interface that should be used by nodes that access the OMERO credentials environments
 */
public interface PythonEnvironmentAccessNode {
    OptionalPythonEnvironment getOverrideEnvironment();

    /**
     * Gets the correct Python environment.
     * Adheres to the chain of overrides.
     *
     * @return the environment
     */
    default PythonEnvironment getConfiguredPythonEnvironment() {
        JIPipeGraphNode node = (JIPipeGraphNode) this;
        JIPipeProject project = node.getRuntimeProject();
        if (project == null) {
            project = node.getParentGraph().getProject();
        }
        return CorePythonPlugin.getEnvironment(project, getOverrideEnvironment());
    }

    /**
     * Gets the correct JIPipe Python adapter environment.
     * Adheres to the chain of overrides.
     *
     * @return the environment
     */
    default JIPipePythonAdapterLibraryEnvironment getConfiguredPythonAdapterEnvironment() {
        JIPipeGraphNode node = (JIPipeGraphNode) this;
        JIPipeProject project = node.getRuntimeProject();
        if (project == null) {
            project = node.getParentGraph().getProject();
        }
        return CorePythonPlugin.getAdapterEnvironment(project);
    }

    /**
     * Generates a validity report entry if the configured environment has an issue
     *
     * @param context the context
     * @param report  the report
     */
    default void reportConfiguredPythonEnvironmentValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (!getConfiguredPythonEnvironment().generateValidityReport(context).isValid()) {
            report.report(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    context,
                    "Python not configured",
                    "The Python integration is not configured correctly.",
                    "Go to the Project > Project settings/overview > Settings > Plugins > Python and setup an appropriate default Python environment."));
        }
        if (!getConfiguredPythonAdapterEnvironment().generateValidityReport(context).isValid()) {
            report.report(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    context,
                    "Python adapter not configured",
                    "The JIPipe Python adapter is not configured correctly.",
                    "Go to the Project > Project settings/overview > Settings > Plugins > Python and setup the Python adapter."));
        }
    }
}
