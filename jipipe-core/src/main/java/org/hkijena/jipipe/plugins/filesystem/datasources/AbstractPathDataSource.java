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

package org.hkijena.jipipe.plugins.filesystem.datasources;

import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.RegisterJIPipeParameterCollectionContextAction;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.filesystem.JIPipeFilesystemPluginApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathList;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Base class for all the data source algorithms
 */
public abstract class AbstractPathDataSource extends JIPipeAlgorithm {

    private Path currentWorkingDirectory;
    private boolean autoRelativizePaths = true;
    private boolean autoUpdateOutputSlotLabel = true;

    public AbstractPathDataSource(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
        initializeDefaults();
    }

    public AbstractPathDataSource(JIPipeNodeInfo info) {
        super(info);
        initializeDefaults();
    }

    public AbstractPathDataSource(AbstractPathDataSource other) {
        super(other);
        this.currentWorkingDirectory = other.currentWorkingDirectory;
        this.autoRelativizePaths = other.autoRelativizePaths;
        this.autoUpdateOutputSlotLabel = other.autoUpdateOutputSlotLabel;
    }

    private void initializeDefaults() {
        JIPipeFilesystemPluginApplicationSettings settings = JIPipeFilesystemPluginApplicationSettings.getInstance();
        if (settings != null) {
            autoUpdateOutputSlotLabel = settings.isAutoLabelOutputWithFileName();
        }
    }

    public void updateOutputSlotIfEnabled() {
        if (autoUpdateOutputSlotLabel) {
            List<Path> paths = getPathsAs(PathLinkageType.Any);
            String name = paths.size() == 1 ? paths.getFirst().getFileName().toString() : "";
            if (!Objects.equals(getFirstOutputSlot().getInfo().getCustomName(), name)) {
                getFirstOutputSlot().getInfo().setCustomName(name);
                getNodeSlotsChangedEventEmitter().emit(new NodeSlotsChangedEvent(this));
            }
        }
        else {
            getFirstOutputSlot().getInfo().setCustomName("");
            getNodeSlotsChangedEventEmitter().emit(new NodeSlotsChangedEvent(this));
        }
    }

    @Override
    public void reportArchiveValidation(JIPipeValidationReportContext context, JIPipeValidationReport report, Path originalBaseDirectory) {
        List<Path> absoluteFileNames = getPathsAs(PathLinkageType.Absolute);
        List<Path> paths = getPathsAs(PathLinkageType.Any);

        for (int i = 0; i < absoluteFileNames.size(); i++) {
            Path source = absoluteFileNames.get(i);
            if (source == null || !Files.exists(source)) {
                context.warning().title("Unable to find path").explanation("The path " + paths.get(i) + " does not exist").report(report);
            } else {
                if (!source.startsWith(originalBaseDirectory)) {
                    context.warning().title("Path not relative to project").explanation("The path " + paths.get(i) + " is not located relative to the project file. The resulting archive will contain directories with randomly generated names.").report(report);
                }
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        for (Path path : getPathsAs(PathLinkageType.Absolute)) {
            if (PathUtils.isNullOrEmpty(path)) {
                reportContext.warning().title("Input path not set!").explanation("One of the paths is not set.").solution("Please provide a valid input path.").report(report);
            } else if (!Files.exists(path)) {
                reportContext.warning().title("Input path does not exist!").explanation("The path '" + path + "' does not exist.").solution("Please provide a valid input path.").report(report);
            }
        }
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);
        if(autoRelativizePaths) {
            autoRelativizePaths(baseDirectory);
        }
        currentWorkingDirectory = baseDirectory;
    }

    private void autoRelativizePaths(Path baseDirectory) {
        boolean modified = false;
        List<Path> paths = getPaths_();
        for (int i = 0; i < paths.size(); ++i) {
            Path folderPath = paths.get(i);
            if (folderPath != null) {
                // Make absolute
                if (!folderPath.isAbsolute()) {
                    if (currentWorkingDirectory != null) {
                        folderPath = currentWorkingDirectory.resolve(folderPath);
                        modified = true;
                    } else if (baseDirectory != null) {
                        folderPath = baseDirectory.resolve(folderPath);
                        modified = true;
                    }
                }
                // Make relative if already absolute and workDirectory != null
                JIPipeFilesystemPluginApplicationSettings settings = JIPipeFilesystemPluginApplicationSettings.getInstance();
                if (settings == null || settings.isRelativizePaths()) {
                    if (folderPath.isAbsolute()) {
                        if (baseDirectory != null && folderPath.startsWith(baseDirectory)) {
                            folderPath = baseDirectory.relativize(folderPath);
                            modified = true;
                        }
                    }
                }

                if (modified) {
                    paths.set(i, folderPath);
                }
            }
        }
        if(modified) {
            setPaths_(paths);
        }
    }

    @SetJIPipeDocumentation(name = "To absolute", description = "Converts the stored paths to absolute paths.")
    @RegisterJIPipeParameterCollectionContextAction(icon = "data-types/path.png")
    public void convertPathsToAbsolute() {
        setPaths_(getPathsAs(PathLinkageType.Absolute));
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "To relative", description = "Converts the stored paths to paths relative to the project directory (if available).")
    @RegisterJIPipeParameterCollectionContextAction(icon = "data-types/path.png")
    public void convertPathsToRelative() {
        setPaths_(getPathsAs(PathLinkageType.Relative));
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Automatically make paths relative", description = "If enabled, absolute paths are made relative to the project file when the project is saved.")
    @JIPipeParameter("auto-relativize-paths")
    public boolean isAutoRelativizePaths() {
        return autoRelativizePaths;
    }

    @JIPipeParameter("auto-relativize-paths")
    public void setAutoRelativizePaths(boolean autoRelativizePaths) {
        this.autoRelativizePaths = autoRelativizePaths;
    }

    @SetJIPipeDocumentation(name = "Auto-label output slot", description = "If enabled, the label of the output slot is automatically set to the current file/directory name if there is exactly one path")
    @JIPipeParameter("auto-update-output-slot-label")
    public boolean isAutoUpdateOutputSlotLabel() {
        return autoUpdateOutputSlotLabel;
    }

    @JIPipeParameter("auto-update-output-slot-label")
    public void setAutoUpdateOutputSlotLabel(boolean autoUpdateOutputSlotLabel) {
        this.autoUpdateOutputSlotLabel = autoUpdateOutputSlotLabel;
        updateOutputSlotIfEnabled();
    }

    @Override
    public void archiveTo(JIPipeWriteDataStorage projectStorage, JIPipeWriteDataStorage wrappedExternalStorage, JIPipeProgressInfo progressInfo, Path originalBaseDirectory, Path relativeInputsPath) {
        PathList relativeFileNames = getRelativePaths();
        PathList absoluteFileNames = getAbsolutePaths();
        List<Path> newPaths = new ArrayList<>();
        Set<String> externalFileNames = new HashSet<>();

        for (int i = 0; i < relativeFileNames.size(); i++) {
            Path source = absoluteFileNames.get(i);
            if (source == null || !Files.exists(source)) {
                throw new RuntimeException("Path " + relativeFileNames.get(i) + " does not exist!");
            } else {
                Path target;
                if (source.startsWith(originalBaseDirectory)) {
                    // The data is located in the project directory. We can directly copy the file.
                    Path relativePath = originalBaseDirectory.relativize(source);
                    target = projectStorage.getFileSystemPath().resolve(relativeInputsPath).resolve(relativePath);
                } else {
                    // The data is located outside the project directory. Needs to be copied into a unique directory.
                    String externalFileName = relativeFileNames.get(i).getFileName().toString();
                    if (!externalFileNames.contains(externalFileName)) {
                        // Not yet in external storage. Add it
                        target = wrappedExternalStorage.resolve(relativeInputsPath).resolve(getAliasIdInParentGraph()).getFileSystemPath().resolve(externalFileName);
                        externalFileNames.add(externalFileName);
                    } else {
                        // We need to make a new target dir (UUID)
                        progressInfo.log("Warning: Duplicate path name in external storage (" + externalFileName + "). Creating new UUID sub-storage in " + getAliasIdInParentGraph());
                        target = wrappedExternalStorage.resolve(relativeInputsPath).resolve(getAliasIdInParentGraph()).resolve(UUID.randomUUID().toString()).getFileSystemPath().resolve(externalFileName);
                        externalFileNames.add(externalFileName);
                    }
                }

                if (Files.exists(target)) {
                    progressInfo.log("Not copying " + source + " -> " + target + " (Already exists)");
                    continue;
                }

                progressInfo.log("Copy " + source + " -> " + target);
                try {
                    Files.createDirectories(target.getParent());
                    if (Files.isRegularFile(source)) {
                        Files.copy(source, target);
                    } else {
                        FileUtils.copyDirectory(source.toFile(), target.toFile());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                newPaths.add(target);
            }
        }
        setPaths_(newPaths);
    }

    public List<Path> getPathsAs(PathLinkageType type) {
        if(type == PathLinkageType.Relative) {
            List<Path> result = new ArrayList<>();
            for (Path path : getPaths_()) {
                if(path.isAbsolute()) {
                    if(!PathUtils.isNullOrEmpty(currentWorkingDirectory) && path.startsWith(currentWorkingDirectory)) {
                        result.add(currentWorkingDirectory.relativize(path));
                    }
                    else {
                        // Use user home
                        result.add(PathUtils.getHomeDirectory().relativize(path));
                    }
                }
                else {
                    result.add(path);
                }
            }
            return result;
        }
        else if(type == PathLinkageType.Absolute) {
            List<Path> result = new ArrayList<>();
            for (Path path : getPaths_()) {
                if(!path.isAbsolute()) {
                    if(!PathUtils.isNullOrEmpty(currentWorkingDirectory)) {
                        result.add(currentWorkingDirectory.resolve(path));
                    }
                    else {
                        // Use user home
                        result.add(PathUtils.getHomeDirectory().resolve(path));
                    }
                }
                else {
                    result.add(path);
                }
            }
            return result;
        }
        else {
            return getPaths_();
        }
    }

    /**
     * Internal method that gets the list of paths that are contained within this node's parameters
     * @return the list of paths
     */
    protected abstract List<Path> getPaths_();

    /**
     * Internal method that sets the path parameter(s) to the given paths
     * @param paths the paths
     */
    protected abstract void setPaths_(List<Path> paths);

    public enum PathLinkageType {
        Any,
        Relative,
        Absolute
    }
}
