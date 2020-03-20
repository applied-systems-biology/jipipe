package org.hkijena.acaq5.extensions.filesystem.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides an input file
 */
@ACAQDocumentation(name = "File")
@AlgorithmOutputSlot(value = ACAQFileData.class, slotName = "Filename", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQFileDataSource extends ACAQAlgorithm {

    private Path currentWorkingDirectory;
    private Path fileName;

    public ACAQFileDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFileDataSource(ACAQFileDataSource other) {
        super(other);
        this.fileName = other.fileName;
        this.currentWorkingDirectory = other.currentWorkingDirectory;
    }

    @Override
    public void run() {
        getFirstOutputSlot().addData(new ACAQFileData(fileName));
    }

    @ACAQParameter("file-name")
    @ACAQDocumentation(name = "File name")
    @FilePathParameterSettings(ioMode = FileSelection.IOMode.Open, pathMode = FileSelection.PathMode.FilesOnly)
    public Path getFileName() {
        return fileName;
    }

    @ACAQParameter("file-name")
    public void setFileName(Path fileName) {
        this.fileName = fileName;
        getEventBus().post(new ParameterChangedEvent(this, "file-name"));
    }

    public Path getAbsoluteFileName() {
        if (fileName == null)
            return null;
        else if (currentWorkingDirectory != null)
            return currentWorkingDirectory.resolve(fileName);
        else
            return fileName;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (fileName == null || !Files.isRegularFile(getAbsoluteFileName()))
            report.reportIsInvalid("Input file does not exist! Please provide a valid input file.");
    }

    @Override
    public void setWorkDirectory(Path workDirectory) {
        super.setWorkDirectory(workDirectory);

        if (fileName != null) {
            // Make absolute
            if (!fileName.isAbsolute()) {
                if (currentWorkingDirectory != null) {
                    setFileName(currentWorkingDirectory.resolve(fileName));
                } else if (workDirectory != null) {
                    setFileName(workDirectory.resolve(fileName));
                }
            }
            // Make relative if already absolute and workDirectory != null
            if (fileName.isAbsolute()) {
                if (workDirectory != null && fileName.startsWith(workDirectory)) {
                    setFileName(workDirectory.relativize(fileName));
                }
            }
        }

        currentWorkingDirectory = workDirectory;
    }
}
