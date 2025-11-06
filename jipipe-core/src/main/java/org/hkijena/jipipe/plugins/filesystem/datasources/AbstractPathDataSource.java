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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.RegisterJIPipeParameterCollectionContextAction;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.filesystem.JIPipeFilesystemPluginApplicationSettings;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.PathForm;
import org.hkijena.jipipe.utils.PathUtils;

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
    private PathForm outputPathForm = PathForm.Absolute;

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
        this.outputPathForm = other.outputPathForm;
    }

    private void initializeDefaults() {
        JIPipeFilesystemPluginApplicationSettings settings = JIPipeFilesystemPluginApplicationSettings.getInstance();
        if (settings != null) {
            autoUpdateOutputSlotLabel = settings.isAutoLabelOutputWithFileName();
        }
    }

    public void updateOutputSlotIfEnabled() {
        if (autoUpdateOutputSlotLabel) {
            List<Path> paths = getPathsAs(PathForm.Any);
            String name = paths.size() == 1 ? paths.getFirst().getFileName().toString() : "";
            if (!Objects.equals(getFirstOutputSlot().getInfo().getCustomName(), name)) {
                getFirstOutputSlot().getInfo().setCustomName(name);
                getNodeSlotsChangedEventEmitter().emit(new NodeSlotsChangedEvent(this));
            }
        } else {
            getFirstOutputSlot().getInfo().setCustomName("");
            getNodeSlotsChangedEventEmitter().emit(new NodeSlotsChangedEvent(this));
        }
    }

    @Override
    public void archiveReportValidation(JIPipeValidationReportContext context, JIPipeValidationReport report, Path originalBaseDirectory) {
        List<Path> absoluteFileNames = getPathsAs(PathForm.Absolute);
        List<Path> paths = getPathsAs(PathForm.Any);

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
        for (Path path : getPathsAs(PathForm.Absolute)) {
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
        if (autoRelativizePaths) {
            autoRelativizePaths(baseDirectory);
        }
        currentWorkingDirectory = baseDirectory;
    }

    private void autoRelativizePaths(Path baseDirectory) {
        JIPipeFilesystemPluginApplicationSettings settings = JIPipeFilesystemPluginApplicationSettings.getInstance();
        if (settings == null || settings.isRelativizePaths()) {
            boolean modified = false;
            List<Path> paths = getPaths_();
            for (int i = 0; i < paths.size(); ++i) {
                Path path = paths.get(i);
                if (path != null) {
                    if (path.isAbsolute()) {
                        if (baseDirectory != null && path.startsWith(baseDirectory)) {
                            path = baseDirectory.relativize(path);
                            modified = true;
                        }
                    }
                }
                if (modified) {
                    paths.set(i, path);
                }
            }
            if (modified) {
                setPaths_(paths);
            }
        }
    }

    @SetJIPipeDocumentation(name = "To absolute", description = "Converts the stored paths to absolute paths.")
    @RegisterJIPipeParameterCollectionContextAction(icon = "data-types/path.png")
    public void convertPathsToAbsolute() {
        setPaths_(getPathsAs(PathForm.Absolute));
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "To relative", description = "Converts the stored paths to paths relative to the project directory (if available).")
    @RegisterJIPipeParameterCollectionContextAction(icon = "data-types/path.png")
    public void convertPathsToRelative() {
        setPaths_(getPathsAs(PathForm.Relative));
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

    @SetJIPipeDocumentation(name = "Output path form", description = "Whether the generated paths are absolute, relative, or a mixture (kep as-is)")
    @JIPipeParameter("output-path-form")
    public PathForm getOutputPathForm() {
        return outputPathForm;
    }

    @JIPipeParameter("output-path-form")
    public void setOutputPathForm(PathForm outputPathForm) {
        this.outputPathForm = outputPathForm;
    }

    @Override
    public Set<Path> archiveDiscoverExternalPaths(JIPipeProgressInfo progressInfo) {
        return new HashSet<>(getPathsAs(PathForm.Absolute));
    }

    @Override
    public void archiveUpdateExternalPaths(Map<Path, Path> updateMap, JIPipeProgressInfo progressInfo) {
        List<Path> updatedPaths = new ArrayList<>();
        for (Path src : getPathsAs(PathForm.Absolute)) {
            Path dst = updateMap.get(src);
            if (dst != null) {
                updatedPaths.add(dst);
            } else {
                progressInfo.aggressiveError("MISSING MAPPING", src.toString(), "to", "?");
            }
        }
        setPaths_(updatedPaths);
    }

    public Path getCurrentWorkingOrProjectDirectory() {
        if (!PathUtils.isNullOrEmpty(currentWorkingDirectory) && Files.isDirectory(currentWorkingDirectory)) {
            return currentWorkingDirectory;
        } else {
            return getProjectDirectory();
        }
    }

    public List<Path> getPathsAs(PathForm type) {

        Path baseDir = getCurrentWorkingOrProjectDirectory();

        if (type == PathForm.Relative) {
            List<Path> result = new ArrayList<>();
            for (Path path : getPaths_()) {
                if (path.isAbsolute()) {
                    if (!PathUtils.isNullOrEmpty(baseDir) && path.startsWith(baseDir)) {
                        result.add(baseDir.relativize(path).normalize());
                    } else {
                        // Use user home
                        result.add(PathUtils.getHomeDirectory().relativize(path).normalize());
                    }
                } else {
                    result.add(path.normalize());
                }
            }
            return result;
        } else if (type == PathForm.Absolute) {
            List<Path> result = new ArrayList<>();
            for (Path path : getPaths_()) {
                if (!path.isAbsolute()) {
                    if (!PathUtils.isNullOrEmpty(baseDir)) {
                        result.add(baseDir.resolve(path).normalize());
                    } else {
                        // Use user home
                        result.add(PathUtils.getHomeDirectory().resolve(path).normalize());
                    }
                } else {
                    result.add(path.normalize());
                }
            }
            return result;
        } else {
            return getPaths_();
        }
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (Path path : getPathsAs(outputPathForm)) {
            getFirstOutputSlot().addData(new PathData(path), JIPipeDataContext.create(this), progressInfo);
        }
    }

    /**
     * Internal method that gets the list of paths that are contained within this node's parameters
     *
     * @return the list of paths
     */
    protected abstract List<Path> getPaths_();

    /**
     * Internal method that sets the path parameter(s) to the given paths
     *
     * @param paths the paths
     */
    protected abstract void setPaths_(List<Path> paths);

}
