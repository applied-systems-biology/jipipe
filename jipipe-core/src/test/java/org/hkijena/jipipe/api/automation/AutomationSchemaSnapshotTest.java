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

package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration safety net for the schema builder reform: pins the pre-migration input schema of
 * every core operation in a committed test resource. Any accidental wire change during the
 * migration (a lost enum, a changed default, a description typo) fails here. The comparison is
 * structural ({@code JsonNode.equals} is key-order independent for objects), so reformatting or
 * reordering cannot produce false alarms while any text or keyword change does.
 * <p>
 * The scaffold is temporary: it is deleted once the migration is complete, because a permanent
 * snapshot would make every schema edit a two-place edit.
 */
public class AutomationSchemaSnapshotTest {

    /**
     * Ship-set pin: the curated core set has exactly one owner ({@code AutomationPlugin}), so an
     * exact count assertion is allowed here (test policy). Must be bumped only together with
     * {@code EXPECTED_CORE_OPERATION_IDS} when a core operation is added.
     */
    private static final int EXPECTED_SNAPSHOT_ENTRY_COUNT = 76;

    private static final String SNAPSHOT_RESOURCE = "automation-schema-snapshot.json";

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static AutomationOperationRegistry registry() {
        return JIPipe.getInstance().getAutomation().getOperationRegistry();
    }

    private static ObjectNode loadSnapshot() throws Exception {
        try (InputStream stream = AutomationSchemaSnapshotTest.class
                .getResourceAsStream("/" + SNAPSHOT_RESOURCE)) {
            assertNotNull(stream, "the pre-migration snapshot resource '" + SNAPSHOT_RESOURCE
                    + "' must exist (generate it with generateSnapshotResource)");
            JsonNode node = new ObjectMapper().readTree(stream);
            assertTrue(node.isObject(), "the snapshot resource must hold a JSON object");
            return (ObjectNode) node;
        }
    }

    @Test
    void everyCoreOperationMatchesThePreMigrationSnapshot() throws Exception {
        JsonNode snapshot = loadSnapshot();
        Map<String, AutomationOperation> operations = registry().getAll();
        for (String id : AutomationOperationsSchemaTest.EXPECTED_CORE_OPERATION_IDS) {
            AutomationOperation operation = operations.get(id);
            assertNotNull(operation, "core operation '" + id + "' must be registered");
            JsonNode expected = snapshot.get(id);
            assertNotNull(expected, "the snapshot must contain an entry for '" + id + "'");
            assertEquals(expected, operation.getInputSchema(),
                    "the input schema of '" + id + "' must still match the pre-migration snapshot");
        }
    }

    @Test
    void snapshotAndRegistryHaveTheSameIdSet() throws Exception {
        ObjectNode snapshot = loadSnapshot();
        assertEquals(EXPECTED_SNAPSHOT_ENTRY_COUNT, snapshot.size(),
                "the snapshot must pin the complete core operation ship set");
        TreeSet<String> snapshotIds = new TreeSet<>();
        snapshot.fieldNames().forEachRemaining(snapshotIds::add);
        assertEquals(new TreeSet<>(AutomationOperationsSchemaTest.EXPECTED_CORE_OPERATION_IDS), snapshotIds,
                "the snapshot must hold exactly the curated core operation ids");
        assertEquals(new TreeSet<>(registry().getAll().keySet()), snapshotIds,
                "the registry id set must equal the snapshot id set: an operation registered but"
                        + " absent from the snapshot must be added to"
                        + " AutomationOperationsSchemaTest.EXPECTED_CORE_OPERATION_IDS, and a curated"
                        + " id must not be unregistered");
    }

    /**
     * One-shot generator for the snapshot resource. Run it once on the pre-migration tree:
     * remove the {@code @Disabled} annotation, run
     * {@code mvn test -pl jipipe-core -Dtest='AutomationSchemaSnapshotTest#generateSnapshotResource'},
     * restore the annotation, and commit the written resource together with this test. The
     * registry state at generation time is the wire contract the migration must preserve.
     * <p>
     * The JSON is pretty-printed with the operation ids in sorted order: an {@link ObjectNode}
     * serializes fields in insertion order, so the ids are inserted sorted
     * (ORDER_MAP_ENTRIES_BY_KEYS covers Map-based serializations; it does not reorder
     * ObjectNode fields).
     */
    @Test
    @Disabled("one-shot generator: run manually on the pre-migration tree, see Javadoc")
    void generateSnapshotResource() throws Exception {
        Map<String, AutomationOperation> operations = registry().getAll();
        List<String> missing = new ArrayList<>();
        ObjectNode snapshot = new ObjectMapper().createObjectNode();
        for (String id : new TreeSet<>(AutomationOperationsSchemaTest.EXPECTED_CORE_OPERATION_IDS)) {
            AutomationOperation operation = operations.get(id);
            if (operation == null) {
                missing.add(id);
                continue;
            }
            JsonNode schema = operation.getInputSchema();
            assertNotNull(schema, "operation '" + id + "' must declare an input schema");
            assertTrue(schema.isObject(), "the input schema of '" + id + "' must be a JSON object");
            snapshot.set(id, schema.deepCopy());
        }
        assertTrue(missing.isEmpty(), "core operations missing from the registry: " + missing);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        Path target = Path.of("src", "test", "resources", SNAPSHOT_RESOURCE);
        Files.createDirectories(target.getParent());
        Files.write(target, (mapper.writeValueAsString(snapshot) + "\n").getBytes(StandardCharsets.UTF_8));
        System.out.println("automation-schema-snapshot.json: wrote " + snapshot.size()
                + " entries to " + target.toAbsolutePath());
    }
}
