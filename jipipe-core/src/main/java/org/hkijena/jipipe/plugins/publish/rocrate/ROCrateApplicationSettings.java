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

package org.hkijena.jipipe.plugins.publish.rocrate;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.hkijena.jipipe.utils.VersionUtils;

import javax.swing.*;

public class ROCrateApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static final String ID = "org.hkijena.jipipe:publish:ro-crate";

    private String dockerImage = "appsysbiohkijena/jipipe";
    private String dockerTag = VersionUtils.getJIPipeVersion();
    private StringAndStringPairParameterList dockerEnvVars = defaultEnvVars();

    public ROCrateApplicationSettings() {
    }

    private static StringAndStringPairParameterList defaultEnvVars() {
        StringAndStringPairParameterList list = new StringAndStringPairParameterList();
        list.add(new StringAndStringPairParameter("JAVA_TOOL_OPTIONS", "-Duser.home=/tmp -Djava.util.prefs.userRoot=/tmp/.java"));
        list.add(new StringAndStringPairParameter("XDG_CACHE_HOME", "/tmp/.cache"));
        list.add(new StringAndStringPairParameter("XDG_CONFIG_HOME", "/tmp/.config"));
        list.add(new StringAndStringPairParameter("XDG_DATA_HOME", "/tmp/.local/share"));
        return list;
    }

    public static ROCrateApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, ROCrateApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Docker image", description = "The default Docker image name used in the CWL DockerRequirement when creating RO-Crates.")
    @JIPipeParameter("docker-image")
    public String getDockerImage() {
        return dockerImage;
    }

    @JIPipeParameter("docker-image")
    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    @SetJIPipeDocumentation(name = "Docker tag", description = "The default Docker image tag used in the CWL DockerRequirement when creating RO-Crates.")
    @JIPipeParameter("docker-tag")
    public String getDockerTag() {
        return dockerTag;
    }

    @JIPipeParameter("docker-tag")
    public void setDockerTag(String dockerTag) {
        this.dockerTag = dockerTag;
    }

    @SetJIPipeDocumentation(name = "Docker environment variables", description = "The default environment variables passed to the Docker container via the CWL EnvVarRequirement.")
    @JIPipeParameter("docker-env-vars")
    public StringAndStringPairParameterList getDockerEnvVars() {
        return dockerEnvVars;
    }

    @JIPipeParameter("docker-env-vars")
    public void setDockerEnvVars(StringAndStringPairParameterList dockerEnvVars) {
        this.dockerEnvVars = dockerEnvVars;
    }

    public ROCrateDockerSettings toDockerSettings() {
        ROCrateDockerSettings settings = new ROCrateDockerSettings();
        settings.setDockerImage(dockerImage);
        settings.setDockerTag(dockerTag);
        settings.setDockerEnvVars(new StringAndStringPairParameterList(dockerEnvVars));
        return settings;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Plugins;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("apps/ro-crate.png");
    }

    @Override
    public String getName() {
        return "RO-Crate";
    }

    @Override
    public String getDescription() {
        return "Settings for RO-Crate creation.";
    }
}
