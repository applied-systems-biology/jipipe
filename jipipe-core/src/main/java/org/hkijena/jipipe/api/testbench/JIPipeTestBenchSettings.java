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

package org.hkijena.jipipe.api.testbench;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.components.PathEditor;

import java.nio.file.Path;

/**
 * Settings for {@link JIPipeTestBench}
 */
public class JIPipeTestBenchSettings implements JIPipeParameterCollection {
    private EventBus eventBus = new EventBus();
    private Path outputPath;
    private boolean loadFromCache = true;
    private boolean storeToCache = true;
    private boolean saveOutputs = true;
    private int numThreads = RuntimeSettings.getInstance().getDefaultTestBenchThreads();

    /**
     * Creates a new instance
     */
    public JIPipeTestBenchSettings() {
        outputPath = RuntimeSettings.generateTempDirectory("TestBench");
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Output path", description = "The path where the results are stored. " +
            "By default, this is a temporary directory. But for some cases, the result data might be too large for a temporary directory. " +
            "In this case, please select a directory that can hold the data.")
    @JIPipeParameter(value = "output-path", uiOrder = -999)
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Save, pathMode = PathEditor.PathMode.DirectoriesOnly)
    public Path getOutputPath() {
        return outputPath;
    }

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

    @JIPipeDocumentation(name = "Save to output folder", description = "If enabled, outputs are saved.")
    @JIPipeParameter("save-outputs")
    public boolean isSaveOutputs() {
        return saveOutputs;
    }

    @JIPipeParameter("save-outputs")
    public void setSaveOutputs(boolean saveOutputs) {
        this.saveOutputs = saveOutputs;
    }
}
