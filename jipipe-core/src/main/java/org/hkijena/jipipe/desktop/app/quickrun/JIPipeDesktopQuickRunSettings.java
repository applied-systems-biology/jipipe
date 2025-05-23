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

package org.hkijena.jipipe.desktop.app.quickrun;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.nio.file.Path;

/**
 * Settings for {@link JIPipeDesktopQuickRun}
 */
public class JIPipeDesktopQuickRunSettings extends AbstractJIPipeParameterCollection {
    private Path outputPath;
    private boolean loadFromCache = true;
    private boolean storeToCache = true;
    private boolean saveToDisk = true;
    private boolean excludeSelected = false;
    private boolean storeIntermediateResults = false;

    private boolean silent = false;
    private int numThreads = JIPipeRuntimeApplicationSettings.getInstance().getDefaultQuickRunThreads();

    /**
     * Creates a new instance
     */
    public JIPipeDesktopQuickRunSettings(JIPipeProject project) {
        if (project != null) {
            outputPath = project.newTemporaryDirectory();
        } else {
            outputPath = JIPipeRuntimeApplicationSettings.getTemporaryDirectory("run");
        }
    }

    @SetJIPipeDocumentation(name = "Output path", description = "The path where the results are stored. " +
            "By default, this is a temporary directory. But for some cases, the result data might be too large for a temporary directory. " +
            "In this case, please select a directory that can hold the data.")
    @JIPipeParameter(value = "output-path", uiOrder = -999)
    @PathParameterSettings(ioMode = PathIOMode.Save, pathMode = PathType.DirectoriesOnly)
    public Path getOutputPath() {
        return outputPath;
    }

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

    @SetJIPipeDocumentation(name = "Save to output folder", description = "If enabled, outputs are saved.")
    @JIPipeParameter("save-outputs")
    public boolean isSaveToDisk() {
        return saveToDisk;
    }

    @JIPipeParameter("save-outputs")
    public void setSaveToDisk(boolean saveToDisk) {
        this.saveToDisk = saveToDisk;
    }

    @SetJIPipeDocumentation(name = "Exclude selected algorithm", description = "If enabled, only dependency algorithms are executed. " +
            "The currently selected algorithm is skipped. This might be useful when debugging the current algorithm.")
    @JIPipeParameter("exclude-selected")
    public boolean isExcludeSelected() {
        return excludeSelected;
    }

    @JIPipeParameter("exclude-selected")
    public void setExcludeSelected(boolean excludeSelected) {
        this.excludeSelected = excludeSelected;
    }

    @SetJIPipeDocumentation(name = "Store intermediate results", description = "If enabled, all intermediate results are also saved to disk/cache")
    @JIPipeParameter("store-intermediate-results")
    public boolean isStoreIntermediateResults() {
        return storeIntermediateResults;
    }

    @JIPipeParameter("store-intermediate-results")
    public void setStoreIntermediateResults(boolean storeIntermediateResults) {
        this.storeIntermediateResults = storeIntermediateResults;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }
}
