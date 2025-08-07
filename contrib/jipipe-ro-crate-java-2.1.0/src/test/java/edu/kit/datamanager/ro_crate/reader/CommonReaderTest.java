package edu.kit.datamanager.ro_crate.reader;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity;
import edu.kit.datamanager.ro_crate.writer.CrateWriter;
import edu.kit.datamanager.ro_crate.writer.Writers;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract class for testing crate readers.
 *
 * @param <SOURCE_T> the source type of the reader strategy. Even though each implementation knows this T,
 *           we can't use it everywhere we'd like to as the code here needs to be generic.
 *           We therefore rely on methods to take a path (as we always assume local testing).
 *           Streams, for example, will therefore need to stream from/to a file.
 *           This parameter is only required to satisfy the generic reader strategy.
 * @param <READER_STRATEGY> the type of the reader strategy
 */
public interface CommonReaderTest<
        SOURCE_T,
        READER_STRATEGY extends GenericReaderStrategy<SOURCE_T>
        >
        extends TestableReaderStrategy<SOURCE_T, READER_STRATEGY>
{
    static RoCrate.RoCrateBuilder newBaseCrate() {
        return new RoCrate.RoCrateBuilder(
                "minimal",
                "minimal RO_crate",
                "2024",
                "https://creativecommons.org/licenses/by-nc-sa/3.0/au/"
        );
    }

    static FileEntity newDataEntity(Path filePath) throws IllegalArgumentException {
        return new FileEntity.FileEntityBuilder()
                .setLocationWithExceptions(filePath)
                .setId(filePath.toFile().getName())
                .addProperty("name", "Survey responses")
                .addProperty("contentSize", "26452")
                .addProperty("encodingFormat", "text/csv")
                .build();
    }

    @Test
    default void testReadingBasicCrate(@TempDir Path temp) throws IOException {

        RoCrate roCrate = newBaseCrate().build();
        Path zipPath = temp.resolve("result.zip");
        this.saveCrate(roCrate, zipPath);
        Crate importedCrate = this.readCrate(zipPath);
        HelpFunctions.compareTwoCrateJson(roCrate, importedCrate);
    }

    @Test
    default void testWithFile(@TempDir Path temp) throws IOException {
        Path csvPath = temp.resolve("survey-responses-2019.csv");
        FileUtils.touch(csvPath.toFile());
        FileUtils.writeStringToFile(csvPath.toFile(), "Dummy content", Charset.defaultCharset());
        RoCrate rawCrate = newBaseCrate()
                .addDataEntity(newDataEntity(csvPath))
                .build();

        assertEquals(1, rawCrate.getAllDataEntities().size());

        Path zipPath = temp.resolve("result.zip");
        this.saveCrate(rawCrate, zipPath);
        Crate importedCrate = this.readCrate(zipPath);

        HelpFunctions.compareTwoCrateJson(rawCrate, importedCrate);
    }

    @Test
    default void testWithFileUrlEncoded(@TempDir Path temp) throws IOException {
        // This URL will be encoded because of whitespaces
        Path csvPath = temp.resolve("survey responses 2019.csv");
        FileUtils.touch(csvPath.toFile());
        FileUtils.writeStringToFile(csvPath.toFile(), "Dummy content", Charset.defaultCharset());
        RoCrate rawCrate = newBaseCrate()
                .addDataEntity(newDataEntity(csvPath))
                .build();

        DataEntity rawEntity = rawCrate.getAllDataEntities().iterator().next();
        assertTrue(rawEntity.getId().contains("survey"));
        assertFalse(rawEntity.getId().contains(" "));
        assertEquals(1, rawCrate.getAllDataEntities().size());

        Path zipPath = temp.resolve("result.zip");
        this.saveCrate(rawCrate, zipPath);
        Crate importedCrate = this.readCrate(zipPath);

        DataEntity importedEntity = importedCrate.getAllDataEntities().iterator().next();
        assertTrue(importedEntity.getId().contains("survey"));
        assertFalse(importedEntity.getId().contains(" "));
        assertEquals(1, importedCrate.getAllDataEntities().size());

        HelpFunctions.compareTwoCrateJson(rawCrate, importedCrate);
    }

    @Test
    default void TestWithFileWithLocation(@TempDir Path temp) throws IOException {
        Path csvPath = temp.resolve("survey-responses-2019.csv");
        FileUtils.writeStringToFile(csvPath.toFile(), "Dummy content", Charset.defaultCharset());
        RoCrate rawCrate = newBaseCrate()
                .addDataEntity(newDataEntity(csvPath))
                .setPreview(null)//disable preview to allow to compare folders before and after
                .build();

        // write to zip file and read via zip stream
        Path zipPath = temp.resolve("result.zip");
        this.saveCrate(rawCrate, zipPath);
        Crate importedCrate = this.readCrate(zipPath);

        // write raw crate and imported crate to folders and compare the results
        Path rawCrateTarget = temp.resolve("rawCrateSaved");
        Path importedCrateTarget = temp.resolve("importedCrateSaved");
        {
            // write raw crate and imported crate to two different directories
            CrateWriter<String> writer = Writers.newFolderWriter();
            writer
                    .withAutomaticProvenance(null)
                    .save(rawCrate, rawCrateTarget.toString());
            writer
                    .withAutomaticProvenance(null)
                    .save(importedCrate, importedCrateTarget.toString());
        }

        assertTrue(HelpFunctions.compareTwoDir(rawCrateTarget.toFile(), importedCrateTarget.toFile()));
        HelpFunctions.compareTwoCrateJson(rawCrate, importedCrate);
    }

    @Test
    default void TestWithFileWithLocationAddEntity(@TempDir Path temp) throws IOException {
        Path csvPath = temp.resolve("file.csv");
        FileUtils.writeStringToFile(csvPath.toFile(), "fakecsv.1", Charset.defaultCharset());
        RoCrate rawCrate = newBaseCrate()
                .addDataEntity(newDataEntity(csvPath))
                .build();

        // write to zip file and import via zip stream
        Path zipPath = temp.resolve("result.zip");
        this.saveCrate(rawCrate, zipPath);
        Crate importedCrate = this.readCrate(zipPath);
        {
            // modify the imported crate
            Path newFile = temp.resolve("new_file");
            FileUtils.writeStringToFile(newFile.toFile(), "Some file content", Charset.defaultCharset());
            importedCrate.addDataEntity(new FileEntity.FileEntityBuilder()
                    .setEncodingFormat("setnew")
                    .setLocationWithExceptions(newFile)
                    .setId("new_file")
                    .build());
        }
        // write raw crate to a folder
        Path rawCrateTarget = temp.resolve("rawCrateSaved");
        Path importedCrateTarget = temp.resolve("importedCrateSaved");
        {
            // write raw crate and imported crate to two different directories
            CrateWriter<String> writer = Writers.newFolderWriter();
            writer.save(rawCrate, rawCrateTarget.toString());
            writer.save(importedCrate, importedCrateTarget.toFile().toString());
        }
        // assert the folders are different
        assertFalse(HelpFunctions.compareTwoDir(rawCrateTarget.toFile(), importedCrateTarget.toFile()));
        HelpFunctions.compareTwoMetadataJsonNotEqual(rawCrate, importedCrate);
        // assert the importedCrateTarget folder contains newFile
        assertTrue(importedCrateTarget.resolve("new_file").toFile().isFile());
    }

    @Test
    default void testReadingBasicCrateWithCustomPath(@TempDir Path temp) throws IOException {
        RoCrate rawCrate = newBaseCrate().build();

        // Write to zip file
        Path zipPath = temp.resolve("result.zip");
        this.saveCrate(rawCrate, zipPath);

        // read again and compare using custom path for temporary extraction folder
        // (if available, otherwise uses default)
        Path differentFolder = temp.resolve("differentFolder");
        READER_STRATEGY strategy = this.newReaderStrategyWithTmp(differentFolder, true);
        Crate importedCrate = this.readCrate(strategy, zipPath);
        HelpFunctions.compareTwoCrateJson(rawCrate, importedCrate);

        {
            // try it again without the UUID subfolder and test if the directory is being cleaned up (for coverage).
            READER_STRATEGY strategyWithoutSubfolder = this.newReaderStrategyWithTmp(differentFolder, false);
            Crate crate = this.readCrate(strategyWithoutSubfolder, zipPath);
            HelpFunctions.compareTwoCrateJson(rawCrate, crate);
        }
    }
}
