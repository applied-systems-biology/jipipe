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

package org.hkijena.jipipe.api.environments;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;

/**
 * Class that helps with keeping track where environments are sourced from and resolve full environments
 */
public class JIPipeEnvironmentConfigurator<T extends JIPipeEnvironment> implements JIPipeValidatable {
    private final Class<T> environmentClass;
    private final JIPipeProject project;
    private final JIPipeEnvironmentConfigurationCache configurationCache;
    private T baseEnvironment;
    private SourceType sourceType;
    private Object source;

    public JIPipeEnvironmentConfigurator(Class<T> environmentClass, JIPipeProject project, JIPipeEnvironmentConfigurationCache configurationCache) {
        this.environmentClass = environmentClass;
        this.project = project;
        this.configurationCache = configurationCache;
    }

    public T get(JIPipeProgressInfo progressInfo) {
        resolveBaseEnvironment();
        return null; // TODO
    }

    /**
     * Ensures that the base (unconfigured) environment is selected
     */
    public void resolveBaseEnvironment() {

    }

    public SourceType getSourceType() {
        resolveBaseEnvironment();
        return sourceType;
    }

    public Object getSource() {
        resolveBaseEnvironment();
        return source;
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeEnvironmentConfigurationCache getConfigurationCache() {
        return configurationCache;
    }

    public T getBaseEnvironment() {
        resolveBaseEnvironment();
        return baseEnvironment;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        resolveBaseEnvironment();
        if (!get(progressInfo).generateValidityReport(new UnspecifiedValidationReportContext(), reportSettings, progressInfo).isValid()) {
            JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(get(progressInfo).getClass());
            switch (getSourceType()) {
                case SourceType.Application -> {
                    new UnspecifiedValidationReportContext().error()
                            .title("Misconfigured environment")
                            .explanation("An application-wide environment of the type '" + info.getName() + "' is invalid. The project cannot to be run.")
                            .solution("Please go into the JIPipe application settings and find the configuration for '" + info.getName() + "'. Ensure that the environment is correctly configured.")
                            .report(report);
                }
                case SourceType.Project -> {
                    var context = getSource() instanceof JIPipeProject ? reportContext.projectSettings((JIPipeProject) getSource()) : JIPipeValidationReportContext.UNSPECIFIED;
                    context.error()
                            .title("Misconfigured environment")
                            .explanation("A project environment of the type '" + info.getName() + "' is invalid. The project cannot to be run.")
                            .solution("Please go to Project > Project settings and find the configuration for '" + info.getName() + "'. Ensure that the environment is correctly configured.")
                            .report(report);
                }
                case SourceType.Node -> {
                    var context = getSource() instanceof JIPipeGraphNode ? reportContext.node((JIPipeGraphNode) getSource()) : JIPipeValidationReportContext.UNSPECIFIED;
                    context.error()
                            .title("Misconfigured environment")
                            .explanation("A project environment of the type '" + info.getName() + "' is invalid. The project cannot to be run.")
                            .solution("Please go to Project > Project settings and find the configuration for '" + info.getName() + "'. Ensure that the environment is correctly configured.")
                            .report(report);
                }
            }
        }
    }

    public Class<T> getEnvironmentClass() {
        return environmentClass;
    }

    public enum SourceType {
        Node,
        Project,
        Application
    }
}
