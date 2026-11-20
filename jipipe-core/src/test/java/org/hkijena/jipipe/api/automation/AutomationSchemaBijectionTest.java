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

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The permanent gate of the schema builder reform: for every core operation, the wire
 * schema ({@link AutomationOperation#getInputSchema()}) must be EXACTLY the derivation of
 * its declared spec ({@link AutomationOperation#getSchemaSpec()} rendered through
 * {@link AutomationSchemaSpec#toJsonSchema()}).
 * <p>
 * The spec layer exists so a schema has one source of truth: the wire schema, the parameter
 * metadata, and the broker's tool descriptors all derive from the same builder calls. An
 * operation that re-introduces a hand-written {@code getInputSchema()} override (or any
 * other second schema source) fails here, because its wire schema drifts from its spec.
 * This replaces the deleted pre-migration snapshot scaffold, whose full-schema copy made
 * every future schema edit a two-place edit.
 * <p>
 * Population: the curated core operation-id set of {@link AutomationOperationsSchemaTest}
 * (single-source ship set, plugin-aware test policy), not the live registry.
 */
public class AutomationSchemaBijectionTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @Test
    void everyCoreOperationSchemaIsTheDerivationOfItsSpec() {
        Map<String, AutomationOperation> operations = JIPipe.getInstance().getAutomation()
                .getOperationRegistry().getAll();
        for (String id : AutomationOperationsSchemaTest.EXPECTED_CORE_OPERATION_IDS) {
            AutomationOperation operation = operations.get(id);
            assertNotNull(operation, "core operation '" + id + "' must be registered");
            assertEquals(operation.getSchemaSpec().toJsonSchema(), operation.getInputSchema(),
                    "the input schema of '" + id + "' must be the derivation of its schema spec");
        }
    }
}
