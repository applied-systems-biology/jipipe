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
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.ui.components.PathEditor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides an input file
 */
@JIPipeDocumentation(name = "File")
@AlgorithmOutputSlot(value = FileData.class, slotName = "Filename", autoCreate = true)
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.DataSource)
public class FileDataSource extends JIPipeAlgorithm {

    private Path currentWorkingDirectory;
    private Path fileName;

    /**
     * Initializes the algorithm
     *
     * @param declaration The algorithm declaration
     */
    public FileDataSource(JIPipeAlgorithmDeclaration declaration) {
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
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        getFirstOutputSlot().addData(new FileData(fileName));
    }

    /**
     * @return The file name
     */
    @JIPipeParameter("file-name")
    @JIPipeDocumentation(name = "File name")
    @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.FilesOnly)
    public Path getFileName() {
        return fileName;
    }

    /**
     * Sets the file name
     *
     * @param fileName The file name
     */
    @JIPipeParameter("file-name")
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
    public void reportValidity(JIPipeValidityReport report) {
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
