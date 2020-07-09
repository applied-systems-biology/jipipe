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

package org.hkijena.pipelinej.api.testbench;

import com.google.common.eventbus.EventBus;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.pipelinej.extensions.settings.RuntimeSettings;
import org.hkijena.pipelinej.ui.components.PathEditor;

import java.nio.file.Path;

/**
 * Settings for {@link ACAQTestBench}
 */
public class ACAQTestBenchSettings implements ACAQParameterCollection {
    private EventBus eventBus = new EventBus();
    private Path outputPath;
    private boolean loadFromCache = true;
    private boolean storeToCache = true;
    private int numThreads = RuntimeSettings.getInstance().getDefaultTestBenchThreads();

    /**
     * Creates a new instance
     */
    public ACAQTestBenchSettings() {
        outputPath = RuntimeSettings.generateTempDirectory("TestBench");
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQDocumentation(name = "Output path", description = "The path where the results are stored. " +
            "By default, this is a temporary directory. But for some cases, the result data might be too large for a temporary directory. " +
            "In this case, please select a directory that can hold the data.")
    @ACAQParameter(value = "output-path", uiOrder = -999)
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Save, pathMode = PathEditor.PathMode.DirectoriesOnly)
    public Path getOutputPath() {
        return outputPath;
    }

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
}
