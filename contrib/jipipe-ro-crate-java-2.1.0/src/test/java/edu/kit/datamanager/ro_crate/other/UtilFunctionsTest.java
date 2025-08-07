package edu.kit.datamanager.ro_crate.other;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;
import edu.kit.datamanager.ro_crate.special.JsonUtilFunctions;

import org.junit.jupiter.api.Test;
import io.json.compare.JSONCompare;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilFunctionsTest {

  @Test
  void testUnwrapSimple() throws IOException {
    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    InputStream fileJson =
        UtilFunctionsTest.class.getResourceAsStream("/json/unwrap/simple.json");

    JsonNode node = JsonUtilFunctions.unwrapSingleArray(objectMapper.readTree(fileJson));
    JsonNode expectedResult = objectMapper.readTree(
        UtilFunctionsTest.class.getResourceAsStream("/json/unwrap/simpleResult.json")
    );
    JSONCompare.assertMatches(node, expectedResult);
  }

  @Test
  void testGettingIdEntriesFromJsonProperty() {
    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    ObjectNode jsonNode = objectMapper.createObjectNode();
    jsonNode.put("@id", "string1");

    var result = JsonUtilFunctions.getIdPropertiesFromProperty(jsonNode);
    assertEquals(1, result.size());
    assertEquals("string1", result.stream().findFirst().get());


    ArrayNode arrayNode = objectMapper.createArrayNode();
    arrayNode.add(objectMapper.createObjectNode().put("@id", "string1"));
    arrayNode.add(objectMapper.createObjectNode().put("@id", "string2"));

    result = JsonUtilFunctions.getIdPropertiesFromProperty(arrayNode);
    assertEquals(2, result.size());
    assertTrue(result.contains("string1"));
    assertTrue(result.contains("string2"));
  }

  @Test
  void testGettingIdFromObject() {
    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    ObjectNode jsonNode = objectMapper.createObjectNode();

    jsonNode.put("random", "not id");
    jsonNode.set("other", objectMapper.createArrayNode().add("kdfjdlk").add("kdfjalkfj"));

    jsonNode.set("test", objectMapper.createObjectNode().put("@id", "test1"));

    jsonNode.set("another", objectMapper.createObjectNode().put("@id", "test2"));

    jsonNode.set("singleArray", objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("@id", "test3")));
    jsonNode.set("multipleArrayEl", objectMapper.createArrayNode()
        .add(objectMapper.createObjectNode().put("@id", "test4"))
        .add(objectMapper.createObjectNode().put("@id", "test5")));

    var set = JsonUtilFunctions.getIdPropertiesFromJsonNode(jsonNode);

    assertEquals(5, set.size());

    for (int i = 1 ; i <= 5 ; i++) {
      assertTrue(set.contains("test"+i));
    }
  }
}
