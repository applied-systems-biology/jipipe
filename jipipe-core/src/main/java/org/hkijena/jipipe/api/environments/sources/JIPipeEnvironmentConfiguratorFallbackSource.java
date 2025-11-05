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

import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurator;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.plugins.parameters.api.optional.JIPipeOptionalParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.utils.ReflectionUtils;

public class JIPipeEnvironmentConfiguratorFallbackSource<T extends JIPipeEnvironment> implements JIPipeEnvironmentConfiguratorSource<T> {

    public JIPipeEnvironmentConfiguratorFallbackSource() {
    }

    @Override
    public JIPipeOptionalParameter<T> resolve(Class<T> environmentClass, JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo) {
        if (environmentInfo.isArtifact() && environmentInfo.hasArtifactQuery()) {
            JIPipeArtifactEnvironment environment = (JIPipeArtifactEnvironment) ReflectionUtils.newInstance(environmentClass);
            environment.setArtifactQuery(new JIPipeArtifactQueryParameter(environmentInfo.getArtifactQuery()));
            environment.setLoadFromArtifact(true);
            JIPipeOptionalParameter<T> result = (JIPipeOptionalParameter<T>) ReflectionUtils.newInstance(environmentInfo.getOptionalEnvironmentClass());
            result.setEnabled(true);
            result.setContent((T) environment);
            return result;
        }
        return null;
    }

    @Override
    public JIPipeEnvironmentConfigurator.SourceType getSourceType() {
        return JIPipeEnvironmentConfigurator.SourceType.Fallback;
    }

    @Override
    public Object getSource() {
        return null;
    }
}
