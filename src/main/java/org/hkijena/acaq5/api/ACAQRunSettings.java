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

package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.acaq5.extensions.settings.RuntimeSettings;
import org.hkijena.acaq5.ui.components.PathEditor;

import java.nio.file.Path;

/**
 * Settings for an {@link ACAQRun}
 */
public class ACAQRunSettings implements ACAQParameterCollection {
    private EventBus eventBus = new EventBus();
    private Path outputPath;
    private boolean loadFromCache = true;
    private boolean storeToCache = false;
    private int numThreads = RuntimeSettings.getInstance().getDefaultRunThreads();

    @ACAQParameter(value = "output-path", uiOrder = -999)
    @ACAQDocumentation(name = "Output folder")
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Save, pathMode = PathEditor.PathMode.DirectoriesOnly)
    public Path getOutputPath() {
        return outputPath;
    }

    /**
     * Sets the output path
     *
     * @param outputPath The output path
     */
    @ACAQParameter("output-path")
    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;

    }

    @ACAQDocumentation(name = "Load from cache", description = "If enabled, the results are automatically loaded from a cache if possible.")
    @ACAQParameter("load-from-cache")
    public boolean isLoadFromCache() {
        return loadFromCache;
    }

    @ACAQParameter("load-from-cache")
    public void setLoadFromCache(boolean loadFromCache) {
        this.loadFromCache = loadFromCache;
    }

    @ACAQDocumentation(name = "Save to cache", description = "If enabled, the results and intermediate results are stored into a cache. " +
            "This cache is limited by the RAM. Will be ignored if the global ACAQ5 cache settings are disabled.")
    @ACAQParameter("store-to-cache")
    public boolean isStoreToCache() {
        return storeToCache;
    }

    @ACAQParameter("store-to-cache")
    public void setStoreToCache(boolean storeToCache) {
        this.storeToCache = storeToCache;
    }

    @ACAQDocumentation(name = "Number of threads", description = "Maximum number of threads that are allocated to the tasks. " +
            "Please note that the actual allocation depends on the algorithms.")
    @ACAQParameter("num-threads")
    public int getNumThreads() {
        return numThreads;
    }

    @ACAQParameter("num-threads")
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
