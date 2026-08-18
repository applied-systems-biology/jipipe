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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.hkijena.jipipe.utils.VersionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class ROCrateDockerSettings extends AbstractJIPipeParameterCollection {

    private String dockerImage = "appsysbiohkijena/jipipe";
    private String dockerTag = VersionUtils.getJIPipeVersion();
    private StringAndStringPairParameterList dockerEnvVars = defaultEnvVars();

    public ROCrateDockerSettings() {
    }

    public ROCrateDockerSettings(ROCrateDockerSettings other) {
        this.dockerImage = other.dockerImage;
        this.dockerTag = other.dockerTag;
        this.dockerEnvVars = new StringAndStringPairParameterList(other.dockerEnvVars);
    }

    private static StringAndStringPairParameterList defaultEnvVars() {
        StringAndStringPairParameterList list = new StringAndStringPairParameterList();
        list.add(new StringAndStringPairParameter("JAVA_TOOL_OPTIONS", "-Duser.home=/tmp -Djava.util.prefs.userRoot=/tmp/.java"));
        list.add(new StringAndStringPairParameter("XDG_CACHE_HOME", "/tmp/.cache"));
        list.add(new StringAndStringPairParameter("XDG_CONFIG_HOME", "/tmp/.config"));
        list.add(new StringAndStringPairParameter("XDG_DATA_HOME", "/tmp/.local/share"));
        return list;
    }

    @SetJIPipeDocumentation(name = "Docker image", description = "The Docker image name used in the CWL DockerRequirement.")
    @JIPipeParameter("docker-image")
    public String getDockerImage() {
        return dockerImage;
    }

    @JIPipeParameter("docker-image")
    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    @SetJIPipeDocumentation(name = "Docker tag", description = "The Docker image tag used in the CWL DockerRequirement.")
    @JIPipeParameter("docker-tag")
    public String getDockerTag() {
        return dockerTag;
    }

    @JIPipeParameter("docker-tag")
    public void setDockerTag(String dockerTag) {
        this.dockerTag = dockerTag;
    }

    @SetJIPipeDocumentation(name = "Docker environment variables", description = "Environment variables passed to the Docker container via the CWL EnvVarRequirement.")
    @JIPipeParameter("docker-env-vars")
    public StringAndStringPairParameterList getDockerEnvVars() {
        return dockerEnvVars;
    }

    @JIPipeParameter("docker-env-vars")
    public void setDockerEnvVars(StringAndStringPairParameterList dockerEnvVars) {
        this.dockerEnvVars = dockerEnvVars;
    }

    public Map<String, String> getEnvVarsAsMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (StringAndStringPairParameter pair : dockerEnvVars) {
            map.put(pair.getKey(), pair.getValue());
        }
        return map;
    }
}
