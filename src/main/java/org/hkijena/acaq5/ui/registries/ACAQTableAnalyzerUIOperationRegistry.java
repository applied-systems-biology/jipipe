/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.registries;


import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableToTableOperation;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableToTableOperationUI;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableVectorOperation;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableVectorOperationUI;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains operations on tables
 */
public class ACAQTableAnalyzerUIOperationRegistry {
    private Map<Class<? extends ACAQTableVectorOperation>, VectorOperationEntry> vectorOperationEntries = new HashMap<>();
    private Map<Class<? extends ACAQTableToTableOperation>, TableToTableOperationEntry> tableToTableOperationEntries = new HashMap<>();

    /**
     * Registers a new operation
     * @param operationClass The operation class
     * @param uiClass User interface
     * @param name Operation name
     * @param shortcut Short operation name shown in table columns
     * @param description Description
     * @param icon Icon for the operation
     */
    public void register(Class<? extends ACAQTableVectorOperation> operationClass,
                         Class<? extends ACAQTableVectorOperationUI> uiClass,
                         String name,
                         String shortcut,
                         String description,
                         Icon icon) {
        vectorOperationEntries.put(operationClass, new VectorOperationEntry(operationClass,
                uiClass,
                name,
                shortcut,
                description,
                icon));
    }

    /**
     * @param operation The operation
     * @return the UI
     */
    public ACAQTableVectorOperationUI createUIForVectorOperation(ACAQTableVectorOperation operation) {
        try {
            if (vectorOperationEntries.get(operation.getClass()).getUiClass() == null)
                return null;
            return vectorOperationEntries.get(operation.getClass()).getUiClass().getConstructor(ACAQTableVectorOperation.class).newInstance(operation);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param operation The operation
     * @return the UI
     */
    public ACAQTableToTableOperationUI createUIForTableToTableOperation(ACAQTableToTableOperation operation) {
        try {
            if (tableToTableOperationEntries.get(operation.getClass()).getUiClass() == null)
                return null;
            return tableToTableOperationEntries.get(operation.getClass()).getUiClass().getConstructor(ACAQTableToTableOperation.class).newInstance(operation);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<VectorOperationEntry> getVectorOperationEntries() {
        return vectorOperationEntries.values();
    }

    public Collection<TableToTableOperationEntry> getTableToTableOperationEntries() {
        return tableToTableOperationEntries.values();
    }

    /**
     * @param operation The operation
     * @return Name of the operation
     */
    public String getNameOf(ACAQTableVectorOperation operation) {
        return vectorOperationEntries.get(operation.getClass()).getName();
    }

    /**
     * @param operation The operation
     * @return Operation shortcut
     */
    public String getShortcutOf(ACAQTableVectorOperation operation) {
        return vectorOperationEntries.get(operation.getClass()).getShortcut();
    }

    /**
     * @param operation The operation
     * @return Operation icon
     */
    public Icon getIconOf(ACAQTableVectorOperation operation) {
        return vectorOperationEntries.get(operation.getClass()).getIcon();
    }

    /**
     * @param operation The operation
     * @return Operation name
     */
    public String getNameOf(ACAQTableToTableOperation operation) {
        return tableToTableOperationEntries.get(operation.getClass()).getName();
    }

    /**
     * @param operation The operation
     * @return operation icon
     */
    public Icon getIconOf(ACAQTableToTableOperation operation) {
        return tableToTableOperationEntries.get(operation.getClass()).getIcon();
    }

    /**
     * Entry with a vector operation
     */
    public static class VectorOperationEntry {
        private Class<? extends ACAQTableVectorOperation> operationClass;
        private Class<? extends ACAQTableVectorOperationUI> uiClass;
        private String name;
        private String shortcut;
        private String description;
        private Icon icon;

        /**
         * @param operationClass the operation class
         * @param uiClass the UI class
         * @param name the name
         * @param shortcut the shortcut
         * @param description the description
         * @param icon the icon
         */
        public VectorOperationEntry(Class<? extends ACAQTableVectorOperation> operationClass, Class<? extends ACAQTableVectorOperationUI> uiClass, String name, String shortcut, String description, Icon icon) {
            this.operationClass = operationClass;
            this.uiClass = uiClass;
            this.name = name;
            this.shortcut = shortcut;
            this.description = description;
            this.icon = icon;
        }

        public Class<? extends ACAQTableVectorOperationUI> getUiClass() {
            return uiClass;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Icon getIcon() {
            return icon;
        }

        public Class<? extends ACAQTableVectorOperation> getOperationClass() {
            return operationClass;
        }

        /**
         * @return New instance
         */
        public ACAQTableVectorOperation instantiateOperation() {
            try {
                return operationClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public String getShortcut() {
            return shortcut;
        }
    }

    /**
     * An oepration entry
     */
    public static class TableToTableOperationEntry {
        private Class<? extends ACAQTableToTableOperation> operationClass;
        private Class<? extends ACAQTableToTableOperationUI> uiClass;
        private String name;
        private String description;
        private Icon icon;

        /**
         * @param operationClass the operation class
         * @param uiClass UI class
         * @param name operation name
         * @param description description
         * @param icon Icon
         */
        public TableToTableOperationEntry(Class<? extends ACAQTableToTableOperation> operationClass, Class<? extends ACAQTableToTableOperationUI> uiClass, String name, String description, Icon icon) {
            this.operationClass = operationClass;
            this.uiClass = uiClass;
            this.name = name;
            this.description = description;
            this.icon = icon;
        }

        public Class<? extends ACAQTableToTableOperationUI> getUiClass() {
            return uiClass;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Icon getIcon() {
            return icon;
        }

        public Class<? extends ACAQTableToTableOperation> getOperationClass() {
            return operationClass;
        }

        /**
         * @return New instance
         */
        public ACAQTableToTableOperation instantiateOperation() {
            try {
                return operationClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
