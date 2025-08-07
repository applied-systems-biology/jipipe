package edu.kit.datamanager.ro_crate.writer;

import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.DataSetEntity;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

interface CommonWriterTest extends TestableWriterStrategy {

    /**
     * Test where the writer needs to rename files or folders in order to make a valid crate.
     * The content will therefore not be equal to its source!
     *
     * @param tempDir the temporary directory given by junit for our test
     * @throws IOException if an error occurs while writing the crate
     */
    @Test
    default void testFilesBeingAdjusted(@TempDir Path tempDir) throws IOException {
        Path correctCrate = tempDir.resolve("compare_with_me");
        Path pathToFile = correctCrate.resolve("you-will-need-to-rename-this-file.ai");
        Path pathToDir = correctCrate.resolve("you-will-need-to-rename-this-dir");
        createManualCrateStructure(correctCrate, pathToFile, pathToDir);

        Path writtenCrate = tempDir.resolve("written-crate");
        Path extractionPath = tempDir.resolve("checkMe");
        {
            RoCrate builtCrate = getCrateWithFileAndDir(pathToFile, pathToDir)
                    .addDataEntity(new DataSetEntity.DataSetBuilder()
                            .addProperty("name", "Subdir")
                            .addProperty("description", "Some subdir")
                            .setLocationWithExceptions(pathToDir.resolve("subdir"))
                            .setId("lots_of_little_files/subdir-renamed/")
                            .build()
                    )
                    .build();
            this.saveCrate(builtCrate, writtenCrate);
            ensureCrateIsExtractedIn(writtenCrate, extractionPath);
        }

        HelpFunctions.printFileTree(correctCrate);
        HelpFunctions.printFileTree(extractionPath);

        // The actual file name should **not** appear in the crate
        String fileName = pathToFile.getFileName().toString();
        assertFalse(
                Files.isRegularFile(extractionPath.resolve(fileName)),
                "The directory should not be present, because '%s' is a file in the crate".formatted(fileName)
        );
        // Instead, the file should be present with the name of the ID
        assertTrue(
                Files.isRegularFile(extractionPath.resolve("cp7glop.ai")),
                "The file 'cp7glop.ai' should be present and have the name adjusted to the ID"
        );
        // The actual directory name should **not** appear in the crate
        String dirName = pathToDir.getFileName().toString();
        assertFalse(
                Files.isDirectory(extractionPath.resolve(dirName)),
                "The directory should not be present, because '%s' is a file in the crate".formatted(dirName)
        );
        // Instead, the directory should be present with the name of the ID
        assertTrue(
                Files.isDirectory(extractionPath.resolve("lots_of_little_files/")),
                "The directory 'lots_of_little_files' should be present"
        );
        assertTrue(
                Files.isDirectory(extractionPath.resolve("lots_of_little_files/").resolve("subdir")),
                "The directory 'lots_of_little_files/subdir' should be present"
        );

        /*
         * As we added another DataSetEntity with location, the subdir should have a renamed copy as well
         * Note: If this behavior is to be changed later on, we possibly need to change the documentation
         *  of {@link AbstractDataEntityBuilder#setLocation(Path)}.
         */
        assertTrue(
                Files.isDirectory(extractionPath.resolve("lots_of_little_files/").resolve("subdir-renamed")),
                "The directory 'lots_of_little_files/subdir' should be present"
        );
    }

    /**
     * Test where the writer should make an exact copy of our defined folder.
     *
     * @param tempDir the temporary directory given by junit for our test
     * @throws IOException if an error occurs while writing the crate
     */
    @Test
    default void testWritingMakesCopy(@TempDir Path tempDir) throws IOException {
        // We need a correct directory to compare with.
        // It is built manually to ensure we meet our expectations.
        // Reader-writer-consistency is tested at {@link CrateReaderTest}
        Path correctCrate = tempDir.resolve("compare_with_me");
        Path pathToFile = correctCrate.resolve("cp7glop.ai");
        Path pathToDir = correctCrate.resolve("lots_of_little_files");

        createManualCrateStructure(correctCrate, pathToFile, pathToDir);

        // Now use the builder to build the same crate independently.
        // The files will be reused (we need a place to take a copy from)
        RoCrate builtCrate = getCrateWithFileAndDir(pathToFile, pathToDir).build();

        Path pathToZip = tempDir.resolve("written-needs_testing.zip");
        this.saveCrate(builtCrate, pathToZip);

        // extract the zip file to a temporary directory
        Path extractionPath = tempDir.resolve("extracted_for_testing");
        ensureCrateIsExtractedIn(pathToZip, extractionPath);
        HelpFunctions.printFileTree(correctCrate);
        HelpFunctions.printFileTree(extractionPath);

        // compare the extracted directory with the correct one
        assertTrue(HelpFunctions.compareTwoDir(
                correctCrate.toFile(),
                extractionPath.toFile()));
        HelpFunctions.compareCrateJsonToFileInResources(
                builtCrate,
                "/json/crate/fileAndDir.json");
    }

    /**
     * Test where the writer should only consider the files that are added to the metadata json.
     * The crate should not contain any files that are not part of it.
     *
     * @param tempDir the temporary directory given by junit for our test
     * @throws IOException if an error occurs while writing the crate
     */
    @Test
    default void testWritingOnlyConsidersAddedFiles(@TempDir Path tempDir) throws IOException {
        Path correctCrate = tempDir.resolve("compare_with_me");
        Path pathToFile = correctCrate.resolve("cp7glop.ai");
        Path pathToDir = correctCrate.resolve("lots_of_little_files");

        createManualCrateStructure(correctCrate, pathToFile, pathToDir);
        {
            // This file is not part of the crate, and should therefore not be present
            Path falseFile = correctCrate.resolve("new");
            FileUtils.writeStringToFile(
                    falseFile.toFile(),
                    "this file contains something else",
                    Charset.defaultCharset());
        }

        // create the RO_Crate including the files that should be present in it
        RoCrate roCrate = getCrateWithFileAndDir(pathToFile, pathToDir).build();

        Path pathToZip = tempDir.resolve("writing-needs_testing.zip");
        this.saveCrate(roCrate, pathToZip);


        // extract and compare
        Path extractionPath = tempDir.resolve("extracted_for_testing");
        ensureCrateIsExtractedIn(pathToZip, extractionPath);
        HelpFunctions.printFileTree(correctCrate);
        HelpFunctions.printFileTree(extractionPath);

        assertFalse(HelpFunctions.compareTwoDir(
                correctCrate.toFile(),
                extractionPath.toFile()),
                "The crate should not contain the file that was not part of the metadata");
        HelpFunctions.compareCrateJsonToFileInResources(
                roCrate,
                "/json/crate/fileAndDir.json");
    }
}
