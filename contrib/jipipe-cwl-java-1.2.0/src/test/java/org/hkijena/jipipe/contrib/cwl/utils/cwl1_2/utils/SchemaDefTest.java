/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.contrib.cwl.utils.cwl1_2.utils;

import java.util.List;

import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.RootLoader;
import org.junit.Assert;
import org.hkijena.jipipe.contrib.cwl.cwl1_2.InputRecordSchema;
import org.hkijena.jipipe.contrib.cwl.cwl1_2.Process;
import org.hkijena.jipipe.contrib.cwl.cwl1_2.SchemaDefRequirement;

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
