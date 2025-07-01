package org.w3id.cwl.cwl1_1.utils;

import java.util.List;
import org.junit.Assert;
import org.w3id.cwl.cwl1_1.InputRecordSchema;
import org.w3id.cwl.cwl1_1.Process;
import org.w3id.cwl.cwl1_1.SchemaDefRequirement;

public class SchemaDefTest {

  @org.junit.Test
  public void testvalid_anon_enum_inside_array_inside_schemadef() throws Exception {
    java.net.URL url = getClass().getResource("valid_anon_enum_inside_array_inside_schemadef.cwl");
    java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
    Process doc = (Process) RootLoader.loadDocument(resPath);
    java.util.Optional<java.util.List<Object>> reqs = doc.getRequirements();
    Assert.assertTrue(reqs.isPresent());
    java.util.List<Object> reqList = reqs.get();
    Assert.assertEquals(reqList.size(), 1);
    SchemaDefRequirement schemaReq = (SchemaDefRequirement) reqList.get(0);
    List<Object> schemaTypes = schemaReq.getTypes();
    for (Object schemaType : schemaTypes) {
      Assert.assertTrue(schemaType instanceof InputRecordSchema);
    }
  }

  @org.junit.Test
  public void testvalid_record_sd_secondaryFiles() throws Exception {
    java.net.URL url = getClass().getResource("valid_record-sd-secondaryFiles.cwl");
    java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
    Process doc = (Process) RootLoader.loadDocument(resPath);
    java.util.Optional<java.util.List<Object>> reqs = doc.getRequirements();
    Assert.assertTrue(reqs.isPresent());
    java.util.List<Object> reqList = reqs.get();
    Assert.assertEquals(reqList.size(), 1);
    SchemaDefRequirement schemaReq = (SchemaDefRequirement) reqList.get(0);
    List<Object> schemaTypes = schemaReq.getTypes();
    for (Object schemaType : schemaTypes) {
      Assert.assertTrue(schemaType instanceof InputRecordSchema);
    }
  }
}
