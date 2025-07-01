package org.w3id.cwl.cwl1_1.utils;

import java.net.URISyntaxException;
import org.junit.Assert;
import org.junit.Test;
import org.w3id.cwl.cwl1_1.CWLVersion;
import org.w3id.cwl.cwl1_1.Process;

public class WorkflowClassTest {
  Process doc;

  public WorkflowClassTest() throws URISyntaxException {
    super();
    this.doc =
        (Process)
            RootLoader.loadDocument(
                java.nio.file.Paths.get(
                    getClass().getResource("valid_count-lines1-wf.cwl").toURI()));
  }

  @Test
  public void className() {
    Assert.assertEquals("WorkflowImpl", doc.getClass().getSimpleName());
  }

  @Test
  public void version() {
    java.util.Optional<CWLVersion> version = doc.getCwlVersion();
    Assert.assertTrue(version.isPresent());
    Assert.assertEquals(CWLVersion.V1_1, version.get());
  }
}
