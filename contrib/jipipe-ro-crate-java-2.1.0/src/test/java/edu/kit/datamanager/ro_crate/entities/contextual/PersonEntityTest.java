package edu.kit.datamanager.ro_crate.entities.contextual;

import java.io.IOException;

import edu.kit.datamanager.ro_crate.HelpFunctions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class PersonEntityTest {

    @Test
    void personSerialization() throws IOException {
        String id = "https://orcid.org/0000-0001-6121-5409";
        PersonEntity person = new PersonEntity.PersonEntityBuilder()
                .setId(id)
                .setContactPoint("mailto:tim.luckett@uts.edu.au")
                .setAffiliation("https://ror.org/03f0f6041")
                .setFamilyName("Luckett")
                .setGivenName("Tim")
                .addProperty("name", "Tim Luckett")
                .build();
        assertEquals(id, person.getId());
        HelpFunctions.compareEntityWithFile(person, "/json/entities/contextual/person.json");
    }

    // test if creating an entity without Id will get a default UUI
    @Test
    void personWithoutId() {
        PersonEntity person = new PersonEntity.PersonEntityBuilder()
                .setContactPoint("mailto:tim.luckett@uts.edu.au")
                .setAffiliation("https://ror.org/03f0f6041")
                .setFamilyName("Luckett")
                .setGivenName("Tim")
                .addProperty("name", "Tim Luckett")
                .build();
        
        assertFalse(person.getId().isEmpty());
    }
}
