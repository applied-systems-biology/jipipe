package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.parameters.primitives.FilePathParameterSettings;
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

    @ACAQParameter("output-path")
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
        getEventBus().post(new ParameterChangedEvent(this, "output-path"));
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
