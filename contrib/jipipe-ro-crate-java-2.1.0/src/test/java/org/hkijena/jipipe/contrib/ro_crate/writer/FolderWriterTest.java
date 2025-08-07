package org.hkijena.jipipe.contrib.ro_crate.writer;

import java.io.IOException;
import java.nio.file.Path;

import org.hkijena.jipipe.contrib.ro_crate.Crate;

import org.apache.commons.io.FileUtils;

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
