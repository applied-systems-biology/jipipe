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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurator;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.plugins.parameters.api.optional.JIPipeOptionalParameter;

public class JIPipeEnvironmentConfiguratorCustomSource<T extends JIPipeEnvironment> implements JIPipeEnvironmentConfiguratorSource<T> {

    private final JIPipeOptionalParameter<T> parameter;
    private final Object source;

    public JIPipeEnvironmentConfiguratorCustomSource(T environment, Object source) {
        this.parameter = createOptionalParameter(environment);
        this.source = source;
    }

    public JIPipeEnvironmentConfiguratorCustomSource(JIPipeOptionalParameter<T> environment, Object source) {
        this.parameter = environment;
        this.source = source;
    }

    private JIPipeOptionalParameter<T> createOptionalParameter(T environment) {
        JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo = JIPipe.getEnvironments().getInfoByClass(environment.getClass());
        JIPipeOptionalParameter<T> o = (JIPipeOptionalParameter<T>) JIPipe.getParameterTypes().getInfoByFieldClass(environmentInfo.getOptionalEnvironmentClass()).newInstance();
        o.setContent(environment);
        o.setEnabled(true);
        return o;
    }

    @Override
    public JIPipeOptionalParameter<T> resolve(Class<T> environmentClass, JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo) {
        if(parameter != null && parameter.isEnabled() && environmentClass.isAssignableFrom(parameter.getContentClass())) {
            return parameter;
        }
        return null;
    }

    @Override
    public JIPipeEnvironmentConfigurator.SourceType getSourceType() {
        if(source instanceof JIPipeGraphNode) {
            return JIPipeEnvironmentConfigurator.SourceType.Node;
        }
        if(source instanceof JIPipeProject) {
            return JIPipeEnvironmentConfigurator.SourceType.Project;
        }
        return JIPipeEnvironmentConfigurator.SourceType.Custom;
    }

    @Override
    public Object getSource() {
        return source;
    }
}
