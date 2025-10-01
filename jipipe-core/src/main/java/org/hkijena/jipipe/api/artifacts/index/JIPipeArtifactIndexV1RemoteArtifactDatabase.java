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
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryReference;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryType;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.api.artifacts.index.v1.*;
import org.hkijena.jipipe.api.artifacts.sources.JIPipeHttpRemoteArtifactSource;
import org.hkijena.jipipe.api.artifacts.sources.JIPipeOrasRemoteArtifactSource;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

public class JIPipeArtifactIndexV1RemoteArtifactDatabase implements JIPipeRemoteArtifactDatabase {

    private JIPipeArtifactIndexV1 index;
    private final StampedLock lock = new StampedLock();

    @Override
    public void rebuild(JIPipeArtifactRepositoryReference repositoryReference, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Contacting " + repositoryReference.getUrl() + " [" + repositoryReference.getType().name() + "]");
        final long stamp = lock.writeLock();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(repositoryReference.getUrl()))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String json = response.body();
            index = JsonUtils.readFromString(json, JIPipeArtifactIndexV1.class);
        } catch (Exception ex) {
            progressInfo.log(ex);
        }
        finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public void query(String groupId, String artifactId, String version, JIPipeProgressInfo progressInfo, JIPipeArtifactRepositoryReference repositoryReference, Map<String, JIPipeRemoteArtifact> downloadMap) {
        if (index == null) {
            rebuild(repositoryReference, progressInfo.resolve("Download index"));
        }
        final long stamp = lock.readLock();
        try {
            for (JIPipeArtifactIndexV1Package indexPackage : index.getPackages().values()) {
                JIPipeArtifact asArtifact = indexPackage.toArtifact();

                if (groupId != null && !groupId.equals(asArtifact.getGroupId())) {
                    return;
                }
                if (artifactId != null && !artifactId.equals(asArtifact.getArtifactId())) {
                    return;
                }
                if (version != null && !version.equals(asArtifact.getVersion())) {
                    return;
                }

                for (JIPipeArtifactIndexV1PackageSource source : indexPackage.getSources()) {
                    for (String tag : indexPackage.getTags()) {

                        // Convert tag to classifier
                        String classifier = tag.substring(tag.indexOf('-') + 1);

                        JIPipeRemoteArtifact remoteArtifact = new JIPipeRemoteArtifact();
                        remoteArtifact.setArtifactId(asArtifact.getArtifactId());
                        remoteArtifact.setGroupId(asArtifact.getGroupId());
                        remoteArtifact.setVersion(asArtifact.getVersion());
                        remoteArtifact.setClassifier(classifier);

                        if (source instanceof JIPipeArtifactIndexV1HttpPackageSource httpPackageSource) {
                            String url =httpPackageSource.getUrls().get(tag);
                            remoteArtifact.setSource(new JIPipeHttpRemoteArtifactSource(url));
                        }
                        else if (source instanceof JIPipeArtifactIndexV1OrasPackageSource orasPackageSource) {
                            String ociRef = orasPackageSource.getOciRef() + ":" + tag;
                            remoteArtifact.setSource(new JIPipeOrasRemoteArtifactSource(ociRef));
                        } else {
                            progressInfo.log("Unsupported source type: " + source.getClass());
                            continue;
                        }

                        downloadMap.put(remoteArtifact.getFullId(), remoteArtifact);
                        progressInfo.log("Found " + remoteArtifact.getFullId());
                    }
                }

            }
        }
        finally {
            lock.unlock(stamp);
        }
    }
}
