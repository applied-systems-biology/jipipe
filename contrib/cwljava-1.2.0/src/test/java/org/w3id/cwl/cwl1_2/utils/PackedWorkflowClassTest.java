package org.w3id.cwl.cwl1_2.utils;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.w3id.cwl.cwl1_2.Process;
import org.w3id.cwl.cwl1_2.Workflow;
import org.w3id.cwl.cwl1_2.WorkflowStep;
import org.w3id.cwl.cwl1_2.WorkflowStepInput;

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
