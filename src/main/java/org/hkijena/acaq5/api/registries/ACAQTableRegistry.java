/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.extensions.tables.operations.ColumnOperation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for table operations
 */
public class ACAQTableRegistry {
    private Map<String, ColumnOperationEntry> registeredColumnOperations = new HashMap<>();

    /**
     * @return all registered column operations
     */
    public Map<String, ColumnOperationEntry> getRegisteredColumnOperations() {
        return Collections.unmodifiableMap(registeredColumnOperations);
    }

    /**
     * Registers a new operation
     *
     * @param id          unique id
     * @param operation   operation instance
     * @param name        name
     * @param shortName   a short name that will be used in table headers (e.g. avg, var, ...)
     * @param description a description
     */
    public void registerColumnOperation(String id, ColumnOperation operation, String name, String shortName, String description) {
        registeredColumnOperations.put(id, new ColumnOperationEntry(id, operation, name, shortName, description));
    }

    /**
     * Returns an operation by ID
     *
     * @param id the ID
     * @return the operation
     */
    public ColumnOperationEntry getColumnOperationById(String id) {
        return registeredColumnOperations.get(id);
    }

    /**
     * Returns all operations that are assignable to the filter class
     *
     * @param filterClass the filter class
     * @return all operations that are assignable to the filter class
     */
    public Map<String, ColumnOperationEntry> getOperationsOfType(Class<? extends ColumnOperation> filterClass) {
        Map<String, ColumnOperationEntry> result = new HashMap<>();
        for (Map.Entry<String, ColumnOperationEntry> entry : registeredColumnOperations.entrySet()) {
            if (filterClass.isAssignableFrom(entry.getValue().getOperation().getClass())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    public static ACAQTableRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getTableRegistry();
    }

    /**
     * A column operation entry
     */
    public static class ColumnOperationEntry {
        private String id;
        private ColumnOperation operation;
        private String name;
        private String shortName;
        private String description;

        /**
         * Creates a new entry
         *
         * @param id          unique id
         * @param operation   the operation
         * @param name        name
         * @param shortName   a short name
         * @param description description
         */
        public ColumnOperationEntry(String id, ColumnOperation operation, String name, String shortName, String description) {
            this.id = id;
            this.operation = operation;
            this.name = name;
            this.shortName = shortName;
            this.description = description;
        }

        public ColumnOperation getOperation() {
            return operation;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getId() {
            return id;
        }

        public String getShortName() {
            return shortName;
        }
    }
}
