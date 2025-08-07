package edu.kit.datamanager.ro_crate.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A class for reading a crate from a folder.
 *
 * @author Nikola Tzotchev on 9.2.2022 г.
 * @version 1
 */
public class ReadFolderStrategy implements GenericReaderStrategy<String> {

  @Override
  public ObjectNode readMetadataJson(String location) throws IOException {
    Path metadata = new File(location).toPath().resolve("ro-crate-metadata.json");
    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    ObjectNode objectNode = objectMapper.createObjectNode();
    objectNode = objectMapper.readTree(metadata.toFile()).deepCopy();
    return objectNode;
  }

  @Override
  public File readContent(String location) {
    return new File(location);
  }
}
