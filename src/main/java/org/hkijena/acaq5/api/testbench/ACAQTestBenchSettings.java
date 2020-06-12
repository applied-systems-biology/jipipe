package org.hkijena.acaq5.api.testbench;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.PathEditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings for {@link ACAQTestBench}
 */
public class ACAQTestBenchSettings implements ACAQParameterCollection {
    private EventBus eventBus = new EventBus();
    private Path outputPath;
    private boolean loadFromCache = true;
    private boolean storeToCache = true;

    /**
     * Creates a new instance
     */
    public ACAQTestBenchSettings() {
        try {
            outputPath = Files.createTempDirectory("ACAQ5-TestBench");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
