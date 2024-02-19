/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.omero;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.ArrayList;
import java.util.List;

public class OMEROSettings extends AbstractJIPipeParameterCollection implements ExternalEnvironmentSettings {
    public static final String ID = "org.hkijena.jipipe:omero";

    private OMEROCredentialsEnvironment defaultCredentials = new OMEROCredentialsEnvironment();
    private OMEROCredentialsEnvironment.List presets = new OMEROCredentialsEnvironment.List();

    public static OMEROSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, OMEROSettings.class);
    }

    @SetJIPipeDocumentation(name = "Default credentials", description = "The default credentials for the OMERO server")
    @JIPipeParameter("default-credentials")
    public OMEROCredentialsEnvironment getDefaultCredentials() {
        return defaultCredentials;
    }

    @JIPipeParameter("default-credentials")
    public void setDefaultCredentials(OMEROCredentialsEnvironment defaultCredentials) {
        this.defaultCredentials = defaultCredentials;
    }

    @SetJIPipeDocumentation(name = "Presets", description = "The list of presets")
    @JIPipeParameter("presets")
    public OMEROCredentialsEnvironment.List getPresets() {
        return presets;
    }

    @JIPipeParameter("presets")
    public void setPresets(OMEROCredentialsEnvironment.List presets) {
        this.presets = presets;
    }

    @Override
    public List<JIPipeEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return new ArrayList<>(presets);
    }

    @Override
    public void setPresetsListInterface(List<JIPipeEnvironment> presets, Class<?> environmentClass) {
        this.presets.clear();
        for (JIPipeEnvironment preset : presets) {
            this.presets.add((OMEROCredentialsEnvironment) preset);
        }
    }
}
