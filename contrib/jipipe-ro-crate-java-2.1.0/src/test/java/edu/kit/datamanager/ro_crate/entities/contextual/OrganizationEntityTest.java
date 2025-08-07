package edu.kit.datamanager.ro_crate.entities.contextual;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import edu.kit.datamanager.ro_crate.HelpFunctions;

import org.junit.jupiter.api.Test;

/**
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class OrganizationEntityTest {

  @Test
  void testSerialization() throws IOException {
    String id = "https://ror.org/03f0f6041";
    OrganizationEntity organization = new OrganizationEntity.OrganizationEntityBuilder()
        .setId(id)
        .setAddress("set")
        .setEmail("Sydney@sy.kit")
        .setTelephone("0665445")
        .setLocationId("#djfffff")
        .addProperty("url", id)
        .addProperty("name", "University of Technology Sydney")
        .build();

    assertEquals(id, organization.getId());
    HelpFunctions.compareEntityWithFile(organization, "/json/entities/contextual/organization.json");
  }
}
