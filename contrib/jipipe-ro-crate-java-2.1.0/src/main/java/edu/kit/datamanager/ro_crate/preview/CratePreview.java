package edu.kit.datamanager.ro_crate.preview;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.writer.CrateWriter;
import edu.kit.datamanager.ro_crate.writer.WriteFolderStrategy;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

/**
 * Interface for the ROCrate preview. This manages the human-readable
 * representation of a crate.
 *
 * @author Nikola Tzotchev on 6.2.2022 г.
 * @author jejkal
 * @version 2
 */
public interface CratePreview {

    /**
     * Generate a preview of the crate and store it into the given target directory.
     * It is the caller's responsibility to handle, e.g. delete after use, the result
     * (The caller takes ownership of the result).
     * <p>
     * <b>IMPORTANT NOTE:</b> This method currently has a default implementation that relies
     * on deprecated methods. In future, you will have to implement this method directly.
     *
     * @param crate the crate to generate a preview for.
     * @param targetDir the target directory to store the preview in,
     *                 owned by the caller.
     * @throws IOException if an error occurs while generating the preview.
     */
    default void generate(Crate crate, File targetDir) throws IOException {
        // disable preview generation to avoid recursion,
        // as this is usually called in the process of writing a crate
        // (including preview)
        new CrateWriter<>(new WriteFolderStrategy().disablePreview())
                // We assume the caller (e.g. a writer) already stored the provenance.
                .withAutomaticProvenance(null)
                .save(crate, targetDir.getAbsolutePath());
        this.saveAllToFolder(targetDir);
        try (var stream = Files.list(targetDir.toPath())) {
            stream
                    .filter(path -> !path.getFileName().toString().equals("ro-crate-preview.html"))
                    .filter(path -> !path.getFileName().toString().equals("ro-crate-preview_files"))
                    .forEach(path -> {
                        try {
                            if (Files.isDirectory(path)) {
                                FileUtils.deleteDirectory(path.toFile());
                            } else {
                                Files.delete(path);
                            }
                        } catch (IOException e) {
                            // Silently ignore deletion errors
                            LoggerFactory.getLogger(CratePreview.class)
                                    .error("Failed to delete temporary file {}", path, e);
                        }
                    });
        }
    }

    /**
     * Takes a crate in form of a zip file and generates a preview of it,
     * which will be stored within the crate.
     *
     * @param zipFile the zip file with the crate, which should receive a preview.
     * @throws IOException if an error occurs while saving the preview
     *
     * @deprecated Use {@link #generate(Crate, File)} instead.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    void saveAllToZip(ZipFile zipFile) throws IOException;

    /**
     * Saves the preview, given by the folder, into the given folder.
     *
     * @param folder the folder (containing a crate) to save the preview in.
     * @throws IOException if an error occurs while saving the preview.
     *
     * @deprecated Use {@link #generate(Crate, File)} instead.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    void saveAllToFolder(File folder) throws IOException;

    /**
     * Saves the preview, given by the metadata, into the given stream.
     *
     * @param metadata the metadata of the crate to save the preview in.
     * @param stream the stream to save the preview in.
     * @throws IOException if an error occurs while saving the preview.
     *
     * @deprecated Use {@link #generate(Crate, File)} instead.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    void saveAllToStream(String metadata, ZipOutputStream stream) throws IOException;

}
