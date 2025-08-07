package edu.kit.datamanager.ro_crate.reader;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.writer.Writers;

import java.io.*;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


class ZipStreamReaderTest implements
        CommonReaderTest<InputStream, ReadZipStreamStrategy>,
        ElnFileFormatTest<InputStream, ReadZipStreamStrategy>
{
    /**
     * At the point of writing this test,
     *  these files are in a zip format which cannot be read in streaming mode
     */
    @Override
    public boolean isInBlacklist(String input) {
        return Set.of(
                "https://github.com/TheELNConsortium/TheELNFileFormat/raw/refs/heads/master/examples/kadi4mat/records-example.eln",
                "https://github.com/TheELNConsortium/TheELNFileFormat/raw/refs/heads/master/examples/kadi4mat/collections-example.eln"
        )
                .contains(input);
    }

    @Override
    public void saveCrate(Crate crate, Path target) throws IOException {
        final File target_file = target.toFile();
        try (
                FileOutputStream fos = new FileOutputStream(target_file)
        ) {
            Writers.newZipStreamWriter()
                    .withAutomaticProvenance(null)
                    .save(crate, fos);
        }
        assertTrue(target_file.isFile());
    }

    @Override
    public Crate readCrate(Path source) throws IOException {
        try (
                FileInputStream fis = new FileInputStream(source.toFile())
        ) {
            return Readers.newZipStreamReader().readCrate(fis);
        }
    }

    @Override
    public ReadZipStreamStrategy newReaderStrategyWithTmp(Path tmpDirectory, boolean useUuidSubfolder) {
        ReadZipStreamStrategy strategy = new ReadZipStreamStrategy(tmpDirectory, useUuidSubfolder);
        assertFalse(strategy.isExtracted());
        if (useUuidSubfolder) {
            assertEquals(strategy.getTemporaryFolder().getFileName().toString(), strategy.getID());
        } else {
            assertEquals(strategy.getTemporaryFolder().getFileName().toString(), tmpDirectory.getFileName().toString());
        }
        assertTrue(strategy.getTemporaryFolder().startsWith(tmpDirectory));
        return strategy;
    }

    @Override
    public Crate readCrate(ReadZipStreamStrategy strategy, Path source) throws IOException {
        try (
                FileInputStream fis = new FileInputStream(source.toFile())
        ) {
            Crate importedCrate = new CrateReader<>(strategy).readCrate(fis);
            assertNotNull(importedCrate);
            assertTrue(strategy.isExtracted());
            return importedCrate;
        }
    }
}
