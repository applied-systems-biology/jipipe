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

package org.hkijena.jipipe.plugins.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings related to how algorithms are executed
 */
public class JIPipeRuntimeApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:runtime";
    private boolean allowSkipAlgorithmsWithoutInput = true;
    private boolean allowCache = true;
    private OptionalPathParameter tempDirectory = new OptionalPathParameter();
    private boolean perProjectTempDirectory = true;
    private int defaultRunThreads = 1;
    private int defaultQuickRunThreads = 1;
    private int logLimit = 15;

    /**
     * Creates a new instance
     */
    public JIPipeRuntimeApplicationSettings() {
    }

    public static JIPipeRuntimeApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeRuntimeApplicationSettings.class);
    }

    public static Path getTemporaryBaseDirectory() {
        if (JIPipe.getInstance() == null || !JIPipe.getInstance().getApplicationSettingsRegistry().getRegisteredSheets().containsKey(ID)) {
            return PathUtils.createGlobalTempDirectory("JIPipe");
        }
        OptionalPathParameter tempDirectory = getInstance().getTempDirectory();
        if (tempDirectory.isEnabled()) {
            try {
                if (tempDirectory.getContent().isAbsolute()) {
                    Files.createDirectories(tempDirectory.getContent());
                    return tempDirectory.getContent();
                } else {
                    Path absPath = Files.createDirectories(PathUtils.getJIPipeUserDir().resolve(tempDirectory.getContent()));
                    Files.createDirectories(absPath);
                    return absPath;
                }
            } catch (IOException e) {
                System.err.println("Fallback temporary directory due to following error:");
                e.printStackTrace();
                return PathUtils.createGlobalTempDirectory("JIPipe");
            }
        } else {
            return PathUtils.createGlobalTempDirectory("JIPipe");
        }
    }

    /**
     * Generates a temporary directory
     *
     * @param baseName optional base name
     * @return a temporary directory
     */
    public static Path getTemporaryDirectory(String baseName) {
        return PathUtils.createTempSubDirectory(getTemporaryBaseDirectory(), baseName);
    }

    /**
     * Generates a temporary directory
     *
     * @param prefix prefix
     * @param suffix suffix
     * @return a temporary directory
     */
    public static Path getTemporaryFile(String prefix, String suffix) {
        return PathUtils.createSubTempFilePath(getTemporaryBaseDirectory(), prefix, suffix);
    }

    @SetJIPipeDocumentation(name = "Temporary directory per project", description = "If enable, store temporary files are stored next to the current project file if possible")
    @JIPipeParameter("per-project-temp-directory")
    public boolean isPerProjectTempDirectory() {
        return perProjectTempDirectory;
    }

    @JIPipeParameter("per-project-temp-directory")
    public void setPerProjectTempDirectory(boolean perProjectTempDirectory) {
        this.perProjectTempDirectory = perProjectTempDirectory;
    }

    @SetJIPipeDocumentation(name = "Automatically skip algorithms without input", description = "If enabled, algorithms and their dependents without " +
            "input are silently ignored. If disabled, a project is not considered valid if such conditions happen.")
    @JIPipeParameter("allow-skip-algorithms-without-input")
    public boolean isAllowSkipAlgorithmsWithoutInput() {
        return allowSkipAlgorithmsWithoutInput;
    }

    @JIPipeParameter("allow-skip-algorithms-without-input")
    public void setAllowSkipAlgorithmsWithoutInput(boolean allowSkipAlgorithmsWithoutInput) {
        this.allowSkipAlgorithmsWithoutInput = allowSkipAlgorithmsWithoutInput;
    }

    @SetJIPipeDocumentation(name = "Enable data caching", description = "If enabled, JIPipe can cache generated to prevent repeating previous steps. " +
            "Please note that this can fill up the available memory.")
    @JIPipeParameter("allow-cache")
    public boolean isAllowCache() {
        return allowCache;
    }

    @JIPipeParameter("allow-cache")
    public void setAllowCache(boolean allowCache) {
        this.allowCache = allowCache;
    }

    @SetJIPipeDocumentation(name = "Override temporary directory", description = "For various tasks - like the Quick Run feature - data " +
            "must be placed into a directory. This defaults to your system's temporary directory. If there are issues with space, " +
            "you can provide an alternative path.")
    @JIPipeParameter("temp-directory")
    @PathParameterSettings(pathMode = PathType.DirectoriesOnly, ioMode = PathIOMode.Open)
    public OptionalPathParameter getTempDirectory() {
        return tempDirectory;
    }

    @JIPipeParameter("temp-directory")
    public void setTempDirectory(OptionalPathParameter tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    @SetJIPipeDocumentation(name = "Default thread count", description = "Default number of threads for running whole pipelines.")
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
        return true;
    }

    @SetJIPipeDocumentation(name = "Default thread count (quick run)", description = "Default number of threads for executing quick runs.")
    @JIPipeParameter("default-test-bench-threads")
    public int getDefaultQuickRunThreads() {
        return defaultQuickRunThreads;
    }

    /**
     * Sets the number of threads for test bench
     *
     * @param defaultTestBenchThreads threads
     * @return if successful
     */
    @JIPipeParameter("default-test-bench-threads")
    public boolean setDefaultQuickRunThreads(int defaultTestBenchThreads) {
        if (defaultTestBenchThreads <= 0)
            return false;
        this.defaultQuickRunThreads = defaultTestBenchThreads;
        return true;
    }

    @SetJIPipeDocumentation(name = "Log limit", description = "Limits how many logs are kept")
    @JIPipeParameter("log-limit")
    public int getLogLimit() {
        return logLimit;
    }

    @JIPipeParameter("log-limit")
    public void setLogLimit(int logLimit) {
        this.logLimit = logLimit;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.General;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/play.png");
    }

    @Override
    public String getName() {
        return "Runtime";
    }

    @Override
    public String getDescription() {
        return "General properties of JIPipe runs (number of threads, etc.)";
    }
}
