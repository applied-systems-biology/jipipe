package edu.kit.datamanager.ro_crate.writer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.kit.datamanager.ro_crate.reader.Readers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.HelpFunctions;

class RoCrateWriterSpec12Test {

    @Test
    void writeDoesNotModifyTest(@TempDir Path tempDir) throws IOException, URISyntaxException {
        // read crate (not the actual part of this test)
        String internalOriginalCratePath = "crates/spec-1.2-DRAFT/minimal-with-conformsTo-Array";
        URL internalOriginalCrateURL = this.getClass().getResource("/" + internalOriginalCratePath);
        assertNotNull(internalOriginalCrateURL);

        Crate crate = Readers.newFolderReader().readCrate(internalOriginalCrateURL.getPath());
        Path targetDir = tempDir.resolve("spec12writeUnmodified");

        Writers.newFolderWriter()
                .withAutomaticProvenance(null)
                .save(crate, targetDir.toAbsolutePath().toString());

        // compare directories
        Path srcDir = Paths.get(internalOriginalCrateURL.toURI());
        assertTrue(HelpFunctions.compareTwoDir(targetDir.toFile(), srcDir.toFile()));

        // compare original metadata file with crate class
        HelpFunctions.compareCrateJsonToFileInResources(
                crate,
                "/" + internalOriginalCratePath + "/ro-crate-metadata.json");
        // compare exported metadata file with original metadata file
        HelpFunctions.compareCrateJsonToFileInResources(
                // created metadata file
                targetDir.resolve("ro-crate-metadata.json").toFile(),
                // original metadata file
                new File(srcDir.resolve("ro-crate-metadata.json").toString()));
        // Compare loaded crate object with crate object made of export
        Crate crate2 = Readers.newFolderReader().readCrate(targetDir.toString());
        HelpFunctions.compareTwoCrateJson(crate, crate2);
    }
}
