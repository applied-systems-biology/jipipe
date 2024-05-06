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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;

/**
 * An environment that can be filled with an artifact
 */
public abstract class JIPipeArtifactEnvironment extends JIPipeEnvironment {
    private boolean loadFromArtifact;
    private JIPipeArtifactQueryParameter artifactQuery = new JIPipeArtifactQueryParameter();

    public JIPipeArtifactEnvironment() {
    }

    public JIPipeArtifactEnvironment(JIPipeArtifactEnvironment other) {
        super(other);
        this.loadFromArtifact = other.loadFromArtifact;
        this.artifactQuery = other.artifactQuery;
    }

    @SetJIPipeDocumentation(name = "Load from artifact", description = "If enabled, this environment will be configured from an artifact. This is recommended " +
            "to ensure reproducibility.")
    @JIPipeParameter(value = "load-from-artifact", uiOrder = -199, important = true)
    public boolean isLoadFromArtifact() {
        return loadFromArtifact;
    }

    @JIPipeParameter("load-from-artifact")
    public void setLoadFromArtifact(boolean loadFromArtifact) {
        this.loadFromArtifact = loadFromArtifact;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Artifact", description = "The artifact to load for this environment")
    @JIPipeParameter(value = "artifact-query", uiOrder = -190, important = true)
    public JIPipeArtifactQueryParameter getArtifactQuery() {
        return artifactQuery;
    }

    @JIPipeParameter("artifact-query")
    public void setArtifactQuery(JIPipeArtifactQueryParameter artifactQuery) {
        this.artifactQuery = artifactQuery;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if("artifact-query".equals(access.getKey())) {
            return isLoadFromArtifact();
        }
        return super.isParameterUIVisible(tree, access);
    }
}
