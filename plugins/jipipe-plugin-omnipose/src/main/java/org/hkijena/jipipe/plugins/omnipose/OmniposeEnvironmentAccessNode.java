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

package org.hkijena.jipipe.plugins.omnipose;

import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurator;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;

/**
 * Interface that should be used by nodes that access the OMERO credentials environments
 */
public interface OmniposeEnvironmentAccessNode {
    OptionalPythonEnvironment getOverrideEnvironment();

    /**
     * Gets the correct Python environment.
     * Adheres to the chain of overrides.
     *
     * @return the environment
     */
    default JIPipeEnvironmentConfigurator<PythonEnvironment> getConfiguredOmniposeEnvironment() {
        JIPipeGraphNode node = (JIPipeGraphNode) this;
        JIPipeProject project = node.getRuntimeProject();
        if (project == null) {
            project = node.getParentGraph().getProject();
        }
        return OmniposePlugin.getEnvironment(project, getOverrideEnvironment(), node);
    }

    /**
     * Generates a validity report entry if the configured environment has an issue
     *
     * @param context the context
     * @param report  the report
     */
    default void reportConfiguredOmniposeEnvironmentValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (!getConfiguredOmniposeEnvironment().get(progressInfo).generateValidityReport(context, JIPipeValidationReportSettings.DEFAULT).isValid()) {
            context.error().title("Omnipose not configured").explanation("The Omnipose integration is not configured correctly.").solution("Go to the Project > Project settings/overview > Settings > Plugins > Omnipose and setup an appropriate default Omnipose environment.").report(report);
        }
    }
}
