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

package org.hkijena.jipipe.api.environments.sources;

import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurator;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.plugins.parameters.api.optional.JIPipeOptionalParameter;
import org.hkijena.jipipe.plugins.settings.project.JIPipeDefaultEnvironmentsProjectSettings;

public class JIPipeEnvironmentConfiguratorProjectSource<T extends JIPipeEnvironment> implements JIPipeEnvironmentConfiguratorSource<T> {

    private final JIPipeProject project;

    public JIPipeEnvironmentConfiguratorProjectSource(JIPipeProject project) {
        this.project = project;
    }

    @Override
    public JIPipeOptionalParameter<T> resolve(Class<T> environmentClass, JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo) {
        JIPipeParameterAccess access = project.getSettingsSheet(JIPipeDefaultEnvironmentsProjectSettings.class).get(environmentInfo.getId());
        if (access != null) {
            JIPipeOptionalParameter<?> parameter = access.get(JIPipeOptionalParameter.class);
            if (parameter != null && parameter.isEnabled() && parameter.getContent() instanceof JIPipeEnvironment
                    && environmentClass.isAssignableFrom(parameter.getContent().getClass())) {
                return (JIPipeOptionalParameter<T>) parameter;
            }
        }
        return null;
    }

    @Override
    public JIPipeEnvironmentConfigurator.SourceType getSourceType() {
        return JIPipeEnvironmentConfigurator.SourceType.Project;
    }

    @Override
    public Object getSource() {
        return project;
    }

    public JIPipeProject getProject() {
        return project;
    }
}
