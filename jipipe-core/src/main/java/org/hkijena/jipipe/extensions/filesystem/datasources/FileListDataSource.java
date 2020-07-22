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
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.PathList;
import org.hkijena.jipipe.ui.components.PathEditor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides an input file
 */
@JIPipeDocumentation(name = "File list")
@JIPipeOutputSlot(value = FileData.class, slotName = "Filenames", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class FileListDataSource extends JIPipeAlgorithm {

    private PathList fileNames = new PathList();
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
        this.fileNames.addAll(other.fileNames);
        this.currentWorkingDirectory = other.currentWorkingDirectory;
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (Path path : fileNames) {
            getFirstOutputSlot().addData(new FileData(path));
        }
    }

    /**
     * @return The file names
     */
    @JIPipeParameter("file-names")
    @JIPipeDocumentation(name = "File names")
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.FilesOnly)
    public PathList getFileNames() {
        return fileNames;
    }

    /**
     * Sets the file names
     *
     * @param fileNames The file names
     */
    @JIPipeParameter("file-names")
    public void setFileNames(PathList fileNames) {
        this.fileNames = fileNames;

    }

    /**
     * @return Absolute file names
     */
    public PathList getAbsoluteFileNames() {
        PathList result = new PathList();
        for (Path fileName : fileNames) {
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
        for (int i = 0; i < fileNames.size(); ++i) {
            Path fileName = fileNames.get(i);
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
                if (fileName.isAbsolute()) {
                    if (workDirectory != null && fileName.startsWith(workDirectory)) {
                        fileName = workDirectory.relativize(fileName);
                        modified = true;
                    }
                }

                if (modified)
                    this.fileNames.set(i, fileName);
            }
        }
        currentWorkingDirectory = workDirectory;
        if (modified) {

        }
    }
}
