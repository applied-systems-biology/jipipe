package edu.kit.datamanager.ro_crate.crate;

import edu.kit.datamanager.ro_crate.reader.Readers;
import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.RoCrate;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestRemoveAddContext {
  private RoCrate crateWithComplexContext;

  @BeforeEach
  void setup() throws IOException {
    String crateManifestPath = "/crates/extendedContextExample/";
    crateManifestPath = Objects.requireNonNull(TestRemoveAddContext.class.getResource(crateManifestPath)).getPath();
    this.crateWithComplexContext = Readers.newFolderReader().readCrate(crateManifestPath);
  }

  @Test
  void testAddRemoveValuePair() throws JsonProcessingException {
    RoCrate crate = new RoCrate.RoCrateBuilder().addValuePairToContext("key", "value").build();
    RoCrate defaultCrate = new RoCrate.RoCrateBuilder().build();

    crate.deleteValuePairFromContext("key");

    HelpFunctions.compareTwoCrateJson(crate, defaultCrate);

  }

  @Test
  void testAddRemoveUrl() throws JsonProcessingException {
    RoCrate crate = new RoCrate.RoCrateBuilder().addUrlToContext("https://example.com").build();
    RoCrate defaultCrate = new RoCrate.RoCrateBuilder().build();

    crate.deleteUrlFromContext("https://example.com");

    HelpFunctions.compareTwoCrateJson(crate, defaultCrate);

  }

  @Test
  void testReadDeleteGetContextPair() {
    String key = "custom";
    String value = "_:";
    assertEquals(value, this.crateWithComplexContext.getMetadataContextValueOf(key));
    this.crateWithComplexContext.deleteValuePairFromContext(key);
    assertNull(this.crateWithComplexContext.getMetadataContextValueOf(key));
  }

  @Test
  void testReadContextKeys() {
    var expected = Set.of("custom", "owl", "datacite", "xsd", "rdfs");
    var given = this.crateWithComplexContext.getMetadataContextKeys();
    for (String key : expected) {
        assertTrue(given.contains(key), "Key " + key + " not found in the context");
    }
    // prove immutability
    assertThrows(UnsupportedOperationException.class, () -> given.add("newKey"));
  }

  @Test
  void testReadContextPairs() {
    var expected = Set.of("custom", "owl", "datacite", "xsd", "rdfs");
    var given = this.crateWithComplexContext.getMetadataContextPairs();
    var keys = given.keySet();
    var values = given.values();
    for (String key : expected) {
        assertTrue(keys.contains(key), "Key " + key + " not found in the context");
        values.forEach(s -> assertFalse(s.isEmpty(), "Value for key " + key + " is empty"));
    }
    // prove immutability
    assertThrows(UnsupportedOperationException.class, () -> given.put("newKey", "newValue"));
  }
}
