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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;

/**
 * An environment that can be filled with an artifact
 */
public abstract class JIPipeArtifactEnvironment extends JIPipeEnvironment {
    private boolean loadFromArtifact;
    private JIPipeLocalArtifact lastConfiguredArtifact;
    private JIPipeArtifactQueryParameter artifactQuery = new JIPipeArtifactQueryParameter();

    public JIPipeArtifactEnvironment() {
    }

    public JIPipeArtifactEnvironment(JIPipeArtifactEnvironment other) {
        super(other);
        this.loadFromArtifact = other.loadFromArtifact;
        this.artifactQuery = new JIPipeArtifactQueryParameter(other.artifactQuery);
        this.lastConfiguredArtifact = other.lastConfiguredArtifact;
    }

    @SetJIPipeDocumentation(name = "Load from artifact", description = "If enabled, this environment will be configured from an artifact. This is recommended " +
            "to ensure reproducibility.")
    @JIPipeParameter(value = "load-from-artifact", uiOrder = -199, important = true)
    @JsonGetter("load-from-artifact")
    public boolean isLoadFromArtifact() {
        return loadFromArtifact;
    }

    @JIPipeParameter("load-from-artifact")
    @JsonSetter("load-from-artifact")
    public void setLoadFromArtifact(boolean loadFromArtifact) {
        this.loadFromArtifact = loadFromArtifact;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Artifact", description = "The artifact to load for this environment")
    @JIPipeParameter(value = "artifact-query", uiOrder = -190, important = true)
    @JsonGetter("artifact-query")
    public JIPipeArtifactQueryParameter getArtifactQuery() {
        return artifactQuery;
    }

    @JIPipeParameter("artifact-query")
    @JsonSetter("artifact-query")
    public void setArtifactQuery(JIPipeArtifactQueryParameter artifactQuery) {
        this.artifactQuery = artifactQuery;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if ("artifact-query".equals(access.getKey())) {
            return isLoadFromArtifact();
        }
        return super.isParameterUIVisible(tree, access);
    }

    /**
     * Applies the artifact configuration to the current environment if isLoadFromArtifact() is true
     * Also sets the last loaded artifact
     *
     * @param artifact     the artifact
     * @param progressInfo the progress info
     */
    public void applyConfigurationFromArtifactAndSetLastArtifact(JIPipeLocalArtifact artifact, JIPipeProgressInfo progressInfo) {
        this.lastConfiguredArtifact = artifact;
        applyConfigurationFromArtifact(artifact, progressInfo);
    }

    /**
     * Applies the artifact configuration to the current environment if isLoadFromArtifact() is true
     */
    public abstract void applyConfigurationFromArtifact(JIPipeLocalArtifact artifact, JIPipeProgressInfo progressInfo);

    public JIPipeLocalArtifact getLastConfiguredArtifact() {
        return lastConfiguredArtifact;
    }

    public void setLastConfiguredArtifact(JIPipeLocalArtifact lastConfiguredArtifact) {
        this.lastConfiguredArtifact = lastConfiguredArtifact;
    }
}
