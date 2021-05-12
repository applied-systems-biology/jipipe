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

package org.hkijena.jipipe.api.registries;

import org.hkijena.jipipe.extensions.expressions.DefaultExpressionEvaluator;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.tables.ColumnOperation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for table operations and other functional expressions
 */
public class JIPipeExpressionRegistry {
    private Map<String, ColumnOperationEntry> registeredTableColumnOperations = new HashMap<>();
    private Map<String, ExpressionFunctionEntry> registeredExpressionFunctions = new HashMap<>();

    /**
     * The registered column operations. Column operations can work on arrays of {@link String} or {@link Double}
     *
     * @return all registered column operations
     */
    public Map<String, ColumnOperationEntry> getRegisteredTableColumnOperations() {
        return Collections.unmodifiableMap(registeredTableColumnOperations);
    }

    /**
     * Registered expression functions. Utilized within {@link DefaultExpressionEvaluator}.
     *
     * @return the registered functions
     */
    public Map<String, ExpressionFunctionEntry> getRegisteredExpressionFunctions() {
        return Collections.unmodifiableMap(registeredExpressionFunctions);
    }

    /**
     * Registers a new operation.
     * The expression function will use the upper-case short name as function name and replace all spaces with underscores.
     *
     * @param id          unique id
     * @param operation   operation instance
     * @param name        name
     * @param shortName   a short name that will be used in table headers (e.g. avg, var, ...)
     * @param description a description
     */
    public void registerColumnOperation(String id, ColumnOperation operation, String name, String shortName, String description) {
        registeredTableColumnOperations.put(id, new ColumnOperationEntry(id, operation, name, shortName, description));
    }

    /**
     * Registers an new function that will be usable within expressions.
     *
     * @param function    the function instance. Its name will be used as identifier.
     * @param name        the human-readable name
     * @param description a description
     */
    public void registerExpressionFunction(ExpressionFunction function, String name, String description) {
        registeredExpressionFunctions.put(function.getName(), new ExpressionFunctionEntry(function.getName(), name, description, function));
    }

    /**
     * Returns an operation by ID
     *
     * @param id the ID
     * @return the operation
     */
    public ColumnOperationEntry getColumnOperationById(String id) {
        return registeredTableColumnOperations.get(id);
    }

    /**
     * Returns all operations that are assignable to the filter class
     *
     * @param filterClass the filter class
     * @return all operations that are assignable to the filter class
     */
    public Map<String, ColumnOperationEntry> getTableColumnOperationsOfType(Class<? extends ColumnOperation> filterClass) {
        Map<String, ColumnOperationEntry> result = new HashMap<>();
        for (Map.Entry<String, ColumnOperationEntry> entry : registeredTableColumnOperations.entrySet()) {
            if (filterClass.isAssignableFrom(entry.getValue().getOperation().getClass())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
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

    public static class ExpressionFunctionEntry {
        private String id;
        private String name;
        private String description;
        private ExpressionFunction function;

        public ExpressionFunctionEntry(String id, String name, String description, ExpressionFunction function) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.function = function;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public ExpressionFunction getFunction() {
            return function;
        }
    }
}
