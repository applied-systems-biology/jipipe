package org.w3id.cwl.cwl1_2.utils;

import java.net.URISyntaxException;
import org.junit.Assert;
import org.junit.Test;
import org.w3id.cwl.cwl1_2.CWLVersion;
import org.w3id.cwl.cwl1_2.InlineJavascriptRequirement;
import org.w3id.cwl.cwl1_2.Process;

public class RequirementsClassTest {
  Process doc;

  public RequirementsClassTest() throws URISyntaxException {
    super();
    this.doc =
        (Process)
            RootLoader.loadDocument(
                java.nio.file.Paths.get(
                    getClass().getResource("valid_writable-dir-docker.cwl").toURI()));
  }

  @Test
  public void className() {
    Assert.assertEquals("CommandLineToolImpl", doc.getClass().getSimpleName());
  }

  @Test
  public void version() {
    java.util.Optional<CWLVersion> version = doc.getCwlVersion();
    Assert.assertTrue(version.isPresent());
    Assert.assertEquals(CWLVersion.V1_2, version.get());
  }

  @Test
  public void hints() {
    java.util.Optional<java.util.List<Object>> hints = doc.getHints();
    Assert.assertTrue(hints.isPresent());
    java.util.List<Object> hintList = hints.get();
    Assert.assertEquals(1, hintList.size());
  }

  @Test
  public void reqs() {
    java.util.Optional<java.util.List<Object>> reqs = doc.getRequirements();
    Assert.assertTrue(reqs.isPresent());
    java.util.List<Object> reqList = reqs.get();
    Assert.assertEquals(2, reqList.size());
    InlineJavascriptRequirement reqOne = (InlineJavascriptRequirement) reqList.get(0);
    Assert.assertEquals("InlineJavascriptRequirementImpl", reqOne.getClass().getSimpleName());
    Assert.assertNotEquals(
        "InlineJavascriptRequirementImpl", reqList.get(1).getClass().getSimpleName());
  }
}
