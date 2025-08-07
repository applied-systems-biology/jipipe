package edu.kit.datamanager.ro_crate.externalproviders;

import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.ro_crate.externalproviders.personprovider.OrcidProvider;

import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Nikola Tzotchev on 10.2.2022 г.
 * @version 1
 */
public class OrcidProviderTest {
    
  @Test
  void testAddingPersonEntity() throws IOException {
    PersonEntity person = OrcidProvider.getPerson("https://orcid.org/0000-0001-9842-9718");
    assertNotNull(person);
    HelpFunctions.compareEntityWithFile(person, "/json/entities/contextual/orcidperson.json");
  }

  @Test
  void testInvalidOrcidUrl() {
    assertThrows(IllegalArgumentException.class, () -> {
      OrcidProvider.getPerson("https://notorcid.org/1234");
    });
  }

  @Test
  void testInvalidOrcidId() {
    PersonEntity person = OrcidProvider.getPerson("https://orcid.org/42");
    assertNull(person);
  }

}
