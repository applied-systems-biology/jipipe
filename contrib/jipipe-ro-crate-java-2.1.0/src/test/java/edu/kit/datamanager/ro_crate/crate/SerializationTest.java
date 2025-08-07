package edu.kit.datamanager.ro_crate.crate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.OrganizationEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PlaceEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataSetEntity;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Nikola Tzotchev on 6.2.2022 г.
 * @version 1
 */
public class SerializationTest {

  @Test
  void simpleROCrateSerialization() throws IOException {
    RoCrate roCrate = new RoCrate.RoCrateBuilder("minimal", "minimal RO_crate", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
        .build();
    HelpFunctions.compareCrateJsonToFileInResources(roCrate, "/json/crate/simple.json");
  }

  @Test
  void simpleTestWithBonusContextPair() throws IOException {
    RoCrate roCrate = new RoCrate.RoCrateBuilder("minimal", "minimal RO_crate", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
        .addValuePairToContext("@test", "ww.test")
        .build();
    HelpFunctions.compareCrateJsonToFileInResources(roCrate, "/json/crate/simple2.json");
  }

  @Test
  void onlyOneFile(@TempDir Path temp) throws IOException {
    Path cvs = temp.resolve("survey-responses-2019.csv");
    FileUtils.touch(cvs.toFile());
    FileUtils.writeStringToFile(cvs.toFile(), "fkdjaflkjfla", Charset.defaultCharset());
    RoCrate roCrate = new RoCrate.RoCrateBuilder("minimal", "minimal RO_crate", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
        .addDataEntity(
            new FileEntity.FileEntityBuilder()
                .setLocationWithExceptions(cvs)
                .setId(cvs.toFile().getName())
                .addProperty("name", "Survey responses")
                .addProperty("contentSize", "26452")
                .addProperty("encodingFormat", "text/csv")
                .build()
        )
        .build();

    HelpFunctions.compareCrateJsonToFileInResources(roCrate, "/json/crate/onlyOneFile.json");
  }

  @Test
  void twoSameId(@TempDir Path temp) throws IOException {
    Path cvs = temp.resolve("survey-responses-2019.csv");
    FileUtils.touch(cvs.toFile());
    FileUtils.writeStringToFile(cvs.toFile(), "fkdjaflkjfla", Charset.defaultCharset());

    RoCrate roCrate = new RoCrate.RoCrateBuilder("minimal", "minimal RO_crate", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
        .addDataEntity(
            new FileEntity.FileEntityBuilder()
                .setLocationWithExceptions(cvs)
                .setId(cvs.toFile().getName())
                .addProperty("name", "dadadada")
                .addProperty("contentSize", "26452")
                .addProperty("encodingFormat", "text/csv")
                .build()
        )
        .addDataEntity(
            new FileEntity.FileEntityBuilder()
                .setLocationWithExceptions(cvs)
                .setId(cvs.toFile().getName())
                .addProperty("name", "Survey responses")
                .addProperty("contentSize", "26452")
                .addProperty("encodingFormat", "text/csv")
                .build()
        )
        .build();
    HelpFunctions.compareCrateJsonToFileInResources(roCrate, "/json/crate/onlyOneFile.json");
  }

  @Test
  void twoFilesAndSomeContextualEntities(@TempDir Path temp) throws IOException {

    PlaceEntity place = new PlaceEntity.PlaceEntityBuilder()
        .setId("http://sws.geonames.org/8152662/")
        .addProperty("name", "Catalina Park")
        .build();

    PersonEntity person = new PersonEntity.PersonEntityBuilder()
        .setId("#alice")
        .addProperty("name", "Alice")
        .addProperty("description", "One of hopefully many Contextual Entities")
        .build();

    Path txt = temp.resolve("data1.txt");
    FileUtils.touch(txt.toFile());
    FileUtils.writeStringToFile(txt.toFile(), "fkdjaflkjfla", Charset.defaultCharset());
    FileEntity file = new FileEntity.FileEntityBuilder()
        .setLocationWithExceptions(txt)
        .setId(txt.toFile().getName())
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
            new FileEntity.FileEntityBuilder()
                .setLocationWithExceptions(txt)
                .setId("data2.txt")
                .build()
        )
        .build();

    HelpFunctions.compareCrateJsonToFileInResources(roCrate, "/json/crate/twoFiles.json");
  }


  @Test
  void fileAndDirTest(@TempDir Path temp) throws IOException {
    Path ai = temp.resolve("file.txt");
    FileUtils.touch(ai.toFile());
    FileUtils.writeStringToFile(ai.toFile(), "fkdjaflkjfla", Charset.defaultCharset());

    Path folder = temp.resolve("folder");
    FileUtils.forceMkdir(folder.toFile());
    RoCrate roCrate = new RoCrate.RoCrateBuilder("Example RO-Crate",
        "The RO-Crate Root Data Entity", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
        .addDataEntity(
            new FileEntity.FileEntityBuilder()
                .setLocationWithExceptions(ai)
                .setId("cp7glop.ai")
                .addProperty("name", "Diagram showing trend to increase")
                .addProperty("contentSize", "383766")
                .addProperty("description", "Illustrator file for Glop Pot")
                .setEncodingFormat("application/pdf")
                .build()
        )
        .addDataEntity(
            new DataSetEntity.DataSetBuilder()
                .setLocationWithExceptions(folder)
                .setId("lots_of_little_files/")
                .addProperty("name", "Too many files")
                .addProperty("description",
                    "This directory contains many small files, that we're not going to describe in detail.")
                .build()
        )
        .build();

    HelpFunctions.compareCrateJsonToFileInResources(roCrate, "/json/crate/fileAndDir.json");
  }

  @Test
  void BiggerExample(@TempDir Path temp) throws IOException {

    ContextualEntity license = new ContextualEntity.ContextualEntityBuilder()
        .setId("https://creativecommons.org/licenses/by/4.0/")
        .addType("CreativeWork")
        .addProperty("name", "CC BY 4.0")
        .addProperty("description", "Creative Commons Attribution 4.0 International License")
        .build();

    ContextualEntity geo = new ContextualEntity.ContextualEntityBuilder()
        .setId("#4241434-33413")
        .addType("GeoCoordinates")
        .addProperty("latitude", "49.00944")
        .addProperty("longitude", "8.41167")
        .build();

    PlaceEntity uni = new PlaceEntity.PlaceEntityBuilder()
        .setId("kit_location")
        .setGeo(geo.getId())
        .build();

    OrganizationEntity organization = new OrganizationEntity.OrganizationEntityBuilder()
        .setId("https://www.geonames.org/7288147")
        .addProperty("name", "Karlsruher Institut fuer Technologie")
        .addProperty("url", "https://www.kit.edu/")
        .setLocationId(uni.getId())
        .build();

    PlaceEntity country = new PlaceEntity.PlaceEntityBuilder()
        .setId("https://www.geonames.org/2921044")
        .addProperty("description", "Big country in central Europe.")
        .build();

    PersonEntity author = new PersonEntity.PersonEntityBuilder()
        .setId("creator")
        .setEmail("uuuuu@student.kit.edu")
        .setFamilyName("Tzotchev")
        .setGivenName("Nikola")
        .setAffiliation(organization.getId())
        .addIdProperty("nationality", country.getId())
        .build();

    ContextualEntity instrument = new ContextualEntity.ContextualEntityBuilder()
        .setId("https://www.aeroqual.com/product/outdoor-portable-monitor-starter-kit")
        .addType("IndividualProduct")
        .addProperty("description",
            "The Outdoor Air Quality Test Kit (Starter) is for users who want an affordable set of tools to measure the common pollutants in ambient outdoor air.")
        .build();

    Path folder = temp.resolve("folder");
    FileUtils.forceMkdir(folder.toFile());
    DataSetEntity measure = new DataSetEntity.DataSetBuilder()
        .setLocationWithExceptions(folder)
        .setId("measurements/")
        .addProperty("name", "Measurement Data")
        .addProperty("description", "This folder contains all relative to the measurements files.")
        .addAuthor(author.getId())
        .addIdProperty("license", license.getId())
        .build();

    ContextualEntity createAction = new ContextualEntity.ContextualEntityBuilder()
        .setId("#MeasurementCapture_23231")
        .addType("CreateAction")
        .addIdProperty("agent", author.getId())
        .addIdProperty("instrument", instrument.getId())
        .addIdProperty("result", measure.getId())
        .build();

    Path pdf = temp.resolve("mm.pdf");
    FileUtils.touch(pdf.toFile());
    FileUtils.writeStringToFile(pdf.toFile(), "fkdjaflkjfla", Charset.defaultCharset());

    RoCrate roCrate = new RoCrate.RoCrateBuilder("Air quality measurements in Karlsruhe",
        "Air quality measurements conducted in different places across Karlsruhe", "2024", "https://creativecommons.org/licenses/by/4.0/")
        .addContextualEntity(geo)
        .addContextualEntity(uni)
        .addContextualEntity(organization)
        .addContextualEntity(country)
        .addContextualEntity(author)
        .addDataEntity(
            new FileEntity.FileEntityBuilder()
                .setLocationWithExceptions(pdf)
                .setId("map.pdf")
                .addProperty("name", "Map of measurements")
                .addProperty("description",
                    "A map of all the location where the tests have been conducted")
                .addProperty("datePublished", "2021-10-22T00:00:00Z")
                .addProperty("encodingFormat", "application/pdf")
                .addAuthor(author.getId())
                .build()
        )
        .addDataEntity(measure)
        .addContextualEntity(instrument)
        .addContextualEntity(createAction)
        .build();

    HelpFunctions.compareCrateJsonToFileInResources(roCrate, "/json/crate/BiggerExample.json");
  }
}
