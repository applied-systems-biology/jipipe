package org.hkijena.jipipe.contrib.ro_crate.writer;

import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.contrib.ro_crate.Crate;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Nikola Tzotchev on 9.2.2022 г.
 * @version 1
 */
class FolderWriterTest implements CommonWriterTest {

    @Override
    public void saveCrate(Crate crate, Path target) throws IOException {
        Writers.newFolderWriter()
                .withAutomaticProvenance(null)
                .save(crate, target.toAbsolutePath().toString());
    }

    @Override
    public void ensureCrateIsExtractedIn(Path pathToCrate, Path expectedPath) throws IOException {
        FileUtils.copyDirectory(pathToCrate.toFile(), expectedPath.toFile());
    }
}
