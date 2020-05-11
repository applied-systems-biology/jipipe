package org.hkijena.acaq5.extensions.filesystem.api.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FileData;
import org.hkijena.acaq5.extensions.parameters.editors.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides an input file
 */
@ACAQDocumentation(name = "File")
@AlgorithmOutputSlot(value = FileData.class, slotName = "Filename", autoCreate = true)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
public class FileDataSource extends ACAQAlgorithm {

    private Path currentWorkingDirectory;
    private Path fileName;

    /**
     * Initializes the algorithm
     *
     * @param declaration The algorithm declaration
     */
    public FileDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FileDataSource(FileDataSource other) {
        super(other);
        this.fileName = other.fileName;
        this.currentWorkingDirectory = other.currentWorkingDirectory;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        getFirstOutputSlot().addData(new FileData(fileName));
    }

    /**
     * @return The file name
     */
    @ACAQParameter("file-name")
    @ACAQDocumentation(name = "File name")
    @FilePathParameterSettings(ioMode = FileSelection.IOMode.Open, pathMode = FileSelection.PathMode.FilesOnly)
    public Path getFileName() {
        return fileName;
    }

    /**
     * Sets the file name
     *
     * @param fileName The file name
     */
    @ACAQParameter("file-name")
    public void setFileName(Path fileName) {
        this.fileName = fileName;
        getEventBus().post(new ParameterChangedEvent(this, "file-name"));
    }

    /**
     * @return The file name as absolute path
     */
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
            report.reportIsInvalid("Input file does not exist!",
                    "The file '" + getAbsoluteFileName() + "' does not exist.",
                    "Please provide a valid input file.",
                    this);
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
