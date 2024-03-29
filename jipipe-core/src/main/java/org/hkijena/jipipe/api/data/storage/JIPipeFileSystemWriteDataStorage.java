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

package org.hkijena.jipipe.api.data.storage;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Storage on a file system. This is the simplest {@link JIPipeWriteDataStorage}.
 */
public class JIPipeFileSystemWriteDataStorage implements JIPipeWriteDataStorage {
    private final JIPipeProgressInfo progressInfo;
    private final Path fileSystemPath;
    private final Path internalPath;

    public JIPipeFileSystemWriteDataStorage(JIPipeProgressInfo progressInfo, Path fileSystemPath) {
        this.progressInfo = progressInfo;
        this.fileSystemPath = fileSystemPath;
        this.internalPath = Paths.get("");
    }

    public JIPipeFileSystemWriteDataStorage(JIPipeProgressInfo progressInfo, Path fileSystemPath, Path internalPath) {
        this.progressInfo = progressInfo;
        this.fileSystemPath = fileSystemPath;
        this.internalPath = internalPath;
    }

    @Override
    public Path getInternalPath() {
        return internalPath;
    }

    @Override
    public boolean isFileSystemPathInitialized() {
        return true;
    }

    @Override
    public Path getFileSystemPath() {
        return fileSystemPath;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public JIPipeWriteDataStorage resolve(Path path) {
        Path newPath = getFileSystemPath().resolve(path);
        try {
            Files.createDirectories(newPath);
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e, "Unable to create directory '" + newPath + "'!",
                    "The path might be invalid, or you might not have the permissions to write in a parent folder.",
                    "Check if the path is valid, and you have write-access.");
        }
        return new JIPipeFileSystemWriteDataStorage(progressInfo, newPath, getInternalPath().resolve(path));
    }

    @Override
    public OutputStream write(Path path) {
        try {
            Path outputPath = getFileSystemPath().resolve(path);
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
            return new FileOutputStream(outputPath.toFile(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "{Filesystem Write} /" + getInternalPath() + " @ " + getFileSystemPath();
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }
}
