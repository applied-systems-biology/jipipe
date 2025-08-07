package edu.kit.datamanager.ro_crate.crate;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.preview.StaticPreview;
import edu.kit.datamanager.ro_crate.reader.CrateReader;
import edu.kit.datamanager.ro_crate.reader.Readers;

import edu.kit.datamanager.ro_crate.writer.Writers;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;


class ReadAndWriteTest {

  @Test
  void testReadingAndWriting(@TempDir Path path) throws IOException {
    Path htmlFile = path.resolve("htmlFile.html");
    FileUtils.writeStringToFile(htmlFile.toFile(), "useful file", Charset.defaultCharset());
    Path htmlDir = path.resolve("dir");
    Path fileInDir = htmlDir.resolve("file.html");
    FileUtils.writeStringToFile(fileInDir.toFile(), "fileN2", Charset.defaultCharset());

    RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
        .setPreview(new StaticPreview(htmlFile.toFile(), htmlDir.toFile()))
        .build();

    Path writeDir = path.resolve("crate");

    Writers.newFolderWriter()
            .save(crate, writeDir.toAbsolutePath().toString());

    CrateReader<String> reader = Readers.newFolderReader();
    Crate newCrate = reader.readCrate(writeDir.toAbsolutePath().toString());

    // the preview files as well as the metadata file should not be included here
    assertEquals(0, newCrate.getUntrackedFiles().size());

    HelpFunctions.compareTwoCrateJson(newCrate, crate);
  }

  @SuppressWarnings("DataFlowIssue")
  @Test
  void testReadCrateWithHasPartHierarchy() throws IOException {
    CrateReader<String> reader = Readers.newFolderReader();
    RoCrate crate = reader.readCrate(ReadAndWriteTest.class.getResource("/crates/hasPartHierarchy").getPath());
    assertEquals(1, crate.getAllContextualEntities().size());
    assertEquals(6, crate.getAllDataEntities().size());
  }
}
