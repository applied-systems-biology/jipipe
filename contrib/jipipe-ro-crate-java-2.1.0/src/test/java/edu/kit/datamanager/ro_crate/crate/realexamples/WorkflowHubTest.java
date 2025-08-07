package edu.kit.datamanager.ro_crate.crate.realexamples;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.reader.CrateReader;
import edu.kit.datamanager.ro_crate.reader.Readers;

import edu.kit.datamanager.ro_crate.writer.Writers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class WorkflowHubTest {

  @SuppressWarnings("DataFlowIssue")
  @Test
  void testImportZip(@TempDir Path temp) throws IOException {
    CrateReader<String> reader = Readers.newZipPathReader();
    Crate crate = reader.readCrate(WorkflowHubTest.class.getResource("/crates/workflowhub/workflow-109-5.crate.zip").getPath());

    HelpFunctions.compareCrateJsonToFileInResources(crate, "/crates/workflowhub/workflow1/ro-crate-metadata.json");
    Writers.newFolderWriter().save(crate, temp.toString());
    HelpFunctions.compareTwoDir(temp.toFile(), new File(WorkflowHubTest.class.getResource("/crates/workflowhub/workflow1/").getPath()));
  }
}
