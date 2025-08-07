package edu.kit.datamanager.ro_crate.entities.contextual;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.kit.datamanager.ro_crate.HelpFunctions;

import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class ContextualEntityTest {

  /**
   * This is exactly the same test as the PlaceEntityTest it shows again (similarly to the
   * DataEntityTest) that we can also just use this class for the creation of our Contextual
   * Entities
   */
  @Test
  void testSerialization() throws IOException {
    // this does not make any difference for our testcase it just shows how the GeoCoordinates entity will look
    ContextualEntity geo = new ContextualEntity.ContextualEntityBuilder()
        .setId("#b4168a98-8534-4c6d-a568-64a55157b656")
        .addType("GeoCoordinates")
        .addProperty("latitude", "-33.7152")
        .addProperty("longitude", "150.30119")
        .addProperty("name", "Latitude: -33.7152 Longitude: 150.30119")
        .build();

    ContextualEntity place = new ContextualEntity.ContextualEntityBuilder()
        .addType("Place")
        .setId("https://sws.geonames.org/8152662/")
        .addProperty("description",
            "Catalina Park is a disused motor racing venue, located at Katoomba ...")
        .addProperty("identifier", "https://sws.geonames.org/8152662/")
        .addProperty("uri", "https://www.geonames.org/8152662/catalina-park.html")
        .addProperty("name", "Catalina Park")
        // here we can also do .setGeo(geo)
        .addIdProperty("geo", geo)
        .build();

    assertTrue(place.getLinkedTo().contains(geo.getId()));
    HelpFunctions.compareEntityWithFile(place, "/json/entities/contextual/place.json");
  }

  @Test
  void testAddAllValidCase() throws JsonProcessingException {
    ContextualEntity first = new ContextualEntity.ContextualEntityBuilder()
        .setId("#b4168a98-8534-4c6d-a568-64a55157b656")
        .addType("GeoCoordinates")
        .addProperty("latitude", "-33.7152")
        .addProperty("longitude", "150.30119")
        .addProperty("name", "Latitude: -33.7152 Longitude: 150.30119")
        .build();

    String allProperties = """
            {
                "@id": "#b4168a98-8534-4c6d-a568-64a55157b656",
                "@type": "GeoCoordinates",
                "latitude": "-33.7152",
                "longitude": "150.30119",
                "name": "Latitude: -33.7152 Longitude: 150.30119"
            }
            """;

    ObjectNode properties = MyObjectMapper.getMapper()
            .readValue(allProperties, ObjectNode.class);
    ContextualEntity second = new ContextualEntity.ContextualEntityBuilder()
            .setAllIfValid(properties)
            .build();
    assertEquals(second.getProperties(), first.getProperties());
  }

  @Test
  void testAddAllInvalidCase() throws JsonProcessingException {
    ContextualEntity first = new ContextualEntity.ContextualEntityBuilder()
            .setId("#b4168a98-8534-4c6d-a568-64a55157b656")
            .addType("GeoCoordinates")
            .addProperty("latitude", "-33.7152")
            .addProperty("longitude", "150.30119")
            .addProperty("name", "Latitude: -33.7152 Longitude: 150.30119")
            .build();

    String allProperties = """
            {
                "wrong property": {"any": "value"},
                "@id": "#b4168a98-8534-4c6d-a568-64a55157b656",
                "@type": "GeoCoordinates",
                "latitude": "-33.7152",
                "longitude": "150.30119",
                "name": "Latitude: -33.7152 Longitude: 150.30119"
            }
            """;

    ObjectNode properties = MyObjectMapper.getMapper()
            .readValue(allProperties, ObjectNode.class);
    ContextualEntity second = new ContextualEntity.ContextualEntityBuilder()
            .setId("second")
            .setAllIfValid(properties)
            .build();
    assertNotEquals(second.getProperties(), first.getProperties());
    ObjectNode empty = new ContextualEntity.ContextualEntityBuilder()
            .setId("second")
            .build()
            .getProperties();
    assertEquals(empty, second.getProperties());
  }

  @Test
  void testAddAllUnsafeDoesInvalidCase() throws JsonProcessingException {
    ContextualEntity first = new ContextualEntity.ContextualEntityBuilder()
            .setId("#b4168a98-8534-4c6d-a568-64a55157b656")
            .addType("GeoCoordinates")
            .addProperty("latitude", "-33.7152")
            .addProperty("longitude", "150.30119")
            .addProperty("name", "Latitude: -33.7152 Longitude: 150.30119")
            .build();

    String allProperties = """
            {
                "wrong property": {"any": "value"},
                "@id": "#b4168a98-8534-4c6d-a568-64a55157b656",
                "@type": "GeoCoordinates",
                "latitude": "-33.7152",
                "longitude": "150.30119",
                "name": "Latitude: -33.7152 Longitude: 150.30119"
            }
            """;

    ObjectNode properties = MyObjectMapper.getMapper()
            .readValue(allProperties, ObjectNode.class);
    ContextualEntity second = new ContextualEntity.ContextualEntityBuilder()
            .setId("second")
            .setAllUnsafe(properties)
            .build();
    assertNotEquals(second.getProperties(), first.getProperties());
    ObjectNode empty = new ContextualEntity.ContextualEntityBuilder()
            .setId("second")
            .build()
            .getProperties();
    assertNotEquals(empty, second.getProperties());
  }
}
