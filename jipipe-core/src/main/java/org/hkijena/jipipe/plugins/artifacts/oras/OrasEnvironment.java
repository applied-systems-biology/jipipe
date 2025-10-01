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

package org.hkijena.jipipe.plugins.artifacts.oras;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.environments.JIPipeProcessArtifactEnvironment;
import org.hkijena.jipipe.plugins.processes.ProcessEnvironment;

import javax.swing.*;

public class OrasEnvironment extends JIPipeProcessArtifactEnvironment {
    public OrasEnvironment() {
    }

    public OrasEnvironment(OrasEnvironment other) {
        super(other);
    }

    @Override
    public void applyConfigurationFromArtifact(JIPipeLocalArtifact artifact, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public Icon getNonArtifactIcon() {
        return null;
    }


}
