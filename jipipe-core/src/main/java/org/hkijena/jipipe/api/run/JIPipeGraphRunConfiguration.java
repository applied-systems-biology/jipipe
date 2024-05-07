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

package org.hkijena.jipipe.api.run;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Settings for an {@link JIPipeGraphRun}
 */
public class JIPipeGraphRunConfiguration extends AbstractJIPipeParameterCollection {
    private Path outputPath;
    private boolean loadFromCache = true;
    private boolean storeToCache = false;
    private boolean storeToDisk = true;
    private int numThreads = JIPipeRuntimeApplicationSettings.getInstance().getDefaultRunThreads();
    private boolean silent = false;
    private boolean ignoreDeactivatedInputs = false;
    private Set<UUID> disableStoreToCacheNodes = new HashSet<>();
    private Set<UUID> disableStoreToDiskNodes = new HashSet<>();

    private JIPipeGraphRunPartitionInheritedBoolean continueOnFailure = JIPipeGraphRunPartitionInheritedBoolean.InheritFromPartition;
    private JIPipeGraphRunPartitionInheritedBoolean continueOnFailureExportFailedInputs = JIPipeGraphRunPartitionInheritedBoolean.InheritFromPartition;

    public JIPipeGraphRunConfiguration() {

    }

    public JIPipeGraphRunConfiguration(JIPipeGraphRunConfiguration other) {
        this.outputPath = other.outputPath;
        this.loadFromCache = other.loadFromCache;
        this.storeToCache = other.storeToCache;
        this.storeToDisk = other.storeToDisk;
        this.numThreads = other.numThreads;
        this.silent = other.silent;
        this.ignoreDeactivatedInputs = other.ignoreDeactivatedInputs;
        this.disableStoreToCacheNodes = new HashSet<>(other.disableStoreToCacheNodes);
        this.disableStoreToDiskNodes = new HashSet<>(other.disableStoreToDiskNodes);
        this.continueOnFailureExportFailedInputs = other.continueOnFailureExportFailedInputs;
        this.continueOnFailure = other.continueOnFailure;
    }

    @JIPipeParameter(value = "output-path", uiOrder = -999)
    @SetJIPipeDocumentation(name = "Output folder")
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

    @SetJIPipeDocumentation(name = "Load from cache", description = "If enabled, the results are automatically loaded from a cache if possible.")
    @JIPipeParameter("load-from-cache")
    public boolean isLoadFromCache() {
        return loadFromCache;
    }

    @JIPipeParameter("load-from-cache")
    public void setLoadFromCache(boolean loadFromCache) {
        this.loadFromCache = loadFromCache;
    }

    @SetJIPipeDocumentation(name = "Save to cache", description = "If enabled, the results and intermediate results are stored into a cache. " +
            "This cache is limited by the RAM. Will be ignored if the global JIPipe cache settings are disabled.")
    @JIPipeParameter("store-to-cache")
    public boolean isStoreToCache() {
        return storeToCache;
    }

    @JIPipeParameter("store-to-cache")
    public void setStoreToCache(boolean storeToCache) {
        this.storeToCache = storeToCache;
    }

    @SetJIPipeDocumentation(name = "Number of threads", description = "Maximum number of threads that are allocated to the tasks. " +
            "Please note that the actual allocation depends on the algorithms.")
    @JIPipeParameter("num-threads")
    public int getNumThreads() {
        return numThreads;
    }

    @JIPipeParameter("num-threads")
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public boolean isStoreToDisk() {
        return storeToDisk;
    }

    public void setStoreToDisk(boolean storeToDisk) {
        this.storeToDisk = storeToDisk;
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
     * @return the set of nodes
     */
    public Set<UUID> getDisableStoreToCacheNodes() {
        return disableStoreToCacheNodes;
    }

    public void setDisableStoreToCacheNodes(Set<UUID> disableStoreToCacheNodes) {
        this.disableStoreToCacheNodes = disableStoreToCacheNodes;
    }

    /**
     * Allows to exclude specific nodes from saving any outputs to the disk
     *
     * @return the set of nodes
     */
    public Set<UUID> getDisableStoreToDiskNodes() {
        return disableStoreToDiskNodes;
    }

    public void setDisableStoreToDiskNodes(Set<UUID> disableStoreToDiskNodes) {
        this.disableStoreToDiskNodes = disableStoreToDiskNodes;
    }

    public JIPipeGraphRunPartitionInheritedBoolean getContinueOnFailureExportFailedInputs() {
        return continueOnFailureExportFailedInputs;
    }

    public void setContinueOnFailureExportFailedInputs(JIPipeGraphRunPartitionInheritedBoolean continueOnFailureExportFailedInputs) {
        this.continueOnFailureExportFailedInputs = continueOnFailureExportFailedInputs;
    }

    public JIPipeGraphRunPartitionInheritedBoolean getContinueOnFailure() {
        return continueOnFailure;
    }

    public void setContinueOnFailure(JIPipeGraphRunPartitionInheritedBoolean continueOnFailure) {
        this.continueOnFailure = continueOnFailure;
    }
}
