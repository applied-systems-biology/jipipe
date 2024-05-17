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

package org.hkijena.jipipe.plugins.omero;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;

/**
 * Interface that should be used by nodes that access the OMERO credentials environments
 */
public interface OMEROCredentialAccessNode {
    OptionalOMEROCredentialsEnvironment getOverrideCredentials();

    /**
     * Gets the correct OMERO environment.
     * Adheres to the chain of overrides.
     *
     * @return the environment
     */
    default OMEROCredentialsEnvironment getConfiguredOMEROCredentialsEnvironment() {
        JIPipeGraphNode node = (JIPipeGraphNode) this;
        JIPipeProject project = node.getRuntimeProject();
        if (project == null) {
            project = node.getParentGraph().getProject();
        }
        return OMEROPlugin.getEnvironment(project, getOverrideCredentials());
    }

    /**
     * Generates a validity report entry if the configured environment has an issue
     *
     * @param context the context
     * @param report  the report
     */
    default void reportConfiguredOMEROEnvironmentValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (!getConfiguredOMEROCredentialsEnvironment().generateValidityReport(context).isValid()) {
            report.report(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    context,
                    "OMERO credentials not configured",
                    "The OMERO integration is not configured correctly.",
                    "Go to the Project > Project settings/overview > Settings > Plugins > OMERO and setup appropriate default OMERO credentials."));
        }
    }
}
