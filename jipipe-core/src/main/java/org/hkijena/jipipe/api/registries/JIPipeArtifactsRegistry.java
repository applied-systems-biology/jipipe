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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.*;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

public class JIPipeArtifactsRegistry {
    private final JIPipe jiPipe;
    private final Map<String, JIPipeArtifact> cachedArtifacts = new HashMap<>();
    private final StampedLock lock = new StampedLock();

    public JIPipeArtifactsRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
    }

    /**
     * Selects a preferred artifact by class
     *
     * @param candidates the candidates
     * @return the best candidate
     */
    public static JIPipeArtifact selectPreferredArtifactByClassifier(List<JIPipeArtifact> candidates) {
        JIPipeArtifact bestCandidate = null;
        for (JIPipeArtifact candidate : candidates) {
            if (candidate.isCompatible()) {
                if (bestCandidate == null) {
                    bestCandidate = candidate;
                } else if (bestCandidate.isNative()) {
                    if (candidate.isNative()) {
                        if (JIPipeArtifactApplicationSettings.getInstance().isPreferGPU() && !bestCandidate.isRequireGPU() && candidate.isRequireGPU()) {
                            bestCandidate = candidate;
                        } else if (!JIPipeArtifactApplicationSettings.getInstance().isPreferGPU() && bestCandidate.isRequireGPU() && !candidate.isRequireGPU()) {
                            bestCandidate = candidate;
                        }
                    }
                } else {
                    if (JIPipeArtifactApplicationSettings.getInstance().isPreferGPU() && !bestCandidate.isRequireGPU() && candidate.isRequireGPU()) {
                        bestCandidate = candidate;
                    } else if (!JIPipeArtifactApplicationSettings.getInstance().isPreferGPU() && bestCandidate.isRequireGPU() && !candidate.isRequireGPU()) {
                        bestCandidate = candidate;
                    }
                }
            }
        }
        return bestCandidate;
    }

    public JIPipe getJiPipe() {
        return jiPipe;
    }

    public Map<String, JIPipeArtifact> getCachedArtifacts() {
        return Collections.unmodifiableMap(cachedArtifacts);
    }

    /**
     * Given a glob-like query, looks for the closest, newest artifact that matches it
     *
     * @param query the query
     * @return the artifact
     */
    public JIPipeArtifact searchClosestCompatibleArtifactFromQuery(String query) {
        List<JIPipeArtifact> artifacts = queryCachedArtifacts(query);
        artifacts.removeIf(artifact -> !artifact.isCompatible());

        JIPipeArtifact targetArtifact = null;

        if (artifacts.isEmpty()) {
            // Find alternative
            artifacts = JIPipe.getArtifacts().queryCachedArtifacts(query);
            for (JIPipeArtifact artifact : artifacts) {
                JIPipeArtifact closestCompatibleArtifact = JIPipe.getArtifacts().findClosestCompatibleArtifact(artifact.getFullId());
                if (closestCompatibleArtifact != null) {
                    targetArtifact = closestCompatibleArtifact;
                    break;
                }
            }
        } else {
            targetArtifact = selectPreferredArtifactByClassifier(artifacts);
        }

        return targetArtifact;
    }

    /**
     * Finds the closest compatible artifact to the provided full artifact ID.
     *
     * @param fullArtifactId full artifact ID
     * @return matching artifact or null
     */
    public JIPipeArtifact findClosestCompatibleArtifact(String fullArtifactId) {
        try {
            JIPipeArtifact parsedArtifact = new JIPipeArtifact(fullArtifactId);
            List<JIPipeArtifact> candidates = Collections.emptyList();

            // If the parsed artifact is compatible, try to match it exactly
            if (parsedArtifact.isCompatible()) {
                candidates = queryCachedArtifacts(fullArtifactId);
            }
            if (!candidates.isEmpty()) {
                return selectPreferredArtifactByClassifier(candidates);
            }

            // Try same version with different classifiers (select GPU versions etc.)
            candidates = queryCachedArtifacts(parsedArtifact.getGroupId() + "." + parsedArtifact.getArtifactId() + ":" + parsedArtifact.getVersion() + "-*");
            if (!candidates.isEmpty()) {
                JIPipeArtifact bestCandidate = selectPreferredArtifactByClassifier(candidates);

                if (bestCandidate != null) {
                    return bestCandidate;
                }
            }

            // Try same artifact ID only
            candidates = queryCachedArtifacts(parsedArtifact.getGroupId() + "." + parsedArtifact.getArtifactId() + ":" + parsedArtifact.getVersion() + "-*");
            Map<String, List<JIPipeArtifact>> byVersion = candidates.stream().collect(Collectors.groupingBy(JIPipeArtifact::getVersion));
            List<String> sortedVersions = byVersion.keySet().stream().sorted((o1, o2) -> -VersionUtils.compareVersions(o1, o2)).collect(Collectors.toList());
            for (String sortedVersion : sortedVersions) {
                JIPipeArtifact bestCandidate = selectPreferredArtifactByClassifier(byVersion.get(sortedVersion));
                if (bestCandidate != null) {
                    return bestCandidate;
                }
            }

            // Nothing found
            return null;

        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Queries all cached artifacts (local and remote)
     *
     * @param filters list of filters (connected with OR), using glob syntax
     * @return the list of matched artifacts, sorted
     */
    public List<JIPipeArtifact> queryCachedArtifacts(String... filters) {
        Set<JIPipeArtifact> available = new HashSet<>();
        for (String filter : filters) {
            String regex = StringUtils.convertGlobToRegex(filter);
            for (JIPipeArtifact artifact : cachedArtifacts.values()) {
                if (artifact.getFullId().matches(regex)) {
                    available.add(artifact);
                }
            }
        }
        return available.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    public void updateCachedArtifacts(JIPipeProgressInfo progressInfo) {
        long stamp = lock.writeLock();
        try {
            cachedArtifacts.clear();
            try {
                for (JIPipeRemoteArtifact artifact : queryRemoteRepositories(null, null, null, progressInfo.resolve("Remote repository"))) {
                    cachedArtifacts.put(artifact.getFullId(), artifact);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                progressInfo.log(ExceptionUtils.getStackTrace(e));
            }
            for (JIPipeLocalArtifact artifact : queryLocalRepositories(null, null, null, progressInfo.resolve("Local repository"))) {
                cachedArtifacts.put(artifact.getFullId(), artifact);
            }
        } finally {
            lock.unlock(stamp);
        }
    }

    public List<JIPipeLocalArtifact> queryLocalRepositories(String groupId, String artifactId, String version, JIPipeProgressInfo progressInfo) {
        Map<String, JIPipeLocalArtifact> artifacts = new HashMap<>();
        try {
            for (Path repositoryPath : Arrays.asList(getLocalSystemRepositoryPath(), getLocalUserRepositoryPath())) {
                progressInfo.log("Checking local repository @ " + repositoryPath);
                Files.walkFileTree(repositoryPath, new FileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path artifactFile = dir.resolve("artifact.json");
                        if (Files.isRegularFile(artifactFile)) {
                            progressInfo.log("Found artifact @ " + artifactFile);
                            JIPipeLocalArtifact artifact = JsonUtils.readFromFile(artifactFile, JIPipeLocalArtifact.class);
                            if (groupId != null && !Objects.equals(artifact.getGroupId(), groupId)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            if (artifactId != null && !Objects.equals(artifact.getArtifactId(), artifactId)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            if (version != null && !Objects.equals(artifact.getVersion(), version)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            artifact.setLocalPath(dir);
                            artifacts.put(artifact.getFullId(), artifact);
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>(artifacts.values());
    }

    public List<JIPipeRemoteArtifact> queryRemoteRepositories(String groupId, String artifactId, String version, JIPipeProgressInfo progressInfo) {
        Map<String, JIPipeRemoteArtifact> downloadMap = new HashMap<>();
        for (JIPipeArtifactRepositoryReference repository : JIPipeArtifactApplicationSettings.getInstance().getRepositories()) {
            Stack<String> tokens = new Stack<>();
            tokens.add(null);
            while (!tokens.isEmpty()) {
                try {
                    String token = tokens.pop();
                    String urlString = repository.getUrl() + "/service/rest/v1/search/assets?repository=" + repository.getRepository();
                    if (groupId != null) {
                        urlString += "&group=" + groupId;
                    }
                    if (artifactId != null) {
                        urlString += "&name=" + artifactId;
                    }
                    if (version != null) {
                        urlString += "&version=" + version;
                    }
                    if (token != null) {
                        urlString += "&continuationToken" + token;
                    }
                    progressInfo.log("Contacting " + urlString);
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(1000);
                    conn.setReadTimeout(5000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");

                    if (conn.getResponseCode() != 200) {
                        progressInfo.log("Failed : HTTP error code : " + conn.getResponseCode());
                        continue;
                    }

                    // Read JSON string data
                    StringBuilder textBuilder = new StringBuilder();
                    try (Reader reader = new BufferedReader(new InputStreamReader
                            (conn.getInputStream(), StandardCharsets.UTF_8))) {
                        int c = 0;
                        while ((c = reader.read()) != -1) {
                            textBuilder.append((char) c);
                        }
                    }
                    conn.disconnect();

                    // Read as JSON
                    JsonNode rootNode = JsonUtils.readFromString(textBuilder.toString(), JsonNode.class);

                    // Read items
                    if (rootNode.has("items")) {
                        for (JsonNode item : ImmutableList.copyOf(rootNode.get("items").elements())) {
                            JIPipeRemoteArtifact download = new JIPipeRemoteArtifact();
                            download.setUrl(item.get("downloadUrl").asText());
                            download.setSize(item.get("fileSize").asLong());
                            download.setArtifactId(item.get("maven2").get("artifactId").asText());
                            download.setGroupId(item.get("maven2").get("groupId").asText());
                            download.setClassifier(item.get("maven2").get("classifier").asText());
                            download.setVersion(item.get("maven2").get("version").asText());

                            if (!downloadMap.containsKey(download.getFullId())) {
                                downloadMap.put(download.getFullId(), download);
                                progressInfo.log("Found " + download.getFullId());
                            }
                        }
                    }

                    // Continuation token
                    if (rootNode.has("continuationToken") && !rootNode.get("continuationToken").isNull()) {
                        tokens.push(rootNode.get("continuationToken").asText());
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return downloadMap.values().stream().sorted((o1, o2) -> VersionUtils.compareVersions(o1.getVersion(), o2.getVersion())).collect(Collectors.toList());
    }

    /**
     * JIPipe's system repository path, which is usually located in IMAGEJ_DIR/jipipe/artifacts
     * This repository is intended for distributors of JIPipe to provide artifacts with JIPipe
     * Can be overwritten by setting the JIPIPE_SYSTEM_REPOSITORY_PATH
     *
     * @return the system repository path
     */
    public Path getLocalSystemRepositoryPath() {
        if (System.getenv("JIPIPE_LOCAL_REPOSITORY") != null) {
            return Paths.get(System.getenv("JIPIPE_LOCAL_REPOSITORY"));
        } else {
            return PathUtils.getJIPipeUserDir().resolve("artifacts");
        }
    }

    /**
     * The path to the local repository that is owned by the user.
     * This is usually located in the user's home directory.
     *
     * @return the user's repository path
     */
    public Path getLocalUserRepositoryPath() {
        if (JIPipeArtifactApplicationSettings.getInstance().getOverrideInstallationPath().isEnabled() && !JIPipeArtifactApplicationSettings.getInstance().getOverrideInstallationPath().getContent().toString().isEmpty()) {
            if (JIPipeArtifactApplicationSettings.getInstance().getOverrideInstallationPath().getContent().isAbsolute()) {
                return JIPipeArtifactApplicationSettings.getInstance().getOverrideInstallationPath().getContent();
            } else {
                return PathUtils.getJIPipeUserDir().resolve(JIPipeArtifactApplicationSettings.getInstance().getOverrideInstallationPath().getContent());
            }
        } else {
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

    public void enqueueUpdateCachedArtifacts() {
        JIPipeRunnableQueue.getInstance().enqueue(new JIPipeArtifactRepositoryUpdateCachedArtifactsRun());
    }

    public JIPipeArtifact queryCachedArtifact(String filter) {
        List<JIPipeArtifact> artifacts = queryCachedArtifacts(filter);
        if (!artifacts.isEmpty()) {
            return artifacts.get(0);
        } else {
            return null;
        }
    }
}
