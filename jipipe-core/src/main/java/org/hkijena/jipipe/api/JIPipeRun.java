/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api;

import com.google.common.base.Charsets;
import ij.IJ;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.HeadlessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runnable instance of an {@link JIPipeProject}
 */
public class JIPipeRun implements JIPipeRunnable {
    private JIPipeRunnableInfo info = new JIPipeRunnableInfo();
    private final JIPipeProject project;
    private final JIPipeProjectCacheQuery cacheQuery;
    private final JIPipeRunSettings configuration;
    JIPipeGraph algorithmGraph;
    private JIPipeFixedThreadPool threadPool;

    /**
     * @param project       The project
     * @param configuration Run configuration
     */
    public JIPipeRun(JIPipeProject project, JIPipeRunSettings configuration) {
        this.project = project;
        this.cacheQuery = new JIPipeProjectCacheQuery(project);
        this.configuration = configuration;
        this.algorithmGraph = new JIPipeGraph(project.getGraph());
        initializeRelativeDirectories();
        initializeInternalStoragePaths();
    }

    private void initializeRelativeDirectories() {
        for (JIPipeGraphNode algorithm : algorithmGraph.getNodes().values()) {
            algorithm.setWorkDirectory(null);
        }
    }

    private void initializeInternalStoragePaths() {
        for (JIPipeGraphNode algorithm : algorithmGraph.getNodes().values()) {
            algorithm.setInternalStoragePath(Paths.get(StringUtils.jsonify(algorithm.getCompartment())).resolve(StringUtils.jsonify(algorithmGraph.getIdOf(algorithm))));
        }
    }

    /**
     * @return The project that created this run
     */
    public JIPipeProject getProject() {
        return project;
    }

    /**
     * This function must be called before running the graph
     */
    private void prepare() {
        assignDataStoragePaths();
    }

    /**
     * Iterates through output slots and assigns the data storage paths
     */
    public void assignDataStoragePaths() {
        if (configuration.getOutputPath() != null) {
            if (!Files.exists(configuration.getOutputPath())) {
                try {
                    Files.createDirectories(configuration.getOutputPath());
                } catch (IOException e) {
                    throw new UserFriendlyRuntimeException(e, "Could not create necessary directory '" + configuration.getOutputPath() + "'!",
                            "Pipeline run", "Either the path is invalid, or you do not have permissions to create the directory",
                            "Check if the path is valid and the parent directories are writeable by your current user");
                }
            }

            // Apply output path to the data slots
            for (JIPipeDataSlot slot : algorithmGraph.getSlotNodes()) {
                if (slot.isOutput()) {
                    slot.setStoragePath(configuration.getOutputPath().resolve(slot.getNode().getInternalStoragePath().resolve(slot.getName())));
                    try {
                        Files.createDirectories(slot.getStoragePath());
                    } catch (IOException e) {
                        throw new UserFriendlyRuntimeException(e, "Could not create necessary directory '" + slot.getStoragePath() + "'!",
                                "Pipeline run", "Either the path is invalid, or you do not have permissions to create the directory",
                                "Check if the path is valid and the parent directories are writeable by your current user");
                    }
                }
            }
        }
    }

    private void flushFinishedSlots(List<JIPipeDataSlot> traversedSlots, Set<JIPipeGraphNode> executedAlgorithms, int currentIndex, JIPipeDataSlot outputSlot, Set<JIPipeDataSlot> flushedSlots, JIPipeRunnableInfo progress) {
        if (!executedAlgorithms.contains(outputSlot.getNode()))
            return;
        if (flushedSlots.contains(outputSlot))
            return;
        boolean canFlush = true;
        for (int j = currentIndex + 1; j < traversedSlots.size(); ++j) {
            JIPipeDataSlot futureSlot = traversedSlots.get(j);
            boolean isDeactivated = (futureSlot.getNode() instanceof JIPipeAlgorithm) && (!((JIPipeAlgorithm) futureSlot.getNode()).isEnabled());
            if (!isDeactivated && futureSlot.isInput() && algorithmGraph.getSourceSlot(futureSlot) == outputSlot) {
                canFlush = false;
                break;
            }
        }
        if (canFlush) {
            if (configuration.isStoreToCache()) {
                JIPipeGraphNode runAlgorithm = outputSlot.getNode();
                JIPipeGraphNode projectAlgorithm = cacheQuery.getNode(runAlgorithm.getIdInGraph());
                JIPipeProjectCache.State stateId = cacheQuery.getCachedId(projectAlgorithm);
                project.getCache().store(projectAlgorithm, stateId, outputSlot);
            }
            if (configuration.isSaveOutputs()) {
                progress.log(String.format("Saving data in slot %s (contains %d rows of type %s)", outputSlot.getDisplayName(), outputSlot.getRowCount(), JIPipeDataInfo.getInstance(outputSlot.getAcceptedDataType()).getName()));
                outputSlot.flush(configuration.getOutputPath(), !configuration.isStoreToCache());
            } else {
                outputSlot.clearData(false);
            }
            flushedSlots.add(outputSlot);
        }
    }

    @Override
    public void run() {
        info.clearLog();
        long startTime = System.currentTimeMillis();
        info.log("JIPipe run starting at " + StringUtils.formatDateTime(LocalDateTime.now()));
        info.log("Preparing output folders ...");
        prepare();
        try {
            info.log("Running main analysis with " + configuration.getNumThreads() + " threads ...\n");
            threadPool = new JIPipeFixedThreadPool(configuration.getNumThreads());
            runAnalysis(info);
        } catch (Exception e) {
            info.log(e.toString());
            info.log(ExceptionUtils.getStackTrace(e));
            try {
                if (configuration.getOutputPath() != null)
                    Files.write(configuration.getOutputPath().resolve("log.txt"), info.getLog().toString().getBytes(Charsets.UTF_8));
            } catch (IOException ex) {
                if (!configuration.isSilent())
                    IJ.handleException(ex);
            }
            throw e;
        } finally {
            if (threadPool != null) {
                threadPool.shutdown();
            }
            threadPool = null;
        }

        // Postprocessing
        info.log("Postprocessing steps ...");
        try {
            if (configuration.getOutputPath() != null && configuration.isSaveOutputs())
                project.saveProject(configuration.getOutputPath().resolve("project.jip"));
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not save project to '" + configuration.getOutputPath().resolve("project.jip") + "'!",
                    "Pipeline run", "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                    "Check if you can write to the output directory.");
        }

        info.log("Run ending at " + StringUtils.formatDateTime(LocalDateTime.now()));
        info.log("\nAnalysis required " + StringUtils.formatDuration(System.currentTimeMillis() - startTime) + " to execute.\n");

        try {
            if (configuration.getOutputPath() != null)
                Files.write(configuration.getOutputPath().resolve("log.txt"), info.getLog().toString().getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not write log '" + configuration.getOutputPath().resolve("log.txt") + "'!",
                    "Pipeline run", "Either the path is invalid, or you have no permission to write to the disk, or the disk space is full",
                    "Check if you can write to the output directory.");
        }
    }

    private void runAnalysis(JIPipeRunnableInfo progress) {
        Set<JIPipeGraphNode> unExecutableAlgorithms = algorithmGraph.getDeactivatedAlgorithms();
        Set<JIPipeGraphNode> executedAlgorithms = new HashSet<>();
        Set<JIPipeDataSlot> flushedSlots = new HashSet<>();
        List<JIPipeDataSlot> traversedSlots = algorithmGraph.traverseSlots();
        progress.setMaxProgress(traversedSlots.size());
        for (int index = 0; index < traversedSlots.size(); ++index) {
            if (progress.isCancelled().get())
                throw new UserFriendlyRuntimeException("Execution was cancelled",
                        "You cancelled the execution of the algorithm pipeline.",
                        "Pipeline run", "You clicked 'Cancel'.",
                        "Do not click 'Cancel' if you do not want to cancel the execution.");
            JIPipeDataSlot slot = traversedSlots.get(index);
            progress.setProgress(index);
            progress.log(slot.getDisplayName());

            // If an algorithm cannot be executed, skip it automatically
            if (unExecutableAlgorithms.contains(slot.getNode()))
                continue;

            // Let algorithms provide sub-progress
            JIPipeRunnableInfo subProgress = progress.resolve("Algorithm: " + slot.getNode().getName());

            if (slot.isInput()) {
                // Copy data from source
                JIPipeDataSlot sourceSlot = algorithmGraph.getSourceSlot(slot);
                slot.copyFrom(sourceSlot);

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, executedAlgorithms, index, sourceSlot, flushedSlots, subProgress);
            } else if (slot.isOutput()) {
                JIPipeGraphNode node = slot.getNode();
                // Ensure the algorithm has run
                if (!executedAlgorithms.contains(node)) {
                    runNode(executedAlgorithms, node, subProgress);
                }

                // Check if we can flush the output
                flushFinishedSlots(traversedSlots, executedAlgorithms, index, slot, flushedSlots, subProgress);
            }
        }

        // There might be some algorithms missing (ones that do not have an output)
        List<JIPipeGraphNode> additionalAlgorithms = new ArrayList<>();
        for (JIPipeGraphNode node : algorithmGraph.getNodes().values()) {
            if (!executedAlgorithms.contains(node) && !unExecutableAlgorithms.contains(node)) {
                additionalAlgorithms.add(node);
            }
        }
        progress.setMaxProgress(progress.getProgress() + additionalAlgorithms.size());
        for (int index = 0; index < additionalAlgorithms.size(); index++) {
            JIPipeGraphNode node = additionalAlgorithms.get(index);
            int absoluteIndex = index + traversedSlots.size() - 1;
            progress.setProgress(absoluteIndex);
            JIPipeRunnableInfo subProgress = progress.resolve("Algorithm: " + node.getName());
            runNode(executedAlgorithms, node, subProgress);
        }
    }

    private void runNode(Set<JIPipeGraphNode> executedAlgorithms, JIPipeGraphNode node, JIPipeRunnableInfo progress) {
        progress.log("");

        // If enabled try to extract outputs from cache
        boolean dataLoadedFromCache = false;
        if (configuration.isLoadFromCache()) {
            dataLoadedFromCache = tryLoadFromCache(node, progress);
        }

        if (!dataLoadedFromCache) {
            try {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setThreadPool(threadPool);
                }
                node.run(progress);
            } catch (HeadlessException e) {
                throw new UserFriendlyRuntimeException("Algorithm " + node + " does not work in a headless environment!",
                        e,
                        "An error occurred during processing",
                        "On running the algorithm '" + node.getName() + "', within compartment '" + getProject().getCompartments().get(node.getCompartment()).getName() + "'",
                        "The algorithm raised an error, as it is not compatible with a headless environment.",
                        "Please contact the plugin developers about this issue. If this happens in an ImageJ method, please contact the ImageJ developers.");
            } catch (Exception e) {
                throw new UserFriendlyRuntimeException("Algorithm " + node + " raised an exception!",
                        e,
                        "An error occurred during processing",
                        "On running the algorithm '" + node.getName() + "', within compartment '" + getProject().getCompartments().get(node.getCompartment()).getName() + "'",
                        "Please refer to the other error messages.",
                        "Please follow the instructions for the other error messages.");
            } finally {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setThreadPool(null);
                }
            }
        } else {
            progress.log("Output data was loaded from cache. Not executing.");
        }

        executedAlgorithms.add(node);
    }

    /**
     * Attempts to load data from cache
     *
     * @param algorithm           the target algorithm
     * @return if successful. This means all output slots were restored.
     */
    private boolean tryLoadFromCache(JIPipeGraphNode algorithm, JIPipeRunnableInfo progress) {
        if (!configuration.isLoadFromCache())
            return false;
        JIPipeGraphNode projectAlgorithm = cacheQuery.getNode(algorithm.getIdInGraph());
        JIPipeProjectCache.State stateId = cacheQuery.getCachedId(projectAlgorithm);
        Map<String, JIPipeDataSlot> cachedData = project.getCache().extract((JIPipeAlgorithm) projectAlgorithm, stateId);
        if (!cachedData.isEmpty()) {
            progress.log(String.format("Accessing cache with slots %s via state id %s", String.join(", ",
                    cachedData.keySet()), stateId));
            for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                if (!cachedData.containsKey(outputSlot.getName())) {
                    progress.log(String.format("Cache access failed. Missing output slot %s", outputSlot.getName()));
                    return false;
                }
            }
            for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                outputSlot.clearData(false);
                outputSlot.copyFrom(cachedData.get(outputSlot.getName()));
            }
            progress.log("Cache data access successful.");
            return true;
        }
        return false;
    }

    /**
     * @return The graph used within this run. A copy of the project graph.
     */
    public JIPipeGraph getGraph() {
        return algorithmGraph;
    }

    /**
     * @return The run configuration
     */
    public JIPipeRunSettings getConfiguration() {
        return configuration;
    }

    @Override
    public JIPipeRunnableInfo getInfo() {
        return info;
    }

    public void setInfo(JIPipeRunnableInfo info) {
        this.info = info;
    }

    /**
     * Loads a JIPipeRun from a folder
     *
     * @param folder Folder containing the run
     * @return The loaded run
     * @throws IOException Triggered by {@link com.fasterxml.jackson.databind.ObjectMapper}
     */
    public static JIPipeRun loadFromFolder(Path folder, JIPipeValidityReport report) throws IOException {
        Path parameterFile = folder.resolve("project.jip");
        JIPipeProject project = JIPipeProject.loadProject(parameterFile, report);
        JIPipeRunSettings configuration = new JIPipeRunSettings();
        configuration.setOutputPath(folder);
        JIPipeRun run = new JIPipeRun(project, configuration);
        run.prepare();
        return run;
    }
}
