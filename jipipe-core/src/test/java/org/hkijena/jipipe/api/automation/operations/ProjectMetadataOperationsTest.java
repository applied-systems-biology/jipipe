/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the full project metadata read/write of the automation layer:
 * the get_project_info metadata enrichment and the extended set_project_metadata.
 */
public class ProjectMetadataOperationsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static AutomationContext createContext(JIPipeProject project) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(project, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    private static JsonNode getInfo(JIPipeProject project) {
        return new InfoOperations.GetProjectInfo().execute(createContext(project), mapper.createObjectNode());
    }

    private static JsonNode setMetadata(JIPipeProject project, ObjectNode params) {
        return new ModificationOperations.SetProjectMetadata().execute(createContext(project), params);
    }

    private static JsonNode checkFair(JIPipeProject project) {
        return new InfoOperations.CheckFair().execute(createContext(project), mapper.createObjectNode());
    }

    private static ObjectNode authorParam(String firstName, String lastName) {
        ObjectNode entry = mapper.createObjectNode();
        entry.put("firstName", firstName);
        entry.put("lastName", lastName);
        return entry;
    }

    private static ArrayNode arrayNode(JsonNode... entries) {
        ArrayNode array = mapper.createArrayNode();
        for (JsonNode entry : entries) {
            array.add(entry);
        }
        return array;
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @Test
    void getProjectInfo_metadataEmitsAllFieldsWithEmptyDefaults() {
        JIPipeProject project = new JIPipeProject();

        JsonNode metadata = getInfo(project).get("metadata");

        assertEquals("", metadata.get("license").asText());
        assertEquals("", metadata.get("summary").asText());
        assertEquals("", metadata.get("website").asText());
        assertEquals("", metadata.get("citation").asText());
        assertTrue(metadata.get("authors").isArray(), "authors must be an array, never an omitted key");
        assertEquals(0, metadata.get("authors").size());
        assertTrue(metadata.get("acknowledgements").isArray(), "acknowledgements must be an array, never an omitted key");
        assertEquals(0, metadata.get("acknowledgements").size());
    }

    @Test
    void getProjectInfo_authorShape() {
        JIPipeProject project = new JIPipeProject();
        JIPipeAuthorMetadata author = new JIPipeAuthorMetadata();
        author.setFirstName("R");
        author.setLastName("G");
        author.setOrcid("0000-0001");
        author.setEmail("r.g@example.org");
        JIPipeOrganizationMetadata affiliation = new JIPipeOrganizationMetadata();
        affiliation.setName("HKI");
        affiliation.setRor("02z9w1g91");
        author.getAffiliations().add(affiliation);
        project.getMetadata().getAuthors().add(author);

        JsonNode metadata = getInfo(project).get("metadata");
        assertEquals(1, metadata.get("authors").size());
        JsonNode entry = metadata.get("authors").get(0);

        // The exact pinned shape: every key is present at every level (empty-string
        // convention), booleans always present
        assertEquals(Set.of("firstName", "lastName", "orcid", "email", "website",
                "firstAuthor", "correspondingAuthor", "affiliations"), fieldNames(entry));
        assertEquals("R", entry.get("firstName").asText());
        assertEquals("G", entry.get("lastName").asText());
        assertEquals("0000-0001", entry.get("orcid").asText());
        assertEquals("r.g@example.org", entry.get("email").asText());
        assertEquals("", entry.get("website").asText());
        assertFalse(entry.get("firstAuthor").asBoolean());
        assertFalse(entry.get("correspondingAuthor").asBoolean());
        assertEquals(1, entry.get("affiliations").size());
        JsonNode affiliationNode = entry.get("affiliations").get(0);
        assertEquals(Set.of("name", "ror", "website"), fieldNames(affiliationNode));
        assertEquals("HKI", affiliationNode.get("name").asText());
        assertEquals("02z9w1g91", affiliationNode.get("ror").asText());
        assertEquals("", affiliationNode.get("website").asText());
    }

    @Test
    void setProjectMetadata_scalarsRoundTrip() {
        JIPipeProject project = new JIPipeProject();
        ObjectNode params = mapper.createObjectNode();
        params.put("license", "MIT");
        params.put("summary", "A short summary");
        params.put("website", "https://example.org");
        params.put("citation", "Doe 2026");

        setMetadata(project, params);

        JsonNode metadata = getInfo(project).get("metadata");
        assertEquals("MIT", metadata.get("license").asText());
        assertEquals("A short summary", metadata.get("summary").asText());
        assertEquals("https://example.org", metadata.get("website").asText());
        assertEquals("Doe 2026", metadata.get("citation").asText());
    }

    @Test
    void setProjectMetadata_clearWithBlankString() {
        JIPipeProject project = new JIPipeProject();
        ObjectNode initial = mapper.createObjectNode();
        initial.put("license", "MIT");
        setMetadata(project, initial);

        ObjectNode clear = mapper.createObjectNode();
        clear.put("license", "");
        setMetadata(project, clear);

        // Stored as-is (clearing intent), no trim-to-default
        assertEquals("", getInfo(project).get("metadata").get("license").asText());
    }

    @Test
    void setProjectMetadata_authorsReplaceNotMerge() {
        JIPipeProject project = new JIPipeProject();
        ObjectNode initial = mapper.createObjectNode();
        initial.set("authors", arrayNode(authorParam("A", "One")));
        initial.set("acknowledgements", arrayNode(authorParam("X", "Ack")));
        setMetadata(project, initial);

        setMetadata(project, (ObjectNode) mapper.createObjectNode()
                .set("authors", arrayNode(authorParam("B", "Two"), authorParam("C", "Three"))));

        JsonNode metadata = getInfo(project).get("metadata");
        assertEquals(2, metadata.get("authors").size(), "the provided array replaces the whole list");
        assertEquals("B", metadata.get("authors").get(0).get("firstName").asText());
        assertEquals("Two", metadata.get("authors").get(0).get("lastName").asText());
        assertEquals("C", metadata.get("authors").get(1).get("firstName").asText());
        assertEquals(1, metadata.get("acknowledgements").size(), "an omitted key keeps the current list");
        assertEquals("X", metadata.get("acknowledgements").get(0).get("firstName").asText());

        // Acknowledgements are replaced independently of authors
        ObjectNode acknowledgementsOnly = mapper.createObjectNode();
        acknowledgementsOnly.set("acknowledgements", arrayNode(authorParam("Y", "Ack2")));
        setMetadata(project, acknowledgementsOnly);

        metadata = getInfo(project).get("metadata");
        assertEquals(2, metadata.get("authors").size(), "authors are kept when the key is omitted");
        assertEquals("B", metadata.get("authors").get(0).get("firstName").asText());
        assertEquals(1, metadata.get("acknowledgements").size());
        assertEquals("Y", metadata.get("acknowledgements").get(0).get("firstName").asText());
    }

    @Test
    void setProjectMetadata_omittedParamsKeepCurrent() {
        JIPipeProject project = new JIPipeProject();
        ObjectNode initial = mapper.createObjectNode();
        initial.put("license", "MIT");
        initial.put("summary", "A summary");
        initial.put("website", "https://example.org");
        initial.put("citation", "Doe 2026");
        initial.set("authors", arrayNode(authorParam("A", "One")));
        setMetadata(project, initial);

        ObjectNode licenseOnly = mapper.createObjectNode();
        licenseOnly.put("license", "GPL v3");
        setMetadata(project, licenseOnly);

        JsonNode metadata = getInfo(project).get("metadata");
        assertEquals("GPL v3", metadata.get("license").asText());
        assertEquals("A summary", metadata.get("summary").asText());
        assertEquals("https://example.org", metadata.get("website").asText());
        assertEquals("Doe 2026", metadata.get("citation").asText());
        assertEquals(1, metadata.get("authors").size());
        assertEquals("A", metadata.get("authors").get(0).get("firstName").asText());
    }

    @Test
    void setProjectMetadata_invalidAuthorEntryRefusesWholeCall() {
        JIPipeProject project = new JIPipeProject();
        setMetadata(project, (ObjectNode) mapper.createObjectNode()
                .set("authors", arrayNode(authorParam("A", "One"))));
        JsonNode authorsBefore = getInfo(project).get("metadata").get("authors").deepCopy();

        ObjectNode invalid = mapper.createObjectNode();
        invalid.put("firstName", "B");
        ObjectNode refused = (ObjectNode) mapper.createObjectNode()
                .set("authors", arrayNode(authorParam("B", "Two"), invalid));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> setMetadata(project, refused));
        assertEquals("Invalid authors entry at index 1: lastName is required", ex.getMessage());

        // No partial apply: the pre-existing list is byte-identical after the failed call
        assertEquals(authorsBefore, getInfo(project).get("metadata").get("authors"));
    }

    @Test
    void setProjectMetadata_invalidAcknowledgementEntryRefusesWholeCall() {
        JIPipeProject project = new JIPipeProject();
        ObjectNode initial = mapper.createObjectNode();
        initial.set("acknowledgements", arrayNode(authorParam("X", "Ack")));
        setMetadata(project, initial);
        JsonNode acknowledgementsBefore = getInfo(project).get("metadata").get("acknowledgements").deepCopy();

        ObjectNode invalid = mapper.createObjectNode();
        invalid.put("lastName", "NoFirstName");
        ObjectNode refused = mapper.createObjectNode();
        refused.set("acknowledgements", arrayNode(invalid));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> setMetadata(project, refused));
        assertEquals("Invalid acknowledgements entry at index 0: firstName is required", ex.getMessage());
        assertEquals(acknowledgementsBefore, getInfo(project).get("metadata").get("acknowledgements"));
    }

    @Test
    void setProjectMetadata_affiliationWithoutNameRefusesWholeCall() {
        JIPipeProject project = new JIPipeProject();
        setMetadata(project, (ObjectNode) mapper.createObjectNode()
                .set("authors", arrayNode(authorParam("A", "One"))));
        JsonNode authorsBefore = getInfo(project).get("metadata").get("authors").deepCopy();

        // A missing and a blank affiliation name are both refusals with the same error
        ObjectNode missingName = authorParam("B", "Two");
        missingName.putArray("affiliations").addObject();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> setMetadata(project, (ObjectNode) mapper.createObjectNode()
                        .set("authors", arrayNode(missingName))));
        assertEquals("Invalid authors affiliation at index 0: name is required", ex.getMessage());

        ObjectNode blankName = authorParam("C", "Three");
        blankName.putArray("affiliations").addObject().put("name", "   ");
        ex = assertThrows(IllegalArgumentException.class,
                () -> setMetadata(project, (ObjectNode) mapper.createObjectNode()
                        .set("authors", arrayNode(blankName))));
        assertEquals("Invalid authors affiliation at index 0: name is required", ex.getMessage());

        // No partial apply: the pre-existing list is byte-identical after the failed calls
        assertEquals(authorsBefore, getInfo(project).get("metadata").get("authors"));
    }

    @Test
    void setProjectMetadata_emptyArrayReplacesWithEmpty() {
        JIPipeProject project = new JIPipeProject();
        setMetadata(project, (ObjectNode) mapper.createObjectNode()
                .set("authors", arrayNode(authorParam("A", "One"))));

        // A present array, even an empty one, replaces the whole list;
        // only the absence of the key keeps the current list
        ObjectNode empty = mapper.createObjectNode();
        empty.putArray("authors");
        setMetadata(project, empty);

        JsonNode metadata = getInfo(project).get("metadata");
        assertTrue(metadata.get("authors").isArray());
        assertEquals(0, metadata.get("authors").size());
    }

    @Test
    void setProjectMetadata_nameAndDescriptionUnchanged() {
        JIPipeProject project = new JIPipeProject();
        ObjectNode params = mapper.createObjectNode();
        params.put("name", "Renamed project");
        params.put("description", "A new description");

        setMetadata(project, params);

        JsonNode info = getInfo(project);
        assertEquals("Renamed project", info.get("name").asText());
        assertEquals("A new description", info.get("metadata").get("description").asText());
    }

    @Test
    void checkFair_freshProjectMissingAllThree() {
        JIPipeProject project = new JIPipeProject();

        JsonNode result = checkFair(project);

        assertFalse(result.get("fair").asBoolean());
        assertTrue(result.get("missing").isArray(), "missing must be an array, never an omitted key");
        assertEquals(3, result.get("missing").size());

        // The checker strings pass through verbatim; the prefixes are pinned so a
        // rewording in the checker fails here and not in dependent agents
        assertTrue(result.get("missing").get(0).asText().startsWith("License -"),
                "got: " + result.get("missing").get(0).asText());
        assertTrue(result.get("missing").get(1).asText().startsWith("Summary -"),
                "got: " + result.get("missing").get(1).asText());
        assertTrue(result.get("missing").get(2).asText().startsWith("Authors -"),
                "got: " + result.get("missing").get(2).asText());

        // The exact known string for the license item
        assertEquals("License - no license is set on the project metadata.",
                result.get("missing").get(0).asText());
    }

    @Test
    void checkFair_completeProjectIsFair() {
        JIPipeProject project = new JIPipeProject();
        ObjectNode params = mapper.createObjectNode();
        params.put("license", "MIT");
        params.put("summary", "A short summary");
        params.set("authors", arrayNode(authorParam("A", "One")));
        setMetadata(project, params);

        JsonNode result = checkFair(project);

        assertTrue(result.get("fair").asBoolean());
        assertEquals(0, result.get("missing").size());
    }

    @Test
    void checkFair_eachFieldFlipsItsItem() {
        JIPipeProject project = new JIPipeProject();
        ObjectNode complete = mapper.createObjectNode();
        complete.put("license", "MIT");
        complete.put("summary", "A short summary");
        complete.set("authors", arrayNode(authorParam("A", "One")));
        setMetadata(project, complete);

        // Clear the license (blank string) -> only the license item is missing
        setMetadata(project, (ObjectNode) mapper.createObjectNode().put("license", ""));
        JsonNode result = checkFair(project);
        assertFalse(result.get("fair").asBoolean());
        assertEquals(1, result.get("missing").size());
        assertTrue(result.get("missing").get(0).asText().startsWith("License -"),
                "got: " + result.get("missing").get(0).asText());

        // Restore the license, clear the summary -> only the summary item is missing
        ObjectNode restoreLicenseClearSummary = mapper.createObjectNode();
        restoreLicenseClearSummary.put("license", "MIT");
        restoreLicenseClearSummary.put("summary", "");
        setMetadata(project, restoreLicenseClearSummary);
        result = checkFair(project);
        assertFalse(result.get("fair").asBoolean());
        assertEquals(1, result.get("missing").size());
        assertTrue(result.get("missing").get(0).asText().startsWith("Summary -"),
                "got: " + result.get("missing").get(0).asText());

        // Restore the summary, remove the author (authors: []) -> only the authors item is missing
        ObjectNode restoreSummaryRemoveAuthor = mapper.createObjectNode();
        restoreSummaryRemoveAuthor.put("summary", "A short summary");
        restoreSummaryRemoveAuthor.putArray("authors");
        setMetadata(project, restoreSummaryRemoveAuthor);
        result = checkFair(project);
        assertFalse(result.get("fair").asBoolean());
        assertEquals(1, result.get("missing").size());
        assertTrue(result.get("missing").get(0).asText().startsWith("Authors -"),
                "got: " + result.get("missing").get(0).asText());
    }

    @Test
    void checkFair_blankStringsCountAsMissing() {
        JIPipeProject project = new JIPipeProject();
        ObjectNode params = mapper.createObjectNode();
        params.put("license", "   ");
        params.put("summary", "   ");
        params.set("authors", arrayNode(authorParam("A", "One")));
        setMetadata(project, params);

        // set_project_metadata stores blank values as-is (no defaults); the FAIR
        // report must still flag them as missing (the checker trims)
        JsonNode result = checkFair(project);

        assertFalse(result.get("fair").asBoolean());
        assertEquals(2, result.get("missing").size());
        assertTrue(result.get("missing").get(0).asText().startsWith("License -"),
                "got: " + result.get("missing").get(0).asText());
        assertTrue(result.get("missing").get(1).asText().startsWith("Summary -"),
                "got: " + result.get("missing").get(1).asText());
    }
}
