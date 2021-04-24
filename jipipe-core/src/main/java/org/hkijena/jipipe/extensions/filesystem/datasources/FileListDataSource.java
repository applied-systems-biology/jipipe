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

package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.FilesystemExtensionSettings;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.PathList;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides an input file
 */
@JIPipeDocumentation(name = "File list", description = "Converts each provided path into file data.")
@JIPipeOutputSlot(value = FileData.class, slotName = "Filenames", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class FileListDataSource extends JIPipeAlgorithm {

    private PathList files = new PathList();
    private Path currentWorkingDirectory;

    /**
     * Initializes the algorithm
     *
     * @param info The algorithm info
     */
    public FileListDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FileListDataSource(FileListDataSource other) {
        super(other);
        this.files.addAll(other.files);
        this.currentWorkingDirectory = other.currentWorkingDirectory;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        for (Path path : files) {
            getFirstOutputSlot().addData(new FileData(path), progressInfo);
        }
    }

    /**
     * @return The file names
     */
    @JIPipeParameter("file-names")
    @JIPipeDocumentation(name = "Files")
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.FilesOnly)
    public PathList getFiles() {
        return files;
    }

    /**
     * Sets the file names
     *
     * @param files The file names
     */
    @JIPipeParameter("file-names")
    public void setFiles(PathList files) {
        this.files = files;
        PathUtils.normalizeList(files);
        FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
        if (settings != null && settings.isAutoLabelOutputWithFileName()) {
            String name = files.size() == 1 ? files.get(0).getFileName().toString() : "";
            if (!Objects.equals(getFirstOutputSlot().getInfo().getCustomName(), name)) {
                getFirstOutputSlot().getInfo().setCustomName(name);
                getEventBus().post(new JIPipeGraph.NodeSlotsChangedEvent(this));
            }
        }
    }

    /**
     * @return Absolute file names
     */
    public PathList getAbsoluteFileNames() {
        PathList result = new PathList();
        for (Path fileName : files) {
            if (fileName == null)
                result.add(null);
            else if (currentWorkingDirectory != null)
                result.add(currentWorkingDirectory.resolve(fileName));
            else
                result.add(fileName);
        }
        return result;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        for (Path fileName : getAbsoluteFileNames()) {
            if (fileName == null) {
                report.reportIsInvalid("Invalid file path!",
                        "An input file does not exist!",
                        "Please provide a valid input file.",
                        this);
            } else if (!Files.isRegularFile(fileName)) {
                report.reportIsInvalid("Invalid file path!",
                        "Input file '" + fileName + "' does not exist!",
                        "Please provide a valid input file.",
                        this);
            }
        }
    }

    @Override
    public void setWorkDirectory(Path workDirectory) {
        super.setWorkDirectory(workDirectory);

        boolean modified = false;
        for (int i = 0; i < files.size(); ++i) {
            Path fileName = files.get(i);
            if (fileName != null) {
                // Make absolute
                if (!fileName.isAbsolute()) {
                    if (currentWorkingDirectory != null) {
                        fileName = currentWorkingDirectory.resolve(fileName);
                        modified = true;
                    } else if (workDirectory != null) {
                        fileName = workDirectory.resolve(fileName);
                        modified = true;
                    }
                }
                // Make relative if already absolute and workDirectory != null
                FilesystemExtensionSettings settings = FilesystemExtensionSettings.getInstance();
                if (settings == null || settings.isRelativizePaths()) {
                    if (fileName.isAbsolute()) {
                        if (workDirectory != null && fileName.startsWith(workDirectory)) {
                            fileName = workDirectory.relativize(fileName);
                            modified = true;
                        }
                    }
                }

                if (modified)
                    this.files.set(i, fileName);
            }
        }
        currentWorkingDirectory = workDirectory;
    }
}
