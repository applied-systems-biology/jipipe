package edu.kit.datamanager.ro_crate.entities.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import edu.kit.datamanager.ro_crate.HelpFunctions;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/**
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class WorkflowEntityTest {

  @Test
  void testSerialization() throws IOException {
    String id = "workflow/alignment.knime";
    WorkflowEntity entity = new WorkflowEntity.WorkflowEntityBuilder()
        .setLocationWithExceptions(Paths.get("src"))
        .setId(id)
        .addIdProperty("conformsTo", "https://bioschemas.org/profiles/ComputationalWorkflow/0.5-DRAFT-2020_07_21/")
        .addProperty("name", "Sequence alignment workflow")
        .addIdProperty("programmingLanguage", "#knime")
        .addAuthor("#alice")
        .addProperty("dateCreated", "2020-05-23")
        .setLicense("https://spdx.org/licenses/CC-BY-NC-SA-4.0")
        .addInput("#36aadbd4-4a2d-4e33-83b4-0cbf6a6a8c5b")
        .addOutput("#6c703fee-6af7-4fdb-a57d-9e8bc4486044")
        .addOutput("#2f32b861-e43c-401f-8c42-04fd84273bdf")
        .addProperty("url", "http://example.com/workflows/alignment")
        .addProperty("version", "0.5.0")
        .addIdProperty("sdPublisher", "#workflow-hub")
        .build();

    assertEquals(id, entity.getId());
    HelpFunctions.compareEntityWithFile(entity, "/json/entities/data/workflow.json");
  }
}
