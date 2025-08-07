package edu.kit.datamanager.ro_crate.writer;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.RoCrate;

import edu.kit.datamanager.ro_crate.reader.CommonReaderTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public interface ElnFileWriterTest extends TestableWriterStrategy {

    /**
     * Write in ELN format style, meaning with a subfolder in the zip file.
     * Must use {@link ElnFormatWriter#usingElnStyle()}.
     *
     * @param crate the crate to write
     * @param target the target path to the save location
     * @throws IOException if an error occurs
     */
    void saveCrateElnStyle(Crate crate, Path target) throws IOException;

    /**
     * Same as {@link #saveCrateElnStyle(Crate, Path)} but with the alias
     * {@link  ElnFormatWriter#withRootSubdirectory()}.
     * @param crate the crate to write
     * @param target the target path to the save location
     */
    void saveCrateSubdirectoryStyle(RoCrate crate, Path target) throws IOException;

    @Test
    default void testMakesElnStyleCrate(@TempDir Path tempDir) throws IOException {
        // We need a correct directory to compare with.
        // It is built manually to ensure we meet our expectations.
        // Reader-writer-consistency is tested at {@link CrateReaderTest}

        // We compare the ELN style like this:
        // tempDir
        //   └── compare_with_me
        //       └── crate-subfolder
        //           ├── ...
        //   └── extracted_for_testing
        //       └── crate-subfolder
        //           ├── ...
        String crateName = "crate-subfolder";
        Path correctCrate = tempDir
                .resolve("compare_with_me")
                .resolve(crateName);
        Path pathToFile = correctCrate.resolve("cp7glop.ai");
        Path pathToDir = correctCrate.resolve("lots_of_little_files");

        createManualCrateStructure(correctCrate, pathToFile, pathToDir);

        // Now use the builder to build the same crate independently.
        // The files will be reused (we need a place to take a copy from)
        RoCrate builtCrate = getCrateWithFileAndDir(pathToFile, pathToDir).build();

        Path pathToZip = tempDir.resolve("%s.eln".formatted(crateName));
        this.saveCrateElnStyle(builtCrate, pathToZip);

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

    @Test
    default void testAlias(@TempDir Path tmpDir) throws IOException {
        Path zip = tmpDir.resolve("test.eln").toAbsolutePath();
        RoCrate crate = CommonReaderTest.newBaseCrate().build();

        this.saveCrateSubdirectoryStyle(crate, zip);

        assertTrue(zip.toFile().exists(), "The zip file should exist");
        Path extractedPath = tmpDir.resolve("extracted");
        ensureCrateIsExtractedIn(zip, extractedPath);
    }
}
