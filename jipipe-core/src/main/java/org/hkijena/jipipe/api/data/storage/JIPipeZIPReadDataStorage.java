package org.hkijena.jipipe.api.data.storage;

import org.apache.commons.io.FilenameUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A storage the reads a ZIP file
 */
public class JIPipeZIPReadDataStorage implements JIPipeReadDataStorage {

    private final Path zipFilePath;

    private final JIPipeProgressInfo progressInfo;

    private final Path internalPath;

    private Path temporaryStorage;

    private JIPipeZIPReadDataStorage parent;

    private ZipFile zipFile;

    public JIPipeZIPReadDataStorage(JIPipeProgressInfo progressInfo, Path zipFilePath) {
        this.zipFilePath = zipFilePath;
        this.progressInfo = progressInfo;
        this.internalPath = Paths.get("");
        initialize();
    }

    private JIPipeZIPReadDataStorage(JIPipeProgressInfo progressInfo, Path zipFilePath, Path internalPath) {
        this.zipFilePath = zipFilePath;
        this.progressInfo = progressInfo;
        this.internalPath = internalPath;
    }

    private void initialize() {
        try {
            zipFile = new ZipFile(zipFilePath.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            temporaryStorage = RuntimeSettings.generateTempDirectory("zip");
            progressInfo.log("Temporary storage requested: " + temporaryStorage);
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
        result.zipFile = zipFile;
        result.parent = this;
        return result;
    }

    @Override
    public boolean isFile(Path path) {
        ZipEntry entry = getZipEntry(path);
        return entry != null && !entry.isDirectory();
    }

    private ZipEntry getZipEntry(Path path) {
        Path fullPath = internalPath.resolve(path);
        String name = FilenameUtils.separatorsToUnix(fullPath.toString());
        return zipFile.getEntry(name);
    }

    @Override
    public boolean exists(Path path) {
        ZipEntry entry = getZipEntry(path);
        return entry != null;
    }

    @Override
    public Collection<Path> list() {
        List<Path> result = new ArrayList<>();
        String name = FilenameUtils.separatorsToUnix(internalPath.toString()) + "/";
        if (name.equals("/"))
            name = "";
        String finalName = name;
        zipFile.stream().forEach(entry -> {
            if (entry.getName().startsWith(finalName)) {
                result.add(Paths.get(entry.getName()));
            }
        });
        return result;
    }

    @Override
    public InputStream open(Path path) {
        ZipEntry entry = getZipEntry(path);
        try {
            return zipFile.getInputStream(entry);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {

        if (parent != null) {
            parent.close();
            return;
        }

        // Close the ZIP file
        zipFile.close();

        // Remove temporary files
        if (temporaryStorage != null) {
            PathUtils.deleteDirectoryRecursively(temporaryStorage, progressInfo.resolve("Cleaning temporary files"));
        }
    }
}
