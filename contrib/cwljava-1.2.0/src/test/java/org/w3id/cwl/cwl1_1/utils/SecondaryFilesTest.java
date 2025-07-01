package org.w3id.cwl.cwl1_1.utils;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.w3id.cwl.cwl1_1.Process;
import org.w3id.cwl.cwl1_1.SecondaryFileSchema;
import org.w3id.cwl.cwl1_1.Workflow;
import org.w3id.cwl.cwl1_1.WorkflowInputParameter;

public class SecondaryFilesTest {
  List<Process> doc;

  @SuppressWarnings("unchecked")
  public SecondaryFilesTest() throws URISyntaxException {
    super();
    this.doc =
        (List<Process>)
            RootLoader.loadDocument(Paths.get(getClass().getResource("525.cwl").toURI()));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void workflowInputSecFiles() {
    Workflow workflow = (Workflow) doc.get(0);
    String workflow_id = workflow.getId().get();
    List<Object> inputs = workflow.getInputs();
    WorkflowInputParameter wf_file_input = (WorkflowInputParameter) inputs.get(1);
    Assert.assertEquals(workflow_id + "/wf_file_input", wf_file_input.getId().get());
    List<SecondaryFileSchema> sec_files_l1 =
        (List<SecondaryFileSchema>) wf_file_input.getSecondaryFiles();
    Assert.assertEquals(1, sec_files_l1.size());
    SecondaryFileSchema sec_files1 = sec_files_l1.get(0);
    Assert.assertEquals(".also", sec_files1.getPattern());
    Assert.assertEquals(true, sec_files1.getRequired());
    WorkflowInputParameter wf_file_input_array = (WorkflowInputParameter) inputs.get(2);
    Assert.assertEquals(workflow_id + "/wf_file_input_array", wf_file_input_array.getId().get());
    List<SecondaryFileSchema> sec_files_l2 =
        (List<SecondaryFileSchema>) wf_file_input_array.getSecondaryFiles();
    Assert.assertEquals(1, sec_files_l2.size());
    SecondaryFileSchema sec_files2 = sec_files_l2.get(0);
    Assert.assertEquals(".also", sec_files2.getPattern());
    Assert.assertEquals(true, sec_files2.getRequired());
  }
}
