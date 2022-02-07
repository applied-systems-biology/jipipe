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

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Settings for an {@link JIPipeRun}
 */
public class JIPipeRunSettings implements JIPipeParameterCollection {
    private EventBus eventBus = new EventBus();
    private Path outputPath;
    private boolean loadFromCache = true;
    private boolean storeToCache = false;
    private boolean saveToDisk = true;
    private int numThreads = RuntimeSettings.getInstance().getDefaultRunThreads();
    private boolean silent = false;
    private boolean ignoreDeactivatedInputs = false;
    private Set<JIPipeGraphNode> disableStoreToCacheNodes = new HashSet<>();
    private Set<JIPipeGraphNode> disableSaveToDiskNodes = new HashSet<>();

    @JIPipeParameter(value = "output-path", uiOrder = -999)
    @JIPipeDocumentation(name = "Output folder")
    @PathParameterSettings(ioMode = PathIOMode.Save, pathMode = PathType.DirectoriesOnly)
    public Path getOutputPath() {
        return outputPath;
    }

    /**
     * Sets the output path
     *
     * @param outputPath The output path
     */
    @JIPipeParameter("output-path")
    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;

    }

    @JIPipeDocumentation(name = "Load from cache", description = "If enabled, the results are automatically loaded from a cache if possible.")
    @JIPipeParameter("load-from-cache")
    public boolean isLoadFromCache() {
        return loadFromCache;
    }

    @JIPipeParameter("load-from-cache")
    public void setLoadFromCache(boolean loadFromCache) {
        this.loadFromCache = loadFromCache;
    }

    @JIPipeDocumentation(name = "Save to cache", description = "If enabled, the results and intermediate results are stored into a cache. " +
            "This cache is limited by the RAM. Will be ignored if the global JIPipe cache settings are disabled.")
    @JIPipeParameter("store-to-cache")
    public boolean isStoreToCache() {
        return storeToCache;
    }

    @JIPipeParameter("store-to-cache")
    public void setStoreToCache(boolean storeToCache) {
        this.storeToCache = storeToCache;
    }

    @JIPipeDocumentation(name = "Number of threads", description = "Maximum number of threads that are allocated to the tasks. " +
            "Please note that the actual allocation depends on the algorithms.")
    @JIPipeParameter("num-threads")
    public int getNumThreads() {
        return numThreads;
    }

    @JIPipeParameter("num-threads")
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public boolean isSaveToDisk() {
        return saveToDisk;
    }

    public void setSaveToDisk(boolean saveToDisk) {
        this.saveToDisk = saveToDisk;
    }

    /**
     * If true, exceptions are discarded
     *
     * @return if exceptions should be discarded
     */
    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    /**
     * If true, the run will ignore cascading checks for deactivated inputs
     * and run any node, that is not explicitly deactivated.
     * If enabled, the data must be provided externally (or the table is empty)
     *
     * @return if cascading deactivation is ignored
     */
    public boolean isIgnoreDeactivatedInputs() {
        return ignoreDeactivatedInputs;
    }

    public void setIgnoreDeactivatedInputs(boolean ignoreDeactivatedInputs) {
        this.ignoreDeactivatedInputs = ignoreDeactivatedInputs;
    }

    /**
     * Allows to set a list of nodes where storing to cache is disabled
     *
     * @return the list of nodes
     */
    public Set<JIPipeGraphNode> getDisableStoreToCacheNodes() {
        return disableStoreToCacheNodes;
    }

    public void setDisableStoreToCacheNodes(Set<JIPipeGraphNode> disableStoreToCacheNodes) {
        this.disableStoreToCacheNodes = disableStoreToCacheNodes;
    }

    /**
     * Allows to exclude specific nodes from saving any outputs to the disk
     *
     * @return the list of nodes
     */
    public Set<JIPipeGraphNode> getDisableSaveToDiskNodes() {
        return disableSaveToDiskNodes;
    }

    public void setDisableSaveToDiskNodes(Set<JIPipeGraphNode> disableSaveToDiskNodes) {
        this.disableSaveToDiskNodes = disableSaveToDiskNodes;
    }
}
