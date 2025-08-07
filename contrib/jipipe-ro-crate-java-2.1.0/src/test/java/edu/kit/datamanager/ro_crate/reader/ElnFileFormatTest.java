package edu.kit.datamanager.ro_crate.reader;

import edu.kit.datamanager.ro_crate.Crate;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public interface ElnFileFormatTest<
        SOURCE_T,
        READER_STRATEGY extends GenericReaderStrategy<SOURCE_T>
        >
        extends TestableReaderStrategy<SOURCE_T, READER_STRATEGY>
{
    /**
     * Some readers may not be able to read a subset of eln files,
     * e.g. because a zip file may not be readable in streaming mode.
     * <p>
     * An implementation test may use this methode to provide a subset of the
     * test cases where an IOException is expected.
     *
     * @param input the input to test for presence in the blacklist
     * @return true if the input is in the blacklist, false otherwise
     */
    default boolean isInBlacklist(String input) {
        return false;
    }

    /**
     * ELN Crates are zip files not fully compatible with the Ro-Crate standard
     * in the sense that they must contain a single subfolder in the zip file
     * which then contain a crate as specified by the Ro-Crate standard.
     * <p>
     * Here we test if we can read them using out ZipReader.
     *
     * @see <a href="https://github.com/TheELNConsortium/TheELNFileFormat"></a>
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/TheELNConsortium/TheELNFileFormat/raw/refs/heads/master/examples/AI4Green/Export%20workbook-2024-08-27-export.eln",
            "https://github.com/TheELNConsortium/TheELNFileFormat/raw/refs/heads/master/examples/OpenSemanticLab/MinimalExample.osl.eln",
            "https://github.com/TheELNConsortium/TheELNFileFormat/raw/refs/heads/master/examples/PASTA/PASTA.eln",
            "https://github.com/TheELNConsortium/TheELNFileFormat/raw/refs/heads/master/examples/RSpace/RSpace-2023-12-08-14-44-xml-SELECTION-c0bEtpHcnNe-HA.eln",
            "https://github.com/TheELNConsortium/TheELNFileFormat/raw/refs/heads/master/examples/SampleDB/sampledb_export.eln",
            "https://github.com/TheELNConsortium/TheELNFileFormat/raw/refs/heads/master/examples/elabftw/export.eln",
            "https://github.com/TheELNConsortium/TheELNFileFormat/raw/refs/heads/master/examples/kadi4mat/records-example.eln",
            "https://github.com/TheELNConsortium/TheELNFileFormat/raw/refs/heads/master/examples/kadi4mat/collections-example.eln"
    })
    default void testReadElnCrates(String urlStr, @TempDir Path tmp) throws IOException {
        // Download the ELN file
        URL url = URI.create(urlStr).toURL();
        Path elnFile = tmp.resolve("downloaded.eln");
        FileUtils.copyURLToFile(url, elnFile.toFile(), 20000, 20000);
        assertTrue(elnFile.toFile().exists());

        if (!isInBlacklist(urlStr)) {
            // Read the crate from the downloaded file
            Crate read = this.readCrate(elnFile);
            assertNotNull(read);
            assertFalse(read.getAllDataEntities().isEmpty());
        } else {
            // If the file is in the blacklist, we expect an IOException
            assertThrows(IOException.class, () -> this.readCrate(elnFile));
        }
    }
}
