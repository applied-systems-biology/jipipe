package edu.kit.datamanager.ro_crate.writer;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.DataSetEntity;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity;
import edu.kit.datamanager.ro_crate.preview.AutomaticPreview;
import edu.kit.datamanager.ro_crate.preview.PreviewGenerator;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * Base Interface for methods required to test all writer strategies.
 */
interface TestableWriterStrategy {
    /**
     * Saves the crate with the writer fitting to this test class.
     *
     * @param crate the crate to save
     * @param target the target path to the save location
     * @throws IOException if an error occurs while saving the crate
     */
    void saveCrate(Crate crate, Path target) throws IOException;

    /**
     * Ensures the crate is in extracted form in the given path.
     *
     * @param pathToCrate       the path to the crate, may not be a folder yet
     * @param expectedPath      the path where the crate should be in extracted form
     * @throws IOException if an error occurs while extracting the crate
     */
    default void ensureCrateIsExtractedIn(Path pathToCrate, Path expectedPath) throws IOException {
        try (ZipFile zf = new ZipFile(pathToCrate.toFile())) {
            zf.extractAll(expectedPath.toFile().getAbsolutePath());
        }
    }

    /**
     * Creates a crate structure manually.
     *
     * @param correctCrate the path to the crate
     * @param pathToFile   the path to the file
     * @param pathToDir    the path to the directory
     * @throws IOException if an error occurs while creating the crate structure
     */
    default void createManualCrateStructure(Path correctCrate, Path pathToFile, Path pathToDir) throws IOException {
        FileUtils.forceMkdir(correctCrate.toFile());
        InputStream fileJson = ZipStreamWriterTest.class
                .getResourceAsStream("/json/crate/fileAndDir.json");
        Assertions.assertNotNull(fileJson);
        // fill the directory with expected files and dirs
        // starting with the .json of our crate
        Path json = correctCrate.resolve("ro-crate-metadata.json");
        FileUtils.copyInputStreamToFile(fileJson, json.toFile());
        // create preview
        PreviewGenerator.generatePreview(correctCrate.toFile().getAbsolutePath());
        // create the files and directories
        FileUtils.writeStringToFile(pathToFile.toFile(), "content of Local File", Charset.defaultCharset());
        // creates the directory and a subdirectory
        Path subdir = pathToDir.resolve("subdir");
        FileUtils.forceMkdir(subdir.toFile());
        FileUtils.writeStringToFile(
                subdir.resolve("subsubfirst.txt").toFile(),
                "content of subsub file in subsubdir",
                Charset.defaultCharset());
        FileUtils.writeStringToFile(
                pathToDir.resolve("first.txt").toFile(),
                "content of first file in dir",
                Charset.defaultCharset());
        FileUtils.writeStringToFile(
                pathToDir.resolve("second.txt").toFile(),
                "content of second file in dir",
                Charset.defaultCharset());
        FileUtils.writeStringToFile(
                pathToDir.resolve("third.txt").toFile(),
                "content of third file in dir",
                Charset.defaultCharset());
    }

    /**
     * Creates a crate resembling the one we manually create in these tests.
     *
     * @param pathToFile      the file to add
     * @param pathToSubdir the directory to add
     * @return the crate builder
     */
    default RoCrate.RoCrateBuilder getCrateWithFileAndDir(Path pathToFile, Path pathToSubdir) {
        return new RoCrate.RoCrateBuilder(
                "Example RO-Crate",
                "The RO-Crate Root Data Entity",
                "2024",
                "https://creativecommons.org/licenses/by-nc-sa/3.0/au/"
        )
                .addDataEntity(
                        new FileEntity.FileEntityBuilder()
                                .addProperty("name", "Diagram showing trend to increase")
                                .addProperty("contentSize", "383766")
                                .addProperty("description", "Illustrator file for Glop Pot")
                                .setEncodingFormat("application/pdf")
                                .setLocationWithExceptions(pathToFile)
                                .setId("cp7glop.ai")
                                .build()
                )
                .addDataEntity(
                        new DataSetEntity.DataSetBuilder()
                                .addProperty("name", "Too many files")
                                .addProperty("description",
                                        "This directory contains many small files, that we're not going to describe in detail.")
                                .setLocationWithExceptions(pathToSubdir)
                                .setId("lots_of_little_files/")
                                .build()
                )
                .setPreview(new AutomaticPreview());
    }
}
