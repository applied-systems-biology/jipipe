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

import org.apache.commons.io.FilenameUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UnclosableOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A storage that creates a ZIP file
 */
public class JIPipeZIPWriteDataStorage implements JIPipeWriteDataStorage {

    private final JIPipeProgressInfo progressInfo;
    private final Path zipFilePath;

    private final Path internalPath;

    private Path temporaryStorage;

    private FileOutputStream fileOutputStream;

    private ZipOutputStream zipOutputStream;

    private JIPipeZIPWriteDataStorage parent;

    public JIPipeZIPWriteDataStorage(JIPipeProgressInfo progressInfo, Path zipFilePath) {
        this.progressInfo = progressInfo;
        this.zipFilePath = zipFilePath;
        this.internalPath = Paths.get("");
        try {
            initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JIPipeZIPWriteDataStorage(JIPipeProgressInfo progressInfo, Path zipFilePath, Path internalPath) {
        this.progressInfo = progressInfo;
        this.zipFilePath = zipFilePath;
        this.internalPath = internalPath;
    }

    private void initialize() throws IOException {
        fileOutputStream = new FileOutputStream(zipFilePath.toFile());
        zipOutputStream = new ZipOutputStream(fileOutputStream);
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public boolean isFileSystemPathInitialized() {
        return temporaryStorage != null;
    }

    @Override
    public Path getFileSystemPath() {
        Path path = getOrCreateTemporaryStorageRoot().resolve(getInternalPath());
        if (!Files.isDirectory(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return path;
    }

    private Path getOrCreateTemporaryStorageRoot() {
        if (parent != null) {
            return parent.getOrCreateTemporaryStorageRoot();
        }
        if (temporaryStorage == null) {
            temporaryStorage = JIPipeRuntimeApplicationSettings.getTemporaryDirectory("zip");
        }
        return temporaryStorage;
    }

    @Override
    public Path getInternalPath() {
        return internalPath;
    }

    @Override
    public JIPipeZIPWriteDataStorage resolve(Path path) {
        JIPipeZIPWriteDataStorage result = new JIPipeZIPWriteDataStorage(progressInfo, zipFilePath, internalPath.resolve(path));
        result.fileOutputStream = fileOutputStream;
        result.zipOutputStream = zipOutputStream;
        result.parent = this;
        return result;
    }

    @Override
    public OutputStream write(Path path) {
        try {
            Path fullPath = internalPath.resolve(path);
            getProgressInfo().log("Creating ZIP entry " + FilenameUtils.separatorsToUnix(fullPath.toString()));
            zipOutputStream.putNextEntry(new ZipEntry(FilenameUtils.separatorsToUnix(fullPath.toString())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new UnclosableOutputStream(zipOutputStream);
    }

    @Override
    public void close() throws IOException {
        // Only the root storage can apply the operation
        if (parent != null) {
            parent.close();
            return;
        }

        if (temporaryStorage != null) {
            try (Stream<Path> stream = Files.walk(temporaryStorage)) {
                stream.forEach(path -> {
                    if (Files.isDirectory(path))
                        return;
                    Path relativePath = temporaryStorage.relativize(path);
                    getProgressInfo().log("ZIP " + path + " -> " + relativePath);
                    try {
                        zipOutputStream.putNextEntry(new ZipEntry(FilenameUtils.separatorsToUnix(relativePath.toString())));
                        Files.copy(path, zipOutputStream);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        // Close the file stream
        zipOutputStream.close();
        fileOutputStream.close();

        // Remove temporary files
        if (temporaryStorage != null) {
            PathUtils.deleteDirectoryRecursively(temporaryStorage, progressInfo.resolve("Cleaning temporary files"));
        }
    }

    @Override
    public String toString() {
        return "{ZIP Write} /" + getInternalPath() + " -> " + zipFilePath + " /" + getInternalPath();
    }

}
