package org.hkijena.acaq5.extensions.filesystem.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.PathCollection;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides an input file
 */
@ACAQDocumentation(name = "File list")
@AlgorithmOutputSlot(value = ACAQFileData.class, slotName = "Filenames", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQFileListDataSource extends ACAQAlgorithm {

    private PathCollection fileNames = new PathCollection();
    private Path currentWorkingDirectory;

    /**
     * Initializes the algorithm
     *
     * @param declaration The algorithm declaration
     */
    public ACAQFileListDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ACAQFileListDataSource(ACAQFileListDataSource other) {
        super(other);
        this.fileNames.addAll(other.fileNames);
        this.currentWorkingDirectory = other.currentWorkingDirectory;
    }

    @Override
    public void run() {
        for (Path path : fileNames) {
            getFirstOutputSlot().addData(new ACAQFileData(path));
        }
    }

    /**
     * @return The file names
     */
    @ACAQParameter("file-names")
    @ACAQDocumentation(name = "File names")
    @FilePathParameterSettings(ioMode = FileSelection.IOMode.Open, pathMode = FileSelection.PathMode.FilesOnly)
    public PathCollection getFileNames() {
        return fileNames;
    }

    /**
     * Sets the file names
     *
     * @param fileNames The file names
     */
    @ACAQParameter("file-names")
    public void setFileNames(PathCollection fileNames) {
        this.fileNames = fileNames;
        getEventBus().post(new ParameterChangedEvent(this, "file-names"));
    }

    /**
     * @return Absolute file names
     */
    public PathCollection getAbsoluteFileNames() {
        PathCollection result = new PathCollection();
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
    public void reportValidity(ACAQValidityReport report) {
        for (Path fileName : getAbsoluteFileNames()) {
            if (fileName == null) {
                report.reportIsInvalid("An input file does not exist! Please provide a valid input file.");
            } else if (!Files.isRegularFile(fileName)) {
                report.reportIsInvalid("Input file '" + fileName + "' does not exist! Please provide a valid input file.");
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
            getEventBus().post(new ParameterChangedEvent(this, "file-names"));
        }
    }
}
