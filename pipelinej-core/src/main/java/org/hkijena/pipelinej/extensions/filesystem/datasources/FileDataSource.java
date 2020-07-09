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

package org.hkijena.pipelinej.extensions.filesystem.datasources;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithm;
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.pipelinej.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.filesystem.dataypes.FileData;
import org.hkijena.pipelinej.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.pipelinej.ui.components.PathEditor;

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
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.FilesOnly)
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
