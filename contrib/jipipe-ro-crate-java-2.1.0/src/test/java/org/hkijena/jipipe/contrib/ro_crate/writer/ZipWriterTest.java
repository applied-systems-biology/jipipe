package org.hkijena.jipipe.contrib.ro_crate.writer;

import org.hkijena.jipipe.contrib.ro_crate.Crate;
import org.hkijena.jipipe.contrib.ro_crate.RoCrate;

import java.io.IOException;
import java.nio.file.Path;

class ZipWriterTest implements
        CommonWriterTest,
        ElnFileWriterTest {
    @Override
    public void saveCrate(Crate crate, Path target) throws IOException {
        Writers.newZipPathWriter()
                .withAutomaticProvenance(null)
                .save(crate, target.toAbsolutePath().toString());
    }

    @Override
    public void saveCrateElnStyle(Crate crate, Path target) throws IOException {
        new CrateWriter<>(new WriteZipStrategy().usingElnStyle())
                .withAutomaticProvenance(null)
                .save(crate, target.toAbsolutePath().toString());
    }

    @Override
    public void saveCrateSubdirectoryStyle(RoCrate crate, Path target) throws IOException {
        new CrateWriter<>(new WriteZipStrategy().withRootSubdirectory())
                .withAutomaticProvenance(null)
                .save(crate, target.toString());
    }
}
