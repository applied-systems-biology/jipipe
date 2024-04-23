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

package org.hkijena.jipipe.api.registries;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.plugins.artifacts.ArtifactSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JIPipeArtifactsRegistry {
    private final JIPipe jiPipe;
    private final JIPipeRunnableQueue queue = new JIPipeRunnableQueue("Artifacts");

    public JIPipeArtifactsRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
    }

    public JIPipe getJiPipe() {
        return jiPipe;
    }

    public JIPipeRunnableQueue getQueue() {
        return queue;
    }

    public Path getArtifactsPath() {
        if(ArtifactSettings.getInstance().getOverrideInstallationPath().isEnabled() && !ArtifactSettings.getInstance().getOverrideInstallationPath().getContent().toString().isEmpty()) {
            if(ArtifactSettings.getInstance().getOverrideInstallationPath().getContent().isAbsolute()) {
                return ArtifactSettings.getInstance().getOverrideInstallationPath().getContent();
            }
            else {
                return PathUtils.getJIPipeUserDir().resolve(ArtifactSettings.getInstance().getOverrideInstallationPath().getContent());
            }
        }
        else {
            if (SystemUtils.IS_OS_WINDOWS) {
               return Paths.get(System.getenv("APPDATA")).resolve("JIPipe")
                        .resolve("artifacts");
            } else if (SystemUtils.IS_OS_LINUX) {
                if (System.getProperties().containsKey("XDG_DATA_HOME") && !StringUtils.isNullOrEmpty(System.getProperty("XDG_DATA_HOME"))) {
                    return Paths.get(System.getProperty("XDG_DATA_HOME"))
                            .resolve("JIPipe")
                            .resolve("artifacts");
                } else {
                    return Paths.get(System.getProperty("user.home")).resolve(".local")
                            .resolve("share").resolve("JIPipe")
                            .resolve("artifacts");
                }
            } else if (SystemUtils.IS_OS_MAC_OSX) {
                return Paths.get(System.getProperty("user.home")).resolve("Library").resolve("Application Support")
                        .resolve("JIPipe").resolve("artifacts");
            } else {
                throw new UnsupportedOperationException("Unknown operating system!");
            }
        }
    }
}
