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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.components.PathEditor;

import java.nio.file.Path;

/**
 * Settings for an {@link JIPipeRun}
 */
public class JIPipeRunSettings implements JIPipeParameterCollection {
    private EventBus eventBus = new EventBus();
    private Path outputPath;
    private boolean loadFromCache = true;
    private boolean storeToCache = false;
    private boolean saveOutputs = true;
    private int numThreads = RuntimeSettings.getInstance().getDefaultRunThreads();
    private boolean silent = false;
    private boolean ignoreDeactivatedInputs = false;

    @JIPipeParameter(value = "output-path", uiOrder = -999)
    @JIPipeDocumentation(name = "Output folder")
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Save, pathMode = PathEditor.PathMode.DirectoriesOnly)
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

    public boolean isSaveOutputs() {
        return saveOutputs;
    }

    public void setSaveOutputs(boolean saveOutputs) {
        this.saveOutputs = saveOutputs;
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
     * @return if cascading deactivation is ignored
     */
    public boolean isIgnoreDeactivatedInputs() {
        return ignoreDeactivatedInputs;
    }

    public void setIgnoreDeactivatedInputs(boolean ignoreDeactivatedInputs) {
        this.ignoreDeactivatedInputs = ignoreDeactivatedInputs;
    }
}
