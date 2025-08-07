package edu.kit.datamanager.ro_crate.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RoCrateMetadataContextTest {

  RoCrateMetadataContext context;
  RoCrateMetadataContext complexContext;

  @BeforeEach
  void initContext() throws IOException {
    // this will load the default context
    this.context = new RoCrateMetadataContext();

    final String crateManifestPath = "/crates/extendedContextExample/ro-crate-metadata.json";
    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    JsonNode jsonNode = objectMapper.readTree(RoCrateMetadataContextTest.class.getResourceAsStream(crateManifestPath));
    this.complexContext = new RoCrateMetadataContext(jsonNode.get("@context"));
  }

  @Test
  void testContext() {

    var entity = this.context.getContextJsonEntity();

    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    // we expect the default RO-crate context;
    var expected = objectMapper.createObjectNode();
    expected.put("@context", "https://w3id.org/ro/crate/1.1/context");
    HelpFunctions.compare(entity, expected, true);

    // this entity is not correct
    // there is no type blabla in schema.org
    var contextualEntity = new ContextualEntity.ContextualEntityBuilder()
        .setId("dkfaj")
        .addType("blabla")
        .build();

    assertFalse(this.context.checkEntity(contextualEntity));

    // this entity is correct the type Accommodation exists in schema.org,
    // so it should be valid
    var contextualEntityCorrect = new ContextualEntity.ContextualEntityBuilder()
        .setId("dkfaj")
        .addType("Accommodation")
        .addProperty("name", "ffjkjk")
        .build();

    assertTrue(this.context.checkEntity(contextualEntityCorrect));

    // this entity has correct types but wrong field
    var contextualEntityWrongField = new ContextualEntity.ContextualEntityBuilder()
        .setId("dkfaj")
        .addType("Accommodation")
        .addProperty("notExisting", "kfdjfk")
        .build();

    assertFalse(this.context.checkEntity(contextualEntityWrongField));
  }

  @Test
  void creationUrlTest() {
    RoCrateMetadataContext newContext = new RoCrateMetadataContext(
        List.of("www.example.com", "www.example.com/context"));

    var contextualEntityWrongField = new ContextualEntity.ContextualEntityBuilder()
        .setId("dkfaj")
        .addType("Accommodation")
        .build();
    // this will be false since here there is no default context, and the example
    // context could not be retrieved
    assertFalse(newContext.checkEntity(contextualEntityWrongField));
    // the two example context should be nevertheless added to the final result
    var jsonNode = newContext.getContextJsonEntity();
    assertEquals(2, jsonNode.get("@context").size());
  }

  @Test
  void creationFromPairsJsonTest() {
    var objectMapper = MyObjectMapper.getMapper();

    ObjectNode rawContext = objectMapper.createObjectNode();
    rawContext.put("house", "www.example.com/house");
    rawContext.put("road", "www.example.com/road");

    ObjectNode rawCrate = objectMapper.createObjectNode();
    rawCrate.set("@context", rawContext);
    RoCrateMetadataContext newContext = new RoCrateMetadataContext(rawContext);
    assertNotNull(newContext);

    HelpFunctions.compare(newContext.getContextJsonEntity(), rawCrate, true);

    var entityWithTerms = new ContextualEntity.ContextualEntityBuilder()
            .setId("dkfaj")
            .addType("house")
            .addType("road")
            .build();
    assertTrue(newContext.checkEntity(entityWithTerms));
  }

  @Test
  void deletePairTest() {
    RoCrateMetadataContext context = new RoCrateMetadataContext();
    RoCrateMetadataContext emptyContext = new RoCrateMetadataContext();

    context.addToContext("key", "value");

    context.deleteValuePairFromContext("key");

    HelpFunctions.compare(context.getContextJsonEntity(), emptyContext.getContextJsonEntity(), true);
  }

  @Test
  void deleteNonExistingPairTest() {
    RoCrateMetadataContext context = new RoCrateMetadataContext();
    RoCrateMetadataContext emptyContext = new RoCrateMetadataContext();

    context.deleteValuePairFromContext("key");

    HelpFunctions.compare(context.getContextJsonEntity(), emptyContext.getContextJsonEntity(), true);
  }

  @Test
  void deleteUrlTest() {
    RoCrateMetadataContext context = new RoCrateMetadataContext();
    RoCrateMetadataContext emptyContext = new RoCrateMetadataContext();

    context.addToContextFromUrl("www.example.com");

    context.deleteUrlFromContext("www.example.com");

    HelpFunctions.compare(context.getContextJsonEntity(), emptyContext.getContextJsonEntity(), true);

  }

  @Test
  void doubledContextUrlsTest() {
    String url = "www.example.com";
    RoCrateMetadataContext context = new RoCrateMetadataContext();
    assertFalse(context.urls.contains(url));
    context.addToContextFromUrl(url);
    assertTrue(context.urls.contains(url));

    RoCrateMetadataContext contextDoubled = new RoCrateMetadataContext();
    contextDoubled.addToContextFromUrl(url);
    contextDoubled.addToContextFromUrl(url);
    
    HelpFunctions.compare(context.getContextJsonEntity(), contextDoubled.getContextJsonEntity(), true);

    RoCrateMetadataContext emptyContext = new RoCrateMetadataContext();
    contextDoubled.deleteUrlFromContext(url);

    HelpFunctions.compare(emptyContext.getContextJsonEntity(), contextDoubled.getContextJsonEntity(), true);
  }

  @Test
  void deleteNonExistentUrlTest() {
    RoCrateMetadataContext context = new RoCrateMetadataContext();
    RoCrateMetadataContext emptyContext = new RoCrateMetadataContext();

    context.deleteUrlFromContext("www.example.com");

    HelpFunctions.compare(context.getContextJsonEntity(), emptyContext.getContextJsonEntity(), true);
  }

  @Test
  void creationFromUrlAndPairsTest() {

    var objectMapper = MyObjectMapper.getMapper();

    ObjectNode res = objectMapper.createObjectNode();
    ArrayNode arr = objectMapper.createArrayNode();

    ObjectNode node = objectMapper.createObjectNode();
    node.put("house", "www.example.con/house");
    node.put("road", "www.example.con/road");
    arr.add(node);
    arr.add("www.example.com/context");

    res.set("@context", arr);
    RoCrateMetadataContext newContext = new RoCrateMetadataContext(arr);
    HelpFunctions.compare(newContext.getContextJsonEntity(), res, true);

    var data = new DataEntity.DataEntityBuilder()
        .addType("house")
        .setId("https://www.example.com/entity")
        .build();
    // house is in the context
    assertTrue(newContext.checkEntity(data));
  }

  @Test
  void testIdType() {
    AbstractEntity validEntity = new DataEntity.DataEntityBuilder()
            .setId("Airline")  // this is defined in the context!
            .addType("@id")  // this is a JSON-LD feature to refer to the ID ("Airline") as a type
            .addType("@json")  // this is a JSON-LD built-in type
            .build();
    assertTrue(this.context.checkEntity(validEntity));

    AbstractEntity invalidEntity = new DataEntity.DataEntityBuilder()
            .setId("Something which is definitely not in the context")
            .addType("@id")
            .build();
    assertFalse(this.context.checkEntity(invalidEntity));
  }

  @Test
  void testJsonType() {
    AbstractEntity validEntity = new DataEntity.DataEntityBuilder()
            .addType("@json")  // this is a JSON-LD built-in type
            .build();
    assertTrue(this.context.checkEntity(validEntity));
  }

  @Test
  void testAbsoluteUrlType() {
    AbstractEntity validEntity = new DataEntity.DataEntityBuilder()
            .addType("http://example.org/Person")  // this is not in the context!
            .addProperty("http://example.org/Thing", "Some thing")
            .build();
    assertTrue(this.context.checkEntity(validEntity));
  }

  @Test
  void testSetDeleteGetPair() {
    String key = "key";
    String value = "value";
    context.addToContext(key, value);
    assertEquals(value, context.getValueOf(key));
    context.deleteValuePairFromContext(key);
    assertNull(context.getValueOf(key));
  }

  @Test
  void testReadDeleteGetPair() {
    String key = "custom";
    String value = "_:";
    assertEquals(value, this.complexContext.getValueOf(key));
    this.complexContext.deleteValuePairFromContext(key);
    assertNull(this.complexContext.getValueOf(key));
    this.complexContext.addToContext(key, value);
    assertEquals(value, this.complexContext.getValueOf(key));
  }

  @Test
  void testReadKeys() {
    var expected = Set.of("custom", "owl", "datacite", "xsd", "rdfs");
    var given = this.complexContext.getKeys();
    for (String key : expected) {
      assertTrue(given.contains(key), "Key " + key + " not found in the context");
    }
    // prove immutability
    assertThrows(UnsupportedOperationException.class, () -> given.add("newKey"));
  }

  @Test
  void testReadPairs() {
    var expected = Set.of("custom", "owl", "datacite", "xsd", "rdfs");
    var given = this.complexContext.getPairs();
    var keys = given.keySet();
    var values = given.values();
    for (String key : expected) {
      assertTrue(keys.contains(key), "Key " + key + " not found in the context");
      values.forEach(s -> assertFalse(s.isEmpty(), "Value for key " + key + " is empty"));
    }
    // prove immutability
    assertThrows(UnsupportedOperationException.class, () -> given.put("newKey", "newValue"));
  }

  @Test
  void checkEntity_withDefinedPrefixedType_succeeds() throws IOException {
    // assume we read a crate just for demonstration
    RoCrate crate = new RoCrate();
    // and we extend the context
    RoCrateMetadataContext context =  new RoCrateMetadataContext();
    context.addToContext("rdfs", "https://www.w3.org/2000/01/rdf-schema#");
    crate.setMetadataContext(context);
    // then we use the new context
    DataEntity entity = new DataEntity.DataEntityBuilder()
            .addType("rdfs:Property")
            .build();
    crate.addDataEntity(entity);
    // Then we expect this to work
    assertTrue(context.checkEntity(entity));
  }
}
