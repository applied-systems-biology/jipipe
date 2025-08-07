package edu.kit.datamanager.ro_crate.crate;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PlaceEntity;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

public class TestRemoveAddEntity {
    @Test
    void testAddRemoveEntity() throws IOException {
        RoCrate roCrate = new RoCrate.RoCrateBuilder("minimal", "minimal RO_crate", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .addDataEntity(
                        new FileEntity.FileEntityBuilder()
                                .setId("survey-responses-2019.csv")
                                .addProperty("name", "Survey responses")
                                .addProperty("contentSize", "26452")
                                .addProperty("encodingFormat", "text/csv")
                                .build()
                )
                .build();
        HelpFunctions.compareCrateJsonToFileInResources(roCrate, "/json/crate/onlyOneFile.json");

        assertEquals(0, roCrate.getAllContextualEntities().size());
        assertEquals(1, roCrate.getAllDataEntities().size());

        // remove entity and check if equals to the basic crate
        roCrate.deleteEntityById("survey-responses-2019.csv");
        assertEquals(0, roCrate.getAllDataEntities().size());
        HelpFunctions.compareCrateJsonToFileInResources(roCrate, "/json/crate/simple.json");
    }

    @Test
    void withTwoFiles() throws IOException {
        PlaceEntity place = new PlaceEntity.PlaceEntityBuilder()
                .setId("http://sws.geonames.org/8152662/")
                .addProperty("name", "Catalina Park")
                .build();

        PersonEntity person = new PersonEntity.PersonEntityBuilder()
                .setId("#alice")
                .addProperty("name", "Alice")
                .addProperty("description", "One of hopefully many Contextual Entities")
                .build();

        FileEntity file = new FileEntity.FileEntityBuilder()
                .setId("data1.txt")
                .addProperty("description", "One of hopefully many Data Entities")
                .setContentLocation(place.getId())
                .addAuthor(person.getId())
                .build();

        RoCrate roCrate = new RoCrate.RoCrateBuilder("Example RO-Crate",
                "The RO-Crate Root Data Entity", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .addContextualEntity(place)
                .addContextualEntity(person)
                .addDataEntity(file)
                .addDataEntity(
                        new FileEntity.FileEntityBuilder().setId("data2.txt").build()
                )
                .build();

        HelpFunctions.compareCrateJsonToFileInResources(roCrate, "/json/crate/twoFiles.json");

        assertEquals(2, roCrate.getAllDataEntities().size());
        assertEquals(2, roCrate.getAllContextualEntities().size());

        roCrate.deleteEntityById("data1.txt");
        assertEquals(1, roCrate.getAllDataEntities().size());

        roCrate.deleteEntityById("data2.txt");
        assertEquals(0, roCrate.getAllDataEntities().size());

        roCrate.deleteEntityById("#alice");
        assertEquals(1, roCrate.getAllContextualEntities().size());

        roCrate.deleteEntityById("http://sws.geonames.org/8152662/");
        assertEquals(0, roCrate.getAllContextualEntities().size());

        RoCrate roCrate2 = new RoCrate.RoCrateBuilder("Example RO-Crate",
                "The RO-Crate Root Data Entity", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/").build();
        HelpFunctions.compareTwoCrateJson(roCrate, roCrate2);
    }

    @Test
    void removeOtherOccur() throws JsonProcessingException {
        PlaceEntity place = new PlaceEntity.PlaceEntityBuilder()
                .setId("http://sws.geonames.org/8152662/")
                .addProperty("name", "Catalina Park")
                .build();

        PersonEntity person = new PersonEntity.PersonEntityBuilder()
                .setId("#alice")
                .addProperty("name", "Alice")
                .addProperty("description", "One of hopefully many Contextual Entities")
                .build();

        FileEntity file = new FileEntity.FileEntityBuilder()
                .setId("data1.txt")
                .addProperty("description", "One of hopefully many Data Entities")
                .setContentLocation(place.getId())
                .addAuthor(person.getId())
                .addAuthor(person.getId())
                .build();

        RoCrate roCrate = new RoCrate.RoCrateBuilder("Example RO-Crate",
                "The RO-Crate Root Data Entity", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .addContextualEntity(place)
                .addContextualEntity(person)
                .addDataEntity(file)
                .build();

        assertEquals(1, roCrate.getAllDataEntities().size());
        assertEquals(2, roCrate.getAllContextualEntities().size());

        roCrate.deleteEntityById(person.getId());
        assertEquals(1, roCrate.getAllContextualEntities().size());

        roCrate.deleteEntityById(place.getId());
        assertEquals(0, roCrate.getAllContextualEntities().size());
        assertEquals(1, roCrate.getAllDataEntities().size());

        FileEntity file2 = new FileEntity.FileEntityBuilder()
                .setId("data1.txt")
                .addProperty("description", "One of hopefully many Data Entities")
                .build();
        RoCrate second2 = new RoCrate.RoCrateBuilder("Example RO-Crate", "The RO-Crate Root Data Entity", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .addDataEntity(file2)
                .build();

        HelpFunctions.compareTwoCrateJson(roCrate, second2);
    }

    @Test
    void testAddEntitiesWithSameId() {

        PersonEntity person = new PersonEntity.PersonEntityBuilder()
                .setId("person")
                .addProperty("name", "Alice")
                .addProperty("description", "One of hopefully many Contextual Entities")
                .build();

        RoCrate roCrate = new RoCrate.RoCrateBuilder("Example RO-Crate",
                "The RO-Crate Root Data Entity", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .addContextualEntity(person)
                .build();

        RoCrate roCrateNew = new RoCrate.RoCrateBuilder("Example RO-Crate",
                "The RO-Crate Root Data Entity", "2023", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .addContextualEntity(person)
                .build();
        
        assertEquals(1, roCrate.getAllContextualEntities().size());
        assertEquals(1, roCrateNew.getAllContextualEntities().size());

        PersonEntity personNew = new PersonEntity.PersonEntityBuilder()
                .setId("person")
                .addProperty("name", "Peter Sefton")
                .addProperty("email", "peter.sefton@uts.edu.au")
                .build();
        
        roCrateNew.addContextualEntity(personNew);
        
        assertEquals(1, roCrate.getAllContextualEntities().size());
        assertEquals(1, roCrateNew.getAllContextualEntities().size());
        
        assertEquals(roCrate.getContextualEntityById("person"), person);
        assertEquals(roCrateNew.getContextualEntityById("person"), personNew);
    }
}
