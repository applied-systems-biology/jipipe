package edu.kit.datamanager.ro_crate.entities.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class DataSetEntityTest {

    @Test
    void testImpossibleRootId() {
        DataSetEntity e = new DataSetEntity.DataSetBuilder()
                .setId("./")
                .addProperty("not_root", true)
                .build();
        assertNotNull(e.getId());
        assertFalse(e.getId().isBlank());
        assertNotEquals(RootDataEntity.ID, e.getId());
    }

    @Test
    void testSimpleDirDeserialization() throws IOException {

        String id = "lots_of_little_files/";
        DataSetEntity dir = new DataSetEntity.DataSetBuilder()
                // this does not change anything, but it shows that the entity contains a physical file
                .setLocationWithExceptions(Paths.get("invalid_file"))
                .setId(id)
                .addProperty("name", "Too many files")
                .addProperty("description",
                        "This directory contains many small files, that we're not going to describe in detail.")
                .build();

        assertEquals(id, dir.getId());
        HelpFunctions.compareEntityWithFile(dir, "/json/entities/data/directory.json");
    }

    /**
     * This is a test with a directory that is located on the web. It is
     * recommended that such a dir list all of its files in the hasPart property
     * https://www.researchobject.org/ro-crate/1.1/data-entities.html#directories-on-the-web-dataset-distributions
     */
    @Test
    void testDirWithHasPartDeserialization() throws IOException {

        String id = "second_content";
        DataEntity second_content = new DataEntity.DataEntityBuilder()
                .setLocationWithExceptions(Paths.get("does_not_matter"))
                .setId(id)
                .addProperty("description", "This entity just describes one of the contents in the Dir")
                .build();

        // we can add to hasPart using directly the id, or passing the entity to it
        DataSetEntity dir = new DataSetEntity.DataSetBuilder()
                .setLocation(URI.create("https://www.example.com/urltodir"))
                .addProperty("name", "Directory that is located on the web")
                .addToHasPart("first_content")
                .addToHasPart(second_content)
                .build();

        assertTrue(dir.hasInHasPart(id));
        HelpFunctions.compareEntityWithFile(dir, "/json/entities/data/directoryWeb.json");
    }

    @Test
    void testWebDir() {
        ObjectMapper objectMapper = MyObjectMapper.getMapper();
        ObjectNode propertyValue = objectMapper.createObjectNode();
        propertyValue.put("@id", "http://example.com/downloads/2020/lots_of_little_files.zip");

        DataSetEntity dir = new DataSetEntity.DataSetBuilder()
                .setLocationWithExceptions(Paths.get("lots_of_little_files/"))
                .setId("lots_of_little_files/")
                .addProperty("name", "Too many files")
                .addProperty("description", "This directory contains many small files, that we're not going to describe in detail.")
                .addProperty("distribution", propertyValue)
                .build();

        assertEquals(dir.getProperty("distribution"), propertyValue);

        DataEntity webDir = new DataEntity.DataEntityBuilder()
                .setLocation(URI.create("http://example.com/downloads/2020/lots_of_little_files.zip"))
                .addType("DataDownload")
                .addProperty("encodingFormat", "application/zip")
                .addProperty("contentSize", "82818928")
                .build();
        
        assertNotNull(webDir);
    }
}
