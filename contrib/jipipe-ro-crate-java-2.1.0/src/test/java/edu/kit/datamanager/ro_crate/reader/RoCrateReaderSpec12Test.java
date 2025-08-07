package edu.kit.datamanager.ro_crate.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import edu.kit.datamanager.ro_crate.Crate;

/**
 * These reading tests are specific for reading 1.2 compliant crates.
 * 
 * They will refer to 1.2-DRAFT as long as the specification is not final.
 * Base for these tests are usually examples from the specification, but may
 * contain modifications if no example for a certain property is given.
 * 
 * Current specification: https://www.researchobject.org/ro-crate/1.2-DRAFT/
 */
public class RoCrateReaderSpec12Test {

    /**
     * Test reading a minimal crate with multiple conformsTo values, as described in
     * https://www.researchobject.org/ro-crate/1.2-DRAFT/profiles.html#declaring-conformance-of-an-ro-crate-profile
     */
    @Test
    void testReadingCrateWithConformsToArray() throws IOException {
        String path = this.getClass().getResource("/crates/spec-1.2-DRAFT/minimal-with-conformsTo-Array").getPath();
        Crate crate = Readers.newFolderReader().readCrate(path);
        JsonNode conformsTo = crate.getJsonDescriptor().getProperty("conformsTo");
        assertTrue(conformsTo.isArray());
        assertEquals(2, conformsTo.size());
        // filter the list to only contain the two expected values and expect no changes
        long numFiltered = StreamSupport.stream(conformsTo.spliterator(), false)
                .filter(node -> {
                    String asText = node.path("@id").asText();
                    return asText.equals("https://w3id.org/ro/crate/1.2-DRAFT")
                            || asText.equals("https://example.com/my/profile/1.0-TEST");
                })
                .count();
        assertEquals(2, numFiltered);
    }
}
