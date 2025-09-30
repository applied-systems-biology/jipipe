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

package org.hkijena.jipipe.api.artifacts.index;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryReference;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;

import java.util.Map;

/**
 * A class that allows to query a compatible repository reference
 */
public interface JIPipeRemoteArtifactDatabase {
    /**
     * Instructs the indexer to rebuild its internal cache.
     * Should be automatically called if there is currently none
     * @param progressInfo the progress info
     */
    void rebuild(JIPipeArtifactRepositoryReference repositoryReference, JIPipeProgressInfo progressInfo);

    /**
     * Queries the indexer
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @param progressInfo the progress info
     * @param repositoryReference the reference to the repository
     * @param downloadMap the map where the remote artifacts will be placed (by their unique ID)
     */
    void query(String groupId, String artifactId, String version, JIPipeProgressInfo progressInfo, JIPipeArtifactRepositoryReference repositoryReference, Map<String, JIPipeRemoteArtifact> downloadMap);
}
