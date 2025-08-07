package edu.kit.datamanager.ro_crate.entities.data;

import java.io.IOException;
import java.net.URL;

import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity.FileEntityBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Nikola Tzotchev on 4.2.2022 г.
 * @version 1
 */
public class FileEntityTest {

  /**
   * The FileEntity class provides a few methods that help to create an entity
   * For any Data Entity (File, Dir, Workflow) it is also possible to just use the
   * DataEntity class (check DataEntityTest for examples) the only difference is it will be more
   * "work"
   * @throws IOException if reading fails
   */
  @Test
  void testSerialization() throws IOException {
    String name = "RO-Crate specification";
    FileEntity file = new FileEntityBuilder()
        .setLocation(URI.create("https://zenodo.org/record/3541888/files/ro-crate-1.0.0.pdf"))
        .addProperty("name", name)
        .setEncodingFormat("application/pdf")
        .addProperty("url", "https://zenodo.org/record/3541888")
        .build();

    assertEquals(name, file.getProperty("name").asText());
    HelpFunctions.compareEntityWithFile(file, "/json/entities/data/fileEntity.json");
  }

  @Test
  void testSerializationWithPhysicalFile() throws IOException, URISyntaxException {

    // add a random json file
    URL url =
        HelpFunctions.class.getResource("/json/crate/simple2.json");
    assert url != null;
    FileEntity file = new FileEntityBuilder()
        .setLocationWithExceptions(Paths.get(url.toURI()))
        .setId("example.json")
        .addProperty("name", "RO-Crate specification")
        .setEncodingFormat("application/json")
        .build();
    HelpFunctions.compareEntityWithFile(file, "/json/entities/data/localFile.json");
  }

  @Test
  void testSerializationWithLicense() throws IOException, URISyntaxException {
    ContextualEntity entity = new ContextualEntity.ContextualEntityBuilder()
        .setId("https://creativecommons.org/licenses/by/4.0/")
        .addType("CreativeWork")
        .build();

    URL url =
        HelpFunctions.class.getResource("/json/crate/simple2.json");
    assertNotNull(url);
    FileEntity file = new FileEntityBuilder()
        .setLocationWithExceptions(Paths.get(url.toURI()))
        .setId("example.json")
        .addProperty("name", "RO-Crate specification")
        .setEncodingFormat("application/json")
        .setLicense(entity)
        .build();
    HelpFunctions.compareEntityWithFile(file, "/json/entities/data/localFileWithLicense.json");
  }
}
