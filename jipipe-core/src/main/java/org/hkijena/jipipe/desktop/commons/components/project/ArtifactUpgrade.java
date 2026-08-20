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

package org.hkijena.jipipe.desktop.commons.components.project;

import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;

import java.util.List;

public class ArtifactUpgrade {
    private final JIPipeArtifactEnvironment environment;
    private final JIPipeArtifact current;
    private final List<JIPipeArtifact> revisionUpgrades;
    private final List<JIPipeArtifact> accelerationUpgrades;

    public ArtifactUpgrade(JIPipeArtifactEnvironment environment, JIPipeArtifact current, List<JIPipeArtifact> revisionUpgrades, List<JIPipeArtifact> accelerationUpgrades) {
        this.environment = environment;
        this.current = current;
        this.revisionUpgrades = revisionUpgrades;
        this.accelerationUpgrades = accelerationUpgrades;
    }

    public JIPipeArtifactEnvironment getEnvironment() {
        return environment;
    }

    public JIPipeArtifact getCurrent() {
        return current;
    }

    public List<JIPipeArtifact> getRevisionUpgrades() {
        return revisionUpgrades;
    }

    public List<JIPipeArtifact> getAccelerationUpgrades() {
        return accelerationUpgrades;
    }
}
