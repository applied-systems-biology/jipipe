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
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ProjectSettingsValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;

/**
 * Helper class that contains a JIPipeEnvironment together with information about its source
 */
public class JIPipeEnvironmentReference<T extends JIPipeEnvironment> implements JIPipeValidatable {
    private final T environment;
    private final SourceType sourceType;
    private final Object source;

    public JIPipeEnvironmentReference(T environment, SourceType sourceType, Object source) {
        this.environment = environment;
        this.sourceType = sourceType;
        this.source = source;
    }

    /**
     * Selects a reference based on the default chain (node, project, application)
     *
     * @return the selector
     */
    public static <T extends JIPipeEnvironment> DefaultSelector<T> defaultOptions(Class<T> environmentClass) {
        return new DefaultSelector<T>();
    }

    public T getEnvironment() {
        return environment;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public Object getSource() {
        return source;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report) {
         if(!getEnvironment().generateValidityReport(new UnspecifiedValidationReportContext(), reportSettings).isValid()) {
                JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(getEnvironment().getClass());
                switch (getSourceType()){
                    case SourceType.Application -> {
                        report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                                new UnspecifiedValidationReportContext(),
                                "Misconfigured environment",
                                "An application-wide environment of the type '" + info.getName() + "' is invalid. The project cannot to be run.",
                                "Please go into the JIPipe application settings and find the configuration for '" + info.getName() + "'. Ensure that the environment is correctly configured."));
                    }
                    case SourceType.Project -> {
                        var context = getSource() instanceof JIPipeProject ? reportContext.projectSettings((JIPipeProject)getSource()) : JIPipeValidationReportContext.UNSPECIFIED;
                        report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                                context,
                                "Misconfigured environment",
                                "A project environment of the type '" + info.getName() + "' is invalid. The project cannot to be run.",
                                "Please go to Project > Project settings and find the configuration for '" + info.getName() + "'. Ensure that the environment is correctly configured."));
                    }
                    case SourceType.Node -> {
                        var context = getSource() instanceof JIPipeGraphNode ? reportContext.node((JIPipeGraphNode)getSource()) : JIPipeValidationReportContext.UNSPECIFIED;
                        report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                                context,
                                "Misconfigured environment",
                                "A project environment of the type '" + info.getName() + "' is invalid. The project cannot to be run.",
                                "Please go to Project > Project settings and find the configuration for '" + info.getName() + "'. Ensure that the environment is correctly configured."));
                    }
                }
            }
    }

    public enum SourceType {
        Node,
        Project,
        Application
    }

    public static final class DefaultSelector<T extends JIPipeEnvironment> {
        private final JIPipeEnvironmentReference<T>[] chain = new JIPipeEnvironmentReference[4];

        public DefaultSelector<T> applicationDefault(T environment) {
            chain[3] = new JIPipeEnvironmentReference<>(environment, SourceType.Application, null);
            return this;
        }

        public DefaultSelector<T> application(T environment) {
            chain[2] = new JIPipeEnvironmentReference<>(environment, SourceType.Application, null);
            return this;
        }

        public DefaultSelector<T> application(OptionalParameter<T> environment) {
            if (environment.isEnabled()) {
                chain[2] = new JIPipeEnvironmentReference<>(environment.getContent(), SourceType.Application, null);
            }
            return this;
        }

        public DefaultSelector<T> project(T environment, JIPipeProject project) {
            if (environment != null) {
                chain[1] = new JIPipeEnvironmentReference<>(environment, SourceType.Project, project);
            }
            return this;
        }

        public DefaultSelector<T> project(OptionalParameter<T> environment, JIPipeProject project) {
            if (environment != null && environment.isEnabled()) {
                chain[1] = new JIPipeEnvironmentReference<>(environment.getContent(), SourceType.Project, project);
            }
            return this;
        }

        public DefaultSelector<T> node(T environment, JIPipeGraphNode node) {
            if (environment != null) {
                chain[0] = new JIPipeEnvironmentReference<>(environment, SourceType.Node, node);
            }
            return this;
        }

        public DefaultSelector<T> node(OptionalParameter<T> environment, JIPipeGraphNode node) {
            if (environment != null && environment.isEnabled()) {
                chain[0] = new JIPipeEnvironmentReference<>(environment.getContent(), SourceType.Node, node);
            }
            return this;
        }

        public JIPipeEnvironmentReference<T> select() {
            for (JIPipeEnvironmentReference<T> reference : chain) {
                if (reference != null) {
                    return reference;
                }
            }
            return null;
        }
    }
}
