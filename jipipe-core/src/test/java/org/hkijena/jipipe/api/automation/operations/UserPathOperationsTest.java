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
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the project user path CRUD of the automation layer: list_user_paths,
 * set_user_path (upsert that preserves unset fields of an existing entry), and
 * remove_user_path. Everything is driven through the operations.
 */
public class UserPathOperationsTest {

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

    private static JsonNode list(JIPipeProject project) {
        return new UserPathOperations.ListUserPaths()
                .execute(createContext(project), mapper.createObjectNode());
    }

    private static JsonNode set(JIPipeProject project, ObjectNode params) {
        return new UserPathOperations.SetUserPath().execute(createContext(project), params);
    }

    private static JsonNode remove(JIPipeProject project, String key) {
        ObjectNode params = mapper.createObjectNode();
        params.put("key", key);
        return new UserPathOperations.RemoveUserPath().execute(createContext(project), params);
    }

    private static ObjectNode setParams(String key, Path path) {
        ObjectNode params = mapper.createObjectNode();
        params.put("key", key);
        params.put("path", path.toString());
        return params;
    }

    /**
     * Finds the single list entry with the given key; fails if it is missing or duplicated.
     */
    private static JsonNode entry(JsonNode listResult, String key) {
        ArrayNode matches = mapper.createArrayNode();
        JsonNode userPaths = listResult.get("userPaths");
        for (JsonNode userPath : userPaths) {
            if (key.equals(userPath.get("key").asText())) {
                matches.add(userPath);
            }
        }
        assertEquals(1, matches.size(), "expected exactly one entry with key '" + key + "' in " + userPaths);
        return matches.get(0);
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static String absolute(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    @Test
    void listUserPaths_emptyOnFreshProject() {
        JIPipeProject project = new JIPipeProject();

        JsonNode result = list(project);

        assertTrue(result.get("userPaths").isArray(), "userPaths must be an array, never an omitted key");
        assertEquals(0, result.get("userPaths").size());
    }

    @Test
    void setUserPath_createAndList() {
        JIPipeProject project = new JIPipeProject();
        Path path = Path.of("example-user-path");
        ObjectNode params = setParams("data", path);
        params.put("name", "Data");
        params.put("description", "The input data");
        params.put("role", "Input");

        JsonNode setResult = set(project, params);

        assertEquals("data", setResult.get("key").asText());
        assertTrue(setResult.get("created").asBoolean());

        JsonNode result = list(project);
        assertEquals(1, result.get("userPaths").size());
        JsonNode userPath = result.get("userPaths").get(0);
        // The pinned shape: every key is present (empty-string convention)
        assertEquals(Set.of("key", "name", "description", "path", "role"), fieldNames(userPath));
        assertEquals("data", userPath.get("key").asText());
        assertEquals("Data", userPath.get("name").asText());
        assertEquals("The input data", userPath.get("description").asText());
        assertEquals(absolute(path), userPath.get("path").asText());
        assertEquals("Input", userPath.get("role").asText());
    }

    @Test
    void setUserPath_updateInPlacePreservesOtherFields() {
        JIPipeProject project = new JIPipeProject();
        ObjectNode create = setParams("data", Path.of("first"));
        create.put("name", "Data");
        create.put("description", "The input data");
        create.put("role", "Input");
        set(project, create);

        // Update with a new path and a new role only: name and description are preserved
        ObjectNode update = setParams("data", Path.of("second"));
        update.put("role", "Output");
        JsonNode updateResult = set(project, update);

        assertFalse(updateResult.get("created").asBoolean());
        assertEquals(1, list(project).get("userPaths").size(), "an existing key must not be duplicated");
        JsonNode userPath = entry(list(project), "data");
        assertEquals(absolute(Path.of("second")), userPath.get("path").asText());
        assertEquals("Output", userPath.get("role").asText());
        assertEquals("Data", userPath.get("name").asText());
        assertEquals("The input data", userPath.get("description").asText());

        // Update with a name only: the description and the role stay as they are
        ObjectNode rename = setParams("data", Path.of("second"));
        rename.put("name", "Renamed");
        set(project, rename);

        JsonNode renamed = entry(list(project), "data");
        assertEquals("Renamed", renamed.get("name").asText());
        assertEquals("The input data", renamed.get("description").asText());
        assertEquals("Output", renamed.get("role").asText());
    }

    @Test
    void setUserPath_roleDefaultsUnspecified() {
        JIPipeProject project = new JIPipeProject();

        set(project, setParams("data", Path.of("somewhere")));

        JsonNode userPath = entry(list(project), "data");
        assertEquals("Unspecified", userPath.get("role").asText());
        assertEquals("", userPath.get("name").asText());
        assertEquals("", userPath.get("description").asText());
    }

    @Test
    void setUserPath_blankKeyRefused() {
        JIPipeProject project = new JIPipeProject();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> set(project, setParams("   ", Path.of("somewhere"))));

        assertTrue(ex.getMessage().contains("key"), "the error must name the offending field: " + ex.getMessage());
        assertEquals(0, list(project).get("userPaths").size(), "a refused call must not touch the project");
    }

    @Test
    void removeUserPath_removesMatched() {
        JIPipeProject project = new JIPipeProject();
        set(project, setParams("first", Path.of("a")));
        set(project, setParams("second", Path.of("b")));

        JsonNode result = remove(project, "first");

        assertTrue(result.get("removed").asBoolean());
        JsonNode remaining = list(project).get("userPaths");
        assertEquals(1, remaining.size());
        assertEquals("second", remaining.get(0).get("key").asText());

        // Remove the second entry too: the list is empty again
        assertTrue(remove(project, "second").get("removed").asBoolean());
        assertEquals(0, list(project).get("userPaths").size());
    }

    @Test
    void removeUserPath_unknownKeyErrorsListingKnown() {
        JIPipeProject project = new JIPipeProject();
        set(project, setParams("alpha", Path.of("a")));
        set(project, setParams("beta", Path.of("b")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> remove(project, "nope"));

        String message = ex.getMessage();
        assertTrue(message.contains("nope"), "the error must name the unknown key: " + message);
        assertTrue(message.contains("alpha"), "the error must list the known keys: " + message);
        assertTrue(message.contains("beta"), "the error must list the known keys: " + message);
        assertEquals(2, list(project).get("userPaths").size(), "a refused call must not touch the project");
    }
}