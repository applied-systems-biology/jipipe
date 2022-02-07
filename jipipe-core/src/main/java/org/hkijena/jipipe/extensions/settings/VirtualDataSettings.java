package org.hkijena.jipipe.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VirtualDataSettings implements JIPipeParameterCollection {
    public static String ID = "virtual-data";
    private final EventBus eventBus = new EventBus();

    private boolean virtualMode = false;
    private boolean largeVirtualDataTypesByDefault = true;
    private boolean virtualCache = true;
    private OptionalPathParameter tempDirectory = new OptionalPathParameter();

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Large data is virtual by default", description = "If enabled, nodes auto-configure themselves to store large data on hard drive when not used. You can disable this behavior on a per-slot level. " +
            "Please note the virtual data will require hard drive space and is by default stored in a temporary path. Choose another temporary folder in the runtime settings if your main drive does not have enough capacity.")
    @JIPipeParameter("default-make-large-data-virtual")
    public boolean isLargeVirtualDataTypesByDefault() {
        return largeVirtualDataTypesByDefault;
    }

    @JIPipeParameter("default-make-large-data-virtual")
    public void setLargeVirtualDataTypesByDefault(boolean largeVirtualDataTypesByDefault) {
        this.largeVirtualDataTypesByDefault = largeVirtualDataTypesByDefault;
    }

    @JIPipeDocumentation(name = "Project-wide cache is virtual", description = "If enabled, the project-wide cache is stored on the hard drive. Otherwise it is stored in your system memory. Hard drive access is slower, but usually not as limited as system memory. " +
            "Please note the virtual data will require hard drive space and is by default stored in a temporary path. Choose another temporary folder in the runtime settings if your main drive does not have enough capacity.")
    @JIPipeParameter("virtual-cache")
    public boolean isVirtualCache() {
        return virtualCache;
    }

    @JIPipeParameter("virtual-cache")
    public void setVirtualCache(boolean virtualCache) {
        this.virtualCache = virtualCache;
    }

    @JIPipeDocumentation(name = "Override virtual cache directory", description = "This defaults to your system's temporary directory. If there are issues with space, " +
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

    @JIPipeDocumentation(name = "Enable virtual storage", description = "If enabled, data will be temporarily stored to the HDD until required. This reduces memory consumption, but requires disk space and increases the run time.")
    @JIPipeParameter("virtual-mode")
    public boolean isVirtualMode() {
        return virtualMode;
    }

    @JIPipeParameter("virtual-mode")
    public void setVirtualMode(boolean virtualMode) {
        this.virtualMode = virtualMode;
    }

    public static VirtualDataSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, VirtualDataSettings.class);
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
}
