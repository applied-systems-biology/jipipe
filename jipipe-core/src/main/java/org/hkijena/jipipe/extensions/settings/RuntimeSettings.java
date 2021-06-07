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

package org.hkijena.jipipe.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalPathParameter;
import org.hkijena.jipipe.ui.components.PathEditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings related to how algorithms are executed
 */
public class RuntimeSettings implements JIPipeParameterCollection {
    public static final String ID = "runtime";

    private EventBus eventBus = new EventBus();
    private boolean allowSkipAlgorithmsWithoutInput = true;
    private boolean allowCache = true;
    private OptionalPathParameter tempDirectory = new OptionalPathParameter();
    private int defaultRunThreads = 1;
    private int defaultTestBenchThreads = 1;
    private int realTimeRunDelay = 400;
    private boolean realTimeRunEnabled = false;
    private int logLimit = 15;

    /**
     * Creates a new instance
     */
    public RuntimeSettings() {
    }

    public static RuntimeSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, RuntimeSettings.class);
    }

    /**
     * Generates a temporary directory
     *
     * @param baseName optional base name
     * @return a temporary directory
     */
    public static Path generateTempDirectory(String baseName) {
        if (JIPipe.getInstance() == null || !JIPipe.getInstance().getSettingsRegistry().getRegisteredSheets().containsKey(ID)) {
            try {
                return Files.createTempDirectory("JIPipe" + baseName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        OptionalPathParameter tempDirectory = getInstance().getTempDirectory();
        if (tempDirectory.isEnabled()) {
            try {
                return Files.createTempDirectory(tempDirectory.getContent(), "JIPipe" + baseName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                return Files.createTempDirectory("JIPipe" + baseName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Generates a temporary directory
     *
     * @param prefix prefix
     * @param suffix suffix
     * @return a temporary directory
     */
    public static Path generateTempFile(String prefix, String suffix) {
        OptionalPathParameter tempDirectory = getInstance().getTempDirectory();
        if (tempDirectory.isEnabled()) {
            try {
                return Files.createTempFile(tempDirectory.getContent(), "JIPipe" + prefix, suffix);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                return Files.createTempFile("JIPipe" + prefix, suffix);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Automatically skip algorithms without input", description = "If enabled, algorithms and their dependents without " +
            "input are silently ignored. If disabled, a project is not considered valid if such conditions happen.")
    @JIPipeParameter("allow-skip-algorithms-without-input")
    public boolean isAllowSkipAlgorithmsWithoutInput() {
        return allowSkipAlgorithmsWithoutInput;
    }

    @JIPipeParameter("allow-skip-algorithms-without-input")
    public void setAllowSkipAlgorithmsWithoutInput(boolean allowSkipAlgorithmsWithoutInput) {
        this.allowSkipAlgorithmsWithoutInput = allowSkipAlgorithmsWithoutInput;
        eventBus.post(new ParameterChangedEvent(this, "allow-skip-algorithms-without-input"));
    }

    @JIPipeDocumentation(name = "Enable data caching", description = "If enabled, JIPipe can cache generated to prevent repeating previous steps. " +
            "Please note that this can fill up the available memory.")
    @JIPipeParameter("allow-cache")
    public boolean isAllowCache() {
        return allowCache;
    }

    @JIPipeParameter("allow-cache")
    public void setAllowCache(boolean allowCache) {
        this.allowCache = allowCache;
        eventBus.post(new ParameterChangedEvent(this, "allow-cache"));
    }

    @JIPipeDocumentation(name = "Override temporary directory", description = "For various tasks - like the Quick Run feature - data " +
            "must be placed into a directory. This defaults to your system's temporary directory. If there are issues with space, " +
            "you can provide an alternative path.")
    @JIPipeParameter("temp-directory")
    @FilePathParameterSettings(pathMode = PathEditor.PathMode.DirectoriesOnly, ioMode = PathEditor.IOMode.Open)
    public OptionalPathParameter getTempDirectory() {
        return tempDirectory;
    }

    @JIPipeParameter("temp-directory")
    public void setTempDirectory(OptionalPathParameter tempDirectory) {
        this.tempDirectory = tempDirectory;
        eventBus.post(new ParameterChangedEvent(this, "temp-directory"));
    }

    @JIPipeDocumentation(name = "Default thread count", description = "Default number of threads for running whole pipelines.")
    @JIPipeParameter("default-run-threads")
    public int getDefaultRunThreads() {
        return defaultRunThreads;
    }

    /**
     * Sets the number of threads
     *
     * @param defaultRunThreads threads
     * @return if successful
     */
    @JIPipeParameter("default-run-threads")
    public boolean setDefaultRunThreads(int defaultRunThreads) {
        if (defaultRunThreads <= 0)
            return false;
        this.defaultRunThreads = defaultRunThreads;
        eventBus.post(new ParameterChangedEvent(this, "default-run-threads"));
        return true;
    }

    @JIPipeDocumentation(name = "Default thread count (quick run)", description = "Default number of threads for executing quick runs.")
    @JIPipeParameter("default-test-bench-threads")
    public int getDefaultTestBenchThreads() {
        return defaultTestBenchThreads;
    }

    /**
     * Sets the number of threads for test bench
     *
     * @param defaultTestBenchThreads threads
     * @return if successful
     */
    @JIPipeParameter("default-test-bench-threads")
    public boolean setDefaultTestBenchThreads(int defaultTestBenchThreads) {
        if (defaultTestBenchThreads <= 0)
            return false;
        this.defaultTestBenchThreads = defaultTestBenchThreads;
        eventBus.post(new ParameterChangedEvent(this, "default-test-bench-threads"));
        return true;
    }

    @JIPipeDocumentation(name = "Real-time update delay", description = "Delay in milliseconds that is added to after the graph was changed and a real-time update is started. A higher delay decreases the required resources.")
    @JIPipeParameter("real-time-run-delay")
    public int getRealTimeRunDelay() {
        return realTimeRunDelay;
    }

    @JIPipeParameter("real-time-run-delay")
    public void setRealTimeRunDelay(int realTimeRunDelay) {
        this.realTimeRunDelay = realTimeRunDelay;
    }

    @JIPipeDocumentation(name = "Real-time update caches", description = "If enabled, caches are automatically updated on changes in the graph or parameters. Please note that this can require a lot of resources from your machine depending on the workload.")
    @JIPipeParameter("real-time-run-enabled")
    public boolean isRealTimeRunEnabled() {
        return realTimeRunEnabled;
    }

    @JIPipeParameter("real-time-run-enabled")
    public void setRealTimeRunEnabled(boolean realTimeRunEnabled) {
        this.realTimeRunEnabled = realTimeRunEnabled;
    }

    @JIPipeDocumentation(name = "Log limit", description = "Limits how many logs are kept")
    @JIPipeParameter("log-limit")
    public int getLogLimit() {
        return logLimit;
    }

    @JIPipeParameter("log-limit")
    public void setLogLimit(int logLimit) {
        this.logLimit = logLimit;
    }
}
