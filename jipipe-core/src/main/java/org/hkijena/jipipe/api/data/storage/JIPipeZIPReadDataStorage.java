package org.hkijena.jipipe.api.data.storage;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public class JIPipeZIPReadDataStorage implements JIPipeReadDataStorage {

    private final Path zipFilePath;

    private final JIPipeProgressInfo progressInfo;

    private final Path internalPath;

    private Path temporaryStorage;

    private JIPipeZIPReadDataStorage parent;

    public JIPipeZIPReadDataStorage(JIPipeProgressInfo progressInfo, Path zipFilePath) {
        this.zipFilePath = zipFilePath;
        this.progressInfo = progressInfo;
        this.internalPath = Paths.get("");
        initialize();
    }

    private void initialize() {
    }

    private JIPipeZIPReadDataStorage(JIPipeProgressInfo progressInfo, Path zipFilePath, Path internalPath) {
        this.zipFilePath = zipFilePath;
        this.progressInfo = progressInfo;
        this.internalPath = internalPath;
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
        if(!Files.isDirectory(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return path;
    }

    private Path getOrCreateTemporaryStorageRoot() {
        if(parent != null) {
            return parent.getOrCreateTemporaryStorageRoot();
        }
        if(temporaryStorage == null) {
            temporaryStorage = RuntimeSettings.generateTempDirectory("zip");
            try {
                ArchiveUtils.decompressZipFile(zipFilePath, temporaryStorage, progressInfo.resolve("Extract ZIP"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return temporaryStorage;
    }

    @Override
    public Path getInternalPath() {
        return internalPath;
    }

    @Override
    public JIPipeReadDataStorage resolve(Path path) {
        JIPipeZIPReadDataStorage result = new JIPipeZIPReadDataStorage(progressInfo, zipFilePath, internalPath.resolve(path));
        result.fileOutputStream = fileOutputStream;
        result.zipOutputStream = zipOutputStream;
        result.parent = this;
        return result;
    }

    @Override
    public boolean isFile(Path path) {
        return false;
    }

    @Override
    public boolean exists(Path path) {
        return false;
    }

    @Override
    public Collection<Path> list() {
        return null;
    }

    @Override
    public InputStream open(Path path) {
        return null;
    }

    @Override
    public void close() throws IOException {
        // Remove temporary files
        if(temporaryStorage != null) {
            PathUtils.deleteDirectoryRecursively(temporaryStorage, progressInfo.resolve("Cleaning temporary files"));
        }
    }
}
