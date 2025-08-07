package edu.kit.datamanager.ro_crate.crate.realexamples;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.contextual.ActionEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.ActionType;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.OrganizationEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PlaceEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataSetEntity;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity;
import edu.kit.datamanager.ro_crate.externalproviders.personprovider.OrcidProvider;
import edu.kit.datamanager.ro_crate.reader.Readers;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RealTest
{
    @Test
    void testWithIDRCProject(@TempDir Path temp) throws IOException {
        final String locationMetadataFile = "/crates/other/idrc_project/ro-crate-metadata.json";
        Crate crate = Readers.newFolderReader()
                .readCrate(RealTest.class.getResource("/crates/other/idrc_project").getPath());

        assertNotNull(crate);
        HelpFunctions.compareCrateJsonToFileInResources(crate, locationMetadataFile);

        Path newFile = temp.resolve("new_file.txt");

        FileUtils.writeStringToFile(newFile.toFile(), "blablablalblalblabla", Charset.defaultCharset());
        crate.addDataEntity(
                new FileEntity.FileEntityBuilder()
                        .setLocationWithExceptions(newFile.toAbsolutePath())
                        .setId("new_file.txt")
                        .addProperty("description", "my new file that I added")
                        .build());

        PersonEntity person = OrcidProvider.getPerson("https://orcid.org/0000-0001-9842-9718");
        assertNotNull(person);
        crate.addContextualEntity(person);

        // problem
        ContextualEntity en = crate.getContextualEntityById("9a4e89e1-13bf-4d44-b5f7-ced40eb33cb2");
        en.addIdProperty("custom", "new_file.txt");

        HelpFunctions.compareTwoMetadataJsonNotEqual(crate, locationMetadataFile);
        crate.deleteEntityById("https://orcid.org/0000-0001-9842-9718");
        crate.deleteEntityById("new_file.txt");

        HelpFunctions.compareCrateJsonToFileInResources(crate, locationMetadataFile);
    }

    @Test
    void testRealExampleWithActions() throws IOException {
        DataSetEntity dataSet = new DataSetEntity.DataSetBuilder()
                .setId("measurements/")
                .addProperty("name", "Measurement Data")
                .addProperty("description", "This folder contains all relative to the measurements files.")
                .addAuthor("creator")
                .addIdProperty("license", "https://creativecommons.org/licenses/by/4.0/")
                .build();

        FileEntity file = new FileEntity.FileEntityBuilder()
                .setId("map.pdf")
                .addProperty("name", "Map of measurements")
                .addProperty("description", "A map of all the location where the tests have been conducted")
                .addProperty("encodingFormat", "application/pdf")
                .addAuthor("creator")
                .build();

        PersonEntity person = new PersonEntity.PersonEntityBuilder()
                .setId("creator")
                .addProperty("email", "uuuuu@student.kit.edu")
                .addProperty("givenName", "Nikola")
                .addProperty("familyName", "Tzotchev")
                .addIdProperty("nationality", "https://www.geonames.org/2921044")
                .addIdProperty("affiliation", "https://www.geonames.org/7288147")
                .build();

        ContextualEntity license = new ContextualEntity.ContextualEntityBuilder()
                .setId("https://creativecommons.org/licenses/by/4.0/")
                .addType("CreativeWork")
                .addProperty("name", "CC BY 4.0")
                .addProperty("description", "Creative Commons Attribution 4.0 International License")
                .build();

        PlaceEntity place = new PlaceEntity.PlaceEntityBuilder()
                .setId("https://www.geonames.org/2921044")
                .addProperty("description", "Big country in central Europe.")
                .build();

        PlaceEntity placeKIT = new PlaceEntity.PlaceEntityBuilder()
                .setId("kit_location")
                .setGeo("#4241434-33413")
                .build();

        ActionEntity createAction = new ActionEntity.ActionEntityBuilder(ActionType.CREATE)
                .setId("#MeasurementCapture_23231")
                .setAgent("creator")
                .addInstrument("https://www.aeroqual.com/product/outdoor-portable-monitor-starter-kit")
                .addResult("measurements/")
                .build();

        ContextualEntity geo = new ContextualEntity.ContextualEntityBuilder()
                .setId("#4241434-33413")
                .addType("GeoCoordinates")
                .addProperty("latitude", "49.00944")
                .addProperty("longitude", "8.41167")
                .build();
        
         OrganizationEntity organization = new OrganizationEntity.OrganizationEntityBuilder()
                .setId("https://www.geonames.org/7288147")
                .addProperty("name", "Karlsruher Institut fuer Technologie")
                .addProperty("url", "https://www.kit.edu/")
                .setLocationId("kit_location")
                .build();

        RoCrate crate = new RoCrate.RoCrateBuilder("Air quality measurements in Karlsruhe", "Air quality measurements conducted in different places across Karlsruhe", "2024", "https://creativecommons.org/licenses/by/4.0/")
                .addDataEntity(dataSet)
                .addDataEntity(file)
                .addContextualEntity(person)
                .addContextualEntity(license)
                .addContextualEntity(place)
                .addContextualEntity(createAction)
                .addContextualEntity(placeKIT)
                .addContextualEntity(geo)
                .addContextualEntity(organization)
                .build();

        HelpFunctions.compareCrateJsonToFileInResources(crate, "/json/crate/BiggerExample.json");
        assertEquals(7, crate.getAllContextualEntities().size());
        assertEquals(2, crate.getAllDataEntities().size());

    }
}
