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

package org.hkijena.jipipe.api.service.components;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.acceleration.JIPipeHardwareAccelerationMode;
import org.hkijena.jipipe.api.artifacts.*;
import org.hkijena.jipipe.api.artifacts.index.JIPipeArtifactIndexV1RemoteArtifactDatabase;
import org.hkijena.jipipe.api.artifacts.index.JIPipeLocalRemoteArtifactDatabase;
import org.hkijena.jipipe.api.artifacts.index.JIPipeNexusRemoteArtifactDatabase;
import org.hkijena.jipipe.api.artifacts.index.JIPipeRemoteArtifactDatabase;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurationCache;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurator;
import org.hkijena.jipipe.api.environments.sources.JIPipeEnvironmentConfiguratorCustomSource;
import org.hkijena.jipipe.api.environments.sources.JIPipeEnvironmentConfiguratorFallbackSource;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactApplicationSettings;
import org.hkijena.jipipe.plugins.artifacts.oras.OrasEnvironment;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeHardwareAccelerationApplicationSettings;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

public final class JIPipeArtifactsServiceComponent extends JIPipeServiceComponent {
    private final Map<String, JIPipeArtifact> cachedArtifacts = new HashMap<>();
    private final Map<String, JIPipeRemoteArtifact> cachedRemoteArtifacts = new HashMap<>();
    private final Map<String, JIPipeLocalArtifact> cachedLocalArtifacts = new HashMap<>();
    private final StampedLock lock = new StampedLock();
    private final UpdatedEventEmitter updatedEventEmitter = new UpdatedEventEmitter();

    private final Map<JIPipeArtifactRepositoryReference, JIPipeRemoteArtifactDatabase> indexerMap = new HashMap<>();

    public JIPipeArtifactsServiceComponent(JIPipeService service) {
        super(service);
    }

    /**
     * Selects a preferred artifact by class
     *
     * @param candidates the candidates
     * @return the best candidate
     */
    public static JIPipeArtifact selectPreferredArtifactByClassifier(List<JIPipeArtifact> candidates) {
        JIPipeArtifact bestCandidate = null;
        JIPipeHardwareAccelerationMode accelerationPreference = JIPipeHardwareAccelerationApplicationSettings.getInstance().getAccelerationPreference();
        Vector2iParameter accelerationPreferenceVersions = JIPipeHardwareAccelerationApplicationSettings.getInstance().getAccelerationPreferenceVersions();
        boolean wantsGPU = accelerationPreference != JIPipeHardwareAccelerationMode.CPU;

        Map<String, List<JIPipeArtifact>> byVersion = candidates.stream().collect(Collectors.groupingBy(JIPipeArtifact::getVersion));
        List<String> sortedVersions = byVersion.keySet().stream().sorted(VersionUtils.VERSION_COMPARATOR.reversed()).collect(Collectors.toList());

        for (String version : sortedVersions) {
            List<JIPipeArtifact> artifactsForVersion = byVersion.get(version);

            // First find something with CPU if possible
            for (JIPipeArtifact candidate : artifactsForVersion) {
                if (candidate.isCompatible() && !candidate.isRequireGPU()) {
                    bestCandidate = candidate;
                    break;
                }
            }

            if (wantsGPU) {
                // Optimize for better GPU (go inverse as newer versions are first)
                for (JIPipeArtifact candidate : artifactsForVersion) {
                    if (candidate.isCompatible()) {
                        if (bestCandidate == null) {
                            // take anything
                            bestCandidate = candidate;
                        } else {
                            // Look for GPU
                            if (candidate.isRequireGPU() && candidate.isGPUCompatible(accelerationPreference, accelerationPreferenceVersions)) {
                                if (!bestCandidate.isRequireGPU()) {
                                    bestCandidate = candidate;
                                } else if (candidate.getGPUVersion(accelerationPreference.getPrefix()) > bestCandidate.getGPUVersion(accelerationPreference.getPrefix())) {
                                    bestCandidate = candidate;
                                }
                            }
                        }
                    }
                }
            }

            if (bestCandidate != null) {
                break;
            }
        }

        return bestCandidate;
    }

    public Map<String, JIPipeArtifact> getCachedArtifacts() {
        return Collections.unmodifiableMap(cachedArtifacts);
    }

    public Map<String, JIPipeRemoteArtifact> getCachedRemoteArtifacts() {
        return Collections.unmodifiableMap(cachedRemoteArtifacts);
    }

    public Map<String, JIPipeLocalArtifact> getCachedLocalArtifacts() {
        return Collections.unmodifiableMap(cachedLocalArtifacts);
    }

    public UpdatedEventEmitter getUpdatedEventEmitter() {
        return updatedEventEmitter;
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
            cachedRemoteArtifacts.clear();
            cachedLocalArtifacts.clear();
            cachedArtifacts.clear();
            try {
                for (JIPipeRemoteArtifact artifact : queryRemoteRepositories(null, null, null, progressInfo.resolve("Remote repository"))) {
                    cachedArtifacts.put(artifact.getFullId(), artifact);
                    cachedRemoteArtifacts.put(artifact.getFullId(), artifact);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                progressInfo.log(ExceptionUtils.getStackTrace(e));
            }
            for (JIPipeLocalArtifact artifact : queryLocalRepositories(null, null, null, progressInfo.resolve("Local repository"))) {
                cachedArtifacts.put(artifact.getFullId(), artifact);
                cachedLocalArtifacts.put(artifact.getFullId(), artifact);
            }
        } finally {
            lock.unlock(stamp);
        }

        updatedEventEmitter.emit(new UpdatedEvent(this));
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
            progressInfo.log("Checking remote repository @ " + repository.getUrl() + " of type " + repository.getType().name());
            try {
                JIPipeRemoteArtifactDatabase indexer = indexerMap.getOrDefault(repository, null);
                if (indexer == null) {
                    Class<? extends JIPipeRemoteArtifactDatabase> indexerClass = switch (repository.getType()) {
                        case LocalDirectory -> JIPipeLocalRemoteArtifactDatabase.class;
                        case SonatypeNexus -> JIPipeNexusRemoteArtifactDatabase.class;
                        case JSONv1 -> JIPipeArtifactIndexV1RemoteArtifactDatabase.class;
                    };
                    indexer = (JIPipeRemoteArtifactDatabase) ReflectionUtils.newInstance(indexerClass);
                }
                indexer.query(groupId, artifactId, version, progressInfo.resolve("[" + repository.getType().name() + "] " + repository.getName()), repository, downloadMap);
            } catch (Throwable e) {
                progressInfo.log(e);
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
        if (System.getenv().containsKey("JIPIPE_OVERRIDE_ARTIFACTS_DIR")) {
            return Paths.get(System.getenv().get("JIPIPE_OVERRIDE_ARTIFACTS_DIR"));
        }
        if (getService().getInitializationSettings().getOverrideArtifactsDir() != null) {
            return getService().getInitializationSettings().getOverrideArtifactsDir();
        }
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
                if (System.getenv().containsKey("XDG_DATA_HOME") && !StringUtils.isNullOrEmpty(System.getProperty("XDG_DATA_HOME"))) {
                    return Paths.get(System.getProperty("XDG_DATA_HOME"))
                            .resolve("JIPipe")
                            .resolve("artifacts");
                } else {
                    return PathUtils.getHomeDirectory().resolve(".local")
                            .resolve("share").resolve("JIPipe")
                            .resolve("artifacts");
                }
            } else if (SystemUtils.IS_OS_MAC_OSX) {
                return PathUtils.getHomeDirectory().resolve("Library").resolve("Application Support")
                        .resolve("JIPipe").resolve("artifacts");
            } else {
                throw new UnsupportedOperationException("Unknown operating system!");
            }
        }
    }

    public void enqueueUpdateCachedArtifacts() {
        JIPipeRunnableQueue.getInstance().enqueue(new JIPipeArtifactRepositoryUpdateCachedArtifactsRun());
    }

    public JIPipeArtifact queryPreferredCachedArtifact(String filter) {
        List<JIPipeArtifact> artifacts = queryCachedArtifacts(filter);
        if (!artifacts.isEmpty()) {
            return selectPreferredArtifactByClassifier(artifacts);
        } else {
            return null;
        }
    }

    public List<JIPipeArtifact> queryCachedVersionPinnedArtifacts(String... filters) {
        List<JIPipeArtifact> result = new ArrayList<>();
        Set<String> alreadyAdded = new HashSet<>();
        for (JIPipeArtifact artifact : queryCachedArtifacts(filters)) {
            String versionPinnedId = artifact.getFullId(JIPipeArtifact.ResolutionStatus.GroupNameVersion);
            if (!alreadyAdded.contains(versionPinnedId)) {
                JIPipeArtifact copy = new JIPipeArtifact(artifact);
                copy.setClassifier("*");
                result.add(copy);
                alreadyAdded.add(versionPinnedId);
            }
        }
        return result;
    }

    /**
     * Gets the configured ORAS environment. Downloads ORAS if necessary (will upgrade the context if needed)
     *
     * @param context      the context
     * @param progressInfo progress info
     * @return the ORAS environment
     */
    public OrasEnvironment getOrasEnvironment(JIPipeArtifactOperationContext context, JIPipeProgressInfo progressInfo) {
        JIPipeArtifactApplicationSettings artifactApplicationSettings = getService().getApplicationSettings().getByType(JIPipeArtifactApplicationSettings.class);
        JIPipeEnvironmentConfigurator<OrasEnvironment> configurator = new JIPipeEnvironmentConfigurator<>(OrasEnvironment.class, new JIPipeEnvironmentConfigurationCache(),
                new JIPipeEnvironmentConfiguratorCustomSource(artifactApplicationSettings.getOrasCliEnvironment(), context),
                new JIPipeEnvironmentConfiguratorFallbackSource<>());
        configurator.setArtifactOperationContext(context); // Important, otherwise we deadlock
        return configurator.get(progressInfo);
    }

    public FileLocker createFileLocker() {
        return new FileLocker(getProgressInfo(), getLocalUserRepositoryPath().resolve("lockfile"));
    }

    public interface UpdatedEventListener {
        void onArtifactsRegistryUpdated(UpdatedEvent event);
    }

    public static class UpdatedEvent extends AbstractJIPipeEvent {

        public UpdatedEvent(Object source) {
            super(source);
        }
    }

    public static class UpdatedEventEmitter extends JIPipeEventEmitter<UpdatedEvent, UpdatedEventListener> {

        @Override
        protected void call(UpdatedEventListener updatedEventListener, UpdatedEvent event) {
            updatedEventListener.onArtifactsRegistryUpdated(event);
        }
    }
}
