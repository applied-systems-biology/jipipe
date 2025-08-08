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
import java.nio.file.Paths;
import java.util.List;

import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.RootLoader;
import org.junit.Assert;
import org.junit.Test;
import org.hkijena.jipipe.contrib.cwl.cwl1_1.Process;
import org.hkijena.jipipe.contrib.cwl.cwl1_1.Workflow;
import org.hkijena.jipipe.contrib.cwl.cwl1_1.WorkflowStep;
import org.hkijena.jipipe.contrib.cwl.cwl1_1.WorkflowStepInput;

public class PackedWorkflowClassTest {
  List<Process> doc;

  @SuppressWarnings("unchecked")
  public PackedWorkflowClassTest() throws URISyntaxException {
    super();
    this.doc =
        (List<Process>)
            RootLoader.loadDocument(
                Paths.get(getClass().getResource("valid_scatter-wf4.cwl").toURI()));
  }

  @Test
  public void className() {
    Workflow workflow = (Workflow) doc.get(1);
    Assert.assertEquals("WorkflowImpl", workflow.getClass().getSimpleName());
  }

  @Test
  public void workflowStepInputSources() {
    Workflow workflow = (Workflow) doc.get(1);
    String workflow_id = workflow.getId().get();
    WorkflowStep step1 = (WorkflowStep) workflow.getSteps().get(0);
    List<Object> inputs = step1.getIn();
    WorkflowStepInput step1_input1 = (WorkflowStepInput) inputs.get(0);
    Assert.assertEquals(workflow_id + "/step1/echo_in1", step1_input1.getId().get());
    Assert.assertEquals(workflow_id + "/inp1", step1_input1.getSource());
  }
}
