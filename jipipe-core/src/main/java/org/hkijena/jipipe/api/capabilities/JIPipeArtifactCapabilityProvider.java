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

package org.hkijena.jipipe.api.capabilities;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.service.components.JIPipeArtifactsServiceComponent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A capability provider backed by an installed artifact.
 * <p>
 * This is the engine-level building block for any capability that is satisfied
 * by a downloadable artifact from the local artifact repository (an embedding
 * model, an upscaler, a decision model, ...). The probe reports
 * {@link JIPipeCapabilityStatus.State#AVAILABLE} when the local artifact
 * repository holds an installed artifact whose coordinates start with the
 * artifact query prefix, and
 * {@link JIPipeCapabilityStatus.State#UNAVAILABLE} otherwise.
 * <p>
 * The probe is read-only and consults the cached local artifacts on every
 * call: it never triggers a download. The prefix is the coordinate prefix
 * without version or classifier (e.g. {@code cn.ac.baai.bge_small_en}), so it
 * matches every version and classifier of the artifact.
 * <p>
 * The kind is fixed to {@link JIPipeProviderKind#ARTIFACT}; the capability ids
 * the provider satisfies and its install hint are set through
 * {@link #provides(String...)} and {@link #installHint(JIPipeInstallHint)}
 * before registration.
 */
public final class JIPipeArtifactCapabilityProvider extends JIPipeCapabilityProvider {
    /**
     * The artifact coordinate prefix this provider is satisfied by, e.g.
     * {@code cn.ac.baai.bge_small_en}.
     */
    private final String artifactQueryPrefix;

    /**
     * Creates an artifact-backed provider.
     *
     * @param artifactQueryPrefix the coordinate prefix (group and artifact id,
     *                            without version and classifier) that an installed
     *                            local artifact must start with for the provider
     *                            to be available
     */
    public JIPipeArtifactCapabilityProvider(String artifactQueryPrefix) {
        super(JIPipeProviderKind.ARTIFACT, probe(Objects.requireNonNull(artifactQueryPrefix, "artifactQueryPrefix")));
        this.artifactQueryPrefix = artifactQueryPrefix;
    }

    /**
     * @return the artifact coordinate prefix this provider is satisfied by
     */
    public String getArtifactQueryPrefix() {
        return artifactQueryPrefix;
    }

    /**
     * @return a presence probe that reports
     *         {@link JIPipeCapabilityStatus.State#AVAILABLE} when the local
     *         artifact repository holds an installed artifact whose full id
     *         starts with {@code artifactQueryPrefix}
     */
    private static Supplier<JIPipeCapabilityStatus.State> probe(String artifactQueryPrefix) {
        return () -> {
            if (JIPipe.getInstance() == null) {
                return JIPipeCapabilityStatus.State.UNAVAILABLE;
            }
            JIPipeArtifactsServiceComponent artifacts = JIPipe.getArtifacts();
            for (JIPipeLocalArtifact artifact : artifacts.getCachedLocalArtifacts().values()) {
                if (artifact.getFullId().startsWith(artifactQueryPrefix)) {
                    return JIPipeCapabilityStatus.State.AVAILABLE;
                }
            }
            return JIPipeCapabilityStatus.State.UNAVAILABLE;
        };
    }
}
