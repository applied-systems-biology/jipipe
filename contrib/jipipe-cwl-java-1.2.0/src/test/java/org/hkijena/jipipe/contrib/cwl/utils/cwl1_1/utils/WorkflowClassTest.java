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

package org.hkijena.jipipe.contrib.cwl.utils.cwl1_1.utils;

import java.net.URISyntaxException;

import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.RootLoader;
import org.junit.Assert;
import org.junit.Test;
import org.hkijena.jipipe.contrib.cwl.cwl1_1.CWLVersion;
import org.hkijena.jipipe.contrib.cwl.cwl1_1.Process;

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
