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
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Stream;

/**
 * Indexes a local source of {@link JIPipeRemoteArtifact}
 */
public class JIPipeLocalRemoteArtifactSourceIndexer implements JIPipeRemoteArtifactSourceIndexer {

    private final List<Entry> entries = new ArrayList<>();
    private final StampedLock stampedLock = new StampedLock();

    @Override
    public void rebuild(JIPipeArtifactRepositoryReference repositoryReference, JIPipeProgressInfo progressInfo) {
        Path root = Paths.get(repositoryReference.getUrl());
        final long stamp = stampedLock.writeLock();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.forEach(path -> {
                try {
                    if (Files.isRegularFile(path) && (path.getFileName().toString().endsWith(".zip") || path.getFileName().toString().endsWith(".tar.gz"))) {
                        Path relativePath = root.relativize(path);
                        String pathVersion = PathUtils.getName(path, -2);
                        String pathArtifactId = PathUtils.getName(relativePath, -3);
                        Path groupIdPath = relativePath.subpath(0, relativePath.getNameCount() - 3);
                        String pathGroupId = String.join(".", PathUtils.disassemble(groupIdPath));
                        String pathClassifier = path.getFileName().toString().split("-")[2].split("\\.")[0];

                        entries.add(new Entry(path, relativePath, pathVersion, pathArtifactId, groupIdPath, pathGroupId, pathClassifier));
                    }
                } catch (Throwable throwable) {
                    progressInfo.log(throwable);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public void query(String groupId, String artifactId, String version, JIPipeProgressInfo progressInfo, JIPipeArtifactRepositoryReference repositoryReference, Map<String, JIPipeRemoteArtifact> downloadMap) {
        if (entries.isEmpty()) {
            rebuild(repositoryReference, progressInfo.resolve("Rebuild index"));
        }
        final long stamp = stampedLock.readLock();
        try {
            for (Entry entry : entries) {
                if (groupId != null && !groupId.equals(entry.pathGroupId)) {
                    return;
                }
                if (artifactId != null && !artifactId.equals(entry.pathArtifactId)) {
                    return;
                }
                if (version != null && !version.equals(entry.pathVersion)) {
                    return;
                }

                JIPipeRemoteArtifact remoteArtifact = new JIPipeRemoteArtifact();
                remoteArtifact.setArtifactId(entry.pathArtifactId);
                remoteArtifact.setGroupId(entry.pathGroupId);
                remoteArtifact.setVersion(entry.pathVersion);
                remoteArtifact.setClassifier(entry.pathClassifier);
                remoteArtifact.setUrl(entry.path.toUri().toString());
                downloadMap.put(remoteArtifact.getFullId(), remoteArtifact);

                progressInfo.log("Found " + remoteArtifact.getFullId());
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    private record Entry(Path path, Path relativePath, String pathVersion, String pathArtifactId, Path groupIdPath,
                         String pathGroupId, String pathClassifier) {
    }
}
